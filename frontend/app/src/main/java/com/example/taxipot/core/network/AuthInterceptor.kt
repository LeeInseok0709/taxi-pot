package com.example.taxipot.core.network

import com.example.taxipot.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 모든 요청에 `Authorization: Bearer {accessToken}`을 붙인다.
 * 로그인·가입·학교 목록처럼 토큰이 필요 없는 API는 인터페이스에
 * `@Headers("No-Auth: true")`를 달면 헤더를 붙이지 않는다.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header(NO_AUTH_HEADER) != null) {
            return chain.proceed(original.newBuilder().removeHeader(NO_AUTH_HEADER).build())
        }

        val token = tokenManager.accessToken ?: return chain.proceed(original)
        return chain.proceed(
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }

    companion object {
        const val NO_AUTH_HEADER = "No-Auth"
    }
}
