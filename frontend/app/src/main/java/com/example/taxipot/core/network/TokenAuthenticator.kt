package com.example.taxipot.core.network

import com.example.taxipot.core.event.AppEvent
import com.example.taxipot.core.event.AppEventBus
import com.example.taxipot.data.local.TokenManager
import com.example.taxipot.data.remote.api.AuthApi
import com.example.taxipot.data.remote.dto.auth.RefreshRequest
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * 401을 받으면 OkHttp가 자동으로 호출한다.
 * - UNAUTHORIZED(만료): /api/auth/refresh로 재발급 후 원래 요청을 다시 보냄
 * - TOKEN_REVOKED(탈퇴·비밀번호 변경): 재발급하지 않고 토큰 삭제 + 로그인 화면 이동 이벤트
 * - INVALID_CREDENTIALS(로그인 실패): 그대로 화면에 전달
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val refreshApi: AuthApi,
    private val gson: Gson
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 토큰 없이 보낸 요청(로그인 등)은 재발급 대상이 아님
        val sentToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ") ?: return null

        // 재발급 후에도 또 401이면 반복하지 않음
        if (responseCount(response) >= 2) {
            forceLogout()
            return null
        }

        when (response.peekErrorCode(gson)) {
            "TOKEN_REVOKED" -> {
                forceLogout()
                return null
            }
            "INVALID_CREDENTIALS" -> return null
        }

        synchronized(this) {
            // 동시에 401을 받은 다른 요청이 이미 재발급했으면 새 토큰으로 바로 재요청
            val current = tokenManager.accessToken
            if (current != null && current != sentToken) {
                return response.request.withToken(current)
            }

            val refreshToken = tokenManager.refreshToken ?: run {
                forceLogout()
                return null
            }

            val refreshResponse = try {
                refreshApi.refresh(RefreshRequest(refreshToken)).execute()
            } catch (e: IOException) {
                return null // 네트워크 문제는 로그아웃시키지 않음
            }

            val body = refreshResponse.body()
            if (!refreshResponse.isSuccessful || body == null) {
                forceLogout()
                return null
            }

            runBlocking { tokenManager.saveTokens(body.accessToken, body.refreshToken) }
            return response.request.withToken(body.accessToken)
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun forceLogout() {
        runBlocking { tokenManager.clear() }
        AppEventBus.emit(AppEvent.ForceLogout)
    }
}
