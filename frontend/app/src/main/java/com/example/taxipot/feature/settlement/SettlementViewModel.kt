package com.example.taxipot.feature.settlement

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.auth.CurrentUser
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.SettlementApi
import com.example.taxipot.data.remote.dto.settlement.PotSettlementResponse
import com.example.taxipot.data.remote.dto.settlement.SettlementResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettlementUiState(
    val isLoading: Boolean = false,
    val settlement: PotSettlementResponse? = null,
    /** 정산 내역 중 내 행 */
    val mine: SettlementResponse? = null,
    val isLoadingReceipt: Boolean = false,
    val errorMessage: String? = null
) {
    /** 운영자가 실요금을 승인해 정산이 끝났는지 */
    val isSettled: Boolean
        get() = settlement?.actualFare != null && settlement.settlements.any { it.status == "SETTLED" }
}

sealed interface SettlementEvent {
    data class ShowReceipt(val bitmap: Bitmap) : SettlementEvent
    data class ShowMessage(val message: String) : SettlementEvent
}

class SettlementViewModel : ViewModel() {

    private val api = ApiClient.create(SettlementApi::class.java)

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettlementEvent>(Channel.BUFFERED)
    val events: Flow<SettlementEvent> = _events.receiveAsFlow()

    fun load(potId: Long) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = safeApiCall { api.getSettlement(potId) }) {
                is ApiResult.Success -> {
                    val me = CurrentUser.id()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            settlement = result.data,
                            mine = result.data.settlements.firstOrNull { row -> row.memberId == me }
                        )
                    }
                }
                is ApiResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (result.code == "NOT_POT_MEMBER") "이 택시팟의 참여자만 볼 수 있어요." else result.message
                    )
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = NETWORK_ERROR_MESSAGE)
                }
            }
        }
    }

    /** 영수증 사진 불러오기 (토큰이 필요해서 앱에서 받아 보여줌) */
    fun loadReceipt(potId: Long) {
        if (_uiState.value.isLoadingReceipt) return
        _uiState.update { it.copy(isLoadingReceipt = true) }
        viewModelScope.launch {
            val result = safeApiCall { api.getReceipt(potId) }
            _uiState.update { it.copy(isLoadingReceipt = false) }
            when (result) {
                is ApiResult.Success -> {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            result.data.bytes().let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        _events.send(SettlementEvent.ShowReceipt(bitmap))
                    } else {
                        _events.send(SettlementEvent.ShowMessage("영수증 사진을 읽지 못했어요."))
                    }
                }
                is ApiResult.Failure -> _events.send(SettlementEvent.ShowMessage(result.message))
                is ApiResult.NetworkError -> _events.send(SettlementEvent.ShowMessage(NETWORK_ERROR_MESSAGE))
            }
        }
    }

    companion object {
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
    }
}
