package com.nseclock.app.util

import com.nseclock.app.schedule.MarketCalendar
import java.time.ZonedDateTime

object Time {
    /** Current time in IST, regardless of the phone's timezone. */
    fun nowIst(): ZonedDateTime = ZonedDateTime.now(MarketCalendar.ZONE)
}
