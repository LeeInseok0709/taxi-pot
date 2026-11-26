package com.example.taxipot.feature.account

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.user.PasswordChangeRequest
import com.example.taxipot.databinding.ActivityPasswordChangeBinding
import kotlinx.coroutines.launch

/**
 * 비밀번호 변경 (Figma엔 AC-01의 메뉴만 있음).
 * 성공하면 다른 기기의 로그인은 끊기고, 이 기기는 서버가 준 새 토큰으로 계속 로그인 상태를 유지한다.
 */
class PasswordChangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordChangeBinding
    private val api by lazy { ApiClient.create(UserApi::class.java) }
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPasswordChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding.appBar.tvAppBarTitle.text = "비밀번호 변경"
        binding.appBar.btnBack.setOnClickListener { finish() }
        binding.etCurrent.doAfterTextChanged { binding.tilCurrent.error = null }
        binding.etNew.doAfterTextChanged { binding.tilNew.error = null }
        binding.etConfirm.doAfterTextChanged { binding.tilConfirm.error = null }
        binding.btnSave.setOnClickListener { submit() }
    }

    private fun submit() {
        if (saving) return
        val current = binding.etCurrent.text?.toString().orEmpty()
        val new = binding.etNew.text?.toString().orEmpty()
        val confirm = binding.etConfirm.text?.toString().orEmpty()

        var ok = true
        if (current.isEmpty()) { binding.tilCurrent.error = "현재 비밀번호를 입력해 주세요."; ok = false }
        if (new.length < 8) { binding.tilNew.error = "새 비밀번호는 8자 이상이어야 해요."; ok = false }
        else if (new == current) { binding.tilNew.error = "지금 비밀번호와 다르게 정해 주세요."; ok = false }
        if (confirm != new) { binding.tilConfirm.error = "새 비밀번호가 일치하지 않아요."; ok = false }
        if (!ok) return

        setSaving(true)
        lifecycleScope.launch {
            val result = safeApiCall { api.changePassword(PasswordChangeRequest(current, new)) }
            setSaving(false)
            when (result) {
                is ApiResult.Success -> {
                    // 기존 토큰은 무효가 되므로 새 토큰으로 바꿔 둔다
                    ApiClient.tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                    toast("비밀번호를 바꿨어요. 다른 기기에서는 다시 로그인해야 해요.")
                    finish()
                }
                is ApiResult.Failure -> if (result.code == "INVALID_CREDENTIALS") {
                    binding.tilCurrent.error = "현재 비밀번호가 맞지 않아요."
                } else {
                    toast(result.message)
                }
                is ApiResult.NetworkError -> toast(ProfileEditViewModel.NETWORK_ERROR_MESSAGE)
            }
        }
    }

    private fun setSaving(value: Boolean) {
        saving = value
        binding.btnSave.isEnabled = !value
        binding.btnSave.text = if (value) "바꾸는 중..." else "비밀번호 변경"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
