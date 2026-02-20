package com.antialarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_FIRED) return

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1) return

        val serviceIntent =
                Intent(context, AlarmService::class.java).apply {
                    putExtras(intent)
                    // putExtra(EXTRA_ALARM_ID, alarmId)
                }

        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_ALARM_FIRED = "com.antialarm.ACTION_ALARM_FIRED"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
        const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
    }
}
