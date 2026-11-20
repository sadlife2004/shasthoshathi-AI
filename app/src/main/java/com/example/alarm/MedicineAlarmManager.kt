package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MedicineReminder
import com.example.ui.components.parseClockTime
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private const val ALARM_TAG = "MedicineAlarm"
private const val CHANNEL_ID = "shasthosathi_medicine_alarms_v2"
private const val PREFS_NAME = "shasthosathi_scheduled_alarms"
private const val KEY_ALARMS_JSON = "active_alarms_json"

private const val EXTRA_REMINDER_ID = "extra_reminder_id"
private const val EXTRA_MED_NAME = "extra_med_name"
private const val EXTRA_DOSAGE = "extra_dosage"
private const val EXTRA_MEAL = "extra_meal"
private const val EXTRA_TIME = "extra_time"
private const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
private const val EXTRA_ACTION = "extra_alarm_action"

private const val ACTION_STOP_ALARM = "com.example.alarm.STOP"
private const val ACTION_SNOOZE_ALARM = "com.example.alarm.SNOOZE_10M"

object AlarmSoundPlayer {
    @Volatile
    private var activeRingtone: Ringtone? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stop() }

    fun play(context: Context, ringtoneUriString: String?, autoStopMs: Long = 45_000L) {
        stop()
        try {
            val uri = resolveRingtoneUri(ringtoneUriString)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = true
                }
                ringtone.play()
                activeRingtone = ringtone
                mainHandler.removeCallbacks(autoStopRunnable)
                mainHandler.postDelayed(autoStopRunnable, autoStopMs)
            }
        } catch (e: Exception) {
            Log.e(ALARM_TAG, "Failed to play alarm ringtone", e)
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(autoStopRunnable)
        try {
            activeRingtone?.stop()
        } catch (e: Exception) {
            Log.w(ALARM_TAG, "Error stopping ringtone", e)
        } finally {
            activeRingtone = null
        }
    }

    fun resolveRingtoneUri(uriString: String?): Uri {
        if (!uriString.isNullOrBlank()) {
            return Uri.parse(uriString)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun resolveRingtoneTitle(context: Context, uriString: String?): String {
        return try {
            val uri = resolveRingtoneUri(uriString)
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)?.takeIf { it.isNotBlank() }
                ?: "Default Alarm"
        } catch (_: Exception) {
            "Default Alarm"
        }
    }
}

object MedicineAlarmScheduler {

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medicine Reminder Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Background medicine dose reminders with alarm sound and vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 250, 400)
                setSound(null, null) // Custom ringtone is played via AlarmSoundPlayer with USAGE_ALARM
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun syncReminders(context: Context, reminders: List<MedicineReminder>) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        // Cancel previously stored alarms before re-scheduling active set
        val previouslyStored = loadStoredSpecs(appContext)
        previouslyStored.forEach { spec ->
            cancelAlarmByKey(appContext, spec.id)
        }

        val activeSpecs = reminders
            .filter { it.active && it.id.isNotBlank() }
            .map { rem ->
                StoredAlarmSpec(
                    id = rem.id,
                    medicineName = rem.medicineName,
                    dosage = rem.dosage,
                    mealTiming = rem.mealTiming,
                    scheduledTime = rem.scheduledTime,
                    ringtoneUri = rem.ringtoneUri
                )
            }

        saveStoredSpecs(appContext, activeSpecs)
        activeSpecs.forEach { spec ->
            scheduleNextDailyAlarm(appContext, spec)
        }
    }

    fun scheduleSnooze10Min(
        context: Context,
        reminderId: String,
        medicineName: String,
        dosage: String,
        mealTiming: String,
        scheduledTime: String,
        ringtoneUri: String
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val triggerAtMillis = System.currentTimeMillis() + 10 * 60 * 1000L
        val spec = StoredAlarmSpec(
            id = "${reminderId}_snooze",
            medicineName = medicineName,
            dosage = dosage,
            mealTiming = mealTiming,
            scheduledTime = scheduledTime,
            ringtoneUri = ringtoneUri
        )
        scheduleExactOrFallback(appContext, spec, triggerAtMillis)
    }

    fun rescheduleAllFromStorage(context: Context) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        loadStoredSpecs(appContext).forEach { spec ->
            scheduleNextDailyAlarm(appContext, spec)
        }
    }

    internal fun scheduleNextDailyAlarm(
        context: Context,
        spec: StoredAlarmSpec,
        forceTomorrow: Boolean = false
    ) {
        val (hour24, minute) = parseClockTime(spec.scheduledTime)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (forceTomorrow || timeInMillis <= System.currentTimeMillis() + 2_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleExactOrFallback(context, spec, cal.timeInMillis)
    }

    private fun scheduleExactOrFallback(
        context: Context,
        spec: StoredAlarmSpec,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context, spec)

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(ALARM_TAG, "Exact alarm denied; falling back to setAndAllowWhileIdle", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(ALARM_TAG, "Failed to schedule alarm for ${spec.medicineName}", e)
        }
    }

    private fun cancelAlarmByKey(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun buildAlarmPendingIntent(context: Context, spec: StoredAlarmSpec): PendingIntent {
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, spec.id)
            putExtra(EXTRA_MED_NAME, spec.medicineName)
            putExtra(EXTRA_DOSAGE, spec.dosage)
            putExtra(EXTRA_MEAL, spec.mealTiming)
            putExtra(EXTRA_TIME, spec.scheduledTime)
            putExtra(EXTRA_RINGTONE_URI, spec.ringtoneUri)
        }
        return PendingIntent.getBroadcast(
            context,
            spec.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun saveStoredSpecs(context: Context, specs: List<StoredAlarmSpec>) {
        val arr = JSONArray()
        specs.forEach { spec ->
            arr.put(
                JSONObject().apply {
                    put("id", spec.id)
                    put("medicineName", spec.medicineName)
                    put("dosage", spec.dosage)
                    put("mealTiming", spec.mealTiming)
                    put("scheduledTime", spec.scheduledTime)
                    put("ringtoneUri", spec.ringtoneUri)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARMS_JSON, arr.toString())
            .apply()
    }

    private fun loadStoredSpecs(context: Context): List<StoredAlarmSpec> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALARMS_JSON, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        StoredAlarmSpec(
                            id = obj.optString("id"),
                            medicineName = obj.optString("medicineName"),
                            dosage = obj.optString("dosage"),
                            mealTiming = obj.optString("mealTiming"),
                            scheduledTime = obj.optString("scheduledTime"),
                            ringtoneUri = obj.optString("ringtoneUri")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

internal data class StoredAlarmSpec(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val mealTiming: String,
    val scheduledTime: String,
    val ringtoneUri: String
)

class MedicineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: "med_alarm"
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine Reminder"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: "1 Dose"
        val meal = intent.getStringExtra(EXTRA_MEAL) ?: "After Meal"
        val scheduledTime = intent.getStringExtra(EXTRA_TIME) ?: ""
        val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI) ?: ""

        MedicineAlarmScheduler.ensureNotificationChannel(appContext)

        // Play custom alarm ringtone in background
        AlarmSoundPlayer.play(appContext, ringtoneUri)

        // Reschedule next day's recurring dose automatically (skip for one-off snooze)
        if (!reminderId.endsWith("_snooze")) {
            MedicineAlarmScheduler.scheduleNextDailyAlarm(
                context = appContext,
                spec = StoredAlarmSpec(
                    id = reminderId,
                    medicineName = medName,
                    dosage = dosage,
                    mealTiming = meal,
                    scheduledTime = scheduledTime,
                    ringtoneUri = ringtoneUri
                ),
                forceTomorrow = true
            )
        }

        val notifId = reminderId.hashCode()
        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(appContext, AlarmStopReceiver::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_STOP_ALARM)
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            appContext,
            notifId + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(appContext, AlarmStopReceiver::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_SNOOZE_ALARM)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MED_NAME, medName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_MEAL, meal)
            putExtra(EXTRA_TIME, scheduledTime)
            putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            appContext,
            notifId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canNotify) {
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Medicine Time: $medName")
                .setContentText("$dosage • $meal • $scheduledTime")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .setDeleteIntent(stopPendingIntent)
                .addAction(0, "Stop Alarm", stopPendingIntent)
                .addAction(0, "Snooze 10m", snoozePendingIntent)
                .build()

            try {
                NotificationManagerCompat.from(appContext).notify(notifId, notification)
            } catch (e: SecurityException) {
                Log.w(ALARM_TAG, "Notification permission not granted", e)
            }
        }
    }
}

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        AlarmSoundPlayer.stop()
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: "med_alarm"
        NotificationManagerCompat.from(appContext).cancel(reminderId.hashCode())

        if (intent.getStringExtra(EXTRA_ACTION) == ACTION_SNOOZE_ALARM) {
            MedicineAlarmScheduler.scheduleSnooze10Min(
                context = appContext,
                reminderId = reminderId.removeSuffix("_snooze"),
                medicineName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine",
                dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: "1 Dose",
                mealTiming = intent.getStringExtra(EXTRA_MEAL) ?: "After Meal",
                scheduledTime = intent.getStringExtra(EXTRA_TIME) ?: "",
                ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI) ?: ""
            )
        }
    }
}

class BootAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MedicineAlarmScheduler.rescheduleAllFromStorage(context)
        }
    }
}
