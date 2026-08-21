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
      communication/    (empty for now — filled in during the communication-protocol step)
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
- Console demo proving the CAR_ONLY → NFC tap → CONNECTED → disconnect → CAR_ONLY journey
- Minimal mocks for NFC, Bluetooth, audio, power and phone-communication — enough to compile
  and drive the connection state machine; their *real* dev-control behavior (Start Navigation,
  Trigger Blizzer, etc.) is filled in during their dedicated steps below

## What's next (not yet built)

1. **Maps panel** — `NavigationState` is defined; `NavigationAudioManager`, a navigation
   simulator, and the Maps UI still need building.
2. **Music panel** — `MediaState`/`MediaCommand` are defined; `MediaManager`, a simulated media
   source, and the Music UI still need building.
3. **Blizzer global overlay** — `BlizzerEvent` is defined; `BlizzerManager` and the
   overlay-over-any-screen behavior still need building.
4. **Communication protocol** — `PhoneCommunication` interface exists; the actual message
   encode/decode layer over `BluetoothProvider`, plus a `MockPhoneCommunication` that generates
   test traffic, still need building.
5. **Full developer control panel** (Start Navigation, Trigger Blizzer, etc.) once 1–4 exist.
6. **Settings/persistence**, **PowerManager** wiring, and the **end-to-end test** for the full
   user journey.
7. **Real Android/Wear OS investigation** — what MediaSession/MediaController, NFC, and
   background access Android actually permits, documented in `docs/`.
8. **Wear OS Compose UI** (`wearos-app/`), built in Android Studio against the now-stable
   `core` service layer.

Nothing above requires rewriting anything already built — each step adds a manager/mock behind
an existing or new interface and wires it into `ConnectionManager`'s `CONNECTED` state.
