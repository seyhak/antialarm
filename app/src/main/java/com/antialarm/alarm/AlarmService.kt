package com.antialarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.antialarm.data.db.AlarmDatabase
import com.antialarm.data.model.Alarm
import com.antialarm.ui.firing.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1

        when (intent?.action) {
            ACTION_SNOOZE -> {
                handleSnooze(alarmId, intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5))
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                stopAlarm()
                return START_NOT_STICKY
            }
        }

        if (alarmId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            val db = androidx.room.Room.databaseBuilder(
                applicationContext,
                AlarmDatabase::class.java,
                "antialarm_db"
            ).build()

            val alarm = db.alarmDao().getAlarmById(alarmId)
            db.close()

            if (alarm != null) {
                launch(Dispatchers.Main) {
                    startForegroundWithNotification(alarm)
                    playAlarmSound(alarm)
                    if (alarm.vibrate) startVibration()
                    launchAlarmActivity(alarm)

                    // Reschedule for next occurrence if repeating
                    if (!alarm.isOneTime) {
                        val scheduler = AlarmSchedulerImpl(applicationContext)
                        scheduler.schedule(alarm)
                    }
                }
            } else {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm firing notifications"
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification(alarm: Alarm) {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_MATH_DIFFICULTY, alarm.mathDifficulty)
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarm.id, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, alarm.id + 10000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, alarm.id + 20000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val label = alarm.label.ifBlank { "Alarm" }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(label)
            .setContentText(alarm.getTimeString())
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun playAlarmSound(alarm: Alarm) {
        try {
            val soundUri = if (alarm.soundUri != null) {
                Uri.parse(alarm.soundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, soundUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to default alarm if custom sound fails
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, defaultUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun startVibration() {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
        val pattern = longArrayOf(0, 500, 200, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun launchAlarmActivity(alarm: Alarm) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_MATH_DIFFICULTY, alarm.mathDifficulty)
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun handleSnooze(alarmId: Int, snoozeMinutes: Int) {
        stopAlarm()

        // Schedule snooze alarm
        serviceScope.launch {
            val db = androidx.room.Room.databaseBuilder(
                applicationContext,
                AlarmDatabase::class.java,
                "antialarm_db"
            ).build()

            val alarm = db.alarmDao().getAlarmById(alarmId)
            db.close()

            if (alarm != null) {
                val snoozeAlarm = alarm.copy(
                    repeatDays = 0 // One-time for snooze
                )
                val scheduler = AlarmSchedulerImpl(applicationContext)

                // Calculate snooze time
                val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
                val calendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = snoozeTime
                }

                val snoozedAlarm = snoozeAlarm.copy(
                    hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    minute = calendar.get(java.util.Calendar.MINUTE)
                )
                scheduler.schedule(snoozedAlarm)
            }
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SNOOZE = "com.antialarm.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.antialarm.ACTION_DISMISS"
        const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"

        fun createDismissIntent(context: Context): Intent {
            return Intent(context, AlarmService::class.java).apply {
                action = ACTION_DISMISS
            }
        }

        fun createSnoozeIntent(context: Context, alarmId: Int, snoozeMinutes: Int): Intent {
            return Intent(context, AlarmService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
        }
    }
}
