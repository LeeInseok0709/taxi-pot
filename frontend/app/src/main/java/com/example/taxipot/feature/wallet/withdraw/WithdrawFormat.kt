package com.example.taxipot.feature.wallet.withdraw

/** 출금 화면 표시용 */
object WithdrawFormat {

    /** 박지연 → 박*연, 홍길 → 홍* */
    fun maskName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return when (name.length) {
            1 -> name
            2 -> name.first() + "*"
            else -> name.first() + "*".repeat(name.length - 2) + name.last()
        }
    }

    /** KB국민은행 253****5356 (박*연) */
    fun account(bankName: String?, masked: String?, holder: String?): String {
        val name = maskName(holder)
        return listOfNotNull(bankName, masked).joinToString(" ") + if (name.isNotEmpty()) " ($name)" else ""
    }
}
