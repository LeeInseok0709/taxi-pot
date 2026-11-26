package com.example.taxipot.core.auth

import android.util.Base64
import com.example.taxipot.core.network.ApiClient
import org.json.JSONObject

/** 지금 로그인한 "나"의 정보 (로그인 토큰 안의 값으로 판단) */
object CurrentUser {

    /** 내 회원 번호 = 토큰(JWT) 가운데 부분의 sub 값. 로그인 전이거나 읽지 못하면 null */
    fun id(): Long? {
        val token = ApiClient.tokenManager.accessToken ?: return null
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            val json = String(Base64.decode(parts[1], flags), Charsets.UTF_8)
            JSONObject(json).optString("sub").toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
