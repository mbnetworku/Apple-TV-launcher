package com.mb.tvlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

// Small Input pop-up shown over whatever is on screen when the remote's Source button is pressed.
// It does not bring up the launcher. Back or a click outside closes it.
class InputPickerActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tim = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        val inputs = runCatching { tim.tvInputList }.getOrDefault(emptyList()).mapNotNull { info ->
            val l = runCatching { info.loadLabel(this)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Triple(info.id, l, info.type)
        }.sortedWith(compareBy({ if (it.third == TvInputInfo.TYPE_TUNER) 1 else 0 }, { it.second.lowercase() }))

        val root = FrameLayout(this).apply { setOnClickListener { finish() } }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0xF0202433.toInt()); setStroke(1, 0x66FFFFFF) }
            isClickable = true
        }
        panel.addView(TextView(this).apply {
            text = "Input"; setTextColor(0x99FFFFFF.toInt()); textSize = 13f
            setPadding(dp(14), dp(4), dp(14), dp(4))
        })
        var first: View? = null
        for ((id, label, _) in inputs) {
            val u = label.uppercase()
            val kind = when { u.contains("HDMI") -> StatusIcon.HDMI; u.startsWith("AV") -> StatusIcon.AV; u.contains("VGA") -> StatusIcon.VGA; else -> StatusIcon.ANTENNA }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(7), dp(24), dp(7))
                background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(Color.TRANSPARENT) }
                isFocusable = true
                isClickable = true
                isFocusableInTouchMode = true
                setOnFocusChangeListener { v, has -> (v.background as GradientDrawable).setColor(if (has) 0x66FFFFFF else Color.TRANSPARENT) }
                setOnHoverListener { v, e -> if (e.action == android.view.MotionEvent.ACTION_HOVER_ENTER && !v.hasFocus()) v.requestFocus(); false }
                setOnClickListener {
                    startActivity(Intent(this@InputPickerActivity, InputActivity::class.java).putExtra("inputId", id).putExtra("label", label))
                    finish()
                }
                addView(StatusIcon(context, kind), LinearLayout.LayoutParams(dp(24), dp(24)))
                addView(TextView(context).apply { text = label; setTextColor(Color.WHITE); textSize = 16f; maxLines = 1; setPadding(dp(12), 0, 0, 0) })
            }
            panel.addView(row, LinearLayout.LayoutParams(dp(190), -2))
            if (first == null) first = row
        }
        root.addView(panel, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = dp(60); marginEnd = dp(40) })
        setContentView(root)
        rows = ArrayList<View>().also { l -> for (i in 0 until panel.childCount) if (panel.getChildAt(i).isFocusable) l.add(panel.getChildAt(i)) }
        first?.post { first.requestFocus() }
    }

    private var rows: List<View> = emptyList()

    // Pressing Source again while the list is open moves down to the next source (wraps to the top at the end).
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (rows.isEmpty()) return
        val i = rows.indexOfFirst { it.hasFocus() }
        rows[(i + 1) % rows.size].requestFocus()
    }

    // Up/Down/OK pick a source and the volume keys keep working; Back or any other button closes the list.
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN,
            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
            android.view.KeyEvent.KEYCODE_VOLUME_UP, android.view.KeyEvent.KEYCODE_VOLUME_DOWN, android.view.KeyEvent.KEYCODE_VOLUME_MUTE -> super.onKeyDown(keyCode, event)
            android.view.KeyEvent.KEYCODE_TV_INPUT -> true // Source again is handled in onNewIntent (next source)
            else -> { finish(); true }
        }
    }

    // Home, Recent apps, an incoming app... anything that takes the screen away closes the list too.
    // (onStop, not onPause: pressing Source again re-delivers the intent through onPause.)
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }
}
