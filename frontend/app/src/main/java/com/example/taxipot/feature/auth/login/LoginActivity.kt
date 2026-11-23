package com.example.taxipot.feature.auth.login

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
import com.example.taxipot.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바·하단바 여백, 밝은 배경이라 상태바 글자는 어둡게
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

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { submit() }

        // 키보드의 완료 버튼으로도 로그인
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        binding.etEmail.doAfterTextChanged { viewModel.clearEmailError() }
        binding.etPassword.doAfterTextChanged { viewModel.clearPasswordError() }

        // TODO: 회원가입 화면 만들면 연결
        binding.tvSignup.setOnClickListener { showToast("회원가입 (준비 중)") }
    }

    private fun submit() {
        viewModel.login(
            email = binding.etEmail.text?.toString().orEmpty(),
            password = binding.etPassword.text?.toString().orEmpty()
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
            }
        }
    }

    private fun render(state: LoginUiState) {
        binding.tilEmail.error = state.emailError
        binding.tilPassword.error = state.passwordError

        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        // 로그인 중에는 버튼 글자 대신 로딩 표시, 중복 클릭 방지
        binding.btnLogin.isEnabled = !state.isLoading
        binding.btnLogin.text = if (state.isLoading) "" else "로그인"
        binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.etEmail.isEnabled = !state.isLoading
        binding.etPassword.isEnabled = !state.isLoading
    }

    private fun handleEvent(event: LoginEvent) {
        when (event) {
            // TODO: PR #21 머지 후 PotMainActivity로 이동 + finish()
            LoginEvent.GoHome -> showToast("로그인 성공 (홈 화면 연결 예정)")
            // TODO: 학생 인증 화면 만들면 연결
            LoginEvent.GoVerification -> showToast("로그인 성공 · 학생 인증이 필요해요 (인증 화면 준비 중)")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
