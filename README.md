# automotive-dashboard

A software-first automotive dashboard for a watch/display mounted permanently inside a car.
The physical hardware (casing, NFC reader, vehicle interface, Bluetooth radio, audio wiring,
power circuitry) doesn't exist yet. This repo builds the **complete application and
architecture** against clean hardware interfaces, backed by mocks/simulators, so that real
hardware can be plugged in later without rewriting the app.

## Target platform decision

The eventual device is a small programmable watch/display. The most realistic hardware for
that form factor today is **Wear OS (Android/Kotlin)** — it's the only mainstream embedded-watch
platform with mature Bluetooth, NFC, and audio-routing APIs, and it lets the dashboard app be
built with Jetpack Compose for a small round/square display.

**However**, this development sandbox has no Android SDK and no network access to Google's
Maven repository (`dl.google.com`), so an Android Gradle build cannot be run here. To avoid
wasting the current session on infrastructure that can't be verified, the project is split in
two:

- **`core/`** — pure, dependency-free Kotlin (JVM target). All domain models, hardware
  interfaces, mocks/simulators, and service logic (state machines, managers) live here. Zero
  Android dependency. Compiles and runs today with `kotlinc` + `java`, and is fully unit
  tested today, in this sandbox.
- **`wearos-app/`** — placeholder for the real Wear OS Compose UI module. It will depend on
  `core` and add nothing but presentation. It should be opened in Android Studio (which has
  real network access to Google's Maven repo) to be built and run on an emulator/device. See
  `wearos-app/README.md`.

This split is exactly the hardware-independence the product spec asks for, just drawn one
layer higher: `core` doesn't know about Android *or* real hardware. Only `wearos-app` will
know about Android, and only concrete hardware implementations (added later, likely also
living under an Android module) will know about NFC/Bluetooth/OBD-II/CAN/audio APIs.

## Project structure

```
automotive-dashboard/
  core/
    src/main/kotlin/com/dashboard/core/
      domain/          VehicleData, ConnectionState, NavigationState, MediaState,
                        BlizzerEvent, DashboardSettings, PowerState — plain data, no logic
      hardware/         VehicleDataProvider, NfcProvider, BluetoothProvider, AudioOutput,
                        PowerProvider, PhoneCommunication — interfaces only, the "hardware
                        contract" the UI is never allowed to bypass
      hardware/mock/    Mock*/Simulated implementations of every interface above
      service/          ConnectionManager (state machine), VehicleDataManager (Car panel
                        data source) — more managers land here as each subsystem is built
      communication/    ProtocolMessage (wire contract), MessageCodec (encode/decode),
                        DomainMapping (wire <-> domain conversions), BluetoothPhoneCommunication
                        (real PhoneCommunication impl, works over any BluetoothProvider)
      demo/             ConsoleDemo — text-mode stand-in for the real UI, used to visually
                        verify behavior until the Wear OS/Compose UI exists
    src/test/kotlin/com/dashboard/core/
      testing/          Tiny hand-rolled assertion/test-suite harness (see note below)
      tests/            Actual test suites + deterministic test fakes
  wearos-app/           Placeholder for the future Jetpack Compose Wear OS UI module
  tools/                Shell scripts to compile/run/test without needing Gradle
  docs/                 Longer-form notes (Android/phone-integration research lands here later)
```

### Why a hand-rolled test harness instead of JUnit?

This sandbox can reach GitHub (to download the Kotlin compiler itself) but not Maven Central,
so JUnit/Kotest can't be pulled in here. `core/src/test/kotlin/.../testing/TestHarness.kt` is a
~40-line assertion/suite runner. Test bodies are plain functions (`test("name") { ... }`), so
migrating to JUnit5 once this project is opened in Android Studio (with normal Maven access) is
a mechanical rename, not a rewrite.

## How to run

```
tools/run_demo.sh
```

Compiles `core` and runs `ConsoleDemo`, which:
1. Boots the dashboard in `CAR_ONLY` and streams live simulated vehicle data (Car panel works
   with no phone at all).
2. Simulates the driver accelerating.
3. Fires a **Simulate NFC Tap** — watch the connection state machine walk
   `CAR_ONLY → NFC_DETECTED → CONNECTING → CONNECTED` in real time.
4. Fires a **Disconnect Phone** — watch it return to `CAR_ONLY`.

## How to test

```
tools/run_tests.sh
```

Compiles and runs all unit tests: `VehicleData`/`Signal` (never-fabricate-unavailable-values
guarantee), `VehicleDataManager` (caching/replay for late subscribers), and `ConnectionManager`
(every state transition, including the tap-while-connecting no-op and the failed-handshake
`ERROR → CAR_ONLY` recovery path).

## What's implemented so far

- Tech-stack decision and project skeleton (this step)
- All core domain models (`VehicleData` w/ per-field availability, `ConnectionState`,
  `NavigationState`, `MediaState`, `BlizzerEvent`, `DashboardSettings`, `PowerState`)
- All six hardware interfaces (`VehicleDataProvider`, `NfcProvider`, `BluetoothProvider`,
  `AudioOutput`, `PowerProvider`, `PhoneCommunication`)
- Connection state machine (`ConnectionManager`) — fully implemented and tested
- Car panel data layer (`VehicleDataManager` + `MockVehicleDataProvider`, a realistic vehicle
  simulator with gradually-changing speed/RPM/temp/load and a toggleable "this signal isn't
  available on this vehicle" case) — fully implemented and tested
- Console demo proving the full simulated journey: Car-only with live vehicle data → Blizzer
  overlay on Car → NFC tap → CONNECTED → Start Navigation → Change Direction → Start Music →
  Next Song → Blizzer overlay on Maps/Music → Stop Navigation/Pause Music → Disconnect → Car-only
- **Communication Layer** (this step): `ProtocolMessage` (wire message types for navigation,
  media, media commands, Blizzer events, connection state, settings), `MessageCodec`
  (dependency-free encode/decode — no JSON/protobuf library is reachable from this sandbox, see
  below), `DomainMapping` (wire ↔ domain conversions), and `BluetoothPhoneCommunication` — the
  real `PhoneCommunication` implementation, which works over *any* `BluetoothProvider`. Proven
  with `LoopbackBluetoothProvider` tests that actually push encoded bytes across two separate
  endpoints and decode them on the other side — not just in-process calls.
- **`MockPhoneCommunication`** now has full developer controls (`startNavigation`,
  `stopNavigation`, `changeDirection`, `decreaseDistance`, `startMusic`, `pauseMusic`,
  `nextSong`, `previousSong`, `triggerBlizzer`) and internally round-trips every update through
  the same `MessageCodec` the real implementation uses, so a codec bug can't hide behind the mock.
- Minimal mocks remain for audio and power — filled in during their dedicated steps below.

## What's next (not yet built)

1. **Maps panel** — `NavigationState` and the developer controls to drive it exist; the
   `NavigationAudioManager` and the actual Maps UI still need building.
2. **Music panel** — `MediaState`/`MediaCommand` and the developer controls to drive it exist;
   `MediaManager` (a thin wrapper matching the `VehicleDataManager` pattern) and the Music UI
   still need building.
3. **Blizzer global overlay** — `BlizzerEvent` and the developer control to trigger it exist;
   `BlizzerManager` and the actual overlay-over-any-screen UI behavior still need building.
4. **Full developer control panel UI** exposing everything already wired in `MockPhoneCommunication`.
5. **Settings/persistence**, **PowerManager** wiring, and the **end-to-end test** for the full
   user journey.
6. **Real Android/Wear OS investigation** — what MediaSession/MediaController, NFC, and
   background access Android actually permits, documented in `docs/`.
7. **Wear OS Compose UI** (`wearos-app/`), built in Android Studio against the now-stable
   `core` service layer.

Nothing above requires rewriting anything already built — each step adds a manager/mock behind
an existing or new interface and wires it into `ConnectionManager`'s `CONNECTED` state.
