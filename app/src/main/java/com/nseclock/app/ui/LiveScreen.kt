package com.nseclock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Mono = FontFamily.Monospace

@Composable
fun LiveScreen(
    ui: LiveUi,
    onOpenSetup: () -> Unit,
    onToggleEnabled: () -> Unit,
    onMuteToday: () -> Unit
) {
    Scaffold(
        containerColor = ScrBg,
        bottomBar = { FooterBar(ui, onToggleEnabled, onMuteToday) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 18.dp)
        ) {
            AppHeader(onOpenSetup)
            StatePill(ui.state)
            Spacer(Modifier.height(14.dp))
            HeroCard(ui)
            Spacer(Modifier.height(4.dp))
            SectionLabel(
                left = "Today · ${ui.dateLine}",
                right = if (ui.totalCount > 0) "${ui.doneCount} / ${ui.totalCount} done" else null
            )
            // Fixed-height viewport that fills the space to the footer.
            // The NEXT event is auto-centered; scroll to see the rest.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (ui.rows.isEmpty()) ClosedCard(ui) else Timeline(ui.rows)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun Timeline(rows: List<EventRow>) {
    val listState = rememberLazyListState()
    val nextIndex = remember(rows) { rows.indexOfFirst { it.isNext } }

    // Re-center only when the NEXT row changes (every 5 min), not every second.
    LaunchedEffect(nextIndex) {
        if (nextIndex >= 0) {
            listState.scrollToItem(nextIndex)              // bring into view + measure
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == nextIndex }
            if (item != null) {
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val itemCenter = item.offset + item.size / 2f
                listState.animateScrollBy(itemCenter - viewportCenter)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(rows) { row -> TimelineRow(row) }
    }
}

@Composable
private fun AppHeader(onOpenSetup: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Text("◆", color = Accent, fontSize = 12.sp) }
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    "NSE CLOCK",
                    color = ScrFg, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp
                )
                Text(
                    "by AjAi",
                    color = Accent, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
                )
            }
        }
        Text(
            "⚙",
            color = ScrMuted, fontSize = 18.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenSetup)
                .padding(6.dp)
        )
    }
}

@Composable
private fun StatePill(state: MarketState) {
    val (fg, bg) = when (state) {
        MarketState.LIVE -> Up to Up.copy(alpha = 0.12f)
        MarketState.PRE_OPEN -> Accent to Accent.copy(alpha = 0.12f)
        MarketState.HOLIDAY -> Accent to Accent.copy(alpha = 0.12f)
        else -> ScrMuted to ScrMuted.copy(alpha = 0.12f)
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(7.dp))
        Text(state.label.uppercase(), color = fg, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun HeroCard(ui: LiveUi) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(listOf(Accent.copy(alpha = 0.08f), Scr1))
            )
            .border(1.dp, ScrLine, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("NEXT BEEP", color = ScrMuted, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            if (ui.countdown != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(ui.countdown, color = Accent, fontFamily = Mono,
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("UNTIL BEEP", color = ScrMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(ui.nextTime ?: "—", color = ScrFg, fontFamily = Mono,
            fontSize = 50.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        val t = ui.nextType
        if (t != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colorFor(t)))
                Spacer(Modifier.width(8.dp))
                Text(t.display, color = ScrFg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionLabel(left: String, right: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(left.uppercase(), color = ScrMuted, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        if (right != null) Text(right.uppercase(), color = ScrMuted, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun TimelineRow(row: EventRow) {
    val bg = if (row.isNext)
        Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.12f), Color.Transparent))
    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.time, fontFamily = Mono, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (row.isNext) Accent else if (row.done) ScrMuted else ScrFg,
            modifier = Modifier.width(48.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(if (row.isNext) 13.dp else 11.dp)
                .clip(CircleShape)
                .background(if (row.done) Scr2 else colorFor(row.type))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            row.label, fontSize = 14.sp,
            fontWeight = if (row.isNext) FontWeight.Bold else FontWeight.Normal,
            color = if (row.done) ScrMuted else ScrFg,
            modifier = Modifier.weight(1f)
        )
        when {
            row.isNext -> Text("NEXT", color = Accent, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            row.done -> Text("✓", color = Up, fontSize = 13.sp)
            else -> Text(row.meta, color = ScrMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ClosedCard(ui: LiveUi) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Scr1)
            .border(1.dp, ScrLine, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text("No trading today", color = ScrFg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        val nxt = if (ui.nextTime != null && ui.nextType != null)
            "Next beep: ${ui.nextTime} · ${ui.nextType.display}" else "No upcoming session"
        Text(nxt, color = ScrMuted, fontSize = 13.sp)
    }
}

@Composable
private fun FooterBar(ui: LiveUi, onToggle: () -> Unit, onMute: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ScrBg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // toggle button
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(13.dp))
                .background(Scr1)
                .border(1.dp, ScrLine, RoundedCornerShape(13.dp))
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .width(34.dp).height(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (ui.enabled) Up else ScrMuted.copy(alpha = 0.5f)),
                contentAlignment = if (ui.enabled) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(Modifier.padding(2.dp).size(16.dp).clip(CircleShape).background(Color.White))
            }
            Spacer(Modifier.width(9.dp))
            Text(if (ui.enabled) "Beeps on" else "Beeps off",
                color = ScrFg, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
        // mute today
        Row(
            Modifier
                .clip(RoundedCornerShape(13.dp))
                .background(if (ui.mutedToday) Accent.copy(alpha = 0.18f) else Scr1)
                .border(1.dp, ScrLine, RoundedCornerShape(13.dp))
                .clickable(onClick = onMute)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (ui.mutedToday) "🔕" else "🔈", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(if (ui.mutedToday) "Muted" else "Mute today",
                color = ScrFg, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
