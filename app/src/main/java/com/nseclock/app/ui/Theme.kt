package com.nseclock.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Trading-terminal palette (committed dark).
val Accent = Color(0xFFF5B301)
val Up = Color(0xFF26C281)
val Down = Color(0xFFF0616B)
val CandleBlue = Color(0xFF57A6F0)

val ScrBg = Color(0xFF0E1116)
val Scr1 = Color(0xFF161B22)
val Scr2 = Color(0xFF1C222B)
val ScrLine = Color(0xFF242B35)
val ScrFg = Color(0xFFE8EBF0)
val ScrMuted = Color(0xFF7E8797)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF17130A),
    background = ScrBg,
    onBackground = ScrFg,
    surface = Scr1,
    onSurface = ScrFg,
    surfaceVariant = Scr2,
    onSurfaceVariant = ScrMuted,
    outline = ScrLine,
    error = Down
)

@Composable
fun NseClockTheme(content: @Composable () -> Unit) {
    // Committed dark terminal regardless of system theme.
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}

fun colorFor(type: com.nseclock.app.model.BeepType): Color = when (type) {
    com.nseclock.app.model.BeepType.OPEN -> Accent
    com.nseclock.app.model.BeepType.START -> Up
    com.nseclock.app.model.BeepType.CANDLE -> CandleBlue
    com.nseclock.app.model.BeepType.CLOSE -> Down
}
