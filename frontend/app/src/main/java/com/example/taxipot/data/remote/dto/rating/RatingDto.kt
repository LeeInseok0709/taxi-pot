package com.example.taxipot.data.remote.dto.rating

/** GET /api/pots/{potId}/ratings/targets 평가할 동승자 */
data class RatingTargetResponse(
    val memberId: Long,
    val nickname: String?,
    /** HOST / GUEST */
    val role: String?,
    /** 이미 평가했는지 (24시간 뒤 자동 기본 평가가 들어가도 true) */
    val isRated: Boolean
)

/** 한 사람에 대한 평가 (각 항목 1~5점) */
data class RatingItem(
    val revieweeId: Long,
    /** 시간 약속 */
    val noshowScore: Int,
    /** 언행·태도 */
    val mannerScore: Int,
    /** 연락 */
    val communicationScore: Int,
    /** 정산 */
    val paymentScore: Int,
    /** 4항목 평균 3점 미만이면 필수 */
    val badReason: String? = null
)

/** POST /api/pots/{potId}/ratings 요청 (한 번에 1~3명) */
data class RatingCreateRequest(
    val ratings: List<RatingItem>
)
