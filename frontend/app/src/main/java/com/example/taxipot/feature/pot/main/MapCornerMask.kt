package com.example.taxipot.feature.pot.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 지도 모서리 덮개
 * 카카오 지도는 둥근 카드 안에서도 네모로 그려져서, 네 모서리 바깥만 배경색으로 덮어 둥글게 보이게 함
 * 가운데는 비어 있고 터치도 그대로 지도로 감
 */
class MapCornerMask @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // 카드 모서리(14dp)에서 카드 테두리 두께(1dp)를 뺀 값
    private val radius = context.dp(14f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PotColors.BACKGROUND
        style = Paint.Style.FILL
    }
    // 테두리선 (카드 테두리와 같은 색·두께)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PotColors.LINE
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1f)
    }
    private val strokeRect = RectF()
    private val path = Path()
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.addRect(rect, Path.Direction.CW)                       // 전체 네모
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)  // 둥근 네모는 빼기
        canvas.drawPath(path, paint)                                // → 네 모서리만 칠해짐

        // 맨 위에 둥근 테두리선 (선 두께의 절반만큼 안쪽으로 그려야 잘리지 않음)
        val half = stroke.strokeWidth / 2
        strokeRect.set(half, half, width - half, height - half)
        canvas.drawRoundRect(strokeRect, radius, radius, stroke)
    }
}