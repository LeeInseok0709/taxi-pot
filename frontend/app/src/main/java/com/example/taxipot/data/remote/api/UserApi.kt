package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.auth.TokenResponse
import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.report.ReportResponse
import com.example.taxipot.data.remote.dto.user.AccountResponse
import com.example.taxipot.data.remote.dto.user.AccountUpdateRequest
import com.example.taxipot.data.remote.dto.user.MemberWithdrawRequest
import com.example.taxipot.data.remote.dto.user.MyPotHistoryResponse
import com.example.taxipot.data.remote.dto.user.MyProfileResponse
import com.example.taxipot.data.remote.dto.user.PasswordChangeRequest
import com.example.taxipot.data.remote.dto.user.UserUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/** 내 정보 API (로그인 토큰 필요) */
interface UserApi {

    /** 내 정보 (닉네임·학교·평점·인증·신고 누적 등) */
    @GET("api/users/me")
    suspend fun getMyProfile(): Response<MyProfileResponse>

    /** 닉네임·휴대폰·마케팅 동의 수정 (보낸 것만 바뀜) */
    @PATCH("api/users/me")
    suspend fun updateMyProfile(@Body request: UserUpdateRequest): Response<MyProfileResponse>

    /** 비밀번호 변경: 다른 기기는 로그아웃되고 이 기기용 새 토큰이 옴 */
    @PATCH("api/users/me/password")
    suspend fun changePassword(@Body request: PasswordChangeRequest): Response<TokenResponse>

    /** 내 출금 계좌 (계좌번호는 가려져서 옴, 없으면 404) */
    @GET("api/users/me/account")
    suspend fun getMyAccount(): Response<AccountResponse>

    /** 출금 계좌 변경 (현재 비밀번호 필요) */
    @PUT("api/users/me/account")
    suspend fun updateMyAccount(@Body request: AccountUpdateRequest): Response<AccountResponse>

    /** 회원 탈퇴 (204) */
    @POST("api/users/me/withdraw")
    suspend fun withdraw(@Body request: MemberWithdrawRequest): Response<Unit>

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
