package com.antialarm.alarm

import com.antialarm.data.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarmId: Int)
}
