package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.report.ReportResponse
import com.example.taxipot.data.remote.dto.user.AccountResponse
import com.example.taxipot.data.remote.dto.user.MyPotHistoryResponse
import com.example.taxipot.data.remote.dto.user.MyProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 내 정보 API (로그인 토큰 필요) */
interface UserApi {

    /** 내 정보 (닉네임·학교·평점·인증·신고 누적 등) */
    @GET("api/users/me")
    suspend fun getMyProfile(): Response<MyProfileResponse>

    /** 내 출금 계좌 (계좌번호는 가려져서 옴) */
    @GET("api/users/me/account")
    suspend fun getMyAccount(): Response<AccountResponse>

    /** 내 택시 이용 기록. role(HOST/GUEST)·status로 거를 수 있음 */
    @GET("api/users/me/pots")
    suspend fun getMyPots(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<MyPotHistoryResponse>>

    /** 내가 접수한 신고 내역 */
    @GET("api/users/me/reports")
    suspend fun getMyReports(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<ReportResponse>>
}
