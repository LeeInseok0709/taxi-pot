package com.example.taxipot.data.remote.dto.pot

/**
 * 택시팟 목록 1건 — API 명세서 3.12.0 PotSummaryResponse 와 이름·모양을 똑같이 맞춤
 * (GET /api/pots 응답의 content 안에 들어오는 값)
 */
data class PotSummaryResponse(
    val id: Long,                  // 방 번호
    val title: String,             // 방 제목
    val hostId: Long,              // 방장 회원 번호
    val hostNickname: String,      // 방장 닉네임
    val hostRatingAvg: Double,     // 방장 평점 (10점 만점)
    val departureName: String,     // 출발지 이름
    val departureLat: Double,      // 출발지 위도 (지도 마커)
    val departureLng: Double,      // 출발지 경도 (지도 마커)
    val destinationName: String,   // 도착지 이름
    val departureTime: String,     // 출발 시각 (예: 2026-11-23T13:00:00+09:00)
    val maxMembers: Int,           // 최대 인원 (3 또는 4)
    val currentMembers: Int,       // 현재 인원
    val genderOption: String,      // 성별 제한: ALL / MALE / FEMALE
    val expectedFare: Int,         // 예상 총 요금 (원)
    val holdAmount: Int,           // 참여 시 1인 예치액 (원)
    val joinDeadline: String,      // 참여 마감 시각 (출발 5분 전)
    val status: String,            // 방 상태: RECRUITING / CLOSED / RUNNING / COMPLETED / CANCELED
    val hasCanceled: Boolean,      // 참여자 이탈로 빈자리가 생긴 적 있음 (결원)
    val createdAt: String,         // 방 생성 시각
    val distanceMeters: Int?       // 내 위치에서 거리 (위치로 조회했을 때만 값이 있음)
)

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

/**
 * 방 만들기 요청 — API 명세서 PotCreateRequest (POST /api/pots)
 * 값이 null인 항목(선택 항목)은 서버로 보내지 않음
 */
data class PotCreateRequest(
    val title: String,              // 방 제목 (100자 이하)
    val departureName: String,      // 출발지 이름
    val departureLat: Double,
    val departureLng: Double,
    val destinationName: String,    // 도착지 이름
    val destinationLat: Double,
    val destinationLng: Double,
    val departureTime: String,      // 출발 시각 "2026-11-25T21:30:00+09:00" (15분 후 ~ 24시간 이내)
    val maxMembers: Int,            // 3 또는 4
    val genderOption: String?,      // ALL / MALE / FEMALE (선택)
    val expectedFare: Int,          // 예상 총 요금 (0 ~ 500,000원)
    val openChatUrl: String?,       // 오픈채팅 링크 (선택)
    val preferredSeat: String?      // FRONT / BACK_LEFT / BACK_MID / BACK_RIGHT / NONE (선택)
)

/** 택시팟 참여 요청 — 명세서 JoinRequest (POST /api/pots/{potId}/members) */
data class PotJoinRequest(
    val preferredSeat: String?      // FRONT / BACK_LEFT / BACK_MID / BACK_RIGHT / NONE (선택)
)

/** 택시팟 참여 결과 — 명세서 JoinResponse */
data class PotJoinResponse(
    val potId: Long,
    val member: PotMemberResponse?, // 내 참여 정보
    val heldAmount: Int,            // 예치된 금액 (예상 분담금 + 수수료)
    val balanceAfter: Int,          // 예치 후 사용 가능 잔액
    val potStatus: String?          // 참여 후 방 상태 (다 차면 CLOSED)
)

/** 도착 확인 요청 — 명세서 ArrivalRequest (내 GPS 위치) */
data class PotArrivalRequest(
    val lat: Double,
    val lng: Double
)

/** 도착 확인 결과 — 명세서 ArrivalResponse */
data class PotArrivalResponse(
    val potId: Long,
    val arrivedAt: String,
    val distanceMeters: Int,    // 출발지까지 거리 (m)
    val arrivedCount: Int,      // 도착한 참여자 수 (방장 포함)
    val totalMembers: Int,      // 현재 참여 인원
    val allArrived: Boolean     // 전원 도착 여부
)