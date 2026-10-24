package com.mb.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.view.View
import java.util.Random

class SceneView(context: Context) : View(context) {

    private var scene: Bitmap? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = Rect()
    private var offset = 0f
    private var running = false
    private val step = object : Runnable {
        override fun run() {
            if (!running) return
            offset += 0.6f
            invalidate()
            postDelayed(this, 60)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val bw = minOf(w, 1280)
        val bh = bw * h / w
        scene = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also { draw(Canvas(it), bw.toFloat(), bh.toFloat()) }
    }

    private fun ridge(rnd: Random, points: Int, rough: Float): FloatArray {
        val a = FloatArray(points + 1)
        a[0] = rnd.nextFloat() - 0.5f
        a[points] = rnd.nextFloat() - 0.5f
        var span = points
        var amp = 1f
        while (span > 1) {
            val half = span / 2
            var i = half
            while (i < points) {
                a[i] = (a[i - half] + a[i + half]) / 2f + (rnd.nextFloat() - 0.5f) * amp
                i += span
            }
            amp *= rough
            span = half
        }
        return a
    }

    private fun drawLayer(c: Canvas, w: Float, h: Float, seed: Long, base: Float, amp: Float, rough: Float, top: Int, bottom: Int) {
        val n = 256
        val r = ridge(Random(seed), n, rough)
        val path = Path()
        path.moveTo(0f, h)
        for (i in 0..n) path.lineTo(w * i / n, h * base - r[i] * amp * h)
        path.lineTo(w, h)
        path.close()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, h * (base - amp), 0f, h, top, bottom, Shader.TileMode.CLAMP)
        c.drawPath(path, p)
    }

    private fun draw(c: Canvas, w: Float, h: Float) {
        val sky = Paint()
        sky.shader = LinearGradient(0f, 0f, 0f, h * 0.6f, 0xFF2E3650.toInt(), 0xFF6B5A72.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, sky)
        drawLayer(c, w, h, 11, 0.50f, 0.20f, 0.55f, 0xFF5A6080.toInt(), 0xFF3A3A58.toInt())
        drawLayer(c, w, h, 23, 0.60f, 0.22f, 0.55f, 0xFF7A3A52.toInt(), 0xFF3A1C30.toInt())
        drawLayer(c, w, h, 37, 0.72f, 0.22f, 0.50f, 0xFFA5333F.toInt(), 0xFF2A1020.toInt())
        drawLayer(c, w, h, 53, 0.88f, 0.16f, 0.50f, 0xFF28121C.toInt(), 0xFF0A060A.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val b = scene ?: return
        dst.set(0, 0, width, height)
        canvas.drawBitmap(b, null, dst, bitmapPaint)
        val w = width.toFloat()
        val h = height.toFloat()
        val specs = arrayOf(
            floatArrayOf(0.15f, 0.12f, 0.30f, 0.22f),
            floatArrayOf(0.55f, 0.20f, 0.36f, 0.16f),
            floatArrayOf(0.85f, 0.09f, 0.26f, 0.14f),
            floatArrayOf(0.35f, 0.28f, 0.40f, 0.12f)
        )
        for ((i, s) in specs.withIndex()) {
            val speed = 1f + i * 0.5f
            val cw = w * s[2]
            val cx = ((w * s[0] + offset * speed * 3f) % (w + cw * 2f)) - cw
            val cy = h * s[1]
            val ch = h * 0.07f
            val rad = cw / 2f
            cloudPaint.shader = RadialGradient(cx, cy, rad, Color.argb((s[3] * 255).toInt(), 200, 210, 235), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.save()
            canvas.scale(1f, ch / rad, cx, cy)
            canvas.drawCircle(cx, cy, rad, cloudPaint)
            canvas.restore()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        post(step)
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(step)
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && !running) { running = true; post(step) }
        else if (visibility != VISIBLE) { running = false; removeCallbacks(step) }
    }
}
