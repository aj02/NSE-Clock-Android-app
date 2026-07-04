# NSE Clock — Android App Plan

A background Android app that beeps on NSE market events: pre-open alert, trading
start, every 5-minute candle close, and market close. Native Kotlin, auto-starts
on boot, runs as a persistent foreground service. Personal sideload build.

---

## 1. Decisions (locked)

| Decision | Choice | Why |
|---|---|---|
| Stack | **Native Kotlin** | Direct `AlarmManager` / `AudioManager`, best exact-timing + boot + Doze control |
| Holidays | **Skip NSE holidays** | No beeps on declared holidays; needs bundled yearly calendar |
| Distribution | **Personal sideload** | `USE_EXACT_ALARM` allowed, no Play policy review |
| Timezone | **Asia/Kolkata (fixed)** | Compute events in IST regardless of phone timezone |

---

## 2. Event timetable (NSE, IST)

Normal session: **09:15 – 15:30**. 5-min candles.

| Time (IST) | Event | Sound |
|---|---|---|
| 09:00:00 | Pre-open / market-open alert | `open` (unique) |
| 09:15:00 | Trading starts | `start` (unique) |
| 09:20, 09:25, … 15:25 | 5-min candle close (74 events) | `candle` (repeated) |
| 15:30:00 | Market close (= last candle close) | `close` (unique) |

- Candle close = end of each 5-min bar. First bar 09:15–09:20 → close at **09:20**.
- 15:30 is both the last candle close and market close → use the **close** sound.
- Total daily beeps: 1 + 1 + 74 + 1 = **77**.
- Non-trading day (weekend or NSE holiday) → **0 beeps**; next alarm = next trading day 09:00.

All four sounds are **distinct** so the event is identifiable without looking at the phone.

---

## 3. Architecture

```
BOOT_COMPLETED ─► BootReceiver ─► start ClockService (foreground)
                                        │
User opens app ─► MainActivity ─────────┤ (arm on launch too)
                                        ▼
                                 AlarmScheduler.armNext()
                                        │  setAlarmClock(next event, PendingIntent)
                                        ▼
                          AlarmManager (Doze-exempt, exact)
                                        │  fires at event time
                                        ▼
                                  AlarmReceiver (goAsync)
                                        │
                        ┌───────────────┴───────────────┐
                        ▼                                ▼
                 BeepPlayer.play(type)          AlarmScheduler.armNext()
                 (STREAM_ALARM + wakelock)      (re-arm the NEXT event)

WorkManager HeartbeatWorker (every 15 min) ─► re-arm if alarm was lost
```

**Reschedule-on-fire loop:** only ever **one** exact alarm is pending — the next event.
When it fires, play the sound, then compute and arm the following event. This is the
most Doze/battery-friendly pattern (no 77 pending alarms).

### Components
- `BootReceiver` — `RECEIVE_BOOT_COMPLETED` → starts `ClockService`, arms next alarm.
- `ClockService` — foreground service, persistent notification ("Next beep 09:20 · Market open"), owns lifecycle.
- `AlarmScheduler` — computes next event, `setAlarmClock()` (exact, wakes device, bypasses Doze).
- `AlarmReceiver` — PendingIntent target; plays sound + re-arms next. Uses `goAsync()`.
- `BeepPlayer` — `SoundPool` (preloaded) on `AudioAttributes.USAGE_ALARM`; partial `WakeLock` while playing.
- `MarketCalendar` — session hours + bundled NSE holiday list; `isTradingDay()`, `sessionFor(date)`.
- `EventTable` — pure Kotlin, generates sorted `Event(time, BeepType)` list for a date; `nextEvent(now)`.
- `HeartbeatWorker` — WorkManager every 15 min; re-arms if the pending alarm is missing (force-stop / app update recovery).
- `MainActivity` — minimal setup + status UI.

### Project layout
```
app/src/main/
  java/com/nseclock/
    MainActivity.kt
    service/ClockService.kt
    alarm/AlarmScheduler.kt
    alarm/AlarmReceiver.kt
    boot/BootReceiver.kt
    sound/BeepPlayer.kt
    schedule/EventTable.kt
    schedule/MarketCalendar.kt
    model/BeepType.kt        // OPEN, START, CANDLE, CLOSE
    work/HeartbeatWorker.kt
    util/Time.kt             // ZonedDateTime helpers, Asia/Kolkata
  res/raw/                   // open.ogg, start.ogg, candle.ogg, close.ogg
  assets/nse_holidays.json   // { "2026": ["2026-01-26", ...], "2027": [...] }
  AndroidManifest.xml
app/src/test/                // EventTable + MarketCalendar unit tests
```

---

## 4. Exact-timing strategy (the hard part)

Android throttles background timing (Doze, App Standby, OEM battery killers). Layered defense:

1. **`AlarmManager.setAlarmClock(AlarmClockInfo, pi)`** for the next event — treated like a
   user alarm: fires exactly, wakes the device, **exempt from Doze**. (Shows a system alarm
   icon — acceptable.) More precise than `setExactAndAllowWhileIdle`.
2. **Foreground service** keeps the process alive so re-arming is instant.
3. **WorkManager heartbeat (15 min)** re-arms if the alarm vanished (after force-stop, reboot
   race, or app update — pending alarms are cleared on update).
4. **Battery-optimization exemption** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — required for
   long-term reliability.
5. **Re-arm on boot** and **on app open**.

**OEM caveat:** Xiaomi/MIUI, Oppo/ColorOS, Vivo, Samsung, OnePlus have separate "Autostart"
and aggressive background-kill managers that no API can override. The setup screen must
**guide the user to whitelist the app manually** (link to `dontkillmyapp.com`-style steps).

---

## 5. Sound

- **Stream:** `AudioAttributes` `USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION` → plays through
  silent/vibrate and **bypasses DND** (alarms are exempt).
- **Player:** `SoundPool`, preload all 4 clips at service start (low latency, no per-play setup).
- **WakeLock:** acquire `PARTIAL_WAKE_LOCK` for the ~1–2 s of playback so screen-off doesn't cut it.
- **Volume:** play at current alarm volume; optional setting to force max alarm volume during play, then restore.
- **Clips:** 4 short distinct tones in `res/raw` (`.ogg`). Can generate procedurally
  (e.g. open = rising 2-tone, start = triple beep, candle = single short beep, close = long low tone).
- **Test buttons** in UI to preview each sound.

---

## 6. Permissions (AndroidManifest + runtime)

| Permission | Purpose | Notes |
|---|---|---|
| `RECEIVE_BOOT_COMPLETED` | Auto-start on reboot | manifest |
| `FOREGROUND_SERVICE` | Persistent background | manifest |
| `FOREGROUND_SERVICE_SPECIAL_USE` (API 34+) | FGS type | declare use-case string |
| `USE_EXACT_ALARM` (API 33+) | Exact alarms, no runtime prompt | OK for sideload/alarm apps |
| `SCHEDULE_EXACT_ALARM` (API 31–32) | Exact alarms | may need Settings grant |
| `POST_NOTIFICATIONS` (API 33+) | Show FGS notification | runtime request |
| `WAKE_LOCK` | Hold CPU during playback | manifest |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Doze exemption prompt | manifest + intent |

---

## 7. Minimal UI (single screen)

- **Status:** running/paused · market state (pre-open / live / closed / holiday) · "Next beep: 09:20 (candle)".
- **Master toggle:** enable/disable all beeps.
- **Mute today:** skip remaining beeps until tomorrow.
- **Setup checklist:** buttons for → grant notifications, allow exact alarms, disable battery
  optimization, OEM autostart guide. Each shows ✅/❌.
- **Test beeps:** 4 buttons (open / start / candle / close).
- **(Later) Settings:** candle interval, session start/end, force-max-volume, demo mode.

---

## 8. Core algorithm

```kotlin
// EventTable.kt — pure, unit-testable
fun nextEvent(now: ZonedDateTime /*Asia/Kolkata*/): Event {
    var day = now.toLocalDate()
    while (true) {
        if (MarketCalendar.isTradingDay(day)) {
            val events = eventsFor(day)              // 09:00 OPEN, 09:15 START,
                                                     // 09:20..15:25 CANDLE, 15:30 CLOSE
            events.firstOrNull { it.time.isAfter(now) }?.let { return it }
        }
        day = day.plusDays(1)                        // roll to next trading day's 09:00
    }
}
```

`AlarmScheduler.armNext()` = `nextEvent(now())` → `setAlarmClock(event.time, pendingIntent(event.type))`.

---

## 9. Build milestones

1. **Scaffold** — Gradle project, manifest, permissions, minimal status UI showing computed next event.
2. **Schedule logic** — `MarketCalendar` + `EventTable`, fully **unit-tested** (pure Kotlin, no Android).
3. **Alarm loop** — `AlarmScheduler` (`setAlarmClock`) + `AlarmReceiver` re-arm; log-only, no sound yet.
4. **Sound** — `BeepPlayer` on alarm stream + wakelock; wire 4 clips; test buttons.
5. **Persistence** — `ClockService` foreground + notification; `BootReceiver`.
6. **Reliability** — `HeartbeatWorker` re-arm; battery-optimization + exact-alarm permission flows.
7. **Setup UX** — permission checklist, OEM autostart guide, mute-today, master toggle.
8. **Field test** — Doze (`adb shell dumpsys deviceidle force-idle`), screen-off, reboot, real trading day.

---

## 10. Testing

- **Unit (`EventTable`/`MarketCalendar`):** boundaries 08:59:59, 09:00, 09:15, 15:25, 15:30, 15:30:01;
  weekend; holiday; rollover to next trading day; leap/DST-free IST.
- **Instrumented:** arm alarm 60 s out, screen off → fires exactly + correct sound.
- **Doze:** force-idle, confirm `setAlarmClock` still fires.
- **Reboot:** device restart → service + alarm re-armed within seconds.
- **Demo mode:** candle every 30 s to validate the full loop without waiting for market hours.

---

## 11. Known risks / open items

- **OEM background kill** — biggest real-world risk; only user whitelisting fully fixes it. Document per-vendor.
- **Holiday list maintenance** — bundled JSON needs a yearly update (or fetch from a source later).
- **App update** clears pending alarms → heartbeat + on-open re-arm covers it.
- **Exact 09:00** — `setAlarmClock` fires within tight tolerance; good enough for an audible cue.
- **Special sessions** (muhurat trading, session extensions) — not handled; out of scope v1.
