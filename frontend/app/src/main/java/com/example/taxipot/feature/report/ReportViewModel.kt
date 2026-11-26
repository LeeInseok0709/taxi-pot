package com.example.taxipot.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.SettlementApi
import com.example.taxipot.data.remote.dto.report.ReportCreateRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 신고 유형 (서버 ReportType과 같은 값) */
enum class ReportType(val code: String, val label: String) {
    NO_SHOW("NO_SHOW", "노쇼"),
    ABUSIVE("ABUSIVE", "불쾌한 언행"),
    ETC("ETC", "기타")
}

data class ReportUiState(
    val type: ReportType? = null,
    val typeError: String? = null,
    val reasonError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ReportEvent {
    data object Done : ReportEvent
}

class ReportViewModel : ViewModel() {

    private val api = ApiClient.create(SettlementApi::class.java)

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReportEvent>(Channel.BUFFERED)
    val events: Flow<ReportEvent> = _events.receiveAsFlow()

    fun selectType(type: ReportType) = _uiState.update { it.copy(type = type, typeError = null, errorMessage = null) }

    fun clearReasonError() = _uiState.update { if (it.reasonError != null) it.copy(reasonError = null) else it }

    fun submit(potId: Long, reportedId: Long, rawReason: String) {
        val state = _uiState.value
        if (state.isSubmitting) return

        val reason = rawReason.trim()
        val typeError = if (state.type == null) "신고 유형을 골라 주세요." else null
        val reasonError = when {
            reason.isEmpty() -> "신고 사유를 입력해 주세요."
            reason.length > MAX_REASON -> "신고 사유는 ${MAX_REASON}자 이하로 입력해 주세요."
            else -> null
        }
        val type = state.type
        if (type == null || typeError != null || reasonError != null) {
            _uiState.update { it.copy(typeError = typeError, reasonError = reasonError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = safeApiCall {
                api.report(ReportCreateRequest(reportedId = reportedId, potId = potId, type = type.code, reason = reason))
            }
            _uiState.update { it.copy(isSubmitting = false) }
            when (result) {
                is ApiResult.Success -> _events.send(ReportEvent.Done)
                is ApiResult.Failure -> _uiState.update {
                    it.copy(
                        errorMessage = when (result.code) {
                            "DUPLICATE_REPORT" -> "이 택시팟에서 이미 신고한 동승자예요."
                            "REPORT_NOT_ELIGIBLE" -> "끝까지 함께 탑승했거나 노쇼를 당한 경우에만 신고할 수 있어요."
                            "SELF_REPORT_NOT_ALLOWED" -> "나 자신은 신고할 수 없어요."
                            // 노쇼한 사람에게는 노쇼 유형만 가능
                            else -> result.message
                        }
                    )
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요.")
                }
            }
        }
    }

    companion object {
        const val MAX_REASON = 255
    }
}
