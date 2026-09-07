# Project Status — read this first, in any new session

This file exists because this repo has been worked on across multiple AI sessions/tools, and at
least once that caused a real conflict (one session built `WearDataLayerBluetoothProvider` in
package `com.dashboard.wearos.transport` with a plain constructor; another had already wired
`MainActivity` to expect it in package `com.dashboard.wearos.hardware` with a singleton
`getInstance()`). **Before making changes, verify the actual current file state with the commands
in "How to verify before changing anything" below — don't trust any prior summary, including this
one, to be 100% current.**

## Architecture (stable, unlikely to change)

Three Gradle modules:
- **`core/`** — pure Kotlin, zero Android dependency. Domain models, hardware interfaces
  (`BluetoothProvider`, `VehicleDataProvider`, `PhoneCommunication`, etc.), managers
  (`ConnectionManager`, `NavigationManager`, `MediaManager`, `BlizzerManager`, `PowerManager`,
  `SettingsManager`), the wire protocol (`ProtocolMessage`/`MessageCodec`), and
  `NavigationAnnouncementParser`, `ObdPidParser`, `MediaSessionSelection`, `CameraProximity`,
  `SettingsCodec`. **22 test suites / 131 assertions (verified 2026-09-07, all passing), run
  via `./tools/run_tests.sh` or `./gradlew :core:runCoreTests`.**
- **`wearos-app/`** — the real Wear OS Compose app (watch side). Real providers wired:
  `WearDataLayerBluetoothProvider` (nav + media + blizzer transport, link state),
  `BleObdVehicleDataProvider` (Car, non-debug), `DataStoreSettingsStore`. NFC is still a mock.
- **`phone-app/`** — the Android companion. `NavigationAccessibilityService` (Maps/Waze turn
  text), `MediaNotificationListenerService` + `WearInboundListenerService` (media),
  `CameraProximityService` + `SpeedCameraRepository` (speed-camera GPS feed), `WearMessageSender`.
  All send/receive over the Wear Data Layer on `/automotive-dashboard`.

**Build (this machine):** `./gradlew clean :core:runCoreTests :wearos-app:assembleDebug
:wearos-app:assembleRelease :phone-app:assembleDebug :phone-app:assembleRelease` → BUILD
SUCCESSFUL (Gradle 9.3.0, JDK 25, AGP 8.13.2, Kotlin 2.1.10, Compose BOM 2025.10.01,
compileSdk 36). Every phase below is code-complete + build-verified; the remaining work is
**on-device verification** (0d + the per-phase device spikes).

The core design principle: `core`'s interfaces are hardware-agnostic. Real implementations
(`WearDataLayerBluetoothProvider`, a future `BleVehicleDataProvider`, etc.) live in the Android
modules and plug into `core`'s existing managers with zero changes to `core` itself. This has
held up in practice — `BluetoothPhoneCommunication` needed no changes to work over the real Data
Layer transport.

## Per-panel status (reconciled, verified against actual files)

| Panel | Spec | Status |
|---|---|---|
| **Car** | OBD-II/CAN data from a Bluetooth device wired to the vehicle console | 🟡 Pure `ObdPidParser` done + tested (8 tests). `BleObdVehicleDataProvider` + `ObdGattProfile` + `VehicleProviderFactory` written and **build-verified** (mock in debug, BLE in release). Watch connects *directly* to the vehicle's BLE ELM327 adapter. **Needs a real dongle** to verify scan/GATT/AT-init. |
| **Maps** | Turn info from phone, transported to watch | 🟡 Parser hardened 2026-09-07 (U-turn/merge/ramp/slight/sharp, imperial + `1,5` decimals, more Croatian — 20 tests). Transport **build-verified**; Data Layer bugs fixed (Phase B). `Direction` enum gained `U_TURN/MERGE/EXIT_LEFT/EXIT_RIGHT`. Decision: AccessibilityService kept. Real-device announcement capture still unconfirmed (the one open spike). |
| **Music** | Song info + visual audio representation, playback controls | 🟡 Real pipe **built + build-verified 2026-09-07**: phone `MediaNotificationListenerService` → `MediaState` over Data Layer → `MediaManager` (real, not mock); watch `⏮⏯⏭` → `MediaCommandMessage` → phone `WearInboundListenerService` → `transportControls`. Decorative waveform + progress bar on `MusicScreen`. Session selection is pure/tested (`MediaSessionSelection`). Needs a device + notification-access grant to confirm. |
| **Blizzer** | Camera/hazard alerts, blinking overlay over all panels, color-coded by distance | 🟡 Overlay + auto-dismiss + **5-tier** colours done. Real feed **built + build-verified 2026-09-07**: pure `CameraProximity` (haversine, nearest, threshold+hysteresis) + phone `CameraProximityService` (foreground GPS → `BlizzerTrigger` over Data Layer) + `SpeedCameraRepository` (OSM GeoJSON asset). No sound. Needs a device + a real ODbL camera dataset (bundled sample is 6 points). |

## Phase tracker

### Phase A — Blizzer color + auto-dismiss + thresholds — ✅ DONE (5-tier as of 2026-09-07)
- `core`: `BlizzerProximity` (pure, tested) is the **single** colour/blink source —
  5 tiers, thresholds 2000/1000/500/200/100 m, blue→green→amber→red. The old 3-tier
  `BlizzerSeverity`/`BlizzerSeverityMapper` was **deleted** (decision #9 in `PLAN.md`).
  `BlizzerManager` has `autoDismissMillis` (default 5000) + `dismissCurrentEvent()`.
- `wearos-app`: `BlizzerOverlay.kt` turns `BlizzerProximity.colorArgbFor()` into a Compose
  `Color`; `DevControlsScreen.kt` has all 5 threshold buttons.
- Sound: `BlizzerAudioManager` exists and is tested but is **not started** in `MainActivity`
  ("no sound" by omission — a deliberate decision; see `PLAN.md` decision #8).
- Tests: `BlizzerProximityTests.kt`, `BlizzerAutoDismissTests.kt`, passing.

### Phase B — Maps transport (Wear Data Layer, phone → watch) — 🟡 BUILD-VERIFIED, device pending
- **All three modules compile and assemble** (`./gradlew :core:runCoreTests
  :wearos-app:assembleDebug :phone-app:assembleDebug` → BUILD SUCCESSFUL, 2026-09-07, on
  Gradle 9.3 / JDK 25 / AGP 8.13.2). See `PLAN.md` Phase 0c for the version stack.
- `wearos-app/.../hardware/WearDataLayerBluetoothProvider.kt` — package
  `com.dashboard.wearos.hardware`, singleton `getInstance(context)`, implements
  `core`'s `BluetoothProvider`. **Rewritten 2026-09-07**: fully async (no main-thread
  `Tasks.await`), live `CapabilityClient` listener for link state, 8-frame replay buffer,
  capability-scoped `send()`. Path prefix `/automotive-dashboard` (any sub-path).
- `wearos-app/.../hardware/NavDataListenerService.kt` — same package, `WearableListenerService`
  registered for `MESSAGE_RECEIVED` on `pathPrefix="/automotive-dashboard"`. Creates the
  singleton via `applicationContext` on cold start.
- `wearos-app/src/main/res/values/wear.xml` + `phone-app/.../res/values/wear.xml` — Data Layer
  capability declarations (`automotive_dashboard_watch` / `_phone`).
- `wearos-app/MainActivity.kt` — `WearDataLayerBluetoothProvider.getInstance(this)` is **both**
  `ConnectionManager`'s transport and `BluetoothPhoneCommunication`'s transport for
  `NavigationManager`. Music/Blizzer still on `MockPhoneCommunication` (Phases 1 & 4).
- `phone-app/.../WearMessageSender.kt` — `sendNavUpdate` **kept** (past conflict point) as an
  alias of the new generic `send()`; resolves the watch node by capability.
- Dependency: `com.google.android.gms:play-services-wearable:19.0.0` in both modules.
- **Still needs a real paired device** to confirm cross-package `MessageClient` delivery and
  live link-state transitions.

### Phase C — Car panel (watch-side BLE, direct to vehicle OBD-II/CAN adapter) — 🟡 BUILD-VERIFIED, dongle pending
- `core/vehicle/ObdPidParser` — pure ELM327-response parsing + Mode-01 PID decoders +
  `toVehicleData` (absent PIDs → `Signal.Unavailable`; gear + oil pressure never come from
  OBD-II). 8 tests, no hardware needed.
- `wearos-app/hardware/vehicle/`:
  - `ObdGattProfile` — 3 candidate GATT layouts (FFF0/FFF1/FFF2, FFE0/FFE1 combined, 18F0),
    AT init sequence, device-name hints. One-file change to support a different dongle.
  - `BleObdVehicleDataProvider` — implements `VehicleDataProvider`; scan → connect → discover
    → enable notify → AT init → round-robin `01XX` poll → emit `VehicleData`, with backoff
    reconnect. `@SuppressLint("MissingPermission")` (perms requested in `MainActivity`).
  - `VehicleProviderFactory` — `MockVehicleDataProvider` in `BuildConfig.DEBUG` (keeps the dev
    "drive" control + `TESTING.md` working), `BleObdVehicleDataProvider` otherwise.
- `MainActivity` — `VehicleProviderFactory.create(this)`; runtime `BLUETOOTH_SCAN`/`CONNECT`
  request (non-debug only). `PowerManager.onActive/onSleep` already call `start()/stop()`.
- `DevControlPanel.vehicleProvider` is now `MockVehicleDataProvider?` — `setTargetSpeedKmh` is
  a no-op on a real link.
- Manifest: `bluetooth_le` feature, `BLUETOOTH_SCAN` (`neverForLocation`) + `BLUETOOTH_CONNECT`
  + the `maxSdkVersion=30` legacy trio.
- **Needs a real ELM327 BLE dongle** (or a bench ECU simulator) to verify scan/GATT/init.
  Radio-concurrency question (watch↔dongle **and** watch↔phone at once) is still open — if it
  fails, the fallback is phone-relayed OBD (a bigger change).

### Phase D — Music (visual audio representation + real session) — 🟡 BUILD-VERIFIED, device pending
- `core`: `MediaSessionSelection` / `SessionSnapshot` — pure "which session do we mirror" rule
  (PLAYING > PAUSED > STOPPED/UNKNOWN, then most-recently-active), `MediaSessionSelectionTests`.
- `phone-app/media/MediaNotificationListenerService` — `NotificationListenerService` purely as
  the vehicle for the "Notification access" grant; reads every app's session via
  `MediaSessionManager.getActiveSessions`, maps the chosen one to `MediaState`, sends over the
  Data Layer. `MediaSessionHub` holds the chosen `MediaController` for the inbound path.
- `phone-app/WearInboundListenerService` — `WearableListenerService`; a `MediaCommandMessage`
  from the watch → `MediaSessionHub.dispatch` → `transportControls.play()/pause()/skip*`.
- `phone-app/MainActivity` — accessibility **and** notification-access grant buttons + status.
- `wearos-app`: `MediaManager` now on `navPhoneCommunication` (real), not the mock;
  `MusicScreen` has a decorative frame-timed bar waveform (advances only while PLAYING) +
  a progress bar.
- **Decorative only** — no real audio/FFT (audio never plays on the watch). Voice cues stay
  off (`PLAN.md` decision).
- **Needs a device**: notification-access grant + a real Spotify/YT-Music session, and the
  watch→phone command round trip.

### Phase E — Blizzer camera-proximity feed — 🟡 BUILD-VERIFIED, device + dataset pending
- `core/blizzer/CameraProximity` — pure haversine, `nearest` (bounding-box prefilter),
  `crossedThreshold` (tightest newly-entered band; 15 m hysteresis), `toBlizzerEvent` /
  `clearedEvent`. 7 tests, no hardware.
- `phone-app/blizzer/SpeedCameraRepository` — loads `assets/speed_cameras.geojson` (OSM
  `FeatureCollection` shape; a 6-point Zagreb **sample** is bundled — replace with a real
  ODbL extract, keep attribution).
- `phone-app/blizzer/CameraProximityService` — foreground `location` service,
  FusedLocation @1 Hz → `nearest` → `crossedThreshold` → `BlizzerTrigger` over the Data
  Layer; sends a cleared event on leaving range or switching to a nearer camera.
- `phone-app/MainActivity` — start/stop buttons + fine + background-location grant flow.
- Manifest: `ACCESS_FINE/COARSE/BACKGROUND_LOCATION`, `FOREGROUND_SERVICE(_LOCATION)`,
  `POST_NOTIFICATIONS`; the service with `foregroundServiceType="location"`.
- `wearos-app`: `BlizzerManager` now on `navPhoneCommunication` (real). `MockPhoneCommunication`
  on the watch is now used **only** by `DevControlPanel`.
- No sound (decision #8). **Needs a device + a real camera dataset** to verify on the road.

### Phase F — persistence + polish — 🟡 BUILD-VERIFIED
- `core/domain/SettingsCodec` — pure `DashboardSettings` ↔ line format, forward-compatible
  (unknown keys ignored, missing keys → defaults). 5 tests.
- `wearos-app/hardware/DataStoreSettingsStore` — Jetpack DataStore behind `core`'s
  `SettingsStore`: one `runBlocking` startup read + in-memory mirror + fire-and-forget
  writes. `SettingsManager` unchanged. Wired in `MainActivity` (was `InMemorySettingsStore`).
- `DevControlsScreen` uses Wear `ScalingLazyColumn`.
- Adaptive launcher icons (vector "gauge", `mipmap-anydpi-v26`) on both apps.
- `release` buildType on both modules (minify **off** for now — R8 is a low-risk
  follow-up; the ⚙ dev entry already hides itself via `BuildConfig.DEBUG`).
- **Deferred polish** (not blocking a functional product): a real settings *screen*;
  moving dev-only code to `src/debug`; enabling R8; rotary input; Wear-native pager +
  `HorizontalPageIndicator`.

## How to verify before changing anything

Run these and read the actual output — don't assume from this doc or any chat summary:

```bash
# Full build gate (JAVA_HOME must be a JDK 21–25; this repo builds on 25 + Gradle 9.3 + AGP 8.13.2)
./gradlew :core:runCoreTests :wearos-app:assembleDebug :phone-app:assembleDebug

./tools/run_tests.sh                                    # core only, via standalone kotlinc — note the count
find wearos-app/src/main/kotlin/com/dashboard/wearos/hardware -type f
grep -n "class BlizzerManager" -A3 core/src/main/kotlin/com/dashboard/core/service/BlizzerManager.kt
grep -n "WearDataLayerBluetoothProvider\|getInstance" wearos-app/src/main/kotlin/com/dashboard/wearos/MainActivity.kt
grep -n "fun send" phone-app/src/main/kotlin/com/dashboard/phoneapp/WearMessageSender.kt
grep -n "NavDataListenerService" wearos-app/src/main/AndroidManifest.xml
```

Full plan, phase detail, risk register, and open decisions live in **`PLAN.md`** — keep the
two files consistent.

## Update this file

Whenever a phase moves from 🔴/🟡 to ✅, or a real API shape changes (a method renamed, a package
moved), update the relevant section above **before** ending the session — that's what this file
is for.
