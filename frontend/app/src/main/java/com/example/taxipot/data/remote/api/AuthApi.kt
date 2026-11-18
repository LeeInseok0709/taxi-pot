package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.auth.RefreshRequest
import com.example.taxipot.data.remote.dto.auth.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * 인증 API. 로그인·가입·로그아웃은 jy/auth 브랜치에서 suspend 함수로 추가한다.
 * (경로 앞에 '/'를 붙이지 않는다. BASE_URL이 '/'로 끝남)
 */
interface AuthApi {

    /** 토큰 재발급 - TokenAuthenticator 전용(동기 호출) */
    @Headers("No-Auth: true")
    @POST("api/auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<TokenResponse>
}
