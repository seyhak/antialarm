package com.antialarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antialarm.data.dao.AlarmDao
import com.antialarm.data.db.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "antialarm_db"
                ).build()

                val scheduler = AlarmSchedulerImpl(context.applicationContext)
                val enabledAlarms = db.alarmDao().getEnabledAlarms()

                enabledAlarms.forEach { alarm ->
                    scheduler.schedule(alarm)
                }

                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
