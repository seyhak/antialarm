package com.antialarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
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

    val isOneTime: Boolean get() = repeatDays == 0

    fun isDayEnabled(dayFlag: Int): Boolean = repeatDays and dayFlag != 0

    fun getEnabledDayNames(): List<String> {
        if (isOneTime) return emptyList()
        return DAY_FLAGS.zip(DAY_NAMES)
            .filter { (flag, _) -> isDayEnabled(flag) }
            .map { (_, name) -> name }
    }

    fun getTimeString(): String {
        return String.format("%02d:%02d", hour, minute)
    }
}
