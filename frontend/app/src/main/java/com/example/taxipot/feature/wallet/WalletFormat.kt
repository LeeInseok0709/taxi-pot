package com.example.taxipot.feature.wallet

import java.text.SimpleDateFormat
import java.util.Locale

/** 지갑 화면 표시용 도우미 */
object WalletFormat {

    /** 12300 → 12,300P */
    fun point(amount: Int): String = String.format(Locale.KOREA, "%,dP", amount)

    /** +10,000P / −3,700P */
    fun signedPoint(amount: Int): String = when {
        amount > 0 -> "+" + point(amount)
        amount < 0 -> "−" + point(-amount)
        else -> point(0)
    }

    /** 내역 종류 이름 */
    fun typeLabel(type: String): String = when (type) {
        "CHARGE" -> "충전"
        "ADMIN_ADJUST" -> "충전"
        "CHARGE_REFUND" -> "충전 환불"
        "FARE_HOLD" -> "예치"
        "FARE_REFUND" -> "정산 환불"
        "FARE_ADJUST" -> "추가 차감"
        "FARE_PAY" -> "택시비 결제"
        "FEE" -> "수수료"
        "HOST_PAYOUT" -> "정산금 입금"
        "DEBT_PAY" -> "미수금 납부"
        "BALANCE_EXPIRE" -> "잔액 소멸"
        "WITHDRAW" -> "출금"
        "WITHDRAW_REFUND" -> "출금 복구"
        else -> type
    }

    /** 2026-11-26T12:10:30.123+09:00 → 11.26 12:10 (읽지 못하면 빈 문자열) */
    fun shortDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val cleaned = iso.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(cleaned) ?: return ""
            SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(date)
        } catch (e: Exception) {
            ""
        }
    }
}
