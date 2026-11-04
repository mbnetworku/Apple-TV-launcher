package com.mb.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

// When the TV has started, opens the input chosen in Settings > Start-up input.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)
        val id = prefs.getString("startup_input", null) ?: return
        val label = prefs.getString("startup_label", null) ?: "Input"
        val app = context.applicationContext
        // Give the home screen and the TV services a few seconds to finish starting first.
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                app.startActivity(
                    Intent(app, InputActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("inputId", id).putExtra("label", label)
                )
            }
        }, 8_000)
    }
}
