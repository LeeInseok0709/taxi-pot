package com.example.taxipot.core.network

import com.google.gson.Gson
import okhttp3.Response

/** 서버 공통 에러 응답 (API 명세서 ErrorResponse) */
data class ErrorResponse(
    val code: String,
    val message: String,
    /** 에러별 추가 정보. Gson은 숫자를 Double로 읽으므로 꺼낼 때 toLong() 등으로 변환 */
    val detail: Map<String, Any?>?,
    val timestamp: String?
)

/** OkHttp 응답 본문을 소비하지 않고(peek) 에러 코드만 읽는다 */
internal fun Response.peekErrorCode(gson: Gson): String? = try {
    val body = peekBody(64 * 1024).string()
    gson.fromJson(body, ErrorResponse::class.java)?.code
} catch (e: Exception) {
    null
}
