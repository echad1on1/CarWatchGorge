# Testing the dashboard with zero hardware

This project was built so the *entire* user journey — Car, Maps, Music, Blizzer, NFC, phone
connection, everything — can be exercised today, before any real watch, NFC reader, vehicle
interface, or phone integration exists. Two ways to do that, from quickest to most complete:

1. **`tools/run_demo.sh`** — runs the whole app as a text console (no Android needed at all).
   Good for a fast sanity check or CI. See the root README's "How to run" section.
2. **A Wear OS emulator in Android Studio** — this is the one that matters for your goal: it's
   the real UI, the real navigation, the real dev-controls screen, running on something that
   actually resembles the target hardware. **No physical watch and no paired phone are needed**
   — the app's phone-side is entirely `MockPhoneCommunication`, so a "standalone" Wear OS
   emulator is sufficient by itself.

The rest of this doc is the second path.

## 1. One-time setup

1. Install **Android Studio** (any recent stable release).
2. Open this repo's root folder in Android Studio (`File > Open`, pick `automotive-dashboard/`).
3. Let it sync Gradle. **This is the first real build-verification this project has had** — see
   `wearos-app/README.md` for what's likely to need a small fix (probably a dependency version
   bump; Android Studio will tell you exactly what and offer a quick-fix).
4. `Tools > Device Manager > Create Device > Wear OS` — pick a round or square Wear OS profile
   (e.g. "Wear OS Large Round"), system image API 33+ recommended. **Don't** pair it with a
   phone/AVD — leave it standalone. The manifest already declares
   `com.google.android.wearable.standalone = true` for exactly this reason.
5. Run the `wearos-app` configuration onto that emulator (green ▶ button, or `Shift+F10`).

You should see the Car panel appear with live (simulated) speed/RPM/coolant/etc. values ticking
up as soon as the app launches — that's `VehicleDataManager` + `MockVehicleDataProvider` running
for real, no phone involved.

## 2. Where the developer controls are

Top-right of the screen, in **debug builds only**, there's a small **⚙** button. Tap it to open
the full developer-controls screen — every control from the spec is a single button there
(Simulate NFC Tap, Connect/Disconnect, Start/Stop Navigation, Change Direction, music transport,
Trigger Blizzer, simulate car sleep/active). Tap **Close** to return to exactly where you were.

This button only exists because `BuildConfig.DEBUG` is true for a debug build — building a
`release` variant makes it disappear automatically, which is the actual mechanism for "these
controls exist only for development and will later be removed or hidden" from the spec.

## 3. A testing checklist matching the spec's user journey

Walk through this once, top to bottom, and you've exercised the same path
`EndToEndJourneyTests.kt` verifies automatically — just watched live on the emulator instead.

| Step | Action | Expected result |
|---|---|---|
| 1 | Launch the app | Car panel shows live vehicle data immediately, no phone needed |
| 2 | Open dev controls (⚙), tap **Trigger Blizzer (Info)** | A dark overlay with "Welcome back!" appears over the Car panel |
| 3 | Close dev controls | You're back on Car — Blizzer never changed what page you were on |
| 4 | Open dev controls, tap **Simulate NFC Tap** | Briefly nothing visible changes (CONNECTING is fast); after ~0.3s the pager gains 2 more pages |
| 5 | Close dev controls, swipe left | Maps panel appears, showing "Navigation not running" |
| 6 | Open dev controls, tap **Start Navigation** | Close dev controls → Maps now shows a direction arrow, road name, ETA |
| 7 | Open dev controls, tap **Direction: Left** | Close dev controls → Maps arrow updates to point left |
| 8 | Swipe left again | Music panel appears, showing "Nothing playing" |
| 9 | Open dev controls, tap **Start Music** | Close dev controls → Music shows a song/artist and a pause button |
| 10 | Tap **⏭** on the Music panel directly (no dev controls needed) | Track changes immediately — this is `MediaManager.next()` calling straight through `PhoneCommunication`, not a dev control |
| 11 | Open dev controls, tap **Trigger Blizzer (Warning)** | Overlay appears over Music (or wherever you are) — same mechanism as step 2, proving Blizzer really is panel-agnostic |
| 12 | Close dev controls, then dev controls again, tap **Disconnect Phone** | Pager snaps back to Car-only (Maps/Music pages disappear) — matches "when the phone disconnects the system returns to the normal Car panel" |
| 13 | Open dev controls, tap **Simulate car SLEEP** | Vehicle data freezes/stops updating — `PowerManager` stopped `VehicleDataManager` |
| 14 | Tap **Simulate car ACTIVE** | Vehicle data resumes ticking |

If every row behaves as described, the software system is doing exactly what it needs to do —
independently of whether any real hardware exists yet. When real hardware does arrive, the plan
is: swap one mock for one real implementation at a time (see the root README's "What's next"),
re-run this same checklist, and confirm nothing above it changed behavior.

## 4. If something doesn't match the table

- **Compile errors on first sync**: expected, see `wearos-app/README.md` — this hasn't been
  build-verified yet in a real Android Studio environment (this project was built in a sandbox
  with no Android SDK access). Fixes are likely small (a version bump Android Studio suggests).
- **Behavior differs from the table**: that's a real bug worth reporting/fixing — the same
  scenario is already covered by an automated test in `core/src/test/kotlin/.../tests/`
  (`ConnectionManagerTests`, `EndToEndJourneyTests`, etc.), so start there to see if the test
  needs updating or the app does.
