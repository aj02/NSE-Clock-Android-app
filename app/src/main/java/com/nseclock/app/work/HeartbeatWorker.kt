package com.nseclock.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nseclock.app.alarm.AlarmScheduler
import com.nseclock.app.prefs.AppPrefs
import com.nseclock.app.util.Time
import java.util.concurrent.TimeUnit

/** Safety net: every ~15 min re-arm the alarm if it was lost (force-stop, app update, missed fire). */
class HeartbeatWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = AppPrefs(applicationContext)
        val sched = prefs.scheduledTrigger
        val now = Time.nowIst().toInstant().toEpochMilli()
        if (sched == 0L || sched < now - 5_000) {
            // No alarm armed, or it should have fired already -> re-arm.
            // Only touch AlarmManager here (allowed from background). The alarm's own
            // fire restarts the foreground service, which is an allowed FGS-start exemption.
            AlarmScheduler.armNext(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val NAME = "nseclock_heartbeat"
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
