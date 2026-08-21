# wearos-app (placeholder)

This module will hold the real Wear OS UI, built with Jetpack Compose for Wear OS, once this
project is opened in an environment with Android SDK + Google Maven access (Android Studio).

It is intentionally empty right now. It will:

- Depend on `core` (already built) for all domain models and service classes
  (`ConnectionManager`, `VehicleDataManager`, and the managers added in upcoming steps).
- Contain **only** presentation: Compose screens for Car/Maps/Music, a Blizzer overlay
  Composable, and the developer-controls screen — no business logic, no direct hardware access.
- Provide the concrete Android implementations of the `core` hardware interfaces
  (`NfcProvider` via Android's NFC APIs, `BluetoothProvider` via BLE, `AudioOutput` via Android's
  audio routing, `PowerProvider` via a broadcast receiver on vehicle power, `VehicleDataProvider`
  via a future OBD-II/CAN library) **when that step is reached** — not before, per the project's
  incremental-and-verified approach.

Until then, `core/src/main/kotlin/com/dashboard/core/demo/ConsoleDemo.kt` is the stand-in UI
used to visually verify each subsystem as it's built.
