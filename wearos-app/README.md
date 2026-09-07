# wearos-app

The real Wear OS UI: Jetpack Compose for Wear OS, wired directly to `core`'s managers. Contains
**only** presentation — no business logic, no direct hardware access.

Real data sources are wired via the Wear OS Data Layer: **navigation**
(`WearDataLayerBluetoothProvider` + `NavDataListenerService`), **media**
(`MediaManager` on the real transport), **Blizzer** (camera-proximity feed). **Vehicle data**
uses `BleObdVehicleDataProvider` in non-debug builds (a BLE ELM327 link) and
`MockVehicleDataProvider` in debug. **Settings** persist via `DataStoreSettingsStore`. NFC is
still a `core` mock (most Wear watches can't read tags — connection is effectively automatic
via `CapabilityClient`).

## Status: builds; on-device verification pending

As of 2026-09-07 this module **compiles and assembles** (debug + release) — see the root
`PLAN.md` Phase 0c for the resolved version stack (Gradle 9.3, AGP 8.13.2, Compose BOM
2025.10.01, compileSdk 36). What still needs a real device / emulator:

- The `TESTING.md` §3 walkthrough on a Wear OS emulator (expect minor Wear Compose API drift
  to fix on first run).
- The paired-phone Data Layer round trip and the BLE OBD link (needs a dongle).

See `PLAN.md`'s "On-device verification checklist". `core` has 22 passing test suites (131
assertions, verified 2026-09-07).

## Structure

```
wearos-app/
  src/main/
    AndroidManifest.xml       Watch feature declaration; hardware permissions commented out
                               until their real implementations exist (see the file itself)
    kotlin/com/dashboard/wearos/
      MainActivity.kt          Composes managers; nav + link state via Wear Data Layer, other
                                subsystems still on mocks (see class doc)
      ComposeBridge.kt         Bridges core's callback-based observe() into Compose State,
                                without core needing a coroutines/Flow dependency
      DashboardApp.kt          Top-level: HorizontalPager for Car/Maps/Music + Blizzer overlay
                                on top, dev-controls entry point (debug builds only)
      ui/
        CarScreen.kt             Always available, works with no phone
        MapsScreen.kt            Renders NavigationState, nothing more
        MusicScreen.kt           Renders MediaState + play/pause/next/previous
        BlizzerOverlay.kt        Global overlay, zero awareness of which panel is underneath
        DevControlsScreen.kt     Every developer control from the spec, bound to DevControlPanel
```

## What still needs doing here (once verified in Android Studio)

1. Confirm the build actually compiles and runs on a Wear OS emulator/device — see the root
   `TESTING.md` for a full walkthrough once it does.
2. Real hardware still needed: NFC tap provider, watch-side vehicle BLE (`VehicleDataProvider`),
   phone-side media session capture. Navigation phone → watch transport is done — see
   `hardware/WearDataLayerBluetoothProvider.kt`.
3. Add a launcher icon (Android Studio's Image Asset tool) — the manifest deliberately omits
   `android:icon` for now since no icon resource exists in this sandbox.

## Developer controls

The **⚙** button (top-right, debug builds only — gated on `BuildConfig.DEBUG`) opens
`DevControlsScreen`, which has one button per developer control from the spec (Simulate NFC Tap,
Connect/Disconnect, Start/Stop Navigation, Change Direction, music transport, Trigger Blizzer,
simulate car sleep/active). A release build won't show this button at all — that's the actual
mechanism behind the spec's "these controls exist only for development and will later be removed
or hidden," rather than relying on someone remembering to delete it before shipping.

Panel switching itself (Car ↔ Maps ↔ Music) is a real swipe gesture via
`androidx.compose.foundation.pager.HorizontalPager` — Maps/Music only exist as pages once
`ConnectionState.CONNECTED`, and disconnecting animates the pager straight back to Car, matching
"when the phone disconnects the system returns to the normal Car panel."
