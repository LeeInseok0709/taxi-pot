package com.example.taxipot.feature.pot.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.PotArrivalResponse
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.feature.pot.chat.PotRealtime
import com.example.taxipot.feature.pot.data.MyInfo
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 대기실 화면에 그릴 상태 */
data class WaitingRoomUiState(
    val pot: PotDetailResponse? = null,
    /** 이 방에서 "나"의 회원 번호 */
    val myUserId: Long? = null,
    /** 버튼 작업(도착 확인·탑승 시작 등) 중인지 */
    val actionLoading: Boolean = false,
    /** 한 번 보여주고 지울 문장 */
    val message: String? = null,
    /** 방 취소·참여 취소가 끝나서 화면을 닫아야 하는지 */
    val closed: Boolean = false
)

/** RD-01 대기실 */
class WaitingRoomViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WaitingRoomUiState())
    val uiState: StateFlow<WaitingRoomUiState> = _uiState.asStateFlow()

    private var potId = -1L

    // 실시간 연결 (POT_UPDATED 오면 방 정보 다시 불러오기)
    private var realtime: PotRealtime? = null

    /** 어느 방의 대기실인지 정하기 */
    fun setPot(potId: Long) {
        this.potId = potId
    }

    /** 방 정보 다시 불러오기 (화면이 다시 보일 때, 버튼 작업 뒤) */
    fun refresh() {
        if (potId < 0) return
        viewModelScope.launch {
            when (val result = PotRepository.getPot(potId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(pot = result.data, myUserId = MyInfo.myUserId(result.data))
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(message = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /** 도착 확인 */
    fun arrive(lat: Double, lng: Double) = runAction(
        call = { PotRepository.arrive(potId, lat, lng, _uiState.value.myUserId) }
    ) { data ->
        val arrival = data as PotArrivalResponse
        if (arrival.allArrived) {
            "도착 확인 완료! 전원 도착했어요"
        } else {
            "도착 확인 완료! (${arrival.arrivedCount} / ${arrival.totalMembers}명 도착)"
        }
    }

    /** 탑승 시작 (방장) */
    fun start() = runAction(call = { PotRepository.start(potId) }) {
        "탑승을 시작했어요! 택시를 불러 주세요"
    }

    /** 미도착자 내보내기 (방장) */
    fun kick(userId: Long, nickname: String) = runAction(call = { PotRepository.kick(potId, userId) }) {
        "${nickname}님을 내보냈어요"
    }

    /** 방 취소 (방장) */
    fun cancelPot() = runAction(closeOnSuccess = true, call = { PotRepository.cancelPot(potId) }) {
        "방을 취소했어요. 예치금은 모두 환불돼요"
    }

    /** 참여 취소 (동승자) */
    fun leave() = runAction(closeOnSuccess = true, call = { PotRepository.leave(potId, _uiState.value.myUserId) }) {
        "참여를 취소했어요. 예치금은 환불돼요"
    }

    /** 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(message = null) }

    /** 화면이 보일 때: 실시간 연결 → 인원·도착·상태가 바뀌면 자동 새로고침 (가짜 데이터일 때는 연결 안 함) */
    fun startRealtime() {
        if (PotRepository.USE_FAKE || potId < 0) return
        if (realtime == null) {
            realtime = PotRealtime(
                potId = potId,
                onChat = { },                 // 대기실에서는 채팅을 그리지 않음
                onPotUpdated = { refresh() }
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

    // 버튼 작업 공통: 한 번에 하나만, 성공하면 문장 + 새로고침(또는 화면 닫기), 실패하면 이유
    private fun runAction(
        closeOnSuccess: Boolean = false,
        call: suspend () -> ApiResult<*>,
        successMessage: (Any?) -> String
    ) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(actionLoading = true) }

        viewModelScope.launch {
            val result = call()
            if (result is ApiResult.Success) {
                _uiState.update {
                    it.copy(actionLoading = false, message = successMessage(result.data), closed = closeOnSuccess)
                }
                if (!closeOnSuccess) refresh()
            } else {
                _uiState.update { it.copy(actionLoading = false, message = PotRepository.errorMessage(result)) }
            }
        }
    }
}