package com.antialarm.ui.firing

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.antialarm.alarm.AlarmReceiver
import com.antialarm.alarm.AlarmService
import com.antialarm.ui.theme.AntiAlarmTheme

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        enableEdgeToEdge()

        val alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        val mathDifficulty = intent.getIntExtra(AlarmService.EXTRA_MATH_DIFFICULTY, 1)
        val snoozeMinutes = intent.getIntExtra(AlarmService.EXTRA_SNOOZE_MINUTES, 5)
        val alarmLabel = intent.getStringExtra(AlarmService.EXTRA_ALARM_LABEL) ?: "Alarm"

        setContent {
            AntiAlarmTheme {
                AlarmFiringScreen(
                    alarmId = alarmId,
                    alarmLabel = alarmLabel,
                    mathDifficulty = mathDifficulty,
                    snoozeMinutes = snoozeMinutes,
                    onDismiss = {
                        startService(AlarmService.createDismissIntent(this))
                        finish()
                    },
                    onSnooze = {
                        startService(AlarmService.createSnoozeIntent(this, alarmId, snoozeMinutes))
                        finish()
                    }
                )
            }
        }
    }
}
