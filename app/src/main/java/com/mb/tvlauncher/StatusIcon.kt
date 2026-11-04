package com.mb.tvlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

class StatusIcon(context: Context, private val kind: Int) : View(context) {

    companion object {
        const val WIFI = 0
        const val LAN = 1
        const val BLUETOOTH = 2
        const val GEAR = 3
        const val VOLUME = 4
        const val INPUT = 5
        const val HDMI = 6
        const val AV = 7
        const val VGA = 8
        const val ANTENNA = 9
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.strokeWidth = w * 0.09f
        when (kind) {
            WIFI -> {
                val cx = w / 2f
                val cy = h * 0.82f
                for (i in 1..3) {
                    val r = w * 0.2f * i
                    c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 225f, 90f, false, paint)
                }
                c.drawCircle(cx, cy, w * 0.06f, fill)
            }
            LAN -> {
                val s = w * 0.26f
                c.drawRect(w / 2 - s / 2, h * 0.08f, w / 2 + s / 2, h * 0.08f + s, paint)
                c.drawRect(w * 0.04f, h * 0.68f, w * 0.04f + s, h * 0.68f + s, paint)
                c.drawRect(w * 0.96f - s, h * 0.68f, w * 0.96f, h * 0.68f + s, paint)
                c.drawLine(w / 2, h * 0.08f + s, w / 2, h * 0.52f, paint)
                c.drawLine(w * 0.04f + s / 2, h * 0.68f, w * 0.04f + s / 2, h * 0.52f, paint)
                c.drawLine(w * 0.96f - s / 2, h * 0.68f, w * 0.96f - s / 2, h * 0.52f, paint)
                c.drawLine(w * 0.04f + s / 2, h * 0.52f, w * 0.96f - s / 2, h * 0.52f, paint)
            }
            GEAR -> {
                val cx = w / 2f
                val cy = h / 2f
                val teeth = 8
                val outer = w * 0.48f
                val inner = w * 0.36f
                val gear = Path()
                gear.fillType = Path.FillType.EVEN_ODD
                val steps = teeth * 4
                for (i in 0 until steps) {
                    val a = Math.PI * 2 * i / steps
                    val r = if ((i % 4) == 1 || (i % 4) == 2) outer else inner
                    val x = (cx + Math.cos(a) * r).toFloat()
                    val y = (cy + Math.sin(a) * r).toFloat()
                    if (i == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
                }
                gear.close()
                gear.addCircle(cx, cy, w * 0.16f, Path.Direction.CW)
                c.drawPath(gear, fill)
            }
            VOLUME -> {
                val sp = Path()
                sp.moveTo(w * 0.08f, h * 0.38f)
                sp.lineTo(w * 0.28f, h * 0.38f)
                sp.lineTo(w * 0.50f, h * 0.18f)
                sp.lineTo(w * 0.50f, h * 0.82f)
                sp.lineTo(w * 0.28f, h * 0.62f)
                sp.lineTo(w * 0.08f, h * 0.62f)
                sp.close()
                c.drawPath(sp, fill)
                for (i in 1..2) {
                    val r = w * 0.17f * i
                    c.drawArc(RectF(w * 0.5f - r, h / 2 - r, w * 0.5f + r, h / 2 + r), -45f, 90f, false, paint)
                }
            }
            INPUT -> {
                // A screen with an arrow coming in from the left (TV source / input).
                val r = w * 0.10f
                c.drawRoundRect(RectF(w * 0.34f, h * 0.16f, w * 0.94f, h * 0.84f), r, r, paint)
                val cy = h / 2f
                c.drawLine(w * 0.06f, cy, w * 0.64f, cy, paint)
                val head = Path()
                head.moveTo(w * 0.48f, cy - h * 0.16f)
                head.lineTo(w * 0.64f, cy)
                head.lineTo(w * 0.48f, cy + h * 0.16f)
                c.drawPath(head, paint)
            }
            HDMI -> {
                val p = Path()
                p.moveTo(w * 0.08f, h * 0.28f); p.lineTo(w * 0.92f, h * 0.28f); p.lineTo(w * 0.92f, h * 0.54f)
                p.lineTo(w * 0.76f, h * 0.76f); p.lineTo(w * 0.24f, h * 0.76f); p.lineTo(w * 0.08f, h * 0.54f); p.close()
                c.drawPath(p, paint)
                c.drawLine(w * 0.3f, h * 0.42f, w * 0.7f, h * 0.42f, paint)
            }
            AV -> {
                val old = fill.color
                val cy = h * 0.5f; val r = w * 0.14f
                fill.color = Color.rgb(255, 214, 10); c.drawCircle(w * 0.2f, cy, r, fill)
                fill.color = Color.WHITE; c.drawCircle(w * 0.5f, cy, r, fill)
                fill.color = Color.rgb(255, 69, 58); c.drawCircle(w * 0.8f, cy, r, fill)
                fill.color = old
            }
            VGA -> {
                val p = Path()
                p.moveTo(w * 0.06f, h * 0.26f); p.lineTo(w * 0.94f, h * 0.26f); p.lineTo(w * 0.80f, h * 0.74f); p.lineTo(w * 0.20f, h * 0.74f); p.close()
                c.drawPath(p, paint)
                for (i in 0 until 5) c.drawCircle(w * (0.22f + 0.14f * i), h * 0.42f, w * 0.03f, fill)
                for (i in 0 until 4) c.drawCircle(w * (0.29f + 0.14f * i), h * 0.58f, w * 0.03f, fill)
            }
            ANTENNA -> {
                c.drawLine(w * 0.5f, h * 0.34f, w * 0.26f, h * 0.92f, paint)
                c.drawLine(w * 0.5f, h * 0.34f, w * 0.74f, h * 0.92f, paint)
                c.drawLine(w * 0.34f, h * 0.72f, w * 0.66f, h * 0.72f, paint)
                c.drawCircle(w * 0.5f, h * 0.28f, w * 0.05f, fill)
                for (i in 1..2) {
                    val r = w * 0.16f * i
                    c.drawArc(RectF(w * 0.5f - r, h * 0.28f - r, w * 0.5f + r, h * 0.28f + r), 205f, 130f, false, paint)
                }
            }
            else -> {
                val p = Path()
                p.moveTo(w * 0.28f, h * 0.30f)
                p.lineTo(w * 0.72f, h * 0.70f)
                p.lineTo(w * 0.50f, h * 0.90f)
                p.lineTo(w * 0.50f, h * 0.10f)
                p.lineTo(w * 0.72f, h * 0.30f)
                p.lineTo(w * 0.28f, h * 0.70f)
                c.drawPath(p, paint)
            }
        }
    }
}
