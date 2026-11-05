<div align="center">

# 📺 Apple TV Home

**A clean, fast, Apple TV–style home screen for old and low-cost Android TV boxes.**

[![Latest release](https://img.shields.io/github/v/release/mbnetworku/Apple-TV-launcher?label=latest&color=2ea44f)](https://github.com/mbnetworku/Apple-TV-launcher/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/mbnetworku/Apple-TV-launcher/total?color=blue)](https://github.com/mbnetworku/Apple-TV-launcher/releases)
![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-no%20libraries-7F52FF?logo=kotlin&logoColor=white)
[![License: MIT](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

<br>

### [⬇️ Download the latest version](https://github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk)

This link always gives you the newest version.

📥 **Downloader app on the TV**: type this address

```
github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk
```

🖱️ Works with a mouse, no remote needed · 🪶 Runs on 1.5 GB RAM · 🔒 No internet permission, no ads, no accounts · ↩️ Fully reversible

</div>

---

## 🧭 Quick links

| | |
|---|---|
| ✨ [What it does](#features) | 📦 [Install](#install) |
| 🏠 [Make it the Home app](#home-app) | ⚡ [One-command setup for a new box](#setup) |
| 📝 [Release history](#releases) | 🛠️ [Troubleshooting](#troubleshooting) |
| 👩‍💻 [Build from source](#build) | 📚 [More guides](#guides) |

---

<a name="features"></a>

## ✨ What it does

| | Feature | Details |
|:-:|---|---|
| 🕰️ | **Big clock and date** | 12 or 24 hour, optional display time zone. Fades away in "All apps" so it never covers tiles. |
| 🧊 | **Frosted dock** | Rounded app tiles; the selected one pops up with a bounce and a white outline. |
| ⬇️ | **All apps view** | Press Down: rows slide up, the background dims and the app name appears. |
| 🖱️ | **Drag to reorder** | Hold an app with the mouse and drop it on another tile. Remote: long-press OK, then use the arrow keys. |
| 🙈 | **Hide / uninstall** | Release without moving (or press Menu) for Move, Hide and Uninstall. A "Hidden apps" tile brings them back. |
| 🔊 | **Volume button** | Click, drag, mouse wheel or Left/Right. Works on boxes whose volume keys do nothing. |
| 📡 | **Status bar** | Bluetooth, LAN, Wi-Fi name, clock and gear. Each one opens its own page. |
| 📺 | **Input button** | HDMI, AV, VGA and antenna through the TV's own menu, also from the Source key. Only shown on TVs with inputs. |
| 🖼️ | **Wallpapers** | Off, one picture, or rotate (15 s to 15 min) with a slow zoom. Draws a mountain scene if there are no images. |
| 🔉 | **Tick sound** | Soft click on arrows and OK; turn it on or off. |

<details>
<summary><b>⚙️ All Settings pages (click to open)</b></summary>

| Page | What you get |
|---|---|
| 📶 Wi-Fi | Scan, connect with a password |
| 🔵 Bluetooth | On/off, scan, pair, forget |
| 🌐 Network | LAN or Wi-Fi, IPv4 address, gateway, subnet mask, DNS, updated live |
| 💾 Storage | Free space, room for apps, your files, apps and data, system, whole chip. Refreshes by itself |
| 🚀 Speed boost | Free memory, animation speed |
| 🖼️ Wallpaper | Off, one picture or rotate, interval, motion |
| 🕰️ Clock | 12/24 hour, clock time zone (display only) |
| 🔍 Icon size / ⭐ Favorite apps | Bigger or smaller tiles, pin favorites first |
| 📺 Start-up input | Opens one TV input when the TV starts |
| 🔉 Sound effects | Tick sound on or off |
| 🧩 Shortcuts | System Sound, Display, Apps, Date, Language, Accessibility, About |

</details>

<details>
<summary><b>🔊 Volume Bar: the companion app</b></summary>

[`VolumeBar/`](VolumeBar) is a tiny separate app that puts a volume control (a level bar, minus and plus) in the notification area. It draws nothing over the screen, starts at boot, and needs no extra permission.

</details>

---

<a name="install"></a>

## 📦 Install

> 🔐 The APK is signed with the project's test key. Android asks you once to allow **unknown sources**. If an update is refused, uninstall the old version first.

<details open>
<summary><b>📥 Option A: Downloader app (easiest, done on the TV)</b></summary>

1. Install **Downloader** (by AFTVnews) from the Play Store.
2. Type `github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk`
3. Press **Install**, and allow unknown apps for Downloader if asked.

</details>

<details>
<summary><b>💾 Option B: USB stick and a file manager</b></summary>

1. Copy `AppleTVHome.apk` to a USB stick and plug it into the TV.
2. Install a file manager (for example X-plore or File Commander).
3. Open the stick, select the APK, allow **Install unknown apps** if asked, then press Install.

</details>

<details>
<summary><b>📱 Option C: from your phone</b></summary>

Send the APK with an app such as "Send files to TV", then open it on the TV and install it.

</details>

<details>
<summary><b>💻 Option D: from a PC with adb</b></summary>

1. On the TV: Settings → Device Preferences → About, click **Build** 7 times, then turn on **USB / network debugging** in Developer options.
2. Install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) on the PC.
3. Connect, and accept the prompt on the TV:

   ```bash
   adb connect <TV_IP>:5555
   ```

4. Install:

   ```bash
   adb install -r AppleTVHome.apk
   ```

5. Optional, lets Speed boost change the animation speed:

   ```bash
   adb shell pm grant com.mb.appletvlauncher android.permission.WRITE_SECURE_SETTINGS
   ```

</details>

---

<a name="home-app"></a>

## 🏠 Make it the Home app

1. Open **Apple TV Home** once, then press **Home**.
2. If the TV asks which Home app to use, choose **Apple TV Home → Always**.

<details>
<summary><b>The TV still opens its own launcher?</b></summary>

**Android 7 and newer:**

```bash
adb shell cmd package set-home-activity com.mb.appletvlauncher/com.mb.tvlauncher.MainActivity
```

**Android 5 and 6** (there is no `cmd`; root needed): disable the stock launcher, one at a time, and press Home after each one to check.

```bash
adb shell "su -c 'pm disable <stock.launcher.package>'"
```

> ⚠️ Never disable the stock launcher before Apple TV Home is installed and opens correctly.

**To go back:** uninstall Apple TV Home. If you disabled the stock launcher, enable it again first:

```bash
adb shell "su -c 'pm enable <stock.launcher.package>'"
```

</details>

---

<a name="setup"></a>

## ⚡ One-command setup for a new box

A PowerShell script connects to the box, backs up the package lists, builds and installs the launcher and Volume Bar, makes it the only Home app (root needed) and writes an undo script.

```powershell
powershell -ExecutionPolicy Bypass -File tools\Setup-New-Box.ps1 -TvIp <TV_IP> -Debloat -HideStatusBar
```

| Option | Effect |
|---|---|
| `-TvIp` | The box's IP address (required) |
| `-Debloat` | Disables demo and factory apps (reversible) |
| `-HideStatusBar` | Hides the system status bar everywhere |
| `-SkipBuild` | Uses prebuilt APKs from `dist\` |

📖 Full guide, debloat list and undo: **[SETUP-NEW-BOX.md](SETUP-NEW-BOX.md)**. With Claude Code, say: *"Set up my new TV box at <TV_IP> using SETUP-NEW-BOX.md"*.

---

<a name="releases"></a>

## 📝 Release history

🟢 = latest · 🆕 = new · 🐛 = bug fixed · 🔧 = changed

<details open>
<summary><b>🟢 1.2.5 · 5 Nov 2026 · Wallpaper settings</b></summary>

- 🆕 **Settings → Wallpaper**: off (plain dark background, lightest on slow boxes), one picture, or rotate.
- 🆕 Choose how often it changes (15 s to 15 min) and turn the slow zoom on or off.
- 🔧 Includes everything from 1.2.4.
- ℹ️ Built and compiled, not yet tested on a device.

</details>

<details>
<summary><b>1.2.4 · 5 Nov 2026 · Clock time zone, cleaner grid</b></summary>

- 🆕 **Settings → Clock time zone**: shows another time zone in the launcher, for boxes whose own time zone is unset and stuck on GMT. The device setting is not changed.
- 🐛 **Issue:** extra system apps showed the same name twice, or a raw package name. **Fix:** duplicates and package names are filtered out.

</details>

<details>
<summary><b>1.2.2 · 5 Nov 2026 · Network page, live Storage, tick sound</b></summary>

- 🆕 **Network page**: LAN or Wi-Fi, IPv4 address, gateway, subnet mask, DNS, updated live.
- 🆕 **Storage page refreshes itself**: free space right away, file totals about every 20 s.
- 🆕 **Tick sound** on arrows and OK, made in code, with an on/off switch.
- 🐛 **Crash on Android 5.0** caused by an animation interpolator: fixed.
- 🐛 Wi-Fi showed **"Connected to 0x"** instead of the network name: fixed.
- 🐛 Storage page **froze the box** while scanning links and deep folders: the scan is lighter now.
- 🐛 **Setup screens** appeared as apps in the grid: removed.
- 🐛 Settings **loaded twice** and the Bluetooth name could crash it: fixed.

</details>

<details>
<summary><b>1.1.9 · 5 Nov 2026 · Smarter Input button</b></summary>

- 🔧 The **Input** button is shown only on a TV that really has inputs (plain boxes have no inputs to open).
- 🔧 Settings rows are shorter, with smaller text and less spacing.

</details>

<details>
<summary><b>1.1.8 · 4 Nov 2026 · TV inputs, Storage, favorites</b></summary>

- 🆕 **Input** button and the remote's **Source** key open the TV's own input menu.
- 🐛 **Issue:** the Source key did nothing once the stock launcher was replaced. **Fix:** the launcher now listens for the TV menu's Source-key signal.
- 🆕 **Storage** page with accurate sizes.
- 🆕 **Icon size**, **Favorite apps** and **Start-up input** in Settings.
- 🆕 All system apps that have a screen are shown in the grid.
- 🔧 Release build (not debuggable) for smoother frames.

</details>

<details>
<summary><b>1.0.2 – 1.0.6 · 29 Oct 2026 · Clock, volume, drag and drop</b> (not published as separate downloads)</summary>

- 🆕 **1.0.2:** large clock and date, 12/24-hour setting, clock opens Date and time, three new wallpapers.
- 🆕 **1.0.3:** volume button and slider in the top bar.
- 🐛 **1.0.3:** the large clock **covered the tiles** in All apps: it now fades out.
- 🆕 **1.0.4:** hold and drag apps with the mouse, new wallpaper.
- 🐛 **1.0.4:** the dragged app **jumped away from the mouse pointer**: fixed.
- 🐛 **1.0.4:** releasing in the last row or a gap did nothing: it now drops on the nearest tile or at the end.
- 🔧 **1.0.5 / 1.0.6:** the volume button moved to the Volume Bar app, then came back next to Bluetooth.
- 🔧 Setup script: `-Gradle` option, fixed version check and adb error handling (tested on a second box).

</details>

<details>
<summary><b>1.1 and 1.0.1 · 24 Oct 2026 · Mouse support, Android 5</b></summary>

- 🆕 Mouse hover and click.
- 🆕 Status bar Shown / Hidden option in Settings.
- 🐛 **Issue:** it would not install on Android 5.1 boxes. **Fix:** Android 5.0 (minSdk 21) is now supported.
- 🔧 1.0.1 only fixed the version number.

</details>

<details>
<summary><b>1.0 · 24 Oct 2026 · First release</b></summary>

- 🆕 Apple TV–style home screen: dock, All apps, rotating wallpapers, Wi-Fi and Bluetooth settings. Android 8.0 and newer only.

</details>

All release pages and checksums: **[Releases](https://github.com/mbnetworku/Apple-TV-launcher/releases)**.

---

<a name="troubleshooting"></a>

## 🛠️ Troubleshooting

<details>
<summary><b>Click for common problems and fixes</b></summary>

| ❗ Problem | ✅ Fix |
|---|---|
| "App not installed", or the update is refused | Uninstall the old version, then install again (different signing key or older version). |
| `INSTALL_FAILED_OLDER_SDK` | The TV runs Android below 5.0, which is not supported. |
| The TV keeps opening its own launcher | See [Make it the Home app](#home-app). |
| The top bar is missing | Settings → Status bar → Shown. |
| The box feels slow | Settings → Wallpaper → Off, and Settings → Speed boost. |
| Speed boost can't change animations | Run the `pm grant` command in [Option D](#install). |
| The remote's volume keys do nothing | Use the volume button in the top bar or the Volume Bar app. |
| The time is wrong (GMT) | Settings → Clock time zone. |

</details>

---

<a name="build"></a>

## 👩‍💻 Build from source

<details>
<summary><b>Build steps</b></summary>

Needs JDK 17, Gradle 8.7, Android SDK platform 34 and build-tools 34.0.0.

1. Create `local.properties` with `sdk.dir`. On Windows escape the backslashes: `sdk.dir=C\:\\Users\\...`
2. Build:

   ```bash
   gradle assembleDebug
   ```

3. Output: `app/build/outputs/apk/debug/app-debug.apk`. Build the Volume Bar the same way inside `VolumeBar/`.

Raise `versionCode` and `versionName` in `app/build.gradle.kts` before every release.
`targetSdk 22` is deliberate: Android 8.0 TV firmware gives apps targeting 23+ an empty Wi-Fi scan list.

```
app/src/main/java/com/mb/tvlauncher/
  MainActivity.kt       home screen, top bar, dock, grid, drag and drop
  SettingsActivity.kt   Settings pages
  WallpaperView.kt      wallpapers (SceneView.kt is the drawn fallback)
  InputActivity.kt      TV input fallback player
  Tick.kt               tick sound
VolumeBar/              notification volume app
tools/Setup-New-Box.ps1 one-command box setup
```

</details>

---

<a name="guides"></a>

## 📚 More guides

| | Guide | For |
|:-:|---|---|
| ⚡ | [SETUP-NEW-BOX.md](SETUP-NEW-BOX.md) | Setting up a new box, debloat list, undo |
| 💡 | [PURPOSE.md](PURPOSE.md) | Why this launcher exists |
| 🤖 | [CLAUDE.md](CLAUDE.md) | Notes for Claude Code and contributors |
| 🔐 | [SECURITY.md](SECURITY.md) | Security policy |
| 🤝 | [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Code of conduct |

---

<div align="center">

🖼️ The sample wallpapers belong to their photographers and artists. Replace them with your own (`app/src/main/assets/wallpapers`, about 1920x1080).

Released under the [MIT License](LICENSE).

</div>
