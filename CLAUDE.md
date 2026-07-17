# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

QuietNight is a single-module Android app (`:app`) that monitors snoring during sleep. A foreground service records from the mic, converts the audio to a decibel level, vibrates when it crosses a threshold, and persists a nightly sleep session with a score. UI strings and code comments are in Korean.

Stack: Kotlin, Jetpack Compose (Material 3), Hilt, Room (via KSP), Navigation Compose. `minSdk` 28, `targetSdk`/`compileSdk` 36, Java 11 source/target, Gradle toolchain JDK 21, AGP 9.

## Commands

Use `gradlew.bat` on Windows, `./gradlew` elsewhere.

```bash
gradlew.bat :app:assembleDebug              # build
gradlew.bat :app:installDebug               # build + install to connected device/emulator
gradlew.bat :app:lintDebug                  # Android Lint (no other linter configured)

gradlew.bat :app:testDebugUnitTest          # JVM unit tests
gradlew.bat :app:testDebugUnitTest --tests 'com.example.quietnight.ExampleUnitTest.addition_isCorrect'

gradlew.bat :app:connectedDebugAndroidTest  # instrumented tests (needs a device)
gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.quietnight.ExampleInstrumentedTest
```

Both test source sets currently contain only the generated template tests. The app needs a real device or emulator with a microphone to exercise anything meaningful.

## Architecture

### The service owns the session

`SnoreService` is the single source of truth for a monitoring session. It is `@AndroidEntryPoint` and **field-injects `SnoreDao` and `SettingsRepository`** directly, and communicates live data to the UI through **static flows on its companion object**:

- `SnoreService.dbFlow: MutableStateFlow<Int>` — current decibel level, emitted every 100 ms.
- `SnoreService.logFlow: MutableSharedFlow<String>` — one emission per snore *event* (cooldown-gated, not per tick).
- `SnoreService.monitoringFlow: MutableStateFlow<Boolean>` — whether a session is active. The service sets it, so it re-syncs after process death.
- `SnoreService.startTimeFlow: MutableStateFlow<Long>` — session start epoch millis (0 when idle), used by the ViewModel to drive the elapsed-time clock.

`SleepViewModel.init` collects all four (plus `SettingsRepository.settings` for `threshold`, and `dao.getSessionByDateFlow(today)` for the live Home stats). The service is started/stopped by `context.startForegroundService` / `stopService` from the ViewModel via the injected `@ApplicationContext`.

### Detection loop (`SnoreService.startAnalysis`)

`AudioRecord` at 44.1 kHz mono PCM 16-bit → RMS over the samples actually read → `20 * log10(rms)` clamped to 0..100 → emitted on `dbFlow`, every 100 ms on `Dispatchers.IO`. Above the **configured threshold** (`SettingsRepository`, default 75 dB) every 100 ms tick adds 100 ms to `runSnoreMs` (snore *duration*), while vibration + `logFlow` fire only once per **3 s cooldown** (`VIBRATE_COOLDOWN_MS`) so it doesn't buzz continuously. Vibration is `createOneShot(300, vibrationStrength)` (amplitude 1–255, set in settings, default 150; ignored by devices without amplitude control) and is skipped when `vibrationEnabled = false`.

### Session accumulation & auto-save

`startSession()` **rebaselines from the DB** on every start: it reads today's row and sets `baseSleepMs`/`baseSnoreMs`/`timeline` from it, then `startTime = now`, `runSnoreMs = 0`. `flush(now)` writes `sleepTime = baseSleepMs + (now - startTime)`, `snoreTime = baseSnoreMs + runSnoreMs`, `score = clamp((sleep-snore)/sleep*100, 0..100)`, and the timeline CSV — an idempotent upsert of today's row. `flush` runs **every 10 s** (`FLUSH_INTERVAL_MS`) and in `onDestroy` (via `runBlocking`), so **the session is saved without pressing "종료"**, and an OS kill (`START_STICKY` restart → `onStartCommand` → rebaseline-from-DB) resumes onto the last flushed row. All snore/sleep math lives here, not in the Activity.

`SleepSession.snoreTime`/`sleepTime` and `state.todaySnoreTime`/`todaySleepTime` are all **milliseconds**. The per-night dB timeline is sampled once a minute (max dB in the minute) into `SleepSession.timelineCsv` (comma-separated ints) and rendered by `DayDetailDialog`'s `TimelineChart`.

### MVI contract

`QuietNightContract.kt` holds `SnoreIntent` (StartMonitoring / StopMonitoring / LoadHistory) and `SnoreState` (one flat state object grouped by screen). There is no `SnoreEffect` — the Home screen refreshes reactively from the today-session Flow, so no one-shot channel is needed. `StopMonitoring` carries no payload (the service already persisted everything).

`SleepViewModel.handleIntent` is **private by design**. Callers use the public wrappers (`startMonitoring()`, `stopMonitoring()`, `loadSessionHistory()`). When adding behavior, add a `SnoreIntent` case plus a wrapper — don't expose a public method that bypasses `handleIntent`, and don't make `handleIntent` public.

Settings are a separate concern: `SettingsRepository` (SharedPreferences-backed, `@Singleton`) exposes `settings: StateFlow<AppSettings>` (`threshold`, `vibrationEnabled`) and is consumed by the service, `SleepViewModel`, and `SettingsViewModel` (which backs `SettingsScreen`).

### Persistence

One row per day. `SleepSession.date` is midnight epoch millis in the system zone and carries a unique index; the service's `flush` looks the row up by date and updates it if present, so repeated flushes and same-day sessions accumulate rather than inserting. `getHistory()` and `getSessionByDateFlow()` return `Flow`s and collect indefinitely.

`QuietNightDatabase` is at version 5. Schemas are exported to `app/schemas` (`room.schemaLocation` KSP arg), and **real migrations preserve user data**: `MIGRATION_4_5` (in `data/Migrations.kt`) recreates `sleep_sessions` to drop `positionStatsJson` and add `timelineCsv`. The builder only falls back to destructive wipe **from the pre-migration dev versions 1–3** (`fallbackToDestructiveMigrationFrom(true, 1, 2, 3)`). When you change the schema again: bump the version, build once to emit the new `N.json`, and add a `MIGRATION_(N-1)_N` whose `CREATE TABLE` matches that JSON exactly (Room validates the resulting schema by identity hash at open time).

`AnalyzeScreen` renders a 7-bar score chart with a 일간/주간/월간 (`ChartPeriod`) toggle. Aggregation is client-side over `state.weeklyHistory` (the full session list). `buildChartBars` produces exactly 7 `ChartBar`s: DAILY = last 7 days' score, WEEKLY = last 7 weeks' average, MONTHLY = last 7 months' average. Empty buckets become a greyed dummy bar (`hasData = false`); the max height uses `coerceAtLeast(1)` to avoid divide-by-zero on an all-empty chart. DAILY bars carry their `SleepSession`, so tapping one opens `DayDetailDialog` (score, 코골이/수면 시간, night timeline graph).

### Known dead code

`SnoreEvent` is a Room `@Entity` but is **not** listed in `@Database(entities = [...])`, so no `snore_events` table exists. The notification is static; the `RemoteViews` decibel-updating variant (`updateNotification`, `res/layout/notification_decibel.xml`) is commented out. (The old hardcoded `positionStatsJson`/`positionStats` fake-position placeholder has been removed.)

## Permissions

`RECORD_AUDIO`, `POST_NOTIFICATIONS`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`. `MainActivity.checkPermissions` requests them at startup, SDK-gating `POST_NOTIFICATIONS` (33+) and `FOREGROUND_SERVICE_MICROPHONE` (34+). If `RECORD_AUDIO` ends up denied, the permission-result callback flips `showMicDialog`, and `MainActivity` shows a blocking `AlertDialog` offering **권한 허용** (re-request, or jump to app settings when permanently denied via `shouldShowRequestPermissionRationale`) or **앱 종료** (`finish()`). `onResume` clears the dialog once the permission is granted. `startAnalysis` also `stopSelf()`s if the mic permission is missing.

## Theming

`QuietNightTheme` picks a hand-rolled dark or light `ColorScheme` from `isSystemInDarkTheme()` (no dynamic color). Each screen composable wraps its own content in `QuietNightTheme` again, in addition to the wrap in `MainActivity.setContent`. Semantic colors come from `MaterialTheme.colorScheme`; the green/red deltas on `HomeScreen` stat cards are hardcoded (`Color(0xFF4ADE80)` / `Color.Red`).
