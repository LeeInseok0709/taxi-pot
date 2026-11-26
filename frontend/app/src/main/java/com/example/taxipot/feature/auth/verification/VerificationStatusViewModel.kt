package com.example.taxipot.feature.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.VerificationApi
import com.example.taxipot.data.remote.dto.verification.VerificationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificationStatusUiState(
    val isLoading: Boolean = false,
    val verification: VerificationResponse? = null,
    val errorMessage: String? = null
)

class VerificationStatusViewModel : ViewModel() {

    private val api = ApiClient.create(VerificationApi::class.java)

    private val _uiState = MutableStateFlow(VerificationStatusUiState())
    val uiState: StateFlow<VerificationStatusUiState> = _uiState.asStateFlow()

    /** 내 인증 상태 다시 불러오기 (화면에 돌아올 때마다) */
    fun refresh() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = safeApiCall { api.getMyVerification() }) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, verification = result.data)
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요.")
                }
            }
        }
    }
}
