package com.nseclock.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nseclock.app.prefs.AppPrefs
import com.nseclock.app.schedule.EventTable
import com.nseclock.app.schedule.MarketCalendar
import com.nseclock.app.util.Time

/** Arms exactly ONE alarm for the next event. Re-armed after each fire (reschedule-on-fire). */
object AlarmScheduler {
    const val REQ = 4200
    const val EXTRA_TYPE = "beep_type"
    const val EXTRA_TRIGGER = "trigger"
    const val ACTION_FIRE = "com.nseclock.app.FIRE"

    private fun fireIntent(context: Context): Intent =
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE)

    fun armNext(context: Context) {
        MarketCalendar.ensureLoaded(context)
        val next = EventTable.nextEvent(Time.nowIst()) ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = fireIntent(context)
            .putExtra(EXTRA_TYPE, next.type.id)
            .putExtra(EXTRA_TRIGGER, next.epochMillis)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, REQ, intent, flags)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val show = launch?.let {
            PendingIntent.getActivity(context, 1, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val canExact = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
        if (canExact) {
            // setAlarmClock = exact, wakes device, exempt from Doze.
            am.setAlarmClock(AlarmManager.AlarmClockInfo(next.epochMillis, show), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.epochMillis, pi)
        }
        AppPrefs(context).scheduledTrigger = next.epochMillis
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, REQ, fireIntent(context),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) am.cancel(pi)
        AppPrefs(context).scheduledTrigger = 0L
    }
}
