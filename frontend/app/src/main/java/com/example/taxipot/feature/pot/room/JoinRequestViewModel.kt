package com.example.taxipot.feature.pot.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.JoinRequestResponse
import com.example.taxipot.feature.pot.chat.PotRealtime
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 입장 요청 화면에 그릴 상태 */
data class JoinRequestUiState(
    /** "백석대 → 천안역 · 빈자리 1석" */
    val subtitle: String = "",
    /** 대기 중인 요청 (요청 시각 순) */
    val requests: List<JoinRequestResponse> = emptyList(),
    /** 처음 불러오기가 끝났는지 (끝나기 전에는 "요청 없음"을 안 보여줌) */
    val loaded: Boolean = false,
    /** 수락·거절 처리 중인 요청자 번호 */
    val workingUserId: Long? = null,
    /** 한 번 보여주고 지울 문장 */
    val message: String? = null
)

/** RD-03 입장 요청 (방장): 대기 목록 → 수락 / 거절 */
class JoinRequestViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(JoinRequestUiState())
    val uiState: StateFlow<JoinRequestUiState> = _uiState.asStateFlow()

    private var potId = -1L

    // 실시간 연결 (새 요청·요청 철회가 오면 POT_UPDATED → 다시 불러오기)
    private var realtime: PotRealtime? = null

    fun start(potId: Long) {
        if (this.potId == potId) return
        this.potId = potId
        reload()
    }

    /** 방 정보(빈자리) + 요청 목록 다시 불러오기 */
    fun reload() {
        if (potId < 0) return
        viewModelScope.launch {
            when (val potResult = PotRepository.getPot(potId)) {
                is ApiResult.Success -> {
                    val pot = potResult.data
                    val vacancy = (pot.maxMembers - pot.currentMembers).coerceAtLeast(0)
                    _uiState.update {
                        it.copy(subtitle = "${pot.departureName.replace("대학교", "대")} → ${pot.destinationName} · 빈자리 ${vacancy}석")
                    }
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> Unit   // 제목만 못 바꿈, 목록은 계속
            }

            when (val result = PotRepository.getJoinRequests(potId)) {
                is ApiResult.Success -> _uiState.update { it.copy(requests = result.data, loaded = true) }
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(loaded = true, message = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /** 수락: 요청자 예치 + 참여(도착 처리) */
    fun accept(request: JoinRequestResponse) {
        if (_uiState.value.workingUserId != null) return
        _uiState.update { it.copy(workingUserId = request.memberId) }

        viewModelScope.launch {
            val result = PotRepository.acceptJoinRequest(potId, request.memberId)
            val message = if (result is ApiResult.Success) {
                if (result.data.potStatus == "CLOSED") {
                    "${request.nickname}님이 참여했어요. 인원이 다 찼어요!"
                } else {
                    "${request.nickname}님이 참여했어요"
                }
            } else {
                acceptErrorMessage(result, request.nickname)
            }
            _uiState.update { it.copy(workingUserId = null, message = message) }
            reload()
        }
    }

    /** 거절: 그 사람은 이 방에 다시 요청할 수 없음 */
    fun reject(request: JoinRequestResponse) {
        if (_uiState.value.workingUserId != null) return
        _uiState.update { it.copy(workingUserId = request.memberId) }

        viewModelScope.launch {
            val result = PotRepository.rejectJoinRequest(potId, request.memberId)
            val message = if (result is ApiResult.Success) {
                "${request.nickname}님의 요청을 거절했어요"
            } else {
                PotRepository.errorMessage(result)
            }
            _uiState.update { it.copy(workingUserId = null, message = message) }
            reload()
        }
    }

    // 수락 실패 문장: 잔액·다른 방 문제는 "요청자"의 문제라서 따로 안내
    private fun acceptErrorMessage(result: ApiResult<*>, nickname: String): String {
        if (result is ApiResult.Failure) {
            when (result.code) {
                "INSUFFICIENT_BALANCE" -> return "${nickname}님의 잔액이 부족해 수락할 수 없어요. 거절할 수 있어요."
                "ACTIVE_POT_EXISTS" -> return "${nickname}님이 이미 다른 방에 참여 중이에요."
                "UNPAID_DEBT_EXISTS" -> return "${nickname}님에게 미납금이 있어 수락할 수 없어요."
                "SUSPENDED_USER" -> return "${nickname}님은 이용 정지 중이라 수락할 수 없어요."
                "JOIN_REQUEST_NOT_FOUND" -> return "이미 취소된 요청이에요."
            }
        }
        return PotRepository.errorMessage(result)
    }

    /** 화면이 보일 때: 실시간 연결 (가짜 데이터일 때는 연결 안 함) */
    fun startRealtime() {
        if (PotRepository.USE_FAKE || potId < 0) return
        if (realtime == null) {
            realtime = PotRealtime(
                potId = potId,
                onChat = { },
                onPotUpdated = { reload() }
            )
        }
        realtime?.connect()
    }

    /** 화면이 안 보일 때: 실시간 끊기 */
    fun stopRealtime() {
        realtime?.disconnect()
    }

    override fun onCleared() {
        realtime?.disconnect()
    }

    /** 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(message = null) }
}