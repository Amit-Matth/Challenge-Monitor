package com.amitmatth.challengemonitor.ui.fragments

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentSettingsBinding
import com.amitmatth.challengemonitor.receiver.ReminderBroadcastReceiver
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.amitmatth.challengemonitor.ui.MainActivity
import com.amitmatth.challengemonitor.utils.SnackbarUtils

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private var reminderHour: Int = -1
    private var reminderMinute: Int = -1
    private var isReminderEnabled: Boolean = false

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
        private const val TAG = "SettingsFragment"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (reminderHour != -1 && reminderMinute != -1) {
                    val scheduledSuccessfully = scheduleReminder(reminderHour, reminderMinute)
                    saveReminderSettings(reminderHour, reminderMinute, scheduledSuccessfully)
                    if (!scheduledSuccessfully && binding.reminderSwitch.isChecked) {
                        binding.reminderSwitch.isChecked = false
                    }
                }
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Notification permission granted."
                )
            } else {
                activity?.let {
                    SnackbarUtils.showCustomSnackbar(
                        it,
                        "Notification permission denied. Reminder cannot be set."
                    )
                }
                binding.reminderSwitch.isChecked = false
                saveReminderSettings(reminderHour, reminderMinute, false)
            }
            updateReminderUI()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadProfileData()
        loadReminderSettings()
        updateReminderUI()
        setupListeners()
    }

    private fun loadProfileData() {
        val userName = sharedPreferences.getString("user_name", "User Name")
        val imageUriString = sharedPreferences.getString("profile_image_uri", null)

        binding.settingsUsernameTextView.text = userName
        Glide.with(this)
            .load(imageUriString)
            .placeholder(R.drawable.outline_account_circle_24)
            .error(R.drawable.outline_account_circle_24)
            .circleCrop()
            .into(binding.settingsProfileImageView)
        Log.d(TAG, "Loaded profile data - Username: $userName, ImageURI: $imageUriString")
    }

    private fun loadReminderSettings() {
        reminderHour = sharedPreferences.getInt(KEY_REMINDER_HOUR, -1)
        reminderMinute = sharedPreferences.getInt(KEY_REMINDER_MINUTE, -1)
        isReminderEnabled = sharedPreferences.getBoolean(KEY_REMINDER_ENABLED, false)
        Log.d(
            TAG,
            "Loaded reminder settings - Hour: $reminderHour, Minute: $reminderMinute, Enabled: $isReminderEnabled"
        )
    }

    private fun saveReminderSettings(hour: Int, minute: Int, enabled: Boolean) {
        sharedPreferences.edit().apply {
            putInt(KEY_REMINDER_HOUR, hour)
            putInt(KEY_REMINDER_MINUTE, minute)
            putBoolean(KEY_REMINDER_ENABLED, enabled)
            apply()
        }
        this.reminderHour = hour
        this.reminderMinute = minute
        this.isReminderEnabled = enabled
        Log.d(TAG, "Saved reminder settings - Hour: $hour, Minute: $minute, Enabled: $enabled")
    }

    private fun updateReminderUI() {
        if (reminderHour != -1 && reminderMinute != -1) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, reminderHour)
                set(Calendar.MINUTE, reminderMinute)
            }
            binding.reminderTimeTextView.text = timeFormat.format(calendar.time)
        } else {
            binding.reminderTimeTextView.text = "Tap to set"
        }
        binding.reminderSwitch.isChecked = isReminderEnabled
        Log.d(
            TAG,
            "UI Updated. Reminder time text: ${binding.reminderTimeTextView.text}, Switch checked: ${binding.reminderSwitch.isChecked}"
        )
    }

    private fun setupListeners() {
        binding.editProfileButton.setOnClickListener {
            (requireActivity() as MainActivity).loadFragment(
                EditProfileFragment(),
                EditProfileFragment::class.java.name
            )
        }

        binding.reminderTimeTextView.setOnClickListener {
            showTimePickerDialog()
        }

        binding.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Reminder switch toggled. IsChecked: $isChecked")
            if (isChecked) {
                if (reminderHour == -1 || reminderMinute == -1) {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Please set a reminder time first."
                    )
                    binding.reminderSwitch.isChecked = false
                    Log.d(TAG, "Reminder time not set, reverting switch.")
                    return@setOnCheckedChangeListener
                }

                var schedulingAttemptedAndSuccessful = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            Log.d(TAG, "Notification permission already granted (Android 13+).")
                            schedulingAttemptedAndSuccessful =
                                scheduleReminder(reminderHour, reminderMinute)
                        }

                        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                            SnackbarUtils.showCustomSnackbar(
                                requireActivity(),
                                "Notification permission is needed to show reminders."
                            )
                            Log.d(
                                TAG,
                                "Showing rationale for notification permission (Android 13+)."
                            )
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        else -> {
                            Log.d(TAG, "Requesting notification permission (Android 13+).")
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    Log.d(
                        TAG,
                        "No runtime POST_NOTIFICATIONS permission needed (Below Android 13)."
                    )
                    schedulingAttemptedAndSuccessful =
                        scheduleReminder(reminderHour, reminderMinute)
                }
                saveReminderSettings(reminderHour, reminderMinute, schedulingAttemptedAndSuccessful)
                if (!schedulingAttemptedAndSuccessful) {
                    binding.reminderSwitch.isChecked = false
                }
            } else {
                cancelReminder()
                saveReminderSettings(reminderHour, reminderMinute, false)
            }
            updateReminderUI()
        }
    }

    private fun showTimePickerDialog() {
        val currentCalendar = Calendar.getInstance()
        val initialHour =
            if (reminderHour != -1) reminderHour else currentCalendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute =
            if (reminderMinute != -1) reminderMinute else currentCalendar.get(Calendar.MINUTE)
        Log.d(TAG, "Showing TimePickerDialog. Initial hour: $initialHour, minute: $initialMinute")

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                Log.d(TAG, "Time selected: $selectedHour:$selectedMinute")

                val switchIsCurrentlyOn = binding.reminderSwitch.isChecked
                var finalEnabledState = switchIsCurrentlyOn

                if (switchIsCurrentlyOn) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        SnackbarUtils.showCustomSnackbar(
                            requireActivity(),
                            "Please grant notification permission to enable reminders."
                        )
                        Log.d(
                            TAG,
                            "Time picked, but POST_NOTIFICATIONS permission needed. Requesting."
                        )
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        finalEnabledState = false
                    } else {
                        val scheduledSuccessfully = scheduleReminder(selectedHour, selectedMinute)
                        finalEnabledState = scheduledSuccessfully
                    }
                } else {
                    Log.d(TAG, "Time picked, but switch is OFF. Saving time, not scheduling.")
                }

                saveReminderSettings(selectedHour, selectedMinute, finalEnabledState)

                if (switchIsCurrentlyOn && !finalEnabledState) {
                    binding.reminderSwitch.isChecked = false
                }
                updateReminderUI()
            },
            initialHour,
            initialMinute,
            false
        )
        timePickerDialog.show()
    }

    private fun scheduleReminder(hour: Int, minute: Int): Boolean {
        Log.d(TAG, "scheduleReminder called for $hour:$minute")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REMINDER_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
                Log.d(TAG, "Scheduled time was in the past for today, moved to tomorrow.")
            }
        }

        val scheduledTimeMillis = calendar.timeInMillis
        Log.d(
            TAG,
            "Calculated schedule time: ${dateTimeLogFormat.format(calendar.time)} (Millis: $scheduledTimeMillis)"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(
                        TAG,
                        "Scheduling exact alarm using setExactAndAllowWhileIdle (Android 12+)."
                    )
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
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Please allow exact alarms from app settings to set reminders."
                    )
                    Log.w(
                        TAG,
                        "SCHEDULE_EXACT_ALARM permission not granted. Taking user to settings."
                    )
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data =
                            android.net.Uri.fromParts("package", requireContext().packageName, null)
                        startActivity(this)
                    }
                    return false
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "Scheduling exact alarm using setExactAndAllowWhileIdle (Android 6-11).")
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
                Log.d(TAG, "Scheduling exact alarm using setExact (Below Android 6).")
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

    private fun cancelReminder() {
        Log.d(TAG, "cancelReminder called.")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REMINDER_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Reminder cancel command sent.")
        SnackbarUtils.showCustomSnackbar(requireActivity(), "Reminder cancelled.")
    }

    override fun onResume() {
        super.onResume()

        val enabled = isReminderEnabled

        if (enabled) {
            var permissionOk = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionOk = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager =
                    requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                permissionOk = permissionOk && alarmManager.canScheduleExactAlarms()
            }

            if (permissionOk) {
                binding.reminderSwitch.isChecked = true
                saveReminderSettings(reminderHour, reminderMinute, true)
            } else {
                binding.reminderSwitch.isChecked = false
                saveReminderSettings(reminderHour, reminderMinute, false)
            }
        }

        updateReminderUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView called, binding set to null.")
    }
}