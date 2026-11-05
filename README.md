# Apple TV Home

An Apple TV–style home screen for Android TV boxes, written in Kotlin with no extra libraries.
Made for old, low-memory boxes (tested on Android 5.1.1, 8.0 and 9 with 1.5 to 1.9 GB RAM), and works fully with a USB mouse, no remote needed.

**[⬇ Download the latest APK](https://github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk)** · Android 5.0 and newer · MIT licensed · no internet permission, no ads, no accounts

---

## Contents

1. [Features](#features)
2. [Download](#download)
3. [Install](#install)
4. [Make it the default Home app](#make-it-the-default-home-app)
5. [Set up a new box with one command](#set-up-a-new-box-with-one-command)
6. [Go back to the stock launcher](#go-back-to-the-stock-launcher)
7. [Build from source](#build-from-source)
8. [Project layout](#project-layout)
9. [Troubleshooting](#troubleshooting)
10. [Documentation](#documentation)
11. [Wallpaper credits and license](#wallpaper-credits-and-license)

---

## Features

### Home screen
- Frosted dock with a row of rounded app tiles. The selected tile pops up with a small bounce and a white outline.
- Press **Down** for the "All apps" view: the first row rises, the other rows come up, the background dims and the app name appears under the selected tile.
- Large clock and date on the left (12 or 24 hour). The clock fades out in "All apps" so it never covers tiles.
- Pre-installed system apps that have a screen are shown too; hide any you don't want.

### Top bar
| Item | What it does |
|---|---|
| Input | Opens the TV's own input menu (HDMI, AV, VGA, antenna). Shown only on TVs that have inputs. The remote's Source key does the same. |
| Volume | Opens a volume bar: click or drag it, use the mouse wheel or Left/Right, OK closes it. |
| Bluetooth | Shown when Bluetooth is on; opens the Bluetooth page. |
| LAN / Wi-Fi name | Opens the Network page. |
| Clock | Opens the system Date and time settings. |
| Gear | Opens Settings. |

### Moving and hiding apps
- **Mouse:** press and hold an app, drag it, release on another tile. Release without moving to get **Move / Hide / Uninstall** (or App info for built-in apps).
- **Remote:** long-press OK to start an arrow-key move, OK to place it; the Menu key opens the menu.
- A **Hidden apps** tile brings hidden apps back.

### Settings (Apple TV style)
| Page | Contents |
|---|---|
| Wi-Fi | Scan and connect with a password |
| Bluetooth | On/off, scan, pair, forget |
| Network | LAN or Wi-Fi, IPv4 address, gateway, subnet mask, DNS, updated live |
| Storage | Free space, room for apps, your files, apps and data, system, whole chip; refreshes by itself |
| Speed boost | Free memory, animation speed |
| Wallpaper | Off (lightest on slow boxes), one picture or rotate; interval 15 s to 15 min; slow zoom on/off |
| Clock | 12/24 hour, Clock time zone (display only, for boxes whose own time zone is unset) |
| Look and sound | Icon size, Favorite apps, soft tick sound on arrows and OK |
| Start-up input | Opens one TV input automatically when the TV starts |
| Shortcuts | System Sound, Display, Apps, Date, Language, Accessibility and About screens |

### Volume Bar (separate app)
[`VolumeBar/`](VolumeBar) puts a volume control (level bar, minus and plus) in the notification area. Nothing is drawn over the screen, and it starts at boot.

---

## Download

Latest release, always the newest: **[AppleTVHome.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk)**

On the TV, in the **Downloader** app, type:

```
github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk
```

All releases with notes are on the **[Releases page](https://github.com/mbnetworku/Apple-TV-launcher/releases)**.

<details>
<summary><b>Older versions</b></summary>

| Version | Date | What's new | Download |
|---|---|---|---|
| **1.2.5** (latest) | 2026-11-05 | Wallpaper settings (off, one picture or rotate, interval, motion) | [AppleTVLauncher-1.2.5.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.2.5/AppleTVLauncher-1.2.5.apk) |
| 1.2.4 | 2026-11-05 | Clock time zone option, cleaner app grid | [AppleTVLauncher-1.2.4.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.2.4/AppleTVLauncher-1.2.4.apk) |
| 1.2.2 | 2026-11-05 | Live Storage page, Network page, tick sound, bug fixes | [AppleTVLauncher-1.2.2.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.2.2/AppleTVLauncher-1.2.2.apk) |
| 1.1.9 | 2026-11-05 | Input button only on TVs with inputs, smaller Settings rows | [AppleTVLauncher-1.1.9.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.1.9/AppleTVLauncher-1.1.9.apk) |
| 1.1.8 | 2026-11-04 | Input button and Source key, Storage page, icon size, favorite apps, start-up input | [AppleTVLauncher-1.1.8.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.1.8/AppleTVLauncher-1.1.8.apk) |
| 1.1 | 2026-10-24 | Mouse support, status bar toggle, Android 5.1 support | [AppleTVLauncher-1.1.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.1/AppleTVLauncher-1.1.apk) |
| 1.0.1 | 2026-10-24 | Version fix | [AppleTVLauncher-1.0.1.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.0.1/AppleTVLauncher-1.0.1.apk) |
| 1.0 | 2026-10-24 | First release | [AppleTVHome.apk](https://github.com/mbnetworku/Apple-TV-launcher/releases/download/v1.0/AppleTVHome.apk) |

</details>

> **Signing:** the APKs are signed with the project's test key, not a Play Store key. Android asks you once to allow "unknown sources". An app installed with one key can't be updated by an APK signed with another, so uninstall first if the TV refuses an update.

---

## Install

Pick one method.

| Method | Needs | Best for |
|---|---|---|
| [A. Downloader app](#a-downloader-app) | Nothing but the TV | Quickest |
| [B. USB stick](#b-usb-stick-and-a-file-manager) | A USB stick and a file manager | No internet on the TV |
| [C. From your phone](#c-from-your-phone) | A "send files to TV" app | Phone users |
| [D. adb](#d-adb-usb-or-network-debugging) | A PC with platform-tools | Full control, setting Home on stubborn TVs |

### A. Downloader app
1. Install **Downloader** (by AFTVnews) from the Play Store.
2. Enter `https://github.com/mbnetworku/Apple-TV-launcher/releases/latest/download/AppleTVHome.apk`.
3. Press Install, and allow unknown apps for Downloader if asked.

### B. USB stick and a file manager
1. Copy `AppleTVHome.apk` to a USB stick and plug it into the TV.
2. Install a file manager from the Play Store (for example X-plore or File Commander).
3. Open the stick, select the APK, allow **Install unknown apps** if asked (Settings → Security & restrictions → Unknown sources), then Install.

### C. From your phone
Send the APK to the TV with an app such as "Send files to TV", then open it on the TV and install it.

### D. adb (USB or network debugging)
1. On the TV: Settings → Device Preferences → About, click **Build** 7 times to unlock Developer options.
2. In Developer options turn on **USB debugging** (also called ADB or Network debugging). Reboot once if there is no network option.
3. On the PC install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools).
4. Find the TV's IP (Settings → Network), connect, and accept the prompt on the TV (tick **Always allow**):

   ```bash
   adb connect <TV_IP>:5555
   ```

5. Install:

   ```bash
   adb install -r AppleTVHome.apk
   ```

6. Optional, lets the Speed boost page change animation speed:

   ```bash
   adb shell pm grant com.mb.appletvlauncher android.permission.WRITE_SECURE_SETTINGS
   ```

---

## Make it the default Home app

**Without a PC:** open "Apple TV Home" once from the apps list, then press Home. If the TV asks which Home app to use, choose **Apple TV Home** and **Always**.

**Android 7 and newer, with adb:**

```bash
adb shell cmd package set-home-activity com.mb.appletvlauncher/com.mb.tvlauncher.MainActivity
```

**Android 5 and 6, or a TV that keeps opening its own launcher:** Android 5 has no `cmd`. Disable the stock launcher instead (root needed), one package at a time, and press Home after each to check:

```bash
adb shell "su -c 'pm disable <stock.launcher.package>'"
```

> Never disable the stock launcher before Apple TV Home is installed and opens correctly. Without root, try disabling it from the TV's Apps settings.

---

## Set up a new box with one command

For a fresh box, a PowerShell script does everything: connects, backs up the package lists, builds and installs the launcher and Volume Bar, makes the launcher the only Home app (root needed), and writes an undo script.

Needs on the PC: adb, JDK 17, Android SDK (platform 34, build-tools 34.0.0), Gradle 8.7, and network ADB turned on in the box's Developer options.

```bash
git clone https://github.com/mbnetworku/Apple-TV-launcher.git
```

```bash
cd Apple-TV-launcher
```

```powershell
powershell -ExecutionPolicy Bypass -File tools\Setup-New-Box.ps1 -TvIp <TV_IP> -Debloat -HideStatusBar
```

| Option | Effect |
|---|---|
| `-TvIp <TV_IP>` | The box's IP address |
| `-Debloat` | Disables the demo and factory apps listed in the setup guide (reversible) |
| `-HideStatusBar` | Hides the system status bar everywhere |
| `-SkipBuild` | Uses prebuilt APKs from `dist\` instead of building |

With Claude Code, just say: *"Set up my new TV box at <TV_IP> using SETUP-NEW-BOX.md"*.
Step-by-step commands, the debloat list, undo and troubleshooting: **[SETUP-NEW-BOX.md](SETUP-NEW-BOX.md)**.

---

## Go back to the stock launcher

- Not rooted / nothing disabled: simply uninstall Apple TV Home.
- Stock launcher disabled: re-enable it first, then disable or uninstall this one:

  ```bash
  adb shell "su -c 'pm enable <stock.launcher.package>'"
  ```

  ```bash
  adb shell "su -c 'pm disable com.mb.appletvlauncher'"
  ```

The one-command setup also writes an undo script that does this for you.

---

## Build from source

Needs JDK 17, Gradle 8.7, Android SDK platform 34 and build-tools 34.0.0.

1. Create `local.properties` with your `sdk.dir`. On Windows escape the backslashes (`sdk.dir=C\:\\Users\\...`), or Gradle fails with "filename ... syntax is incorrect".
2. Build the launcher, then the Volume Bar:

   ```bash
   gradle assembleDebug
   ```

   ```bash
   cd VolumeBar && gradle assembleDebug
   ```

3. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Before every release raise `versionCode` and `versionName` in `app/build.gradle.kts`; a lower version fails to install with `INSTALL_FAILED_VERSION_DOWNGRADE`.

> **Why targetSdk 22?** On the Android 8.0 TV it was built for, apps targeting 23 or newer got an empty Wi-Fi scan list. Changing the target requires uninstalling first, which clears the default Home setting.

---

## Project layout

```
app/src/main/java/com/mb/tvlauncher/
  MainActivity.kt       home screen: clock, top bar, dock, app grid, drag and drop
  SettingsActivity.kt   Apple TV-style Settings (open a page with extra page=wifi|bt)
  WallpaperView.kt      wallpaper cross-fade; falls back to SceneView.kt
  RoundFrame.kt         rounded tile corners
  StatusIcon.kt         drawn Wi-Fi, LAN, Bluetooth, gear and volume icons
  Tick.kt               tick sound generated in code
app/src/main/assets/wallpapers/   sample wallpapers (1920x1080)
VolumeBar/                        separate notification volume app (com.mb.volumebar)
tools/Setup-New-Box.ps1           one-command box setup
releases/                         published APKs
```

Package name `com.mb.appletvlauncher`, code in `com.mb.tvlauncher`, minSdk 21, no AndroidX.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `INSTALL_FAILED_VERSION_DOWNGRADE` | The installed version is newer; install a newer APK or uninstall first. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` / update refused | Different signing key; uninstall the old app first. |
| `INSTALL_FAILED_OLDER_SDK` | The APK was built with a newer minSdk; build from source for old boxes. |
| TV keeps opening its own launcher | See [Make it the default Home app](#make-it-the-default-home-app). |
| `pm disable-user` gives SecurityException | Use `su -c 'pm disable ...'` on rooted boxes. |
| Top bar missing | The `hide_bar` setting is on; turn the status bar back on in Settings. |
| Speed boost can't change animations | Grant `WRITE_SECURE_SETTINGS` (see [Install D](#d-adb-usb-or-network-debugging)). |
| Volume keys over adb do nothing | Expected on some boxes; use the launcher's volume button or Volume Bar. |
| Box feels slow | Settings → Wallpaper → Off, and Settings → Speed boost. |

More in [SETUP-NEW-BOX.md](SETUP-NEW-BOX.md).

---

## Documentation

| File | For |
|---|---|
| [SETUP-NEW-BOX.md](SETUP-NEW-BOX.md) | Setting up a new box, debloat list, undo |
| [PURPOSE.md](PURPOSE.md) | Why this launcher exists and its benefits |
| [CLAUDE.md](CLAUDE.md) | Project notes for Claude Code and contributors, version history |
| [VolumeBar/](VolumeBar) | The notification volume app |
| [SECURITY.md](SECURITY.md) | Security policy |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Code of conduct |

---

## Wallpaper credits and license

The pictures in `app/src/main/assets/wallpapers` are not mine. They belong to their respective photographers and artists and are included only as samples. Replace them with your own images (`.jpg`, `.png` or `.webp`, about 1920x1080) or remove any you do not have the right to share. With no images, the launcher draws its own mountain scene.

The code is released under the [MIT License](LICENSE).
