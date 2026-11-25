package com.example.taxipot.feature.pot.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.databinding.ActivitySeatPickBinding
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg

/**
 * 좌석 선택하기 (방 만들기에서 내 희망 좌석 고르기)
 * 고른 좌석 값(FRONT / BACK_LEFT / BACK_MID / BACK_RIGHT / NONE)을 이전 화면으로 돌려줌
 * 나중에 JN-01 참여하기에서도 다시 쓸 수 있음
 */
class SeatPickActivity : ComponentActivity() {

    private lateinit var binding: ActivitySeatPickBinding

    // 지금 고른 좌석
    private var selectedSeat = "NONE"

    // 좌석 값 ↔ 버튼
    private lateinit var seatViews: Map<String, TextView>

    companion object {
        private const val EXTRA_SEAT = "seat"
        private const val EXTRA_HOLD = "hold"
        private const val EXTRA_BALANCE = "balance"

        /**
         * 좌석 선택 화면 열기
         * seat: 지금 골라 둔 좌석 / hold: 예치액 (모르면 null → 예치액 카드 숨김) / balance: 현재 잔액
         */
        fun newIntent(context: Context, seat: String, hold: Int?, balance: Int): Intent =
            Intent(context, SeatPickActivity::class.java).apply {
                putExtra(EXTRA_SEAT, seat)
                putExtra(EXTRA_HOLD, hold ?: -1)
                putExtra(EXTRA_BALANCE, balance)
            }

        /** 돌려받은 결과에서 고른 좌석 꺼내기 */
        fun seatFrom(data: Intent?): String? = data?.getStringExtra(EXTRA_SEAT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedSeat = intent.getStringExtra(EXTRA_SEAT) ?: "NONE"

        // 1) XML 화면 붙이기
        binding = ActivitySeatPickBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) 상태바·하단바 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 3) 디자인 (자동차는 둥근 큰 상자, 운전석은 회색)
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.carCard.background = roundedBg(PotColors.CARD, 40f, PotColors.LINE)
        binding.seatDriver.background = roundedBg(0xFFEFE9DF.toInt(), 10f, PotColors.LINE)
        binding.moneyCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.btnConfirm.background = roundedBg(PotColors.ACCENT, 12f)

        // 4) 좌석 버튼
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

        // 5) 예치액 카드 (요금을 몰라서 예치액이 없으면 숨김)
        val hold = intent.getIntExtra(EXTRA_HOLD, -1)
        val balance = intent.getIntExtra(EXTRA_BALANCE, 0)
        if (hold < 0) {
            binding.moneyCard.visibility = View.GONE
        } else {
            binding.tvHoldValue.text = "%,dP".format(hold)
            val after = balance - hold
            binding.tvAfterBalance.text = if (after >= 0) {
                "%,dP".format(after)
            } else {
                "잔액 부족 (%,dP 모자람)".format(-after)
            }
        }

        // 6) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_SEAT, selectedSeat))
            finish()
        }

        // 7) 처음 선택 표시
        renderSeats()
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}