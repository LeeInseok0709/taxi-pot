package com.example.taxipot.feature.pot.join

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
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
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.databinding.ActivityPotJoinBinding
import com.example.taxipot.databinding.DialogInsufficientBalanceBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.PotRepository
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.main.LocationHelper
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg
import com.example.taxipot.feature.pot.room.showActivePotDialog
import com.example.taxipot.feature.wallet.WalletBalance
import kotlinx.coroutines.launch

/**
 * JN-01 참여하기 (희망 좌석 선택 → 예치하고 참여)
 * 성공하면 JN-02 참여 완료, 잔액이 부족하면 JN-03 잔액 부족 안내 창
 *
 * RD-03 입장 요청 모드 (참여 마감 후): 같은 화면에서 좌석 선택 → 내 위치(출발지 50m) 확인 → 입장 요청 보내기
 * 성공하면 상세 화면으로 돌아가서 "방장 수락 대기 중"으로 보임
 */
class PotJoinActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotJoinBinding
    private lateinit var pot: PotDetailResponse

    // 참여 담당
    private val viewModel: PotJoinViewModel by viewModels()

    // 지금 고른 좌석 (처음엔 상관없음)
    private var selectedSeat = "NONE"

    // 좌석 값 ↔ 버튼
    private lateinit var seatViews: Map<String, TextView>

    // 현재 잔액 (지갑 API 연결 전까지 가짜 값)
    private val balance = WalletBalance.latest ?: 0

    // 성공 후 화면 이동은 한 번만
    private var moved = false

    // true = 입장 요청 모드 (참여 마감 후)
    private var requestMode = false

    // 위에 "참여하기" 제목, 주의 카드 둘째 줄 (XML에 id가 없어서 옆 뷰로 찾음)
    private var titleView: TextView? = null
    private var warningBodyView: TextView? = null

    // 출발지 반경 (명세서: 50m 안에서만 입장 요청)
    private val requestRadiusMeters = 50f

    companion object {
        private const val EXTRA_POT = "pot"
        private const val EXTRA_REQUEST_MODE = "request_mode"

        /**
         * 상세 화면의 방 정보를 담아서 참여하기 열기
         * requestMode = true 면 입장 요청 모드 (참여 마감 후)
         */
        fun newIntent(context: Context, pot: PotDetailResponse, requestMode: Boolean = false): Intent =
            Intent(context, PotJoinActivity::class.java)
                .putExtra(EXTRA_POT, ApiClient.gson.toJson(pot))
                .putExtra(EXTRA_REQUEST_MODE, requestMode)
    }

    // 위치 권한 허용 창의 결과 (허용하면 바로 입장 요청 이어서)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            sendJoinRequest()
        } else {
            showToast("위치 권한을 허용해야 입장 요청을 보낼 수 있어요")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 상세 화면에서 넘겨준 방 정보 꺼내기
        val json = intent.getStringExtra(EXTRA_POT)
        if (json == null) {
            finish()
            return
        }
        pot = ApiClient.gson.fromJson(json, PotDetailResponse::class.java)
        requestMode = intent.getBooleanExtra(EXTRA_REQUEST_MODE, false)

        // 2) XML 화면 붙이기
        binding = ActivityPotJoinBinding.inflate(layoutInflater)
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
        binding.carCard.background = roundedBg(PotColors.CARD, 40f, PotColors.LINE)
        binding.seatDriver.background = roundedBg(0xFFEFE9DF.toInt(), 10f, PotColors.LINE)
        binding.moneyCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.warningCard.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnJoin.background = roundedBg(PotColors.ACCENT, 12f)

        // 5) 좌석 버튼
        seatViews = mapOf(
            "FRONT" to binding.seatFront,
            "BACK_LEFT" to binding.seatBackLeft,
            "BACK_MID" to binding.seatBackMid,
            "BACK_RIGHT" to binding.seatBackRight,
            "NONE" to binding.seatNone
        )
        seatViews.forEach { (code, view) ->
            view.setOnClickListener {
                selectedSeat = code
                renderSeats()
            }
        }
        binding.seatDriver.setOnClickListener { showToast("운전석은 고를 수 없어요") }

        // 6) 예치액 · 참여 후 잔액 · 취소 안내
        val after = balance - pot.holdAmount
        binding.tvHoldValue.text = "%,dP".format(pot.holdAmount)
        binding.tvAfterBalance.text = if (after >= 0) {
            "%,dP".format(after)
        } else {
            "잔액 부족 (%,dP 모자람)".format(-after)
        }
        titleView = (binding.btnBack.parent as? ViewGroup)?.getChildAt(1) as? TextView
        warningBodyView = (binding.tvWarningTitle.parent as? ViewGroup)?.getChildAt(1) as? TextView

        // 7) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnJoin.setOnClickListener {
            if (requestMode) {
                onRequestClick()
            } else {
                viewModel.join(pot.id, selectedSeat, pot.holdAmount, balance)
            }
        }

        // 8) 처음 표시 + 결과 기다리기
        renderSeats()
        renderMode()
        observeViewModel()
    }

    // 참여 / 입장 요청 모드에 맞게 제목·안내·버튼 글자 바꾸기
    private fun renderMode() {
        if (requestMode) {
            titleView?.text = "입장 요청"
            binding.tvWarningTitle.text = "ⓘ 참여 마감이 지나 방장이 수락해야 참여돼요"
            warningBodyView?.text = "출발지 50m 안에서만 보낼 수 있어요. 수락되면 예치금이 빠지고 바로 도착 처리돼요."
            binding.btnJoin.text = "입장 요청 보내기"
        } else {
            titleView?.text = "참여하기"
            binding.tvWarningTitle.text = "ⓘ 출발 10분 전(${clockOf(pot.cancelDeadline)})부터는 참여 취소 불가"
            warningBodyView?.text = "좌석과 예치액을 확인하고 참여해주세요."
            binding.btnJoin.text = "예치하고 참여"
        }
    }

    // 입장 요청 버튼: 위치 권한 → 현재 위치 → 출발지 50m 안인지 → 서버에 요청
    private fun onRequestClick() {
        if (LocationHelper.hasPermission(this)) {
            sendJoinRequest()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun sendJoinRequest() {
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
            if (!PotRepository.USE_FAKE && meters > requestRadiusMeters) {
                showToast("출발지까지 약 %,dm 남았어요. 50m 안에서 다시 눌러 주세요".format(meters.toInt()))
                return@getCurrentLocation
            }
            viewModel.requestJoin(
                pot.id, selectedSeat, location.latitude, location.longitude, pot.holdAmount, balance
            )
        }
    }

    // 고른 좌석만 베이지 + 굵게
    private fun renderSeats() {
        seatViews.forEach { (code, view) ->
            val isSelected = code == selectedSeat
            view.background = if (isSelected) {
                roundedBg(PotColors.ACCENT, 10f, PotColors.LINE)
            } else {
                roundedBg(PotColors.CARD, 10f, PotColors.LINE)
            }
            view.paint.isFakeBoldText = isSelected
            view.invalidate()
        }
    }

    // 버튼 상태, 실패 메시지, 잔액 부족 창, 성공하면 JN-02
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 서버 판단으로 참여 ↔ 입장 요청 바꾸기 (그사이 마감이 지났거나, 아직 마감 전)
                    state.switchToRequest?.let {
                        requestMode = it
                        viewModel.switchShown()
                    }
                    if (state.isLoading) {
                        binding.btnJoin.text = if (requestMode) "요청 보내는 중..." else "참여하는 중..."
                    } else {
                        renderMode()
                    }
                    binding.btnJoin.isEnabled = !state.isLoading

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }

                    state.insufficient?.let {
                        showInsufficientDialog(it)
                        viewModel.insufficientShown()
                    }

                    // 이미 다른 방에 참여 중 → 그 방으로 가는 안내 창
                    state.activePotId?.let {
                        showActivePotDialog(it)
                        viewModel.activePotShown()
                    }

                    // 입장 요청 성공 → 상세 화면으로 돌아가서 "수락 대기 중"
                    if (state.requested != null && !moved) {
                        moved = true
                        showToast("입장 요청을 보냈어요! 방장이 수락하면 바로 참여돼요")
                        finish()
                        return@collect
                    }

                    val joined = state.joined
                    if (joined != null && !moved) {
                        moved = true
                        startActivity(PotJoinDoneActivity.newIntent(this@PotJoinActivity, pot, selectedSeat, joined))
                        finish()
                    }
                }
            }
        }
    }

    // JN-03 잔액 부족 안내 창
    private fun showInsufficientDialog(info: InsufficientInfo) {
        val dialog = Dialog(this)
        val dialogBinding = DialogInsufficientBalanceBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // 창 모양: 둥근 크림색 카드, 화면 폭의 88%
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogBinding.dialogRoot.background = roundedBg(PotColors.BACKGROUND, 16f)
        dialogBinding.btnClose.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        dialogBinding.btnCharge.background = roundedBg(PotColors.ACCENT, 10f)
        dialogBinding.reasonCard.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)

        // 금액
        dialogBinding.tvRequired.text = "%,dP".format(info.required)
        dialogBinding.tvBalance.text = "%,dP".format(info.balance)
        dialogBinding.tvShortage.text = "%,dP".format(info.shortage)

        // 버튼
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnCharge.setOnClickListener {
            dialog.dismiss()
            openWalletCharge()
        }

        dialog.show()
    }

    // WL-02 포인트 충전 화면 열기 (지연님 WalletChargeActivity)
    // 아직 화면이 없으면 빌드 오류가 나지 않도록 이름(글자)으로 찾아서 열고, 없으면 안내만
    private fun openWalletCharge() {
        val intent = Intent().setClassName(this, "com.example.taxipot.feature.wallet.charge.WalletChargeActivity")
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            showToast("충전 화면은 준비 중이에요")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}