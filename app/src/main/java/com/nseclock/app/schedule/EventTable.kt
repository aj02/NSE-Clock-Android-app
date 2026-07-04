package com.nseclock.app.schedule

import com.nseclock.app.model.BeepType
import java.time.LocalDate
import java.time.ZonedDateTime

data class MarketEvent(val time: ZonedDateTime, val type: BeepType) {
    val epochMillis: Long get() = time.toInstant().toEpochMilli()
}

/**
 * Pure schedule logic. Generates a day's events and finds the next one.
 * No Android dependencies -> unit-testable.
 */
object EventTable {

    /** All beeps for [date], sorted ascending. Empty on a non-trading day. */
    fun eventsFor(date: LocalDate): List<MarketEvent> {
        if (!MarketCalendar.isTradingDay(date)) return emptyList()
        val z = MarketCalendar.ZONE
        val out = ArrayList<MarketEvent>(80)

        out.add(MarketEvent(date.atTime(MarketCalendar.OPEN_ALERT).atZone(z), BeepType.OPEN))
        out.add(MarketEvent(date.atTime(MarketCalendar.TRADING_START).atZone(z), BeepType.START))

        var t = MarketCalendar.FIRST_CANDLE_CLOSE
        val end = MarketCalendar.MARKET_CLOSE
        while (t.isBefore(end)) {                       // 09:20 .. 15:25
            out.add(MarketEvent(date.atTime(t).atZone(z), BeepType.CANDLE))
            t = t.plusMinutes(MarketCalendar.CANDLE_MINUTES)
        }
        out.add(MarketEvent(date.atTime(end).atZone(z), BeepType.CLOSE))  // 15:30
        return out
    }

    /** First event strictly after [now]; searches up to 14 days ahead for the next trading day. */
    fun nextEvent(now: ZonedDateTime): MarketEvent? {
        var date = now.toLocalDate()
        repeat(14) {
            eventsFor(date).firstOrNull { it.time.isAfter(now) }?.let { return it }
            date = date.plusDays(1)
        }
        return null
    }
}
