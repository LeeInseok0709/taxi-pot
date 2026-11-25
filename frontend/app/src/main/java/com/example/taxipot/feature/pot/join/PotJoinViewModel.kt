package com.example.taxipot.feature.pot.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.JoinRequestResponse
import com.example.taxipot.data.remote.dto.pot.PotJoinResponse
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 좌석 값 → 화면 글자 (참여하기·참여 완료 화면에서 같이 사용) */
fun seatLabel(code: String?): String = when (code) {
    "FRONT" -> "조수석"
    "BACK_LEFT" -> "뒤 왼쪽"
    "BACK_MID" -> "뒤 가운데"
    "BACK_RIGHT" -> "뒤 오른쪽"
    else -> "상관없음"
}

/** 잔액 부족 정보 (JN-03 안내 창에 보여줄 값) */
data class InsufficientInfo(
    val required: Int,   // 필요 예치액
    val balance: Int,    // 현재 잔액
    val shortage: Int    // 부족 금액
)

/** 참여하기 화면에 그릴 상태 */
data class PotJoinUiState(
    val isLoading: Boolean = false,
    /** 참여 성공 결과 */
    val joined: PotJoinResponse? = null,
    /** 입장 요청 성공 결과 (RD-03) */
    val requested: JoinRequestResponse? = null,
    /** 잔액 부족이면 값이 들어옴 → JN-03 창 */
    val insufficient: InsufficientInfo? = null,
    /** 그 외 실패 문장 */
    val errorMessage: String? = null,
    /** 이미 다른 방에 참여 중이면 그 방 번호 (안내 창에서 그 방으로 이동) */
    val activePotId: Long? = null,
    /** 서버 판단으로 화면 바꾸기: true = 입장 요청으로, false = 일반 참여로 (null = 그대로) */
    val switchToRequest: Boolean? = null
)

/** 택시팟 참여 (JN-01 → POST /members) + 입장 요청 (RD-03 → POST /join-requests) */
class PotJoinViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PotJoinUiState())
    val uiState: StateFlow<PotJoinUiState> = _uiState.asStateFlow()

    /**
     * 참여하기
     * holdAmount, balance: 서버가 잔액 부족 detail을 안 줄 때 대신 보여줄 값
     */
    fun join(potId: Long, seat: String, holdAmount: Int, balance: Int) {
        if (!startLoading()) return

        viewModelScope.launch {
            when (val result = PotRepository.join(potId, seat)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, joined = result.data) }
                else -> handleFailure(result, holdAmount, balance)
            }
        }
    }

    /** 입장 요청 보내기 (참여 마감 후, 지금 내 위치와 함께) */
    fun requestJoin(potId: Long, seat: String, lat: Double, lng: Double, holdAmount: Int, balance: Int) {
        if (!startLoading()) return

        viewModelScope.launch {
            when (val result = PotRepository.requestJoin(potId, seat, lat, lng)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, requested = result.data) }
                else -> handleFailure(result, holdAmount, balance)
            }
        }
    }

    // 여러 번 눌러도 한 번만 (이미 보내는 중이면 false)
    private fun startLoading(): Boolean {
        if (_uiState.value.isLoading) return false
        _uiState.update {
            it.copy(isLoading = true, errorMessage = null, insufficient = null, activePotId = null, switchToRequest = null)
        }
        return true
    }

    // 실패 공통 처리: 잔액 부족 창 / 참여 중인 방 안내 / 참여↔입장 요청 전환 / 문장
    private fun handleFailure(result: ApiResult<*>, holdAmount: Int, balance: Int) {
        if (result is ApiResult.Failure) {
            when (result.code) {
                "INSUFFICIENT_BALANCE" -> {
                    // 서버 detail: requiredAmount, balance, shortageAmount (숫자는 Double로 옴)
                    val detail = result.detail
                    val required = (detail?.get("requiredAmount") as? Number)?.toInt() ?: holdAmount
                    val myBalance = (detail?.get("balance") as? Number)?.toInt() ?: balance
                    val shortage = (detail?.get("shortageAmount") as? Number)?.toInt()
                        ?: (required - myBalance).coerceAtLeast(0)
                    _uiState.update {
                        it.copy(isLoading = false, insufficient = InsufficientInfo(required, myBalance, shortage))
                    }
                    return
                }
                "ACTIVE_POT_EXISTS" -> {
                    // 이미 다른 방에 참여 중 → 그 방으로 가는 안내 창 (방 번호를 모르면 문장만)
                    val activePotId = PotRepository.activePotIdOf(result)
                    if (activePotId != null) {
                        _uiState.update { it.copy(isLoading = false, activePotId = activePotId) }
                        return
                    }
                }
                // 그사이 참여 마감이 지남 → 입장 요청으로 바꾸기
                "JOIN_DEADLINE_PASSED" -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            switchToRequest = true,
                            errorMessage = "참여 마감이 지났어요. 출발지 50m 안에서 입장 요청을 보내 주세요."
                        )
                    }
                    return
                }
                // 아직 마감 전 → 일반 참여로 바꾸기
                "JOIN_REQUEST_NOT_AVAILABLE" -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            switchToRequest = false,
                            errorMessage = "아직 참여 마감 전이에요. 바로 참여할 수 있어요."
                        )
                    }
                    return
                }
            }
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result)) }
    }

    /** 잔액 부족 창을 띄운 뒤 지우기 */
    fun insufficientShown() = _uiState.update { it.copy(insufficient = null) }

    /** 에러 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(errorMessage = null) }

    /** 참여 중인 방 안내 창을 띄운 뒤 지우기 */
    fun activePotShown() = _uiState.update { it.copy(activePotId = null) }

    /** 화면 전환을 반영한 뒤 지우기 */
    fun switchShown() = _uiState.update { it.copy(switchToRequest = null) }
}