package com.mb.tvlauncher

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private class AppEntry(val label: String, val component: ComponentName?, val info: ResolveInfo?, val kind: Int = 0) {
        val pkg: String? get() = component?.packageName
    }

    private lateinit var rows: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var clock: TextView
    private lateinit var bigClock: TextView
    private lateinit var bigAmPm: TextView
    private lateinit var bigDate: TextView
    private lateinit var bigBox: LinearLayout
    private lateinit var inputGroup: View
    private lateinit var volGroup: View
    private lateinit var volText: TextView
    private lateinit var volBar: View
    private lateinit var volPanel: LinearLayout
    private lateinit var volPanelText: TextView
    private val audio by lazy { getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    private val hideVolume = Runnable { volPanel.visibility = View.GONE }

    private fun volPercent(): Int {
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) * 100 / max
    }

    private fun refreshVolume() {
        val p = volPercent()
        volText.text = "$p%"
        volPanelText.text = "Volume  $p%"
        (volBar.layoutParams as FrameLayout.LayoutParams).width = dp(520) * p / 100
        volBar.requestLayout()
    }

    private fun showVolume() {
        refreshVolume()
        volPanel.visibility = View.VISIBLE
        volGroup.requestFocus()
        handler.removeCallbacks(hideVolume)
        handler.postDelayed(hideVolume, 8_000)
    }

    private fun stepVolume(dir: Int) {
        audio.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, if (dir > 0) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER, 0)
        showVolume()
    }
    private lateinit var btGroup: View
    private lateinit var lanGroup: View
    private lateinit var wifiGroup: View
    private lateinit var wifiText: TextView
    private lateinit var dim: View
    private var gridMode = false
    private var spacerH = 0
    private var gridTop = 0
    private var movingPkg: String? = null
    private var visibleApps: MutableList<AppEntry> = ArrayList()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("home", Context.MODE_PRIVATE) }
    private val tick = object : Runnable {
        override fun run() {
            val now = Date()
            val h24 = prefs.getBoolean("clock24", false)
            // Settings > Clock time zone shows another zone in the launcher when the device's own zone is wrong or unset.
            val zone = prefs.getString("clock_tz", null)?.let { java.util.TimeZone.getTimeZone(it) }
            fun fmt(p: String, l: Locale) = SimpleDateFormat(p, l).also { if (zone != null) it.timeZone = zone }
            clock.text = fmt(if (h24) "HH:mm" else "h:mm a", Locale.US).format(now)
            bigClock.text = fmt(if (h24) "HH:mm" else "h:mm", Locale.US).format(now)
            bigAmPm.text = if (h24) "" else fmt("a", Locale.US).format(now)
            volText.text = "${volPercent()}%"
            bigDate.text = fmt("EEEE, d MMMM", Locale.getDefault()).format(now)
            updateStatus()
            handler.postDelayed(this, 5_000)
        }
    }

    // Icons per row: 8 = small, 6 = medium (default), 5 = large, 4 = extra large (set in Settings > Icon size).
    private val perRow: Int get() = prefs.getInt("cols", 6).coerceIn(3, 8)
    private lateinit var pill: LinearLayout

    private fun hoverFocus(v: View) {
        v.isFocusableInTouchMode = true
        v.setOnHoverListener { x, e ->
            if (e.action == android.view.MotionEvent.ACTION_HOVER_ENTER && !x.hasFocus()) x.requestFocus()
            false
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun hiddenSet(): MutableSet<String> = HashSet(prefs.getStringSet("hidden", emptySet()) ?: emptySet())
    private fun saveHidden(s: Set<String>) = prefs.edit().putStringSet("hidden", s).apply()
    private fun savedOrder(): List<String> = (prefs.getString("order", "") ?: "").split("|").filter { it.isNotEmpty() }
    private fun saveOrder(list: List<String>) = prefs.edit().putString("order", list.joinToString("|")).apply()

    private fun statusGroup(kind: Int, label: String, labelView: TextView? = null, target: String? = null): LinearLayout {
        val g = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(Color.TRANSPARENT) }
            isFocusable = true
            isClickable = true
            hoverFocus(this)
            setOnFocusChangeListener { v, has ->
                if (has) Tick.play(this@MainActivity, 0)
                (v.background as GradientDrawable).setColor(if (has) 0x55FFFFFF else Color.TRANSPARENT)
                v.animate().scaleX(if (has) 1.1f else 1f).scaleY(if (has) 1.1f else 1f).setDuration(140).start()
            }
            setOnClickListener {
                Tick.play(this@MainActivity, 1)
                open(Intent(this@MainActivity, SettingsActivity::class.java).putExtra("page", target ?: "main"))
            }
        }
        g.addView(StatusIcon(this, kind), LinearLayout.LayoutParams(dp(22), dp(22)))
        val t = labelView ?: TextView(this)
        t.text = label
        t.setTextColor(Color.WHITE)
        t.textSize = 16f
        t.maxLines = 1
        t.setPadding(dp(6), 0, 0, 0)
        g.addView(t)
        return g
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val root = FrameLayout(this)
        root.addView(WallpaperView(this), FrameLayout.LayoutParams(-1, -1))
        root.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x33000000, 0x00000000, 0x00000000, 0x77000000)
            )
        }, FrameLayout.LayoutParams(-1, -1))
        dim = View(this).apply { setBackgroundColor(Color.BLACK); alpha = 0f }
        root.addView(dim, FrameLayout.LayoutParams(-1, -1))

        pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(16), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply { cornerRadius = dp(26).toFloat(); setColor(0x66000000); setStroke(1, 0x44FFFFFF) }
        }
        btGroup = statusGroup(StatusIcon.BLUETOOTH, "Bluetooth", null, "bt")
        lanGroup = statusGroup(StatusIcon.LAN, "LAN", null, "main")
        wifiText = TextView(this)
        wifiGroup = statusGroup(StatusIcon.WIFI, "Wi-Fi", wifiText, "wifi")
        clock = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 20f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(Color.TRANSPARENT) }
            isFocusable = true
            isClickable = true
            hoverFocus(this)
            setOnClickListener { open(Intent(Settings.ACTION_DATE_SETTINGS)) }
            setOnFocusChangeListener { v, has ->
                (v.background as GradientDrawable).setColor(if (has) 0x55FFFFFF else Color.TRANSPARENT)
                v.animate().scaleX(if (has) 1.1f else 1f).scaleY(if (has) 1.1f else 1f).setDuration(140).start()
            }
        }
        bigClock = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 96f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setShadowLayer(12f, 0f, 3f, 0xAA000000.toInt())
        }
        bigAmPm = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 30f
            setShadowLayer(10f, 0f, 2f, 0xAA000000.toInt())
        }
        bigDate = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 28f
            setShadowLayer(10f, 0f, 2f, 0xAA000000.toInt())
        }
        val bigTime = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            addView(bigClock)
            addView(bigAmPm, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8); bottomMargin = dp(18) })
        }
        bigBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bigTime)
            addView(bigDate)
        }
        val gear = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x44FFFFFF) }
            isFocusable = true
            isClickable = true
            hoverFocus(this)
            addView(StatusIcon(this@MainActivity, StatusIcon.GEAR), FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
            setOnClickListener { open(Intent(this@MainActivity, SettingsActivity::class.java)) }
            setOnFocusChangeListener { v, has ->
                (v.background as GradientDrawable).setColor(if (has) 0xAAFFFFFF.toInt() else 0x44FFFFFF)
                v.animate().scaleX(if (has) 1.2f else 1f).scaleY(if (has) 1.2f else 1f).setDuration(140).start()
            }
        }
        volText = TextView(this)
        volGroup = statusGroup(StatusIcon.VOLUME, "", volText, null).apply {
            visibility = View.VISIBLE
            setOnClickListener { showVolume() }
        }
        volBar = View(this).apply { background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(0xFFFFFFFF.toInt()) } }
        val volTrack = FrameLayout(this).apply {
            background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(0x55FFFFFF) }
            addView(volBar, FrameLayout.LayoutParams(0, -1))
            setOnTouchListener { v, ev ->
                if (ev.action == android.view.MotionEvent.ACTION_DOWN || ev.action == android.view.MotionEvent.ACTION_MOVE) {
                    val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val frac = (ev.x / v.width).coerceIn(0f, 1f)
                    audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, Math.round(frac * max), 0)
                    showVolume()
                }
                true
            }
        }
        val wheel = View.OnGenericMotionListener { _, ev ->
            if (ev.action == android.view.MotionEvent.ACTION_SCROLL) {
                stepVolume(if (ev.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL) > 0) 1 else -1)
                true
            } else false
        }
        volGroup.setOnGenericMotionListener(wheel)
        inputGroup = statusGroup(StatusIcon.INPUT, "Input", null, null).apply {
            visibility = View.VISIBLE
            setOnClickListener { openInputs() }
        }
        volPanelText = TextView(this).apply { setTextColor(Color.WHITE); textSize = 28f; gravity = Gravity.CENTER }
        volPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            setPadding(dp(36), dp(20), dp(36), dp(24))
            background = GradientDrawable().apply { cornerRadius = dp(26).toFloat(); setColor(0xCC000000.toInt()); setStroke(1, 0x66FFFFFF) }
            addView(volPanelText, LinearLayout.LayoutParams(-2, -2))
            addView(volTrack, LinearLayout.LayoutParams(dp(520), dp(14)).apply { topMargin = dp(14) })
            addView(TextView(this@MainActivity).apply { setTextColor(0xAAFFFFFF.toInt()); textSize = 16f; gravity = Gravity.CENTER; text = "Left / Right to change, OK to close" }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        }
        volPanel.setOnGenericMotionListener(wheel)
        val gap = dp(6)
        pill.addView(inputGroup, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = gap })
        pill.addView(volGroup, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = gap })
        pill.addView(btGroup, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = gap })
        pill.addView(lanGroup, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = gap })
        pill.addView(wifiGroup, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = gap })
        pill.addView(clock, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        pill.addView(gear, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginStart = dp(14) })

        val screenH = resources.displayMetrics.heightPixels
        spacerH = (screenH * 0.66f).toInt()
        rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(32), 0, dp(32), (screenH * 0.35f).toInt())
        }
        scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; clipChildren = false; clipToPadding = false }
        scroll.addView(rows)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        root.addView(bigBox, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { topMargin = dp(24); marginStart = dp(48) })
        root.addView(pill, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = dp(28); marginEnd = dp(40) })
        root.addView(volPanel, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply { topMargin = dp(110) })

        setContentView(root)
    }

    @Suppress("DEPRECATION")
    private fun updateStatus() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiUp = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected == true
        val lanUp = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)?.isConnected == true
        lanGroup.visibility = if (lanUp) View.VISIBLE else View.GONE
        if (wifiUp) {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ssid = wm.connectionInfo?.ssid?.removeSurrounding("\"").orEmpty()
            wifiText.text = if (ssid.isBlank() || ssid == "<unknown ssid>") "Wi-Fi" else ssid
            wifiGroup.visibility = View.VISIBLE
        } else {
            wifiGroup.visibility = View.GONE
        }
        val bt = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
        btGroup.visibility = if (bt) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        Tick.quiet(900) // the first icon is focused by itself when Home appears: no tick for that
        pill.visibility = if (prefs.getBoolean("hide_bar", false)) View.GONE else View.VISIBLE
        // The Input button is only shown on a TV that actually has inputs (HDMI, AV, antenna...).
        inputGroup.visibility = if (hasInputs()) View.VISIBLE else View.GONE
        if (movingPkg == null) populate()
        handler.post(tick)
        // The remote's Source button (via SourceKeyService) asks for the Input list.
        if (intent?.getBooleanExtra("showInputs", false) == true) {
            intent.removeExtra("showInputs")
            handler.postDelayed({ openInputs() }, 400)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        movingPkg = null
        firstTile()?.requestFocus()
        setGridMode(false)
    }

    private fun setGridMode(on: Boolean) {
        if (gridMode == on) return
        gridMode = on
        dim.animate().alpha(if (on) 0.6f else 0f).setDuration(350).start()
        bigBox.animate().alpha(if (on) 0f else 1f).setDuration(350).start()
        val target = if (on) maxOf(0, spacerH - gridTop) else 0
        ObjectAnimator.ofInt(scroll, "scrollY", scroll.scrollY, target).setDuration(420).apply {
            interpolator = DecelerateInterpolator(1.6f)
        }.start()
    }

    @Deprecated("Stay on the home screen")
    override fun onBackPressed() { }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (volPanel.visibility == View.VISIBLE) {
            if (e.action == KeyEvent.ACTION_DOWN) {
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { stepVolume(-1); return true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { stepVolume(1); return true }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> { handler.removeCallbacks(hideVolume); volPanel.visibility = View.GONE; return true }
                }
            } else return true
        }
        if (movingPkg != null) {
            if (e.action == KeyEvent.ACTION_DOWN) {
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> moveBy(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> moveBy(1)
                    KeyEvent.KEYCODE_DPAD_UP -> moveBy(-perRow)
                    KeyEvent.KEYCODE_DPAD_DOWN -> moveBy(perRow)
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> endMove()
                }
            }
            return true
        }
        if (e.keyCode == KeyEvent.KEYCODE_TV_INPUT) {
            if (e.action == KeyEvent.ACTION_UP) openInputs()
            return true
        }
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode == KeyEvent.KEYCODE_MENU) {
            val tag = currentFocus?.tag as? String
            visibleApps.firstOrNull { it.pkg == tag }?.let { showMenu(it); return true }
        }
        return super.dispatchKeyEvent(e)
    }

    private fun moveBy(delta: Int) {
        val pkg = movingPkg ?: return
        val from = visibleApps.indexOfFirst { it.pkg == pkg }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, visibleApps.size - 1)
        if (to == from) return
        val item = visibleApps.removeAt(from)
        visibleApps.add(to, item)
        saveOrder(visibleApps.mapNotNull { it.pkg })
        populate(keep = true, focusPkg = pkg)
    }

    private fun dropOn(pkg: String?, x: Float, y: Float) {
        if (pkg == null) return
        val from = visibleApps.indexOfFirst { it.pkg == pkg }
        val loc = IntArray(2)
        var to = -1
        var best = Float.MAX_VALUE
        visibleApps.forEachIndexed { idx, other ->
            val t = other.pkg?.let { findTile(it) } ?: return@forEachIndexed
            t.getLocationOnScreen(loc)
            val cx = loc[0] + t.width / 2f
            val cy = loc[1] + t.height / 2f
            val d = (cx - x) * (cx - x) + (cy - y) * (cy - y)
            if (d < best) { best = d; to = idx }
            if (idx == visibleApps.size - 1) {
                val afterLast = y > loc[1] + t.height || (y >= loc[1] && x > cx)
                if (afterLast) { to = idx; best = 0f }
            }
        }
        if (from >= 0 && to >= 0 && to != from) {
            val item = visibleApps.removeAt(from)
            visibleApps.add(to, item)
            saveOrder(visibleApps.mapNotNull { it.pkg })
        }
        populate(keep = true, focusPkg = pkg)
    }

    private fun endMove() {
        movingPkg = null
        Toast.makeText(this, "Placed", Toast.LENGTH_SHORT).show()
    }

    private fun showMenu(app: AppEntry) {
        val pkg = app.pkg ?: return
        val flags = runCatching { packageManager.getApplicationInfo(pkg, 0).flags }.getOrDefault(0)
        val pureSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        val options = ArrayList<String>().apply {
            add("Move"); add("Hide")
            add(if (pureSystem) "App info (system app, can only be disabled)" else "Uninstall")
        }
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(app.label)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        movingPkg = pkg
                        Toast.makeText(this, "Use the arrows to move, press OK to place", Toast.LENGTH_LONG).show()
                    }
                    1 -> {
                        val h = hiddenSet(); h.add(pkg); saveHidden(h)
                        val idx = visibleApps.indexOfFirst { it.pkg == pkg }
                        val next = visibleApps.getOrNull(idx + 1)?.pkg ?: visibleApps.getOrNull(idx - 1)?.pkg
                        populate(keep = true, focusPkg = next)
                    }
                    else -> {
                        if (pureSystem) open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$pkg")))
                        else open(Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:$pkg")))
                    }
                }
            }
            .show()
    }

    private fun showHidden() {
        val pkgs = hiddenSet().toList()
        val pm = packageManager
        val labels = pkgs.map { p -> runCatching { pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString() }.getOrDefault(p) }.toTypedArray()
        val checked = BooleanArray(pkgs.size)
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Hidden apps: tick to show again")
            .setMultiChoiceItems(labels, checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Show selected") { _, _ ->
                val h = hiddenSet()
                pkgs.indices.filter { checked[it] }.forEach { h.remove(pkgs[it]) }
                saveHidden(h)
                populate(keep = true)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun tileCard(row: View?): View? = ((row as? FrameLayout)?.getChildAt(0))

    private fun firstTile(): View? = tileCard((rows.getChildAt(1) as? LinearLayout)?.getChildAt(0))

    private fun findTile(pkg: String): View? {
        for (r in 1 until rows.childCount) {
            val dock = rows.getChildAt(r) as? LinearLayout ?: continue
            for (c in 0 until dock.childCount) {
                val card = tileCard(dock.getChildAt(c))
                if (card?.tag == pkg) return card
            }
        }
        return null
    }

    private fun loadApps(): MutableList<AppEntry> {
        val pm = packageManager
        val hidden = hiddenSet()
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        fun collect(category: String) {
            val i = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(i, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || pkg in hidden || !seen.add(pkg)) continue
                out.add(AppEntry(ri.loadLabel(pm).toString(), ComponentName(pkg, ri.activityInfo.name), ri))
            }
        }
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER)
        collect(Intent.CATEGORY_LAUNCHER)
        // Pre-installed system apps that have a screen but no launcher icon (TV channel setup, keyboard, voice search…).
        // Never factory/test menus, setup wizards, system dialogs or Google's internal screens.
        val skip = listOf("factory", "setupwizard", "tvsetup", "provision", "vpndialogs", "iperf", "com.google.android.gms", "com.android.systemui", "cts")
        fun score(n: String) = when {
            n.contains("Main") -> 0
            n.contains("Settings") && !n.contains("SpellChecker") && !n.contains("Dictionary") -> 1
            n.contains("Wizard") || n.contains("Dialog") -> 9
            else -> 2
        }
        // Extras must not repeat an app that is already shown (several "Settings" tiles) and must have a real name.
        val usedLabels = out.map { it.label.trim().lowercase() }.toHashSet()
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN), 0)
            .filter { ri ->
                val ai = ri.activityInfo
                val pkg = ai.packageName
                (ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && ai.exported && ai.enabled &&
                    ai.applicationInfo.enabled && ai.permission == null && pkg != packageName && pkg !in hidden && pkg !in seen &&
                    skip.none { pkg.contains(it) } && !ai.name.contains("Dialog")
            }
            .groupBy { it.activityInfo.packageName }
            .forEach { (pkg, list) ->
                val ri = list.minWithOrNull(compareBy({ score(it.activityInfo.name) }, { it.activityInfo.name.length })) ?: return@forEach
                val label = ri.loadLabel(pm).toString().trim()
                // Skip nameless or raw-package-name entries (e.g. "com.droidlogic.promptuse") and labels already in the grid.
                if (label.isEmpty() || label.startsWith("com.") || label.startsWith("org.") || !usedLabels.add(label.lowercase())) return@forEach
                if (seen.add(pkg)) out.add(AppEntry(label, ComponentName(pkg, ri.activityInfo.name), ri))
            }
        val saved = savedOrder()
        val favs = listOf("youtube", "netflix")
        val favPkgs = (prefs.getString("favs", "") ?: "").split("|").filter { it.isNotEmpty() }
        out.sortWith(compareBy(
            { e -> favPkgs.indexOf(e.pkg).let { if (it < 0) Int.MAX_VALUE else it } },
            { e -> saved.indexOf(e.pkg).let { if (it < 0) Int.MAX_VALUE else it } },
            { e -> favs.indexOfFirst { e.label.lowercase().contains(it) }.let { if (it < 0) 99 else it } },
            { it.label.lowercase() }
        ))
        // Lets Settings > Favorite apps list every app the launcher shows.
        prefs.edit().putString("applist", out.joinToString("\n") { "${it.pkg}\t${it.label}" }).apply()
        return out
    }

    private fun populate(keep: Boolean = false, focusPkg: String? = null) {
        rows.removeAllViews()
        visibleApps = loadApps()
        val display = ArrayList<AppEntry>(visibleApps)
        display.add(AppEntry("Settings", null, null, 1))
        val hiddenCount = hiddenSet().size
        if (hiddenCount > 0) display.add(AppEntry("Hidden apps ($hiddenCount)", null, null, 2))
        val width = resources.displayMetrics.widthPixels
        val gap = dp(18)
        val tileW = (width - dp(64) - dp(40) - gap * (perRow - 1)) / perRow
        val tileH = tileW * 9 / 16 + dp(8)
        rows.addView(View(this), LinearLayout.LayoutParams(-1, spacerH))
        display.chunked(perRow).forEachIndexed { rowIndex, chunk ->
            val dock = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
                setPadding(dp(20), dp(24), dp(20), dp(24))
                if (rowIndex == 0) background = GradientDrawable().apply { cornerRadius = dp(32).toFloat(); setColor(0x40FFFFFF); setStroke(1, 0x66FFFFFF) }
            }
            chunk.forEachIndexed { i, app ->
                dock.addView(tile(app, tileW, tileH, rowIndex), LinearLayout.LayoutParams(tileW, tileH).apply { if (i < chunk.size - 1) marginEnd = gap })
            }
            rows.addView(dock, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = if (rowIndex == 0) dp(18) else dp(30) })
        }
        val rowCount = (display.size + perRow - 1) / perRow
        val rowH = tileH + dp(48) + dp(30)
        val gridH = rowCount * rowH + dp(40)
        gridTop = maxOf(dp(150),(resources.displayMetrics.heightPixels - gridH) / 2)
        if (!keep) {
            gridMode = false
            scroll.scrollTo(0, 0)
            dim.alpha = 0f
            bigBox.alpha = 1f
        }
        val target = focusPkg?.let { findTile(it) } ?: firstTile()
        target?.post { target.requestFocus() }
    }

    private fun tile(app: AppEntry, w: Int, h: Int, rowIndex: Int): View {
        val radius = dp(18).toFloat()
        val card = RoundFrame(this, radius).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF353A5C.toInt(), 0xFF1C1F3A.toInt())).apply { cornerRadius = radius }
            isFocusable = true
            isClickable = true
            tag = app.pkg
            hoverFocus(this)
            foreground = ColorDrawable(Color.TRANSPARENT)
        }
        val pm = packageManager
        val banner = app.info?.activityInfo?.let { runCatching { it.loadBanner(pm) }.getOrNull() }
        val img = ImageView(this)
        if (banner != null) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setImageDrawable(banner)
            card.addView(img, FrameLayout.LayoutParams(-1, -1))
        } else {
            val icon = app.info?.loadIcon(pm)
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            if (icon != null) img.setImageDrawable(icon) else img.setImageResource(android.R.drawable.ic_menu_preferences)
            val iconView: View = if (app.kind == 1) StatusIcon(this, StatusIcon.GEAR) else img
            card.addView(iconView, FrameLayout.LayoutParams(h * 55 / 100, h * 55 / 100, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply { topMargin = h * 10 / 100 })
            val name = TextView(this).apply {
                text = app.label; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER; maxLines = 1
                setPadding(dp(6), 0, dp(6), dp(8))
            }
            card.addView(name, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        }
        val label = TextView(this).apply {
            text = app.label; setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; maxLines = 1
            alpha = 0f
            translationY = (h + dp(16)).toFloat()
        }
        val wrapper = FrameLayout(this).apply {
            clipChildren = false
            addView(card, FrameLayout.LayoutParams(-1, -1))
            addView(label, FrameLayout.LayoutParams(-1, -2))
        }
        card.setOnFocusChangeListener { v, has ->
            if (has) { Tick.play(this, 0); setGridMode(rowIndex >= 1) }
            label.animate().alpha(if (has && rowIndex >= 1) 1f else 0f).setDuration(200).start()
            val s = if (has) 1.12f else 1f
            v.animate().cancel()
            // Explicit type: Android 5.0 (API 21) does not know BaseInterpolator as a TimeInterpolator.
            val ip: android.animation.TimeInterpolator = if (has) OvershootInterpolator(2.2f) else DecelerateInterpolator()
            v.animate().scaleX(s).scaleY(s).translationY(if (has) -dp(8).toFloat() else 0f)
                .setInterpolator(ip)
                .setDuration(if (has) 260 else 160).start()
            (v as FrameLayout).foreground = if (has)
                GradientDrawable().apply { cornerRadius = radius; setStroke(dp(3), Color.WHITE); setColor(Color.TRANSPARENT) }
            else ColorDrawable(Color.TRANSPARENT)
        }
        card.setOnClickListener { v ->
            Tick.play(this, 1)
            when (app.kind) {
                2 -> showHidden()
                else -> {
                    val opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.width, v.height).toBundle()
                    open(
                        if (app.kind == 1) Intent(this@MainActivity, SettingsActivity::class.java)
                        else Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component),
                        opts
                    )
                }
            }
        }
        if (app.kind == 0) {
            var touching = false
            var dragging = false
            var moved = false
            var startX = 0f
            var startY = 0f
            card.setOnLongClickListener {
                if (touching) {
                    dragging = true
                    moved = false
                    card.parent?.requestDisallowInterceptTouchEvent(true)
                    wrapper.translationZ = dp(12).toFloat()
                    (wrapper.parent as? View)?.translationZ = dp(12).toFloat()
                    Toast.makeText(this, "Drag to move, release to place", Toast.LENGTH_SHORT).show()
                } else {
                    movingPkg = app.pkg
                    Toast.makeText(this, "Use the arrows to move, press OK to place", Toast.LENGTH_LONG).show()
                }
                true
            }
            card.setOnTouchListener { _, ev ->
                when (ev.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> { touching = true; startX = ev.rawX; startY = ev.rawY; false }
                    android.view.MotionEvent.ACTION_MOVE -> if (dragging) {
                        wrapper.translationX = ev.rawX - startX
                        wrapper.translationY = ev.rawY - startY
                        if (Math.abs(ev.rawX - startX) > dp(14) || Math.abs(ev.rawY - startY) > dp(14)) moved = true
                        true
                    } else false
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        touching = false
                        if (dragging) {
                            dragging = false
                            wrapper.translationX = 0f
                            wrapper.translationY = 0f
                            wrapper.translationZ = 0f
                            (wrapper.parent as? View)?.translationZ = 0f
                            if (moved) dropOn(app.pkg, ev.rawX, ev.rawY) else showMenu(app)
                            true
                        } else false
                    }
                    else -> false
                }
            }
        }
        return wrapper
    }

    private fun hasInputs(): Boolean {
        // A TV with its own input menu, or any TV input registered with Android (HDMI, AV, antenna...).
        if (packageManager.resolveActivity(Intent("com.toptech.tv.action.INPUT_ACTIVITY"), 0) != null) return true
        return runCatching {
            (getSystemService(Context.TV_INPUT_SERVICE) as android.media.tv.TvInputManager).tvInputList.isNotEmpty()
        }.getOrDefault(false)
    }

    // TV source picker (HDMI, AV, VGA, antenna). Uses the TV maker's own picker when the firmware has one.
    // Dropdown under the Input button: pick HDMI 1-3, AV, VGA or antenna and it plays full screen.
    private fun openInputs() {
        // The TV's own input menu switches sources exactly like the stock launcher did.
        val tvMenu = Intent("com.toptech.tv.action.INPUT_ACTIVITY")
        if (packageManager.resolveActivity(tvMenu, 0) != null) { open(tvMenu); return }
        val tim = getSystemService(Context.TV_INPUT_SERVICE) as android.media.tv.TvInputManager
        val inputs = runCatching { tim.tvInputList }.getOrDefault(emptyList()).mapNotNull { info ->
            val l = runCatching { info.loadLabel(this)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Triple(info.id, l, info.type)
        }
        // Passthrough inputs (HDMI, AV, VGA) first, then antenna / digital TV tuners.
        val sorted = inputs.sortedWith(compareBy({ if (it.third == android.media.tv.TvInputInfo.TYPE_TUNER) 1 else 0 }, { it.second.lowercase() }))
        val labels = ArrayList(sorted.map { it.second })
        val vendor: Intent? = null
        if (labels.isEmpty()) { Toast.makeText(this, "This TV has no inputs", Toast.LENGTH_SHORT).show(); return }
        val popup = android.widget.ListPopupWindow(this)
        popup.anchorView = inputGroup
        popup.width = dp(160)
        popup.isModal = true
        popup.setBackgroundDrawable(GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0xF0202433.toInt()); setStroke(1, 0x66FFFFFF) })
        popup.setAdapter(object : android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val name = labels[position]
                val u = name.uppercase()
                val kind = when { u.contains("HDMI") -> StatusIcon.HDMI; u.startsWith("AV") -> StatusIcon.AV; u.contains("VGA") -> StatusIcon.VGA; else -> StatusIcon.ANTENNA }
                return LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(5), dp(14), dp(5))
                    addView(StatusIcon(context, kind), LinearLayout.LayoutParams(dp(22), dp(22)))
                    addView(TextView(context).apply { text = name; setTextColor(Color.WHITE); textSize = 14f; maxLines = 1; setPadding(dp(10), 0, 0, 0) })
                }
            }
        })
        popup.setOnItemClickListener { _, _, pos, _ ->
            popup.dismiss()
            if (pos < sorted.size) runCatching { startActivity(Intent(this, InputActivity::class.java).putExtra("inputId", sorted[pos].first).putExtra("label", sorted[pos].second)) }
                .onFailure { android.util.Log.e("TVH", "input open failed", it) }
            else vendor?.let { open(it) }
        }
        popup.show()
        popup.listView?.requestFocus()
    }

    private fun open(i: Intent, opts: Bundle? = null) {
        if (i.component?.className?.endsWith("SettingsActivity") != true) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        runCatching { startActivity(i, opts) }.onFailure { android.util.Log.e("TVH", "open failed", it) }
    }
}
