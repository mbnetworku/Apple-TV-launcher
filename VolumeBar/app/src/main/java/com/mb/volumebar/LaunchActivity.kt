package com.mb.volumebar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, VolumeService::class.java))
        Toast.makeText(this, "Volume control is in the notification area", Toast.LENGTH_LONG).show()
        finish()
    }
}
