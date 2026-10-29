<#
  One command to set up a new Android TV box with the Apple TV launcher and the Volume Bar app.

  Example:
    powershell -ExecutionPolicy Bypass -File tools\Setup-New-Box.ps1 -TvIp <TV_IP>
    powershell -ExecutionPolicy Bypass -File tools\Setup-New-Box.ps1 -TvIp <TV_IP> -Debloat -HideStatusBar

  What it does, in order:
    1. connects with ADB and checks the box (Android 5.0+ needed)
    2. backs up the package lists and writes restore.ps1 (undo script) next to the backup
    3. builds the launcher and VolumeBar (or uses prebuilt APKs from .\dist with -SkipBuild)
    4. installs both and starts Volume Bar
    5. makes the launcher the only Home app (disables the other launchers with root; checks Home; undoes it if the check fails)
    6. optional: -HideStatusBar hides the system status bar, -Debloat disables a short list of unused system extras

  Needs on this PC: adb, JDK 17, Android SDK (platform 34, build-tools 34.0.0), Gradle 8.7 (or a gradlew in the repo).
  Needs on the box: network ADB on port 5555, and root (su) to disable other launchers. No root = steps 5 and 6 are skipped.
#>
param(
    [Parameter(Mandatory)][string]$TvIp,
    [int]$Port = 5555,
    [switch]$SkipBuild,
    [switch]$HideStatusBar,
    [switch]$Debloat,
    [string]$Gradle
)

$ErrorActionPreference = 'Continue'   # adb prints progress on stderr; real failures are checked and thrown below
$root = Split-Path $PSScriptRoot -Parent
$tv = "${TvIp}:$Port"
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup = Join-Path $root "box-backup-$stamp"

function Find-Adb {
    $c = Get-Command adb -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    $g = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter adb.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($g) { return $g.FullName }
    throw 'adb not found. Install Android platform-tools and put adb on PATH.'
}
$adb = Find-Adb

function Adb { & $adb -s $tv @args }
function Su([string]$cmd) { Adb shell "su -c '$cmd'" }
function Step([string]$t) { Write-Host "`n== $t" -ForegroundColor Cyan }

Step "Connect to $tv"
& $adb connect $tv | Out-Host
function Prop([string]$name) { ((Adb shell getprop $name) -join '').Trim() }
$sdk = [int](Prop ro.build.version.sdk)
$rel = Prop ro.build.version.release
$abi = Prop ro.product.cpu.abilist
Write-Host "Android $rel (API $sdk), CPU: $abi"
if ($sdk -lt 21) { throw 'This launcher needs Android 5.0 (API 21) or newer.' }
$hasRoot = ((Adb shell "su -c id") -join '') -match 'uid=0'
Write-Host "Root: $hasRoot"

Step "Back up to $backup"
New-Item -ItemType Directory -Force $backup | Out-Null
Adb shell pm list packages -f | Set-Content "$backup\packages.txt"
Adb shell getprop | Set-Content "$backup\getprop.txt"
$restore = @("`$adb = '$adb'")
$restore += "# Undo script: turns back on what Setup-New-Box.ps1 disabled."
if ($hasRoot) { Su 'pm list packages -d' | Set-Content "$backup\disabled-before.txt" }

if (-not $SkipBuild) {
    Step 'Build launcher and Volume Bar'
    $gradle = if ($Gradle) { $Gradle } else { (Get-Command gradle -ErrorAction SilentlyContinue).Source }
    if (-not $gradle -and (Test-Path "$root\gradlew.bat")) { $gradle = "$root\gradlew.bat" }
    if (-not $gradle) { throw 'Gradle not found. Install Gradle 8.7 or add a gradle wrapper.' }
    $sdkDir = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $null }
    foreach ($p in @($root, "$root\VolumeBar")) {
        if (-not (Test-Path "$p\local.properties")) {
            if (-not $sdkDir) { throw 'Set ANDROID_HOME to your Android SDK folder (needs platform 34 and build-tools 34.0.0).' }
            Set-Content "$p\local.properties" ('sdk.dir=' + ($sdkDir -replace '\\', '\\\\' -replace ':', '\:'))
        }
        & $gradle -p $p assembleDebug
        if ($LASTEXITCODE -ne 0) { throw "Build failed in $p" }
    }
    $launcherApk = "$root\app\build\outputs\apk\debug\app-debug.apk"
    $volumeApk = "$root\VolumeBar\app\build\outputs\apk\debug\app-debug.apk"
} else {
    $launcherApk = "$root\dist\AppleTVHome.apk"
    $volumeApk = "$root\dist\VolumeBar.apk"
}

Step 'Install'
foreach ($apk in @($launcherApk, $volumeApk)) {
    if (-not (Test-Path $apk)) { throw "Missing $apk" }
    $r = Adb install -r $apk 2>&1 | Out-String
    Write-Host $r.Trim()
    if ($r -notmatch 'Success') { throw "Install failed for $apk. If it says VERSION_DOWNGRADE raise versionCode; if OLDER_SDK the box is too old." }
}
Adb shell am start -n com.mb.volumebar/.LaunchActivity | Out-Null
$restore += "& `$adb -s $tv shell `"su -c 'pm enable com.mb.appletvlauncher'`""

if ($hasRoot) {
    Step 'Make Apple TV Home the only Home app'
    $cands = (Adb shell pm list packages) -replace 'package:', '' | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match 'launcher|\.home$|\.home\.' -and $_ -notlike 'com.mb.*' }
    $disabled = @()
    foreach ($p in $cands) {
        $dump = (Adb shell pm dump $p | Out-String)
        if ($dump -match 'android.intent.category.HOME') {
            Write-Host "Disabling stock launcher $p"
            Su "pm disable $p" | Out-Host
            $disabled += $p
            $restore += "& `$adb -s $tv shell `"su -c 'pm enable $p'`""
        }
    }
    Adb shell input keyevent KEYCODE_HOME
    Start-Sleep 5
    $focus = (Adb shell "dumpsys window | grep mCurrentFocus") -join ' '
    if ($focus -match 'com.mb.appletvlauncher') {
        Write-Host 'OK: Home opens the Apple TV launcher.' -ForegroundColor Green
    } else {
        Write-Warning "Home did not open the launcher ($focus). Turning the stock launchers back on."
        foreach ($p in $disabled) { Su "pm enable $p" | Out-Null }
    }

    if ($HideStatusBar) {
        Step 'Hide the system status bar'
        Adb shell settings put global policy_control 'immersive.status=*'
        $restore += "& `$adb -s $tv shell settings put global policy_control null"
    }

    if ($Debloat) {
        Step 'Disable unused system extras'
        $extras = 'com.android.phasebeam', 'com.android.wallpaper.holospiral', 'com.android.musicvis',
                  'com.android.dreams.phototable', 'com.android.wallpaper.livepicker',
                  'com.android.soundrecorder', 'com.android.exchange', 'com.google.android.feedback'
        $installed = (Adb shell pm list packages) -replace 'package:', '' | ForEach-Object { $_.Trim() }
        foreach ($p in $extras | Where-Object { $installed -contains $_ }) {
            Su "pm disable $p" | Out-Host
            $restore += "& `$adb -s $tv shell `"su -c 'pm enable $p'`""
        }
        Adb shell sync
    }
} else {
    Write-Warning 'No root: the stock launcher was not disabled. Pick Apple TV Home as the default when Android asks after pressing Home.'
}

Set-Content "$backup\restore.ps1" $restore
Step 'Done'
Write-Host "Backup and undo script: $backup"
Write-Host 'Undo: powershell -ExecutionPolicy Bypass -File <backup folder>\restore.ps1'
Write-Host 'Turn network ADB off on the box when you are finished.'
