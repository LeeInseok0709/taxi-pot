package com.example.taxipot.feature.pot.data

/**
 * 참여자 1명 — API 명세서 PotMemberResponse 와 같은 모양
 */
data class PotMemberResponse(
    val id: Long,               // 참여 고유번호
    val memberId: Long,         // 회원 번호
    val nickname: String,       // 닉네임
    val ratingAvg: Double,      // 평점
    val role: String,           // HOST(방장) / GUEST(동승자)
    val preferredSeat: String,  // FRONT / BACK_LEFT / BACK_MID / BACK_RIGHT / NONE
    val status: String,         // JOINED / BOARDED / CANCELED / NO_SHOW / KICKED / REQUESTED / REJECTED
    val arrivedAt: String?,     // 도착 확인 시각 (없으면 null)
    val joinedAt: String,       // 참여 시각
    val canceledAt: String?     // 취소·퇴장 시각 (없으면 null)
)

/**
 * 택시팟 상세 — API 명세서 PotDetailResponse 와 같은 모양
 * (GET /api/pots/{potId} 응답)
 */
data class PotDetailResponse(
    val id: Long,
    val title: String,
    val hostId: Long,
    val hostNickname: String,
    val hostRatingAvg: Double,
    val departureName: String,
    val departureLat: Double,
    val departureLng: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val departureTime: String,      // 출발 시각
    val arrivalDeadline: String,    // 집결 기한 겸 참여 마감 (출발 5분 전)
    val cancelDeadline: String,     // 참여 취소 마감 (출발 10분 전)
    val maxMembers: Int,
    val currentMembers: Int,
    val genderOption: String,       // ALL / MALE / FEMALE
    val expectedFare: Int,
    val holdAmount: Int,            // 1인 예치액
    val actualFare: Int?,           // 실제 요금 (정산 전 null)
    val status: String,             // RECRUITING / CLOSED / RUNNING / COMPLETED / CANCELED
    val hasCanceled: Boolean,       // 결원 발생 여부
    val openChatUrl: String?,
    val isJoined: Boolean,          // 내가 참여 중인지
    val myMemberStatus: String?,    // 내 참여 상태 (참여 이력 없으면 null)
    val hasReceipt: Boolean,
    val fareClaimStatus: String,    // NONE / PENDING / APPROVED / REJECTED / OVERDUE
    val fareRejectReason: String?,
    val members: List<PotMemberResponse>,
    val createdAt: String,
    val updatedAt: String
)

/** "2026-11-23T21:30:00+09:00" → "21:30" */
fun clockOf(time: String): String =
    if (time.length >= 16) time.substring(11, 16) else time

/**
 * 서버 연결 전까지 쓸 가짜 상세 데이터
 * 목록 가짜 데이터(FakePotData)를 바탕으로 상세 정보를 만들어 줌
 */
object FakePotDetailData {

    // 도착지 좌표 (가짜, 대략적인 위치)
    private val destinations = mapOf(
        "천안역" to Pair(36.8101, 127.1464),
        "천안아산역" to Pair(36.7945, 127.1046),
        "천안터미널" to Pair(36.8197, 127.1566),
        "두정역" to Pair(36.8335, 127.1491)
    )

    // 동승자 가짜 닉네임
    private val guestNames = listOf("새내기", "천안러", "막차러")

    /** 시각 문자열에서 n분 빼기 ("…T21:30:00+09:00", 10 → "…T21:20:00+09:00") */
    private fun minusMinutes(time: String, n: Int): String {
        val hour = time.substring(11, 13).toInt()
        val minute = time.substring(14, 16).toInt()
        val total = hour * 60 + minute - n
        val newClock = "%02d:%02d".format(total / 60, total % 60)
        return time.substring(0, 11) + newClock + time.substring(16)
    }

    /** 방 번호로 상세 정보 찾기 (없으면 null) */
    fun find(potId: Long): PotDetailResponse? {
        val pot = FakePotData.pots.find { it.id == potId } ?: return null
        val dest = destinations[pot.destinationName] ?: Pair(36.8101, 127.1464)

        // 참여자: 방장 1명 + 나머지 동승자
        val host = PotMemberResponse(
            pot.id * 10, pot.hostId, pot.hostNickname, pot.hostRatingAvg,
            "HOST", "NONE", "JOINED", null, pot.createdAt, null
        )
        val guests = (1 until pot.currentMembers).map { i ->
            PotMemberResponse(
                pot.id * 10 + i, 200L + i, guestNames[(i - 1) % guestNames.size], 5.0 + i * 0.5,
                "GUEST", "NONE", "JOINED", null, pot.createdAt, null
            )
        }

        return PotDetailResponse(
            id = pot.id,
            title = pot.title,
            hostId = pot.hostId,
            hostNickname = pot.hostNickname,
            hostRatingAvg = pot.hostRatingAvg,
            departureName = pot.departureName,
            departureLat = pot.departureLat,
            departureLng = pot.departureLng,
            destinationName = pot.destinationName,
            destinationLat = dest.first,
            destinationLng = dest.second,
            departureTime = pot.departureTime,
            arrivalDeadline = pot.joinDeadline,
            cancelDeadline = minusMinutes(pot.departureTime, 10),
            maxMembers = pot.maxMembers,
            currentMembers = pot.currentMembers,
            genderOption = pot.genderOption,
            expectedFare = pot.expectedFare,
            holdAmount = pot.holdAmount,
            actualFare = null,
            status = pot.status,
            hasCanceled = pot.hasCanceled,
            openChatUrl = null,
            isJoined = false,
            myMemberStatus = null,
            hasReceipt = false,
            fareClaimStatus = "NONE",
            fareRejectReason = null,
            members = listOf(host) + guests,
            createdAt = pot.createdAt,
            updatedAt = pot.createdAt
        )
    }
}