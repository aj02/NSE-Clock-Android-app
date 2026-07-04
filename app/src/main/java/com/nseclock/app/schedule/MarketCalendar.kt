package com.nseclock.app.schedule

import android.content.Context
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** NSE session definition (IST) + trading-holiday set loaded from assets. */
object MarketCalendar {
    val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    val OPEN_ALERT: LocalTime = LocalTime.of(9, 0)      // pre-open alert
    val TRADING_START: LocalTime = LocalTime.of(9, 15)  // trading starts
    val MARKET_CLOSE: LocalTime = LocalTime.of(15, 30)  // market close
    val FIRST_CANDLE_CLOSE: LocalTime = LocalTime.of(9, 20)
    const val CANDLE_MINUTES = 5L

    @Volatile private var holidays: Set<LocalDate> = emptySet()
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            holidays = try {
                val txt = context.assets.open("nse_holidays.json")
                    .bufferedReader().use { it.readText() }
                val root = JSONObject(txt)
                val set = mutableSetOf<LocalDate>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val year = keys.next()
                    if (year.startsWith("_")) continue
                    val arr = root.getJSONArray(year)
                    for (i in 0 until arr.length()) {
                        runCatching { set.add(LocalDate.parse(arr.getString(i))) }
                    }
                }
                set
            } catch (e: Exception) {
                emptySet()
            }
            loaded = true
        }
    }

    fun isHoliday(date: LocalDate): Boolean = holidays.contains(date)

    fun isWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    fun isTradingDay(date: LocalDate): Boolean = !isWeekend(date) && !isHoliday(date)
}
