package com.nseclock.app.prefs

import android.content.Context
import java.time.LocalDate

class AppPrefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("nseclock", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) { sp.edit().putBoolean("enabled", v).apply() }

    var mutedDate: String?
        get() = sp.getString("muted_date", null)
        set(v) { sp.edit().putString("muted_date", v).apply() }

    /** Epoch millis of the currently armed alarm; used by the heartbeat to detect a missed alarm. */
    var scheduledTrigger: Long
        get() = sp.getLong("sched", 0L)
        set(v) { sp.edit().putLong("sched", v).apply() }

    fun isMutedToday(today: LocalDate): Boolean = mutedDate == today.toString()
    fun muteToday(today: LocalDate) { mutedDate = today.toString() }
    fun unmute() { mutedDate = null }
}
