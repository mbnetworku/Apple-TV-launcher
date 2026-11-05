package com.mb.tvlauncher

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

// A soft, short "tick" when you move with the arrow keys and a slightly lower one when you press OK.
// The sounds are generated here (no sound files shipped); Settings > Sound effects switches them on or off.
object Tick {
    private var pool: SoundPool? = null
    private var move = 0
    private var select = 0
    private var loaded = 0
    private var quietUntil = 0L

    /** Stay silent for a moment (used when the home screen comes back and focuses the first icon by itself). */
    fun quiet(ms: Long) { quietUntil = System.currentTimeMillis() + ms }

    fun enabled(ctx: Context) = ctx.getSharedPreferences("home", Context.MODE_PRIVATE).getBoolean("tick_sound", true)

    /** kind 0 = moving, 1 = pressing OK. */
    fun play(ctx: Context, kind: Int) {
        if (!enabled(ctx) || System.currentTimeMillis() < quietUntil) return
        val p = pool ?: init(ctx.applicationContext) ?: return
        val id = if (kind == 1) select else move
        if (id != 0 && loaded >= 2) p.play(id, 0.55f, 0.55f, 1, 0, 1f)
    }

    private fun init(app: Context): SoundPool? = runCatching {
        @Suppress("DEPRECATION")
        val p = SoundPool(4, AudioManager.STREAM_MUSIC, 0)
        p.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded++ }
        move = p.load(wav(app, "tick_move.wav", 1900.0, 0.022, 0.0045), 1)
        select = p.load(wav(app, "tick_select.wav", 1250.0, 0.034, 0.0070), 1)
        pool = p
        p
    }.getOrNull()

    // A short decaying sine (with a quiet octave above it) saved as a small WAV file in the cache.
    private fun wav(app: Context, name: String, hz: Double, seconds: Double, decay: Double): String {
        val f = File(app.cacheDir, name)
        if (f.exists() && f.length() > 44) return f.path
        val rate = 22050
        val n = (rate * seconds).toInt()
        val data = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val env = Math.exp(-t / decay) * minOf(1.0, i / 12.0)
            val s = (Math.sin(2 * Math.PI * hz * t) * 0.75 + Math.sin(2 * Math.PI * hz * 2 * t) * 0.25) * env
            data.putShort((s * 0.8 * Short.MAX_VALUE).toInt().toShort())
        }
        val out = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVE".toByteArray()).put("fmt ".toByteArray())
        out.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        out.put("data".toByteArray()).putInt(n * 2).put(data.array())
        f.writeBytes(out.array())
        return f.path
    }
}
