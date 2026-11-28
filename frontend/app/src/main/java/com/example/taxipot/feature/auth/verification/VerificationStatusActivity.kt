package com.example.taxipot.feature.auth.verification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.data.remote.dto.verification.VerificationResponse
import com.example.taxipot.databinding.ActivityVerificationStatusBinding
import com.example.taxipot.feature.pot.main.PotMainActivity
import kotlinx.coroutines.launch

/**
 * AU-04 인증 상태.
 * 로그인했는데 학생 인증이 안 끝난 사람도 여기로 와서, 남은 단계(이메일 인증·캡처본 제출)로 이동한다.
 */
class VerificationStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationStatusBinding
    private val viewModel: VerificationStatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVerificationStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding.appBar.tvAppBarTitle.text = "인증 상태"
        binding.appBar.btnBack.setOnClickListener { goHome() }

        binding.btnEmailAction.setOnClickListener {
            startActivity(EmailVerificationActivity.newIntent(this, email = null, justSent = false))
        }
        binding.btnReviewAction.setOnClickListener {
            val reason = viewModel.uiState.value.verification?.rejectReason
            startActivity(CaptureVerificationActivity.newIntent(this, reason))
        }
        binding.tvRefresh.setOnClickListener { viewModel.refresh() }
        binding.btnHome.setOnClickListener { goHome() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 인증 화면에서 돌아오거나 운영자 승인 후 다시 열면 최신 상태로
        viewModel.refresh()
    }

    private fun render(state: VerificationStatusUiState) {
        binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        val v = state.verification
        binding.content.visibility = if (v != null) View.VISIBLE else View.INVISIBLE
        if (v == null) return

        val emailDone = v.isEmailVerified
        val review = v.reviewStatus

        // 제목
        binding.tvTitle.text = when {
            review == VerificationResponse.REVIEW_APPROVED -> "학생 인증이 완료됐어요"
            review == VerificationResponse.REVIEW_REJECTED -> "캡처본이 반려됐어요"
            else -> "학생 인증이 진행 중이에요"
        }

        // 1차: 이메일 인증
        if (emailDone) {
            binding.tvEmailChip.text = "✓ 1차 이메일 인증 완료"
            binding.tvEmailDesc.text = "이메일 확인이 완료되었습니다."
            binding.btnEmailAction.visibility = View.GONE
        } else {
            binding.tvEmailChip.text = "1차 이메일 인증 필요"
            binding.tvEmailDesc.text = "학교 메일로 받은 인증코드를 입력해 주세요."
            binding.btnEmailAction.visibility = View.VISIBLE
        }

        // 2차: 운영자 심사
        when (review) {
            VerificationResponse.REVIEW_APPROVED -> {
                binding.tvReviewChip.text = "✓ 2차 운영자 심사 완료"
                binding.tvReviewDesc.text = "학생 인증이 완료되었습니다. 택시팟을 이용할 수 있어요."
                binding.btnReviewAction.visibility = View.GONE
            }
            VerificationResponse.REVIEW_PENDING -> {
                binding.tvReviewChip.text = "○ 2차 운영자 심사 중"
                binding.tvReviewDesc.text = "운영자 심사 후 푸시 알림으로 결과를 안내합니다."
                binding.btnReviewAction.visibility = View.GONE
            }
            VerificationResponse.REVIEW_REJECTED -> {
                binding.tvReviewChip.text = "2차 심사 반려"
                binding.tvReviewDesc.text = "반려 사유: ${v.rejectReason ?: "사유 없음"}"
                binding.btnReviewAction.text = "반려됨 → 캡처본 다시 제출"
                binding.btnReviewAction.visibility = View.VISIBLE
            }
            else -> {
                binding.tvReviewChip.text = "2차 캡처본 미제출"
                binding.tvReviewDesc.text = "종합정보시스템 학생 기본 정보 캡처본을 제출해 주세요."
                binding.btnReviewAction.text = "캡처본 제출하기"
                // 이메일 인증을 먼저 해야 제출할 수 있음
                binding.btnReviewAction.visibility = if (emailDone) View.VISIBLE else View.GONE
            }
        }

        val approved = review == VerificationResponse.REVIEW_APPROVED
        binding.boxLocked.visibility = if (approved) View.GONE else View.VISIBLE
        binding.btnHome.text = if (approved) "택시팟 시작하기" else "둘러보기 (홈으로)"
    }

    /** 홈으로 이동하면서 인증 화면들은 정리 */
    private fun goHome() {
        startActivity(
            Intent(this, PotMainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    companion object {
        /** 이전 화면들을 정리하고 연다 (뒤로가기로 가입·로그인 화면에 돌아가지 않도록) */
        fun newIntent(context: Context): Intent =
            Intent(context, VerificationStatusActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
