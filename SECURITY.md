# Security policy

## What this project does and does not do
- The launcher has no `INTERNET` permission, so it cannot send data off the box.
- It reads the list of installed apps, Wi-Fi and Bluetooth state, and the volume level, only to show them on screen.
- The optional "Speed boost" animation setting needs a permission you grant yourself over ADB (`WRITE_SECURE_SETTINGS`). Do not grant it if you do not want that setting.
- The Volume Bar app only changes the media volume and shows a notification. It has no network access.
- The APKs in `releases/` are debug-signed, not release-signed. Build from source if you want to be sure what runs on your box.

## Risks you should know about
- Setting up a box uses network ADB (port 5555), which has no password. Anyone on the same network can control the box while it is on. Turn it off when you finish (see `SETUP-NEW-BOX.md`, step 8).
- Disabling stock launchers needs root. Always keep the undo script made by `tools/Setup-New-Box.ps1`.
- Old Android versions (for example 5.1.1) no longer get security updates. Do not use such a box for banking or sign-ins you care about.

## Supported versions
Only the latest release on `main` is supported.

## Reporting a vulnerability
Please do not open a public issue for a security problem. Use GitHub's private reporting: open the **Security** tab of this repository and choose **Report a vulnerability**. Include the version, the Android version of the box and the steps to reproduce. You can expect a reply within about a week, and a fix or a clear answer after that.
