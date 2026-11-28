package com.example.taxipot.feature.pot.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.feature.pot.chat.PotRealtime
import com.example.taxipot.feature.pot.data.MyInfo
import com.example.taxipot.feature.pot.data.PlaceRepository
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 탑승 중 화면에 그릴 상태 */
data class RideUiState(
    val pot: PotDetailResponse? = null,
    /** 이 방에서 "나"의 회원 번호 */
    val myUserId: Long? = null,
    /** 한 번 보여주고 지울 문장 */
    val message: String? = null,
    /** 카카오 T에 붙여넣을 주소 (못 찾으면 null → 장소명 사용) */
    val departureAddress: String? = null,
    val destinationAddress: String? = null
)

/** RD-04 탑승 중 */
class RideViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RideUiState())
    val uiState: StateFlow<RideUiState> = _uiState.asStateFlow()

    private var potId = -1L

    // 실시간 연결 (청구·정산 상태가 바뀌면 POT_UPDATED → 다시 불러오기)
    private var realtime: PotRealtime? = null

    fun setPot(potId: Long) {
        this.potId = potId
    }

    // onResume마다 refresh가 돌아서, 주소는 한 번만 요청
    private var addressRequested = false

    private fun loadAddresses(pot: PotDetailResponse) {
        if (addressRequested) return
        addressRequested = true
        viewModelScope.launch {
            val dep = PlaceRepository.addressAt(pot.departureLat, pot.departureLng)
            val dest = PlaceRepository.addressAt(pot.destinationLat, pot.destinationLng)
            _uiState.update { it.copy(departureAddress = dep, destinationAddress = dest) }
        }
    }

    /** 방 정보 다시 불러오기 */
    fun refresh() {
        if (potId < 0) return
        viewModelScope.launch {
            when (val result = PotRepository.getPot(potId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(pot = result.data, myUserId = MyInfo.myUserId(result.data)) }
                    loadAddresses(result.data)
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(message = PotRepository.errorMessage(result))
                }
            }
        }
    }

    /** 화면이 보일 때: 실시간 연결 (가짜 데이터일 때는 연결 안 함) */
    fun startRealtime() {
        if (PotRepository.USE_FAKE || potId < 0) return
        if (realtime == null) {
            realtime = PotRealtime(
                potId = potId,
                onChat = { },
                onPotUpdated = { refresh() }
            )
        }
        realtime?.connect()
    }

    /** 화면이 안 보일 때: 실시간 끊기 */
    fun stopRealtime() {
        realtime?.disconnect()
    }

    override fun onCleared() {
        realtime?.disconnect()
    }

    /** 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(message = null) }
}