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
