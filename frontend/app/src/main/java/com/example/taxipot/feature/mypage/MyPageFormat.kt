package com.example.taxipot.feature.mypage

import java.util.Locale

/** 마이페이지 표시용 */
object MyPageFormat {

    /** 7.2 → ★7.20 / 10 */
    fun rating(avg: Double?): String =
        "★" + String.format(Locale.US, "%.2f", avg ?: 0.0) + " / 10"

    /**
     * 신고 누적 안내 (명세서: 3회 3일 / 6회 7일 / 10회 영구 정지)
     * 예) 누적 1회, 2회 더 → "2회 더 받으면 3일 정지"
     */
    fun nextSuspension(reportCount: Int, nextIn: Int?): String? {
        if (nextIn == null || nextIn <= 0) return null
        val period = when (reportCount + nextIn) {
            3 -> "3일"
            6 -> "7일"
            10 -> "영구"
            else -> null
        }
        return when (period) {
            null -> "${nextIn}회 더 받으면 이용이 정지돼요"
            "영구" -> "${nextIn}회 더 받으면 영구 정지"
            else -> "${nextIn}회 더 받으면 $period 정지"
        }
    }

    /** "2026-11-26T12:10:30+09:00" → "11/26" */
    fun monthDay(iso: String?): String {
        if (iso.isNullOrBlank() || iso.length < 10) return ""
        return iso.substring(5, 7).trimStart('0') + "/" + iso.substring(8, 10)
    }
}
