package com.nseclock.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.nseclock.app.prefs.AppPrefs
import com.nseclock.app.schedule.MarketCalendar
import com.nseclock.app.service.ClockService
import com.nseclock.app.sound.BeepPlayer
import com.nseclock.app.util.Time
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private enum class Screen { LIVE, SETUP }

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MarketCalendar.ensureLoaded(this)
        ClockService.start(this)
        maybeRequestNotif()

        setContent {
            NseClockTheme {
                var screen by remember { mutableStateOf(Screen.LIVE) }
                var ui by remember { mutableStateOf(Ui.compute(this)) }
                var perms by remember { mutableStateOf(readPerms()) }

                LaunchedEffect(Unit) {
                    while (true) {
                        ui = Ui.compute(this@MainActivity)
                        perms = readPerms()
                        delay(1000)
                    }
                }

                when (screen) {
                    Screen.LIVE -> LiveScreen(
                        ui = ui,
                        onOpenSetup = { screen = Screen.SETUP },
                        onToggleEnabled = {
                            val p = AppPrefs(this)
                            p.enabled = !p.enabled
                            ClockService.start(this)
                            ui = Ui.compute(this)
                        },
                        onMuteToday = {
                            val p = AppPrefs(this)
                            val today = Time.nowIst().toLocalDate()
                            if (p.isMutedToday(today)) p.unmute() else p.muteToday(today)
                            ClockService.start(this)
                            ui = Ui.compute(this)
                        }
                    )
                    Screen.SETUP -> SetupScreen(
                        perms = perms,
                        onBack = { screen = Screen.LIVE },
                        onFixNotif = { maybeRequestNotif(force = true) },
                        onFixExact = { openExactAlarmSettings() },
                        onFixBattery = { requestIgnoreBattery() },
                        onAutostartGuide = { openAppDetails() },
                        onTest = { type -> Thread { BeepPlayer.play(type) }.start() }
                    )
                }
            }
        }
    }

    private fun readPerms(): Perms {
        val notif = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        val am = getSystemService(AlarmManager::class.java)
        val exact = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true

        val pm = getSystemService(PowerManager::class.java)
        val battery = pm.isIgnoringBatteryOptimizations(packageName)

        return Perms(notif, exact, battery)
    }

    private fun maybeRequestNotif(force: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted || force) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                )
            }.onFailure { openAppDetails() }
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBattery() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }.onFailure { openAppDetails() }
    }

    private fun openAppDetails() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }
}
