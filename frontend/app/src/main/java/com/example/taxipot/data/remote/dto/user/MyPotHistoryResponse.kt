package com.example.taxipot.data.remote.dto.user

/** GET /api/users/me/pots 내 택시 이용 기록 한 건 */
data class MyPotHistoryResponse(
    val potId: Long,
    val title: String?,
    val departureName: String?,
    val destinationName: String?,
    val departureTime: String?,
    /** RECRUITING / CLOSED / RUNNING / COMPLETED / CANCELED */
    val potStatus: String,
    /** 실제 요금 (정산 전 null) */
    val actualFare: Int?,
    /** HOST / GUEST */
    val myRole: String?,
    /** JOINED / CANCELED / BOARDED / NO_SHOW / KICKED / REQUESTED / REJECTED */
    val myStatus: String?,
    val heldAmount: Int?,
    /** 분담금 (정산 전·노쇼·환불이면 null) */
    val fareShare: Int?,
    val feeAmount: Int?,
    /** 양수: 환불 / 음수: 추가 차감 */
    val diffAmount: Int?,
    val uncoveredAmount: Int?,
    val debtStatus: String?,
    val debtAmount: Int?,
    /** HELD / SETTLED / REFUNDED */
    val settlementStatus: String?,
    /** 이 방 동승자 평가를 마쳤는지 */
    val isRated: Boolean?
)
