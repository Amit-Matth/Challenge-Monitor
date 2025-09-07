package com.amitmatth.challengemonitor.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.data.ChallengeRepository
import com.amitmatth.challengemonitor.model.ChallengeDailyLog
import com.amitmatth.challengemonitor.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AutoSkipWorker(private val appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        private const val CHANNEL_ID = "ChallengeMonitorReminderChannel"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_NAME = "Challenge Monitor Reminders"
        private const val CHANNEL_DESCRIPTION = "Channel for daily challenge reminders"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = ChallengeRepository(applicationContext)
        val dbHelper = ChallengeDbHelper(applicationContext)

        val executionTimeCalendar = Calendar.getInstance()
        val targetDateCalendar = Calendar.getInstance()

        if (executionTimeCalendar.get(Calendar.HOUR_OF_DAY) < 3) {
            targetDateCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        val targetDateStr =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(targetDateCalendar.time)

        Log.d(
            "AutoSkipWorker",
            "Worker starting for target date: $targetDateStr (Execution time: ${
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(executionTimeCalendar.time)
            })"
        )
        showAutoSkipNotification("Checking for unlogged challenges for $targetDateStr...")

        try {
            val unloggedChallenges = dbHelper.getUnloggedChallengesForDate(targetDateStr)

            if (unloggedChallenges.isEmpty()) {
                Log.d("AutoSkipWorker", "No unlogged challenges found for $targetDateStr.")
                updateNotification("Auto-skip complete. No challenges needed skipping for $targetDateStr.")
            } else {
                Log.d(
                    "AutoSkipWorker",
                    "Found ${unloggedChallenges.size} unlogged challenges for $targetDateStr. Marking as skipped."
                )
                updateNotification("Auto-skipping ${unloggedChallenges.size} challenge(s) for $targetDateStr...")

                unloggedChallenges.forEach { challenge ->
                    val skipLog = ChallengeDailyLog(
                        challengeId = challenge.id,
                        logDate = targetDateStr,
                        status = ChallengeDbHelper.STATUS_SKIPPED,
                        notes = "Automatically skipped by system."
                    )
                    dbHelper.addDailyLog(skipLog)
                    Log.d(
                        "AutoSkipWorker",
                        "Challenge ID ${challenge.id} ('''${challenge.title}''') marked as SKIPPED for $targetDateStr."
                    )

                    repository.updateChallengeProgressAndStatus(challenge.id, targetDateStr)
                }
                updateNotification("Finished auto-skipping ${unloggedChallenges.size} challenge(s) for $targetDateStr.")
            }
            Log.d("AutoSkipWorker", "Worker finished successfully for $targetDateStr.")
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("AutoSkipWorker", "Error in AutoSkipWorker for $targetDateStr: ${e.message}", e)
            updateNotification("Error during auto-skip for $targetDateStr. Please check logs.")
            return@withContext Result.failure()
        }
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }
        val notificationManager: NotificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("AutoSkipWorker", "Notification channel $CHANNEL_ID ensured.")
    }

    private fun showAutoSkipNotification(contentText: String) {
        createNotificationChannel()

        val mainActivityIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val activityPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            mainActivityIntent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.reminder_icon)
            .setContentTitle("Challenge Monitor - Auto Skip")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        try {
            with(NotificationManagerCompat.from(appContext)) {
                notify(NOTIFICATION_ID, builder.build())
                Log.d("AutoSkipWorker", "Notification $NOTIFICATION_ID shown: $contentText")
            }
        } catch (e: SecurityException) {
            Log.e(
                "AutoSkipWorker",
                "SecurityException when trying to show notification. Missing POST_NOTIFICATIONS permission? Error: ${e.message}"
            )
        }
    }

    private fun updateNotification(contentText: String) {
        showAutoSkipNotification(contentText)
    }
}