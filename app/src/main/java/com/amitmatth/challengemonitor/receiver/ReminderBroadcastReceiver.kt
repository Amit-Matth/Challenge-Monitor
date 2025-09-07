package com.amitmatth.challengemonitor.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.ui.MainActivity
import com.amitmatth.challengemonitor.ui.fragments.OnboardingFragment5
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "ChallengeMonitorReminderChannel"
        const val NOTIFICATION_ID = 1
        private const val TAG = "ReminderReceiver"
        const val ACTION_REMINDER = OnboardingFragment5.ACTION_REMINDER
        const val REMINDER_REQUEST_CODE = OnboardingFragment5.REMINDER_REQUEST_CODE
        const val PREFS_NAME = OnboardingFragment5.PREFS_NAME
        const val KEY_REMINDER_HOUR = OnboardingFragment5.KEY_REMINDER_HOUR
        const val KEY_REMINDER_MINUTE = OnboardingFragment5.KEY_REMINDER_MINUTE
        const val KEY_REMINDER_ENABLED = OnboardingFragment5.KEY_REMINDER_ENABLED
    }

    private fun dateTimeLogFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault())
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive triggered.")
        Log.d(TAG, "Received intent with action: ${intent.action}")

        if (intent.action == ACTION_REMINDER) {
            Log.i(TAG, "Action matches ACTION_REMINDER. Proceeding to show notification.")
            createNotificationChannel(context)

            val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntentFlags =
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val activityPendingIntent =
                PendingIntent.getActivity(context, 0, mainActivityIntent, pendingIntentFlags)

            val notificationTitle = "Challenge Monitor Reminder"
            val notificationText = "Don\'t forget to log your challenge progress today!"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.reminder_icon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.i(TAG, "Notification successfully built and sent with ID: $NOTIFICATION_ID")

            rescheduleReminder(context)

        } else {
            Log.w(TAG, "Received intent with unknown action: ${intent.action}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        val name = "Challenge Monitor Reminders"
        val descriptionText = "Channel for daily challenge reminders"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel '$CHANNEL_ID' created or ensured to exist.")
    }

    private fun rescheduleReminder(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1)
        val minute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1)
        val enabled = sharedPreferences.getBoolean(KEY_REMINDER_ENABLED, false)

        if (!enabled || hour == -1 || minute == -1) {
            Log.d(TAG, "Reminder is disabled or time not set. Not rescheduling.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DATE, 1)
        }

        if (calendar.before(Calendar.getInstance())) {
            Log.d(
                TAG,
                "Calculated time for next day is still in the past (should not happen if adding a day). Adjusting."
            )
            calendar.add(Calendar.DATE, 1)
        }

        val scheduledTimeMillis = calendar.timeInMillis
        Log.d(
            TAG,
            "Rescheduling reminder for: ${dateTimeLogFormat().format(calendar.time)} (Millis: $scheduledTimeMillis)"
        )

        try {
            if (SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(
                        TAG,
                        "Rescheduling exact alarm using setExactAndAllowWhileIdle (Android 12+)."
                    )
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledTimeMillis,
                        pendingIntent
                    )
                } else {
                    Log.w(
                        TAG,
                        "Cannot reschedule exact alarm. SCHEDULE_EXACT_ALARM permission not granted."
                    )
                }
            } else
                Log.d(
                    TAG,
                    "Rescheduling exact alarm using setExactAndAllowWhileIdle (Android 6-11)."
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMillis,
                    pendingIntent
                )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while rescheduling reminder.", e)
        }
    }
}