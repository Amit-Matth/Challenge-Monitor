package com.amitmatth.challengemonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.amitmatth.challengemonitor.work.AutoSkipWorker

class AutoSkipSchedulerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoSkipScheduler"
        const val ACTION_SCHEDULE_AUTO_SKIP = "com.amitmatth.challengemonitor.ACTION_SCHEDULE_AUTO_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")

        if (intent.action == ACTION_SCHEDULE_AUTO_SKIP) {
            Log.i(TAG, "ACTION_SCHEDULE_AUTO_SKIP received. Enqueuing AutoSkipWorker.")

            val autoSkipWorkRequest = OneTimeWorkRequestBuilder<AutoSkipWorker>()
                .addTag("oneTimeAutoSkip")
                .build()

            WorkManager.getInstance(context).enqueue(autoSkipWorkRequest)
            Log.d(TAG, "AutoSkipWorker enqueued as a one-time request.")
        } else {
            Log.w(TAG, "Received an intent with an unknown or unexpected action: ${intent.action}")
        }
    }
}