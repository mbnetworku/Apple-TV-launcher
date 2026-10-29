# Apple TV Home: notes for Claude and other contributors

Android TV launcher in Kotlin (no AndroidX, minSdk 21, targetSdk 22). Package `com.mb.appletvlauncher`, code in `com.mb.tvlauncher`. This repo also holds `VolumeBar/`, a tiny separate app that puts a volume control in the notification area.

Target device the project was built and tested on: an Android 5.1.1 TV box (Amlogic, model KII PRO, 1.8 GB RAM, 32-bit ARM, rooted, controlled with a USB mouse, no remote). Everything below assumes a box like that.

## Files
- `app/src/main/java/com/mb/tvlauncher/MainActivity.kt`: home screen. Large clock and date top left (12/24-hour switch in Settings, key `clock24` in prefs `home`), top bar (volume, Bluetooth, LAN, Wi-Fi name, clock that opens Date and time settings, gear), dock row and grid of app tiles, "All apps" mode on Down, saved order and hidden apps in SharedPreferences `home`.
- `SettingsActivity.kt`: Apple TV-style settings (Wi-Fi, Bluetooth, Speed boost, status bar, clock 12/24, shortcuts to system screens). Accepts an intent extra `page` = `wifi` or `bt` to open that page directly.
- `WallpaperView.kt`: cross-fades the images in `assets/wallpapers` every 40 s; falls back to `SceneView.kt` (drawn mountains) when the folder has no images.
- `RoundFrame.kt`: clips a tile to rounded corners. `StatusIcon.kt`: drawn Wi-Fi, LAN, Bluetooth, gear and volume icons.
- `VolumeBar/`: separate app `com.mb.volumebar`. A foreground service shows an ongoing notification with a level bar, a minus and a plus button. No overlay, nothing drawn on the screen. Starts at boot (`BootReceiver`) and from its own launcher tile.

## What the launcher does (UI behaviour)
- Big clock and date on the left; the clock fades out in "All apps" mode so it never covers tiles.
- Top bar: volume button (click opens a bar; click or drag the bar, mouse wheel, or Left/Right keys change it, OK closes), Bluetooth, LAN or Wi-Fi name (opens the network page), the time (opens Date and time settings), and the gear (opens Settings).
- Moving apps: press and hold an app with the mouse, drag it, release on another tile to put it there (release after the last tile = move to the end). Release without moving = Move / Hide / Uninstall menu. Remote: long-press OK starts an arrow-key move, OK places it, the Menu key opens the menu.
- Wallpapers: `assets/wallpapers/*.jpg`, 1920x1080, rotate every 40 s.

## Install everything on a box from scratch (works with Claude Code)
Needs: JDK 17, Android SDK platform 34 + build-tools 34.0.0, platform-tools (`adb`), Gradle 8.7, and the TV on the same network with network ADB on (port 5555). Do not put the TV's IP address in the repo; use `<TV_IP>` below.
1. Connect: `adb connect <TV_IP>:5555`, accept the prompt on the TV. Check `adb shell getprop ro.build.version.release` and `ro.product.cpu.abilist`.
2. Back up first: `adb shell pm list packages -f` and `adb shell su -c 'pm list packages -d'` into a folder outside the repo, plus a script that re-enables whatever gets disabled.
3. Build: `gradle assembleDebug` in this folder, and again inside `VolumeBar/`. Outputs: `app/build/outputs/apk/debug/app-debug.apk`. Check `local.properties` has `sdk.dir` with escaped backslashes (`C\:\\Users\\...`); a single-backslash path makes Gradle fail with "filename ... syntax is incorrect".
4. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk` for both apps. A version lower than the installed one fails with `INSTALL_FAILED_VERSION_DOWNGRADE`; raise `versionCode` and `versionName` in `app/build.gradle.kts` before every release. An APK built with a newer minSdk fails with `INSTALL_FAILED_OLDER_SDK`.
5. Make it the default Home. Android 5 has no `cmd package set-home-activity`. Disable the other launchers with root, one at a time, and check after each: `adb shell "su -c 'pm disable <stock.launcher.package>'"`, then press Home and check `dumpsys window | grep mCurrentFocus` shows `com.mb.appletvlauncher`. On the KII PRO the stock ones were `com.vs.vslauncher` (the real Home) and `com.android.launcher`. Plain `adb shell pm disable-user` is refused on this firmware (SecurityException); `su -c 'pm disable ...'` works. Never disable the stock launcher before this launcher is installed.
6. Hide the system status bar everywhere (optional): `adb shell settings put global policy_control immersive.status=*`. Undo: `settings put global policy_control null`.
7. Volume Bar: start it once with `adb shell am start -n com.mb.volumebar/.LaunchActivity`. It needs no extra permission (targetSdk 22).
8. Revert to the stock Home: `su -c 'pm enable <stock launcher>'` for each disabled one, then `su -c 'pm disable com.mb.appletvlauncher'`. To go forward again, enable this launcher and disable the others.

## Debloat done on the reference box (all reversible)
- Uninstalled: Telegram, ExpressVPN, Speed VPN, VPN Unlimited, Psiphon, Aptoide, Opera, UC Browser, Netflix.
- Disabled with root (`pm disable`): demo wallpapers and screensaver (`com.android.phasebeam`, `com.android.wallpaper.holospiral`, `com.android.musicvis`, `com.android.dreams.phototable`, `com.android.wallpaper.livepicker`), `com.android.soundrecorder`, `com.android.exchange`, `com.google.android.feedback`, two factory-test apps (`com.videostrong.*`), the stock video player `com.droidlogic.videoplayer`, and the stock launchers above.
- Never disable: Amlogic TV and remote services (`com.droidlogic.*` core), Play Store and Play Services, `com.android.settings`, `com.android.systemui`, the keyboard, anything named factory or fac menu should not even be opened.

## Rules and gotchas
- Never commit personal data: no Wi-Fi names, IP addresses, MAC addresses, e-mail addresses or local file paths (`local.properties` is ignored).
- Any parent of a tile that has padding needs `clipToPadding = false` and `clipChildren = false`, or the enlarged selected tile gets flat corners.
- `targetSdk = 22` is deliberate: Android 8.0 TV firmware returned empty Wi-Fi scan lists to apps targeting 23 or newer. Changing the target needs an uninstall first, which clears the default Home setting.
- Do not use `bringToFront()` on a tile inside its row while dragging: it re-orders the row and the tile jumps away from the pointer. Raise it with `translationZ` (tile and its row) instead.
- Android 5 has no `cmd`, and `settings list` is missing. `adb shell input tap x y` hits the exact pixel (screen is 1920x1080, density 240), but it cannot do hold-then-drag, so a real mouse drag cannot be tested over ADB.
- `adb shell input keyevent KEYCODE_VOLUME_*` does not change the volume on this box; change it with the launcher's volume button, the Volume Bar notification, or `AudioManager`.
- Some prefs live on the device only: `hide_bar` (hides the whole top bar; it was `true` once and made the bar look missing), `clock24`, `order`, `hidden`. Read them with `adb shell run-as com.mb.appletvlauncher cat shared_prefs/home.xml` (debug build).
- The "Speed boost" animation setting needs `adb shell pm grant com.mb.appletvlauncher android.permission.WRITE_SECURE_SETTINGS`.
- Be careful with scripted key presses in Settings: a stray OK can toggle Wi-Fi or Bluetooth or open a system screen. Take a screenshot (`adb shell screencap -p`) after each step.
- Never disable the only Home app on a device without first checking that this launcher is installed and set as default.
- Push to GitHub only when the owner asks.

## Build
`gradle assembleDebug` with JDK 17, Gradle 8.7, Android SDK platform 34 and build-tools 34.0.0. APK output: `app/build/outputs/apk/debug/app-debug.apk`; the published copy is `releases/AppleTVHome-1.0.apk` (old, needs a newer Android; build from source for old boxes).

## Version history
- 1.0.1: base launcher, minSdk 21.
- 1.0.2: large clock and date, 12/24-hour setting, clock opens Date and time settings, new wallpapers.
- 1.0.3: volume button and slider in the top bar; large clock hidden in All apps.
- 1.0.4: hold and drag apps with the mouse; new wallpaper.
- 1.0.5 / 1.0.6: volume button removed, then put back next to Bluetooth.
- VolumeBar 1.0 / 1.1: notification-only volume control; 1.1 has the blue speaker icon.
