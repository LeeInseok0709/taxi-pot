package com.example.taxipot.feature.pot.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 상세 화면에 그릴 상태 */
data class PotDetailUiState(
    val isLoading: Boolean = false,
    val pot: PotDetailResponse? = null,
    /** 처음 불러오기 실패 문장 (보여주고 화면 닫기) */
    val errorMessage: String? = null,
    /** 한 번 보여주고 지울 문장 (요청 취소·수락·거절 안내) */
    val message: String? = null,
    /** 입장 요청이 수락됨 → 대기실로 이동 */
    val goWaitingRoom: Boolean = false,
    /** 요청 취소 중 */
    val actionLoading: Boolean = false
)

/** 택시팟 상세 (EX-04) + 입장 요청 상태 확인 (RD-03) */
class PotDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PotDetailUiState())
    val uiState: StateFlow<PotDetailUiState> = _uiState.asStateFlow()

    private var potId = -1L

    /** 처음 불러오기 (실패하면 화면 닫기) */
    fun load(potId: Long) {
        this.potId = potId
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = PotRepository.getPot(potId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, pot = result.data)
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /**
     * 조용히 다시 불러오기 (화면이 다시 보일 때, 입장 요청 대기 중 5초마다)
     * 입장 요청 대기(REQUESTED) → 수락·거절·취소로 바뀌었으면 안내
     */
    fun refresh() {
        if (potId < 0 || _uiState.value.pot == null) return

        viewModelScope.launch {
            val result = PotRepository.getPot(potId)
            if (result !is ApiResult.Success) return@launch   // 다시 불러오기 실패는 조용히 넘어감

            val before = _uiState.value.pot?.myMemberStatus
            val after = result.data
            _uiState.update { it.copy(pot = after) }

            if (before == "REQUESTED" && after.myMemberStatus != "REQUESTED") {
                when {
                    after.isJoined -> _uiState.update {
                        it.copy(message = "입장 요청이 수락됐어요! 대기실로 이동해요", goWaitingRoom = true)
                    }
                    after.myMemberStatus == "REJECTED" -> _uiState.update {
                        it.copy(message = "방장이 입장 요청을 거절했어요")
                    }
                    else -> _uiState.update {
                        it.copy(message = "자리가 마감돼 입장 요청이 취소됐어요")
                    }
                }
            }
        }
    }

    /** 내 입장 요청 취소 */
    fun withdraw() {
        if (_uiState.value.actionLoading || potId < 0) return
        _uiState.update { it.copy(actionLoading = true) }

        viewModelScope.launch {
            val result = PotRepository.withdrawJoinRequest(potId)
            if (result is ApiResult.Success) {
                // 내가 취소한 것이므로 "수락·거절" 안내가 뜨지 않게 상태를 먼저 바꿔 둠
                _uiState.update {
                    it.copy(
                        actionLoading = false,
                        message = "입장 요청을 취소했어요",
                        pot = it.pot?.copy(myMemberStatus = "CANCELED")
                    )
                }
                refresh()
            } else {
                _uiState.update { it.copy(actionLoading = false, message = PotRepository.errorMessage(result)) }
                refresh()
            }
        }
    }

    /** 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(message = null) }

    /** 대기실로 이동한 뒤 지우기 */
    fun waitingRoomOpened() = _uiState.update { it.copy(goWaitingRoom = false) }
}