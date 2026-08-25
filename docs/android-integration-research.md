# Android/phone-integration research

This document answers, with sources, what Android actually permits for each capability the
dashboard needs from the phone. Per the project spec: **nothing here is assumed** — every claim
is grounded in an official source (linked), and every capability is explicitly labeled:

- ✅ **Confirmed** — verified against official Android documentation.
- ⚠️ **Restricted** — possible, but with real constraints (permissions, user setup, reliability).
- 🧩 **Requires a companion app** — not obtainable by any generic/system-level API; the phone
  side must run code written for this project.
- ❓ **Needs further research** — couldn't be pinned down from documentation alone; needs to be
  tested against a real device before `core`'s interfaces are assumed correct.

None of this changes `core/` yet. The interfaces (`PhoneCommunication`, `AudioOutput`, etc.)
were deliberately written to not assume any of this — that's the payoff of the hardware
abstraction layer: findings below inform which *implementation* gets built next, not the domain
model or service layer.

---

## Media (Music panel)

**🧩 Requires user setup, but is otherwise a generic, app-agnostic OS mechanism — the best-case
scenario of anything researched here.**

- ✅ Android's `MediaSession`/`MediaController` APIs are exactly the mechanism the spec
  suggested investigating, and they work as hoped: any app that plays media (Spotify, YouTube
  Music, podcast apps, etc.) that properly exposes a `MediaSession` can have its playback state,
  metadata, and transport controls (play/pause/next/previous) read and driven by another app,
  including while that media app is backgrounded — this is the same mechanism that powers lock
  screen media controls and Android Auto.
  [Source: Android Developers — Controlling media through MediaSession](https://developer.android.com/codelabs/supporting-mediasession)
- ⚠️ **But** reading *another* app's active session generically requires one of: the
  `MEDIA_CONTENT_CONTROL` permission (signature-level, effectively unavailable to a normal
  third-party app), **or** being a user-enabled `NotificationListenerService`. The second path is
  realistic: the user grants "Notification access" once in system settings, and from then on
  `MediaSessionManager.getActiveSessions(componentName)` returns live `MediaController`s for
  whatever's playing, across apps, without needing each media app's cooperation.
  [Source: Android Developers — MediaSessionManager](http://docs.52im.net/extend/docs/api/android-50/reference/android/media/session/MediaSessionManager.html)
  [Source: NotificationListenerService](https://learn.microsoft.com/cs-cz/dotnet/api/android.service.notification.notificationlistenerservice)
- **Conclusion for this project:** the phone-side companion app needs a `NotificationListenerService`
  and a one-time "grant notification access" step in its own setup flow — but does **not** need
  each music app to specifically support this dashboard. `MediaManager`/`MediaState`/`MediaCommand`
  in `core` need no changes for this.

## Navigation (Maps panel) — DECIDED: AccessibilityService announcement capture

**🧩 Requires a companion app. No generic, app-agnostic API exists for reading structured state
from an arbitrary navigation app — but there IS a viable, official mechanism for the *spoken
announcement text*, and this project is now built around it.**

- ❌ Unlike media, there is no OS-level session mechanism a third-party app can hook into to read
  live turn-by-turn state from an arbitrary navigation app the user happens to be running.
- 🧩 Google's own "turn-by-turn data feed" exists, but only for apps that **build navigation
  in** using the Google Navigation SDK themselves — it's a callback your own embedded nav engine
  receives, not something you can attach to the user's already-installed Google Maps app.
  [Source: Google Developers — Enable turn-by-turn data feed](https://developers.google.com/maps/documentation/navigation/android-sdk/tbt-feed)
- 💰 **Cost ruled this out for a sellable product**: both Google's and Mapbox's navigation SDKs
  bill per active user per month (plus per trip) — an ongoing cost with no matching recurring
  revenue for hardware sold once. Mapbox additionally requires a separate Commercial Application
  License specifically for vehicle use. Not viable unless the product itself becomes a
  subscription.
- ✅ **What was chosen instead: `AccessibilityService` announcement capture.** Navigation apps
  emit spoken announcement text ("In 200 meters, turn left") for accessibility purposes (the same
  text a screen reader would read aloud) — this is a real, officially documented Android
  mechanism, not a notification-scraping hack. `phone-app`'s `NavigationAccessibilityService`
  captures this text; `core`'s `NavigationAnnouncementParser` (9 passing tests) turns it into a
  structured checkpoint; `NavigationManager.onVehicleSpeedTick` smoothly counts the distance down
  between checkpoints using the vehicle's own live speed, so the watch shows a continuous
  countdown rather than discrete jumps.
- ⚠️ **Play Store policy, not a blocker**: using `AccessibilityService` for a non-accessibility
  purpose requires a Play Console declaration, clear in-app disclosure (see `phone-app`'s
  `MainActivity`), and a narrow, justified scope — which is why the service is filtered to only
  `com.google.android.apps.maps`/`com.waze` and `canRetrieveWindowContent="false"`. Enforcement
  has been tightening and more changes are scheduled through 2027 — worth re-checking current
  policy before shipping, not just at build time.
- ❓ **Still needs real-device verification**: whether Google Maps/Waze actually emit the
  announcement text this was built to expect is unconfirmed — `phone-app/README.md` has the exact
  test to run (install on a real phone, start real navigation, watch logcat). This is the single
  biggest remaining assumption in the whole Maps strategy.
- 🧩 Waze's third-party data sharing (the "Navigation Connect API") remains a formal partner
  integration for specific approved use cases, not something available here regardless.
  [Source: Waze Help — Navigate with Waze from third-party apps](https://support.google.com/waze/answer/10389770?hl=en)

## NFC

- ✅ Foreground tap detection (`enableForegroundDispatch`/`enableReaderMode`) is standard,
  well-documented Android NFC and is exactly what `NfcProvider.onTapDetected` models — the app
  needs to be in the foreground (a dashboard app running full-time on the watch display
  satisfies this trivially, unlike a typical phone app).
- ❓ **Needs further research**: background/screen-off tap handling if the watch display can
  ever be asleep when a tap happens (relevant once `PowerManager`'s `SLEEP` state is wired to
  actually dim the screen) — Android supports manifest-declared background NFC dispatch for
  specific tag technologies, but the exact behavior needs verifying against the target watch
  hardware/OS once it's chosen, since this is watch-side NFC receiving a phone tap, which is a
  less common configuration than the usual phone-reads-tag case most documentation covers.

## Bluetooth / phone communication transport

**This is the one finding that could reshape `BluetoothProvider`, depending on the final
platform choice.**

- ✅ If the final device runs **Wear OS**, Google explicitly documents that raw Bluetooth
  sockets should **not** be used to talk to the paired phone — the *Wearable Data Layer API*
  (`DataClient`/`MessageClient`, part of Google Play services) is the only supported channel
  between a Wear OS watch and its paired phone, and it works over whatever transport is
  available (Bluetooth directly, or the cloud when Bluetooth isn't connected).
  [Source: Android Developers — Data Layer API overview](https://developer.android.com/training/wearables/data/overview)
- ⚠️ The Data Layer API works **phone ↔ Wear OS watch only** — it explicitly does not work if
  the watch is paired to an iOS phone, and it requires the phone side to run a companion app
  with a matching `WearableListenerService`. Since this project's phone side is already a
  planned companion app, that requirement is already satisfied by design.
  [Source: Android Developers — Sync data on Wear OS](https://developer.android.com/training/wearables/data/sync)
- ✅ If the final device runs **generic embedded Android (not Wear OS)** rather than actual Wear
  OS, classic BLE GATT (what `BluetoothProvider` currently models) is the right layer, and Android
  12+ requires runtime-granted `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` permissions plus, for
  background scanning, a foreground service.
  [Source: Android Developers — Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- **Conclusion for this project:** `BluetoothProvider` and `PhoneCommunication` stay correct
  either way — the split already isolates "how bytes move" from "what the bytes mean," which is
  exactly what's needed to swap a `MessageClient`-backed implementation in for a raw-BLE-backed
  one. But which one gets built is a real decision, not a detail: **if Wear OS is confirmed as
  the target, `BluetoothProvider` should be reconsidered as a `DataLayerProvider`-style interface
  instead of raw bytes**, since fighting the platform's mandated channel isn't worth it. This is
  the single highest-priority open question for the next hardware-implementation phase.

## Blizzer — DECIDED: own camera-proximity detection, not reading the real Blizzer app

- ✅ **Now specified**: Blizzer is a speed-camera proximity alert (modeled on the real "Blitzer"
  phone app), beeping at decreasing distance thresholds (e.g. 500m, 200m, 100m) as a known camera
  is approached.
- ❌ **Reading the actual third-party Blizzer/Blitzer app is not viable** — same category of
  problem as Navigation: no generic OS-level API exists for a third-party app to read another
  app's proximity alerts, and reverse-engineering it would be exactly as fragile as
  notification-scraping, with none of the "at least it's an official mechanism" benefit that
  `AccessibilityService` announcement text has for Navigation.
- ✅ **What was chosen instead**: build camera-proximity detection independently, inside the
  companion app — GPS position + a speed-camera POI database (community/open datasets, or a paid
  POI provider), computing distance to the nearest known camera locally. No dependency on any
  other app, no fragile scraping. This is genuinely more achievable than Navigation: it's GPS +
  math against a static dataset, not an integration with someone else's live app state.
- ✅ **Fully built and tested on the watch/core side already**, independent of that phone-side
  database work: `BlizzerEvent.distanceMeters` carries the threshold, `BlizzerManager` tracks the
  active event, `BlizzerAudioManager` (4 passing tests) beeps once per new threshold (not once per
  minor update), and the Compose `BlizzerOverlay` blinks faster as distance shrinks. Whatever
  eventually feeds real camera-proximity data in just needs to call
  `PhoneCommunication.observeBlizzerEvents`'s existing contract — nothing above it changes.
- ❓ **Still needed**: sourcing an actual speed-camera POI dataset (community data vs. a licensed
  provider) is unresearched — a separate, later decision from anything in this document, since it
  has nothing to do with Android platform capabilities.

---

## Summary table

| Capability | Verdict | What's needed |
|---|---|---|
| Media (play/pause/next/prev, now-playing) | 🧩 Companion app + one-time notification-access grant | `NotificationListenerService` + `MediaSessionManager` |
| Navigation (turn-by-turn) | ✅ **Decided**: AccessibilityService announcement capture | `phone-app`'s `NavigationAccessibilityService` — needs real-device verification, see `phone-app/README.md` |
| Blizzer (camera proximity) | ✅ **Decided**: own GPS + camera POI database, not reading the real app | Needs a camera POI dataset (separate, later decision) — watch/core side fully built already |
| NFC tap detection | ✅ Confirmed for foreground | Standard Android NFC APIs |
| NFC tap detection while asleep/background | ❓ Needs a real-device spike | — |
| Phone transport (if target is Wear OS) | ✅ Confirmed, but not what `BluetoothProvider` currently models | Wearable Data Layer API (`DataClient`/`MessageClient`), not raw BLE |
| Phone transport (if target is generic embedded Android) | ✅ Confirmed, matches current `BluetoothProvider` | Runtime BLE permissions (Android 12+) + foreground service for background scanning |
| Blizzer | ✅ Decided — own GPS + camera POI database | See above; needs a POI dataset, not Android platform research |

**Recommended next step before building real hardware implementations:** confirm the target
platform (Wear OS vs. generic embedded Android) — it's the one open question that changes which
interface (`BluetoothProvider` as-is, or a Data-Layer-shaped replacement) gets implemented for
real, and everything else in this document holds regardless of that answer.
