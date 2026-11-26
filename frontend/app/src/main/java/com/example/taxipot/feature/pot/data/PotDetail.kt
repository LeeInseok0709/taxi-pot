package com.example.taxipot.feature.pot.data

import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotMemberResponse

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