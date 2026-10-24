# Apple TV Home: notes for Claude and other contributors

Android TV launcher in Kotlin (no AndroidX, minSdk 26, targetSdk 22). Package `com.mb.appletvlauncher`, code in `com.mb.tvlauncher`.

## Files
- `MainActivity.kt`: home screen. Top bar (Bluetooth, LAN, Wi-Fi name, clock, gear), dock rows of app tiles, "All apps" mode on Down, Move/Hide/Uninstall menu, saved order and hidden apps in SharedPreferences `home`.
- `SettingsActivity.kt`: Apple TV–style settings (Wi-Fi, Bluetooth, Speed boost, shortcuts to system screens). Accepts an intent extra `page` = `wifi` or `bt` to open that page directly.
- `WallpaperView.kt`: cross-fades the images in `assets/wallpapers` every 40 s; falls back to `SceneView.kt` (drawn mountains) when the folder has no images.
- `RoundFrame.kt`: clips a tile to rounded corners. `StatusIcon.kt`: drawn Wi-Fi, LAN, Bluetooth and gear icons.

## Rules and gotchas
- Never commit personal data: no Wi-Fi names, IP addresses, MAC addresses, e-mail addresses or local file paths (`local.properties` is ignored).
- Any parent of a tile that has padding needs `clipToPadding = false` and `clipChildren = false`, or the enlarged selected tile gets flat corners.
- `targetSdk = 22` is deliberate: Android 8.0 TV firmware returned empty Wi-Fi scan lists to apps targeting 23 or newer. Changing the target needs an uninstall first, which clears the default Home setting.
- The "Speed boost" animation setting needs `adb shell pm grant com.mb.appletvlauncher android.permission.WRITE_SECURE_SETTINGS`.
- Be careful with scripted key presses in Settings: a stray OK can toggle Wi-Fi or Bluetooth.
- Never disable the only Home app on a device without first checking that this launcher is installed and set as default.

## Build
`gradle assembleDebug` with JDK 17, Gradle 8.7, Android SDK platform 34 and build-tools 34.0.0. APK output: `app/build/outputs/apk/debug/app-debug.apk`; the published copy is `releases/AppleTVHome-1.0.apk`.
