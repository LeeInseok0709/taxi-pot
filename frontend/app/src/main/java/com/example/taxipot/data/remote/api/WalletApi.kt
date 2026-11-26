package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.wallet.DebtPayResponse
import com.example.taxipot.data.remote.dto.wallet.DebtResponse
import com.example.taxipot.data.remote.dto.wallet.PointHistoryResponse
import com.example.taxipot.data.remote.dto.wallet.WalletResponse
import com.example.taxipot.data.remote.dto.wallet.WithdrawalRequest
import com.example.taxipot.data.remote.dto.wallet.WithdrawalResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 지갑 API (로그인 토큰 필요) */
interface WalletApi {

    /** 내 지갑 (잔액·예치금·미수금) */
    @GET("api/wallet")
    suspend fun getWallet(): Response<WalletResponse>

    /** 포인트 내역. type을 비우면 전체 */
    @GET("api/wallet/history")
    suspend fun getHistory(
        @Query("type") type: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<PointHistoryResponse>>

    /** 남은 미수금 목록 */
    @GET("api/wallet/debts")
    suspend fun getDebts(): Response<List<DebtResponse>>

    /** 미수금 납부 (잔액에서 차감, 잔액이 모자라면 있는 만큼 부분 납부) */
    @POST("api/wallet/debts/{settlementId}/pay")
    suspend fun payDebt(@Path("settlementId") settlementId: Long): Response<DebtPayResponse>

    /** 등록 계좌로 출금 (시연은 더미 송금이라 결과가 바로 옴) */
    @POST("api/wallet/withdrawals")
    suspend fun requestWithdrawal(@Body request: WithdrawalRequest): Response<WithdrawalResponse>
}
