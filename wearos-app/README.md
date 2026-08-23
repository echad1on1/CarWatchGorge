# wearos-app

The real Wear OS UI: Jetpack Compose for Wear OS, wired directly to `core`'s managers. Contains
**only** presentation — no business logic, no direct hardware access. It still uses `core`'s
*mock* hardware implementations (see `MainActivity.kt`); real NFC/Bluetooth/vehicle
implementations are a deliberately separate, later step.

## Status: written, not yet build-verified

This code was written in a sandbox with **no Android SDK and no network access to Google's Maven
repository** (`dl.google.com` isn't reachable — see the root README's "Target platform decision"
section). That means:

- Every `.kt` file here is syntactically real Kotlin/Compose, written against APIs and package
  names sourced from Android's official Wear OS Compose documentation (linked in
  `build.gradle.kts`) — nothing invented.
- **None of it has actually been compiled.** The very first thing to do when this project is
  opened in Android Studio is `File > Sync Project with Gradle Files`, fix whatever Studio flags
  (most likely: a dependency version that's moved on since this was written, or a Compose API
  signature that's shifted slightly), and confirm it builds and runs on a Wear OS emulator.

Treat this module as a strong, structurally-correct starting point, not a finished, verified
deliverable — unlike `core`, which has 63 passing automated tests run in this same sandbox.

## Structure

```
wearos-app/
  src/main/
    AndroidManifest.xml       Watch feature declaration; hardware permissions commented out
                               until their real implementations exist (see the file itself)
    kotlin/com/dashboard/wearos/
      MainActivity.kt          Composes the app exactly like ConsoleDemo does (same mocks, same
                                managers) but renders Compose UI instead of println
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
2. Real hardware implementations (`AndroidNfcProvider`, a Bluetooth or Wear Data Layer-based
   `BluetoothProvider`, etc.) — see `docs/android-integration-research.md` first, since it flags
   an open question (raw BLE vs. the Wear OS Data Layer API) that should be resolved before this
   step, not during it.
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
