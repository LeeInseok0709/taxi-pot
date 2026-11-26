package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.rating.RatingCreateRequest
import com.example.taxipot.data.remote.dto.rating.RatingTargetResponse
import com.example.taxipot.data.remote.dto.report.ReportCreateRequest
import com.example.taxipot.data.remote.dto.report.ReportResponse
import com.example.taxipot.data.remote.dto.settlement.PotSettlementResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** 하차 후 처리 API: 정산 내역 · 영수증 · 동승자 평가 · 신고 (로그인 토큰 필요) */
interface SettlementApi {

    /** 택시팟 정산 내역 (참여자 전원 행 포함) */
    @GET("api/pots/{potId}/settlement")
    suspend fun getSettlement(@Path("potId") potId: Long): Response<PotSettlementResponse>

    /** 결제내역(영수증) 사진 (이미지 파일) */
    @GET("api/pots/{potId}/receipt")
    suspend fun getReceipt(@Path("potId") potId: Long): Response<ResponseBody>

    /** 평가할 동승자 목록 (나 제외 탑승자) */
    @GET("api/pots/{potId}/ratings/targets")
    suspend fun getRatingTargets(@Path("potId") potId: Long): Response<List<RatingTargetResponse>>

    /** 동승자 평가 제출 (정산 후 24시간 안, 응답 본문은 쓰지 않음) */
    @POST("api/pots/{potId}/ratings")
    suspend fun submitRatings(
        @Path("potId") potId: Long,
        @Body request: RatingCreateRequest
    ): Response<Unit>

    /** 신고 접수 */
    @POST("api/reports")
    suspend fun report(@Body request: ReportCreateRequest): Response<ReportResponse>
}
