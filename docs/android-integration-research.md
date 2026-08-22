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

## Navigation (Maps panel)

**🧩 Requires a companion app, and is fundamentally more restricted than Media — there is no
generic, app-agnostic way to read "whatever navigation app the user has running."**

- ❌ Unlike media, there is no OS-level session mechanism a third-party app can hook into to read
  live turn-by-turn state from an arbitrary navigation app the user happens to be running.
- 🧩 Google's own "turn-by-turn data feed" exists, but only for apps that **build navigation
  in** using the Google Navigation SDK themselves — it's a callback your own embedded nav engine
  receives, not something you can attach to the user's already-installed Google Maps app.
  [Source: Google Developers — Enable turn-by-turn data feed](https://developers.google.com/maps/documentation/navigation/android-sdk/tbt-feed)
- 🧩 Waze's third-party data sharing (the "Navigation Connect API" / rideshare-style integration)
  is a formal partner integration for specific approved use cases, not a generic public API any
  app can subscribe to for a user's ordinary Waze session.
  [Source: Waze Help — Navigate with Waze from third-party apps](https://support.google.com/waze/answer/10389770?hl=en)
- ❓ **Needs further research**: whether a `NotificationListenerService` could reliably parse
  Android Auto-style ongoing navigation notifications (road name, distance, ETA are sometimes
  present in the notification's text/extras for some navigation apps). This is unofficial,
  fragile (breaks on notification format changes, differs per app), and closer to screen-scraping
  than an API — worth a real-device spike, but not something to design the primary implementation
  around.
- **Conclusion for this project:** realistically, Maps needs its own dedicated companion
  navigation experience — either the phone-side companion app embeds Google's Navigation SDK
  directly (so it *is* the navigation engine, not a passive observer of Google Maps), or the
  scope is narrowed to "navigation info this project's own companion app produces." This is a
  bigger decision than the others and should be made deliberately, not discovered late — flagging
  it now while `NavigationState`/`NavigationManager` are still cheap to adjust.

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

## Blizzer

- ❓ **Needs further research** — Blizzer's real data source isn't specified anywhere in the
  provided context (is it vehicle-derived, phone-derived, cloud-derived, or dashboard-local?).
  Nothing here can be confirmed or restricted without knowing what "Blizzer" actually
  integrates with in the real product. `BlizzerEvent`/`BlizzerManager` were built to be a pure
  "event in, overlay out" pipe specifically so this is a safe unknown to defer — whatever
  produces Blizzer events later just needs to call the same `PhoneCommunication.observeBlizzerEvents`-
  shaped contract (or a new hardware interface, if it turns out Blizzer doesn't come from the
  phone at all).

---

## Summary table

| Capability | Verdict | What's needed |
|---|---|---|
| Media (play/pause/next/prev, now-playing) | 🧩 Companion app + one-time notification-access grant | `NotificationListenerService` + `MediaSessionManager` |
| Navigation (live turn-by-turn from an arbitrary nav app) | 🧩 Companion app, narrower than hoped | Either embed Google's Navigation SDK in the companion app, or scope down to nav data the companion app itself produces |
| NFC tap detection | ✅ Confirmed for foreground | Standard Android NFC APIs |
| NFC tap detection while asleep/background | ❓ Needs a real-device spike | — |
| Phone transport (if target is Wear OS) | ✅ Confirmed, but not what `BluetoothProvider` currently models | Wearable Data Layer API (`DataClient`/`MessageClient`), not raw BLE |
| Phone transport (if target is generic embedded Android) | ✅ Confirmed, matches current `BluetoothProvider` | Runtime BLE permissions (Android 12+) + foreground service for background scanning |
| Blizzer | ❓ Unknown — spec doesn't define its real data source | Needs product clarification before any research is possible |

**Recommended next step before building real hardware implementations:** confirm the target
platform (Wear OS vs. generic embedded Android) — it's the one open question that changes which
interface (`BluetoothProvider` as-is, or a Data-Layer-shaped replacement) gets implemented for
real, and everything else in this document holds regardless of that answer.
