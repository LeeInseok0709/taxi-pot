package com.example.taxipot.data.remote.dto.common

/** GET /api/banks 응답: 출금 은행 */
data class BankResponse(
    /** SignupRequest.bankCode에 보낼 코드 (예: 004) */
    val code: String,
    val name: String
)
