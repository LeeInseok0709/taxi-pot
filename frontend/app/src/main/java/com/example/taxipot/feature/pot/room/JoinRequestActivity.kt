package com.example.taxipot.feature.pot.room

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
import com.example.taxipot.data.remote.dto.pot.JoinRequestResponse
import com.example.taxipot.databinding.ActivityJoinRequestsBinding
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.join.seatLabel
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.dp
import com.example.taxipot.feature.pot.main.roundedBg
import kotlinx.coroutines.launch

/**
 * RD-03 입장 요청 (방장 전용)
 * 참여 마감 뒤 출발지 근처에서 보낸 요청 목록 → 수락(예치 + 참여) / 거절
 * 새 요청·철회는 실시간(POT_UPDATED)으로 자동 반영
 */
class JoinRequestActivity : ComponentActivity() {

    private lateinit var binding: ActivityJoinRequestsBinding
    private val viewModel: JoinRequestViewModel by viewModels()

    companion object {
        private const val EXTRA_POT_ID = "pot_id"

        /** 방 번호를 담아서 입장 요청 화면 열기 */
        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, JoinRequestActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) XML 화면 붙이기
        binding = ActivityJoinRequestsBinding.inflate(layoutInflater)
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

        // 3) 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.infoCard.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)

        // 4) 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 5) 시작
        viewModel.start(intent.getLongExtra(EXTRA_POT_ID, -1L))
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvSubtitle.text = state.subtitle

                    // 요청자 카드 다시 그리기
                    binding.requestsContainer.removeAllViews()
                    state.requests.forEach { request ->
                        binding.requestsContainer.addView(requestCard(request, state.workingUserId))
                    }
                    binding.tvEmpty.visibility =
                        if (state.loaded && state.requests.isEmpty()) View.VISIBLE else View.GONE

                    state.message?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }
                }
            }
        }
    }

    // 요청자 카드: "늦은새 ★5.8" / "희망 좌석 조수석 · 21:26 요청" / [거절] [수락]
    private fun requestCard(request: JoinRequestResponse, workingUserId: Long?): LinearLayout {
        val working = workingUserId == request.memberId
        val busy = workingUserId != null

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
            val pad = dp(14f).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12f).toInt() }
        }

        card.addView(TextView(this).apply {
            text = "${request.nickname} ★${request.ratingAvg}"
            setTextColor(PotColors.TEXT)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = "희망 좌석 ${seatLabel(request.preferredSeat)} · ${clockOf(request.requestedAt)} 요청"
            setTextColor(PotColors.SUB_TEXT)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4f).toInt() }
        })

        // 버튼 줄
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12f).toInt() }
        }
        buttons.addView(actionButton("거절", PotColors.WHITE, true, !busy) { confirmReject(request) })
        buttons.addView(
            actionButton(if (working) "처리 중..." else "수락", PotColors.ACCENT, false, !busy) { confirmAccept(request) }
        )
        card.addView(buttons)
        return card
    }

    private fun actionButton(
        label: String,
        fill: Int,
        first: Boolean,
        enabled: Boolean,
        onClick: () -> Unit
    ): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        setTextColor(PotColors.TEXT)
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        background = roundedBg(fill, 10f, PotColors.LINE)
        alpha = if (enabled) 1f else 0.5f
        isEnabled = enabled
        layoutParams = LinearLayout.LayoutParams(0, dp(44f).toInt(), 1f).apply {
            if (!first) marginStart = dp(8f).toInt()
        }
        setOnClickListener { onClick() }
    }

    private fun confirmAccept(request: JoinRequestResponse) {
        AlertDialog.Builder(this)
            .setTitle("${request.nickname}님을 수락할까요?")
            .setMessage("수락하면 ${request.nickname}님의 예치금이 빠지고 바로 참여(도착 처리)돼요.")
            .setNegativeButton("아니요", null)
            .setPositiveButton("수락") { _, _ -> viewModel.accept(request) }
            .show()
    }

    private fun confirmReject(request: JoinRequestResponse) {
        AlertDialog.Builder(this)
            .setTitle("${request.nickname}님의 요청을 거절할까요?")
            .setMessage("거절하면 ${request.nickname}님은 이 방에 다시 요청할 수 없어요.")
            .setNegativeButton("아니요", null)
            .setPositiveButton("거절") { _, _ -> viewModel.reject(request) }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startRealtime()   // 새 요청·철회가 오면 자동으로 다시 불러오기
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRealtime()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }
}