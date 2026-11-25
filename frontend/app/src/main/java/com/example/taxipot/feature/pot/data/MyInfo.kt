package com.example.taxipot.feature.pot.data

import android.util.Base64
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import org.json.JSONObject

/**
 * "나"의 회원 번호 알아내기
 * - 실제 서버: 로그인 토큰(JWT) 안의 sub 값 = 내 회원 번호
 * - 가짜 데이터: PotRepository.FAKE_MY_ID (내가 만든 방이면 방장, 참여한 방이면 동승자로 보임)
 */
object MyInfo {

    /** 이 방에서 "나"의 회원 번호 (모르면 null) */
    @Suppress("UNUSED_PARAMETER")
    fun myUserId(pot: PotDetailResponse): Long? {
        if (PotRepository.USE_FAKE) return PotRepository.FAKE_MY_ID
        return userIdFromToken()
    }

    /** 로그인 토큰(JWT)의 가운데 부분을 풀어서 sub(회원 번호) 꺼내기 */
    private fun userIdFromToken(): Long? {
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