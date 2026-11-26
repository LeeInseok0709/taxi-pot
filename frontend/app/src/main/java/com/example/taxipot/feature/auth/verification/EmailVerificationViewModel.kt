package com.example.taxipot.feature.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.VerificationApi
import com.example.taxipot.data.remote.dto.verification.EmailCodeConfirmRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EmailVerificationUiState(
    /** 인증코드 남은 유효 시간(초). 모르면 null (가입 직후에는 서버가 만료 시각을 주지 않음) */
    val remainingSeconds: Int? = null,
    /** 재발송까지 남은 시간(초). 0이면 바로 재발송 가능 */
    val resendCooldown: Int = 0,
    val codeError: String? = null,
    val isConfirming: Boolean = false,
    val isResending: Boolean = false
)

sealed interface EmailVerificationEvent {
    /** 이메일 인증 완료 → 학생 정보 인증(AU-03)으로 */
    data object Verified : EmailVerificationEvent
    data class ShowMessage(val message: String) : EmailVerificationEvent
}

class EmailVerificationViewModel : ViewModel() {

    private val api = ApiClient.create(VerificationApi::class.java)

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val _events = Channel<EmailVerificationEvent>(Channel.BUFFERED)
    val events: Flow<EmailVerificationEvent> = _events.receiveAsFlow()

    private var started = false
    private var expiresAtMillis: Long? = null
    private var cooldownUntilMillis = 0L

    /** 화면이 처음 열릴 때 1회. 가입 직후면 코드가 방금 발송됐으니 재발송 대기부터 시작 */
    fun start(justSent: Boolean) {
        if (started) return
        started = true
        if (justSent) cooldownUntilMillis = System.currentTimeMillis() + RESEND_INTERVAL_MS
        viewModelScope.launch {
            while (isActive) {
                updateTimers()
                delay(1_000)
            }
        }
    }

    private fun updateTimers() {
        val now = System.currentTimeMillis()
        val remaining = expiresAtMillis?.let { ((it - now) / 1_000).toInt().coerceAtLeast(0) }
        val cooldown = ((cooldownUntilMillis - now + 999) / 1_000).toInt().coerceAtLeast(0)
        _uiState.update { it.copy(remainingSeconds = remaining, resendCooldown = cooldown) }
    }

    fun clearError() = _uiState.update { if (it.codeError != null) it.copy(codeError = null) else it }

    fun confirm(rawCode: String) {
        val state = _uiState.value
        if (state.isConfirming) return

        val code = rawCode.trim()
        if (code.isEmpty() || !code.all { it.isDigit() }) {
            _uiState.update { it.copy(codeError = "메일로 받은 인증코드(숫자)를 입력해 주세요.") }
            return
        }
        if (state.remainingSeconds == 0) {
            _uiState.update { it.copy(codeError = "인증코드가 만료됐어요. 재발송을 눌러 새 코드를 받아 주세요.") }
            return
        }

        _uiState.update { it.copy(isConfirming = true, codeError = null) }
        viewModelScope.launch {
            val result = safeApiCall { api.confirmEmailCode(EmailCodeConfirmRequest(code)) }
            _uiState.update { it.copy(isConfirming = false) }
            when (result) {
                is ApiResult.Success -> {
                    if (result.data.isEmailVerified) {
                        _events.send(EmailVerificationEvent.Verified)
                    } else {
                        _events.send(EmailVerificationEvent.ShowMessage("인증을 완료하지 못했어요. 다시 시도해 주세요."))
                    }
                }

                is ApiResult.Failure -> when (result.code) {
                    "ALREADY_VERIFIED" -> _events.send(EmailVerificationEvent.Verified)
                    // 서버 문구에 남은 입력 횟수가 들어 있음
                    "INVALID_EMAIL_CODE" -> _uiState.update { it.copy(codeError = result.message) }
                    "EXPIRED_EMAIL_CODE" -> _uiState.update {
                        it.copy(codeError = "인증코드가 만료됐어요. 재발송을 눌러 새 코드를 받아 주세요.")
                    }
                    "EMAIL_CODE_ATTEMPTS_EXCEEDED" -> _uiState.update {
                        it.copy(codeError = "입력 횟수를 초과했어요. 재발송을 눌러 새 코드를 받아 주세요.")
                    }
                    else -> _events.send(EmailVerificationEvent.ShowMessage(result.message))
                }

                is ApiResult.NetworkError ->
                    _events.send(EmailVerificationEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    fun resend() {
        val state = _uiState.value
        if (state.isResending || state.resendCooldown > 0) return

        _uiState.update { it.copy(isResending = true) }
        viewModelScope.launch {
            val result = safeApiCall { api.resendEmailCode() }
            _uiState.update { it.copy(isResending = false) }
            when (result) {
                is ApiResult.Success -> {
                    expiresAtMillis = VerificationFormat.parseIsoMillis(result.data.expiresAt)
                    cooldownUntilMillis = System.currentTimeMillis() + RESEND_INTERVAL_MS
                    _uiState.update { it.copy(codeError = null) }
                    updateTimers()
                    _events.send(EmailVerificationEvent.ShowMessage("인증코드를 다시 보냈어요."))
                }

                is ApiResult.Failure -> when (result.code) {
                    "ALREADY_VERIFIED" -> _events.send(EmailVerificationEvent.Verified)
                    // 서버 문구: "인증코드는 60초 후에 다시 받을 수 있습니다."
                    else -> _events.send(EmailVerificationEvent.ShowMessage(result.message))
                }

                is ApiResult.NetworkError ->
                    _events.send(EmailVerificationEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    companion object {
        /** 서버 재발송 간격(app.verification.resend-interval-seconds)과 같은 값 */
        private const val RESEND_INTERVAL_MS = 60_000L
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
    }
}
