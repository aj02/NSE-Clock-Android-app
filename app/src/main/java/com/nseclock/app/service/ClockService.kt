package com.nseclock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nseclock.app.R
import com.nseclock.app.alarm.AlarmScheduler
import com.nseclock.app.model.BeepType
import com.nseclock.app.prefs.AppPrefs
import com.nseclock.app.schedule.EventTable
import com.nseclock.app.sound.BeepPlayer
import com.nseclock.app.ui.MainActivity
import com.nseclock.app.util.Time
import com.nseclock.app.work.HeartbeatWorker
import java.time.format.DateTimeFormatter

/**
 * Persistent foreground service. Owns the reschedule-on-fire loop:
 * arm next alarm -> on fire, play the beep and arm the following event.
 */
class ClockService : Service() {

    companion object {
        const val ACTION_START = "com.nseclock.app.START"
        const val ACTION_FIRE = "com.nseclock.app.FIRE"
        const val ACTION_STOP = "com.nseclock.app.STOP"

        const val CH_ONGOING = "ongoing"
        const val CH_ALERT = "alert"
        const val NOTIF_ONGOING = 1001
        const val NOTIF_ALERT = 1002

        private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun start(context: Context) {
            val i = Intent(context, ClockService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            val i = Intent(context, ClockService::class.java).setAction(ACTION_STOP)
            ContextCompat.startForegroundService(context, i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        goForeground(buildOngoing("NSE Clock", "Starting…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AlarmScheduler.cancel(this)
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_FIRE -> handleFire(BeepType.from(intent.getStringExtra(AlarmScheduler.EXTRA_TYPE)))
        }
        AlarmScheduler.armNext(this)
        HeartbeatWorker.enqueue(this)
        updateOngoing()
        return START_STICKY
    }

    // ---- alarm fired ----
    private fun handleFire(type: BeepType) {
        val prefs = AppPrefs(this)
        val today = Time.nowIst().toLocalDate()
        if (prefs.enabled && !prefs.isMutedToday(today)) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nseclock:beep")
            wl.acquire(5_000)
            runCatching { BeepPlayer.play(type) }
            Handler(Looper.getMainLooper()).postDelayed({
                if (wl.isHeld) runCatching { wl.release() }
            }, 3_000)
            notifyAlert(type)
        }
    }

    // ---- notifications ----
    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_ONGOING, "Clock status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the next scheduled beep."
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Beep alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Heads-up when a market event beeps."
                setSound(null, null)   // tone is synthesized separately; avoid double sound
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun contentPi(): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildOngoing(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(contentPi())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun goForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this, NOTIF_ONGOING, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ONGOING, n)
        }
    }

    private fun updateOngoing() {
        val next = EventTable.nextEvent(Time.nowIst())
        val text = if (next != null) {
            "Next: ${next.time.format(HHMM)} · ${next.type.display}"
        } else {
            "No upcoming session"
        }
        val prefs = AppPrefs(this)
        val title = when {
            !prefs.enabled -> "NSE Clock — paused"
            prefs.isMutedToday(Time.nowIst().toLocalDate()) -> "NSE Clock — muted today"
            else -> "NSE Clock"
        }
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ONGOING, buildOngoing(title, text))
    }

    private fun notifyAlert(type: BeepType) {
        val now = Time.nowIst().format(HHMM)
        val body = when (type) {
            BeepType.CANDLE -> "5-minute bar closed at $now."
            BeepType.OPEN -> "Pre-open at $now. Market opens 09:15."
            BeepType.START -> "Trading started at $now."
            BeepType.CLOSE -> "Market closed at $now."
        }
        val n = NotificationCompat.Builder(this, CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle("${type.display} · $now")
            .setContentText(body)
            .setColor(type.colorArgb)
            .setContentIntent(contentPi())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(60_000)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ALERT, n)
    }
}
