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
  `NavigationAnnouncementParser`. **19 test suites / 98 assertions (verified 2026-09-07 with
  kotlinc 2.3.10, all passing), run via `./tools/run_tests.sh` or `./gradlew :core:runCoreTests`.**
- **`wearos-app/`** — the real Wear OS Compose app (watch side).
- **`phone-app/`** — the real Android companion app (phone side), currently just
  `NavigationAccessibilityService` + a disclosure `MainActivity`.

The core design principle: `core`'s interfaces are hardware-agnostic. Real implementations
(`WearDataLayerBluetoothProvider`, a future `BleVehicleDataProvider`, etc.) live in the Android
modules and plug into `core`'s existing managers with zero changes to `core` itself. This has
held up in practice — `BluetoothPhoneCommunication` needed no changes to work over the real Data
Layer transport.

## Per-panel status (reconciled, verified against actual files)

| Panel | Spec | Status |
|---|---|---|
| **Car** | OBD-II/CAN data from a Bluetooth device wired to the vehicle console | 🔴 Mock only (`MockVehicleDataProvider`). **Decision locked**: watch connects *directly* to the vehicle's BLE adapter (not via phone relay). Real `BleVehicleDataProvider` not started. |
| **Maps** | Turn info from phone, transported to watch | 🟡 Logic fully proven with real captured Google Maps data (parser handles English + Croatian, correctly ignores trip-total-distance traps). Transport (Wear Data Layer) **in progress** — see Phase B below. |
| **Music** | Song info + visual audio representation, playback controls | 🔴 Mock only (`MockPhoneCommunication`/`MediaState`). Shows title/artist as text. No visualizer, no real `NotificationListenerService` session capture. Not started. |
| **Blizzer** | Camera/hazard alerts, blinking overlay over all panels, color-coded by distance | 🟢 **Done** (Phase A, see below): 5 thresholds (2000/1000/500/200/100m), blue→green→red by distance, 5s auto-dismiss via `BlizzerManager`, no sound. Real camera/GPS data source not started (separate, later decision). |

## Phase tracker

### Phase A — Blizzer color + auto-dismiss + thresholds — ✅ DONE
- `core`: `BlizzerSeverity`/`BlizzerSeverityMapper` (pure, tested), `BlizzerManager` gained
  `autoDismissMillis` (default 5000) + `dismissCurrentEvent()`.
- `wearos-app`: `BlizzerOverlay.kt` maps severity → color (blue/green/red) and blink speed.
  `DevControlsScreen.kt` has all 5 threshold buttons.
- Sound: `BlizzerAudioManager` exists and is tested, but is **not started** in `MainActivity` —
  confirmed via `grep -n "blizzerAudioManager" MainActivity.kt` returning nothing. "No sound" is
  satisfied by omission, not an explicit disable — if someone adds `blizzerAudioManager.start()`
  later, they need to know this decision first.
- Tests: `BlizzerSeverityTests.kt`, `BlizzerAutoDismissTests.kt`, both passing.

### Phase B — Maps transport (Wear Data Layer, phone → watch) — 🟡 IN PROGRESS
Files that must exist and agree with each other (this is where the conflict happened):
- `wearos-app/.../hardware/WearDataLayerBluetoothProvider.kt` — package `com.dashboard.wearos.hardware`,
  **singleton** via `getInstance(context)`, implements `core`'s `BluetoothProvider`.
- `wearos-app/.../hardware/NavDataListenerService.kt` — same package, a `WearableListenerService`
  registered in `AndroidManifest.xml` for `com.google.android.gms.wearable.MESSAGE_RECEIVED`
  scoped to path `/automotive-dashboard/nav`.
- `wearos-app/MainActivity.kt` — uses `WearDataLayerBluetoothProvider.getInstance(this)` as
  **both** `ConnectionManager`'s real transport (real NFC-tap → real link state) **and**
  `BluetoothPhoneCommunication`'s transport for the real `NavigationManager`. Music/Blizzer still
  use a separate `MockPhoneCommunication` instance — this is intentional, not a bug: those
  subsystems aren't real yet.
- `phone-app/.../WearMessageSender.kt` — method name is **`sendNavUpdate`** (not `send`) — this
  was the second mismatch found. `NavigationAccessibilityService.kt` calls `sendNavUpdate`.
- Both `build.gradle.kts` files have `com.google.android.gms:play-services-wearable:20.0.0`.
- **Status as of last check**: package/API mismatches between the provider files and
  `MainActivity`/`WearMessageSender` were just fixed. **Not yet build-verified** — waiting on the
  user to sync + run both apps and report back.

### Phase C — Car panel (watch-side BLE, direct to vehicle OBD-II/CAN adapter) — 🔴 NOT STARTED
**Decision locked**: watch connects directly to the vehicle's Bluetooth adapter (not phone-relayed).
Plan: `BleVehicleDataProvider` (wearos-app) implementing `core`'s `VehicleDataProvider`, BLE GATT
scan/connect/subscribe against an ELM327-style OBD-II PID interface (recommended over a custom
protocol — standard, documented), auto-connect wired into `PowerManager`'s `onActive`/`onSleep`
(already scaffolded), `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` runtime permissions. Not
device-testable here — plan is to add unit tests for the OBD-II PID *parsing* logic only (pure
functions), verified without hardware.

### Phase D — Music (visual audio representation + real session) — 🔴 NOT STARTED
Plan: a synthetic/decorative waveform in `MusicScreen` driven by `MediaState.positionMillis`
(no real audio/FFT, since audio doesn't play on the watch) as the first step. Real now-playing
data via phone-side `NotificationListenerService` + `MediaSessionManager` (confirmed viable in
`docs/android-integration-research.md`) is a separate, later effort — same shape as the Maps
transport work, not yet started.

## How to verify before changing anything

Run these and read the actual output — don't assume from this doc or any chat summary:

```bash
./tools/run_tests.sh                                    # core — should show all passing, note the count
find wearos-app/src/main/kotlin/com/dashboard/wearos/hardware -type f
grep -n "class BlizzerManager" -A3 core/src/main/kotlin/com/dashboard/core/service/BlizzerManager.kt
grep -n "WearDataLayerBluetoothProvider\|getInstance" wearos-app/src/main/kotlin/com/dashboard/wearos/MainActivity.kt
grep -n "fun send" phone-app/src/main/kotlin/com/dashboard/phoneapp/WearMessageSender.kt
grep -n "NavDataListenerService" wearos-app/src/main/AndroidManifest.xml
```

## Update this file

Whenever a phase moves from 🔴/🟡 to ✅, or a real API shape changes (a method renamed, a package
moved), update the relevant section above **before** ending the session — that's what this file
is for.
