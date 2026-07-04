package com.nseclock.app.ui

import android.content.Context
import com.nseclock.app.model.BeepType
import com.nseclock.app.prefs.AppPrefs
import com.nseclock.app.schedule.EventTable
import com.nseclock.app.schedule.MarketCalendar
import com.nseclock.app.util.Time
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class MarketState(val label: String) {
    LIVE("Market live"), PRE_OPEN("Pre-open"), CLOSED("Market closed"),
    HOLIDAY("Holiday"), WEEKEND("Weekend")
}

data class EventRow(
    val time: String,
    val label: String,
    val type: BeepType,
    val done: Boolean,
    val isNext: Boolean,
    val meta: String
)

data class LiveUi(
    val clock: String,
    val dateLine: String,
    val state: MarketState,
    val nextTime: String?,
    val nextType: BeepType?,
    val countdown: String?,
    val rows: List<EventRow>,
    val doneCount: Int,
    val totalCount: Int,
    val enabled: Boolean,
    val mutedToday: Boolean
)

object Ui {
    private val HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE = DateTimeFormatter.ofPattern("EEE dd MMM")

    fun compute(context: Context): LiveUi {
        MarketCalendar.ensureLoaded(context)
        val prefs = AppPrefs(context)
        val now = Time.nowIst()
        val today = now.toLocalDate()
        val events = EventTable.eventsFor(today)
        val next = EventTable.nextEvent(now)

        val rows = events.map { e ->
            val past = !e.time.isAfter(now)
            val isNext = next != null && e.time == next.time
            EventRow(
                time = e.time.format(HHMM),
                label = e.type.display,
                type = e.type,
                done = past && !isNext,
                isNext = isNext,
                meta = metaFor(e.type)
            )
        }
        val doneCount = events.count { !it.time.isAfter(now) }
        val countdown = next?.let { fmt(it.epochMillis - now.toInstant().toEpochMilli()) }

        return LiveUi(
            clock = now.format(HHMMSS),
            dateLine = today.format(DATE),
            state = stateFor(now),
            nextTime = next?.time?.format(HHMM),
            nextType = next?.type,
            countdown = countdown,
            rows = rows,
            doneCount = doneCount,
            totalCount = events.size,
            enabled = prefs.enabled,
            mutedToday = prefs.isMutedToday(today)
        )
    }

    private fun metaFor(t: BeepType) = when (t) {
        BeepType.CLOSE -> "final"
        BeepType.OPEN -> "alert"
        BeepType.START -> "go"
        BeepType.CANDLE -> "+5m"
    }

    private fun stateFor(now: ZonedDateTime): MarketState {
        val d = now.toLocalDate()
        if (MarketCalendar.isWeekend(d)) return MarketState.WEEKEND
        if (MarketCalendar.isHoliday(d)) return MarketState.HOLIDAY
        val t = now.toLocalTime()
        return when {
            t.isBefore(MarketCalendar.OPEN_ALERT) -> MarketState.CLOSED
            t.isBefore(MarketCalendar.TRADING_START) -> MarketState.PRE_OPEN
            t.isBefore(MarketCalendar.MARKET_CLOSE) -> MarketState.LIVE
            else -> MarketState.CLOSED
        }
    }

    private fun fmt(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }
}
