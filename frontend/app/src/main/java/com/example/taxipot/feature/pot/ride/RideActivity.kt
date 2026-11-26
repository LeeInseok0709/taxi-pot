package com.example.taxipot.feature.pot.ride

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.BuildConfig
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.databinding.ActivityRideBinding
import com.example.taxipot.feature.pot.chat.ChatActivity
import com.example.taxipot.feature.pot.join.seatLabel
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
import kotlinx.coroutines.launch

/**
 * RD-04 탑승 중
 * 함께 탄 사람, 카카오 T·우버 바로 부르기(출발지·도착지 좌표로 딥링크), 채팅, (방장) 하차 · 실요금 청구
 */
class RideActivity : ComponentActivity() {

    private lateinit var binding: ActivityRideBinding
    private val viewModel: RideViewModel by viewModels()

    // 지도는 처음 한 번만 켜기
    private var mapStarted = false

    companion object {
        private const val EXTRA_POT_ID = "pot_id"

        // 앱이 없을 때 여는 스토어 패키지 이름
        private const val KAKAO_T_PACKAGE = "com.kakao.taxi"

        /** 방 번호를 담아서 탑승 중 화면 열기 */
        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, RideActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵 준비
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) XML 화면 붙이기
        binding = ActivityRideBinding.inflate(layoutInflater)
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

        // 4) 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.tvChip.background = roundedBg(PotColors.ACCENT, 8f)
        binding.mapCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.membersCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.btnKakaoT.background = roundedBg(PotColors.ACCENT, 12f)
        binding.btnUber.background = roundedBg(PotColors.WHITE, 12f, PotColors.LINE)
        binding.btnChat.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.tvInfo.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnFareClaim.background = roundedBg(PotColors.ACCENT, 12f)

        // 5) 버튼
        val potId = intent.getLongExtra(EXTRA_POT_ID, -1L)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnKakaoT.setOnClickListener { viewModel.uiState.value.pot?.let { openKakaoT(it) } }
        binding.btnUber.setOnClickListener { viewModel.uiState.value.pot?.let { openUber(it) } }
        binding.btnChat.setOnClickListener { startActivity(ChatActivity.newIntent(this, potId)) }
        binding.btnFareClaim.setOnClickListener { startActivity(FareClaimActivity.newIntent(this, potId)) }

        // 6) 어느 방인지 정하고 결과 기다리기 (불러오기는 onResume에서)
        viewModel.setPot(potId)
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.message?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }
                    val pot = state.pot ?: return@collect
                    render(pot, state.myUserId)
                }
            }
        }
    }

    // 방 정보로 화면 채우기
    private fun render(pot: PotDetailResponse, myUserId: Long?) {
        // 정산이 끝났거나 방이 취소됐으면 안내하고 홈으로
        if (pot.status == "COMPLETED" || pot.status == "CANCELED") {
            showToast(if (pot.status == "COMPLETED") "정산이 끝났어요. 이용해 주셔서 고마워요!" else "방이 취소됐어요")
            goHome()
            return
        }

        val isHost = myUserId != null && myUserId == pot.hostId
        val members = pot.members.filter { it.status == "BOARDED" || it.status == "JOINED" }

        binding.tvChip.text = "탑승 중 · ${members.size}명 · ${fareClaimLabel(pot.fareClaimStatus)}"
        binding.tvRoute.text = "${pot.departureName} → ${pot.destinationName}"

        // 함께 탄 사람: "콩떡 (방장) (나) ........ 조수석"
        binding.membersContainer.removeAllViews()
        members.forEachIndexed { index, m ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (index > 0) topMargin = dp(8f).toInt() }
            }
            row.addView(TextView(this).apply {
                text = buildString {
                    append(m.nickname)
                    if (m.role == "HOST") append(" (방장)")
                    if (m.memberId == myUserId) append(" (나)")
                }
                setTextColor(PotColors.SUB_TEXT)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = seatLabel(m.preferredSeat)
                setTextColor(PotColors.TEXT)
                textSize = 13f
            })
            binding.membersContainer.addView(row)
        }

        // 안내 + (방장) 하차·청구 버튼
        binding.tvInfo.text = infoText(pot, isHost)
        val canClaim = pot.fareClaimStatus in listOf("NONE", "REJECTED", "OVERDUE")
        binding.btnFareClaim.visibility = if (isHost && canClaim) View.VISIBLE else View.GONE
        binding.btnFareClaim.text = if (pot.fareClaimStatus == "NONE") "하차 · 실요금 청구" else "실요금 다시 청구"

        // 지도 (처음 한 번)
        if (!mapStarted) {
            mapStarted = true
            startMap(pot)
        }
    }

    // 청구 상태 → 짧은 글자
    private fun fareClaimLabel(status: String): String = when (status) {
        "PENDING" -> "청구 확인 중"
        "APPROVED" -> "정산 완료"
        "REJECTED" -> "청구 반려"
        "OVERDUE" -> "청구 기한 지남"
        else -> "청구 전"
    }

    // 아래 안내 문장
    private fun infoText(pot: PotDetailResponse, isHost: Boolean): String = when (pot.fareClaimStatus) {
        "PENDING" -> "ⓘ 방장이 실요금을 청구했어요. 확인이 끝나면 예치금에서 각자 몫이 정산돼요."
        "APPROVED" -> "ⓘ 정산이 끝났어요."
        "REJECTED" -> "ⓘ 청구가 반려됐어요." +
                (pot.fareRejectReason?.let { " (사유: $it)" } ?: "") +
                if (isHost) "\n결제내역을 확인하고 다시 청구해 주세요." else ""
        "OVERDUE" -> "ⓘ 실요금 청구 기한(탑승 후 24시간)이 지났어요." +
                if (isHost) " 지금이라도 청구해 주세요." else ""
        else -> if (isHost) {
            "ⓘ 위 버튼으로 택시를 부르고, 하차한 뒤 결제내역 사진과 실요금을 청구해 주세요.\n탑승 후 24시간 안에 청구해야 해요."
        } else {
            "ⓘ 택시는 방장이 불러요. 하차 후 방장이 실요금을 청구하면 예치금에서 내 몫만 빠지고 나머지는 돌아와요."
        }
    }

    // 카카오 T: 출발지·도착지 좌표를 담아 택시 호출 화면 열기, 앱이 없으면 스토어
    private fun openKakaoT(pot: PotDetailResponse) {
        val uri = Uri.parse(
            "https://t.kakao.com/launch?type=taxi" +
                    "&origin_lat=${pot.departureLat}&origin_lng=${pot.departureLng}" +
                    "&dest_lat=${pot.destinationLat}&dest_lng=${pot.destinationLng}"
        )
        // 카카오 T 앱으로만 열기 (앱이 없으면 브라우저 대신 예외가 나서 아래로 넘어감)
        if (tryOpen(Intent(Intent.ACTION_VIEW, uri).setPackage(KAKAO_T_PACKAGE))) return
        // 앱은 있는데 위 링크를 못 받는 버전이면 앱만 열기
        if (tryOpen(Intent(Intent.ACTION_VIEW, Uri.parse("kakaot://")))) return
        showToast("카카오 T 앱이 없어서 설치 화면으로 이동해요")
        openStore(KAKAO_T_PACKAGE)
    }

    // 우버: 우버 공식 링크 (앱이 있으면 앱, 없으면 웹에서 출발·도착이 채워진 채로 열림)
    private fun openUber(pot: PotDetailResponse) {
        val url = "https://m.uber.com/ul/?action=setPickup" +
                "&pickup[latitude]=${pot.departureLat}&pickup[longitude]=${pot.departureLng}" +
                "&pickup[nickname]=${Uri.encode(pot.departureName)}" +
                "&dropoff[latitude]=${pot.destinationLat}&dropoff[longitude]=${pot.destinationLng}" +
                "&dropoff[nickname]=${Uri.encode(pot.destinationName)}"
        if (!tryOpen(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
            showToast("우버를 열 수 없어요")
        }
    }

    // 플레이 스토어 앱 → 없으면 웹 스토어
    private fun openStore(packageName: String) {
        if (!tryOpen(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))) {
            tryOpen(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    // 열 수 있는 앱이 있으면 열고 true, 없으면 false
    private fun tryOpen(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    // 지도: 출발지·도착지 말풍선, 가운데를 보여줌
    private fun startMap(pot: PotDetailResponse) {
        val centerLat = (pot.departureLat + pot.destinationLat) / 2
        val centerLng = (pot.departureLng + pot.destinationLng) / 2

        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "탑승 중 지도 오류: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                val labelManager = map.labelManager ?: return
                val layer = labelManager.layer ?: return
                val startStyle = labelManager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(bubbleBitmap("출발 · ${pot.departureName}", 0.65f)))
                )
                val endStyle = labelManager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(bubbleBitmap("도착 · ${pot.destinationName}", 0.65f)))
                )
                layer.addLabel(
                    LabelOptions.from("start", LatLng.from(pot.departureLat, pot.departureLng)).setStyles(startStyle)
                )
                layer.addLabel(
                    LabelOptions.from("end", LatLng.from(pot.destinationLat, pot.destinationLng)).setStyles(endStyle)
                )
            }

            override fun getPosition(): LatLng = LatLng.from(centerLat, centerLng)

            override fun getZoomLevel(): Int = 13
        })
        // 화면이 이미 켜진 뒤에 지도를 시작했으면 바로 이어서 켜기
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            binding.mapView.resume()
        }
    }

    // 홈으로 (중간 화면들은 닫힘)
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
        viewModel.startRealtime()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRealtime()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
        viewModel.refresh()   // 택시 앱에서 돌아왔을 때도 최신으로
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }
}