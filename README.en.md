# Toolbox

[![platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android)](https://developer.android.com/)
[![kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

[中文](README.md)

A fully on-device, ad-free Android utility app with **no network permission**, built with Kotlin and Jetpack Compose Material3 as a single-Activity app. It does what the phone's sensors make possible — magnetic field, sound, vibration, light, location, camera, astronomical almanac math — with every computation performed locally.

## Tools (16)

| Tool | Description | Hardware / permission |
| --- | --- | --- |
| Compass | Magnetic/true north, rotation-vector + magnetometer fusion | Magnetometer, orientation |
| Light meter | Ambient illuminance (lux) live readout, min/avg/max, chart | Light sensor |
| Altimeter | GPS elevation (bundled EGM96 geoid converts WGS84 ellipsoidal height to orthometric height offline); barometric with QNH/known-altitude calibration when a pressure sensor exists | Location |
| Flashlight | Rear/front torch always-on | Camera flash, CAMERA |
| Level | Bubble, pitch/roll, calibratable zero | Gravity/accelerometer |
| Sound meter | Mic RMS → dB with a manual calibration offset for SPL estimation | Microphone, RECORD_AUDIO |
| Speed meter | GPS current/max/average speed and trip distance, km/h ↔ mph | Location |
| Metal detector | Magnetic field µT, strength band, 60 s rolling chart | Magnetometer |
| Pedometer | Steps, distance, calories (estimated step length) | Step sensor, ACTIVITY_RECOGNITION |
| GPS info | Coordinates (decimal/DMS), altitude, accuracy, speed/heading, satellites (used/in view), copy/share/open-in-maps | Location |
| Vibration meter | Linear-acceleration magnitude m/s², waveform, peak | Accelerometer |
| Metronome / tone | BPM beat (time signature, tap tempo) + sine/square/saw tone generator | Speaker |
| Stopwatch / countdown | Centisecond timing with laps; 1–15 min countdown with end tone + vibration | VIBRATE |
| Ruler / protractor / plumb | 30 cm ruler drawn from physical screen DPI; tilt protractor; plumb line | Gravity sensor |
| Magnifier / mirror | CameraX live preview, pinch zoom, torch, freeze frame, lens switch | Camera, CAMERA |
| Sun & Moon | Apparent/mean solar time with equation of time, sunrise/sunset, day length, noon sun altitude, lunar phase and illumination | Location |

Tools whose hardware is missing are hidden from the home grid automatically; permission-gated tools show an inline rationale with a Grant button instead of nagging dialogs.

Every tool can be **pinned as a 1×1 home-screen shortcut widget** (Glance); tapping it deep-links straight into that tool. The home grid supports long-press drag reordering and persists the order.

## Privacy

- The manifest declares **no `INTERNET` permission** — the app technically cannot connect to the network; there is no upload and no third-party tracking SDK.
- Microphone, camera and sensors are sampled only while the owning screen is in the foreground and are released immediately on leave (the mic privacy indicator turns off with the page).
- Settings (units, calibration, grid order, zero angles) live solely in local DataStore.

## Tech stack

- Kotlin 2.3, AGP 9.4, JVM 17; minSdk 26 (Android 8.0), targetSdk 36, compileSdk 37
- Jetpack Compose (BOM 2026.09) Material3, Navigation Compose, single Activity
- Glance 1.2 AppWidget, DataStore Preferences
- CameraX 1.4 (magnifier)
- Sensor framework / LocationManager / AudioTrack / AudioRecord / Vibrator system APIs — no third-party runtime libraries

## Build

Requires JDK 17 and Android SDK (Platform 37):

```bash
./gradlew assembleDebug          # outputs app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug              # lint
./gradlew testDebugUnitTest      # engine-layer JVM unit tests
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

Each tool follows an `Engine` (pure Kotlin math, JVM-testable) → `ViewModel` (StateFlow) → `Screen` (Compose) layering. The home grid, routes, deep links and widgets are all generated from a single enum in `core/Tool.kt`. Adding a tool is checklist work — see [docs/architecture.md](docs/architecture.md).

```text
app/src/main/java/com/phongchen/toolbox/
├── core/            # Tool registry, sensor flows, location, settings, shared UI
├── feature/         # one package per tool: Engine + ViewModel + Screen
├── widget/          # generic Glance 1x1 entry widget + per-tool receivers
└── MainActivity.kt  # single Activity + NavHost + deep links
app/src/test/        # JVM unit tests for the engines
```

## Data sources

- The EGM96 0.5° geoid grid (`app/src/main/assets/egm96_0_5.bin`) used for elevation conversion is an exact node subset of the official NGA 15′ grid `WW15MGH.DAC` (a U.S. Government work, public domain), with bicubic interpolation — typically within 0.3 m of the full-harmonic reference, worst case about 2 m. See the `GeoidModel.kt` header for the build/verification method.

## License

MIT
