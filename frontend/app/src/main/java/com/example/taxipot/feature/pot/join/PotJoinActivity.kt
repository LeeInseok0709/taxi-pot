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
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg
import kotlinx.coroutines.launch

/**
 * JN-01 참여하기 (희망 좌석 선택 → 예치하고 참여)
 * 성공하면 JN-02 참여 완료, 잔액이 부족하면 JN-03 잔액 부족 안내 창
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
    private val balance = FakePotData.FAKE_BALANCE

    // 성공 후 화면 이동은 한 번만
    private var moved = false

    companion object {
        private const val EXTRA_POT = "pot"

        /** 상세 화면의 방 정보를 담아서 참여하기 열기 */
        fun newIntent(context: Context, pot: PotDetailResponse): Intent =
            Intent(context, PotJoinActivity::class.java)
                .putExtra(EXTRA_POT, ApiClient.gson.toJson(pot))
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
        binding.tvWarningTitle.text = "ⓘ 출발 10분 전(${clockOf(pot.cancelDeadline)})부터는 참여 취소 불가"

        // 7) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnJoin.setOnClickListener {
            viewModel.join(pot.id, selectedSeat, pot.holdAmount, balance)
        }

        // 8) 처음 표시 + 결과 기다리기
        renderSeats()
        observeViewModel()
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
                    binding.btnJoin.text = if (state.isLoading) "참여하는 중..." else "예치하고 참여"
                    binding.btnJoin.isEnabled = !state.isLoading

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }

                    state.insufficient?.let {
                        showInsufficientDialog(it)
                        viewModel.insufficientShown()
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
            showToast("포인트 충전(WL-02)은 준비 중이에요")
        }

        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}