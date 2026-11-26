package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.auth.AvailabilityResponse
import com.example.taxipot.data.remote.dto.auth.LoginRequest
import com.example.taxipot.data.remote.dto.auth.LoginResponse
import com.example.taxipot.data.remote.dto.auth.RefreshRequest
import com.example.taxipot.data.remote.dto.auth.SignupRequest
import com.example.taxipot.data.remote.dto.auth.TokenResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 인증 API (경로 앞에 '/'를 붙이지 않는다. BASE_URL이 '/'로 끝남)
 */
interface AuthApi {

    /** 회원가입. 성공하면 토큰 발급 + 학교 메일로 인증코드 발송 */
    @Headers("No-Auth: true")
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<LoginResponse>

    /** 닉네임 사용 가능 여부 */
    @Headers("No-Auth: true")
    @GET("api/auth/nickname-check")
    suspend fun checkNickname(@Query("nickname") nickname: String): Response<AvailabilityResponse>

    /** 로그인. 실패: 401 INVALID_CREDENTIALS, 403 WITHDRAWN_USER */
    @Headers("No-Auth: true")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /** 로그아웃: 서버의 리프레시 토큰 무효화 (204, 토큰 필요) */
    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    /** 토큰 재발급 - TokenAuthenticator 전용(동기 호출) */
    @Headers("No-Auth: true")
    @POST("api/auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<TokenResponse>
}
