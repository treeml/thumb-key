# Nightshift Tracker

Native Android app for managing hospital shifts, built for an Australian
JMO. Kotlin + Jetpack Compose + Room. Everything is stored locally on the
device — the app requests **no network permission at all**.

One codebase, two apps (Gradle product flavors), installable side by side:

- **Nightshift** (`assembleNightshiftRelease`): true dark theme; tabs
  Jobs / Reviews / Guides for overnight cover shifts.
- **UroDay** (`assembleUrodayRelease`): light theme; tabs Jobs / Rounds /
  Reviews / Learn for urology day shifts — ward-round notes per patient
  with a round-note generator, plus urology ward-skill tutorials and
  condition guides.

## Why this exists

A previous single-file web app version kept losing data (localStorage
eviction). This rewrite makes persistence the core design constraint:

- **Room (SQLite) in write-ahead-logging mode** is the single source of
  truth. Every mutation — including each keystroke in a text field — is
  committed to disk immediately. There is no in-memory model to lose:
  the UI is a projection of database `Flow`s, so force-quit, crash, or
  process death lose nothing.
- **Automatic JSON backup after every save** (bursts coalesce into one
  snapshot ~1.5 s after the last write) to `filesDir/backups/` **and**
  the app-specific external dir (`Android/data/com.nightshift.tracker/files/backups/`,
  readable over USB). Latest + 10 rotating timestamped copies, written
  atomically (temp file + rename). These locations are never cache-evicted.
- **Export backup** (always visible on the home screen) writes a JSON
  file anywhere via the system file picker — Downloads survives even
  uninstall. **Import / restore** validates the file and takes a safety
  snapshot of current data before replacing anything.
- **App updates** can't wipe data: destructive Room migration is disabled;
  schema changes require explicit migrations.
- **Job timers** are stored as absolute deadlines in the DB and re-armed
  after reboot (`BOOT_COMPLETED`), fired via exact `AlarmManager` alarms
  with an audible alarm-channel notification.

## Build

Requirements: Android Studio (Ladybug or newer) with Android SDK 35, or a
command line with JDK 17+ and `ANDROID_HOME` pointing at an SDK with
platform 35.

```bash
cd nightshift-tracker
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

Or open the `nightshift-tracker` folder in Android Studio and Build →
Build APK(s).

## Sideload

1. Copy `app-debug.apk` to the phone (USB, or Quick Share to yourself).
2. Tap the APK in My Files → allow "Install unknown apps" for My Files
   when prompted → Install.
3. First launch: allow notifications (needed for timer alarms).
4. Optional but recommended on Samsung: Settings → Apps → Nightshift →
   Battery → **Unrestricted**, so timer alarms are never deferred.

## Clinical content disclaimer

The Guides tab and ABCDE prompts are a memory aid written to align with
Australian practice (Therapeutic Guidelines style), current to early 2026.
They are **not** a prescribing reference. Doses marked `VERIFY` in
particular must be checked against current eTG / local hospital protocols
before prescribing. Local protocols always win.
