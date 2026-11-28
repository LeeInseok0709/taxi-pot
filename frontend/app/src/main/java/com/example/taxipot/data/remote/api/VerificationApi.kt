package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.verification.EmailCodeConfirmRequest
import com.example.taxipot.data.remote.dto.verification.EmailCodeSendResponse
import com.example.taxipot.data.remote.dto.verification.VerificationResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** 학생 인증 API (로그인 토큰 필요) */
interface VerificationApi {

    /** 인증코드 재발송. 직전 발송 후 60초 안이면 429 TOO_MANY_REQUESTS */
    @POST("api/verifications/email-code")
    suspend fun resendEmailCode(): Response<EmailCodeSendResponse>

    /** 인증코드 확인 (1차 인증) */
    @POST("api/verifications/email-code/confirm")
    suspend fun confirmEmailCode(@Body request: EmailCodeConfirmRequest): Response<VerificationResponse>

    /** 학생 기본 정보 캡처본 제출 (2차 인증, 반려 후 재제출 포함) */
    @Multipart
    @POST("api/verifications/capture")
    suspend fun submitCapture(
        @Part captureImage: MultipartBody.Part,
        @Part("studentNo") studentNo: RequestBody
    ): Response<VerificationResponse>

    /** 내 인증 진행 상태 */
    @GET("api/verifications/me")
    suspend fun getMyVerification(): Response<VerificationResponse>
}
