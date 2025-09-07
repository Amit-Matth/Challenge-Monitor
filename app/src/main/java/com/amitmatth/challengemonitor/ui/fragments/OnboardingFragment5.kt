package com.amitmatth.challengemonitor.ui.fragments

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.amitmatth.challengemonitor.databinding.FragmentOnboarding5Binding
import com.amitmatth.challengemonitor.receiver.ReminderBroadcastReceiver
import com.amitmatth.challengemonitor.ui.OnboardingActivity
import com.amitmatth.challengemonitor.ui.listeners.OnboardingValidationListener
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import java.text.SimpleDateFormat
import java.util.*

class OnboardingFragment5 : Fragment(), OnboardingValidationListener {

    private var _binding: FragmentOnboarding5Binding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private var reminderHour: Int = -1
    private var reminderMinute: Int = -1
    private var isReminderEnabled: Boolean = false
    private var hasUserAttemptedToFinish: Boolean = false

    private var waitingForExactAlarmSettings: Boolean = false

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeLogFormat =
        SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault())

    companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_REMINDER_HOUR = "reminderHour"
        const val KEY_REMINDER_MINUTE = "reminderMinute"
        const val KEY_REMINDER_ENABLED = "reminderEnabled"
        const val REMINDER_REQUEST_CODE = 123
        const val ACTION_REMINDER = "com.amitmatth.challengemonitor.ACTION_REMINDER"
        private const val TAG = "OnboardingFragment5"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val selectedHour = binding.timePicker.hour
            val selectedMinute = binding.timePicker.minute

            if (isGranted) {
                Log.d(TAG, "Notification permission granted by user.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager =
                        requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (alarmManager.canScheduleExactAlarms()) {
                        Log.d(
                            TAG,
                            "Exact alarm permission already available after notification grant. Scheduling and completing."
                        )
                        val scheduled = scheduleReminder(selectedHour, selectedMinute)
                        saveReminderSettings(selectedHour, selectedMinute, scheduled)
                        (activity as? OnboardingActivity)?.completeOnboarding()
                    } else {
                        Log.d(
                            TAG,
                            "Exact alarm permission NOT available after notification grant. Guiding to settings."
                        )
                        openExactAlarmSettings()
                    }
                } else {
                    Log.d(TAG, "Below S: scheduling reminder and completing onboarding.")
                    val scheduled = scheduleReminder(selectedHour, selectedMinute)
                    saveReminderSettings(selectedHour, selectedMinute, scheduled)
                    (activity as? OnboardingActivity)?.completeOnboarding()
                }
            } else {
                Log.d(TAG, "Notification permission denied by user.")
                saveReminderSettings(selectedHour, selectedMinute, false)
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Notification permission denied. Reminder not set."
                )
                if (hasUserAttemptedToFinish) {
                    (activity as? OnboardingActivity)?.completeOnboarding()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding5Binding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadReminderSettings()

        binding.skipSetupButton.setOnClickListener {
            hasUserAttemptedToFinish = true
            (activity as? OnboardingActivity)?.setPageValidation(4, true)
            val selectedHour = binding.timePicker.hour
            val selectedMinute = binding.timePicker.minute
            saveReminderSettings(selectedHour, selectedMinute, false)
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Reminder time saved, but notifications are off. You can enable them later."
            )
            Log.d(
                TAG,
                "Skip & Finish: Saved time $selectedHour:$selectedMinute, reminder disabled."
            )
            (activity as? OnboardingActivity)?.completeOnboarding()
        }

        binding.finishSetupButton.setOnClickListener {
            hasUserAttemptedToFinish = true
            (activity as? OnboardingActivity)?.setPageValidation(4, true)
            val selectedHour = binding.timePicker.hour
            val selectedMinute = binding.timePicker.minute

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        Log.d(TAG, "Notification permission already granted when Finish clicked.")

                        val alarmManager =
                            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (alarmManager.canScheduleExactAlarms()) {
                            Log.d(
                                TAG,
                                "Exact alarm permission already granted. Scheduling and completing."
                            )
                            val scheduled = scheduleReminder(selectedHour, selectedMinute)
                            saveReminderSettings(selectedHour, selectedMinute, scheduled)
                            (activity as? OnboardingActivity)?.completeOnboarding()
                        } else {
                            Log.d(TAG, "Exact alarm permission NOT granted. Guiding to settings.")
                            openExactAlarmSettings()
                        }
                    }

                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        SnackbarUtils.showCustomSnackbar(
                            requireActivity(),
                            "Notification permission is needed for reminders."
                        )
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                checkExactAlarmPermissionAndSchedule(selectedHour, selectedMinute)
            }
        }
    }

    /**
     * This method will attempt to schedule a reminder if possible.
     * For Android S+: if exact-alarm is already available we schedule and complete.
     * If not available we open settings (via openExactAlarmSettings) and wait for the user.
     */
    private fun checkExactAlarmPermissionAndSchedule(hour: Int, minute: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager =
                requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG, "Exact alarm permission granted.")
                val scheduled = scheduleReminder(hour, minute)
                saveReminderSettings(hour, minute, scheduled)
                (activity as? OnboardingActivity)?.completeOnboarding()
            } else {
                Log.d(TAG, "Exact alarm permission NOT granted. Guiding to settings.")
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Please enable 'Alarms & reminders' in settings for reliable reminders."
                )
                openExactAlarmSettings()
            }
        } else {
            Log.d(TAG, "Below Android S, no specific exact alarm permission check needed here.")
            val scheduled = scheduleReminder(hour, minute)
            saveReminderSettings(hour, minute, scheduled)
            if (scheduled) {
                (activity as? OnboardingActivity)?.completeOnboarding()
            } else {
                Log.w(TAG, "scheduleReminder returned false on older OS.")
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Could not set reminder. Please try again."
                )
                (activity as? OnboardingActivity)?.completeOnboarding()
            }
        }
    }

    /**
     * Open system exact alarm settings for this app so the user can grant 'Alarms & reminders'.
     * Sets waitingForExactAlarmSettings = true so onResume() knows we opened settings intentionally.
     */
    private fun openExactAlarmSettings() {
        try {
            waitingForExactAlarmSettings = true
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${requireContext().packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to open exact alarm settings, falling back to app details settings.",
                e
            )
            waitingForExactAlarmSettings = true
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun saveReminderSettings(hour: Int, minute: Int, enabled: Boolean) {
        sharedPreferences.edit {
            putInt(KEY_REMINDER_HOUR, hour)
            putInt(KEY_REMINDER_MINUTE, minute)
            putBoolean(KEY_REMINDER_ENABLED, enabled)
        }
        this.reminderHour = hour
        this.reminderMinute = minute
        this.isReminderEnabled = enabled
        Log.d(TAG, "Saved reminder - Hour: $hour, Minute: $minute, Enabled: $enabled")
    }

    private fun loadReminderSettings() {
        val storedHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1)
        val storedMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1)

        if (storedHour != -1 && storedMinute != -1) {
            binding.timePicker.hour = storedHour
            binding.timePicker.minute = storedMinute
        } else {
            binding.timePicker.hour = 9
            binding.timePicker.minute = 0
        }
        Log.d(TAG, "Loaded time picker to $storedHour:$storedMinute")
    }

    private fun scheduleReminder(hour: Int, minute: Int): Boolean {
        Log.d(TAG, "scheduleReminder called for $hour:$minute")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), REMINDER_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }
        val scheduledTimeMillis = calendar.timeInMillis
        Log.d(TAG, "Calculated schedule time: ${dateTimeLogFormat.format(calendar.time)}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledTimeMillis,
                        pendingIntent
                    )
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Reminder set for ${timeFormat.format(calendar.time)}."
                    )
                    return true
                } else {
                    Log.w(
                        TAG,
                        "Attempted to schedule reminder without exact alarm permission on S+."
                    )
                    return false
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMillis,
                    pendingIntent
                )
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Reminder set for ${timeFormat.format(calendar.time)}."
                )
                return true
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTimeMillis, pendingIntent)
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Reminder set for ${timeFormat.format(calendar.time)}."
                )
                return true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling reminder.", e)
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Could not schedule reminder due to security restrictions."
            )
            return false
        }
    }

    override fun isInputValid(): Boolean = hasUserAttemptedToFinish

    override fun onResume() {
        super.onResume()
        if (hasUserAttemptedToFinish) {
            Log.d(TAG, "onResume: hasUserAttemptedToFinish is true. Finalizing onboarding attempt.")
            val selectedHour = binding.timePicker.hour
            val selectedMinute = binding.timePicker.minute

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(
                        TAG,
                        "onResume: Notification permission NOT granted. Saving reminder disabled, completing."
                    )
                    saveReminderSettings(selectedHour, selectedMinute, false)
                    (activity as? OnboardingActivity)?.completeOnboarding()
                    waitingForExactAlarmSettings = false
                    return
                }
                Log.d(TAG, "onResume: Notification permission GRANTED.")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager =
                    requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.d(TAG, "onResume: Exact alarm permission NOT granted.")
                    if (waitingForExactAlarmSettings) {
                        waitingForExactAlarmSettings = false
                        saveReminderSettings(selectedHour, selectedMinute, false)
                        SnackbarUtils.showCustomSnackbar(
                            requireActivity(),
                            "Exact alarm not enabled. You can enable it in Settings or press Skip to finish onboarding."
                        )
                        Log.d(
                            TAG,
                            "onResume: user returned from settings without enabling exact alarms — staying on onboarding."
                        )
                        return
                    } else {
                        saveReminderSettings(selectedHour, selectedMinute, false)
                        SnackbarUtils.showCustomSnackbar(
                            requireActivity(),
                            "Reminder not set. 'Alarms & reminders' permission is still needed."
                        )
                        (activity as? OnboardingActivity)?.completeOnboarding()
                        return
                    }
                }
                Log.d(TAG, "onResume: Exact alarm permission GRANTED.")
            }

            Log.d(TAG, "onResume: All necessary permissions appear granted. Scheduling reminder.")
            val scheduled = scheduleReminder(selectedHour, selectedMinute)
            saveReminderSettings(selectedHour, selectedMinute, scheduled)
            waitingForExactAlarmSettings = false

            Log.d(TAG, "onResume: Completing onboarding process.")
            (activity as? OnboardingActivity)?.completeOnboarding()
        } else {
            Log.d(TAG, "onResume: hasUserAttemptedToFinish is false. No action.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}