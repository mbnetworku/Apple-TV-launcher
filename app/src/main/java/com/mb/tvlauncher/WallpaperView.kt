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

    companion object {
        // Settings > Wallpaper writes these to the "home" preferences.
        const val PREF_FIXED = "wp_fixed"       // file name of the one wallpaper to keep, or absent = rotate all
        const val PREF_INTERVAL = "wp_interval" // ms between wallpapers when rotating
        const val PREF_ZOOM = "wp_zoom"         // slow zoom while a wallpaper is shown
        const val DEFAULT_INTERVAL = 40_000L
        const val FIXED_OFF = "off"             // PREF_FIXED value: no wallpaper, plain dark background

        fun isOff(context: Context) =
            context.getSharedPreferences("home", Context.MODE_PRIVATE).getString(PREF_FIXED, null) == FIXED_OFF

        fun wallpaperNames(context: Context): List<String> = (context.assets.list("wallpapers") ?: emptyArray())
            .filter { it.lowercase().let { n -> n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") } }
            .sorted()
    }

    private val names: List<String> = wallpaperNames(context)
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)
    private val views = arrayOf(ImageView(context), ImageView(context))
    private val handler = Handler(Looper.getMainLooper())
    private val loader = Executors.newSingleThreadExecutor()
    private var index = (System.currentTimeMillis() % maxOf(names.size, 1)).toInt()
    private var front = 0
    private var running = false
    private var interval = DEFAULT_INTERVAL
    private var zoom = true
    private var fixed: String? = null
    private var off = false
    private var shownFixed: String? = null
    private var scene: View? = null
    private val fadeMs = 1_500L

    private fun readPrefs() {
        interval = prefs.getLong(PREF_INTERVAL, DEFAULT_INTERVAL).coerceAtLeast(5_000L)
        zoom = prefs.getBoolean(PREF_ZOOM, true)
        val f = prefs.getString(PREF_FIXED, null)
        off = f == FIXED_OFF
        fixed = f?.takeIf { it in names }
    }

    // Wallpaper off: drop the pictures (frees their memory) and show only the plain background.
    private fun applyOff() {
        scene?.visibility = if (off) GONE else VISIBLE
        if (off) for (v in views) { v.animate().cancel(); v.alpha = 0f; v.setImageDrawable(null) }
    }

    private val advance = object : Runnable {
        override fun run() {
            if (!running) return
            if (off) { shownFixed = null; return }
            val one = fixed
            if (one != null) {
                // One chosen wallpaper: show it once and stop rotating.
                if (shownFixed != one) { shownFixed = one; load(one) }
                return
            }
            shownFixed = null
            loadNext()
            handler.postDelayed(this, interval)
        }
    }

    // Settings calls this after a change so the new choice shows straight away.
    fun reload() {
        halt()
        shownFixed = null
        if (isAttachedToWindow && windowVisibility == VISIBLE) begin()
    }

    init {
        setBackgroundColor(0xFF0A0F1E.toInt())
        if (names.isEmpty()) scene = SceneView(context).also { addView(it, LayoutParams(-1, -1)) }
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
        load(name)
    }

    private fun load(name: String) {
        loader.execute {
            val bmp = decode(name) ?: return@execute
            handler.post { if (running && !off) show(bmp) }
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
        if (zoom) {
            // A fixed wallpaper zooms slowly over 2 minutes; a rotating one over its time on screen.
            val span = if (fixed != null) 120_000L else interval + 2 * fadeMs
            incoming.animate().scaleX(1.15f).scaleY(1.15f).setDuration(span).setInterpolator(LinearInterpolator()).start()
        } else {
            incoming.scaleX = 1f
            incoming.scaleY = 1f
        }
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
        readPrefs()
        applyOff()
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
