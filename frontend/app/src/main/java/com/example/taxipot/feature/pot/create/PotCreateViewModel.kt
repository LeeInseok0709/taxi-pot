package com.example.taxipot.feature.pot.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 방 만들기 확인 화면에 그릴 상태 */
data class PotCreateUiState(
    val isLoading: Boolean = false,
    /** 만들어진 방 (성공하면 값이 들어옴) */
    val created: PotDetailResponse? = null,
    /** 한 번 보여주고 지울 에러 문장 */
    val errorMessage: String? = null
)

/** 방 만들기 (CR-04 → POST /api/pots) */
class PotCreateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PotCreateUiState())
    val uiState: StateFlow<PotCreateUiState> = _uiState.asStateFlow()

    fun create(request: PotCreateRequest) {
        if (_uiState.value.isLoading) return   // 버튼 여러 번 눌러도 한 번만
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = PotRepository.createPot(request)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, created = result.data)
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /** 에러 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(errorMessage = null) }
}