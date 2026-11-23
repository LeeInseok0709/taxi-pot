package com.example.taxipot.feature.pot.data

/**
 * 방 하나의 정보 (나중에 서버 응답 GET /api/pots 와 이름을 맞출 예정)
 */
data class PotSummaryResponse(
    val potId: Long,            // 방 번호
    val status: String,         // 상태: "RECRUITING"(모집 중), "VACANCY"(결원 · 모집 중)
    val departureName: String,  // 출발지 이름
    val departureLat: Double,   // 출발지 위도 (핀 위치)
    val departureLng: Double,   // 출발지 경도 (핀 위치)
    val arrivalName: String,    // 도착지 이름
    val departureTime: String,  // 출발 시간
    val currentMembers: Int,    // 현재 인원
    val maxMembers: Int,        // 최대 인원
    val totalFare: Int,         // 예상 총 요금 (원)
    val hostNickname: String,   // 방장 닉네임
    val hostRating: Double,     // 방장 평점
    val distanceMeters: Int     // 내 위치에서 거리 (m)
)

/**
 * 서버 연결 전까지 쓸 가짜 데이터 (백석대학교 주변)
 */
object FakePotData {
    // 백석대학교 위치 (지도 처음 위치, 현위치 버튼 위치로 사용)
    const val BAEKSEOK_LAT = 36.84021
    const val BAEKSEOK_LNG = 127.18322

    // 가짜 잔액 (나중에 지연님 GET /api/wallet 으로 교체)
    const val FAKE_BALANCE = 12300

    val pots = listOf(
        PotSummaryResponse(1, "VACANCY",    "백석대학교",     36.84021, 127.18322, "천안역",     "21:30", 2, 4, 9000,  "택시왕",     7.2, 300),
        PotSummaryResponse(2, "RECRUITING", "백석대학교",     36.83900, 127.18150, "천안아산역", "21:40", 1, 3, 18000, "새벽러",     8.0, 450),
        PotSummaryResponse(3, "RECRUITING", "백석대학교",     36.84150, 127.18500, "천안터미널", "21:45", 3, 4, 7500,  "백석러",     6.1, 600),
        PotSummaryResponse(4, "RECRUITING", "백석대학교",     36.83850, 127.18450, "두정역",     "22:00", 2, 4, 10000, "막차탑승",   5.0, 800),
        PotSummaryResponse(5, "RECRUITING", "백석문화대학교", 36.83780, 127.18246, "천안역",     "22:10", 1, 4, 9000,  "천안토박이", 6.5, 700)
    )
}