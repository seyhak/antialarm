package com.antialarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val hour: Int,
        val minute: Int,
        val label: String = "",
        val isEnabled: Boolean = true,
        val soundUri: String? = null,
        val repeatDays: Int = 0,
        val mathDifficulty: Int = 1,
        val snoozeMinutes: Int = 5,
        val vibrate: Boolean = true,
        val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MONDAY = 1
        const val TUESDAY = 2
        const val WEDNESDAY = 4
        const val THURSDAY = 8
        const val FRIDAY = 16
        const val SATURDAY = 32
        const val SUNDAY = 64

        val DAY_FLAGS = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
        val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    val isOneTime: Boolean
        get() = repeatDays == 0

    fun isDayEnabled(dayFlag: Int): Boolean = repeatDays and dayFlag != 0

    fun getEnabledDayNames(): List<String> {
        if (isOneTime) return emptyList()
        return DAY_FLAGS.zip(DAY_NAMES).filter { (flag, _) -> isDayEnabled(flag) }.map { (_, name)
            ->
            name
        }
    }

    fun getTimeString(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun getTimeUntilNext(nowMillis: Long): String? {
        if (!isEnabled) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val trigger =
                Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

        val triggerTime =
                if (isOneTime) {
                    if (trigger.timeInMillis <= now.timeInMillis) {
                        trigger.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    trigger.timeInMillis
                } else {
                    val calDayToFlag =
                            mapOf(
                                    Calendar.MONDAY to MONDAY,
                                    Calendar.TUESDAY to TUESDAY,
                                    Calendar.WEDNESDAY to WEDNESDAY,
                                    Calendar.THURSDAY to THURSDAY,
                                    Calendar.FRIDAY to FRIDAY,
                                    Calendar.SATURDAY to SATURDAY,
                                    Calendar.SUNDAY to SUNDAY
                            )

                    var found = false
                    if (trigger.timeInMillis > now.timeInMillis) {
                        val todayFlag = calDayToFlag[now.get(Calendar.DAY_OF_WEEK)] ?: 0
                        if (repeatDays and todayFlag != 0) {
                            found = true
                        }
                    }

                    if (!found) {
                        for (i in 1..7) {
                            trigger.add(Calendar.DAY_OF_YEAR, 1)
                            val dayFlag = calDayToFlag[trigger.get(Calendar.DAY_OF_WEEK)] ?: 0
                            if (repeatDays and dayFlag != 0) {
                                found = true
                                break
                            }
                        }
                    }
                    trigger.timeInMillis
                }

        val diff = triggerTime - nowMillis
        if (diff < 0) return null

        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60

        return when {
            hours > 0 -> "Alarm in ${hours}h ${minutes}m"
            minutes > 0 -> "Alarm in ${minutes}m"
            else -> "Alarm in < 1m"
        }
    }
}
