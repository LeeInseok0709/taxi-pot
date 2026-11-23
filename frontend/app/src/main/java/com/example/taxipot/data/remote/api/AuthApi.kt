package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.auth.LoginRequest
import com.example.taxipot.data.remote.dto.auth.LoginResponse
import com.example.taxipot.data.remote.dto.auth.RefreshRequest
import com.example.taxipot.data.remote.dto.auth.TokenResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * 인증 API (경로 앞에 '/'를 붙이지 않는다. BASE_URL이 '/'로 끝남)
 */
interface AuthApi {

    /** 로그인. 실패: 401 INVALID_CREDENTIALS, 403 WITHDRAWN_USER */
    @Headers("No-Auth: true")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /** 토큰 재발급 - TokenAuthenticator 전용(동기 호출) */
    @Headers("No-Auth: true")
    @POST("api/auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<TokenResponse>
}
