package com.example.taxipot.feature.account

/** 계정 화면 표시용 */
object AccountFormat {

    /** 01012345678 → 010-1234-5678 (형식이 다르면 그대로) */
    fun phone(raw: String?): String {
        val digits = raw.orEmpty().filter { it.isDigit() }
        return when (digits.length) {
            11 -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
            10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> raw.orEmpty()
        }
    }
}
