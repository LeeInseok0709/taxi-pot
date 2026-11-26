package com.example.taxipot.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.AuthApi
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.user.MyProfileResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageUiState(
    val profile: MyProfileResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface MyPageEvent {
    data object LoggedOut : MyPageEvent
}

class MyPageViewModel : ViewModel() {

    private val userApi = ApiClient.create(UserApi::class.java)
    private val authApi = ApiClient.create(AuthApi::class.java)

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _events = Channel<MyPageEvent>(Channel.BUFFERED)
    val events: Flow<MyPageEvent> = _events.receiveAsFlow()

    fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = safeApiCall { userApi.getMyProfile() }) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, profile = result.data) }
                is ApiResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요.")
                }
            }
        }
    }

    /** 로그아웃: 서버 토큰 무효화(실패해도 진행) → 기기 토큰 삭제 */
    fun logout() {
        viewModelScope.launch {
            safeApiCall { authApi.logout() }
            ApiClient.tokenManager.clear()
            _events.send(MyPageEvent.LoggedOut)
        }
    }
}
