package com.mb.tvlauncher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.concurrent.Executors

class WallpaperView(context: Context) : FrameLayout(context) {

    private val names: List<String> = (context.assets.list("wallpapers") ?: emptyArray())
        .filter { it.lowercase().let { n -> n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") } }
        .sorted()
    private val views = arrayOf(ImageView(context), ImageView(context))
    private val handler = Handler(Looper.getMainLooper())
    private val loader = Executors.newSingleThreadExecutor()
    private var index = (System.currentTimeMillis() % maxOf(names.size, 1)).toInt()
    private var front = 0
    private var running = false
    private val interval = 40_000L
    private val fadeMs = 1_500L

    private val advance = object : Runnable {
        override fun run() {
            if (!running) return
            loadNext()
            handler.postDelayed(this, interval)
        }
    }

    init {
        setBackgroundColor(0xFF0A0F1E.toInt())
        if (names.isEmpty()) addView(SceneView(context), LayoutParams(-1, -1))
        for (v in views) {
            v.scaleType = ImageView.ScaleType.CENTER_CROP
            v.alpha = 0f
            addView(v, LayoutParams(-1, -1))
        }
    }

    private fun decode(name: String): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inSampleSize = if (bounds.outWidth > 2400) 2 else 1
        }
        context.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()

    private fun loadNext() {
        if (names.isEmpty()) return
        val name = names[index % names.size]
        index++
        loader.execute {
            val bmp = decode(name) ?: return@execute
            handler.post { if (running) show(bmp) }
        }
    }

    private fun show(bmp: Bitmap) {
        val old = views[front]
        front = 1 - front
        val incoming = views[front]
        incoming.setImageBitmap(bmp)
        incoming.pivotX = width / 2f
        incoming.pivotY = 0f
        incoming.scaleX = 1.07f
        incoming.scaleY = 1.07f
        incoming.alpha = 0f
        incoming.animate().cancel()
        incoming.animate().scaleX(1.15f).scaleY(1.15f).setDuration(interval + 2 * fadeMs).setInterpolator(LinearInterpolator()).start()
        ObjectAnimator.ofFloat(incoming, View.ALPHA, 0f, 1f).setDuration(fadeMs).start()
        ObjectAnimator.ofFloat(old, View.ALPHA, old.alpha, 0f).setDuration(fadeMs).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    old.animate().cancel()
                    old.setImageDrawable(null)
                }
            })
        }.start()
    }

    private fun begin() {
        if (running) return
        running = true
        handler.post(advance)
    }

    private fun halt() {
        running = false
        handler.removeCallbacks(advance)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); begin() }
    override fun onDetachedFromWindow() { halt(); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) begin() else halt()
    }
}
