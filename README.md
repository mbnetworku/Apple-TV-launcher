# Apple TV Home

An Apple TV–style home screen for Android TV / Google TV, written in Kotlin with no extra libraries.
Built for an old, low-memory TV (Android 8.0, 1.9 GB RAM), so it stays light.

## What it does

- Frosted dock with a row of rounded app tiles. The selected tile pops up with a small bounce and a white outline.
- Press Down for the "All apps" view: the first row rises, the other rows come up, the background dims and the app name appears under the selected tile. How far it rises depends on how many apps you have.
- Rotating wallpapers with a slow zoom and cross-fade (`app/src/main/assets/wallpapers`). With no images the launcher draws its own mountain scene.
- Fixed top bar: Bluetooth (when on), LAN (when the cable is connected), Wi-Fi network name, 12-hour clock and a settings button. Each item opens the matching Settings page.
- Long-press OK (or press Menu) on an app: Move, Hide, Uninstall (or App info for built-in system apps). A "Hidden apps" tile brings hidden apps back.
- Apple TV–style Settings: Wi-Fi (scan, connect with password), Bluetooth (on/off, scan, pair, forget), Speed boost (free memory, animation speed) and shortcuts to the system Sound, Display, Apps, Storage, Date, Language, Accessibility and About screens.

## Download

`releases/AppleTVHome-1.0.apk` (about 8.6 MB, debug-signed, Android 8.0 or newer).

## Quick download (Downloader app)

Open **Downloader** on the TV, choose the address box and type:

`github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk`

It always downloads the newest version.

## Install, method 1: with USB debugging (adb)

1. On the TV open Settings → Device Preferences → About and click **Build** 7 times. This unlocks Developer options.
2. Go back to Developer options and turn on **USB debugging** (some TVs call it ADB debugging or Network debugging). If the TV has no network option, reboot it once after switching it on.
3. On your PC install the Android platform-tools (<https://developer.android.com/tools/releases/platform-tools>).
4. Find the TV's IP address (Settings → Network) and connect: `adb connect <tv-ip>:5555`. Accept the "Allow USB debugging?" prompt on the TV (tick **Always allow**).
5. Install: `adb install -r releases/AppleTVHome-1.0.apk`
6. Make it the Home app: `adb shell cmd package set-home-activity com.mb.appletvlauncher/com.mb.tvlauncher.MainActivity`
7. Optional, to let the Speed boost page change animation speed: `adb shell pm grant com.mb.appletvlauncher android.permission.WRITE_SECURE_SETTINGS`

## Install, method 2: without USB debugging

Pick whichever is easiest for you.

**A. USB stick + a file manager**
1. Copy `AppleTVHome-1.0.apk` to a USB stick and plug it into the TV.
2. Install a file manager on the TV from the Play Store (for example X-plore or File Commander).
3. Open the stick in the file manager and select the APK.
4. If Android asks, allow the file manager to **Install unknown apps** (Settings → Security & restrictions → Unknown sources), then press Install.

**B. The Downloader app**
1. Install **Downloader** (by AFTVnews) from the Play Store.
2. Open it and enter this address: `https://github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk`
3. Let it download, press Install, and allow unknown apps for Downloader if asked.

**C. From your phone**
Send the APK to the TV with an app such as "Send files to TV", then open it on the TV and install it.

**Making it the default Home app without adb**
Open the app once ("Apple TV Home" appears in your apps list), then press the Home button. If the TV asks which Home app to use, choose **Apple TV Home** and **Always**. Some TVs ignore this and keep opening their own launcher. In that case use method 1 for step 6, or disable the stock launcher from the TV's Apps settings.

To go back: uninstall Apple TV Home. If you disabled the stock launcher, re-enable it (`adb shell pm enable <package>` or the TV's Apps settings).

## Build

Needs JDK 17 and the Android SDK (platform 34, build-tools 34.0.0). Create `local.properties` with your `sdk.dir`, then run `gradle assembleDebug`.

The app targets SDK 22 on purpose. On the Android 8.0 TV it was built for, newer targets get an empty Wi-Fi scan list.

## Wallpapers

The pictures in `app/src/main/assets/wallpapers` are not mine. They belong to their respective photographers and artists, and are included only as sample wallpapers. Replace them with your own images (`.jpg`, `.png` or `.webp`, about 1920x1080) or remove any you do not have the right to share.
