package com.example.taxipot.feature.pot.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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

/**
 * 지도 말풍선 이미지 그리기 (베이지 배경 + 테두리 + 동그라미 + 글자)
 * scale: 크기 비율 (카카오맵이 그림을 확대해서 그리므로 1보다 작게)
 */
fun Context.bubbleBitmap(text: String, scale: Float): Bitmap {
    fun s(value: Float) = dp(value) * scale

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PotColors.TEXT
        textSize = s(12f)
        isFakeBoldText = true
    }
    val padH = s(8f)
    val padV = s(6f)
    val icon = s(10f)
    val gap = s(5f)
    val fm = textPaint.fontMetrics
    val textHeight = fm.descent - fm.ascent

    val width = (padH + icon + gap + textPaint.measureText(text) + padH).toInt()
    val height = (padV + textHeight + padV).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rect = RectF(1f, 1f, width - 1f, height - 1f)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PotColors.ACCENT }
    canvas.drawRoundRect(rect, s(6f), s(6f), fill)

    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PotColors.LINE
        style = Paint.Style.STROKE
        strokeWidth = s(1f).coerceAtLeast(1f)
    }
    canvas.drawRoundRect(rect, s(6f), s(6f), stroke)

    val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PotColors.TEXT
        style = Paint.Style.STROKE
        strokeWidth = s(1.5f).coerceAtLeast(1f)
    }
    canvas.drawCircle(padH + icon / 2, height / 2f, icon / 2 - s(1f), pin)

    val baseline = height / 2f - (fm.ascent + fm.descent) / 2
    canvas.drawText(text, padH + icon + gap, baseline, textPaint)

    return bitmap
}