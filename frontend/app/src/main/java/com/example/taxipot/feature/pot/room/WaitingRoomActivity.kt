package com.example.taxipot.feature.pot.room

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.BuildConfig
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotMemberResponse
import com.example.taxipot.databinding.ActivityWaitingRoomBinding
import com.example.taxipot.feature.pot.chat.ChatActivity
import com.example.taxipot.feature.pot.data.PotRepository
import com.example.taxipot.feature.pot.join.seatLabel
import com.example.taxipot.feature.pot.main.LocationHelper
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.PotMainActivity
import com.example.taxipot.feature.pot.main.bubbleBitmap
import com.example.taxipot.feature.pot.main.dp
import com.example.taxipot.feature.pot.main.roundedBg
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RD-01 대기실
 * 참여자·좌석·도착 현황, 도착 확인(GPS 50m), 탑승 시작·내보내기·방 취소(방장), 참여 취소(동승자)
 */
class WaitingRoomActivity : ComponentActivity() {

    private lateinit var binding: ActivityWaitingRoomBinding
    private val viewModel: WaitingRoomViewModel by viewModels()

    // 지도는 처음 한 번만 켜기
    private var mapStarted = false

    // 출발 반경 (명세서: 출발지 50m 안에서만 도착 확인)
    private val arrivalRadiusMeters = 50f

    companion object {
        private const val EXTRA_POT_ID = "pot_id"

        /** 방 번호를 담아서 대기실 열기 */
        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, WaitingRoomActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }

    // 위치 권한 허용 창의 결과 (허용하면 바로 도착 확인 이어서)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            checkArrival()
        } else {
            showToast("위치 권한을 허용해야 도착 확인을 할 수 있어요")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵 준비
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) XML 화면 붙이기
        binding = ActivityWaitingRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3) 상태바·하단바 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 4) 고정 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.tvChip.background = roundedBg(PotColors.ACCENT, 8f)
        binding.mapCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.membersCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        listOf(binding.btnEdit, binding.btnChat, binding.btnCancel, binding.btnJoinRequests, binding.tvInfo)
            .forEach { it.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE) }

        // 5) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener { showEditDialog() }
        binding.btnChat.setOnClickListener {
            startActivity(ChatActivity.newIntent(this, intent.getLongExtra(EXTRA_POT_ID, -1L)))
        }
        binding.btnJoinRequests.setOnClickListener { startActivity(JoinRequestActivity.newIntent(this, intent.getLongExtra(EXTRA_POT_ID, -1L))) }
        binding.btnCancel.setOnClickListener { confirmCancel() }
        binding.btnArrive.setOnClickListener { onArriveClick() }
        binding.btnStart.setOnClickListener { viewModel.start() }

        // 6) 어느 방인지 정하고 결과 기다리기 (불러오기는 onResume에서)
        viewModel.setPot(intent.getLongExtra(EXTRA_POT_ID, -1L))
        observeViewModel()
        startCountdown()
    }

    // 상태가 바뀔 때마다 화면 다시 그리기
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.message?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }
                    if (state.closed) {
                        goHome()
                        return@collect
                    }
                    val pot = state.pot ?: return@collect
                    render(pot, state.myUserId, state.actionLoading)
                    binding.btnJoinRequests.text = if (state.joinRequestCount > 0) "입장 요청 (${state.joinRequestCount})" else "입장 요청"
                    binding.btnJoinRequests.background = roundedBg(if (state.joinRequestCount > 0) PotColors.ACCENT else PotColors.CARD, 10f, PotColors.LINE)
                }
            }
        }
    }

    // 방 정보로 화면 채우기
    private fun render(pot: PotDetailResponse, myUserId: Long?, actionLoading: Boolean) {
        // 다른 사람(방장)이 방을 취소했으면 안내하고 닫기
        if (pot.status == "CANCELED") {
            showToast("방이 취소됐어요. 예치금은 환불돼요")
            goHome()
            return
        }

        // 탑승이 시작됐으면 탑승 중 화면으로 (동승자는 실시간으로 자동 이동)
        if (pot.status == "RUNNING") {
            startActivity(com.example.taxipot.feature.pot.ride.RideActivity.newIntent(this, pot.id))
            finish()
            return
        }

        val isHost = myUserId != null && myUserId == pot.hostId
        val members = pot.members.filter { it.status == "JOINED" || it.status == "BOARDED" }
        val me = members.firstOrNull { it.memberId == myUserId }
        val iArrived = me?.arrivedAt != null
        val allArrived = members.isNotEmpty() && members.all { it.arrivedAt != null }
        val canStart = allArrived && members.size >= 2
        val waiting = pot.status == "RECRUITING" || pot.status == "CLOSED"

        // 제목: "백석대 → 천안역"
        binding.tvTitle.text = "${pot.departureName.replace("대학교", "대")} → ${pot.destinationName}"

        // 참여자 줄 + (방장) 미도착 동승자 내보내기 버튼
        binding.membersContainer.removeAllViews()
        members.forEachIndexed { index, m ->
            binding.membersContainer.addView(memberRow(m, myUserId, index > 0))
            if (isHost && waiting && m.role != "HOST" && m.arrivedAt == null) {
                binding.membersContainer.addView(kickButton(m))
            }
        }
        binding.tvVacancy.text = "${(pot.maxMembers - pot.currentMembers).coerceAtLeast(0)}석"

        // 방장 / 동승자에 따라 버튼 다르게
        binding.btnEdit.visibility = if (isHost) View.VISIBLE else View.GONE
        binding.btnJoinRequests.visibility = if (isHost) View.VISIBLE else View.GONE
        binding.btnStart.visibility = if (isHost) View.VISIBLE else View.GONE
        binding.btnCancel.text = if (isHost) "방 취소" else "참여 취소"
        // 출발 10분 전(취소 마감)이 지나면 취소 버튼 잠그기
        val cancelClosed = (parseTime(pot.cancelDeadline) ?: Long.MAX_VALUE) <= System.currentTimeMillis()
        if (cancelClosed && waiting) binding.btnCancel.text = "취소 마감 (출발 10분 전)"
        binding.btnCancel.isEnabled = waiting && !cancelClosed
        binding.btnCancel.alpha = if (binding.btnCancel.isEnabled) 1f else 0.5f

        // 도착 확인 버튼
        binding.btnArrive.text = when {
            !waiting -> "도착 확인 마감"
            iArrived -> "도착 완료 ✓"
            actionLoading -> "확인 중..."
            else -> "도착 확인 (50m 이내)"
        }
        binding.btnArrive.isEnabled = waiting && !iArrived && !actionLoading
        binding.btnArrive.background = if (binding.btnArrive.isEnabled) {
            roundedBg(PotColors.WHITE, 12f, PotColors.LINE)
        } else {
            roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        }

        // 탑승 시작 버튼 (방장): 전원 도착 + 2명 이상이면 베이지로 강조
        binding.btnStart.text = when {
            pot.status == "RUNNING" -> "탑승 중 (RD-04 준비 중)"
            canStart -> "탑승 시작"
            else -> "탑승 시작 (전원 도착 시)"
        }
        binding.btnStart.isEnabled = waiting && !actionLoading
        binding.btnStart.background = if (canStart && waiting) {
            roundedBg(PotColors.ACCENT, 12f)
        } else {
            roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        }

        // 지도 (처음 한 번)
        if (!mapStarted) {
            mapStarted = true
            startMap(pot)
        }
    }

    // 참여자 한 줄: "콩떡 (방장) (나)" ........ "조수석 · ✓ 도착"
    private fun memberRow(m: PotMemberResponse, myUserId: Long?, addGap: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { if (addGap) topMargin = dp(10f).toInt() }
        }
        val name = buildString {
            append(m.nickname)
            if (m.role == "HOST") append(" (방장)")
            if (m.memberId == myUserId) append(" (나)")
        }
        row.addView(TextView(this).apply {
            text = name
            setTextColor(PotColors.SUB_TEXT)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "${seatLabel(m.preferredSeat)} · ${if (m.arrivedAt != null) "✓ 도착" else "미도착"}"
            setTextColor(PotColors.TEXT)
            textSize = 13f
            paint.isFakeBoldText = m.arrivedAt != null
        })
        return row
    }

    // (방장) 미도착 동승자 내보내기 버튼
    private fun kickButton(m: PotMemberResponse): TextView = TextView(this).apply {
        text = "${m.nickname} 내보내기"
        setTextColor(PotColors.TEXT)
        textSize = 13f
        gravity = Gravity.CENTER
        background = roundedBg(PotColors.WHITE, 10f, PotColors.LINE)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42f).toInt()
        ).apply { topMargin = dp(8f).toInt() }
        setOnClickListener { confirmKick(m) }
    }

    // 도착 확인 버튼: 위치 권한 → 현재 위치 → 출발지 50m 안인지 → 서버에 도착 확인
    private fun onArriveClick() {
        if (LocationHelper.hasPermission(this)) {
            checkArrival()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkArrival() {
        val pot = viewModel.uiState.value.pot ?: return
        if (!LocationHelper.isLocationOn(this)) {
            showToast("휴대폰 설정에서 위치(GPS)를 켜 주세요")
            return
        }
        showToast("내 위치를 확인하는 중...")
        LocationHelper.getCurrentLocation(this) { location ->
            if (location == null) {
                showToast("내 위치를 찾지 못했어요. 잠시 후 다시 눌러 주세요")
                return@getCurrentLocation
            }
            val meters = LocationHelper.distanceMeters(
                location.latitude, location.longitude, pot.departureLat, pot.departureLng
            )
            // 실제 서버일 때는 50m 밖이면 미리 안내 (서버도 한 번 더 검사함)
            // 가짜 데이터일 때는 집에서도 시험할 수 있게 거리와 상관없이 진행
            if (!PotRepository.USE_FAKE && meters > arrivalRadiusMeters) {
                showToast("출발지까지 약 %,dm 남았어요. 50m 안에서 다시 눌러 주세요".format(meters.toInt()))
                return@getCurrentLocation
            }
            viewModel.arrive(location.latitude, location.longitude)
        }
    }

    // 방 취소(방장) / 참여 취소(동승자) 확인 창
    private fun confirmCancel() {
        val pot = viewModel.uiState.value.pot ?: return
        val isHost = viewModel.uiState.value.myUserId == pot.hostId
        AlertDialog.Builder(this)
            .setTitle(if (isHost) "방을 취소할까요?" else "참여를 취소할까요?")
            .setMessage(
                if (isHost) "모든 참여자의 예치금이 환불되고 방이 사라져요."
                else "예치금은 환불돼요. 출발 10분 전부터는 취소할 수 없어요."
            )
            .setNegativeButton("아니요", null)
            .setPositiveButton("취소하기") { _, _ ->
                if (isHost) viewModel.cancelPot() else viewModel.leave()
            }
            .show()
    }

    // 방 수정 창 (방장): 제목·오픈채팅 링크, 바뀐 것만 보냄
    private fun showEditDialog() {
        val pot = viewModel.uiState.value.pot ?: return
        if (pot.status != "RECRUITING" && pot.status != "CLOSED") {
            showToast("탑승이 시작된 뒤에는 수정할 수 없어요")
            return
        }
        val pad = dp(20f).toInt()
        val etTitle = android.widget.EditText(this).apply {
            hint = "방 제목 (100자 이하)"
            setText(pot.title)
            filters = arrayOf(android.text.InputFilter.LengthFilter(100))
        }
        val etUrl = android.widget.EditText(this).apply {
            hint = "오픈채팅 링크 (선택)"
            setText(pot.openChatUrl ?: "")
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            filters = arrayOf(android.text.InputFilter.LengthFilter(500))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(etTitle)
            addView(etUrl)
        }
        AlertDialog.Builder(this)
            .setTitle("방 수정")
            .setView(box)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장") { _, _ ->
                val title = etTitle.text.toString().trim()
                val url = etUrl.text.toString().trim()
                if (title.isEmpty()) {
                    showToast("방 제목을 입력해 주세요")
                    return@setPositiveButton
                }
                val newTitle = title.takeIf { it != pot.title }
                val newUrl = url.takeIf { it.isNotEmpty() && it != (pot.openChatUrl ?: "") }
                if (newTitle == null && newUrl == null) {
                    showToast("바뀐 내용이 없어요")
                    return@setPositiveButton
                }
                viewModel.updatePot(newTitle, newUrl)
            }
            .show()
    }

    // 내보내기 확인 창
    private fun confirmKick(m: PotMemberResponse) {
        AlertDialog.Builder(this)
            .setTitle("${m.nickname}님을 내보낼까요?")
            .setMessage("아직 도착하지 않은 참여자예요. 내보내면 같은 방에 다시 들어올 수 없어요.")
            .setNegativeButton("아니요", null)
            .setPositiveButton("내보내기") { _, _ -> viewModel.kick(m.memberId, m.nickname) }
            .show()
    }

    // 집결까지 남은 시간을 1초마다 갱신 ("집결까지 04:12 · 모집 마감")
    private fun startCountdown() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    viewModel.uiState.value.pot?.let { binding.tvChip.text = chipText(it) }
                    delay(1000)
                }
            }
        }
    }

    private fun chipText(pot: PotDetailResponse): String {
        val statusText = when (pot.status) {
            "RECRUITING" -> "모집 중"
            "CLOSED" -> "모집 마감"
            "RUNNING" -> "탑승 중"
            "CANCELED" -> "취소됨"
            else -> "종료"
        }
        if (pot.status == "RUNNING") return statusText

        val deadline = parseTime(pot.arrivalDeadline) ?: return statusText
        val leftSec = (deadline - System.currentTimeMillis()) / 1000
        return if (leftSec > 0) {
            "집결까지 %02d:%02d · %s".format(leftSec / 60, leftSec % 60, statusText)
        } else {
            "집결 마감 · $statusText"
        }
    }

    // "2026-11-25T21:25:00+09:00" → 밀리초
    private fun parseTime(time: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.KOREA).parse(time)?.time
    } catch (e: Exception) {
        null
    }

    // 지도: 집결지(출발지)에 말풍선
    private fun startMap(pot: PotDetailResponse) {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "대기실 지도 오류: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                val labelManager = map.labelManager ?: return
                val layer = labelManager.layer ?: return
                val style = labelManager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(bubbleBitmap("집결지 · ${pot.departureName}", 0.65f)))
                )
                layer.addLabel(
                    LabelOptions.from("gather", LatLng.from(pot.departureLat, pot.departureLng)).setStyles(style)
                )
            }

            override fun getPosition(): LatLng = LatLng.from(pot.departureLat, pot.departureLng)

            override fun getZoomLevel(): Int = 17
        })
        // 화면이 이미 켜진 뒤에 지도를 시작했으면 바로 이어서 켜기
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            binding.mapView.resume()
        }
    }

    // 방 취소·참여 취소 뒤: 홈으로 (중간 화면들은 닫힘)
    private fun goHome() {
        startActivity(
            Intent(this, PotMainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startRealtime()   // 화면이 보이는 동안 실시간 연결 (POT_UPDATED → 자동 새로고침)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRealtime()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }
}