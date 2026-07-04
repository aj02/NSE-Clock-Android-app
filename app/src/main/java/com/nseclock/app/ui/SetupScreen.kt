package com.nseclock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nseclock.app.model.BeepType

data class Perms(val notif: Boolean, val exact: Boolean, val battery: Boolean) {
    val okCount get() = listOf(notif, exact, battery).count { it }
}

@Composable
fun SetupScreen(
    perms: Perms,
    onBack: () -> Unit,
    onFixNotif: () -> Unit,
    onFixExact: () -> Unit,
    onFixBattery: () -> Unit,
    onAutostartGuide: () -> Unit,
    onTest: (BeepType) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(ScrBg)
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item { SetupHeader(onBack) }
        item {
            SectionRow("Keep beeps firing", "${perms.okCount} / 3")
        }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Scr1)
                    .border(1.dp, ScrLine, RoundedCornerShape(16.dp))
            ) {
                CheckRow(
                    ok = perms.notif, title = "Notifications",
                    sub = "Foreground service banner allowed",
                    actionLabel = "Allow", onAction = onFixNotif
                )
                Divider()
                CheckRow(
                    ok = perms.exact, title = "Exact alarms",
                    sub = "Fires precisely through Doze",
                    actionLabel = "Allow", onAction = onFixExact
                )
                Divider()
                CheckRow(
                    ok = perms.battery, title = "Battery optimization",
                    sub = "Restricting background — may miss beeps",
                    actionLabel = "Disable", onAction = onFixBattery
                )
                Divider()
                CheckRow(
                    ok = null, title = "Autostart (OEM)",
                    sub = "Whitelist so it re-launches on reboot",
                    actionLabel = "Guide", onAction = onAutostartGuide
                )
            }
        }

        item { SectionRow("Test sounds", null) }
        item {
            val items = listOf(
                Triple(BeepType.OPEN, "Open", "rising 2-tone"),
                Triple(BeepType.START, "Start", "triple chime"),
                Triple(BeepType.CANDLE, "Candle", "short tick"),
                Triple(BeepType.CLOSE, "Close", "long low")
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { (type, name, sub) ->
                            TestButton(Modifier.weight(1f), type, name, sub) { onTest(type) }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Scr1)
                    .border(1.dp, ScrLine, RoundedCornerShape(10.dp))
                    .padding(13.dp)
            ) {
                Text("Plays through silent & DND.", color = ScrFg,
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Sounds route on the alarm channel, so they ring with the phone muted. " +
                        "Volume follows your alarm slider.",
                    color = ScrMuted, fontSize = 12.sp
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Accent)
                    .clickable(onClick = onBack)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓  Finish setup", color = Color(0xFF17130A),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetupHeader(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = ScrFg, fontSize = 26.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 6.dp))
            Spacer(Modifier.width(6.dp))
            Text("SETUP", color = ScrFg, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Accent.copy(alpha = 0.12f))
                .border(1.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 5.dp)
        ) {
            Text("Reliability", color = Accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionRow(left: String, right: String?) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(left.uppercase(), color = ScrMuted, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        if (right != null) Text(right, color = ScrMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(ScrLine))
}

@Composable
private fun CheckRow(
    ok: Boolean?, title: String, sub: String,
    actionLabel: String, onAction: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icBg, icFg, glyph) = when (ok) {
            true -> Triple(Up.copy(alpha = 0.14f), Up, "✓")
            false -> Triple(Accent.copy(alpha = 0.14f), Accent, "!")
            null -> Triple(ScrMuted.copy(alpha = 0.14f), ScrMuted, "›")
        }
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(icBg),
            contentAlignment = Alignment.Center
        ) { Text(glyph, color = icFg, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ScrFg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = ScrMuted, fontSize = 12.sp)
        }
        if (ok == true) {
            Text("✓", color = Up, fontSize = 15.sp)
        } else {
            val amber = ok == false
            Text(
                actionLabel,
                color = if (amber) Color(0xFF17130A) else ScrFg,
                fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (amber) Accent else Color.Transparent)
                    .border(
                        if (amber) 0.dp else 1.dp,
                        if (amber) Color.Transparent else ScrLine,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun TestButton(
    modifier: Modifier, type: BeepType, name: String, sub: String, onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Scr1)
            .border(1.dp, ScrLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(colorFor(type)),
            contentAlignment = Alignment.Center
        ) { Text("▶", color = Color(0xFF0C0D10), fontSize = 11.sp) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(name, color = ScrFg, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = ScrMuted, fontSize = 11.sp)
        }
    }
}
