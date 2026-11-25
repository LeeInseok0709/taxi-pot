package com.example.taxipot.feature.pot.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
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
    /** 잔액 부족이면 값이 들어옴 → JN-03 창 */
    val insufficient: InsufficientInfo? = null,
    /** 그 외 실패 문장 */
    val errorMessage: String? = null,
    /** 이미 다른 방에 참여 중이면 그 방 번호 (안내 창에서 그 방으로 이동) */
    val activePotId: Long? = null
)

/** 택시팟 참여 (JN-01 → POST /api/pots/{potId}/members) */
class PotJoinViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PotJoinUiState())
    val uiState: StateFlow<PotJoinUiState> = _uiState.asStateFlow()

    /**
     * 참여하기
     * holdAmount, balance: 서버가 잔액 부족 detail을 안 줄 때 대신 보여줄 값
     */
    fun join(potId: Long, seat: String, holdAmount: Int, balance: Int) {
        if (_uiState.value.isLoading) return   // 여러 번 눌러도 한 번만
        _uiState.update { it.copy(isLoading = true, errorMessage = null, insufficient = null, activePotId = null) }

        viewModelScope.launch {
            when (val result = PotRepository.join(potId, seat)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, joined = result.data)
                }

                is ApiResult.Failure -> {
                    if (result.code == "INSUFFICIENT_BALANCE") {
                        // 서버 detail: requiredAmount, balance, shortageAmount (숫자는 Double로 옴)
                        val detail = result.detail
                        val required = (detail?.get("requiredAmount") as? Number)?.toInt() ?: holdAmount
                        val myBalance = (detail?.get("balance") as? Number)?.toInt() ?: balance
                        val shortage = (detail?.get("shortageAmount") as? Number)?.toInt()
                            ?: (required - myBalance).coerceAtLeast(0)
                        _uiState.update {
                            it.copy(isLoading = false, insufficient = InsufficientInfo(required, myBalance, shortage))
                        }
                    } else if (result.code == "ACTIVE_POT_EXISTS") {
                        // 이미 다른 방에 참여 중 → 그 방으로 가는 안내 창 (방 번호를 모르면 문장만)
                        val activePotId = PotRepository.activePotIdOf(result)
                        _uiState.update {
                            if (activePotId != null) {
                                it.copy(isLoading = false, activePotId = activePotId)
                            } else {
                                it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result))
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result))
                        }
                    }
                }

                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /** 잔액 부족 창을 띄운 뒤 지우기 */
    fun insufficientShown() = _uiState.update { it.copy(insufficient = null) }

    /** 에러 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(errorMessage = null) }

    /** 참여 중인 방 안내 창을 띄운 뒤 지우기 */
    fun activePotShown() = _uiState.update { it.copy(activePotId = null) }
}