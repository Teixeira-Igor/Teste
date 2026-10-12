package com.igor.teclado

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Ícones da barra superior: grade de pontos e seta para esconder o teclado. */
class IconView(ctx: Context, private val kind: Int) : View(ctx) {
    companion object { const val GRID = 0; const val CHEVRON = 1 }

    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.HINT
        strokeWidth = 2.5f * d
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        if (kind == GRID) {
            p.style = Paint.Style.FILL
            for (i in 0 until 3) for (j in 0 until 5)
                c.drawCircle(cx + (j - 2) * 5 * d, cy + (i - 1) * 5 * d, 1.3f * d, p)
        } else {
            p.style = Paint.Style.STROKE
            val path = Path().apply {
                moveTo(cx - 8 * d, cy - 3 * d); lineTo(cx, cy + 4 * d); lineTo(cx + 8 * d, cy - 3 * d)
            }
            c.drawPath(path, p)
        }
    }
}
