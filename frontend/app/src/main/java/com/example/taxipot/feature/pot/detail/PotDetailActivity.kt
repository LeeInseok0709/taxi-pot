package com.example.taxipot.feature.pot.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.BuildConfig
import com.example.taxipot.databinding.ActivityPotDetailBinding
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.bubbleBitmap
import com.example.taxipot.feature.pot.main.roundedBg
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

/**
 * EX-04 택시팟 상세
 * 다른 화면에서 열 때는 PotDetailActivity.newIntent(context, potId) 사용
 */
class PotDetailActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotDetailBinding
    private lateinit var pot: PotDetailResponse

    // 상세 불러오기 담당
    private val viewModel: PotDetailViewModel by viewModels()

    // 지도는 처음 한 번만 켜기
    private var mapStarted = false

    companion object {
        private const val EXTRA_POT_ID = "pot_id"

        /** 방 번호(potId)를 담아서 상세 화면 열기 */
        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, PotDetailActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵 준비
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) 받은 방 번호 (불러오기는 7번에서)
        val potId = intent.getLongExtra(EXTRA_POT_ID, -1L)

        // 3) XML 화면 붙이기
        binding = ActivityPotDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4) 상태바·하단바 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 5) 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.mapCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.tvStatusChip.background = roundedBg(PotColors.ACCENT, 8f)
        binding.infoCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)

        // 6) 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 7) 상세 불러오기 → 도착하면 내용 채우기 + 지도
        observeViewModel()
        if (viewModel.uiState.value.pot == null) viewModel.load(potId)
    }

    // 불러온 결과가 오면 화면 채우기, 실패하면 메시지 보여주고 닫기
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.errorMessage != null) {
                        showToast(state.errorMessage)
                        finish()
                        return@collect
                    }
                    val loaded = state.pot ?: return@collect
                    pot = loaded
                    showDetail()

                    if (!mapStarted) {
                        mapStarted = true
                        startMap()
                        // 화면이 이미 켜진 뒤에 지도를 시작했으면 바로 이어서 켜기
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            binding.mapView.resume()
                        }
                    }
                }
            }
        }
    }

    // 상세 정보를 화면에 채우기
    private fun showDetail() {
        val members = "${pot.currentMembers} / ${pot.maxMembers}명"

        // 상태 칩
        val statusText = when {
            pot.status == "RECRUITING" && pot.hasCanceled -> "결원 · 모집 중"
            pot.status == "RECRUITING" -> "모집 중"
            pot.status == "CLOSED" -> "모집 마감"
            pot.status == "RUNNING" -> "운행 중"
            else -> "종료"
        }
        binding.tvStatusChip.text = "$statusText · $members"

        binding.tvRoute.text = "${pot.departureName} → ${pot.destinationName}"
        binding.tvFare.text = "예상 총 요금 %,d원".format(pot.expectedFare)

        // 정보 카드
        binding.tvDepartureValue.text = "${clockOf(pot.departureTime)} (집결 ${clockOf(pot.arrivalDeadline)})"
        binding.tvMembersValue.text = members
        binding.tvGenderValue.text = when (pot.genderOption) {
            "FEMALE" -> "여성만"
            "MALE" -> "남성만"
            else -> "제한 없음"
        }
        binding.tvHostValue.text = "${pot.hostNickname} ★${pot.hostRatingAvg}"
        binding.tvHoldValue.text = "%,dP".format(pot.holdAmount)
        binding.tvCancelValue.text = "${clockOf(pot.cancelDeadline)}까지"

        // 함께 타는 학생 (참여 중·탑승한 사람만)
        binding.tvMembersList.text = pot.members
            .filter { it.status == "JOINED" || it.status == "BOARDED" }
            .joinToString("\n") { m ->
                val hostMark = if (m.role == "HOST") " (방장)" else ""
                "${m.nickname}$hostMark · ★${m.ratingAvg}"
            }

        // 참여 버튼: 상황에 따라 글자와 색이 바뀜 (이미 참여한 방이면 대기실로)
        val canJoin = !pot.isJoined && pot.status == "RECRUITING" && pot.currentMembers < pot.maxMembers
        binding.btnJoin.text = when {
            pot.isJoined -> "대기실로 이동"
            canJoin -> "참여하기"
            else -> "모집 마감"
        }
        binding.btnJoin.background = if (canJoin || pot.isJoined) {
            roundedBg(PotColors.ACCENT, 12f)
        } else {
            roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        }
        binding.btnJoin.setOnClickListener {
            when {
                pot.isJoined -> startActivity(com.example.taxipot.feature.pot.room.WaitingRoomActivity.newIntent(this, pot.id))
                canJoin -> startActivity(com.example.taxipot.feature.pot.join.PotJoinActivity.newIntent(this, pot))
            }
        }
    }

    // 지도 켜기: 출발·도착 가운데를 보여주고 말풍선 2개 찍기
    private fun startMap() {
        val centerLat = (pot.departureLat + pot.destinationLat) / 2
        val centerLng = (pot.departureLng + pot.destinationLng) / 2

        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "상세 화면 지도 오류: ${error.message}")
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

            // 출발지와 도착지의 가운데
            override fun getPosition(): LatLng = LatLng.from(centerLat, centerLng)

            // 두 곳이 다 보이게 조금 넓게 (숫자가 작을수록 넓게 보임)
            override fun getZoomLevel(): Int = 13
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized) binding.mapView.pause()
    }
}