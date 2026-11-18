package com.example.taxipot.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.Response

/** 모든 API 호출 결과를 이 세 가지로 받는다 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    /** 서버가 에러 응답을 준 경우. code로 분기 (예: INSUFFICIENT_BALANCE) */
    data class Failure(
        val httpStatus: Int,
        val code: String,
        val message: String,
        val detail: Map<String, Any?>?
    ) : ApiResult<Nothing>

    /** 서버에 닿지 못했거나(인터넷·서버 꺼짐) 응답을 해석하지 못한 경우 */
    data class NetworkError(val throwable: Throwable) : ApiResult<Nothing>
}

/**
 * 사용법:
 *   val result = safeApiCall { api.getUniversities() }
 * API 인터페이스는 항상 `suspend fun xxx(): Response<T>`로 선언한다.
 * 204(본문 없음) API는 `Response<Unit>`으로 선언하면 Success(Unit)이 온다.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.body() ?: Unit as T)
        } else {
            val error = try {
                ApiClient.gson.fromJson(response.errorBody()?.string(), ErrorResponse::class.java)
            } catch (e: Exception) {
                null
            }
            ApiResult.Failure(
                httpStatus = response.code(),
                code = error?.code ?: "UNKNOWN",
                message = error?.message ?: "요청을 처리하지 못했습니다.",
                detail = error?.detail
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.NetworkError(e)
    }
}
