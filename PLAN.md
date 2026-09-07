# Implementation Plan — `automotive-dashboard` → fully functional app

**Living document.** Update the Changelog and the affected phase/section after *every*
change to the codebase. When a phase moves 🔴/🟡 → ✅, or a file/API shape changes, or a
bug in the register is fixed, edit this file **in the same commit**. This file and
`PROJECT_STATUS.md` must never disagree — `PROJECT_STATUS.md` is the terse per-phase
tracker, this is the full plan with file-level detail.

---

## Changelog

| Date | Change | By |
|---|---|---|
| 2026-09-07 | Initial plan created from a full-repo + environment audit. | Claude (Plan agent) |
| 2026-09-07 | **Phase 0b done.** `core/bin/` (60 committed dupes) removed from git; `tools/run_tests.sh` green on this machine with bundled kotlinc 2.3.10 — **19 suites / 98 assertions, 0 errors, 0 required fixes** (3 harmless `!!` warnings left as-is); `ConsoleDemo` runs end-to-end. Added `runCoreTests` Gradle task + `check` hook to `core/build.gradle.kts` and migrated its deprecated `kotlinOptions` → `compilerOptions` DSL. Added `.github/workflows/build.yml` (core job + an android job that stays red until 0c). Corrected the "63"/"91+" test counts and the stale "no Android SDK on this machine" claims in `README.md` / `PROJECT_STATUS.md` / `wearos-app/README.md`. | Claude |
| 2026-09-07 | **Phase 0c done — and it built here, no Android Studio needed.** This machine has network to Google Maven + Maven Central and Gradle 9.3.0 runs on JDK 25. Resolved every fictitious dependency version against the real repos; **AGP 8.9.1 → 8.13.2**, **Compose BOM `2026.05.00` → `2025.10.01`** (newer BOMs demand AGP 9.1 + compileSdk 37), `play-services-wearable 20.0.0 → 19.0.0`, `lifecycle 2.9.0 → 2.9.4`, `core-ktx 1.13.1 → 1.16.0`, dropped the pinned `ui-tooling` versions (BOM governs), Wear Compose stayed 1.6.2 (already the latest stable). `kotlinOptions` → `compilerOptions` in both app modules. **`./gradlew :core:runCoreTests :wearos-app:assembleDebug :phone-app:assembleDebug` → BUILD SUCCESSFUL** — first successful compile of either Android module in the project's history. `local.properties` recreated in the worktree (gitignored). | Claude |
| 2026-09-07 | **Blizzer 5-tier consolidation (decision #9).** Deleted `BlizzerSeverity.kt` + `BlizzerSeverityTests.kt`, removed the suite from `AllTests.kt`; `BlizzerOverlay.kt` rewritten to use `BlizzerProximity` (blue→green→amber→red across 2000/1000/500/200/100 m) and the missing `getValue` import added. Core suite now **18 suites / 94 assertions**, green. | Claude |
| 2026-09-07 | **Phase 5 (persistence + polish) — code complete, build-verified incl. release.** `core`: pure `SettingsCodec` (`DashboardSettings` ↔ line format, forward-compatible) + 5 tests → **22 suites / 131 assertions, green.** `wearos-app`: `DataStoreSettingsStore` (Jetpack DataStore behind `core`'s `SettingsStore` — one `runBlocking` startup read + in-memory mirror + fire-and-forget writes; `SettingsManager` unchanged); `InMemorySettingsStore` no longer wired (kept for `ConsoleDemo`/tests). `DevControlsScreen` `LazyColumn` → Wear `ScalingLazyColumn`. Adaptive launcher icons (vector gauge, `mipmap-anydpi-v26`) on **both** apps + `android:icon`/`roundIcon`. `release` buildType on both modules (minify off for now — R8 is a low-risk follow-up; the ⚙ dev entry already hides itself via `BuildConfig.DEBUG`). **`./gradlew :core:runCoreTests :wearos-app:assembleDebug :wearos-app:assembleRelease :phone-app:assembleDebug :phone-app:assembleRelease` → BUILD SUCCESSFUL.** No settings *UI* yet (out of scope) and dev code still compiles into release (the `src/debug` move is deferred polish). | Claude |
| 2026-09-07 | **Phase 4 (Blizzer real feed) — code complete, build-verified.** `core`: `com.dashboard.core.blizzer.CameraProximity` — pure haversine, `nearest` (lat/lon bounding-box prefilter), `crossedThreshold` (tightest newly-entered band, 15 m hysteresis so GPS jitter doesn't re-fire), `toBlizzerEvent`/`clearedEvent`. **7 tests; core now 21 suites / 126 assertions, green.** `phone-app`: `SpeedCameraRepository` (loads `assets/speed_cameras.geojson`, an OSM-shaped FeatureCollection — a 6-point Zagreb sample is bundled, swap for a real ODbL extract), `CameraProximityService` (foreground `location` service, FusedLocation @1 Hz → nearest → crossedThreshold → `BlizzerTrigger` frame; clears on leaving range / switching camera). `MainActivity` start/stop buttons + fine/background-location grant flow. Manifest: location + FGS + `POST_NOTIFICATIONS` perms, the service. `wearos-app`: `BlizzerManager` moved mock → `navPhoneCommunication`. **Needs a device + a real camera dataset** to verify on the road. `MockPhoneCommunication` on the watch is now only used by `DevControlPanel`. | Claude |
| 2026-09-07 | **Phase 3 (Car / OBD-II) — code complete, build-verified.** `core`: `com.dashboard.core.vehicle.ObdPidParser` — pure ELM327-response tokenizer (spaced / unspaced / command-echo / 11-bit-CAN-header / multi-PID / `NO DATA`/`SEARCHING`/`?`/truncated), per-PID decoders (RPM, speed, coolant, oil temp, load, throttle, fuel, control-module voltage), `toVehicleData` that leaves absent PIDs `Signal.Unavailable` (gear + oil pressure are never OBD-II). **8 tests; core now 20 suites / 119 assertions, green.** `wearos-app`: `ObdGattProfile` (3 candidate GATT layouts + AT init), `BleObdVehicleDataProvider` (scan → connect → discover → init → round-robin PID poll → emit, with reconnect), `VehicleProviderFactory` (mock in debug so TESTING.md still works, BLE otherwise). Manifest: `BLUETOOTH_SCAN`/`CONNECT` + legacy trio + `bluetooth_le` feature; runtime permission request in `MainActivity` (non-debug only). `DevControlPanel.vehicleProvider` is now nullable (no simulated driver on a real link). **Device wiring unverifiable here — needs a real ELM327 dongle.** | Claude |
| 2026-09-07 | **Phase 2 (Maps) — parser hardened.** `NavigationAnnouncementParser` keyword coverage expanded to documented Maps/Waze phrasings: U-turn, merge, ramp/exit (→ `EXIT_LEFT`/`EXIT_RIGHT`), slight/sharp turns (→ KEEP/TURN), "head <compass>", "continue on/for", more Croatian; imperial units (mi/ft/yd) and European `1,5` decimal commas now convert to metres; road-name extraction also runs on the live-banner fragment. `Direction` enum gained `U_TURN, MERGE, EXIT_LEFT, EXIT_RIGHT` — `MapsScreen` glyphs + `NavigationAudioManager` updated. **20 parser tests, core now 19 suites / 111 assertions, green.** `docs/android-integration-research.md` corrected (`canRetrieveWindowContent` is `true`, and the Play-Store para now reflects the sideload decision). AccessibilityService kept (decision #4). Real-device announcement capture is still the open spike — user-run. | Claude |
| 2026-09-07 | **Phase 1 (Music) — code complete, build-verified.** `core`: `MediaSessionSelection` + `SessionSnapshot` (pure "which app do we follow" rule: PLAYING > PAUSED > STOPPED/UNKNOWN, then most-recent) + `MediaSessionSelectionTests` (6 tests). **Core now 19 suites / 100 assertions, green.** `phone-app`: `MediaNotificationListenerService` (reads any app's session via `MediaSessionManager.getActiveSessions`, needs the notification-access grant), `MediaSessionHub` (process-wide handle to the chosen controller), `WearInboundListenerService` (applies `MediaCommandMessage` from the watch to `transportControls`). `MainActivity` rewritten with both grant buttons + live status. Manifest: 2 new services. `wearos-app`: `MediaManager` moved from mock → `navPhoneCommunication` (real Data Layer); `MusicScreen` gained a decorative bar waveform (frame-timed, gated on PLAYING) + a progress bar. All three modules still `assembleDebug` / tests green. **Needs a device** for the real session-capture + watch→phone command round trip. | Claude |
| 2026-09-07 | **Phase 0e (Data Layer bugs) — fixed and compiling.** `WearDataLayerBluetoothProvider` rewritten: no more main-thread `Tasks.await` (all async `Task` callbacks — safe to call from `onNfcTap`), a live `CapabilityClient` listener drives link state on later phone connect/disconnect, an 8-frame replay buffer covers the cold-start race, `send()` resolves the target node by capability instead of broadcasting. `NavDataListenerService` now creates the singleton via `applicationContext`. Added `res/values/wear.xml` capability decls to **both** apps (`automotive_dashboard_watch` / `_phone`). `phone-app/WearMessageSender` rewritten to resolve the watch by capability; `sendNavUpdate` kept as an alias, generic `send()` added. Both apps still `assembleDebug` clean. **Still needs a real device to confirm cross-package delivery + link-state behaviour.** | Claude |

---

## Status snapshot

| Phase | Title | Status |
|---|---|---|
| 0 | Environment & build verification | 🟡 0b + 0c + 0e done (all 3 modules build here). 0d = emulator/device walkthrough (user) |
| 1 | Music — real now-playing + controls + waveform | 🟡 Code complete + builds; needs a device to verify session capture |
| 2 | Maps — harden parser (AccessibilityService kept, per decision #4) | 🟡 Parser hardened + builds; real-device announcement spike still open (user) |
| 3 | Car — watch-side BLE OBD-II | 🟡 Parser done + tested; BLE provider written + builds; needs a dongle |
| 4 | Blizzer — real GPS + camera-POI feed | 🟡 5-tier colours + `CameraProximity` + phone GPS service done + builds; needs a device + real dataset |
| 5 | Persistence & polish | 🟡 DataStore + icons + release build done; no settings UI, `src/debug` move deferred |

### Locked decisions (2026-09-07)

- **#4 Maps capture:** keep the AccessibilityService (not the notification pivot).
- **Distribution:** standalone watch app, hardcoded/preinstalled on the in-car device; the
  phone companion just feeds it when connected. No Play Store ceremony, OSM camera data.
- **#9 Blizzer colours:** 5-tier (`BlizzerProximity`) — done.
- **Voice cues:** none — `AudioOutput` stays the mock; visual nav only.
- **Build toolchain (this machine):** Gradle 9.3.0 + JDK 25 + AGP 8.13.2 + Kotlin 2.1.10 +
  Compose BOM 2025.10.01 + compileSdk 36. Verified building.

Panel data sources today: **Car** mock · **Maps** transport wired (unbuilt) · **Music** mock ·
**Blizzer** watch/core done, no real feed.

---

## Verified current state (trust this over the repo docs)

### Environment on this machine — *contradicts `README.md` / `wearos-app/README.md`*

The repo docs claim "no Android SDK, no kotlinc, no network to Google Maven" — **false for
this machine**:

- Android SDK at `~/Library/Android/sdk` — platforms `android-35/36/37.0`, build-tools
  `34/35/36`, a **Wear OS system image** (`android-37.0/android-wear-signed/arm64-v8a`),
  phone image (`android-37.1 google_apis_playstore`).
- Two AVDs already exist: `Wear_OS_Large_Round` (target `android-37.0`) and `Pixel_10`.
- Android Studio 2026.1.3 (`AI-261…`), bundling **JBR = JDK 25** and **kotlinc 2.3.10** at
  `/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc/bin/`.
- System JDK = Temurin 25, `JAVA_HOME` unset. No Homebrew, no SDKMAN, no standalone
  `kotlinc`/`adb`/`sdkmanager` on PATH, no `cmdline-tools` in the SDK.
- `.idea/gradle.xml` → `gradleJvm = #GRADLE_LOCAL_JAVA_HOME`.

### Repo facts

- 3 Gradle modules exactly as documented. `core` is genuinely pure Kotlin/JVM — no Android
  import anywhere.
- **Nothing in `wearos-app` or `phone-app` has ever been compiled.**
- ~~`core/bin/` — 60 committed byte-identical dupes of `core/src/main/kotlin/**`.~~
  **Removed from git 2026-09-07** (`git rm -r --cached`; `.gitignore` already covered it).
- Test harness = a `main()` in `core/src/test/kotlin/.../tests/AllTests.kt` running
  **19 suites / 98 assertions** — all passing, verified 2026-09-07 with kotlinc 2.3.10.
  (Docs previously claimed "63" and "91+" — both corrected.)
- Phase B nav-transport files are self-consistent *as written*:
  `WearDataLayerBluetoothProvider` in package `com.dashboard.wearos.hardware`, singleton via
  `getInstance(context)`, `MESSAGE_PATH = "/automotive-dashboard/nav"`;
  `NavDataListenerService` → `WearDataLayerBluetoothProvider.pushInbound`;
  `phone-app/WearMessageSender.sendNavUpdate` matches its caller. Manifest registers the
  listener with `pathPrefix="/automotive-dashboard"`. **None of it compiles yet.**
- `wearos-app/MainActivity.kt` wires `WearDataLayerBluetoothProvider` into both
  `ConnectionManager` and `BluetoothPhoneCommunication`→`NavigationManager`; Music/Blizzer
  still on `MockPhoneCommunication`; vehicle/NFC/power/settings on mocks.

---

## Bugs found by reading (not yet caught — nothing compiles or runs)

Tracked in the Risk Register too; listed here for visibility.

1. **`WearDataLayerBluetoothProvider.connect()` calls `Tasks.await(...)` synchronously.**
   `ConnectionManager.onNfcTap()` runs on the UI thread → `Tasks.await` on main thread
   throws `IllegalStateException`. Crashes on the first NFC tap. *(Fix in Phase 0e.)*
2. **Link state is a one-shot poll inside `connect()`.** No `CapabilityClient` /
   `onPeerConnected` listener → the watch never learns about later phone connect/disconnect,
   so `ConnectionManager` never transitions to `DISCONNECTING` when the phone drops.
3. **`NavDataListenerService.pushInbound` no-ops on a cold-started process** (no Activity →
   `getInstance` never called → `instance == null`). Works only while the watch app is
   already alive. *(Fix: start provider from `Application.onCreate`, or buffer in the
   service.)*
4. **Cross-package Data Layer unverified** — watch `com.dashboard.wearos`, phone
   `com.dashboard.phoneapp`. `MessageClient` delivery across different package names +
   signing keys needs a real test.
5. **Senders broadcast to all connected nodes** instead of resolving the right one via
   `CapabilityClient`.
6. **Two parallel, inconsistent Blizzer color systems in `core`:**
   `BlizzerSeverityMapper` (3 tiers, thresholds 1000/500 — what `BlizzerOverlay.kt`
   actually uses) vs `BlizzerProximity` (5 tiers incl. amber, thresholds 1000/500/200/100 —
   **unused**). `PROJECT_STATUS.md` Phase A claims "5 thresholds (2000/1000/500/200/100)"
   and "blue→green→red"; the code implements 3 tiers. *(Reconcile in Phase 4 — decision #9.)*
7. **`accessibility_service_config.xml` has `canRetrieveWindowContent="true"`** (correct,
   required for `rootInActiveWindow`); `docs/android-integration-research.md` says it's
   `false`. Doc is stale.
8. **Version stack is internally incompatible** — see Phase 0c.

---

## Stale / contradictory / decision-blocked items in the docs

| Item | Where | Problem | Action |
|---|---|---|---|
| ~~"No Android SDK / no kotlinc in this sandbox"~~ | `README.md`, `wearos-app/README.md` | ✅ Fixed 2026-09-07 — added a build-environment note | done |
| ~~Test count "63" vs "91+"~~ | `README.md` / `PROJECT_STATUS.md` / `wearos-app/README.md` | ✅ Fixed 2026-09-07 → "19 suites / 98 assertions" | done |
| `canRetrieveWindowContent="false"` | `docs/android-integration-research.md` | Actual config is `true` | Correct the doc (Phase 2) |
| Blizzer "5 thresholds, blue→green→red" done | `PROJECT_STATUS.md` Phase A | Overlay uses the 3-tier mapper; 5-tier class unused | Decision #9, then reconcile |
| `BluetoothProvider` "should become Data-Layer-shaped" | `README.md` "What's next" #2 | `docs/android-integration-research.md` concludes the opposite | **Resolved below — no change** |
| Gradle 9.3.0 + AGP 8.9.1 + Kotlin 2.1.10 | root `build.gradle.kts`, wrapper, module builds | Mutually incompatible; wearos versions self-described as guesses | Phase 0c |
| ~~`core/bin/` committed~~ | `.gitignore` vs git index | ✅ Fixed 2026-09-07 — `git rm -r --cached core/bin` | done |
| Summary table lists "Blizzer" twice | `docs/android-integration-research.md` | Cosmetic | Optional cleanup |
| "Music/Blizzer use a separate MockPhoneCommunication — intentional" | `PROJECT_STATUS.md` Phase B | Becomes false after Phases 1 & 4 | Update per phase |

---

## Resolved architectural question — `BluetoothProvider` shape

**Decision: do NOT reshape `BluetoothProvider`. Keep it a raw `ByteArray` in / `ByteArray`
out pipe.**

- The watch↔phone transport is `MessageClient`, itself a `byte[]` pipe
  (`sendMessage(nodeId, path, data: ByteArray)`). The current interface maps 1:1.
  `WearDataLayerBluetoothProvider` already implements it with zero `core` change;
  `LoopbackBluetoothProvider` proves transport-independence in tests.
- `MessageCodec`'s wire format already carries a type tag on line 1 (`NavigationUpdate`,
  `MediaUpdate`, `BlizzerTrigger`, `MediaCommandMessage`, …). When Music & Blizzer go real
  over Data Layer, everything shares the single path `/automotive-dashboard/*` and is
  demuxed by that tag in `BluetoothPhoneCommunication.onInbound` (already a `when(message)`).
- The Car panel's watch↔vehicle BLE link does **not** use `BluetoothProvider` at all — it
  implements `VehicleDataProvider`, returning `VehicleData` directly with its own GATT
  stack. So `BluetoothProvider` is *only ever* the phone link.
- A Data-Layer-shaped `core` interface would leak Wear OS concepts (nodes, capabilities,
  `MessageClient`) into the pure module — the exact coupling the architecture prevents.

**Optional cosmetic follow-up (not on critical path):** rename `BluetoothProvider` →
`PhoneLinkTransport` (+ the two mock impls) so the name stops implying raw BLE. Pure rename.

**Where the architecture must bend (call-outs):**
- Phase 3 adds one new **pure** `core` package `com.dashboard.core.vehicle` (`ObdPidParser`)
  — additive, Android-free, no interface change.
- Phase 4 adds one new **pure** `core` package `com.dashboard.core.blizzer`
  (`CameraProximity`) — same.
- Phase 5: `SettingsStore.load()/save()` are synchronous, Android DataStore is async.
  Handled *inside* the impl (`runBlocking` for the single startup read, fire-and-forget
  writes) — no interface change. Fallback if unacceptable: add `suspend fun loadInitial()`
  to the interface (a real small `core` change) — flagged, not expected.

---

# Phase 0 — Environment & build verification

**Goal:** every module compiles; `core` tests green on this machine; `wearos-app` runs on
the standalone Wear emulator and passes the `TESTING.md` checklist; the paired-phone nav
pipe delivers one real checkpoint; CI build gate running. De-risks all "never compiled"
code and establishes the gate that would have caught the past
`WearDataLayerBluetoothProvider` / `sendNavUpdate` conflicts.

### 0a. Toolchain (no installs strictly required)

- **kotlinc for the harness:** `export PATH="/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc/bin:$PATH"`
  (kotlinc 2.3.10, `kotlin` runner included). Optionally install standalone Kotlin later
  for CI portability.
- **JDK for Gradle:** AGP rejects JDK 25. Either run all Gradle from **Android Studio** (it
  manages JDK + Gradle + AGP upgrade prompts), or install Temurin **21** and set
  `org.gradle.java.home` in `gradle.properties` / `JAVA_HOME`. Do **not** rely on system
  Temurin 25.
- **SDK cmdline-tools:** only needed for CLI `sdkmanager`. Install `cmdline-tools;latest`
  via Studio's SDK Manager if scripted/CI builds are wanted.
- `local.properties` already points at the real SDK and is git-ignored — leave it.

### 0b. Get `core` tests green (headless, no network) — ✅ DONE 2026-09-07

1. ✅ `git rm -r --cached core/bin` + removed the directory (60-file byte-identical dupe of
   `src/main`). `.gitignore` already lists `core/bin/`.
2. ✅ `tools/run_tests.sh` with the Studio-bundled `kotlinc` on PATH
   (`/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc/bin`, **kotlinc-jvm
   2.3.10, JRE 25**). Compiles `core/src/main` + `core/src/test`, runs
   `com.dashboard.core.tests.AllTestsKt`.
3. ✅ **No compile errors** under Kotlin 2.3.10 — the "fix newer-Kotlin errors" step was a
   no-op. 3 warnings only: `unnecessary non-null assertion (!!)` in
   `BluetoothPhoneCommunicationTests.kt:36` and `MockPhoneCommunicationTests.kt:95,107` —
   left as-is (cosmetic; not worth the diff noise now, sweep during the JUnit5 migration).
4. ✅ Real count: **19 suites, 98 `PASS` assertions, `ALL TESTS PASSED`.** Updated
   `README.md`, `PROJECT_STATUS.md`, `wearos-app/README.md` (were "63" / "91+").
5. ✅ `tools/run_demo.sh` (`ConsoleDemo`) walks the full journey and ends
   "Dashboard asleep. Demo complete."

Also done here (pulled forward from 0c / Testing Strategy #2 & #4):
- `core/build.gradle.kts`: deprecated `tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget }`
  → `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }`; added
  `tasks.register<JavaExec>("runCoreTests")` (+ `check` depends on it). **Unverified** — no
  Gradle-compatible JDK on this machine yet (needs 0a/0c); the `kotlinc` path is what's
  proven.
- `.github/workflows/build.yml`: `core` job (`:core:runCoreTests` on Temurin 21) + `android`
  job (`assembleDebug` + `lint`). The `android` job is **expected red until 0c** — left in
  deliberately as the reminder.

### 0c. First Gradle sync + version realignment — **the explicit early task**

Open repo root in Android Studio → `Sync Project with Gradle Files`. Expect failures. Fix:

- **`gradle/wrapper/gradle-wrapper.properties`:** distribution → the version Studio 2026.1.3
  bundles/recommends. Gradle 9.3.0 as-is is unsupported by AGP 8.9.1.
- **root `build.gradle.kts`:**
  - `com.android.application` → AGP version Studio recommends (expect ~8.13+ or 9.x). Must
    support the Gradle JDK.
  - `kotlin("android")` / `kotlin("jvm")` / `org.jetbrains.kotlin.plugin.compose` → one
    Kotlin version (≥ 2.1.20; match the toolchain, e.g. 2.2.x). All three identical.
- **`core/build.gradle.kts`:** replace deprecated
  `tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "17" }` with
  `compilerOptions { jvmTarget = JvmTarget.JVM_17 }`. Keep `java { …VERSION_17 }`. Add the
  `runCoreTests` task (see Testing Strategy).
- **`wearos-app/build.gradle.kts`** — every version is a guess; resolve all against real
  repos:
  - `compileSdk` / `targetSdk` `36`: `android-36` is installed; keep 36 only if the chosen
    AGP supports it as stable, else drop to 35 (also installed). **Not `37` (preview).**
  - `platform("androidx.compose:compose-bom:2026.05.00")` → real latest stable BOM (the
    date-coordinate almost certainly won't resolve).
  - Drop explicit `androidx.compose.ui:ui-tooling*:1.11.3` versions — let the BOM govern.
  - `androidx.activity:activity-compose:1.13.0` → real latest (~1.9–1.10).
  - `androidx.wear.compose:compose-material3:1.6.2` / `compose-foundation:1.6.2` /
    `compose-ui-tooling:1.6.2` → real latest stable Wear Compose (verify each coordinate;
    Wear `compose-material3` is a distinct, lower version line than mobile material3).
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.9.0` → real latest.
  - `com.google.android.gms:play-services-wearable:20.0.0` → real latest (likely 18.x–19.x;
    `20.0.0` may not exist).
  - `kotlinOptions` → `compilerOptions`.
  - Add a `release` buildType (Phase 5) — even a stub, so `assembleRelease` is exercised.
- **`phone-app/build.gradle.kts`:** same `compileSdk`/`targetSdk` decision, same
  `play-services-wearable` bump, `androidx.core:core-ktx:1.13.1` → latest, `kotlinOptions` →
  `compilerOptions`.
- **Manifests:** lint will flag missing `android:icon` on both `<application>` tags
  (Phase 5 fixes properly; a placeholder `@mipmap/ic_launcher` unblocks). `wearos-app` may
  need `<uses-feature android:name="android.hardware.type.watch" android:required="true">`.

**Deliverable:** green
`./gradlew :core:compileKotlin :wearos-app:assembleDebug :phone-app:assembleDebug`. **This
is the build-verification checkpoint.** Commit the resolved versions.

### 0d. Standalone Wear emulator + `TESTING.md` walkthrough

- Use `Wear_OS_Large_Round` (or recreate on a stable API — 34/35 — not 37 preview;
  decision #5).
- `./gradlew :wearos-app:installDebug` (or Studio Run), walk all 14 rows of `TESTING.md` §3.
- Expected first-run fixes: Wear Compose API drift (`Button`, `IconButton`, `Text`,
  `HorizontalPageIndicator` signatures), `androidx.compose.foundation.pager.HorizontalPager`
  vs Wear's own pager, `String.format` locale lint in `CarScreen`.
- Any behavioral mismatch vs the table maps to an existing `core` test
  (`EndToEndJourneyTests`, `ConnectionManagerTests`) — start there.

### 0e. Paired-phone Data Layer nav pipe

Fix the read-found bugs **before** testing:
- **`WearDataLayerBluetoothProvider.connect()`** — move off the main thread (use the async
  `addOnSuccessListener` pattern already in `send()`, or a background executor). Never
  `Tasks.await` on the caller's thread.
- Add `CapabilityClient` / `MessageClient.OnMessageReceivedListener` + node-presence
  listener → real link state feeding `ConnectionManager`.
- Make `NavDataListenerService` resilient to a null singleton (buffer to a static queue
  `getInstance` drains, or start the provider from `Application.onCreate`).
- Resolve target node via `CapabilityClient`; declare a `wear.xml` capability each side.
- Verify cross-package `MessageClient` delivery `com.dashboard.phoneapp` →
  `com.dashboard.wearos` (bug #4 / decision-ish). If it fails: align applicationIds, add
  the capability declaration, or (worst case) same signing config.
- Pair `Pixel_10` with the Wear AVD, install both apps, grant accessibility on the phone,
  start Google Maps navigation, follow `phone-app/README.md` §"How to test", watch both
  logcats.
- Add `.github/workflows/build.yml`:
  `./gradlew :core:runCoreTests :wearos-app:assembleDebug :phone-app:assembleDebug lint` on
  every push. **Highest-value process fix** — fails immediately on cross-session
  package/method drift.

**Exit criteria:** all three modules compile; `core` tests green with a known count;
`wearos-app` passes the full `TESTING.md` checklist standalone; one real nav checkpoint
travels phone→watch; CI build gate running.

---

# Phase 1 — Music (real now-playing + controls + waveform)

Most achievable per the research doc. `core`'s `MediaManager` / `MediaState` /
`MediaCommand` need **zero changes** (confirmed).

### New files — `phone-app`

- `src/main/kotlin/com/dashboard/phoneapp/media/MediaNotificationListenerService.kt` —
  `NotificationListenerService`. `onListenerConnected()`: `MediaSessionManager`,
  `getActiveSessions(ComponentName(this, …))`, register
  `OnActiveSessionsChangedListener`. Owns the tracked `MediaController` + a
  `MediaController.Callback`.
- `src/main/kotlin/com/dashboard/phoneapp/media/MediaSessionReader.kt` — from the live
  `List<MediaController>`, picks the active one (delegates the choice to a pure `core`
  helper — below), reads `PlaybackState` + `MediaMetadata`, converts to
  `com.dashboard.core.domain.MediaState`, encodes with
  `MessageCodec.encode(mediaState.toProtocol())`, sends via `WearMessageSender`.
- `src/main/kotlin/com/dashboard/phoneapp/WearInboundListenerService.kt` —
  `WearableListenerService`, manifest-registered for `MESSAGE_RECEIVED` on
  `pathPrefix="/automotive-dashboard"`. Decodes with `MessageCodec.decode`; on
  `ProtocolMessage.MediaCommandMessage` →
  `mediaController.transportControls.play()/pause()/skipToNext()/skipToPrevious()`. (The
  phone-side counterpart the repo currently lacks — watch already `send()`s these bytes.)

### Changed files — `phone-app`

- `WearMessageSender.kt` — add `fun send(context, data: ByteArray)` (path unchanged;
  `MessageCodec` type tag demuxes on the watch). Keep `sendNavUpdate` as a thin delegate
  (explicitly preserve the name — past conflict point). Switch to `CapabilityClient` node
  resolution.
- `MainActivity.kt` — "Grant Notification Access" button →
  `Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` (fallback
  `ACTION_NOTIFICATION_LISTENER_SETTINGS`); show granted status for both accessibility and
  notification access.
- `src/main/AndroidManifest.xml` — add the `MediaNotificationListenerService` (with
  `BIND_NOTIFICATION_LISTENER_SERVICE` + the notification-listener intent-filter) and
  `WearInboundListenerService` (with the `MESSAGE_RECEIVED` + `wear` scheme
  `pathPrefix="/automotive-dashboard"` filter). No new `<uses-permission>` (bind permission
  is on the `<service>`; `MEDIA_CONTENT_CONTROL` is signature-level — do not use).

### New pure code — `core` (unit-testable, no device)

- `core/src/main/kotlin/com/dashboard/core/communication/MediaSessionSelection.kt` — pure
  `fun chooseActive(candidates: List<SessionSnapshot>): SessionSnapshot?` where
  `SessionSnapshot` is a tiny `core` data class (`packageName`, `playbackState`,
  `lastActiveMillis`, position/duration/metadata). Rule: prefer `PLAYING`, then most
  recently active. Android service maps its `MediaController`s ↔ `SessionSnapshot`s.
- Optionally `core/.../PlaybackStateMapping.kt` — pure `Int → PlaybackState`.
- `core/src/test/kotlin/com/dashboard/core/tests/MediaSessionSelectionTests.kt` — add to
  `AllTests.kt`: no sessions, one paused, two playing (most-recent wins), playing beats
  paused.

### Wiring — `wearos-app/MainActivity.kt`

- `MediaManager(mockPhoneCommunication)` → `MediaManager(navPhoneCommunication)` (the real
  `BluetoothPhoneCommunication` over Data Layer). After this, `MockPhoneCommunication` is
  used only by `BlizzerManager` (until Phase 4) and `DevControlPanel`.
- Doc note (mirroring `phone-app/README.md`'s nav note): dev-control music buttons now feed
  only the mock.

### UI — `wearos-app/ui/MusicScreen.kt`

- Decorative waveform: a `Canvas` of bars / a sine, animated by `mediaState.positionMillis`.
  Since updates are discrete, drive smooth motion with a local `LaunchedEffect` ticker gated
  on `playbackState == PLAYING` (same idea as `NavigationManager.onVehicleSpeedTick`). Pure
  decoration — no FFT, no audio.
- Position/duration progress indicator.

### Testable now vs needs a device

| Testable now (pure `core` / JVM) | Needs a device |
|---|---|
| `chooseActive` session selection | `NotificationListenerService` grant + real `getActiveSessions` |
| `PlaybackState` int→enum mapping | Real Spotify / YT Music / podcast metadata fidelity |
| `MediaState ↔ ProtocolMessage` round-trip (existing) | Cross-package `MessageClient` command delivery watch→phone |
| `MediaCommand → transportControls` mapping table | `transportControls` actually affecting the third-party app |
| Waveform math | Waveform rendering on the round bezel |

**Migration:** `MockPhoneCommunication` stays for `DevControlPanel` + `ConsoleDemo`
(dev-only). One line changes in `MainActivity`.

**Risks:** apps that don't publish a proper `MediaSession` (rare); wrong session when
several active (pure selector + tests); OS revoking notification access after updates;
cross-package messaging (shared with Phase 0e).

---

# Phase 2 — Maps (verify the biggest assumption)

Mostly **verification + parser hardening** — the transport already exists.

### Pre-work decision (blocks the phase) — capture mechanism · **decision #4**

`NavigationAccessibilityService` reads `rootInActiveWindow`, which only reflects the
**foreground** window. In a car, Maps navigates **backgrounded** → `rootInActiveWindow` is
the launcher and **nothing is captured**. Largest risk in the whole Maps strategy; the
research doc's "❓ needs real-device verification" understates it.

**Recommended pivot:** capture Maps' **ongoing navigation notification** via
`NotificationListenerService` (the Phase 1 service), reading `Notification.extras`
(`EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_SUB_TEXT`) from `com.google.android.apps.maps`.
Ongoing notifications **do** update while backgrounded. Feed that into the existing
`NavigationAnnouncementParser` unchanged. Reuses Phase 1 infra, removes the
AccessibilityService + its Play Console policy exposure.

The spike should test **both** on a real device and pick from data. Keep the
AccessibilityService only if notification text proves insufficient.

### The real-device spike (needs a phone, ideally not just an emulator)

1. Build/install `phone-app`, grant accessibility **and** notification access.
2. Start real Google Maps navigation. Capture from logcat:
   - `NavAccessibilityService` "Screen text from…" blobs (foreground).
   - The Maps ongoing-notification `extras` dump (add temp logging in the notification
     listener).
   - Repeat with Maps backgrounded / screen off.
   - Repeat per target language (EN + Croatian confirmed in parser; add others as needed).
   - Repeat with Waze if in scope (decision #3).
3. Save captured strings as test fixtures.

### Changed files

- `core/.../communication/NavigationAnnouncementParser.kt` — extend `directionKeywords`,
  `distancePattern`, `liveBannerFragmentPattern`, `ontoRoadPattern` to match the **real**
  captured text (only observed phrases — the file's own doctrine). Likely: notification
  `EXTRA_SUB_TEXT` = distance, `EXTRA_TEXT` = instruction, `EXTRA_TITLE` = ETA — a cleaner
  structured 3-field input than the pipe-blob; may justify a second entry point
  `parseNotification(title, text, subText): Checkpoint?` alongside `parse(blob)`.
- `core/.../tests/NavigationAnnouncementParserTests.kt` — add real-fixture cases (the 9
  existing stay).
- `phone-app/.../media/MediaNotificationListenerService.kt` (or a renamed shared
  `WearNotificationListenerService`) — add a branch for
  `packageName == "com.google.android.apps.maps"` → parse →
  `MessageCodec.encode(navState.toProtocol())` → `WearMessageSender.send`. If keeping
  accessibility, `NavigationAccessibilityService.kt` is unchanged.
- `phone-app/src/main/res/xml/accessibility_service_config.xml` — if accessibility is
  dropped: delete the service + config + manifest entry + `MainActivity` accessibility
  button. If kept: correct the `docs/android-integration-research.md`
  `canRetrieveWindowContent` claim.

### Wiring

Already done — `MainActivity.kt` routes `NavigationManager` through `navPhoneCommunication`.
`onVehicleSpeedTick` interpolation is fed from `vehicleManager.observe` (real once Phase 3
lands; mock speed until then is fine — interpolation still demonstrates).

### Testable now vs needs a device

| Testable now | Needs a device |
|---|---|
| Parser against captured fixtures (EN/HR/…) | Whether Maps/Waze emit usable text at all, fg **and** bg |
| `NavigationState ↔ ProtocolMessage` round-trip | Notification `extras` key stability across Maps versions |
| `NavigationManager` countdown interpolation (existing) | End-to-end latency phone→watch during real driving |

**Migration:** dev-control "Announce…" buttons keep feeding `MockPhoneCommunication`
(already documented). Real Maps drives the panel via the transport.

**Risks:** (1) Maps emits nothing useful backgrounded → the notification-capture pivot is
the mitigation, built into the spike. (2) Notification `extras` schema changes between Maps
releases → fixture tests catch regressions; keep the parser tolerant. (3) Play policy on
AccessibilityService for non-a11y use → dropping it removes the problem; if kept, the
existing disclosure `MainActivity` + narrow `packageNames` filter is the mitigation,
re-check policy before shipping. (4) Waze bg behavior differs → scope decision #3.

---

# Phase 3 — Car panel (watch-side BLE OBD-II, direct to vehicle)

Decision locked: watch connects directly to the vehicle's BLE ELM327-style adapter, not
phone-relayed. Implements `VehicleDataProvider` (returns `VehicleData` directly — does
**not** touch `BluetoothProvider`).

### Blocking decisions

- **#6** — specific OBD adapter model (GATT UUIDs and AT-command quirks vary widely across
  ELM327 BLE clones).
- **#7** — whether a Wear OS watch can hold a GATT connection to the dongle **while** paired
  to the phone (single-radio concurrency). May force reverting to phone-relay.

### New pure code — `core` (fully unit-testable, no device) — **doable now**

- `core/src/main/kotlin/com/dashboard/core/vehicle/ObdPidParser.kt`:
  - `fun parseFrame(ascii: String): ObdReading?` — tokenizes ELM327 ASCII
    (`"41 0C 1A F8"`, multiline, stray spaces, `>` prompt, `SEARCHING...`, `NO DATA`,
    `CAN ERROR`, `?`), returns mode+PID+data bytes or null.
  - Per-PID pure decoders: `rpm(a,b)=((a*256)+b)/4`, `speedKmh(a)=a`, `coolantC(a)=a-40`,
    `engineLoadPct(a)=a*100/255`, `fuelPct(a)=a*100/255`, `intakeTempC(a)=a-40`,
    `controlModuleVoltage(a,b)=((a*256)+b)/1000.0`, oil temp `0x5C` `a-40`, etc.
  - `fun toVehicleData(readings: Map<Pid, ObdReading>, nowMillis): VehicleData` —
    `Signal.Available` for present PIDs, `Signal.Unavailable` for absent (never fabricates
    — the `Signal` doctrine).
  - `GearPosition` is not an OBD-II PID → leave `gear` as `Signal.Unavailable` unless a
    vehicle-specific extended PID exists. Document this.
- `core/src/test/kotlin/com/dashboard/core/tests/ObdPidParserTests.kt` — add to
  `AllTests.kt`: valid multi-PID frame, `NO DATA`, partial frame, `SEARCHING...`, unit
  conversions, unavailable-PID → `Signal.Unavailable`.

The **only** unit-testable-without-hardware part of Phase 3, and the highest-value part
(wrong byte math = the most likely silent bug).

### New files — `wearos-app`

- `src/main/kotlin/com/dashboard/wearos/hardware/vehicle/BleObdVehicleDataProvider.kt` —
  implements `VehicleDataProvider`:
  - `start()`: `BluetoothAdapter` / `BluetoothLeScanner`, scan by name / configured MAC,
    `connectGatt`, `discoverServices`, locate UART-style write/notify characteristics, init
    (`ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATSP0`), then a ~2–4 Hz polling loop issuing PID
    requests (`010C`, `010D`, `0105`, `0104`, `012F`, `015C`, `0142`, …), feeding responses
    through `core`'s `ObdPidParser`, emitting `VehicleData` via an `Emitter`.
  - `stop()`: cancel polling, `gatt.disconnect()`, `gatt.close()`, release scanner.
  - Reconnect with backoff; expose internal connection state for diagnostics.
- `src/main/kotlin/com/dashboard/wearos/hardware/vehicle/ObdGattProfile.kt` — the
  adapter-specific UUIDs + init sequence, isolated so a different dongle is a one-file
  change.
- `src/main/kotlin/com/dashboard/wearos/hardware/vehicle/VehicleProviderFactory.kt` —
  `MockVehicleDataProvider` for `BuildConfig.DEBUG` (keeps the dev "Drive: 60 km/h" control
  and `TESTING.md` working), `BleObdVehicleDataProvider` otherwise. Or gate by a settings
  toggle.

### Changed files — `wearos-app`

- `MainActivity.kt` — `val vehicleProvider = VehicleProviderFactory.create(this)`.
  `VehicleDataManager(vehicleProvider)` unchanged. `DevControlPanel` currently requires a
  concrete `MockVehicleDataProvider` for `setTargetSpeedKmh` → make that param nullable, or
  only construct `DevControlPanel` in debug builds. Auto-connect already wired
  (`PowerManager.onActive { vehicleManager.start() }` → `provider.start()`; `onSleep` →
  `stop()`). **No `PowerManager` change.**
- `src/main/AndroidManifest.xml` — add `<uses-feature bluetooth_le required>`,
  `BLUETOOTH_SCAN` (`neverForLocation`), `BLUETOOTH_CONNECT`, and the `maxSdkVersion="30"`
  legacy `BLUETOOTH` / `BLUETOOTH_ADMIN` / `ACCESS_FINE_LOCATION` trio.
- `MainActivity.kt` — runtime permission flow for `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`
  before `start()`.

### Testable now vs needs hardware

| Testable now | Needs hardware |
|---|---|
| `ObdPidParser` frame tokenizing + every PID conversion | BLE scan/connect/GATT discovery against a real dongle |
| `toVehicleData` unavailable-PID handling | AT init sequence quirks of the specific adapter |
| `VehicleDataManager` caching (existing) | Watch↔dongle + watch↔phone radio concurrency |
| Factory selection logic | Polling rate vs adapter throughput; reconnect behavior |

Bench alternative to a car: an ELM327 emulator (OBDSim + BLE bridge, or a hardware ECU
simulator).

**Migration:** `MockVehicleDataProvider` stays the debug-build + `ConsoleDemo` provider.
Real build uses `BleObdVehicleDataProvider`. Re-run `TESTING.md` in debug (unchanged) + a
new BLE-specific checklist.

**Risks:** radio concurrency (could invalidate the "direct" decision → fallback: phone
reads OBD via classic BT SPP and relays over Data Layer, which *would* reshape things);
adapter variance (isolated in `ObdGattProfile`); ELM327 clone firmware bugs; Wear OS
background BLE limits when screen sleeps (Car needs data only while `ACTIVE` — acceptable).

---

# Phase 4 — Blizzer real feed (phone GPS + speed-camera POI dataset)

Watch/core side is done (`BlizzerManager` auto-dismiss, `BlizzerSeverity`,
`BlizzerOverlay`). This phase builds the phone-side data source.

### Blocking decision — **#1**

POI dataset source + licensing. Options: OpenStreetMap (`highway=speed_camera` /
`enforcement=*`, ODbL + attribution), community DBs (license varies, often non-commercial),
a paid POI provider. Region coverage (Croatia + EU?). Gates the phase.

### New pure code — `core` (fully unit-testable) — **doable now**

- `core/src/main/kotlin/com/dashboard/core/blizzer/CameraProximity.kt`:
  - `data class CameraPoi(id, lat, lon, kind, roadName?)`.
  - `fun haversineMeters(lat1, lon1, lat2, lon2): Double`.
  - `fun nearest(pos, cameras: List<CameraPoi>, maxMeters): Pair<CameraPoi, Double>?` — with
    a lat/lon bounding-box prefilter.
  - `fun crossedThreshold(prevMeters: Double?, nowMeters: Double, thresholds: IntArray = intArrayOf(2000,1000,500,200,100)): Int?`
    — returns the newly-entered threshold or null; hysteresis so GPS jitter around a
    boundary doesn't re-fire.
  - `fun toBlizzerEvent(poi, threshold, eventId): BlizzerEvent` (`distanceMeters=threshold`,
    `active=true`); `fun clearedEvent(eventId): BlizzerEvent` (`active=false`).
- `core/src/test/kotlin/com/dashboard/core/tests/CameraProximityTests.kt` — add to
  `AllTests.kt`: haversine vs known pairs, bounding-box correctness, threshold entry/exit,
  hysteresis, "no camera in range" → null, sequence 2000→1000→500→200→100 fires once each.

### New files — `phone-app`

- `src/main/kotlin/com/dashboard/phoneapp/blizzer/SpeedCameraRepository.kt` — loads the POI
  dataset (bundled asset GeoJSON/CSV for v1, later downloadable + cached), exposes
  `camerasNear(pos): List<CameraPoi>`.
- `src/main/kotlin/com/dashboard/phoneapp/blizzer/CameraProximityService.kt` — foreground
  `Service`. `FusedLocationProviderClient` ~1 Hz; each fix → `CameraProximity.nearest` →
  `crossedThreshold` → on a crossing, `MessageCodec.encode(event.toProtocol())` →
  `WearMessageSender.send`; on leaving range, send the `active=false` event. Shows the
  required FGS notification.
- `src/main/kotlin/com/dashboard/phoneapp/blizzer/BlizzerBootReceiver.kt` (optional) —
  restart after reboot / on app open.

### Changed files — `phone-app`

- `MainActivity.kt` — button to grant location (foreground + background) and start/stop the
  service; status display.
- `AndroidManifest.xml` — `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`,
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`; the
  `CameraProximityService` with `foregroundServiceType="location"`.
- `build.gradle.kts` — add `com.google.android.gms:play-services-location:<v>`.

### Wiring — `wearos-app/MainActivity.kt`

- `BlizzerManager(mockPhoneCommunication)` → `BlizzerManager(navPhoneCommunication)`. After
  this, `MockPhoneCommunication` is referenced **only** by `DevControlPanel` → can move to
  `src/debug` in Phase 5.

### Reconcile the two color systems — **decision #9**

If 5-tier (+amber): change `BlizzerOverlay.kt` to use `BlizzerProximity.colorArgbFor` /
`blinkPeriodMillisFor`, delete `BlizzerSeverity`/`BlizzerSeverityMapper` (or vice-versa).
Update the corresponding tests. Update `PROJECT_STATUS.md` Phase A to match reality.

### Blizzer sound — **decision #8**

`BlizzerAudioManager` exists, is tested, deliberately **not** `start()`ed ("no sound" by
omission). Real speed-camera apps beep. If the user wants sound: `blizzerAudioManager.start()`
+ a real `AudioOutput` — but that needs a real audio route (BT A2DP to the car stereo),
currently unimplemented anywhere. Keep silent this phase unless the user says otherwise.

### Testable now vs needs a device

| Testable now | Needs a device |
|---|---|
| All of `CameraProximity` (haversine, nearest, thresholds, hysteresis) | `FusedLocationProviderClient` accuracy/rate |
| `BlizzerEvent ↔ ProtocolMessage` round-trip | Background location permission behavior while driving |
| `BlizzerManager` auto-dismiss (existing) | POI dataset positional accuracy vs real cameras |
| Dataset parser (asset → `List<CameraPoi>`) | FGS survival over a long drive |

**Migration:** `DevControlPanel.triggerCameraWarning` keeps feeding `MockPhoneCommunication`
for the emulator checklist. Real alerts come from `CameraProximityService` → Data Layer →
`BluetoothPhoneCommunication` → `BlizzerManager`.

**Risks:** dataset licensing for a sold product (decision #2); dataset staleness (cameras
move); background location = a heavy Play Store review; battery drain from 1 Hz GPS + FGS;
GPS jitter causing flapping (mitigated by hysteresis in `crossedThreshold`).

---

# Phase 5 — Persistence & polish

### Real `SettingsStore` via DataStore

- `wearos-app/src/main/kotlin/com/dashboard/wearos/hardware/DataStoreSettingsStore.kt` —
  implements `SettingsStore`:
  - `androidx.datastore:datastore-preferences` (add to `wearos-app/build.gradle.kts`).
  - `load()`: `runBlocking { dataStore.data.first() }` — one sync read at startup is
    acceptable; map preference keys → `DashboardSettings`.
  - `save(settings)`: fire-and-forget `appScope.launch(Dispatchers.IO) { dataStore.edit { … } }`.
  - Keep an in-memory mirror so `load()` after the first call is instant.
- `core/src/main/kotlin/com/dashboard/core/domain/SettingsCodec.kt` (optional, pure) —
  `DashboardSettings ↔ String`, testable in `core`, reusable by a future `SettingsUpdate`
  transport (the `ProtocolMessage.SettingsUpdate` placeholder exists).
- `core/src/test/kotlin/com/dashboard/core/tests/SettingsCodecTests.kt` — round-trip,
  unknown-field tolerance, default fallback.
- Wiring: `MainActivity.kt` → `val settingsStore = DataStoreSettingsStore(this)`.
  `SettingsManager` **unchanged**. `InMemorySettingsStore` stays for `ConsoleDemo` / tests.

### Real gestures / navigation polish

`DashboardApp.kt` already uses a real `HorizontalPager` (README's "tap/long-press
placeholders" note is stale for panel switching). Polish:
- `androidx.compose.foundation.pager.HorizontalPager` →
  `androidx.wear.compose.foundation.pager.HorizontalPager` (curved edges, rotary support).
- Hand-rolled `PageIndicator` dots → Wear material3 `HorizontalPageIndicator`.
- Rotary input (`Modifier.rotaryScrollable` / `rememberActiveFocusRequester`) for scrolling
  `CarScreen` fields and `DevControlsScreen`.
- `DevControlsScreen` `LazyColumn` → `ScalingLazyColumn` + swipe-to-dismiss to close.
- `CarScreen` / `MapsScreen` / `MusicScreen` — proper Wear `ScreenScaffold`/`TimeText`.

### Launcher icon

- `wearos-app/src/main/res/mipmap-*` + `phone-app/src/main/res/mipmap-*` via Studio Image
  Asset (adaptive + round for Wear).
- `android:icon="@mipmap/ic_launcher"` / `android:roundIcon` on both `<application>` tags.

### Release build — dev controls disappear

- `wearos-app/build.gradle.kts` — `buildTypes { release { isMinifyEnabled = true;
  proguardFiles(...); signingConfig = ... } }`. `core` uses no reflection → R8 keep rules
  minimal.
- **Move dev-only code to `wearos-app/src/debug/kotlin/`** so it isn't compiled into
  release: `ui/DevControlsScreen.kt`, and the `DevControlPanel` construction +
  `MockPhoneCommunication` / `MockVehicleDataProvider` usage in `MainActivity`.
  `DashboardApp` already guards the `⚙` entry with `if (BuildConfig.DEBUG)`; a
  `src/debug`-only reference is cleaner. (`DevControlPanel` itself can stay in `core` — just
  don't wire it in release.)
- Verify: `./gradlew :wearos-app:assembleRelease`, install, confirm no `⚙`, confirm
  `DevControlsScreen` unreferenced, run the `TESTING.md` non-dev rows.
- `phone-app` — same `release` buildType + signing.

### Testable now vs needs a device

| Testable now | Needs a device |
|---|---|
| `SettingsCodec` round-trip | `DataStoreSettingsStore` persistence across launches |
| `SettingsManager` (existing) | Rotary input, swipe-to-dismiss feel |
| — | Icon rendering in the Wear launcher |
| — | Release APK: `⚙` absent, R8 didn't strip something needed |

---

## Testing strategy (cross-cutting)

1. **Keep the hand-rolled harness green on every `core` change.** `tools/run_tests.sh` with
   the Studio-bundled kotlinc on PATH. New pure suites added to `AllTests.kt`:
   - Phase 1: `MediaSessionSelectionTests`
   - Phase 2: expanded `NavigationAnnouncementParserTests` (real fixtures)
   - Phase 3: `ObdPidParserTests`
   - Phase 4: `CameraProximityTests`
   - Phase 5: `SettingsCodecTests`
2. **Add a Gradle path to run the harness** (so CI / other machines don't need kotlinc): in
   `core/build.gradle.kts`,
   `tasks.register<JavaExec>("runCoreTests") { mainClass = "com.dashboard.core.tests.AllTestsKt"; classpath = sourceSets["test"].runtimeClasspath }`.
   Zero new deps. CI runs `./gradlew :core:runCoreTests`.
3. **Mechanical JUnit5 migration** (once first Gradle sync gives Maven access): add
   `testImplementation("org.jetbrains.kotlin:kotlin-test")` + `junit-jupiter`,
   `tasks.test { useJUnitPlatform() }`. Convert each of the 19 suites into a class of
   `@Test fun`s — `assertEquals`/`assertTrue` signatures already match `kotlin-test`, bodies
   unchanged. Delete `TestHarness.kt` + `AllTests.kt` + `runCoreTests` after. Do it in one
   pass right after Phase 0c, before the phases add more suites.
4. **CI build gate** (`.github/workflows/build.yml`):
   `./gradlew :core:runCoreTests :wearos-app:assembleDebug :phone-app:assembleDebug lint`
   on every push. This is what would have caught the past
   `WearDataLayerBluetoothProvider` / `sendNavUpdate` conflicts.
5. **Instrumented / device tests** (new `androidTest`, lower priority): Compose
   state-rendering smoke tests per screen; `DataStoreSettingsStore` persistence; a Data
   Layer round-trip test. The `TESTING.md` manual checklist remains primary end-to-end.
6. **Per-phase manual checklists**: extend `TESTING.md` with a section per real integration
   (Music grant flow, Maps real-nav capture, BLE OBD connect, Blizzer live approach), each
   mirroring an automated `core` test where one exists.

---

## Phase ordering & dependencies

```
Phase 0  ─── everything depends on this (compile + green tests + emulator + CI gate)
  │
  ├── JUnit5 migration (optional, best done here before more suites pile up)
  │
  ├── Phase 1  Music        (independent; lowest risk; builds the NotificationListenerService)
  │      │
  │      └──> Phase 2  Maps  (reuses Phase 1's NotificationListenerService if the pivot is
  │                            chosen; sequence after Music regardless — the pivot decision
  │                            wants Phase 1's service to exist)
  │
  ├── Phase 3  Car / BLE OBD (independent of 1/2/4; gated on hardware decisions;
  │                            ObdPidParser buildable + testable now, wiring waits for a dongle)
  │
  └── Phase 4  Blizzer feed  (independent; gated on POI dataset decision;
                               CameraProximity buildable + testable now)

Phase 5  Persistence & polish  ─── after ≥1 real panel exists; the release-build check runs
                                   last, after 1/3/4 have moved off their mocks
```

- Phases 1–4 touch **different files** and different mock/provider slots in
  `wearos-app/MainActivity.kt` — parallelizable with low merge risk **provided** the Phase
  0e CI gate is running.
- The pure `core` additions (`ObdPidParser`, `CameraProximity`, `MediaSessionSelection`,
  `SettingsCodec`, parser fixtures) are all doable **now, headless** — no device, no Gradle.
  Front-load them while Android-Studio-dependent steps are scheduled.

### Doable now (headless CLI) vs Android Studio vs physical device

| Doable now (this machine, headless) | Needs Android Studio | Needs a physical device / hardware |
|---|---|---|
| Remove `core/bin` from git | First Gradle sync + version realignment (0c) | Cross-package `MessageClient` delivery (2 emulators OK) |
| Run `tools/run_tests.sh`; fix `core` under Kotlin 2.3 | Build `wearos-app` / `phone-app` | Real Maps/Waze announcement capture (Phase 2 spike) |
| Write + test `ObdPidParser`, `CameraProximity`, `MediaSessionSelection`, `SettingsCodec` | Wear emulator + `TESTING.md` walkthrough (0d) | BLE OBD-II dongle (Phase 3 wiring) |
| Extend `NavigationAnnouncementParser` + fixtures | Paired-phone nav pipe (0e) | GPS while driving + POI accuracy (Phase 4) |
| JUnit5 migration of the harness | Fix the `Tasks.await` / link-state / null-singleton bugs & verify | Real media apps' `MediaSession` fidelity (Phase 1) |
| Add `runCoreTests` task, CI workflow YAML | All UI polish, DataStore, release build | Watch↔dongle + watch↔phone radio concurrency (Phase 3 risk) |

---

## Risk register

| # | Risk | Phase | Likelihood | Mitigation |
|---|---|---|---|---|
| R1 | Version stack (Gradle 9.3 / AGP 8.9.1 / Kotlin 2.1.10 / JBR 25 / date-versioned Compose BOM) won't sync | 0c | Certain | Realign all versions on first Studio sync; the explicit first task |
| R2 | Google Maps emits no usable turn text while backgrounded (accessibility sees only foreground) | 2 | High | Pivot to `NotificationListenerService` on Maps' ongoing notification; spike tests both |
| R3 | Wear OS watch can't hold GATT to the dongle while paired to the phone | 3 | Medium–High | Verify early on target hardware; fallback = phone reads OBD (classic BT SPP) & relays over Data Layer |
| R4 | Cross-package (`com.dashboard.wearos` ↔ `com.dashboard.phoneapp`) Data Layer messaging fails | 0e | Medium | `CapabilityClient` + capability declarations; align applicationId or signing if needed |
| R5 | `WearDataLayerBluetoothProvider.connect()` `Tasks.await` on main thread crashes on first NFC tap | 0e | Certain (as written) | Rewrite async before testing; add link-state listener |
| R6 | POI dataset licensing incompatible with a sold product | 4 | Medium | Resolve decisions #1/#2 before Phase 4; OSM (ODbL + attribution) as the safe default |
| R7 | ELM327 BLE clone firmware quirks / non-standard UUIDs | 3 | Medium | Isolate in `ObdGattProfile.kt`; get the specific adapter before wiring; bench-test with an emulator app |
| R8 | Play Store review: AccessibilityService + NotificationListener + background location = heavy scrutiny | 2, 4 | High if published | Decide product vs personal (#2); the R2 pivot removes one; strong in-app disclosures |
| R9 | Cross-session code drift reintroduces package/method mismatches | all | Medium | CI build gate (0e) compiling both app modules every push; keep `PROJECT_STATUS.md` + this file updated per phase |
| R10 | ~~`core` test count unknown; latent failure under newer Kotlin~~ | 0b | — | ✅ Retired 2026-09-07 — 19 suites / 98 assertions all green on kotlinc 2.3.10, no code changes needed |
| R11 | Two Blizzer color systems diverge further | 4 | Low | Reconcile to one in Phase 4 (decision #9) |
| R12 | DataStore async vs synchronous `SettingsStore` interface | 5 | Low | `runBlocking` for the single startup read + async writes inside the impl; interface unchanged unless that fails |
| R13 | Wear emulator on API 37 (preview) behaves inconsistently | 0d | Low | Recreate the AVD on a stable API (34/35) per decision #5 |
| R14 | `NavDataListenerService` drops messages when the app process is cold-started by the system | 0e | Medium | Start the provider from `Application.onCreate`; buffer in the service |

---

## Open questions / decisions the user must make

Numbered for reference from the phases above.

1. **Speed-camera POI dataset** — OpenStreetMap (`highway=speed_camera`, ODbL + attribution),
   a community DB (license often non-commercial), or a paid provider? Which regions
   (Croatia + EU)? *Blocks Phase 4.*
2. **Sold product or personal/sideloaded build?** — determines Play Store policy exposure
   (accessibility, notification listener, background location), dataset licensing, nav-SDK
   cost calculus, release/signing/R8 rigor. *Affects Phases 2, 4, 5.*
3. **Waze in scope, or Google Maps only?** — affects the parser, the `packageNames` filter,
   the background-capture strategy. *Affects Phase 2.*
4. **Maps capture mechanism** — accept the pivot to `NotificationListenerService` on Maps'
   ongoing notification (works backgrounded, reuses Phase 1, drops the AccessibilityService
   + its policy burden), or keep the AccessibilityService (foreground-only, likely captures
   nothing in a real car)? *Blocks Phase 2.*
5. **Target watch hardware + OS level** — specific Wear OS device model? Sets `minSdk`
   (currently 30), the emulator API (current AVD is 37-preview; recommend stable Wear OS
   4/5 = API 34/35), screen-shape assumptions, whether the watch has an NFC reader (#12),
   whether it can do concurrent BLE (#7).
6. **Which OBD-II BLE adapter** (make/model)? GATT UUIDs + AT quirks needed to finalize
   `BleObdVehicleDataProvider` / `ObdGattProfile`. *Blocks Phase 3 device wiring (not the
   parser).*
7. **Can the target watch maintain a direct BLE GATT link to the OBD dongle while staying
   paired to the phone?** If not, the "watch connects directly to the vehicle" locked
   decision must change to phone-relay. *Blocks Phase 3 wiring.*
8. **Blizzer: audible alert or silent?** Currently silent by omission. Real Blitzer-style
   apps beep. Turning it on needs `BlizzerAudioManager.start()` **and** a real audio route
   (#11).
9. **Blizzer color scheme: 3-tier (blue/green/red) or 5-tier (+amber)?** Reconciles
   `BlizzerSeverityMapper` (in use) vs `BlizzerProximity` (unused). *Affects Phase 4
   cleanup.*
10. **Audio output path** — turn-by-turn cues and any Blizzer beeps need a real
    `AudioOutput` impl (BT A2DP to the car stereo, or AUX). Nothing implements `AudioOutput`
    today. In scope now, later, or never?
11. **NFC "tap to connect"** — does the target watch have NFC reader-mode hardware, and is
    tap-to-connect still the intended UX, or should the watch auto-connect whenever the
    paired companion is reachable (`CapabilityClient` node-found)? Most Wear OS watches
    can't act as NFC readers — this may make `MockNfcProvider` permanent and connection
    automatic.
12. **Phone companion distribution** — Play Store (accessibility + notification listener +
    background location = a hard review) or sideload/internal only? Also affects the
    same-package/same-signing question for Data Layer (R4).
13. **`compileSdk`/`targetSdk`** — keep 36, or drop to 35 (both installed), depending on the
    AGP version chosen in Phase 0c.

---

## Critical files for implementation

- `wearos-app/src/main/kotlin/com/dashboard/wearos/MainActivity.kt` — the single
  composition root where every mock→real provider swap is wired (Music, Car, Blizzer,
  Settings).
- `wearos-app/build.gradle.kts` (+ root `build.gradle.kts`, `phone-app/build.gradle.kts`,
  `gradle/wrapper/gradle-wrapper.properties`) — the version realignment that unblocks
  Phase 0.
- `wearos-app/src/main/kotlin/com/dashboard/wearos/hardware/WearDataLayerBluetoothProvider.kt`
  — the transport with the main-thread `Tasks.await` bug and the one-shot link-state
  limitation; central to Phases 0e, 1, 2, 4.
- `phone-app/src/main/kotlin/com/dashboard/phoneapp/WearMessageSender.kt` +
  `phone-app/src/main/AndroidManifest.xml` — where every new phone-side service (media
  session, wear-inbound commands, notification-based nav, camera proximity) plugs in and
  gets its permissions.
- `core/src/main/kotlin/com/dashboard/core/communication/NavigationAnnouncementParser.kt` +
  `core/src/test/kotlin/com/dashboard/core/tests/AllTests.kt` — the parser to harden with
  real fixtures, and the harness registry every new pure suite (`ObdPidParser`,
  `CameraProximity`, `MediaSessionSelection`, `SettingsCodec`) must be added to.
