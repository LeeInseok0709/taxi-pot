package com.example.taxipot.data.remote.dto.wallet

/** POST /api/wallet/withdrawals 요청 */
data class WithdrawalRequest(
    /** 출금 금액 (1,000원 이상, 사용 가능 잔액 이하) */
    val amount: Int
)

/** 출금 결과 (시연은 더미 송금이라 요청 즉시 COMPLETED 또는 FAILED) */
data class WithdrawalResponse(
    val id: Long,
    val amount: Int,
    val bankName: String?,
    val maskedAccountNumber: String?,
    val accountHolder: String?,
    /** DUMMY: 시연용 가상 송금 */
    val transferProvider: String?,
    val transferTid: String?,
    /** REQUESTED / COMPLETED / FAILED */
    val status: String,
    val failReason: String?,
    /** 처리 직후 사용 가능 잔액 (실패면 돌려받은 뒤 잔액) */
    val balanceAfter: Int?,
    val requestedAt: String?,
    val completedAt: String?
)
