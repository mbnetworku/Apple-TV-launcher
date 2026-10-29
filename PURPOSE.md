# Purpose: why this launcher exists

## The problem
Cheap and old Android TV boxes ship with a slow, cluttered home screen: duplicate apps, ads and tracking services, too little memory, and a launcher that was never made for a mouse. On a box with under 2 GB of RAM this makes everything feel slow.

## What Apple TV Home does
It replaces the stock home screen with a clean, light one in the style of Apple TV:
- a large clock and date, and a top bar with volume, Bluetooth, network name, the time and settings;
- a dock of app tiles that you can drag to reorder with a mouse or the remote, plus an "All apps" view;
- rotating wallpapers, and its own simple Settings for Wi-Fi, Bluetooth, clock format and system shortcuts;
- a separate Volume Bar app that puts a volume control in the notification area.

## The benefits
- **Faster:** no background clutter, no blur, video or network polling; made for about 1.8 GB of RAM.
- **Works without a remote:** everything can be done with a USB mouse, including moving apps and setting the volume.
- **Private:** no internet permission, no accounts, no ads, nothing is sent anywhere.
- **Reversible:** the stock launcher is only disabled, never deleted, so one command brings it back.
- **Repeatable:** `tools/Setup-New-Box.ps1` sets up the next box with one command, and `SETUP-NEW-BOX.md` lists every step.
- **Free and open:** MIT licensed, so you can change it for your own box.

## Who it is for
People who own an old or low-cost Android TV box (tested on Android 5.1.1 and 8.0) and want a faster, calmer home screen that they control.
