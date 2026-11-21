package com.example.taxipot.feature.pot.main

import android.content.Context
import android.graphics.drawable.GradientDrawable

/**
 * 디자인 색상 모음 (색을 바꾸고 싶으면 여기만 고치면 됨)
 */
object PotColors {
    val BACKGROUND = 0xFFFBF7F0.toInt()  // 화면 배경 (크림색)
    val CARD = 0xFFFFFDF9.toInt()        // 카드 배경
    val LINE = 0xFFD9CBB5.toInt()        // 테두리 선
    val ACCENT = 0xFFF1DEC0.toInt()      // 베이지 (버튼, 상태 칩, 잔액)
    val WHITE = 0xFFFFFFFF.toInt()
    val TEXT = 0xFF2B2B2B.toInt()        // 기본 글자
    val SUB_TEXT = 0xFF6B6B6B.toInt()    // 회색 글자
}

/** dp 단위를 화면 픽셀로 바꾸기 */
fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

/** 둥근 모서리 배경 만들기 (테두리 색을 주면 테두리도 그림) */
fun Context.roundedBg(fillColor: Int, radiusDp: Float, strokeColor: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        setColor(fillColor)
        cornerRadius = dp(radiusDp)
        if (strokeColor != null) {
            setStroke(dp(1f).toInt().coerceAtLeast(1), strokeColor)
        }
    }