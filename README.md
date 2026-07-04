# NSE Clock (Android)

Beeps a distinct alarm at every NSE market event — pre-open (09:00), trading start
(09:15), every 5-minute candle close, and market close (15:30 IST). Runs as a
persistent background service, auto-starts on boot, and fires **exact** alarms
straight through Doze and silent/DND mode.

Native Kotlin + Jetpack Compose. Personal-sideload build. UI matches `design-mock.html`.

---

## Build & install

Needs **Android Studio** (Koala / 2024.1+) with JDK 17.

1. Android Studio → **Open** → select this folder (`NSEClock`).
2. Let Gradle sync (it downloads Gradle 8.7 + generates the wrapper jar automatically).
3. Plug in your phone (USB debugging on) → **Run ▶**, or **Build → Build APK(s)** and
   sideload the APK from `app/build/outputs/apk/debug/`.

Config: `minSdk 29` (Android 10), `targetSdk 34`. `applicationId com.nseclock.app`.

Run the schedule unit tests: `gradlew test` (or Studio → run `EventTableTest`).

---

## First-run setup (do all of these — this is what makes it reliable)

Open the app → tap the gear → **Setup**:

1. **Notifications** — allow (needed for the ongoing service banner).
2. **Exact alarms** — allow (precise firing through Doze).
3. **Battery optimization** — **Disable** for this app. Critical.
4. **Autostart (OEM)** — on Xiaomi/MIUI, Oppo/ColorOS, Vivo, OnePlus, Samsung there is a
   separate "Autostart" / "background" toggle no app can set for you. Enable it manually,
   else the OS kills the app and beeps stop. See https://dontkillmyapp.com for your brand.

Test the 4 sounds from the Setup screen.

---

## How it works

- **One exact alarm at a time.** `AlarmManager.setAlarmClock()` arms only the *next*
  event (Doze-exempt, wakes device). When it fires, the app plays the beep and arms the
  following event — the reschedule-on-fire loop. See
  [AlarmScheduler](app/src/main/java/com/nseclock/app/alarm/AlarmScheduler.kt).
- **Foreground service** keeps the process alive with a persistent notification —
  [ClockService](app/src/main/java/com/nseclock/app/service/ClockService.kt).
- **Boot re-arm** — [BootReceiver](app/src/main/java/com/nseclock/app/boot/BootReceiver.kt)
  restarts everything after reboot / app update.
- **15-min heartbeat** — [HeartbeatWorker](app/src/main/java/com/nseclock/app/work/HeartbeatWorker.kt)
  re-arms if an alarm was lost (force-stop, update).
- **Sounds are synthesized at runtime** on the alarm stream (`USAGE_ALARM`) so they play
  through silent + DND — no audio files shipped. See
  [ToneSynth](app/src/main/java/com/nseclock/app/sound/ToneSynth.kt).
- **All times computed in `Asia/Kolkata`** regardless of the phone's timezone.

## Event timetable (IST)

| Time | Event | Sound |
|---|---|---|
| 09:00 | Market open (pre-open alert) | rising 2-tone |
| 09:15 | Trading start | triple chime |
| 09:20, 09:25 … 15:25 | Candle close (74×) | short tick |
| 15:30 | Market close | long low |

77 beeps per trading day. Weekends and NSE holidays = silent.

---

## Maintenance

- **Holidays**: [`app/src/main/assets/nse_holidays.json`](app/src/main/assets/nse_holidays.json)
  ships only the certain fixed-date national holidays. Each year, paste the full official
  list from NSE's holiday circular. Dates falling on Sat/Sun are ignored anyway.
- **Special sessions** (muhurat trading, session extensions) are not handled — out of scope.

## Known limits

- OEM battery killers can still stop it if autostart/battery aren't whitelisted (see setup).
- App updates clear pending alarms → the heartbeat + opening the app re-arm within minutes.
