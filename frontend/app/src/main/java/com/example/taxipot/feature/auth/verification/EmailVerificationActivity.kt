package com.example.taxipot.feature.auth.verification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.databinding.ActivityEmailVerificationBinding
import kotlinx.coroutines.launch

/** AU-02 이메일 인증 (1차 인증) */
class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private val viewModel: EmailVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding.appBar.tvAppBarTitle.text = "이메일 인증"
        binding.appBar.btnBack.setOnClickListener { finish() }

        // 받는 메일 주소 (가입 직후에만 알 수 있음)
        val masked = VerificationFormat.maskEmail(intent.getStringExtra(EXTRA_EMAIL))
        binding.tvEmail.text = masked ?: "가입한 학교 메일"

        binding.etCode.doAfterTextChanged { viewModel.clearError() }
        binding.etCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        binding.btnResend.setOnClickListener { viewModel.resend() }
        binding.btnConfirm.setOnClickListener { submit() }

        viewModel.start(justSent = intent.getBooleanExtra(EXTRA_JUST_SENT, false))
        observeViewModel()
    }

    private fun submit() {
        viewModel.confirm(binding.etCode.text?.toString().orEmpty())
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
            }
        }
    }

    private fun render(state: EmailVerificationUiState) {
        binding.tilCode.error = state.codeError

        // 남은 시간: 재발송 후에만 서버가 만료 시각을 알려줌
        val remaining = state.remainingSeconds
        binding.tvRemaining.text = when {
            remaining == null -> "인증코드는 발송 후 잠시 동안만 유효해요."
            remaining > 0 -> "남은 시간 ${VerificationFormat.formatMmSs(remaining)}"
            else -> "인증코드가 만료됐어요. 재발송을 눌러 주세요."
        }

        binding.btnResend.isEnabled = state.resendCooldown == 0 && !state.isResending
        binding.btnResend.text = when {
            state.isResending -> "보내는 중..."
            state.resendCooldown > 0 -> "인증코드 재발송 (${state.resendCooldown}초 후)"
            else -> "인증코드 재발송"
        }

        binding.btnConfirm.isEnabled = !state.isConfirming
        binding.btnConfirm.text = if (state.isConfirming) "" else "확인"
        binding.progress.visibility = if (state.isConfirming) View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: EmailVerificationEvent) {
        when (event) {
            EmailVerificationEvent.Verified -> {
                showToast("이메일 인증이 완료됐어요.")
                startActivity(CaptureVerificationActivity.newIntent(this))
                finish()
            }
            is EmailVerificationEvent.ShowMessage -> showToast(event.message)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_EMAIL = "email"
        private const val EXTRA_JUST_SENT = "justSent"

        /**
         * @param email 가입한 메일 (화면에 가려서 표시, 모르면 null)
         * @param justSent 가입 직후라 인증코드가 방금 발송됐으면 true
         */
        fun newIntent(context: Context, email: String?, justSent: Boolean): Intent =
            Intent(context, EmailVerificationActivity::class.java)
                .putExtra(EXTRA_EMAIL, email)
                .putExtra(EXTRA_JUST_SENT, justSent)
    }
}
