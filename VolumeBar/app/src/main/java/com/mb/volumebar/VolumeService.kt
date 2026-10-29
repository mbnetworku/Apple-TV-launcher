package com.mb.volumebar

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import android.widget.RemoteViews

class VolumeService : Service() {

    companion object {
        const val ACTION_UP = "com.mb.volumebar.UP"
        const val ACTION_DOWN = "com.mb.volumebar.DOWN"
        private const val NOTIF_ID = 7
        private const val STREAM = AudioManager.STREAM_MUSIC
    }

    private lateinit var audio: AudioManager

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { updateNotification() }
    }

    private fun pct(): Int {
        val max = audio.getStreamMaxVolume(STREAM).coerceAtLeast(1)
        return audio.getStreamVolume(STREAM) * 100 / max
    }

    private fun step(dir: Int) {
        audio.adjustStreamVolume(STREAM, if (dir > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
        updateNotification()
    }

    private fun action(a: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, VolumeService::class.java).setAction(a), PendingIntent.FLAG_UPDATE_CURRENT
    )

    @Suppress("DEPRECATION")
    private fun notification(): Notification {
        val p = pct()
        val views = RemoteViews(packageName, R.layout.notif_volume).apply {
            setTextViewText(R.id.vol_pct, "$p%")
            setProgressBar(R.id.vol_bar, 100, p, false)
            setOnClickPendingIntent(R.id.vol_minus, action(ACTION_DOWN, 1))
            setOnClickPendingIntent(R.id.vol_plus, action(ACTION_UP, 2))
        }
        return Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_vol)
            .setContentTitle("Volume $p%")
            .setContent(views)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, notification())
    }

    override fun onCreate() {
        super.onCreate()
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        startForeground(NOTIF_ID, notification())
        registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UP -> step(1)
            ACTION_DOWN -> step(-1)
            else -> updateNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(volumeReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
