package com.example.taxipot.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.AuthApi
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.user.AccountResponse
import com.example.taxipot.data.remote.dto.user.MyProfileResponse
import com.example.taxipot.data.remote.dto.user.UserUpdateRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val profile: MyProfileResponse? = null,
    val account: AccountResponse? = null,
    val isCheckingNickname: Boolean = false,
    /** 중복확인을 통과한 닉네임 */
    val checkedNickname: String? = null,
    val nicknameError: String? = null,
    val phoneError: String? = null,
    val isSaving: Boolean = false
)

sealed interface ProfileEditEvent {
    data class ShowMessage(val message: String) : ProfileEditEvent
    data object Saved : ProfileEditEvent
}

class ProfileEditViewModel : ViewModel() {

    private val userApi = ApiClient.create(UserApi::class.java)
    private val authApi = ApiClient.create(AuthApi::class.java)

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEditEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEditEvent> = _events.receiveAsFlow()

    /** 내 정보·출금 계좌 불러오기 (계좌 변경하고 돌아와도 최신으로) */
    fun load() {
        viewModelScope.launch {
            val profileJob = async { safeApiCall { userApi.getMyProfile() } }
            val accountJob = async { safeApiCall { userApi.getMyAccount() } }
            val profile = profileJob.await()
            val account = accountJob.await()
            _uiState.update {
                it.copy(
                    profile = if (profile is ApiResult.Success) profile.data else it.profile,
                    account = if (account is ApiResult.Success) account.data else null
                )
            }
            if (profile !is ApiResult.Success) _events.send(ProfileEditEvent.ShowMessage("내 정보를 불러오지 못했어요."))
        }
    }

    fun onNicknameChanged() = _uiState.update { it.copy(nicknameError = null) }
    fun onPhoneChanged() = _uiState.update { it.copy(phoneError = null) }

    fun checkNickname(raw: String) {
        val nickname = raw.trim()
        val current = _uiState.value.profile?.nickname
        val error = when {
            nickname.isEmpty() -> "닉네임을 입력해 주세요."
            nickname.length > 30 -> "닉네임은 30자 이하로 입력해 주세요."
            nickname.startsWith("탈퇴회원_") -> "사용할 수 없는 닉네임이에요."
            nickname == current -> "지금 쓰고 있는 닉네임이에요."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(nicknameError = error) }
            return
        }
        _uiState.update { it.copy(isCheckingNickname = true) }
        viewModelScope.launch {
            val result = safeApiCall { authApi.checkNickname(nickname) }
            _uiState.update { it.copy(isCheckingNickname = false) }
            when (result) {
                is ApiResult.Success -> _uiState.update {
                    if (result.data.available) {
                        it.copy(checkedNickname = nickname, nicknameError = null)
                    } else {
                        it.copy(
                            checkedNickname = null,
                            nicknameError = if (result.data.reason == "RESERVED") "사용할 수 없는 닉네임이에요." else "이미 사용 중인 닉네임이에요."
                        )
                    }
                }
                is ApiResult.Failure -> _uiState.update { it.copy(nicknameError = result.message) }
                is ApiResult.NetworkError -> _events.send(ProfileEditEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    fun save(rawNickname: String, rawPhone: String, marketing: Boolean) {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.isSaving) return

        val nickname = rawNickname.trim()
        val phone = rawPhone.trim()
        val nicknameChanged = nickname != profile.nickname
        val phoneChanged = phone.filter { it.isDigit() } != profile.phone.orEmpty().filter { it.isDigit() }
        val marketingChanged = marketing != (profile.agreeMarketing == true)

        val nicknameError = when {
            !nicknameChanged -> null
            nickname.isEmpty() -> "닉네임을 입력해 주세요."
            state.checkedNickname != nickname -> "닉네임 중복확인을 해주세요."
            else -> null
        }
        val phoneError = if (phoneChanged && !PHONE_REGEX.matches(phone)) "휴대폰 번호를 확인해 주세요. (예: 010-1234-5678)" else null
        if (nicknameError != null || phoneError != null) {
            _uiState.update { it.copy(nicknameError = nicknameError, phoneError = phoneError) }
            return
        }
        if (!nicknameChanged && !phoneChanged && !marketingChanged) {
            viewModelScope.launch { _events.send(ProfileEditEvent.ShowMessage("바뀐 내용이 없어요.")) }
            return
        }

        val request = UserUpdateRequest(
            nickname = nickname.takeIf { nicknameChanged },
            phone = phone.takeIf { phoneChanged },
            agreeMarketing = marketing.takeIf { marketingChanged }
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = safeApiCall { userApi.updateMyProfile(request) }
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is ApiResult.Success -> _events.send(ProfileEditEvent.Saved)
                is ApiResult.Failure -> when (result.code) {
                    "DUPLICATE_NICKNAME" -> _uiState.update { it.copy(nicknameError = "이미 사용 중인 닉네임이에요.", checkedNickname = null) }
                    "RESERVED_NICKNAME" -> _uiState.update { it.copy(nicknameError = "사용할 수 없는 닉네임이에요.", checkedNickname = null) }
                    else -> _events.send(ProfileEditEvent.ShowMessage(result.message))
                }
                is ApiResult.NetworkError -> _events.send(ProfileEditEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    companion object {
        private val PHONE_REGEX = Regex("^01[016789]-?[0-9]{3,4}-?[0-9]{4}$")
        const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
    }
}
