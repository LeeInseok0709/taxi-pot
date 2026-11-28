package com.example.taxipot.feature.wallet.withdraw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.api.WalletApi
import com.example.taxipot.data.remote.dto.user.AccountResponse
import com.example.taxipot.data.remote.dto.wallet.WalletResponse
import com.example.taxipot.data.remote.dto.wallet.WithdrawalRequest
import com.example.taxipot.data.remote.dto.wallet.WithdrawalResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WithdrawUiState(
    val wallet: WalletResponse? = null,
    val account: AccountResponse? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val amountError: String? = null,
    val errorMessage: String? = null
)

sealed interface WithdrawEvent {
    /** 출금 처리 끝 (성공·실패 모두) → 결과 화면 */
    data class Finished(val result: WithdrawalResponse) : WithdrawEvent
}

class WithdrawViewModel : ViewModel() {

    private val walletApi = ApiClient.create(WalletApi::class.java)
    private val userApi = ApiClient.create(UserApi::class.java)

    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState.asStateFlow()

    private val _events = Channel<WithdrawEvent>(Channel.BUFFERED)
    val events: Flow<WithdrawEvent> = _events.receiveAsFlow()

    /** 잔액·출금 계좌 불러오기 (화면에 돌아올 때마다, 실패 후 잔액이 복구되므로) */
    fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val walletJob = async { safeApiCall { walletApi.getWallet() } }
            val accountJob = async { safeApiCall { userApi.getMyAccount() } }
            val wallet = walletJob.await()
            val account = accountJob.await()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    wallet = if (wallet is ApiResult.Success) wallet.data else it.wallet,
                    account = if (account is ApiResult.Success) account.data else it.account,
                    errorMessage = when {
                        wallet is ApiResult.NetworkError || account is ApiResult.NetworkError -> NETWORK_ERROR_MESSAGE
                        account is ApiResult.Failure && account.code == "ACCOUNT_NOT_REGISTERED" -> NO_ACCOUNT_MESSAGE
                        else -> null
                    }
                )
            }
        }
    }

    fun clearAmountError() = _uiState.update {
        if (it.amountError != null) it.copy(amountError = null) else it
    }

    /** 앱에서 먼저 확인 (서버 규칙과 같음). 문제없으면 true */
    fun validate(amount: Int?): Boolean {
        val state = _uiState.value
        val balance = state.wallet?.balance
        val error = when {
            state.account?.maskedAccountNumber.isNullOrBlank() -> NO_ACCOUNT_MESSAGE
            (state.wallet?.debtAmount ?: 0) > 0 -> DEBT_MESSAGE
            amount == null || amount <= 0 -> "출금할 금액을 입력해 주세요."
            amount < MIN_AMOUNT -> "출금은 %,d원 이상부터 가능해요.".format(MIN_AMOUNT)
            amount > MAX_AMOUNT -> "1회 출금은 %,d원까지 가능해요.".format(MAX_AMOUNT)
            balance != null && amount > balance -> "사용 가능 잔액(%,dP)보다 많아요.".format(balance)
            else -> null
        }
        _uiState.update { it.copy(amountError = error) }
        return error == null
    }

    fun withdraw(amount: Int) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = safeApiCall { walletApi.requestWithdrawal(WithdrawalRequest(amount)) }
            _uiState.update { it.copy(isSubmitting = false) }
            when (result) {
                is ApiResult.Success -> _events.send(WithdrawEvent.Finished(result.data))
                is ApiResult.Failure -> {
                    val message = when (result.code) {
                        "ACCOUNT_NOT_REGISTERED" -> NO_ACCOUNT_MESSAGE
                        "INSUFFICIENT_BALANCE" -> "사용 가능 잔액보다 많아요. 금액을 다시 확인해 주세요."
                        "UNPAID_DEBT_EXISTS" -> DEBT_MESSAGE
                        else -> result.message
                    }
                    _uiState.update { it.copy(amountError = message) }
                    load()
                }
                is ApiResult.NetworkError -> _uiState.update { it.copy(errorMessage = NETWORK_ERROR_MESSAGE) }
            }
        }
    }

    companion object {
        /** 서버 규칙: 1,000원 이상, 1회 최대 1,000,000원, 사용 가능 잔액 이하 */
        private const val MIN_AMOUNT = 1_000
        private const val MAX_AMOUNT = 1_000_000
        private const val NO_ACCOUNT_MESSAGE = "등록된 출금 계좌가 없어요. 내 정보 수정에서 계좌를 등록해 주세요."
        private const val DEBT_MESSAGE = "미수금이 있으면 출금할 수 없어요. 내 지갑에서 먼저 납부해 주세요."
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
    }
}
