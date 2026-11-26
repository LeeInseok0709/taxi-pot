package com.example.taxipot.data.remote.dto.verification

/** POST /api/verifications/email-code/confirm 요청 */
data class EmailCodeConfirmRequest(
    val emailCode: String
)

/** POST /api/verifications/email-code (재발송) 응답 */
data class EmailCodeSendResponse(
    val verificationId: Long,
    val email: String,
    /** 새 인증코드 만료 시각 (ISO, 예: 2026-11-26T12:01:00+09:00) */
    val expiresAt: String?
)

/** 인증 진행 상태 (확인·제출·조회 공용 응답) */
data class VerificationResponse(
    val id: Long,
    val studentNo: String?,
    /** PENDING(대기) / VERIFIED(이메일 인증 완료) */
    val emailStatus: String,
    /** NOT_SUBMITTED(캡처 미제출) / PENDING(심사 대기) / APPROVED(승인) / REJECTED(반려) */
    val reviewStatus: String,
    /** 반려 사유 (반려일 때만) */
    val rejectReason: String?,
    val reviewedAt: String?,
    val createdAt: String?
) {
    val isEmailVerified: Boolean get() = emailStatus == EMAIL_VERIFIED

    companion object {
        const val EMAIL_VERIFIED = "VERIFIED"
        const val REVIEW_NOT_SUBMITTED = "NOT_SUBMITTED"
        const val REVIEW_PENDING = "PENDING"
        const val REVIEW_APPROVED = "APPROVED"
        const val REVIEW_REJECTED = "REJECTED"
    }
}
