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

### Service ↔ ViewModel channel

`SnoreService` and `SleepViewModel` communicate through **static flows on the service's companion object**, not through binding, a repository, or DI:

- `SnoreService.dbFlow: MutableStateFlow<Int>` — current decibel level, emitted every 100 ms.
- `SnoreService.logFlow: MutableSharedFlow<String>` — one emission per snore detection.

`SleepViewModel.init` collects both for the lifetime of the ViewModel. The service is started and stopped by `context.startForegroundService` / `stopService` from inside the ViewModel, using the injected `@ApplicationContext`. Anything new that flows from the service to the UI goes through this companion-object channel.

### Detection loop (`SnoreService.startAnalysis`)

`AudioRecord` at 44.1 kHz mono PCM 16-bit → RMS → `20 * log10(rms)` clamped to 0..100 → emitted on `dbFlow`. Above **75 dB** the service vibrates for 300 ms at amplitude 150 (`VibrationEffect.createOneShot(300, 150)`) and emits a line on `logFlow`. The loop ticks every 100 ms on `Dispatchers.IO`.

### The snoreTime unit conversion

This spans three files and is easy to get wrong:

1. Each `logFlow` emission increments `SnoreState.snoreTime` by **1** — so in state it is a *tick count*, not a duration.
2. Each tick is one 100 ms loop iteration, so `MainActivity.createSnoreSessionIntent` multiplies by 100 to get **milliseconds** before persisting.
3. `SleepSession.snoreTime` in Room is therefore milliseconds, and `state.todaySnoreTime` (read back from the DB) is also milliseconds — a different unit from `state.snoreTime`.

### MVI contract

`QuietNightContract.kt` holds `SnoreIntent` (StartMonitoring / StopMonitoring / LoadHistory / TodayScore), `SnoreState` (one flat state object grouped by screen), and `SnoreEffect` (one-shot, over a `Channel`).

`SleepViewModel.handleIntent` is **private by design**. Callers use the public wrappers (`startMonitoring()`, `stopMonitoring(session)`, `loadSessionHistory()`, `updateTodayScore()`). When adding behavior, add an `SnoreIntent` case plus a wrapper — don't expose a public method that bypasses `handleIntent`, and don't make `handleIntent` public.

Effects are collected in `MainActivity.onEffect()` under `repeatOnLifecycle(STARTED)`.

### Score calculation lives in the Activity

`MainActivity.createSnoreSessionIntent` builds the `SleepSession` — score, accumulated sleep time, and the date key — and hands it to the ViewModel inside `SnoreIntent.StopMonitoring`. Score is `(sleepTime - snoreMs) / sleepTime * 100`. The monitoring `startTime` is a plain `var` local to `onCreate`, captured by the `setContent` lambda; it is not part of `SnoreState` and does not survive process death.

### Persistence

One row per day. `SleepSession.date` is midnight epoch millis in the system zone and carries a unique index; `SleepViewModel.upsertSleepSession` looks the row up by date and updates it if present, so a second session on the same day accumulates rather than inserting. `getHistory()` returns a `Flow`, so `LoadHistory` collects indefinitely.

`QuietNightDatabase` is at version 4 with `fallbackToDestructiveMigration(true)` — schema changes wipe user data and no migration needs to be written.

`WeeklyScreen.fillMissingDays` pads history to exactly 7 days with score-0 dummy sessions so the bar chart always has seven bars.

### Known dead code

`SnoreEvent` is a Room `@Entity` but is **not** listed in `@Database(entities = [...])`, so no `snore_events` table exists. `SleepSession.positionStatsJson` and `SnoreState.positionStats` are placeholders — the JSON is hardcoded to `"등:82,옆:18"` and never parsed. The notification is static; the `RemoteViews` decibel-updating variant (`updateNotification`, `res/layout/notification_decibel.xml`) is commented out.

## Permissions

`RECORD_AUDIO`, `POST_NOTIFICATIONS`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`. `MainActivity.checkPermissions` requests them at startup, SDK-gating `POST_NOTIFICATIONS` (33+) and `FOREGROUND_SERVICE_MICROPHONE` (34+). `startAnalysis` returns silently if `RECORD_AUDIO` was denied, so a denied mic permission looks like a service that starts but reports 0 dB forever.

## Theming

`QuietNightTheme` picks a hand-rolled dark or light `ColorScheme` from `isSystemInDarkTheme()` (no dynamic color). Each screen composable wraps its own content in `QuietNightTheme` again, in addition to the wrap in `MainActivity.setContent`. Semantic colors come from `MaterialTheme.colorScheme`; the green/red deltas on `HomeScreen` stat cards are hardcoded (`Color(0xFF4ADE80)` / `Color.Red`).
