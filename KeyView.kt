package com.igor.teclado

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/** Tecla 3D desenhada em Canvas: face arredondada + sombra inferior. */
class KeyView(
    ctx: Context,
    private val key: Key,
    private val onKey: (Key, Boolean) -> Unit
) : View(ctx) {

    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var down = false
    private var handled = false

    private fun sp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private val longPress = Runnable {
        if (down && key.secondary != null) {
            handled = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onKey(key, true)
        }
    }
    private val repeat = object : Runnable {
        override fun run() {
            if (!down) return
            handled = true
            onKey(key, false)
            postDelayed(this, 50)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true; handled = false
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (key.type == Type.DEL) postDelayed(repeat, 400)
                else if (key.secondary != null) postDelayed(longPress, 380)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val out = e.x < 0 || e.y < 0 || e.x > width || e.y > height
                if (out && down) { cancelPress(); handled = true }
            }
            MotionEvent.ACTION_UP -> {
                val fire = down && !handled
                cancelPress()
                if (fire) onKey(key, false)
            }
            MotionEvent.ACTION_CANCEL -> cancelPress()
        }
        return true
    }

    private fun cancelPress() {
        down = false
        removeCallbacks(longPress)
        removeCallbacks(repeat)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPress); removeCallbacks(repeat)
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val l = 3 * d
        val r = width - 3 * d
        val t = 3 * d
        val b = height.toFloat()
        val depth = 4 * d
        val press = if (down) 2.5f * d else 0f
        val rad = 10 * d
        val faceT = t + press
        val faceB = b - depth + press

        val (top, bottom, shadow) = when {
            key.type == Type.ENTER -> Triple(Theme.ENTER_TOP, Theme.ENTER_BOTTOM, Theme.ENTER_SHADOW)
            key.special -> Triple(Theme.SPECIAL_TOP, Theme.SPECIAL_BOTTOM, Theme.SPECIAL_SHADOW)
            else -> Triple(Theme.KEY_TOP, Theme.KEY_BOTTOM, Theme.KEY_SHADOW)
        }

        fill.shader = null
        fill.color = shadow
        c.drawRoundRect(l, t + depth, r, b, rad, rad, fill)

        fill.shader = LinearGradient(0f, faceT, 0f, faceB, top, bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(l, faceT, r, faceB, rad, rad, fill)
        fill.shader = null

        val cx = (l + r) / 2
        val cy = (faceT + faceB) / 2
        val s = 9 * d
        stroke.color = if (key.type == Type.ENTER) Color.WHITE else Theme.INK
        fill.color = stroke.color

        when (key.type) {
            Type.SHIFT -> {
                val p = Path().apply {
                    moveTo(cx, cy - s); lineTo(cx + s, cy); lineTo(cx + s * .45f, cy)
                    lineTo(cx + s * .45f, cy + s * .9f); lineTo(cx - s * .45f, cy + s * .9f)
                    lineTo(cx - s * .45f, cy); lineTo(cx - s, cy); close()
                }
                if (key.state > 0) { fill.style = Paint.Style.FILL; c.drawPath(p, fill) }
                else c.drawPath(p, stroke)
                if (key.state == 2) c.drawLine(cx - s * .45f, cy + s * 1.3f, cx + s * .45f, cy + s * 1.3f, stroke)
            }
            Type.DEL -> {
                val p = Path().apply {
                    moveTo(cx - s * 1.3f, cy); lineTo(cx - s * .4f, cy - s * .85f)
                    lineTo(cx + s * 1.2f, cy - s * .85f); lineTo(cx + s * 1.2f, cy + s * .85f)
                    lineTo(cx - s * .4f, cy + s * .85f); close()
                }
                c.drawPath(p, stroke)
                c.drawLine(cx - s * .1f, cy - s * .35f, cx + s * .65f, cy + s * .35f, stroke)
                c.drawLine(cx - s * .1f, cy + s * .35f, cx + s * .65f, cy - s * .35f, stroke)
            }
            Type.ENTER -> {
                c.drawLine(cx + s, cy - s * .8f, cx + s, cy + s * .2f, stroke)
                c.drawLine(cx + s, cy + s * .2f, cx - s, cy + s * .2f, stroke)
                c.drawLine(cx - s, cy + s * .2f, cx - s * .35f, cy - s * .4f, stroke)
                c.drawLine(cx - s, cy + s * .2f, cx - s * .35f, cy + s * .8f, stroke)
            }
            Type.SPACE -> {
                text.color = Theme.HINT; text.textSize = sp(15f)
                c.drawText(key.label, cx, cy - (text.ascent() + text.descent()) / 2, text)
            }
            else -> {
                text.color = Theme.INK
                text.textSize = sp(if (key.type == Type.SYM || key.label.length > 1) 16f else 24f)
                c.drawText(key.label, cx, cy - (text.ascent() + text.descent()) / 2, text)
            }
        }

        key.secondary?.let {
            text.color = Theme.HINT
            text.textSize = sp(11f)
            text.textAlign = Paint.Align.RIGHT
            c.drawText(it, r - 7 * d, faceT + 15 * d, text)
            text.textAlign = Paint.Align.CENTER
        }
    }
}
