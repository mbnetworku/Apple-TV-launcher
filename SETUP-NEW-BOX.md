# Set up a new Android TV box (Apple TV Home + Volume Bar)

Everything that was done on the first box (an Android 5.1.1 Amlogic box, rooted, used with a USB mouse), written so the next box takes one command. Replace `<TV_IP>` with the box's address. Nothing here contains personal data.

## 0. Before you start (on the box)
1. Settings > About > press OK on "Build number" 7 times, then Developer options > turn on USB debugging and Network (ADB) debugging. Accept the "Allow debugging" prompt when it appears.
2. Find the box's IP address (Settings > Network, or the router's device list). The box and this PC must be on the same network.
3. Network ADB (port 5555) has no password. Anyone on the network can get a shell. Switch it off when you are done (step 8).

## 1. The one command
On the PC (needs adb, JDK 17, Android SDK platform 34 + build-tools 34.0.0, Gradle 8.7; set `ANDROID_HOME` to the SDK folder):

```powershell
git clone https://github.com/mbnetworku/Apple-TV-launcher.git
cd Apple-TV-launcher
powershell -ExecutionPolicy Bypass -File tools\Setup-New-Box.ps1 -TvIp <TV_IP> -Debloat -HideStatusBar
```

What it does: connects, checks the Android version, backs up the package lists, builds and installs the launcher and Volume Bar, starts Volume Bar, disables the stock launchers (root needed) and checks that Home opens the new launcher, then writes `box-backup-<time>\restore.ps1`.
Switches: `-SkipBuild` uses prebuilt APKs from `dist\`, `-Debloat` disables a short list of unused system extras, `-HideStatusBar` hides the Android status bar everywhere.

With Claude Code you can just say: "Set up my new TV box at <TV_IP> using SETUP-NEW-BOX.md".

## 2. The same thing by hand (if the script stops)
```powershell
adb connect <TV_IP>:5555
adb shell getprop ro.build.version.release          # needs 5.0 or newer
adb shell getprop ro.product.cpu.abilist
adb shell "su -c id"                                 # uid=0 means root works
adb shell pm list packages -f > packages.txt         # backup
gradle -p . assembleDebug
gradle -p VolumeBar assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r VolumeBar\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.mb.volumebar/.LaunchActivity
```
Make the launcher the only Home (root). Find the stock launchers (apps that declare the HOME category), disable one, press Home, check, repeat:
```powershell
adb shell pm list packages launcher
adb shell "pm dump <package> | grep category.HOME"
adb shell "su -c 'pm disable <stock.launcher.package>'"
adb shell input keyevent KEYCODE_HOME
adb shell "dumpsys window | grep mCurrentFocus"      # must show com.mb.appletvlauncher
```
Plain `pm disable-user` from the shell is refused on old firmware; `su -c 'pm disable ...'` works. Never disable the stock launcher before the new one is installed. On the first box the stock ones were `com.vs.vslauncher` (the real Home) and `com.android.launcher`.

Hide the system status bar everywhere: `adb shell settings put global policy_control immersive.status=*` (undo: `... policy_control null`).

## 3. Debloat (what was done on the first box, all reversible)
- Uninstalled (user's choice): Telegram, ExpressVPN, Speed VPN, VPN Unlimited, Psiphon, Aptoide, Opera, UC Browser, Netflix: `adb shell pm uninstall <package>`.
- Disabled with root: `com.android.phasebeam`, `com.android.wallpaper.holospiral`, `com.android.musicvis`, `com.android.dreams.phototable`, `com.android.wallpaper.livepicker`, `com.android.soundrecorder`, `com.android.exchange`, `com.google.android.feedback`, the factory-test apps, and the stock video player. Re-enable with `adb shell "su -c 'pm enable <package>'"`.
- Never disable: TV and remote services (`com.droidlogic.*` core), Play Store, Play Services, Settings, SystemUI, the keyboard. Never open an app called factory or fac menu.
- Wait 15 seconds and run `adb shell sync` after the last change, then reboot once and check `su -c 'pm list packages -d'`.

## 4. Why a box feels slow, and what helped
- Low RAM (1.8 GB): several browsers, VPNs and app stores running in the background used about 500 MB. Removing the duplicates freed about 100 MB.
- Slow DNS: the router's DNS answered in about 300 ms, Cloudflare (1.1.1.1) or Google (8.8.8.8) in about 30 ms. Fix on the router (best, covers every device) or per PC/box in the network settings. Check with `Resolve-DnsName example.com -Server <dns ip>` and time it.
- Old Android (5.1.1): no security updates since 2015, many apps (Netflix, YouTube) no longer support it. Do not use such a box for banking.

## 5. What the launcher gives you
Large clock and date on the left (12 or 24 hour in Settings), top bar with volume, Bluetooth, LAN or Wi-Fi name, the time (opens Date and time settings) and the gear (opens Settings). Wallpapers rotate every 40 s (`app/src/main/assets/wallpapers`, 1920x1080 JPG). Press and hold an app with the mouse and drag it to a new place; release without moving for Move / Hide / Uninstall. Volume Bar adds a volume control (minus, level bar, plus) in the notification area and starts at boot.

## 6. Install other apps from your own projects
```powershell
adb install -r <path\to\app.apk>        # use the build that matches: armeabi-v7a for 32-bit boxes
adb shell pm list packages -3           # check it is there
```
Example: YouNew TV (`com.younew.tv`) was installed from `YouNew\smarttubetv\build\outputs\apk\stfdroid\release\SmartTube_fdroid_32.68_armeabi-v7a.apk`.

## 7. Undo
- Everything the script disabled: `powershell -ExecutionPolicy Bypass -File box-backup-<time>\restore.ps1`.
- Back to the stock Home by hand: `adb shell "su -c 'pm enable <stock launcher>'"` then `adb shell "su -c 'pm disable com.mb.appletvlauncher'"`.
- Status bar back: `adb shell settings put global policy_control null`.

## 8. When you are finished
Turn Network (ADB) debugging off on the box (and USB debugging if you do not need it), reboot, and check the port is closed: `Test-NetConnection <TV_IP> -Port 5555` should fail.

## 9. Known problems and tips
- `INSTALL_FAILED_OLDER_SDK`: the APK needs a newer Android than the box has (the old `releases/AppleTVHome-1.0.apk` does; build from source). `INSTALL_FAILED_VERSION_DOWNGRADE`: raise `versionCode` and `versionName` in `app/build.gradle.kts`.
- Gradle error "filename, directory name or volume label syntax is incorrect": `local.properties` needs escaped backslashes (`sdk.dir=C\:\\Users\\...`).
- The top bar can look "missing": the launcher's own setting "Status bar: Hidden" hides it (Settings > Status bar).
- `adb shell input tap x y` is exact, but it cannot hold and drag, and the box's own `VOLUME` key events do nothing over ADB.
- If ADB suddenly says "device offline", the box may have gone to sleep or restarted; wake it, then `adb disconnect` and `adb connect <TV_IP>:5555`. Avoid long shell loops over ADB on a weak box.
- Push to GitHub only when the owner asks.
