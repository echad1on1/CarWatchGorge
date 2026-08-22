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
      DashboardApp.kt          Top-level: panel switching + Blizzer overlay on top
      ui/
        Panel.kt                CAR / MAPS / MUSIC / DEV_CONTROLS
        CarScreen.kt             Always available, works with no phone
        MapsScreen.kt            Renders NavigationState, nothing more
        MusicScreen.kt           Renders MediaState + play/pause/next/previous
        BlizzerOverlay.kt        Global overlay, zero awareness of which panel is underneath
        DevControlsScreen.kt     Every developer control from the spec, bound to DevControlPanel
```

## What still needs doing here (once verified in Android Studio)

1. Confirm the build actually compiles and runs on a Wear OS emulator/device.
2. Replace the placeholder tap/long-press navigation in `CarScreen`/`MapsScreen` with real swipe
   gestures (`androidx.wear.compose.foundation`'s `SwipeToDismissBox` or a `HorizontalPager`).
3. Add a launcher icon (Android Studio's Image Asset tool) — the manifest deliberately omits
   `android:icon` for now since no icon resource exists in this sandbox.
4. Real hardware implementations (`AndroidNfcProvider`, a Bluetooth or Wear Data Layer-based
   `BluetoothProvider`, etc.) — see `docs/android-integration-research.md` first, since it flags
   an open question (raw BLE vs. the Wear OS Data Layer API) that should be resolved before this
   step, not during it.
