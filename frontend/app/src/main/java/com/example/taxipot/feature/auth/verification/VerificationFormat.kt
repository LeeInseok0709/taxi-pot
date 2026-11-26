package com.example.taxipot.feature.auth.verification

import java.text.SimpleDateFormat
import java.util.Locale

/** 인증 화면에서 쓰는 작은 도우미 */
object VerificationFormat {

    /** 서버 시각(ISO, 예: 2026-11-26T12:01:00.123+09:00)을 밀리초로. 읽지 못하면 null */
    fun parseIsoMillis(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        return try {
            val cleaned = text.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(cleaned)?.time
        } catch (e: Exception) {
            null
        }
    }

    /** ab1234@bu.ac.kr → ab***@bu.ac.kr */
    fun maskEmail(email: String?): String? {
        if (email.isNullOrBlank()) return null
        val at = email.indexOf('@')
        if (at <= 0) return email
        return email.substring(0, at).take(2) + "***" + email.substring(at)
    }

    /** 299 → 04:59 */
    fun formatMmSs(seconds: Int): String =
        String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}
