package com.nseclock.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nseclock.app.schedule.MarketCalendar
import com.nseclock.app.service.ClockService

/** Re-launches the service + re-arms the alarm after reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MarketCalendar.ensureLoaded(context)
                ClockService.start(context)
            }
        }
    }
}
