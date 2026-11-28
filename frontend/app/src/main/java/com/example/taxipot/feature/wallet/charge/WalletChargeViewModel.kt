package com.example.taxipot.feature.wallet.charge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.api.WalletApi
import com.example.taxipot.data.remote.dto.user.AccountResponse
import com.example.taxipot.data.remote.dto.wallet.WalletResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletChargeUiState(
    val wallet: WalletResponse? = null,
    /** 충전 계좌 = 회원가입·계좌 관리에서 등록한 본인 계좌 (없으면 null) */
    val account: AccountResponse? = null,
    val accountLoaded: Boolean = false,
    /** 입력한 충전 금액 (원) */
    val amount: Int = 0,
    /** 미수금에서 먼저 빠지는 금액 */
    val debtDeduction: Int = 0,
    /** 실제로 지갑에 들어갈 금액 */
    val actualCharge: Int = 0,
    val amountError: String? = null
) {
    val hasAccount: Boolean get() = !account?.maskedAccountNumber.isNullOrBlank()
}

class WalletChargeViewModel : ViewModel() {

    private val walletApi = ApiClient.create(WalletApi::class.java)
    private val userApi = ApiClient.create(UserApi::class.java)

    private val _uiState = MutableStateFlow(WalletChargeUiState())
    val uiState: StateFlow<WalletChargeUiState> = _uiState.asStateFlow()

    /** 지갑(미수금·보관 한도 계산용)과 충전 계좌 불러오기. 계좌를 바꾸고 돌아와도 최신으로 */
    fun load() {
        viewModelScope.launch {
            val walletJob = async { safeApiCall { walletApi.getWallet() } }
            val accountJob = async { safeApiCall { userApi.getMyAccount() } }
            val wallet = walletJob.await()
            val account = accountJob.await()
            _uiState.update {
                it.copy(
                    wallet = if (wallet is ApiResult.Success) wallet.data else it.wallet,
                    account = if (account is ApiResult.Success) account.data else null,
                    accountLoaded = true
                )
            }
            setAmount(_uiState.value.amount)
        }
    }

    fun setAmount(amount: Int) {
        val debt = _uiState.value.wallet?.debtAmount ?: 0
        val deduction = minOf(amount, debt)
        _uiState.update {
            it.copy(
                amount = amount,
                debtDeduction = deduction,
                actualCharge = amount - deduction,
                amountError = null
            )
        }
    }

    fun addAmount(add: Int) = setAmount((_uiState.value.amount + add).coerceAtMost(MAX_INPUT))

    /** 서버 규칙(명세서)과 같은 기준으로 먼저 확인. 문제없으면 true */
    fun validate(): Boolean {
        val state = _uiState.value
        val wallet = state.wallet
        val error = when {
            state.accountLoaded && !state.hasAccount -> "충전할 계좌를 먼저 등록해 주세요."
            state.amount < MIN_CHARGE || state.amount > MAX_CHARGE ->
                "1회 충전은 %,d원 ~ %,d원이에요.".format(MIN_CHARGE, MAX_CHARGE)
            wallet != null && wallet.balance + wallet.heldBalance + state.actualCharge > WALLET_LIMIT ->
                "충전하면 지갑 보관 한도(%,dP)를 넘어요.".format(WALLET_LIMIT)
            else -> null
        }
        _uiState.update { it.copy(amountError = error) }
        return error == null
    }

    companion object {
        /** 명세서 3.12.0: 1회 충전 10,000 ~ 50,000원, 지갑 잔액(balance + held) 최대 500,000원 */
        const val MIN_CHARGE = 10_000
        const val MAX_CHARGE = 50_000
        const val WALLET_LIMIT = 500_000
        private const val MAX_INPUT = 9_999_999
    }
}
