package com.example.taxipot.feature.pot.data

import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse

/** 결원 방인지: 모집 중이면서 빈자리가 생긴 적 있는 방 */
val PotSummaryResponse.isVacancy: Boolean
    get() = status == "RECRUITING" && hasCanceled

/** 출발 시각에서 "21:30"만 꺼내기 */
val PotSummaryResponse.departureClock: String
    get() = clockOf(departureTime)

/** 성별 제한 글자 (제한 없으면 null) */
val PotSummaryResponse.genderLabel: String?
    get() = when (genderOption) {
        "FEMALE" -> "여성전용"
        "MALE" -> "남성전용"
        else -> null
    }

/**
 * 서버 연결 전까지 쓸 가짜 데이터 (백석대학교 주변, 명세서 모양 그대로)
 */
object FakePotData {
    // 백석대학교 위치 (지도 처음 위치, 현위치 버튼 위치로 사용)
    const val BAEKSEOK_LAT = 36.84021
    const val BAEKSEOK_LNG = 127.18322

    // 가짜 잔액 (나중에 지연님 GET /api/wallet 으로 교체)
    const val FAKE_BALANCE = 12300

    private const val DAY = "2026-11-23"

    // 1인 예치액 = floor(예상 요금 × 1.2 ÷ 최대 인원) + 1,000 (명세서 공식)
    private fun hold(fare: Int, max: Int) = fare * 12 / (max * 10) + 1000

    val pots = listOf(
        PotSummaryResponse(1, "천안역 같이 가요", 101, "택시왕", 7.2,
            "백석대학교", 36.84021, 127.18322, "천안역",
            "${DAY}T13:00:00+09:00", 4, 2, "ALL", 9000, hold(9000, 4),
            "${DAY}T12:55:00+09:00", "RECRUITING", true, "${DAY}T12:00:00+09:00", 300),
        PotSummaryResponse(2, "아산역 KTX 타러", 102, "새벽러", 8.0,
            "백석대학교", 36.84450, 127.17900, "천안아산역",
            "${DAY}T13:10:00+09:00", 3, 1, "FEMALE", 18000, hold(18000, 3),
            "${DAY}T13:05:00+09:00", "RECRUITING", false, "${DAY}T12:00:00+09:00", 450),
        PotSummaryResponse(3, "터미널 가실 분", 103, "백석러", 6.1,
            "백석대학교", 36.84400, 127.18850, "천안터미널",
            "${DAY}T13:15:00+09:00", 4, 3, "ALL", 7500, hold(7500, 4),
            "${DAY}T13:10:00+09:00", "RECRUITING", false, "${DAY}T13:10:00+09:00", 600),
        PotSummaryResponse(4, "두정역 가요", 104, "막차탑승", 5.0,
            "백석대학교", 36.83600, 127.18800, "두정역",
            "${DAY}T13:30:00+09:00", 4, 2, "ALL", 10000, hold(10000, 4),
            "${DAY}T13:25:00+09:00", "RECRUITING", false, "${DAY}T13:15:00+09:00", 800),
        PotSummaryResponse(5, "천안역 같이 타요", 105, "천안토박이", 6.5,
            "백석문화대학교", 36.83780, 127.18246, "천안역",
            "${DAY}T13:40:00+09:00", 4, 1, "ALL", 9000, hold(9000, 4),
            "${DAY}T13:35:00+09:00", "RECRUITING", false, "${DAY}T13:20:00+09:00", 700)
    )
}