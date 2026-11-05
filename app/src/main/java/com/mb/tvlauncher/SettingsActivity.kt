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
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
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
import java.util.Locale

class SettingsActivity : Activity() {

    private lateinit var iconBox: FrameLayout
    private lateinit var leftTitle: TextView
    private lateinit var leftSub: TextView
    private lateinit var list: LinearLayout
    private lateinit var listScroll: ScrollView
    private var resumedBefore = false
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
                if (d != null && !runCatching { d.name }.getOrNull().isNullOrBlank()) found[d.address] = d
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

        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        // Numbers on the main list (free storage) and the Storage page must never be out of date.
        if (page == "main" && resumedBefore) showMain(currentFocus?.let { list.indexOfChild(it) }?.takeIf { it >= 0 } ?: lastMainIndex)
        resumedBefore = true
        handler.removeCallbacks(storageLive)
        handler.postDelayed(storageLive, 4000)
    }

    override fun onPause() {
        handler.removeCallbacks(storageLive)
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
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(0x33FFFFFF) }
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = true
            setOnHoverListener { x, e ->
                if (e.action == android.view.MotionEvent.ACTION_HOVER_ENTER && !x.hasFocus()) x.requestFocus()
                false
            }
        }
        val t = TextView(this).apply { text = title; setTextColor(Color.WHITE); textSize = 17f; maxLines = 1 }
        box.addView(t)
        var s: TextView? = null
        if (!sub.isNullOrBlank()) {
            s = TextView(this).apply { text = sub; setTextColor(0xB3FFFFFF.toInt()); textSize = 12f; maxLines = 1 }
            box.addView(s)
        }
        box.setOnFocusChangeListener { v, has ->
            (v.background as GradientDrawable).setColor(if (has) 0xF2FFFFFF.toInt() else 0x33FFFFFF)
            t.setTextColor(if (has) 0xFF111111.toInt() else Color.WHITE)
            s?.setTextColor(if (has) 0xFF555555.toInt() else 0xB3FFFFFF.toInt())
            v.animate().scaleX(if (has) 1.03f else 1f).scaleY(if (has) 1.03f else 1f).setDuration(140).start()
            if (has) { Tick.play(this, 0); onFocus?.invoke() }
        }
        box.setOnClickListener { Tick.play(this, 1); onClick() }
        list.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
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
        val s = realSsid()
        return if (s.isBlank()) "Not connected" else "Connected to $s"
    }

    // Some firmware answers "0x" or "<unknown ssid>" when Wi-Fi is on but not connected.
    private fun realSsid(): String {
        val info = runCatching { wm.connectionInfo }.getOrNull() ?: return ""
        val s = info.ssid?.removeSurrounding("\"").orEmpty()
        if (s.isBlank() || s == "<unknown ssid>" || s == "0x" || info.networkId == -1) return ""
        return s
    }

    private fun showMain(focusIndex: Int) {
        startPage("main")
        val btState = if (bt == null) "Not available" else if (bt.isEnabled) "On" else "Off"
        row("Wi-Fi", currentSsid(), { setLeft("Wi-Fi", currentSsid(), StatusIcon.WIFI) }) { lastMainIndex = 0; showWifi(0) }
        row("Bluetooth", btState, { setLeft("Bluetooth", btState, StatusIcon.BLUETOOTH) }) { lastMainIndex = 1; showBluetooth(0) }
        row("Speed boost", "Free memory and faster animations", { setLeft("Speed boost", "Make the TV feel faster", -1, 0xFFE8590C.toInt()) }) { lastMainIndex = 2; showBoost(0) }
        val storage = storageSummary()
        row("Storage", storage, { setLeft("Storage", storage, -1, STORAGE_COLOR) }) { lastMainIndex = 3; showStorage(0) }
        val home = getSharedPreferences("home", Context.MODE_PRIVATE)
        val sizes = listOf("Small" to 8, "Medium" to 6, "Large" to 5, "Extra large" to 4)
        val cols = home.getInt("cols", 6)
        val sizeName = sizes.firstOrNull { it.second == cols }?.first ?: "Medium"
        row("Icon size: $sizeName", "How big the app icons are on the home screen", { setLeft("Icon size", sizeName, -1, 0xFFD9480F.toInt()) }) {
            AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Icon size")
                .setItems(sizes.map { "${it.first}  (${it.second} per row)" }.toTypedArray()) { _, w ->
                    home.edit().putInt("cols", sizes[w].second).apply(); lastMainIndex = 4; showMain(4)
                }.show()
        }
        val favList = (home.getString("favs", "") ?: "").split("|").filter { it.isNotEmpty() }
        row("Favorite apps", if (favList.isEmpty()) "Pick the apps shown first on the home screen" else "${favList.size} chosen", { setLeft("Favorite apps", if (favList.isEmpty()) "None chosen" else "${favList.size} chosen", -1, 0xFFF59F00.toInt()) }) {
            lastMainIndex = 5; pickFavorites(home)
        }
        val startLabel = home.getString("startup_label", null)
        row("Start-up input: " + (startLabel ?: "Off"), "Open this input automatically when the TV starts", { setLeft("Start-up input", startLabel ?: "Off", -1, 0xFF2F9E44.toInt()) }) {
            lastMainIndex = 6; pickStartupInput(home)
        }
        val barHidden = home.getBoolean("hide_bar", false)
        row("Status bar: " + if (barHidden) "Hidden" else "Shown", "Wi-Fi, Bluetooth and clock on the home screen", { setLeft("Status bar", if (barHidden) "Hidden" else "Shown", -1, 0xFF0B7285.toInt()) }) {
            home.edit().putBoolean("hide_bar", !barHidden).apply(); lastMainIndex = 7; showMain(7)
        }
        val h24 = home.getBoolean("clock24", false)
        row("Clock: " + if (h24) "24-hour" else "12-hour", "Press OK to switch", { setLeft("Clock", if (h24) "24-hour" else "12-hour", -1, 0xFF5F3DC4.toInt()) }) {
            home.edit().putBoolean("clock24", !h24).apply(); lastMainIndex = 8; showMain(8)
        }
        lastFree = dataSpace().free
        val net = networks()
        val netSummary = if (net.isEmpty()) "Not connected" else net.joinToString("  ·  ") { "${it.kind} ${it.ip ?: ""}".trim() }
        row("Network", netSummary, { setLeft("Network", netSummary, StatusIcon.LAN, 0xFF1C7ED6.toInt()) }) { lastMainIndex = 9; showNetwork(0) }
        val tickOn = Tick.enabled(this)
        row("Sound effects: " + if (tickOn) "On" else "Off", "A soft tick when you move with the arrows or press OK", { setLeft("Sound effects", if (tickOn) "On" else "Off", -1, 0xFFE64980.toInt()) }) {
            home.edit().putBoolean("tick_sound", !tickOn).apply(); lastMainIndex = 10; showMain(10)
            if (!tickOn) Tick.play(this, 1)
        }
        val tzId = home.getString("clock_tz", null)
        val tzName = if (tzId == null) "System (" + java.util.TimeZone.getDefault().getDisplayName(false, java.util.TimeZone.SHORT) + ")" else zoneLabel(tzId)
        row("Clock time zone: $tzName", "Show another time zone in the launcher if the device's clock is off", { setLeft("Clock time zone", tzName, -1, 0xFF0CA678.toInt()) }) {
            lastMainIndex = 11; pickClockZone(home)
        }
        val items = listOf(
            Triple("Network and Internet", Settings.ACTION_WIRELESS_SETTINGS, 0xFF2B8A3E),
            Triple("Sound", Settings.ACTION_SOUND_SETTINGS, 0xFFC2255C),
            Triple("Display", Settings.ACTION_DISPLAY_SETTINGS, 0xFF7048E8),
            Triple("Apps", Settings.ACTION_APPLICATION_SETTINGS, 0xFF1098AD),
            Triple("Date and time", Settings.ACTION_DATE_SETTINGS, 0xFF495057),
            Triple("Language", Settings.ACTION_LOCALE_SETTINGS, 0xFF5F3DC4),
            Triple("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS, 0xFF1864AB),
            Triple("About this TV", Settings.ACTION_DEVICE_INFO_SETTINGS, 0xFF343A40),
            Triple("All system settings", Settings.ACTION_SETTINGS, 0xFF212529)
        )
        items.forEachIndexed { n, it ->
            row(it.first, null, { setLeft(it.first, "Open", -1, it.third.toInt()) }) { lastMainIndex = 12 + n; openSystem(it.second) }
        }
        focusAt(focusIndex)
    }

    // Time zones the launcher clock can show (does not change the device's own setting).
    private val zones: List<Pair<String, String>> = listOf(
        "GMT-12:00" to "UTC-12", "GMT-11:00" to "UTC-11", "GMT-10:00" to "UTC-10 (Hawaii)", "GMT-09:00" to "UTC-9 (Alaska)",
        "GMT-08:00" to "UTC-8 (Los Angeles)", "GMT-07:00" to "UTC-7 (Denver)", "GMT-06:00" to "UTC-6 (Chicago, Mexico City)",
        "GMT-05:00" to "UTC-5 (New York, Bogota)", "GMT-04:00" to "UTC-4 (Caracas, Halifax)", "GMT-03:00" to "UTC-3 (Sao Paulo, Buenos Aires)",
        "GMT-02:00" to "UTC-2", "GMT-01:00" to "UTC-1 (Azores)", "GMT+00:00" to "UTC+0 (London, Lisbon, Accra)",
        "GMT+01:00" to "UTC+1 (Paris, Berlin, Lagos)", "GMT+02:00" to "UTC+2 (Cairo, Athens, Johannesburg)",
        "GMT+03:00" to "UTC+3 (Riyadh, Baghdad, Kuwait, Istanbul, Moscow, Nairobi, Addis Ababa)", "GMT+03:30" to "UTC+3:30 (Tehran)",
        "GMT+04:00" to "UTC+4 (Dubai, Baku)", "GMT+04:30" to "UTC+4:30 (Kabul)", "GMT+05:00" to "UTC+5 (Karachi, Tashkent)",
        "GMT+05:30" to "UTC+5:30 (India, Sri Lanka)", "GMT+05:45" to "UTC+5:45 (Nepal)", "GMT+06:00" to "UTC+6 (Dhaka, Almaty)",
        "GMT+07:00" to "UTC+7 (Bangkok, Jakarta)", "GMT+08:00" to "UTC+8 (Beijing, Singapore, Perth)", "GMT+09:00" to "UTC+9 (Tokyo, Seoul)",
        "GMT+09:30" to "UTC+9:30 (Adelaide)", "GMT+10:00" to "UTC+10 (Sydney, Brisbane)", "GMT+12:00" to "UTC+12 (Auckland, Fiji)"
    )

    private fun zoneLabel(id: String) = zones.firstOrNull { it.first == id }?.second ?: id

    private fun pickClockZone(home: android.content.SharedPreferences) {
        val names = (listOf("System default") + zones.map { it.second }).toTypedArray()
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Clock time zone")
            .setItems(names) { _, w ->
                if (w == 0) home.edit().remove("clock_tz").apply() else home.edit().putString("clock_tz", zones[w - 1].first).apply()
                showMain(11)
            }.show()
    }

    // Pick the input (HDMI 1-3, AV, VGA, antenna) that opens by itself a few seconds after the TV starts.
    private fun pickStartupInput(home: android.content.SharedPreferences) {
        val tim = getSystemService(Context.TV_INPUT_SERVICE) as android.media.tv.TvInputManager
        val inputs = runCatching { tim.tvInputList }.getOrDefault(emptyList()).mapNotNull { info ->
            val l = runCatching { info.loadLabel(this)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Triple(info.id, l, info.type)
        }.sortedWith(compareBy({ if (it.third == android.media.tv.TvInputInfo.TYPE_TUNER) 1 else 0 }, { it.second.lowercase() }))
        val names = (listOf("Off (show the home screen)") + inputs.map { it.second }).toTypedArray()
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Start-up input")
            .setItems(names) { _, w ->
                val e = home.edit()
                if (w == 0) e.remove("startup_input").remove("startup_label")
                else e.putString("startup_input", inputs[w - 1].first).putString("startup_label", inputs[w - 1].second)
                e.apply()
                showMain(6)
            }.show()
    }

    // Tick the apps that should come first on the home screen (in the order they were ticked).
    private fun pickFavorites(home: android.content.SharedPreferences) {
        val apps = (home.getString("applist", "") ?: "").split("\n").mapNotNull {
            val p = it.split("\t", limit = 2); if (p.size == 2 && p[0].isNotEmpty()) p[0] to p[1] else null
        }
        if (apps.isEmpty()) { Toast.makeText(this, "Open the home screen once, then try again", Toast.LENGTH_LONG).show(); return }
        val old = (home.getString("favs", "") ?: "").split("|").filter { it.isNotEmpty() }
        val sorted = apps.sortedBy { it.second.lowercase() }
        val checked = BooleanArray(sorted.size) { sorted[it].first in old }
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Favorite apps: tick to show first")
            .setMultiChoiceItems(sorted.map { it.second }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Save") { _, _ ->
                val chosen = sorted.indices.filter { checked[it] }.map { sorted[it].first }
                val ordered = old.filter { it in chosen } + chosen.filter { it !in old }
                home.edit().putString("favs", ordered.joinToString("|")).apply()
                showMain(5)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            android.util.Log.d("TVH", "scan size=${wm.scanResults?.size}")
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

    // ---- Storage: real numbers from the file system, in the same units as Android's own Settings (1 GB = 1000 MB) ----

    private val STORAGE_COLOR = 0xFF5C677D.toInt()
    private var userFiles: Long? = null

    private class Space(val total: Long, val free: Long) { val used: Long get() = total - free }

    private fun size(bytes: Long): String =
        if (bytes >= 1_000_000_000L && bytes % 1_000_000_000L == 0L) "${bytes / 1_000_000_000L} GB"
        else if (bytes >= 1_000_000_000L) String.format(Locale.US, "%.2f GB", bytes / 1e9)
        else String.format(Locale.US, "%d MB", bytes / 1_000_000L)

    private fun space(path: String): Space? = runCatching { StatFs(path).let { Space(it.totalBytes, it.availableBytes) } }.getOrNull()

    private fun dataSpace(): Space = space(Environment.getDataDirectory().path) ?: Space(0, 0)

    // The whole flash chip (system, firmware and the space for apps together). Exact size when the kernel lets apps
    // read it; otherwise Android's own figure for the device's storage (the size it was sold as, e.g. 8 GB).
    private fun chipBytes(): Long? =
        runCatching { java.io.File("/sys/block/mmcblk0/size").readText().trim().toLong() * 512 }.getOrNull()?.takeIf { it > 0 }
            ?: if (android.os.Build.VERSION.SDK_INT >= 26) runCatching {
                (getSystemService(Context.STORAGE_STATS_SERVICE) as android.app.usage.StorageStatsManager)
                    .getTotalBytes(android.os.storage.StorageManager.UUID_DEFAULT)
            }.getOrNull()?.takeIf { it > 0 } else null

    // Android refuses to install or update apps when free space falls below this (5% of the total, at most 500 MiB).
    private fun lowBytes(total: Long): Long {
        val pct = Settings.Global.getInt(contentResolver, "sys_storage_threshold_percentage", 5)
        val max = Settings.Global.getLong(contentResolver, "sys_storage_threshold_max_bytes", 500L * 1024 * 1024)
        return minOf(total * pct / 100, max)
    }

    private fun storageSummary(): String { val d = dataSpace(); return "${size(d.free)} free of ${size(d.total)}" }

    // Adds up a folder. Skips links (they can loop back on themselves) and stops very deep trees.
    private fun folderBytes(f: java.io.File, depth: Int = 0): Long = runCatching {
        when {
            f.isFile -> f.length()
            depth > 12 || f.canonicalPath != f.absolutePath -> 0L
            else -> f.listFiles()?.sumOf { folderBytes(it, depth + 1) } ?: 0L
        }
    }.getOrDefault(0L)

    // Downloads, pictures, videos and other files in shared storage (app folders under Android/ count as app data).
    private fun measureUserFiles(): Long =
        Environment.getExternalStorageDirectory().listFiles()?.filter { it.name != "Android" }?.sumOf { folderBytes(it) } ?: 0L

    private fun addBar(box: View, pct: Int, color: Int) {
        val p = pct.coerceIn(0, 100)
        val track = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(0x66888888) }
        }
        track.addView(View(this).apply { background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(color) } }, LinearLayout.LayoutParams(0, -1, p.toFloat()))
        track.addView(View(this), LinearLayout.LayoutParams(0, -1, (100 - p).toFloat()))
        (box as LinearLayout).addView(track, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(10) })
    }

    private fun showStorage(focusIndex: Int, measure: Boolean = true) {
        startPage("storage")
        val d = dataSpace()
        val low = lowBytes(d.total)
        val pct = if (d.total > 0) (d.used * 100 / d.total).toInt() else 0
        val left = { setLeft("Storage", "${size(d.free)} free", -1, STORAGE_COLOR) }
        val top = row("Space for apps and files: ${size(d.total)}", "Used ${size(d.used)} ($pct%)   ·   Free ${size(d.free)}", left) { }
        addBar(top, pct, if (d.free < low * 2) 0xFFE03131.toInt() else 0xFF4DABF7.toInt())
        row("Room to install apps: ${size(maxOf(0L, d.free - low))}", "Android always keeps ${size(low)} free and stops installs below that", left) { }
        val files = userFiles
        row("Your files: " + (files?.let { size(it) } ?: "measuring…"), "Downloads, pictures, videos and other files", left) { }
        row("Apps and app data: " + (files?.let { size(maxOf(0L, d.used - it)) } ?: "measuring…"), "Installed apps with their data and cache, plus Android's own files", left) { }
        space(Environment.getRootDirectory().path)?.let { s ->
            row("Android system: ${size(s.total)}", "Read-only part of the chip, not counted above", left) { }
        }
        chipBytes()?.let { c -> row("Whole storage chip: ${size(c)}", "Space for apps and files + Android system + firmware", left) { } }
        row("Open the TV's storage settings", "Clear an app's cache or data", left) { openSystem(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) }
        focusAt(focusIndex)
        lastFree = d.free
        if (measure) Thread {
            val b = runCatching { measureUserFiles() }.getOrNull()
            handler.post {
                val changed = b != null && b != userFiles
                if (b != null) userFiles = b
                if (page == "storage" && changed) showStorage(currentFocus?.let { list.indexOfChild(it) }?.takeIf { it >= 0 } ?: 0, measure = false)
            }
        }.start()
    }

    // ---- Network: connection type, IPv4 address, gateway, subnet mask and DNS of every connected network ----

    private class Net(val kind: String, val ip: String?, val mask: String?, val gateway: String?, val dns: List<String>)

    private fun maskOf(prefix: Int): String {
        val m = if (prefix <= 0) 0L else (0xFFFFFFFFL shl (32 - prefix.coerceAtMost(32))) and 0xFFFFFFFFL
        return "${(m shr 24) and 255}.${(m shr 16) and 255}.${(m shr 8) and 255}.${m and 255}"
    }

    @Suppress("DEPRECATION")
    private fun networks(): List<Net> = runCatching {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val out = ArrayList<Net>()
        for (n in cm.allNetworks) {
            val info = cm.getNetworkInfo(n)?.takeIf { it.isConnected } ?: continue
            val kind = when (info.type) {
                android.net.ConnectivityManager.TYPE_ETHERNET -> "LAN"
                android.net.ConnectivityManager.TYPE_WIFI -> {
                    val s = realSsid()
                    if (s.isBlank()) "Wi-Fi" else "Wi-Fi $s"
                }
                else -> continue
            }
            val lp = cm.getLinkProperties(n) ?: continue
            val addr = lp.linkAddresses.firstOrNull { it.address is java.net.Inet4Address }
            val gw = lp.routes.firstOrNull { it.isDefaultRoute && it.gateway is java.net.Inet4Address }?.gateway?.hostAddress
            out.add(Net(kind, addr?.address?.hostAddress, addr?.let { maskOf(it.prefixLength) }, gw,
                lp.dnsServers.filterIsInstance<java.net.Inet4Address>().mapNotNull { it.hostAddress }))
        }
        out.sortedBy { if (it.kind == "LAN") 0 else 1 }
    }.getOrDefault(emptyList())

    private var lastNetSig = ""

    private fun showNetwork(focusIndex: Int) {
        startPage("network")
        val nets = networks()
        lastNetSig = nets.joinToString("|") { "${it.kind}${it.ip}${it.gateway}" }
        val left = { setLeft("Network", if (nets.isEmpty()) "Not connected" else nets.first().kind, StatusIcon.LAN, 0xFF1C7ED6.toInt()) }
        if (nets.isEmpty()) row("Not connected", "No LAN cable or Wi-Fi connection found", left) { }
        for (n in nets) {
            row("${n.kind} (connected)", "Connection type", left) { }
            row("IPv4 address: ${n.ip ?: "none"}", "This device's address on the network", left) { }
            row("Gateway: ${n.gateway ?: "none"}", "The router's IPv4 address", left) { }
            n.mask?.let { row("Subnet mask: $it", null, left) { } }
            if (n.dns.isNotEmpty()) row("DNS: ${n.dns.joinToString(", ")}", "Name servers", left) { }
        }
        row("Open the TV's network settings", "Change Wi-Fi or LAN", left) { openSystem(Settings.ACTION_WIRELESS_SETTINGS) }
        focusAt(focusIndex)
    }

    // Keeps the Storage and Network pages (and the Storage line on the main list) live: every few seconds it re-reads
    // the real numbers and redraws only if something changed.
    private var lastFree = -1L
    private var storageTicks = 0
    private val storageLive = object : Runnable {
        override fun run() {
            val idx0 = currentFocus?.let { list.indexOfChild(it) }?.takeIf { it >= 0 } ?: 0
            if (page == "main" && dataSpace().free != lastFree) showMain(idx0)
            if (page == "network") {
                val sig = networks().joinToString("|") { "${it.kind}${it.ip}${it.gateway}" }
                if (sig != lastNetSig) showNetwork(idx0)
            }
            if (page == "storage") {
                val idx = currentFocus?.let { list.indexOfChild(it) }?.takeIf { it >= 0 } ?: 0
                if (dataSpace().free != lastFree) showStorage(idx, measure = true)
                else if (++storageTicks % 5 == 0) Thread { // walking every file is heavy: only every ~20 s unless free space changed
                    val b = runCatching { measureUserFiles() }.getOrNull()
                    handler.post { if (page == "storage" && b != null && b != userFiles) { userFiles = b; showStorage(idx, measure = false) } }
                }.start()
            }
            handler.postDelayed(this, 4000)
        }
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
        val canSet = checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        row("Animations", if (canSet) name else "$name  ·  OK opens Developer options", { setLeft("Animations", name, -1, 0xFFE8590C.toInt()) }) {
            if (!canSet) {
                Toast.makeText(this, "Set Window, Transition and Animator scale to 0.5x", Toast.LENGTH_LONG).show()
                openSystem(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                return@row
            }
            AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("System animations")
                .setItems(arrayOf("Off (fastest)", "Fast (0.5x)", "Normal (1x)")) { _, w ->
                    setAnim(floatArrayOf(0f, 0.5f, 1f)[w]); showBoost(1)
                }.show()
        }
        focusAt(focusIndex)
    }
}
