package com.antialarm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antialarm.data.dao.AlarmDao
import com.antialarm.data.model.Alarm

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}
