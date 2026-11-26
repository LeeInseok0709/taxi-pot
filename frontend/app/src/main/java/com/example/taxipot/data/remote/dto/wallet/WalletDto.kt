package com.example.taxipot.data.remote.dto.wallet

/** GET /api/wallet 내 지갑 */
data class WalletResponse(
    val id: Long,
    val memberId: Long,
    /** 바로 쓸 수 있는 잔액 */
    val balance: Int,
    /** 남은 미수금 합계 (없으면 0) */
    val debtAmount: Int,
    /** 선결제로 예치(홀딩)된 금액 */
    val heldBalance: Int,
    val updatedAt: String?
)

/** 포인트 내역 한 줄 (GET /api/wallet/history, PageResponse로 감싸서 옴) */
data class PointHistoryResponse(
    val id: Long,
    val potId: Long?,
    val potTitle: String?,
    val paymentId: Long?,
    /** CHARGE, ADMIN_ADJUST, FARE_HOLD, FARE_REFUND, FARE_PAY, FEE, HOST_PAYOUT, DEBT_PAY, WITHDRAW 등 */
    val type: String,
    /** +입금 / −차감 */
    val amount: Int,
    val balanceAfter: Int,
    val createdAt: String?
)

/** GET /api/wallet/debts 미수금 한 건 */
data class DebtResponse(
    val settlementId: Long,
    val potId: Long,
    val potTitle: String?,
    val settledAt: String?,
    /** 정산 당시 못 걷은 금액 */
    val uncoveredAmount: Int,
    /** UNPAID / OVERDUE / PAID */
    val debtStatus: String,
    /** 지금 내야 할 남은 금액 */
    val debtAmount: Int,
    val debtDueAt: String?
)

/** POST /api/wallet/debts/{settlementId}/pay 응답 */
data class DebtPayResponse(
    val settlementId: Long,
    /** 이번에 납부한 금액 */
    val paidAmount: Int,
    val debtStatus: String,
    /** 납부 후 남은 금액 (0이면 완납) */
    val debtAmount: Int,
    val balanceAfter: Int
)
