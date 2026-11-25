package com.example.taxipot.feature.pot.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse
import com.example.taxipot.feature.pot.data.PotFilter
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 목록 화면에 그릴 상태 */
data class PotListUiState(
    val isLoading: Boolean = false,
    val pots: List<PotSummaryResponse> = emptyList(),
    /** 한 번 보여주고 지울 에러 문장 */
    val errorMessage: String? = null
)

/**
 * 택시팟 목록 불러오기 — 홈(EX-01)과 목록(EX-02)이 같이 사용
 */
class PotListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PotListUiState())
    val uiState: StateFlow<PotListUiState> = _uiState.asStateFlow()

    fun load(filter: PotFilter, lat: Double?, lng: Double?) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = PotRepository.getPots(filter, lat, lng)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, pots = result.data)
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /** 에러 문장을 보여준 뒤 지우기 (화면을 다시 켰을 때 또 뜨지 않게) */
    fun errorShown() = _uiState.update { it.copy(errorMessage = null) }
}
