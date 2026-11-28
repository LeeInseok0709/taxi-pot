package com.example.taxipot.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.WalletApi
import com.example.taxipot.data.remote.dto.wallet.DebtResponse
import com.example.taxipot.data.remote.dto.wallet.PointHistoryResponse
import com.example.taxipot.data.remote.dto.wallet.WalletResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 내역 탭. 충전은 시연에서 운영자 잔액 조정(ADMIN_ADJUST)으로만 들어옴 */
enum class WalletTab(val type: String?) {
    POINT(null),
    CHARGE("ADMIN_ADJUST"),
    WITHDRAW("WITHDRAW")
}

data class WalletUiState(
    val wallet: WalletResponse? = null,
    val debts: List<DebtResponse> = emptyList(),
    val tab: WalletTab = WalletTab.POINT,
    val history: List<PointHistoryResponse> = emptyList(),
    val hasMoreHistory: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingHistory: Boolean = false,
    /** 납부 중인 미수금 (버튼 잠금용) */
    val payingSettlementId: Long? = null,
    val errorMessage: String? = null
)

sealed interface WalletEvent {
    data class ShowMessage(val message: String) : WalletEvent
}

class WalletViewModel : ViewModel() {

    private val api = ApiClient.create(WalletApi::class.java)

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _events = Channel<WalletEvent>(Channel.BUFFERED)
    val events: Flow<WalletEvent> = _events.receiveAsFlow()

    private var historyJob: Job? = null
    private var nextHistoryPage = 0

    /** 잔액·미수금·내역 첫 페이지를 다시 불러온다 (화면에 돌아올 때마다) */
    fun refresh() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val walletJob = async { safeApiCall { api.getWallet() } }
            val debtsJob = async { safeApiCall { api.getDebts() } }
            val wallet = walletJob.await()
            val debts = debtsJob.await()
            // 홈·참여하기 화면도 같은 잔액을 보도록 공용 창구에 알려 둔다
            if (wallet is ApiResult.Success) WalletBalance.update(wallet.data.balance)

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    wallet = if (wallet is ApiResult.Success) wallet.data else state.wallet,
                    // 이미 낸 건(PAID)은 빼고 보여준다
                    debts = if (debts is ApiResult.Success) {
                        debts.data.filter { it.debtAmount > 0 }
                    } else {
                        state.debts
                    },
                    errorMessage = when {
                        wallet is ApiResult.Failure -> wallet.message
                        wallet is ApiResult.NetworkError -> NETWORK_ERROR_MESSAGE
                        else -> null
                    }
                )
            }
        }
        loadHistory(reset = true)
    }

    fun selectTab(tab: WalletTab) {
        if (_uiState.value.tab == tab) return
        _uiState.update { it.copy(tab = tab, history = emptyList(), hasMoreHistory = false) }
        loadHistory(reset = true)
    }

    fun loadMoreHistory() {
        val state = _uiState.value
        if (state.isLoadingHistory || !state.hasMoreHistory) return
        loadHistory(reset = false)
    }

    private fun loadHistory(reset: Boolean) {
        historyJob?.cancel()
        if (reset) nextHistoryPage = 0
        val tab = _uiState.value.tab
        val page = nextHistoryPage

        _uiState.update { it.copy(isLoadingHistory = true) }
        historyJob = viewModelScope.launch {
            when (val result = safeApiCall { api.getHistory(type = tab.type, page = page, size = PAGE_SIZE) }) {
                is ApiResult.Success -> {
                    val meta = result.data.page
                    nextHistoryPage = meta.page + 1
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            history = if (reset) result.data.content else it.history + result.data.content,
                            hasMoreHistory = meta.page + 1 < meta.totalPages
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingHistory = false) }
                    _events.send(WalletEvent.ShowMessage(result.message))
                }
                is ApiResult.NetworkError -> {
                    _uiState.update { it.copy(isLoadingHistory = false) }
                    _events.send(WalletEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
                }
            }
        }
    }

    /** 미수금 납부: 잔액에서 차감 (잔액이 모자라면 서버가 있는 만큼만 부분 납부) */
    fun payDebt(debt: DebtResponse) {
        if (_uiState.value.payingSettlementId != null) return
        _uiState.update { it.copy(payingSettlementId = debt.settlementId) }
        viewModelScope.launch {
            val result = safeApiCall { api.payDebt(debt.settlementId) }
            _uiState.update { it.copy(payingSettlementId = null) }
            when (result) {
                is ApiResult.Success -> {
                    val paid = result.data
                    val message = if (paid.debtAmount == 0) {
                        "${WalletFormat.point(paid.paidAmount)}을 납부했어요. 미수금이 정리됐어요."
                    } else {
                        "잔액이 부족해 ${WalletFormat.point(paid.paidAmount)}만 납부했어요. " +
                            "남은 미수금은 ${WalletFormat.point(paid.debtAmount)}이에요."
                    }
                    _events.send(WalletEvent.ShowMessage(message))
                    refresh()
                }
                is ApiResult.Failure -> {
                    val message = when (result.code) {
                        "INSUFFICIENT_BALANCE" -> "잔액이 부족해요. 충전 후 다시 납부해 주세요."
                        "NO_DEBT" -> "이미 납부된 미수금이에요."
                        else -> result.message
                    }
                    _events.send(WalletEvent.ShowMessage(message))
                    if (result.code == "NO_DEBT") refresh()
                }
                is ApiResult.NetworkError -> _events.send(WalletEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
    }
}
