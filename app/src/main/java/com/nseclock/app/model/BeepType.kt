package com.nseclock.app.model

/** The four distinct market events, each with its own sound and color. */
enum class BeepType(val id: String, val display: String, val colorArgb: Int) {
    OPEN("open", "Market open", 0xFFF5B301.toInt()),
    START("start", "Trading start", 0xFF26C281.toInt()),
    CANDLE("candle", "Candle close", 0xFF57A6F0.toInt()),
    CLOSE("close", "Market close", 0xFFF0616B.toInt());

    companion object {
        fun from(id: String?): BeepType = entries.firstOrNull { it.id == id } ?: CANDLE
    }
}
