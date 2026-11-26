package com.example.taxipot.data.remote.dto.settlement

/** GET /api/pots/{potId}/settlement 택시팟 정산 내역 */
data class PotSettlementResponse(
    val potId: Long,
    val expectedFare: Int,
    /** 실제 요금 (정산 전 null) */
    val actualFare: Int?,
    /** 탑승 인원 */
    val boardedCount: Int,
    /** 방장에게 입금된 정산금 (정산 전 null) */
    val hostPayoutAmount: Int?,
    /** 영수증 사진 API 경로 (없으면 null) */
    val receiptUrl: String?,
    val settlements: List<SettlementResponse>
)

/** 회원 한 명의 정산 */
data class SettlementResponse(
    val id: Long,
    val potId: Long,
    val memberId: Long,
    val nickname: String?,
    /** 예치액 */
    val heldAmount: Int,
    /** 분담금 (정산 전·환불이면 null) */
    val fareShare: Int?,
    /** 수수료 (정산 전·환불이면 null) */
    val feeAmount: Int?,
    /** 예치액 − (분담금 + 수수료). 양수: 환불 / 음수: 추가 차감 */
    val diffAmount: Int?,
    /** 잔액 부족으로 못 걷은 금액 (미수금으로 전환) */
    val uncoveredAmount: Int,
    /** NONE / UNPAID / OVERDUE / PAID */
    val debtStatus: String?,
    val debtAmount: Int?,
    val debtDueAt: String?,
    /** HELD(예치 중) / SETTLED(정산 완료) / REFUNDED(전액 환불) */
    val status: String,
    val settledAt: String?,
    val createdAt: String?
)
