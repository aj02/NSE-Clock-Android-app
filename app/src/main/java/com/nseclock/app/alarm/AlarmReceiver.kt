package com.nseclock.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nseclock.app.service.ClockService

/** Fires at each event time. Hands off to the foreground service to play + re-arm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE)
        val svc = Intent(context, ClockService::class.java)
            .setAction(ClockService.ACTION_FIRE)
            .putExtra(AlarmScheduler.EXTRA_TYPE, type)
        // Allowed to start an FGS from an exact-alarm broadcast.
        ContextCompat.startForegroundService(context, svc)
    }
}
