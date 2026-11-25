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
    /** 불러오기 실패 문장 (보여주고 화면 닫기) */
    val errorMessage: String? = null
)

/** 택시팟 상세 불러오기 (EX-04) */
class PotDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PotDetailUiState())
    val uiState: StateFlow<PotDetailUiState> = _uiState.asStateFlow()

    fun load(potId: Long) {
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
}
