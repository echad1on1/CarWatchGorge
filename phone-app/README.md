# phone-app

The phone-side companion app. Captures spoken turn-by-turn announcement text from Google Maps or
Waze via `NavigationAccessibilityService`, parses it through `core`'s
`NavigationAnnouncementParser`, and sends encoded checkpoints to the paired watch over the
Wear OS Data Layer (`WearMessageSender`).

The companion now does three jobs, all over the Wear Data Layer on `/automotive-dashboard`:
| Feature | Component |
|---|---|
| Navigation | `NavigationAccessibilityService` → `NavigationAnnouncementParser` → `WearMessageSender` |
| Media | `media/MediaNotificationListenerService` (+ `MediaSessionHub`); `WearInboundListenerService` applies watch transport commands |
| Speed cameras | `blizzer/CameraProximityService` (foreground GPS) + `blizzer/SpeedCameraRepository` (`assets/speed_cameras.geojson`) |

## Status

- **Builds** (debug + release) as of 2026-09-07 — see root `PLAN.md` Phase 0c.
- **Not yet device-verified:** whether Maps/Waze actually emit the announcement text this
  service expects **while backgrounded** (the single biggest open risk — see `PLAN.md`'s
  on-device checklist step 3); the media session capture + watch→phone controls; the GPS
  camera feed on a real drive. The bundled camera dataset is a 6-point sample — replace it
  with a real OpenStreetMap extract (ODbL, keep attribution).

## How to test (real device)

1. Install `phone-app` on an Android phone and `wearos-app` on a paired Wear OS watch/emulator.
2. Grant **Accessibility** access to Dashboard Companion (button on the phone app's main screen).
3. Start navigation in Google Maps or Waze on the phone.
4. On the watch: simulate NFC tap (dev controls ⚙) to reach `CONNECTED`, open the Maps panel.
5. Watch logcat:
   - Phone: `NavAccessibilityService` — parsed checkpoints; `WearMessageSender` — send success/failure.
   - Watch: inbound messages should update `NavigationManager` / Maps panel.

## Structure

```
phone-app/
  src/main/kotlin/com/dashboard/phoneapp/
    MainActivity.kt                    Accessibility setup instructions
    NavigationAccessibilityService.kt  Maps/Waze screen-text capture + parse
    WearMessageSender.kt               Data Layer send to watch
```

## Developer note

Dev-control "Announce navigation" buttons on the watch still feed `MockPhoneCommunication` for
music/blizzer simulation — they do **not** drive the Maps panel once real transport is wired.
Use the phone app + real Maps navigation to test the live pipe.
