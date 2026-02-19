package com.antialarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_FIRED) return

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1) return

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_ALARM_FIRED = "com.antialarm.ACTION_ALARM_FIRED"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
