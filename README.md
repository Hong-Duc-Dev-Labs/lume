<div align="center">

<img src="docs/logo.png" alt="Lume logo" width="128" height="128">

# Lume

**A Material 3 toolkit for custom ROMs: Vietnamese lunar calendar on the lockscreen, DIY automation, QR scanner and car dashboard.**

**English** · [Tiếng Việt](README.vi.md)

![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-6750A4?logo=materialdesign&logoColor=white)
![Xposed](https://img.shields.io/badge/LSPosed-module-4F378B)
![License: MIT](https://img.shields.io/badge/License-MIT-FFC857)

</div>

---

Lume is an Android toolkit for custom ROMs. It combines a Material 3 app with an
Xposed/LSPosed module that adds a Vietnamese lunar calendar to the lockscreen, plus
automation rules, a QR scanner, a car dashboard and a few system tweaks.

The app UI is in Vietnamese.

## ✨ Features

- **Vietnamese lunar calendar**: month view, auspicious-day lookup, and the lunar date shown
  next to the date line on the lockscreen (needs the Xposed module).
- **DIY automation**: build rules from triggers and ordered action sequences (charging, battery,
  connectivity, apps, time).
- **Car dashboard**: MapLibre map, GPS speed HUD, speed-camera alerts and routing, also exposed to
  Android Auto through the Car App Library.
- **QR and barcode scanner**: live camera or a picture from the gallery (URL, Wi-Fi, text), with
  history.
- **System UI font**: apply your own `.ttf` / `.otf` font to System UI (needs the Xposed module).
- **Screen timeout**: separate presets for the unlocked screen and the lockscreen (needs the Xposed
  module).
- **Live wallpaper**: a lunar-calendar lockscreen wallpaper.

## 📋 Requirements

- Android 10 or newer (`minSdk` 29).
- For the lockscreen lunar date, system font and screen timeout: an Xposed-compatible framework
  such as [LSPosed](https://github.com/LSPosed/LSPosed).
- For the car dashboard: location permission; Android Auto for the in-car screen.
- Some automation actions run shell commands through `su`, so they need root.

## 📲 Install

1. Download the APK from the repository's Releases page, or [build it yourself](#-build).
2. Install it on the device.
3. To use the module features, open LSPosed, enable **Lume**, and select the scopes **System UI**
   and **System Framework**. Reboot, or restart System UI.
4. Open Lume and grant the permissions each feature asks for.

If you are updating from an earlier build named *Toolkit*, uninstall it first. The package name
changed to `labs.hongducdev.lume`, so Android treats Lume as a separate app. Settings are not
migrated, and the module must be enabled again in LSPosed.

## 🔨 Build

Requires JDK 17 and the Android SDK (compile SDK 36).

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # APK in app/build/outputs/apk/debug/
```

The Xposed API stub is vendored at `app/libs/api-82.jar` and is compile-only.

## 🗂️ Project layout

- `app/src/main/java/labs/hongducdev/lume/xposed/`: Xposed hooks and the settings provider the
  hooks read from. Hooks swallow their own errors so System UI is never crashed by the module.
- `app/src/main/java/labs/hongducdev/lume/domain/diy/` and `services/automation/`: automation
  model and runtime.
- `app/src/main/java/labs/hongducdev/lume/car/`: car dashboard and Android Auto screens.
- `app/src/main/java/labs/hongducdev/lume/lunar/`: lunar calendar logic.
- `app/src/test/`: unit tests.

## ⚠️ Notes and limits

- Hooks target specific System UI classes, so they can stop working on a ROM whose lockscreen
  differs from the one they were written against. A failed hook is logged and skipped.
- Lume is early software (version 0.1.0). Expect rough edges.

## 📄 License

[MIT](LICENSE)
