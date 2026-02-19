package com.antialarm.data.repository

import com.antialarm.alarm.AlarmScheduler
import com.antialarm.data.dao.AlarmDao
import com.antialarm.data.model.Alarm
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) {

    fun getAllAlarms(): Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Int): Alarm? = alarmDao.getAlarmById(id)

    suspend fun getEnabledAlarms(): List<Alarm> = alarmDao.getEnabledAlarms()

    suspend fun saveAlarm(alarm: Alarm): Long {
        val id = alarmDao.insertAlarm(alarm)
        val savedAlarm = if (alarm.id == 0) alarm.copy(id = id.toInt()) else alarm
        if (savedAlarm.isEnabled) {
            alarmScheduler.schedule(savedAlarm)
        } else {
            alarmScheduler.cancel(savedAlarm.id)
        }
        return id
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmScheduler.cancel(alarm.id)
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun toggleAlarm(id: Int, enabled: Boolean) {
        alarmDao.setEnabled(id, enabled)
        if (enabled) {
            val alarm = alarmDao.getAlarmById(id)
            alarm?.let { alarmScheduler.schedule(it) }
        } else {
            alarmScheduler.cancel(id)
        }
    }
}
