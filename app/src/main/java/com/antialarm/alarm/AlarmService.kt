package com.antialarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.session.MediaSession
import android.net.Uri
import android.os.Build
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
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    private fun setupMediaSession() {
        mediaSession =
                MediaSession(this, "AlarmMathlyMediaSession").apply {
                    setMetadata(
                            MediaMetadata.Builder()
                                    .putString(MediaMetadata.METADATA_KEY_TITLE, "Alarm")
                                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Alarm Mathly")
                                    .build()
                    )
                    isActive = true
                }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1

        // Extract alarm data from extras to show notification immediately
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: ""
        val hour = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 0) ?: 0
        val minute = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0) ?: 0
        val mathDifficulty = intent?.getIntExtra(AlarmReceiver.EXTRA_MATH_DIFFICULTY, 1) ?: 1
        val snoozeMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5) ?: 5
        val vibrate = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true) ?: true
        val soundUri = intent?.getStringExtra(AlarmReceiver.EXTRA_SOUND_URI)

        // Show notification immediately from extras
        startForegroundWithNotification(
                id = alarmId,
                label = label,
                hour = hour,
                minute = minute,
                mathDifficulty = mathDifficulty,
                snoozeMinutes = snoozeMinutes
        )

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
            val db =
                    androidx.room.Room.databaseBuilder(
                                    applicationContext,
                                    AlarmDatabase::class.java,
                                    "antialarm_db"
                            )
                            .build()

            val alarm = db.alarmDao().getAlarmById(alarmId)
            db.close()

            launch(Dispatchers.Main) {
                if (alarm != null) {
                    playAlarmSound(alarm)
                    if (alarm.vibrate) startVibration()
                    launchAlarmActivity(alarm)

                    if (!alarm.isOneTime) {
                        val scheduler = AlarmSchedulerImpl(applicationContext)
                        scheduler.schedule(alarm)
                    }
                } else {
                    // Fallback to extras if alarm not found in DB
                    playAlarmSound(soundUri)
                    if (vibrate) startVibration()
                    launchAlarmActivity(alarmId, label, hour, minute, mathDifficulty, snoozeMinutes)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel =
                NotificationChannel(
                                CHANNEL_ID,
                                "Alarm Notifications",
                                NotificationManager.IMPORTANCE_HIGH
                        )
                        .apply {
                            description = "Alarm firing notifications"
                            setBypassDnd(true)
                            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification(
            id: Int,
            label: String?,
            hour: Int,
            minute: Int,
            mathDifficulty: Int,
            snoozeMinutes: Int
    ) {
        val displayLabel = label?.ifBlank { "Alarm" } ?: "Alarm"
        val timeLabel = String.format("%02d:%02d", hour, minute)

        val fullScreenIntent =
                Intent(this, AlarmActivity::class.java).apply {
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
                    putExtra(EXTRA_MATH_DIFFICULTY, mathDifficulty)
                    putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
                    putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
                    addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }

        val fullScreenPendingIntent =
                PendingIntent.getActivity(
                        this,
                        id,
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val dismissIntent =
                Intent(this, AlarmService::class.java).apply {
                    action = ACTION_DISMISS
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
                }
        val dismissPendingIntent =
                PendingIntent.getService(
                        this,
                        id + 10000,
                        dismissIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val snoozeIntent =
                Intent(this, AlarmService::class.java).apply {
                    action = ACTION_SNOOZE
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
                    putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                }
        val snoozePendingIntent =
                PendingIntent.getService(
                        this,
                        id + 20000,
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val notificationBuilder =
                NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(displayLabel)
                        .setContentText("Ringing: $timeLabel")
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setContentIntent(fullScreenPendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOngoing(true)
                        .setLocalOnly(true)
                        .setSilent(false) // Ensure it makes sound if notification settings allow
                        .setWhen(System.currentTimeMillis())
                        .setShowWhen(true)
                        .setAutoCancel(false)
                        .addAction(
                                android.R.drawable.ic_menu_close_clear_cancel,
                                "Dismiss",
                                dismissPendingIntent
                        )
                        .addAction(
                                android.R.drawable.ic_popup_reminder,
                                "Snooze",
                                snoozePendingIntent
                        )

        // For Android 12+, we want to ensure the foreground service starts with immediate
        // importance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setForegroundServiceBehavior(
                    Notification.FOREGROUND_SERVICE_IMMEDIATE
            )
        }

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun launchAlarmActivity(alarm: Alarm) {
        launchAlarmActivity(
                alarm.id,
                alarm.label,
                alarm.hour,
                alarm.minute,
                alarm.mathDifficulty,
                alarm.snoozeMinutes
        )
    }

    private fun launchAlarmActivity(
            id: Int,
            label: String,
            hour: Int,
            minute: Int,
            difficulty: Int,
            snooze: Int
    ) {
        val intent =
                Intent(this, AlarmActivity::class.java).apply {
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
                    putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
                    putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
                    putExtra(EXTRA_MATH_DIFFICULTY, difficulty)
                    putExtra(EXTRA_SNOOZE_MINUTES, snooze)
                    putExtra(EXTRA_ALARM_LABEL, label)
                    addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
        startActivity(intent)
    }

    private fun playAlarmSound(alarm: Alarm) {
        playAlarmSound(alarm.soundUri)
    }

    private fun playAlarmSound(soundUriString: String?) {
        if (mediaPlayer != null) return

        try {
            val soundUri =
                    if (soundUriString != null) {
                        Uri.parse(soundUriString)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

            mediaPlayer =
                    MediaPlayer().apply {
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
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer =
                        MediaPlayer().apply {
                            setAudioAttributes(
                                    AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                            .setContentType(
                                                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                                            )
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

    private fun handleSnooze(alarmId: Int, snoozeMinutes: Int) {
        stopAlarm()

        serviceScope.launch {
            val db =
                    androidx.room.Room.databaseBuilder(
                                    applicationContext,
                                    AlarmDatabase::class.java,
                                    "antialarm_db"
                            )
                            .build()

            val alarm = db.alarmDao().getAlarmById(alarmId)
            db.close()

            if (alarm != null) {
                val snoozeAlarm = alarm.copy(repeatDays = 0)
                val scheduler = AlarmSchedulerImpl(applicationContext)
                val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = snoozeTime }

                val snoozedAlarm =
                        snoozeAlarm.copy(
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
        mediaSession?.let {
            it.isActive = false
            it.release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel_v5"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SNOOZE = "com.antialarm.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.antialarm.ACTION_DISMISS"
        const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"

        fun createDismissIntent(context: Context): Intent {
            return Intent(context, AlarmService::class.java).apply { action = ACTION_DISMISS }
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
