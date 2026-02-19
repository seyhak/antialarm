package com.antialarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.antialarm.data.model.Alarm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: Alarm) {
        val triggerTime = calculateNextTriggerTime(alarm)
        val pendingIntent = createPendingIntent(alarm)

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            triggerTime,
            createShowIntent(alarm)
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    override fun cancel(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRED
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createShowIntent(alarm: Alarm): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun calculateNextTriggerTime(alarm: Alarm): Long {
            val now = Calendar.getInstance()
            val trigger = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (alarm.isOneTime) {
                // If time already passed today, schedule for tomorrow
                if (trigger.timeInMillis <= now.timeInMillis) {
                    trigger.add(Calendar.DAY_OF_YEAR, 1)
                }
                return trigger.timeInMillis
            }

            // Repeating alarm — find next matching day
            // Calendar: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
            // Our bitmask: Mon=1(bit0), Tue=2(bit1), Wed=4(bit2), Thu=8(bit3), Fri=16(bit4), Sat=32(bit5), Sun=64(bit6)
            val calDayToFlag = mapOf(
                Calendar.MONDAY to Alarm.MONDAY,
                Calendar.TUESDAY to Alarm.TUESDAY,
                Calendar.WEDNESDAY to Alarm.WEDNESDAY,
                Calendar.THURSDAY to Alarm.THURSDAY,
                Calendar.FRIDAY to Alarm.FRIDAY,
                Calendar.SATURDAY to Alarm.SATURDAY,
                Calendar.SUNDAY to Alarm.SUNDAY
            )

            // Check today first (only if time hasn't passed)
            if (trigger.timeInMillis > now.timeInMillis) {
                val todayFlag = calDayToFlag[now.get(Calendar.DAY_OF_WEEK)] ?: 0
                if (alarm.repeatDays and todayFlag != 0) {
                    return trigger.timeInMillis
                }
            }

            // Check next 7 days
            for (i in 1..7) {
                trigger.add(Calendar.DAY_OF_YEAR, 1)
                val dayFlag = calDayToFlag[trigger.get(Calendar.DAY_OF_WEEK)] ?: 0
                if (alarm.repeatDays and dayFlag != 0) {
                    return trigger.timeInMillis
                }
            }

            // Fallback: schedule for tomorrow if no day matches (shouldn't happen)
            trigger.timeInMillis = now.timeInMillis
            trigger.add(Calendar.DAY_OF_YEAR, 1)
            trigger.set(Calendar.HOUR_OF_DAY, alarm.hour)
            trigger.set(Calendar.MINUTE, alarm.minute)
            trigger.set(Calendar.SECOND, 0)
            trigger.set(Calendar.MILLISECOND, 0)
            return trigger.timeInMillis
        }
    }
}
