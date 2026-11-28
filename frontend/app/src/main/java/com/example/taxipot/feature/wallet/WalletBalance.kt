package com.example.taxipot.feature.wallet

import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.WalletApi

/**
 * 여러 화면(홈·방 만들기·참여하기·내 지갑)이 같은 잔액을 보도록 하는 공용 창구.
 * - 홈 화면이 열릴 때마다 refresh()로 서버 잔액을 받아 두고
 * - 다른 화면은 latest(마지막으로 받은 값)를 쓴다
 * - 실제 예치·참여 가능 여부는 서버가 다시 확인하므로, 여기 값은 화면 표시용
 */
object WalletBalance {

    private val api by lazy { ApiClient.create(WalletApi::class.java) }

    /** 마지막으로 서버에서 받은 사용 가능 잔액 (아직 모르면 null) */
    @Volatile
    var latest: Int? = null
        private set

    /** 서버에서 잔액을 다시 받아 저장하고 돌려준다 (실패하면 마지막 값) */
    suspend fun refresh(): Int? {
        val result = safeApiCall { api.getWallet() }
        if (result is ApiResult.Success) latest = result.data.balance
        return latest
    }

    /** 내 지갑 화면처럼 이미 잔액을 받아 온 곳에서 알려줄 때 */
    fun update(balance: Int) {
        latest = balance
    }
}
