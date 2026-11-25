package com.example.taxipot.feature.pot.join

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotJoinResponse
import com.example.taxipot.databinding.ActivityPotJoinDoneBinding
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.detail.PotDetailActivity
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg

/**
 * JN-02 참여 완료
 * 채팅 바로가기 / 대기실로 이동 (대기실 RD-01이 생기기 전까지는 상세 화면으로)
 */
class PotJoinDoneActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotJoinDoneBinding
    private lateinit var pot: PotDetailResponse

    companion object {
        private const val EXTRA_POT = "pot"
        private const val EXTRA_SEAT = "seat"
        private const val EXTRA_HELD = "held"
        private const val EXTRA_BALANCE_AFTER = "balance_after"

        /** 참여한 방 정보 + 고른 좌석 + 참여 결과를 담아서 열기 */
        fun newIntent(context: Context, pot: PotDetailResponse, seat: String, joined: PotJoinResponse): Intent =
            Intent(context, PotJoinDoneActivity::class.java)
                .putExtra(EXTRA_POT, ApiClient.gson.toJson(pot))
                .putExtra(EXTRA_SEAT, seat)
                .putExtra(EXTRA_HELD, joined.heldAmount)
                .putExtra(EXTRA_BALANCE_AFTER, joined.balanceAfter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 넘겨받은 값 꺼내기
        val json = intent.getStringExtra(EXTRA_POT)
        if (json == null) {
            finish()
            return
        }
        pot = ApiClient.gson.fromJson(json, PotDetailResponse::class.java)
        val seat = intent.getStringExtra(EXTRA_SEAT)
        val held = intent.getIntExtra(EXTRA_HELD, pot.holdAmount)
        val balanceAfter = intent.getIntExtra(EXTRA_BALANCE_AFTER, 0)

        // 2) XML 화면 붙이기
        binding = ActivityPotJoinDoneBinding.inflate(layoutInflater)
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
        binding.tvCheck.background = roundedBg(PotColors.ACCENT, 36f)
        binding.summaryCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.infoCard.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnChat.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnWaitingRoom.background = roundedBg(PotColors.ACCENT, 12f)

        // 5) 참여 정보
        binding.tvRoute.text = "${pot.departureName} → ${pot.destinationName}"
        binding.tvTimeValue.text = clockOf(pot.departureTime)
        binding.tvGatherValue.text = clockOf(pot.arrivalDeadline)
        binding.tvSeatValue.text = seatLabel(seat)
        binding.tvMembersValue.text = "${pot.currentMembers + 1} / ${pot.maxMembers}명"
        binding.tvHoldValue.text = "%,dP (잔액 %,dP)".format(held, balanceAfter)
        binding.tvInfoBody.text =
            "출발 10분 전(${clockOf(pot.cancelDeadline)})부터 취소 불가\n" +
                    "집결 기한까지 출발지 50m 안에서 '도착 확인'을 눌러주세요."

        // 6) 버튼
        binding.btnWaitingRoom.setOnClickListener {
            // 홈만 남기고 (상세·참여 화면은 닫고) 대기실(RD-01) 열기
            startActivity(
                Intent(this, com.example.taxipot.feature.pot.main.PotMainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            startActivity(com.example.taxipot.feature.pot.room.WaitingRoomActivity.newIntent(this, pot.id))
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}