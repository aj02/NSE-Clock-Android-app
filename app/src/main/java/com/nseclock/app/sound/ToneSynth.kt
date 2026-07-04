package com.nseclock.app.sound

import com.nseclock.app.model.BeepType
import kotlin.math.PI
import kotlin.math.sin

/** Generates the four distinct beeps as 16-bit PCM at runtime (no audio files shipped). */
object ToneSynth {
    const val SAMPLE_RATE = 44100

    private data class Seg(val freq: Double, val ms: Int, val gapMs: Int = 0, val amp: Double = 0.85)

    private fun spec(type: BeepType): List<Seg> = when (type) {
        // rising 2-tone
        BeepType.OPEN   -> listOf(Seg(660.0, 150), Seg(990.0, 260))
        // triple chime
        BeepType.START  -> listOf(Seg(880.0, 90, 70), Seg(880.0, 90, 70), Seg(880.0, 130))
        // single short tick
        BeepType.CANDLE -> listOf(Seg(1040.0, 110))
        // long low two-step
        BeepType.CLOSE  -> listOf(Seg(392.0, 260), Seg(294.0, 560))
    }

    fun pcm16(type: BeepType): ShortArray {
        val segs = spec(type)
        var total = 0
        for (s in segs) total += SAMPLE_RATE * (s.ms + s.gapMs) / 1000
        val out = ShortArray(total)
        var idx = 0
        for (s in segs) {
            val n = SAMPLE_RATE * s.ms / 1000
            val fade = (SAMPLE_RATE * 8 / 1000).coerceAtMost(n / 2)  // 8 ms fade to kill clicks
            for (i in 0 until n) {
                val env = when {
                    i < fade -> i.toDouble() / fade
                    i > n - fade -> (n - i).toDouble() / fade
                    else -> 1.0
                }
                val v = sin(2 * PI * s.freq * i / SAMPLE_RATE) * s.amp * env
                out[idx++] = (v * Short.MAX_VALUE).toInt().toShort()
            }
            repeat(SAMPLE_RATE * s.gapMs / 1000) { out[idx++] = 0 }
        }
        return out
    }
}
