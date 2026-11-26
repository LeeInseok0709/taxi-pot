package com.example.taxipot.feature.auth.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.AuthApi
import com.example.taxipot.data.remote.api.CommonApi
import com.example.taxipot.data.remote.dto.auth.SignupRequest
import com.example.taxipot.data.remote.dto.common.BankResponse
import com.example.taxipot.data.remote.dto.common.UniversityResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 입력 항목 (화면 위에서 아래 순서. 에러가 여러 개면 가장 위 항목으로 스크롤) */
enum class SignupField {
    EMAIL, PASSWORD, PASSWORD_CONFIRM, NAME, NICKNAME, GENDER, PHONE,
    UNIVERSITY, STUDENT_NO, BANK, ACCOUNT_NUMBER, AGREE
}

enum class NicknameCheck { NONE, CHECKING, AVAILABLE }

/** 화면에서 직접 입력한 값 */
data class SignupInput(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val name: String,
    val nickname: String,
    val phone: String,
    val studentNo: String,
    val accountNumber: String,
    val agreeRequired: Boolean,
    val agreeMarketing: Boolean
)

/** 화면에 그릴 상태 */
data class SignupUiState(
    val universities: List<UniversityResponse> = emptyList(),
    val banks: List<BankResponse> = emptyList(),
    val university: UniversityResponse? = null,
    val bank: BankResponse? = null,
    /** MALE / FEMALE */
    val gender: String? = null,
    val nicknameCheck: NicknameCheck = NicknameCheck.NONE,
    /** 중복확인을 통과한 닉네임 (입력을 바꾸면 다시 확인해야 함) */
    val checkedNickname: String? = null,
    val errors: Map<SignupField, String> = emptyMap(),
    val isLoading: Boolean = false,
    /** 입력칸이 아닌 버튼 위에 보여줄 에러 */
    val errorMessage: String? = null
)

sealed interface SignupEvent {
    /** 가입 완료 → 이메일 인증 화면으로 (받는 메일 주소 표시용) */
    data class Done(val email: String) : SignupEvent
    data class ScrollTo(val field: SignupField) : SignupEvent
    data class ShowMessage(val message: String) : SignupEvent
}

class SignupViewModel : ViewModel() {

    private val authApi = ApiClient.create(AuthApi::class.java)
    private val commonApi = ApiClient.create(CommonApi::class.java)

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignupEvent>(Channel.BUFFERED)
    val events: Flow<SignupEvent> = _events.receiveAsFlow()

    init {
        loadOptions()
    }

    /** 학교·은행 목록 불러오기 (화면 열 때 1회, 실패하면 선택할 때 다시) */
    fun loadOptions() {
        viewModelScope.launch {
            val universitiesJob = async { safeApiCall { commonApi.getUniversities() } }
            val banksJob = async { safeApiCall { commonApi.getBanks() } }
            val universities = universitiesJob.await()
            val banks = banksJob.await()

            _uiState.update { state ->
                state.copy(
                    universities = if (universities is ApiResult.Success) universities.data else state.universities,
                    banks = if (banks is ApiResult.Success) banks.data else state.banks
                )
            }
            if (universities !is ApiResult.Success || banks !is ApiResult.Success) {
                _events.send(SignupEvent.ShowMessage("학교·은행 목록을 불러오지 못했어요. 인터넷 연결을 확인해 주세요."))
            }
        }
    }

    fun selectGender(gender: String) = _uiState.update {
        it.copy(gender = gender, errors = it.errors - SignupField.GENDER)
    }

    fun selectUniversity(university: UniversityResponse) = _uiState.update {
        it.copy(university = university, errors = it.errors - SignupField.UNIVERSITY)
    }

    fun selectBank(bank: BankResponse) = _uiState.update {
        it.copy(bank = bank, errors = it.errors - SignupField.BANK)
    }

    /** 입력을 고치기 시작하면 해당 에러를 지운다 */
    fun clearError(field: SignupField) = _uiState.update {
        if (field in it.errors || it.errorMessage != null) {
            it.copy(errors = it.errors - field, errorMessage = null)
        } else {
            it
        }
    }

    /** 닉네임을 바꾸면 중복확인을 다시 해야 한다 */
    fun onNicknameChanged(nickname: String) = _uiState.update {
        if (it.nicknameCheck != NicknameCheck.NONE && nickname != it.checkedNickname) {
            it.copy(nicknameCheck = NicknameCheck.NONE, checkedNickname = null)
        } else {
            it
        }
    }

    fun checkNickname(raw: String) {
        if (_uiState.value.nicknameCheck == NicknameCheck.CHECKING) return
        val nickname = raw.trim()
        val localError = when {
            nickname.isEmpty() -> "닉네임을 입력해 주세요."
            nickname.length > 30 -> "닉네임은 30자 이하로 입력해 주세요."
            nickname.startsWith("탈퇴회원_") -> "사용할 수 없는 닉네임이에요."
            else -> null
        }
        if (localError != null) {
            _uiState.update { it.copy(errors = it.errors + (SignupField.NICKNAME to localError)) }
            return
        }

        _uiState.update { it.copy(nicknameCheck = NicknameCheck.CHECKING) }
        viewModelScope.launch {
            when (val result = safeApiCall { authApi.checkNickname(nickname) }) {
                is ApiResult.Success -> _uiState.update {
                    if (result.data.available) {
                        it.copy(
                            nicknameCheck = NicknameCheck.AVAILABLE,
                            checkedNickname = nickname,
                            errors = it.errors - SignupField.NICKNAME
                        )
                    } else {
                        val message = if (result.data.reason == "RESERVED") {
                            "사용할 수 없는 닉네임이에요."
                        } else {
                            "이미 사용 중인 닉네임이에요."
                        }
                        it.copy(
                            nicknameCheck = NicknameCheck.NONE,
                            errors = it.errors + (SignupField.NICKNAME to message)
                        )
                    }
                }

                is ApiResult.Failure -> _uiState.update {
                    it.copy(
                        nicknameCheck = NicknameCheck.NONE,
                        errors = it.errors + (SignupField.NICKNAME to result.message)
                    )
                }

                is ApiResult.NetworkError -> {
                    _uiState.update { it.copy(nicknameCheck = NicknameCheck.NONE) }
                    _events.send(SignupEvent.ShowMessage("서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."))
                }
            }
        }
    }

    fun signup(input: SignupInput) {
        val state = _uiState.value
        if (state.isLoading) return

        val errors = validate(input, state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, errorMessage = null) }
            _events.trySend(SignupEvent.ScrollTo(errors.keys.minBy { it.ordinal }))
            return
        }

        // 위 검사를 통과했으면 선택값은 모두 있음
        val gender = state.gender ?: return
        val university = state.university ?: return
        val bank = state.bank ?: return

        val request = SignupRequest(
            email = input.email.trim(),
            password = input.password,
            name = input.name.trim(),
            nickname = input.nickname.trim(),
            phone = input.phone.trim(),
            gender = gender,
            university = university.name,
            studentNo = input.studentNo.trim(),
            bankCode = bank.code,
            accountNumber = input.accountNumber.trim(),
            accountHolder = input.name.trim(),
            agreeRequired = input.agreeRequired,
            agreeMarketing = input.agreeMarketing
        )

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = safeApiCall { authApi.signup(request) }) {
                is ApiResult.Success -> {
                    // 이메일 인증 API는 로그인 상태로 호출하므로 토큰을 바로 저장
                    ApiClient.tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(SignupEvent.Done(result.data.user.email))
                }

                is ApiResult.Failure -> handleFailure(result)

                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
                    )
                }
            }
        }
    }

    private fun validate(input: SignupInput, state: SignupUiState): Map<SignupField, String> {
        val errors = linkedMapOf<SignupField, String>()
        val university = state.university

        val email = input.email.trim()
        when {
            email.isEmpty() -> errors[SignupField.EMAIL] = "학교 이메일을 입력해 주세요."
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                errors[SignupField.EMAIL] = "이메일 형식이 올바르지 않아요."
            university != null &&
                !email.substringAfter('@').equals(university.emailDomain, ignoreCase = true) ->
                errors[SignupField.EMAIL] = "${university.name} 메일(@${university.emailDomain})로 입력해 주세요."
        }

        if (input.password.length < 8) {
            errors[SignupField.PASSWORD] = "비밀번호는 8자 이상이어야 해요."
        }
        if (input.passwordConfirm.isEmpty() || input.passwordConfirm != input.password) {
            errors[SignupField.PASSWORD_CONFIRM] = "비밀번호가 일치하지 않아요."
        }
        if (input.name.isBlank()) {
            errors[SignupField.NAME] = "이름을 입력해 주세요."
        }

        val nickname = input.nickname.trim()
        when {
            nickname.isEmpty() -> errors[SignupField.NICKNAME] = "닉네임을 입력해 주세요."
            state.nicknameCheck != NicknameCheck.AVAILABLE || state.checkedNickname != nickname ->
                errors[SignupField.NICKNAME] = "닉네임 중복확인을 해주세요."
        }

        if (state.gender == null) {
            errors[SignupField.GENDER] = "성별을 선택해 주세요."
        }
        if (!PHONE_REGEX.matches(input.phone.trim())) {
            errors[SignupField.PHONE] = "휴대폰 번호를 확인해 주세요. (예: 010-1234-5678)"
        }
        if (university == null) {
            errors[SignupField.UNIVERSITY] = "대학교를 선택해 주세요."
        }
        if (input.studentNo.isBlank()) {
            errors[SignupField.STUDENT_NO] = "학번을 입력해 주세요."
        }
        if (state.bank == null) {
            errors[SignupField.BANK] = "은행을 선택해 주세요."
        }

        val account = input.accountNumber.trim()
        val accountDigits = account.filter { it.isDigit() }
        if (!ACCOUNT_REGEX.matches(account) || accountDigits.length !in 10..14) {
            errors[SignupField.ACCOUNT_NUMBER] = "계좌번호는 숫자 10~14자리로 입력해 주세요."
        }
        if (!input.agreeRequired) {
            errors[SignupField.AGREE] = "필수 약관에 동의해 주세요."
        }
        return errors
    }

    /** 서버 에러 코드를 해당 입력칸 에러로 바꿔서 보여준다 */
    private suspend fun handleFailure(failure: ApiResult.Failure) {
        val fieldError: Pair<SignupField, String>? = when (failure.code) {
            "DUPLICATE_EMAIL" -> SignupField.EMAIL to "이미 가입된 이메일이에요."
            "EMAIL_DOMAIN_MISMATCH" -> SignupField.EMAIL to "선택한 학교의 메일 주소가 아니에요."
            "DUPLICATE_NICKNAME" -> SignupField.NICKNAME to "이미 사용 중인 닉네임이에요."
            "RESERVED_NICKNAME" -> SignupField.NICKNAME to "사용할 수 없는 닉네임이에요."
            "DUPLICATE_STUDENT_NO" -> SignupField.STUDENT_NO to "이미 가입된 학번이에요."
            "UNSUPPORTED_UNIVERSITY" -> SignupField.UNIVERSITY to "가입할 수 없는 학교예요."
            "TERMS_NOT_AGREED" -> SignupField.AGREE to "필수 약관에 동의해 주세요."
            else -> null
        }

        if (fieldError != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errors = it.errors + fieldError,
                    nicknameCheck = if (fieldError.first == SignupField.NICKNAME) NicknameCheck.NONE else it.nicknameCheck
                )
            }
            _events.send(SignupEvent.ScrollTo(fieldError.first))
        } else {
            val message = when (failure.code) {
                "ACCOUNT_HOLDER_MISMATCH" -> "예금주가 이름과 달라요."
                // INVALID_INPUT은 서버 메시지에 어떤 항목인지 적혀 있음 (예: accountNumber: ...)
                else -> failure.message
            }
            _uiState.update { it.copy(isLoading = false, errorMessage = message) }
        }
    }

    companion object {
        private val PHONE_REGEX = Regex("^01[016789]-?[0-9]{3,4}-?[0-9]{4}$")
        private val ACCOUNT_REGEX = Regex("^[0-9-]+$")
    }
}
