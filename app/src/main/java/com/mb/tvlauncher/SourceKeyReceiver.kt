package com.mb.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// The TV's menu app announces every press of the remote's Source / Input button with this broadcast, but it
// only opens its own input list when the stock launcher is Home. Here the launcher shows its Input list instead.
class SourceKeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        // One press sends several events (down, up, repeats within ~100 ms): react once per press.
        if (now - last < 350) return
        last = now
        // Prefer the TV's own input menu (switches sources exactly like the stock launcher did);
        // fall back to the launcher's own list only if this TV has none.
        val vendor = Intent("com.toptech.tv.action.INPUT_ACTIVITY").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.packageManager.resolveActivity(vendor, 0) != null) { context.startActivity(vendor); return }
        context.startActivity(
            Intent(context, InputPickerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    companion object { private var last = 0L }
}
