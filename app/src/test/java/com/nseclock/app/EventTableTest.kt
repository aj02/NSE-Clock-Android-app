package com.nseclock.app

import com.nseclock.app.model.BeepType
import com.nseclock.app.schedule.EventTable
import com.nseclock.app.schedule.MarketCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventTableTest {

    // Holidays are empty in a pure JVM test (assets not loaded) -> only weekends are non-trading.
    private val thu = LocalDate.of(2026, 7, 2)   // Thursday, trading day
    private val sat = LocalDate.of(2026, 7, 4)   // Saturday, weekend

    @Test fun tradingDay_has77Events() {
        val e = EventTable.eventsFor(thu)
        assertEquals(77, e.size)
        assertEquals(BeepType.OPEN, e.first().type)
        assertEquals(LocalTime.of(9, 0), e.first().time.toLocalTime())
        assertEquals(BeepType.CLOSE, e.last().type)
        assertEquals(LocalTime.of(15, 30), e.last().time.toLocalTime())
    }

    @Test fun candleCloses_are74() {
        val candles = EventTable.eventsFor(thu).count { it.type == BeepType.CANDLE }
        assertEquals(74, candles)  // 09:20 .. 15:25 inclusive
    }

    @Test fun firstAndLastCandle() {
        val candles = EventTable.eventsFor(thu).filter { it.type == BeepType.CANDLE }
        assertEquals(LocalTime.of(9, 20), candles.first().time.toLocalTime())
        assertEquals(LocalTime.of(15, 25), candles.last().time.toLocalTime())
    }

    @Test fun weekend_hasNoEvents() {
        assertTrue(EventTable.eventsFor(sat).isEmpty())
    }

    @Test fun nextEvent_beforeOpen_returnsOpen() {
        val now = thu.atTime(8, 59).atZone(MarketCalendar.ZONE)
        val next = EventTable.nextEvent(now)!!
        assertEquals(BeepType.OPEN, next.type)
        assertEquals(LocalTime.of(9, 0), next.time.toLocalTime())
    }

    @Test fun nextEvent_atNextCandleBoundary() {
        val now = thu.atTime(9, 47).atZone(MarketCalendar.ZONE)
        val next = EventTable.nextEvent(now)!!
        assertEquals(BeepType.CANDLE, next.type)
        assertEquals(LocalTime.of(9, 50), next.time.toLocalTime())
    }

    @Test fun nextEvent_afterClose_rollsToNextTradingDay() {
        val fri = LocalDate.of(2026, 7, 3)               // Friday 16:00
        val now = fri.atTime(16, 0).atZone(MarketCalendar.ZONE)
        val next = EventTable.nextEvent(now)!!
        // Sat/Sun skipped -> Monday 06 Jul 09:00
        assertEquals(BeepType.OPEN, next.type)
        assertEquals(LocalDate.of(2026, 7, 6), next.time.toLocalDate())
    }

    @Test fun nextEvent_neverNull_within14Days() {
        val now = thu.atTime(23, 0).atZone(MarketCalendar.ZONE)
        assertTrue(EventTable.nextEvent(now) != null)
    }
}
