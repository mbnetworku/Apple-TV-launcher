package com.mb.tvlauncher

import android.app.Activity
import android.graphics.Color
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvView
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

// Plays one TV input (HDMI 1-3, AV, VGA, antenna) full screen. Back returns to the home screen.
class InputActivity : Activity() {

    private lateinit var tv: TvView
    private lateinit var label: TextView
    private var tries = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideLabel = Runnable { label.animate().alpha(0f).setDuration(400).start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        tv = TvView(this)
        root.addView(tv, FrameLayout.LayoutParams(-1, -1))
        label = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 26f
            setPadding(dp(28), dp(14), dp(28), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(22).toFloat(); setColor(0xAA000000.toInt()) }
        }
        root.addView(label, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { topMargin = dp(40); marginStart = dp(48) })
        setContentView(root)

        val id = intent.getStringExtra("inputId")
        label.text = intent.getStringExtra("label") ?: "Input"
        if (id == null) { finish(); return }
        tv.setCallback(object : TvView.TvInputCallback() {
            override fun onConnectionFailed(inputId: String?) {
                // Right after the TV starts its input services may not be ready yet: try again a few times.
                if (tries++ < 5) handler.postDelayed({ tv.tune(inputId ?: return@postDelayed, TvContract.buildChannelUriForPassthroughInput(inputId)) }, 3000)
                else { label.text = "${label.text}: can't open this input"; label.alpha = 1f }
            }
            override fun onVideoUnavailable(inputId: String?, reason: Int) {
                // A free input with no cable shows "No signal" instead of a black screen.
                label.text = "${intent.getStringExtra("label")}: no signal"; label.animate().cancel(); label.alpha = 1f
            }
            override fun onVideoAvailable(inputId: String?) { label.text = intent.getStringExtra("label") ?: "Input"; label.alpha = 1f; handler.removeCallbacks(hideLabel); handler.postDelayed(hideLabel, 3000) }
        })
        tv.tune(id, TvContract.buildChannelUriForPassthroughInput(id))
    }

    // Back must always leave: the TV input's own session can swallow keys sent to the video view.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            label.animate().cancel(); label.alpha = 1f; handler.removeCallbacks(hideLabel); handler.postDelayed(hideLabel, 3000); return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        handler.removeCallbacks(hideLabel)
        runCatching { tv.reset() }
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
