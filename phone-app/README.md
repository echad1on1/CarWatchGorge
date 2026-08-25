# phone-app

The phone-side companion app. Runs on the driver's phone, captures spoken turn-by-turn
announcements from Google Maps or Waze via an `AccessibilityService`, and (once wired) sends
the resulting checkpoints to the watch.

## Why this exists — the Maps strategy

There's no official Android API for reading structured navigation data from an arbitrary app the
user is already running (see the root `docs/android-integration-research.md`). What those apps
*do* provide, for accessibility purposes, is the same spoken text a screen reader would read
aloud — "In 200 meters, turn left". This app captures that text and turns it into structured
data using `core`'s `NavigationAnnouncementParser` — the exact same parser already exercised by
`MockPhoneCommunication.announceNavigation` and its passing tests, so the logic is proven; only
its *input source* (real captured text vs. a dev-control string) differs here.

## Status: written, NOT yet verified on a real device

Two things specifically need a physical phone with Google Maps or Waze to confirm:

1. **Does `onAccessibilityEvent` actually receive announcement text the way this code expects?**
   This is the single biggest open assumption in the whole Maps strategy. `NavigationAccessibilityService`
   logs everything it captures (`Log.d(TAG, "Captured raw event text...")`) specifically so this
   can be checked immediately: install this app, enable it in Accessibility settings, start
   turn-by-turn navigation in Google Maps, and watch `adb logcat`.
2. **Sending to the watch is not wired yet.** The service currently only logs the encoded bytes
   it *would* send. Wiring a real `BluetoothProvider` here depends on resolving the same open
   question flagged in `docs/android-integration-research.md` (raw BLE vs. the Wear OS Data Layer
   API) — worth resolving once, since it affects both this app and the watch side identically.

## How to test the one thing that matters most right now

1. Open this project in Android Studio, run the `phone-app` configuration on a real phone (not
   an emulator — emulators can't run real Google Maps navigation).
2. Tap "Open Accessibility Settings" in the app, enable "Dashboard Companion".
3. Open Google Maps, start turn-by-turn navigation to anywhere nearby.
4. Watch `adb logcat -s NavAccessibilityService` (or the Logcat panel in Android Studio, filtered
   to that tag).
5. Look for `Captured raw event text from com.google.android.apps.maps: "..."` lines as
   turns approach.

If real announcement text shows up there looking roughly like what
`NavigationAnnouncementParser` expects (e.g. contains "turn left/right", a distance with a unit),
the whole strategy is validated and the remaining work is just wiring the Bluetooth send. If
nothing shows up, or the text looks completely different, that's the moment to revisit — cheaply,
before any more time is invested downstream.
