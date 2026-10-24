package com.mb.tvlauncher

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var iconBox: FrameLayout
    private lateinit var leftTitle: TextView
    private lateinit var leftSub: TextView
    private lateinit var list: LinearLayout
    private lateinit var listScroll: ScrollView
    private var page = "main"
    private var lastMainIndex = 0
    private var direct = false
    private val handler = Handler(Looper.getMainLooper())
    private val wm by lazy { applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val bt: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val found = LinkedHashMap<String, BluetoothDevice>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == BluetoothDevice.ACTION_FOUND) {
                val d = i.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (d != null && !d.name.isNullOrBlank()) found[d.address] = d
            }
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, 600)
        }
    }
    private val refresh = Runnable {
        val keep = currentFocus?.let { list.indexOfChild(it) } ?: 0
        when (page) {
            "wifi" -> showWifi(keep)
            "bt" -> showBluetooth(keep)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val root = FrameLayout(this)
        root.addView(WallpaperView(this), FrameLayout.LayoutParams(-1, -1))
        root.addView(View(this).apply { setBackgroundColor(0xC0000000.toInt()) }, FrameLayout.LayoutParams(-1, -1))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(64), dp(48), dp(64), dp(32))
        }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        iconBox = FrameLayout(this)
        leftTitle = TextView(this).apply { setTextColor(Color.WHITE); textSize = 34f; gravity = Gravity.CENTER; setPadding(0, dp(24), 0, 0) }
        leftSub = TextView(this).apply { setTextColor(0xCCFFFFFF.toInt()); textSize = 18f; gravity = Gravity.CENTER; setPadding(dp(16), dp(8), dp(16), 0) }
        left.addView(iconBox, LinearLayout.LayoutParams(dp(180), dp(180)).apply { topMargin = dp(70) })
        left.addView(leftTitle, LinearLayout.LayoutParams(-1, -2))
        left.addView(leftSub, LinearLayout.LayoutParams(-1, -2))
        content.addView(left, LinearLayout.LayoutParams(0, -1, 4f))

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false; setPadding(dp(8), dp(8), dp(8), dp(80)) }
        listScroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; clipChildren = false; clipToPadding = false }
        listScroll.addView(list)
        content.addView(listScroll, LinearLayout.LayoutParams(0, -1, 6f))
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        when (intent.getStringExtra("page")) {
            "wifi" -> { direct = true; showWifi(0) }
            "bt" -> { direct = true; showBluetooth(0) }
            else -> showMain(0)
        }
    }

    override fun onResume() {
        super.onResume()
        val f = IntentFilter().apply {
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(receiver, f)
    }

    override fun onPause() {
        runCatching { unregisterReceiver(receiver) }
        runCatching { bt?.cancelDiscovery() }
        super.onPause()
    }

    @Deprecated("Back goes up one level")
    override fun onBackPressed() {
        if (page != "main" && !direct) showMain(lastMainIndex) else finish()
    }

    private fun setLeft(title: String, sub: String, kind: Int = -1, color: Int = 0xFF3A6FF7.toInt()) {
        leftTitle.text = title
        leftSub.text = sub
        iconBox.removeAllViews()
        iconBox.background = GradientDrawable().apply { cornerRadius = dp(40).toFloat(); setColor(color) }
        if (kind >= 0) {
            iconBox.addView(StatusIcon(this, kind), FrameLayout.LayoutParams(dp(100), dp(100), Gravity.CENTER))
        } else {
            iconBox.addView(TextView(this).apply { text = title.take(1); setTextColor(Color.WHITE); textSize = 64f; gravity = Gravity.CENTER }, FrameLayout.LayoutParams(-1, -1))
        }
    }

    private fun row(title: String, sub: String? = null, onFocus: (() -> Unit)? = null, onClick: () -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(14), dp(24), dp(14))
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(0x33FFFFFF) }
            isFocusable = true
            isClickable = true
        }
        val t = TextView(this).apply { text = title; setTextColor(Color.WHITE); textSize = 22f; maxLines = 1 }
        box.addView(t)
        var s: TextView? = null
        if (!sub.isNullOrBlank()) {
            s = TextView(this).apply { text = sub; setTextColor(0xB3FFFFFF.toInt()); textSize = 15f; maxLines = 1 }
            box.addView(s)
        }
        box.setOnFocusChangeListener { v, has ->
            (v.background as GradientDrawable).setColor(if (has) 0xF2FFFFFF.toInt() else 0x33FFFFFF)
            t.setTextColor(if (has) 0xFF111111.toInt() else Color.WHITE)
            s?.setTextColor(if (has) 0xFF555555.toInt() else 0xB3FFFFFF.toInt())
            v.animate().scaleX(if (has) 1.03f else 1f).scaleY(if (has) 1.03f else 1f).setDuration(140).start()
            if (has) onFocus?.invoke()
        }
        box.setOnClickListener { onClick() }
        list.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        return box
    }

    private fun startPage(name: String) {
        page = name
        list.removeAllViews()
    }

    private fun focusAt(index: Int) {
        val i = index.coerceIn(0, maxOf(0, list.childCount - 1))
        list.getChildAt(i)?.post { list.getChildAt(i)?.requestFocus() }
    }

    private fun openSystem(action: String) {
        runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    private fun currentSsid(): String {
        if (!wm.isWifiEnabled) return "Wi-Fi is off"
        val s = wm.connectionInfo?.ssid?.removeSurrounding("\"").orEmpty()
        return if (s.isBlank() || s == "<unknown ssid>") "Not connected" else "Connected to $s"
    }

    private fun showMain(focusIndex: Int) {
        startPage("main")
        val btState = if (bt == null) "Not available" else if (bt.isEnabled) "On" else "Off"
        row("Wi-Fi", currentSsid(), { setLeft("Wi-Fi", currentSsid(), StatusIcon.WIFI) }) { lastMainIndex = 0; showWifi(0) }
        row("Bluetooth", btState, { setLeft("Bluetooth", btState, StatusIcon.BLUETOOTH) }) { lastMainIndex = 1; showBluetooth(0) }
        row("Speed boost", "Free memory and faster animations", { setLeft("Speed boost", "Make the TV feel faster", -1, 0xFFE8590C.toInt()) }) { lastMainIndex = 2; showBoost(0) }
        val items = listOf(
            Triple("Network and Internet", Settings.ACTION_WIRELESS_SETTINGS, 0xFF2B8A3E),
            Triple("Sound", Settings.ACTION_SOUND_SETTINGS, 0xFFC2255C),
            Triple("Display", Settings.ACTION_DISPLAY_SETTINGS, 0xFF7048E8),
            Triple("Apps", Settings.ACTION_APPLICATION_SETTINGS, 0xFF1098AD),
            Triple("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, 0xFF5C677D),
            Triple("Date and time", Settings.ACTION_DATE_SETTINGS, 0xFF495057),
            Triple("Language", Settings.ACTION_LOCALE_SETTINGS, 0xFF5F3DC4),
            Triple("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS, 0xFF1864AB),
            Triple("About this TV", Settings.ACTION_DEVICE_INFO_SETTINGS, 0xFF343A40),
            Triple("All system settings", Settings.ACTION_SETTINGS, 0xFF212529)
        )
        items.forEachIndexed { n, it ->
            row(it.first, null, { setLeft(it.first, "Open", -1, it.third.toInt()) }) { lastMainIndex = 3 + n; openSystem(it.second) }
        }
        focusAt(focusIndex)
    }

    private fun secured(r: ScanResult) = r.capabilities.contains("WPA") || r.capabilities.contains("WEP") || r.capabilities.contains("PSK")

    private fun showWifi(focusIndex: Int) {
        startPage("wifi")
        val on = wm.isWifiEnabled
        row("Wi-Fi: " + if (on) "On" else "Off", currentSsid(), { setLeft("Wi-Fi", currentSsid(), StatusIcon.WIFI) }) {
            wm.isWifiEnabled = !on
            handler.postDelayed({ showWifi(0) }, 1500)
        }
        if (on) {
            row("Scan for networks", "Looking for nearby networks", { setLeft("Wi-Fi", "Scan again", StatusIcon.WIFI) }) {
                wm.startScan(); Toast.makeText(this, "Scanning", Toast.LENGTH_SHORT).show()
            }
            val saved = wm.configuredNetworks?.map { it.SSID.removeSurrounding("\"") }?.toSet() ?: emptySet()
            val cur = wm.connectionInfo?.ssid?.removeSurrounding("\"")
            android.util.Log.d("TVH", "scan size=${wm.scanResults?.size} coarse=${checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)} fine=${checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)}")
            val results = wm.scanResults.orEmpty().filter { it.SSID.isNotBlank() }
                .groupBy { it.SSID }.map { e -> e.value.maxByOrNull { it.level }!! }.sortedByDescending { it.level }
            results.forEach { r ->
                val status = when {
                    r.SSID == cur -> "Connected"
                    r.SSID in saved -> "Saved"
                    secured(r) -> "Secured"
                    else -> "Open"
                }
                row(r.SSID, status, { setLeft(r.SSID, status, StatusIcon.WIFI) }) { connectWifi(r, r.SSID in saved) }
            }
            if (results.isEmpty()) row("No networks found yet", "Press Scan and wait a few seconds") { }
            row("Other options", "Open the TV's own Wi-Fi settings", { setLeft("Wi-Fi", "System Wi-Fi settings", StatusIcon.WIFI) }) { openSystem(Settings.ACTION_WIFI_SETTINGS) }
            if (results.isEmpty()) handler.postDelayed({ if (page == "wifi") wm.startScan() }, 2500)
        }
        focusAt(focusIndex)
    }

    private fun connectWifi(r: ScanResult, saved: Boolean) {
        if (saved) {
            val cfg = wm.configuredNetworks?.firstOrNull { it.SSID.removeSurrounding("\"") == r.SSID }
            if (cfg != null) { wm.enableNetwork(cfg.networkId, true); wm.reconnect(); Toast.makeText(this, "Connecting to ${r.SSID}", Toast.LENGTH_SHORT).show() }
            return
        }
        if (!secured(r)) { addAndConnect(r, null); return }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            hint = "Password"
            setSingleLine()
        }
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Connect to ${r.SSID}")
            .setView(input)
            .setPositiveButton("Connect") { _, _ -> addAndConnect(r, input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addAndConnect(r: ScanResult, password: String?) {
        val cfg = WifiConfiguration().apply { SSID = "\"${r.SSID}\"" }
        when {
            password == null -> cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            r.capabilities.contains("WEP") -> {
                cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                cfg.wepKeys[0] = "\"$password\""; cfg.wepTxKeyIndex = 0
            }
            else -> cfg.preSharedKey = "\"$password\""
        }
        val id = wm.addNetwork(cfg)
        if (id < 0) { Toast.makeText(this, "Could not add network", Toast.LENGTH_LONG).show(); return }
        wm.enableNetwork(id, true)
        wm.reconnect()
        wm.saveConfiguration()
        Toast.makeText(this, "Connecting to ${r.SSID}", Toast.LENGTH_LONG).show()
    }

    private fun showBluetooth(focusIndex: Int) {
        startPage("bt")
        val b = bt
        if (b == null) { row("Bluetooth is not available on this TV", null) { }; return }
        val on = b.isEnabled
        row("Bluetooth: " + if (on) "On" else "Off", null, { setLeft("Bluetooth", if (on) "On" else "Off", StatusIcon.BLUETOOTH) }) {
            if (on) b.disable() else b.enable()
            handler.postDelayed({ showBluetooth(0) }, 1500)
        }
        if (on) {
            row(if (b.isDiscovering) "Searching…" else "Scan for devices", "Put your device in pairing mode", { setLeft("Bluetooth", "Scan for devices", StatusIcon.BLUETOOTH) }) {
                found.clear(); b.cancelDiscovery(); b.startDiscovery()
                Toast.makeText(this, "Searching for devices", Toast.LENGTH_SHORT).show()
                handler.postDelayed({ showBluetooth(1) }, 400)
            }
            b.bondedDevices.orEmpty().forEach { d ->
                row(d.name ?: d.address, "Paired", { setLeft(d.name ?: d.address, "Paired", StatusIcon.BLUETOOTH) }) {
                    AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle(d.name ?: d.address)
                        .setItems(arrayOf("Forget this device")) { _, _ ->
                            runCatching { d.javaClass.getMethod("removeBond").invoke(d) }
                            handler.postDelayed({ showBluetooth(1) }, 800)
                        }.show()
                }
            }
            val paired = b.bondedDevices.orEmpty().map { it.address }.toSet()
            found.values.filter { it.address !in paired }.forEach { d ->
                row(d.name ?: d.address, "Tap to pair", { setLeft(d.name ?: d.address, "Tap to pair", StatusIcon.BLUETOOTH) }) {
                    b.cancelDiscovery(); d.createBond()
                    Toast.makeText(this, "Pairing with ${d.name}", Toast.LENGTH_LONG).show()
                }
            }
        }
        focusAt(focusIndex)
    }

    private fun freeMb(): Long {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return mi.availMem / (1024 * 1024)
    }

    private fun animScale(): Float = runCatching { Settings.Global.getFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE) }.getOrDefault(1f)

    private fun setAnim(v: Float) {
        runCatching {
            Settings.Global.putFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, v)
            Settings.Global.putFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, v)
            Settings.Global.putFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, v)
        }.onFailure { Toast.makeText(this, "Permission missing for animations", Toast.LENGTH_LONG).show() }
    }

    private fun showBoost(focusIndex: Int) {
        startPage("boost")
        row("Free up memory now", "Free: ${freeMb()} MB", { setLeft("Speed boost", "Free: ${freeMb()} MB", -1, 0xFFE8590C.toInt()) }) {
            val before = freeMb()
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            packageManager.getInstalledApplications(0).forEach { if (it.packageName != packageName) runCatching { am.killBackgroundProcesses(it.packageName) } }
            handler.postDelayed({
                Toast.makeText(this, "Freed about ${maxOf(0, freeMb() - before)} MB", Toast.LENGTH_LONG).show()
                showBoost(0)
            }, 900)
        }
        val s = animScale()
        val name = if (s == 0f) "Off" else if (s < 1f) "Fast (${s}x)" else "Normal"
        row("Animations", name, { setLeft("Animations", name, -1, 0xFFE8590C.toInt()) }) {
            AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("System animations")
                .setItems(arrayOf("Off (fastest)", "Fast (0.5x)", "Normal (1x)")) { _, w ->
                    setAnim(floatArrayOf(0f, 0.5f, 1f)[w]); showBoost(1)
                }.show()
        }
        focusAt(focusIndex)
    }
}
