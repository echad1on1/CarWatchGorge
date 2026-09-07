# automotive-dashboard

A software-first automotive dashboard for a Wear OS watch/display mounted permanently inside a
car, with an Android phone companion that feeds it live data over the Wear OS Data Layer. The
final physical hardware (casing, mount, wiring) doesn't exist yet — the app is built against
clean hardware interfaces so real adapters plug in without rewriting anything above them.

**Status (2026-09-07): all planned features are implemented and the whole project builds** —
`core` + both Android apps, debug and release. What's left is verification on real
devices/hardware. See [`PLAN.md`](PLAN.md) for the full phase-by-phase history and the
on-device checklist, and [`PROJECT_STATUS.md`](PROJECT_STATUS.md) for the terse per-panel
tracker.

## Architecture

Three Gradle modules:

- **`core/`** — pure Kotlin/JVM, **zero Android dependency**. Domain models, the hardware
  interfaces (`VehicleDataProvider`, `PhoneCommunication`, `BluetoothProvider`, `AudioOutput`,
  `SettingsStore`, …), the managers (`ConnectionManager`, `NavigationManager`, `MediaManager`,
  `BlizzerManager`, `PowerManager`, `SettingsManager`), the wire protocol
  (`ProtocolMessage` / `MessageCodec`), and the pure algorithm code:
  `NavigationAnnouncementParser`, `ObdPidParser`, `MediaSessionSelection`, `CameraProximity`,
  `SettingsCodec`. **22 test suites / 131 assertions**, all passing.
- **`wearos-app/`** — the Wear OS Jetpack Compose UI. Presentation only, wired to `core`'s
  managers, plus the watch-side real hardware implementations
  (`WearDataLayerBluetoothProvider`, `BleObdVehicleDataProvider`, `DataStoreSettingsStore`).
- **`phone-app/`** — the Android companion. Captures navigation, media and location data and
  sends it to the watch over the Data Layer.

The design principle: `core`'s interfaces are hardware-agnostic. Real implementations live in
the Android modules and plug into `core`'s existing managers with **zero changes to `core`**.
This held up across every phase — the only `core` additions were new pure algorithm files
(`ObdPidParser`, `CameraProximity`, …), never interface changes.

## What's implemented

### Watch (`wearos-app`)
- **Four swipeable panels** — Car, Maps, Music (+ a global Blizzer overlay). Maps/Music only
  appear once the phone is connected; disconnecting snaps back to Car.
- **Car** — live vehicle data from a **BLE OBD-II (ELM327) adapter** in release builds
  (`BleObdVehicleDataProvider`: scan → GATT → AT init → round-robin PID poll → `VehicleData`);
  a realistic simulator in debug builds so it runs with no hardware.
- **Maps** — turn-by-turn arrow / distance / road name / ETA, driven by the phone; distance
  counts down smoothly between checkpoints using the car's own speed.
- **Music** — now-playing title/artist, a decorative waveform + progress bar, and working
  ⏮ ⏯ ⏭ transport controls that drive the phone's actual media session.
- **Blizzer** — a full-screen speed-camera proximity overlay, blue → green → amber → red
  across five distance bands (2000 / 1000 / 500 / 200 / 100 m), auto-dismissing after 5 s.
- **Settings** persist on-disk via Jetpack DataStore (`DataStoreSettingsStore`).
- **Connection** state machine (`ConnectionManager`), driven by the real Data Layer link
  (`CapabilityClient` tracks the phone appearing/disappearing).
- Adaptive launcher icon; `release` build type (the ⚙ dev-controls screen is debug-only).

### Phone (`phone-app`)
- **Navigation** — `NavigationAccessibilityService` reads Google Maps / Waze turn text →
  `NavigationAnnouncementParser` (English + Croatian, imperial + metric, ~20 phrasings) →
  encoded checkpoints to the watch.
- **Media** — `MediaNotificationListenerService` reads whatever app is playing via
  `MediaSessionManager`; `WearInboundListenerService` applies the watch's transport commands.
- **Speed cameras** — `CameraProximityService` (foreground GPS) + `SpeedCameraRepository`
  (OpenStreetMap-shaped GeoJSON) → distance thresholds → alerts to the watch.
- One setup screen with the three permission grants (Accessibility, Notification access,
  Location).

### `core` (pure, fully tested)
Everything above the hardware line: the managers, the state machines, the wire protocol
proven over a real loopback transport, and the pure algorithms
(OBD PID decoding, haversine + camera-threshold logic with hysteresis, media-session
selection, nav-text parsing, settings serialization).

## Build & test

Builds headlessly (no Android Studio needed) with **JDK 17–25, Gradle 9.3, AGP 8.13.2**:

```bash
export JAVA_HOME=<a JDK 17-25>
./gradlew :core:runCoreTests \
          :wearos-app:assembleDebug :wearos-app:assembleRelease \
          :phone-app:assembleDebug :phone-app:assembleRelease
```

`core` tests alone, either way:

```bash
tools/run_tests.sh            # needs kotlinc + kotlin on PATH
./gradlew :core:runCoreTests  # no standalone kotlinc needed
```

Text-console walkthrough of the whole app with no Android at all: `tools/run_demo.sh`.
Wear OS emulator walkthrough (real UI, no phone/hardware): [`TESTING.md`](TESTING.md).

## Next steps

All remaining work is **verification on real devices** — no feature code is outstanding.
Full detail + ordering in `PLAN.md` → "On-device verification checklist". In short:

1. **Wear emulator smoke test** — install the debug build, walk `TESTING.md`. Fix any Wear
   Compose API drift the first run flags.
2. **Paired phone** — pair a phone AVD (or a real phone) with the watch, confirm the Data
   Layer link and connection state transitions.
3. **Maps capture (biggest unknown)** — on a real phone, confirm Google Maps turn text is
   actually readable **while Maps is backgrounded / screen off** during a drive. If not, the
   capture strategy needs revisiting.
4. **Music** — grant Notification access, confirm a real Spotify/YouTube-Music session shows
   on the watch and the transport buttons drive it.
5. **Car** — with a real OBD adapter (below), confirm scan/connect/PID polling; **also test
   whether the watch can hold the OBD link *and* the phone link at once**.
6. **Blizzer** — swap in a real regional camera dataset, grant location, drive past a known
   camera.
7. **Signing** — add a signing config so the `release` APKs can actually be installed.

Deferred polish (not blocking a working product): a settings screen, moving dev-only code to
`src/debug`, enabling R8/minify, rotary-crown input.

## Hardware needed

| For | Hardware | Notes |
|---|---|---|
| Running the watch app | A **Wear OS 4/5 watch** (or a Wear OS emulator, API 34/35) | `minSdk 30`. Standalone — no phone needed for the Car panel + dev controls. |
| Maps / Music / Blizzer | An **Android phone** (or phone AVD) running `phone-app`, paired to the watch | `minSdk 26`. Google Maps or Waze installed for navigation. |
| Car panel (real data) | A **Bluetooth LE OBD-II adapter** (ELM327-style — e.g. Veepeak BLE, Vgate iCar Pro BLE) plugged into the car's OBD-II port | Classic-Bluetooth-only ("SPP") adapters won't work — it must be **BLE**. A bench ECU/ELM327 simulator works for testing without a car. |
| Blizzer data | A speed-camera POI dataset for your region | OpenStreetMap `highway=speed_camera` / `enforcement=*` extract (ODbL — keep attribution). The bundled `phone-app/src/main/assets/speed_cameras.geojson` is a 6-point sample only. |
| Audio cues (not built) | Bluetooth A2DP to the car stereo, or an AUX adapter | `AudioOutput` is currently a no-op mock — navigation is visual-only by decision. |
| NFC "tap to connect" (not used) | — | Most Wear OS watches can't act as NFC readers, so connection is automatic via `CapabilityClient` instead; `NfcProvider` stays a mock. |

## Project layout

```
core/          pure Kotlin/JVM — domain, interfaces, managers, protocol, algorithms, tests
wearos-app/    Wear OS Compose UI + watch-side hardware impls
phone-app/     Android companion — navigation / media / location capture
docs/          android-integration-research.md (what Android actually permits, with sources)
tools/         run_tests.sh / run_demo.sh — build & run core without Gradle
PLAN.md        living implementation plan: phase history, risks, on-device checklist
PROJECT_STATUS.md   terse per-panel status tracker
TESTING.md     Wear OS emulator walkthrough
```

## Author

Built and maintained by Eldar Dacic.
