package com.example.taxipot.feature.pot.data

import android.content.Intent
import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse

/**
 * 필터·정렬 조건 — API 명세서 GET /api/pots 의 조건값과 이름을 맞춤
 * - genderOption: ALL(전체) / FEMALE(여성전용) / MALE(남성전용)
 * - minHostRating: 방장 최소 평점 1.0 ~ 10.0 (1.0이면 전체)
 * - sort: DEPARTURE_TIME(출발시간순) / DISTANCE(거리순) / HOST_RATING(평점순)
 */
data class PotFilter(
    val genderOption: String = "ALL",
    val minHostRating: Double = 1.0,
    val sort: String = "DEPARTURE_TIME"
) {

    /** 목록에 조건 적용 (지금은 가짜 데이터라 앱에서 처리, 서버 연결 후에는 서버가 처리) */
    fun apply(pots: List<PotSummaryResponse>): List<PotSummaryResponse> {
        // 1) 조건에 맞는 방만 남기기
        val filtered = pots.filter { pot ->
            (genderOption == "ALL" || pot.genderOption == genderOption) &&
                    pot.hostRatingAvg >= minHostRating
        }
        // 2) 정렬
        val sorted = when (sort) {
            "DISTANCE" -> filtered.sortedBy { it.distanceMeters ?: Int.MAX_VALUE }
            "HOST_RATING" -> filtered.sortedByDescending { it.hostRatingAvg }
            else -> filtered.sortedBy { it.departureTime }
        }
        // 3) 결원 방은 정렬과 관계없이 맨 위 (명세서 규칙)
        return sorted.sortedByDescending { it.isVacancy }
    }

    /** EX-02 정렬 버튼 글자 */
    val sortText: String
        get() = when (sort) {
            "DISTANCE" -> "거리순"
            "HOST_RATING" -> "평점순"
            else -> "출발순"
        }

    /** EX-02 성별 버튼 글자 */
    val genderText: String
        get() = when (genderOption) {
            "FEMALE" -> "여성전용"
            "MALE" -> "남성전용"
            else -> "성별"
        }

    /** 화면 이동할 때 조건을 담아 보내기 */
    fun putInto(intent: Intent): Intent = intent.apply {
        putExtra(EXTRA_GENDER, genderOption)
        putExtra(EXTRA_MIN_RATING, minHostRating)
        putExtra(EXTRA_SORT, sort)
    }

    companion object {
        private const val EXTRA_GENDER = "filter_gender"
        private const val EXTRA_MIN_RATING = "filter_min_rating"
        private const val EXTRA_SORT = "filter_sort"

        /** 화면 이동으로 받은 조건 꺼내기 (없으면 기본값) */
        fun from(intent: Intent?): PotFilter {
            if (intent == null) return PotFilter()
            return PotFilter(
                genderOption = intent.getStringExtra(EXTRA_GENDER) ?: "ALL",
                minHostRating = intent.getDoubleExtra(EXTRA_MIN_RATING, 1.0),
                sort = intent.getStringExtra(EXTRA_SORT) ?: "DEPARTURE_TIME"
            )
        }
    }
}