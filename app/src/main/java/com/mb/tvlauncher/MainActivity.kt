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
            clock.text = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            updateStatus()
            handler.postDelayed(this, 5_000)
        }
    }

    private val perRow = 6
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
                (v.background as GradientDrawable).setColor(if (has) 0x55FFFFFF else Color.TRANSPARENT)
                v.animate().scaleX(if (has) 1.1f else 1f).scaleY(if (has) 1.1f else 1f).setDuration(140).start()
            }
            setOnClickListener {
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
        clock = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f }
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
        val gap = dp(6)
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
        root.addView(pill, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = dp(28); marginEnd = dp(40) })

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
        pill.visibility = if (prefs.getBoolean("hide_bar", false)) View.GONE else View.VISIBLE
        if (movingPkg == null) populate()
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        movingPkg = null
        firstTile()?.requestFocus()
        setGridMode(false)
    }

    private fun setGridMode(on: Boolean) {
        if (gridMode == on) return
        gridMode = on
        dim.animate().alpha(if (on) 0.6f else 0f).setDuration(350).start()
        val target = if (on) maxOf(0, spacerH - gridTop) else 0
        ObjectAnimator.ofInt(scroll, "scrollY", scroll.scrollY, target).setDuration(420).apply {
            interpolator = DecelerateInterpolator(1.6f)
        }.start()
    }

    @Deprecated("Stay on the home screen")
    override fun onBackPressed() { }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
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
        val saved = savedOrder()
        val favs = listOf("youtube", "netflix")
        out.sortWith(compareBy(
            { e -> saved.indexOf(e.pkg).let { if (it < 0) Int.MAX_VALUE else it } },
            { e -> favs.indexOfFirst { e.label.lowercase().contains(it) }.let { if (it < 0) 99 else it } },
            { it.label.lowercase() }
        ))
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
        gridTop = maxOf(dp(92), (resources.displayMetrics.heightPixels - gridH) / 2)
        if (!keep) {
            gridMode = false
            scroll.scrollTo(0, 0)
            dim.alpha = 0f
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
            if (has) setGridMode(rowIndex >= 1)
            label.animate().alpha(if (has && rowIndex >= 1) 1f else 0f).setDuration(200).start()
            val s = if (has) 1.12f else 1f
            v.animate().cancel()
            v.animate().scaleX(s).scaleY(s).translationY(if (has) -dp(8).toFloat() else 0f)
                .setInterpolator(if (has) OvershootInterpolator(2.2f) else DecelerateInterpolator())
                .setDuration(if (has) 260 else 160).start()
            (v as FrameLayout).foreground = if (has)
                GradientDrawable().apply { cornerRadius = radius; setStroke(dp(3), Color.WHITE); setColor(Color.TRANSPARENT) }
            else ColorDrawable(Color.TRANSPARENT)
        }
        card.setOnClickListener { v ->
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
        if (app.kind == 0) card.setOnLongClickListener { showMenu(app); true }
        return wrapper
    }

    private fun open(i: Intent, opts: Bundle? = null) {
        if (i.component?.className?.endsWith("SettingsActivity") != true) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        runCatching { startActivity(i, opts) }.onFailure { android.util.Log.e("TVH", "open failed", it) }
    }
}
