package com.example.taxipot.feature.auth.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.AuthApi
import com.example.taxipot.data.remote.dto.auth.LoginRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 화면에 그릴 상태 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    /** 입력칸이 아닌 화면 아래에 보여줄 에러 (로그인 실패, 연결 실패 등) */
    val errorMessage: String? = null
)

/** 한 번만 처리할 이동 이벤트 */
sealed interface LoginEvent {
    data object GoHome : LoginEvent
    data object GoVerification : LoginEvent
}

class LoginViewModel : ViewModel() {

    private val authApi = ApiClient.create(AuthApi::class.java)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return

        val trimmedEmail = email.trim()
        val emailError = when {
            trimmedEmail.isEmpty() -> "이메일을 입력해 주세요."
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> "이메일 형식이 올바르지 않습니다."
            else -> null
        }
        val passwordError = if (password.isEmpty()) "비밀번호를 입력해 주세요." else null

        if (emailError != null || passwordError != null) {
            _uiState.value = LoginUiState(emailError = emailError, passwordError = passwordError)
            return
        }

        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            when (val result = safeApiCall { authApi.login(LoginRequest(trimmedEmail, password)) }) {
                is ApiResult.Success -> {
                    val body = result.data
                    ApiClient.tokenManager.saveTokens(body.accessToken, body.refreshToken)
                    _uiState.update { it.copy(isLoading = false) }
                    // 학생 인증 전이면 인증 화면, 아니면 홈
                    _events.send(
                        if (body.user.isVerified == true) LoginEvent.GoHome
                        else LoginEvent.GoVerification
                    )
                }

                is ApiResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = failureMessage(result))
                }

                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
                    )
                }
            }
        }
    }

    /** 입력을 고치기 시작하면 해당 에러를 지운다 */
    fun clearEmailError() = _uiState.update { it.copy(emailError = null, errorMessage = null) }
    fun clearPasswordError() = _uiState.update { it.copy(passwordError = null, errorMessage = null) }

    private fun failureMessage(failure: ApiResult.Failure): String = when (failure.code) {
        "INVALID_CREDENTIALS" -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "WITHDRAWN_USER" -> "탈퇴한 계정입니다."
        "INVALID_INPUT" -> "입력한 내용을 다시 확인해 주세요."
        else -> failure.message
    }
}
