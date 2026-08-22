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
Maven repository (`dl.google.com`) or Gradle's distribution service, so an Android Gradle build
cannot actually be *run* here. To avoid wasting the session on infrastructure that can't be
verified, the project is split in two:

- **`core/`** — pure, dependency-free Kotlin (JVM target). All domain models, hardware
  interfaces, mocks/simulators, and service logic (state machines, managers) live here. Zero
  Android dependency. Compiles, runs, and is **fully unit tested today** in this sandbox via
  `kotlinc` + `java` directly (see "How to test" below).
- **`wearos-app/`** — the real Wear OS Compose UI, wired to `core`. Written against real,
  sourced Wear OS Compose APIs, but **not yet build-verified** — see `wearos-app/README.md` for
  exactly what that means and what to check first when this is opened in Android Studio.

Both modules have proper `build.gradle.kts` files and a root `settings.gradle.kts`, so opening
the whole repo in Android Studio should work directly — `core` builds as a plain Kotlin/JVM
library module, `wearos-app` as the Android application module that depends on it.

This split is exactly the hardware-independence the product spec asks for, just drawn one layer
higher: `core` doesn't know about Android *or* real hardware. Only `wearos-app` knows about
Android, and only concrete hardware implementations (a later step — see
`docs/android-integration-research.md`) will know about NFC/Bluetooth/OBD-II/CAN/audio APIs.

## Project structure

```
automotive-dashboard/
  settings.gradle.kts, build.gradle.kts    Root Gradle project (see wearos-app/README.md re: build status)
  core/
    build.gradle.kts                       Plain Kotlin/JVM module, zero dependencies
    src/main/kotlin/com/dashboard/core/
      domain/          VehicleData, ConnectionState, NavigationState, MediaState,
                        BlizzerEvent, DashboardSettings, PowerState — plain data, no logic
      hardware/         VehicleDataProvider, NfcProvider, BluetoothProvider, AudioOutput,
                        PowerProvider, PhoneCommunication, SettingsStore — interfaces only,
                        the "hardware contract" the UI is never allowed to bypass
      hardware/mock/    Mock*/Simulated implementations of every interface above, plus
                        LoopbackBluetoothProvider (a paired test double for the transport)
      communication/    ProtocolMessage (wire contract), MessageCodec (encode/decode),
                        DomainMapping (wire <-> domain conversions), BluetoothPhoneCommunication
                        (the real PhoneCommunication impl, works over any BluetoothProvider)
      service/          ConnectionManager, VehicleDataManager, NavigationManager,
                        NavigationAudioManager, MediaManager, BlizzerManager, SettingsManager,
                        PowerManager, DevControlPanel — one manager per subsystem, each the
                        single thing its panel is allowed to depend on
      demo/             ConsoleDemo — text-mode stand-in for the real UI, composes the whole
                        app exactly like wearos-app's MainActivity does
    src/test/kotlin/com/dashboard/core/
      testing/          Tiny hand-rolled assertion/test-suite harness (see note below)
      tests/            63 tests across every manager, the codec, and a full end-to-end journey
  wearos-app/           Real Jetpack Compose Wear OS UI — see wearos-app/README.md for status
  docs/
    android-integration-research.md   Sourced research on what Android actually permits for
                                       Media, Navigation, NFC, and the phone-communication
                                       transport — every claim marked confirmed/restricted/
                                       requires-a-companion-app/needs-further-research
  tools/                Shell scripts to compile/run/test core without needing Gradle
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

Compiles `core` and runs `ConsoleDemo`, which composes the **entire** app (Car, Maps, Music,
Blizzer, Settings, Power, and the developer-control facade) exactly the way `wearos-app`'s
`MainActivity` does, and walks through the full spec'd user journey: dashboard boots with live
vehicle data → Blizzer overlays the Car panel → simulated NFC tap connects the phone → Maps and
Music become available → navigation updates with a turn-by-turn audio cue → music is controlled
→ Blizzer overlays Maps/Music the same way it did Car → phone disconnects, back to Car-only →
simulated ignition-off puts the dashboard to sleep.

## How to test

```
tools/run_tests.sh
```

Compiles and runs all **63 tests**: domain models (never-fabricate-unavailable-values
guarantee), every manager (`ConnectionManager`'s full state machine including the
tap-while-connecting no-op and failed-handshake `ERROR` recovery; `VehicleDataManager`,
`NavigationManager`, `MediaManager`, `BlizzerManager`'s late-subscriber caching;
`NavigationAudioManager`'s change-only audio firing; `SettingsManager`'s persistence;
`PowerManager`'s ACTIVE/SLEEP/WAKE dispatch), the wire protocol (`MessageCodec` round-trips,
plus `BluetoothPhoneCommunication` tested over a genuine `LoopbackBluetoothProvider` transport,
not just in-process calls), and one end-to-end test walking the entire user journey.

## What's implemented

Everything through the full simulated-hardware software system described in the spec:

- Tech-stack decision, Gradle project skeleton for both modules
- All domain models, all six hardware interfaces (+ `SettingsStore`)
- `ConnectionManager` — the full CAR_ONLY/NFC_DETECTED/CONNECTING/CONNECTED/DISCONNECTING/ERROR
  state machine
- Car panel (`VehicleDataManager` + a realistic gradually-changing vehicle simulator)
- Maps panel (`NavigationManager` + `NavigationAudioManager`, sounds decoupled from the UI)
- Music panel (`MediaManager` — play/pause/next/previous, no dependency on any specific app)
- Blizzer (`BlizzerManager` — a true global overlay with zero panel-awareness)
- The full communication protocol (`ProtocolMessage`/`MessageCodec`/`BluetoothPhoneCommunication`),
  proven over a real (loopback) transport, not just asserted
- Settings (`SettingsManager`, persistence-independent) and Power (`PowerManager`, drives the
  rest of the app's start/stop)
- `DevControlPanel` — every developer control from the spec behind one facade
- The real Wear OS Compose UI (`wearos-app/`) — written, not yet build-verified (see its README)
- Sourced research on real Android/phone integration capabilities and limits

## What's next

1. **Verify `wearos-app` actually builds** in Android Studio and fix whatever the first Gradle
   sync flags (see `wearos-app/README.md`'s "What still needs doing" section).
2. **Resolve the open platform question from `docs/android-integration-research.md`**: if Wear
   OS is confirmed as the real target, `BluetoothProvider` should likely become a Data-Layer-API
   -shaped interface instead of raw BLE bytes, since Google's own guidance says not to open raw
   Bluetooth sockets between a Wear OS watch and its paired phone.
3. **Build real hardware implementations** one at a time, per the research doc's findings —
   Media is realistically achievable via `NotificationListenerService` + `MediaSessionManager`;
   Navigation needs a real product decision first (there's no generic API for reading an
   arbitrary nav app's turn-by-turn state, unlike Media).
4. **Real swipe gestures, launcher icon, and UI polish** in `wearos-app` (currently tap/long-press
   placeholders — noted inline in the code).
5. **Persist settings for real** (Android DataStore) behind the existing `SettingsStore`
   interface — no consumer of `SettingsManager` needs to change.
