package com.example.taxipot.feature.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.SettlementApi
import com.example.taxipot.data.remote.dto.rating.RatingCreateRequest
import com.example.taxipot.data.remote.dto.rating.RatingItem
import com.example.taxipot.data.remote.dto.rating.RatingTargetResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 평가 항목 4개 (화면 위에서 아래 순서) */
enum class RatingCategory(val label: String) {
    NOSHOW("시간 약속"),
    MANNER("언행 · 태도"),
    COMMUNICATION("연락"),
    PAYMENT("정산")
}

data class RatingUiState(
    val isLoading: Boolean = false,
    val targets: List<RatingTargetResponse> = emptyList(),
    val selectedId: Long? = null,
    /** 동승자별 항목 점수 (1~5) */
    val scores: Map<Long, Map<RatingCategory, Int>> = emptyMap(),
    val reasonError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val selected: RatingTargetResponse? get() = targets.firstOrNull { it.memberId == selectedId }
    fun scoresOf(memberId: Long): Map<RatingCategory, Int> =
        scores[memberId] ?: RatingCategory.entries.associateWith { DEFAULT_SCORE }

    /** 4항목 평균이 3점 미만이면 사유 필수 */
    fun needsReason(memberId: Long): Boolean = scoresOf(memberId).values.average() < 3.0

    companion object {
        const val DEFAULT_SCORE = 5
    }
}

sealed interface RatingEvent {
    data class ShowMessage(val message: String) : RatingEvent
    /** 모든 동승자 평가 끝 */
    data object AllDone : RatingEvent
}

class RatingViewModel : ViewModel() {

    private val api = ApiClient.create(SettlementApi::class.java)

    private val _uiState = MutableStateFlow(RatingUiState())
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()

    private val _events = Channel<RatingEvent>(Channel.BUFFERED)
    val events: Flow<RatingEvent> = _events.receiveAsFlow()

    private var potId = -1L

    fun load(potId: Long) {
        this.potId = potId
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = safeApiCall { api.getRatingTargets(potId) }) {
                is ApiResult.Success -> _uiState.update { state ->
                    val targets = result.data
                    state.copy(
                        isLoading = false,
                        targets = targets,
                        // 아직 평가 안 한 첫 사람부터
                        selectedId = state.selectedId ?: (targets.firstOrNull { !it.isRated } ?: targets.firstOrNull())?.memberId
                    )
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = failureMessage(result.code, result.message))
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = NETWORK_ERROR_MESSAGE)
                }
            }
        }
    }

    fun select(memberId: Long) = _uiState.update { it.copy(selectedId = memberId, reasonError = null) }

    fun setScore(category: RatingCategory, score: Int) {
        val id = _uiState.value.selectedId ?: return
        _uiState.update { state ->
            val updated = state.scoresOf(id) + (category to score.coerceIn(1, 5))
            state.copy(scores = state.scores + (id to updated), reasonError = null)
        }
    }

    fun submit(rawReason: String) {
        val state = _uiState.value
        val target = state.selected ?: return
        if (state.isSubmitting || target.isRated) return

        val reason = rawReason.trim()
        if (state.needsReason(target.memberId) && reason.isEmpty()) {
            _uiState.update { it.copy(reasonError = "평균 3점 미만이면 사유를 입력해 주세요.") }
            return
        }

        val scores = state.scoresOf(target.memberId)
        val item = RatingItem(
            revieweeId = target.memberId,
            noshowScore = scores.getValue(RatingCategory.NOSHOW),
            mannerScore = scores.getValue(RatingCategory.MANNER),
            communicationScore = scores.getValue(RatingCategory.COMMUNICATION),
            paymentScore = scores.getValue(RatingCategory.PAYMENT),
            badReason = reason.ifEmpty { null }
        )

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = safeApiCall { api.submitRatings(potId, RatingCreateRequest(listOf(item))) }
            _uiState.update { it.copy(isSubmitting = false) }
            when (result) {
                is ApiResult.Success -> markRated(target, "${target.nickname ?: "동승자"}님 평가를 보냈어요.")
                is ApiResult.Failure -> when (result.code) {
                    "ALREADY_RATED" -> markRated(target, "이미 평가한 동승자예요.")
                    "BAD_REASON_REQUIRED" -> _uiState.update {
                        it.copy(reasonError = "평균 3점 미만이면 사유를 입력해 주세요.")
                    }
                    else -> _events.send(RatingEvent.ShowMessage(failureMessage(result.code, result.message)))
                }
                is ApiResult.NetworkError -> _events.send(RatingEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    /** 평가 완료 표시 → 다음 사람으로, 다 끝났으면 알림 */
    private suspend fun markRated(target: RatingTargetResponse, message: String) {
        val targets = _uiState.value.targets.map {
            if (it.memberId == target.memberId) it.copy(isRated = true) else it
        }
        val next = targets.firstOrNull { !it.isRated }
        _uiState.update { it.copy(targets = targets, selectedId = next?.memberId ?: target.memberId) }
        _events.send(RatingEvent.ShowMessage(message))
        if (next == null) _events.send(RatingEvent.AllDone)
    }

    private fun failureMessage(code: String, fallback: String): String = when (code) {
        "INVALID_POT_STATUS" -> "정산이 끝난 뒤에 평가할 수 있어요."
        "RATING_PERIOD_EXPIRED" -> "평가 기간(정산 후 24시간)이 지났어요."
        "NOT_POT_MEMBER" -> "함께 탑승한 동승자만 평가할 수 있어요."
        "SELF_RATING_NOT_ALLOWED" -> "나 자신은 평가할 수 없어요."
        else -> fallback
    }

    companion object {
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
    }
}
