package com.amitmatth.challengemonitor.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.databinding.FragmentChallengeDetailsBinding
import com.amitmatth.challengemonitor.model.CalendarDay
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.ChallengeDailyLog
import com.amitmatth.challengemonitor.ui.adapter.CalendarAdapter
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.view.Gravity
import android.widget.LinearLayout

class ChallengeDetailsFragment : Fragment() {

    private var _binding: FragmentChallengeDetailsBinding? = null
    private val binding get() = _binding!!

    private var challengeId: Long = -1
    private lateinit var viewModel: ChallengeViewModel

    private var currentChallenge: Challenge? = null
    private var allLogsForChallenge: List<ChallengeDailyLog> = emptyList()

    private lateinit var calendarAdapter: CalendarAdapter
    private val displayedCalendar = Calendar.getInstance()
    private var selectedCalendarDayForHistory: CalendarDay? = null

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    private val historyTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    private val dbDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val ARG_CHALLENGE_ID = "challengeId"

        fun newInstance(challengeId: Long): ChallengeDetailsFragment {
            val fragment = ChallengeDetailsFragment()
            val args = Bundle()
            args.putLong(ARG_CHALLENGE_ID, challengeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            challengeId = it.getLong(ARG_CHALLENGE_ID, -1)
        }
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (challengeId == -1L) {
            SnackbarUtils.showCustomSnackbar(requireActivity(), "Error: Challenge ID missing.")
            parentFragmentManager.popBackStack()
            return
        }

        setupCalendarRecyclerView()
        setupClickListeners()
        setupObservers()
        Log.d(
            "ChallengeDetailsFragment",
            "onViewCreated: My instance's challengeId is $challengeId. Calling fetchChallengeById."
        )
        viewModel.fetchChallengeById(challengeId)
    }

    private fun setupObservers() {
        viewModel.currentChallenge.observe(viewLifecycleOwner) { challenge ->
            this.currentChallenge = challenge
            if (challenge != null) {
                binding.loadingContainerDetails.visibility = View.GONE
                binding.contentGroupDetails.visibility = View.VISIBLE
                binding.challengeDetailTitleTextView.text = challenge.title
                binding.challengeDetailDescriptionTextView.text =
                    if (challenge.description.isNullOrEmpty()) "No description." else challenge.description
                binding.challengeDetailStartDateTextView.text =
                    "Start: ${challenge.startDate}"
                binding.challengeDetailEndDateTextView.text = "End: ${challenge.endDate}"
                binding.challengeDetailDurationTextView.text = "${challenge.durationDays} Days"

                var initialDateAutoSelected = false
                if (selectedCalendarDayForHistory == null) {
                    val todayCal = Calendar.getInstance()
                    if (isDateWithinChallengeRange(
                            todayCal.time,
                            challenge.startDate,
                            challenge.endDate
                        )
                    ) {
                        displayedCalendar.time = todayCal.time

                        val todayDate = todayCal.time
                        val todayDayOfMonthStr =
                            SimpleDateFormat("d", Locale.getDefault()).format(todayDate)
                        val todayLogDateString = dbDateFormat.format(todayDate)

                        val latestLogForToday = allLogsForChallenge
                            .filter { it.logDate == todayLogDateString }
                            .maxByOrNull { parseLogTime(it.lastUpdatedTime) ?: Date(0) }
                        val todayLogStatus = latestLogForToday?.status

                        selectedCalendarDayForHistory = CalendarDay(
                            date = todayDate,
                            dayOfMonth = todayDayOfMonthStr,
                            isCurrentMonth = true,
                            isChallengeDay = true,
                            logStatus = todayLogStatus,
                            isSelected = true
                        )
                        initialDateAutoSelected = true
                    }
                }

                if (!initialDateAutoSelected && selectedCalendarDayForHistory == null) {
                    val challengeStartDate = challenge.startDate.let { dbDateFormat.parse(it) }
                    val calNow = Calendar.getInstance()
                    if (displayedCalendar.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                        displayedCalendar.get(Calendar.MONTH) == calNow.get(Calendar.MONTH) &&
                        displayedCalendar.get(Calendar.DAY_OF_MONTH) == calNow.get(Calendar.DAY_OF_MONTH) &&
                        challengeStartDate != null
                    ) {
                        displayedCalendar.time = challengeStartDate
                    }
                } else if (selectedCalendarDayForHistory != null) {
                    displayedCalendar.time = selectedCalendarDayForHistory!!.date
                }

                updateCalendar()

                selectedCalendarDayForHistory?.let { handleCalendarDayClick(it) }

            } else {
                binding.loadingContainerDetails.visibility = View.VISIBLE
                binding.contentGroupDetails.visibility = View.GONE
            }
        }

        viewModel.currentChallengeDailyLogs.observe(viewLifecycleOwner) { logs ->
            allLogsForChallenge =
                logs.sortedWith(compareBy({ it.logDate }, { parseLogTime(it.lastUpdatedTime) }))
            currentChallenge?.let { challenge ->
                updateChallengeProgress(challenge, allLogsForChallenge)
                if (selectedCalendarDayForHistory != null) {
                    val selectedDateStr = dbDateFormat.format(selectedCalendarDayForHistory!!.date)
                    val latestLog = allLogsForChallenge
                        .filter { it.logDate == selectedDateStr }
                        .maxByOrNull { parseLogTime(it.lastUpdatedTime) ?: Date(0) }
                    selectedCalendarDayForHistory!!.logStatus = latestLog?.status
                }
            }
            updateCalendar()
            selectedCalendarDayForHistory?.let { handleCalendarDayClick(it) }
        }
    }

    private fun parseLogTime(timeString: String): Date? {
        if (timeString.isBlank()) return null
        return try {
            dbDateTimeFormat.parse(timeString)
        } catch (_: Exception) {
            try {
                fullDateTimeFormat.parse(timeString)
            } catch (_: Exception) {
                try {
                    val dummyDate = "1970-01-01"
                    fullDateTimeFormat.parse("$dummyDate $timeString")
                } catch (_: Exception) {
                    try {
                        historyTimeFormat.parse(timeString)
                    } catch (e4: Exception) {
                        Log.w(
                            "ChallengeDetailsFragment",
                            "Could not parse timeString: $timeString",
                            e4
                        )
                        null
                    }
                }
            }
        }
    }

    private fun updateChallengeProgress(challenge: Challenge, logs: List<ChallengeDailyLog>) {
        val loggedDays = logs
            .filter {
                it.status == ChallengeDbHelper.STATUS_FOLLOWED ||
                        it.status == ChallengeDbHelper.STATUS_NOT_FOLLOWED ||
                        it.status == ChallengeDbHelper.STATUS_SKIPPED
            }
            .distinctBy { it.logDate }
            .count()

        val progressPercentage = if (challenge.durationDays > 0) {
            (loggedDays * 100 / challenge.durationDays)
        } else {
            0
        }
        binding.challengeDetailProgressBar.progress = progressPercentage
        binding.challengeDetailProgressTextView.text =
            "$loggedDays / ${challenge.durationDays} Days"
    }

    private fun setupCalendarRecyclerView() {
        calendarAdapter = CalendarAdapter(requireContext(), ArrayList()) { clickedCalendarDay ->
            selectedCalendarDayForHistory = clickedCalendarDay

            displayedCalendar.time = clickedCalendarDay.date
            updateCalendar()

            handleCalendarDayClick(clickedCalendarDay)
        }
        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }
    }

    private fun handleCalendarDayClick(calendarDay: CalendarDay) {
        val selectedDateStr = dbDateFormat.format(calendarDay.date)
        val displayDate = displayDateFormat.format(calendarDay.date)
        val container = binding.logHistoryContainer
        container.removeAllViews()

        val logsForSelectedDay = allLogsForChallenge
            .filter { it.logDate == selectedDateStr }
            .sortedBy { parseLogTime(it.lastUpdatedTime) }

        if (logsForSelectedDay.isNotEmpty()) {
            logsForSelectedDay.forEach { log ->
                val cardView = layoutInflater.inflate(R.layout.item_log_history, container, false)
                val logDateTextView = cardView.findViewById<TextView>(R.id.logDateTextView)
                val logTimeTextView = cardView.findViewById<TextView>(R.id.logTimeTextView)
                val logStatusTextView = cardView.findViewById<TextView>(R.id.logStatusTextView)
                val logNotesTextView = cardView.findViewById<TextView>(R.id.logNotesTextView)

                logDateTextView.text = displayDate
                val parsedTime = parseLogTime(log.lastUpdatedTime)
                val timeString =
                    parsedTime?.let { historyTimeFormat.format(it) } ?: log.lastUpdatedTime
                logTimeTextView.text = timeString
                logStatusTextView.text = "Status: ${log.status}"
                if (log.notes.isNullOrBlank()) {
                    logNotesTextView.visibility = View.GONE
                } else {
                    logNotesTextView.visibility = View.VISIBLE
                    logNotesTextView.text = "Notes: ${log.notes}"
                }
                container.addView(cardView)
            }
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val message: String
                val challenge = currentChallenge

                if (challenge != null) {
                    val isWithinChallenge = isDateWithinChallengeRange(
                        calendarDay.date,
                        challenge.startDate,
                        challenge.endDate
                    )
                    val todayCal = Calendar.getInstance().apply { clearTime() }
                    val selectedDayCal =
                        Calendar.getInstance().apply { time = calendarDay.date; clearTime() }
                    val isFutureDate = selectedDayCal.after(todayCal)

                    message = if (!isWithinChallenge) {
                        "This date ($displayDate) is outside the challenge period."
                    } else if (isFutureDate) {
                        "This date ($displayDate) is a future date. No logs yet."
                    } else {
                        "No logs for $displayDate. You can mark its status or skip it."
                    }
                } else {
                    message =
                        "Could not load log history for $displayDate. Challenge details unavailable."
                }

                val emptyTextView = TextView(requireContext()).apply {
                    this.text = message
                    setTextColor(Color.LTGRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                    textSize = 16f
                }
                container.addView(emptyTextView)
            }
        }
    }

    private fun setupClickListeners() {
        binding.prevMonthButton.setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        binding.nextMonthButton.setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        binding.editChallengeButton.setOnClickListener {
            currentChallenge?.let { challenge ->
                if (challenge.id == -1L) {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Challenge data not available for editing."
                    )
                    return@setOnClickListener
                }
                val createChallengeFragment = CreateChallengeFragment().apply {
                    arguments = Bundle().apply {
                        putLong("challengeIdToEdit", challenge.id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, createChallengeFragment)
                    .addToBackStack(CreateChallengeFragment::class.java.simpleName)
                    .commit()
            } ?: run {
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Challenge details not loaded yet."
                )
            }
        }

        binding.deleteChallengeButton.setOnClickListener {
            currentChallenge?.let { challenge ->

                val dialogView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_input, null)
                val notesEditText = dialogView.findViewById<TextInputEditText>(R.id.notesEditText)
                val dialogTitleTextView =
                    dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
                val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)
                val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)

                notesEditText.visibility = View.GONE
                saveButton.text = "Delete"
                dialogTitleTextView.text =
                    "Are you sure you want to delete '${challenge.title}'? This action cannot be undone."

                val notesDialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()

                notesDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                saveButton.setOnClickListener {
                    viewModel.deleteChallenge(challenge)
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "'${challenge.title}' deleted"
                    )
                    parentFragmentManager.popBackStack()
                    notesDialog.dismiss()
                }

                cancelButton.setOnClickListener {
                    notesDialog.dismiss()
                }
                notesDialog.show()
            } ?: run {
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Challenge details not loaded yet."
                )
            }
        }

        binding.skipChallengeButton.setOnClickListener {
            currentChallenge?.let { challenge ->
                if (challenge.id == -1L) {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Challenge not properly loaded."
                    )
                    return@setOnClickListener
                }

                val today = Calendar.getInstance()
                val dateToSkip = selectedCalendarDayForHistory?.date ?: today.time
                val dateToSkipStr = dbDateFormat.format(dateToSkip)
                val displayDateToSkip = displayDateFormat.format(dateToSkip)

                if (isDateWithinChallengeRange(
                        dateToSkip,
                        challenge.startDate,
                        challenge.endDate
                    )
                ) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val hasLog = viewModel.hasActionableLogForDate(challenge.id, dateToSkipStr)
                        val showSkipDialog = {
                            val dialogView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.dialog_notes_input, null)
                            val notesEditText =
                                dialogView.findViewById<TextInputEditText>(R.id.notesEditText)
                            val dialogTitleTextView =
                                dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
                            val saveButton =
                                dialogView.findViewById<MaterialButton>(R.id.saveButton)
                            val cancelButton =
                                dialogView.findViewById<MaterialButton>(R.id.cancelButton)

                            dialogTitleTextView.text =
                                "Reason for Skipping $displayDateToSkip (Optional)"
                            notesEditText.visibility = View.VISIBLE
                            saveButton.text = "Skip"

                            val notesDialog = AlertDialog.Builder(requireContext())
                                .setView(dialogView)
                                .create()
                            notesDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                            saveButton.setOnClickListener {
                                val notes = notesEditText.text.toString().trim()
                                viewModel.markChallengeDayStatus(
                                    challenge.id,
                                    dateToSkipStr,
                                    ChallengeDbHelper.STATUS_SKIPPED,
                                    notes
                                )
                                SnackbarUtils.showCustomSnackbar(
                                    requireActivity(),
                                    "$displayDateToSkip marked as SKIPPED for '${challenge.title}'."
                                )
                                notesDialog.dismiss()
                            }
                            cancelButton.setOnClickListener { notesDialog.dismiss() }
                            notesDialog.show()
                        }

                        if (hasLog) {
                            val overrideDialogView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.dialog_notes_input, null)
                            val overrideNotesEditText =
                                overrideDialogView.findViewById<TextInputEditText>(R.id.notesEditText)
                            val overrideDialogTitleTextView =
                                overrideDialogView.findViewById<TextView>(R.id.dialogTitleTextView)
                            val overrideSaveButton =
                                overrideDialogView.findViewById<MaterialButton>(R.id.saveButton)
                            val overrideCancelButton =
                                overrideDialogView.findViewById<MaterialButton>(R.id.cancelButton)

                            overrideNotesEditText.visibility = View.GONE
                            overrideDialogTitleTextView.text =
                                "Override Log for $displayDateToSkip?\nMarking as skipped will override the current status. Continue?"
                            overrideSaveButton.text = "Yes, Override"

                            val overrideAlertDialog = AlertDialog.Builder(requireContext())
                                .setView(overrideDialogView)
                                .create()
                            overrideAlertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                            overrideSaveButton.setOnClickListener {
                                showSkipDialog()
                                overrideAlertDialog.dismiss()
                            }

                            overrideCancelButton.setOnClickListener {
                                overrideAlertDialog.dismiss()
                            }
                            overrideAlertDialog.show()
                        } else {
                            showSkipDialog()
                        }
                    }
                } else {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Cannot skip: $displayDateToSkip is outside the challenge period."
                    )
                }
            } ?: run {
                SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    "Challenge details not loaded yet."
                )
            }
        }
    }

    private fun isDateWithinChallengeRange(
        dateToCheck: Date,
        startDateStr: String?,
        endDateStr: String?
    ): Boolean {
        if (startDateStr.isNullOrEmpty() || endDateStr.isNullOrEmpty()) {
            return false
        }
        try {
            val challengeStartDate = dbDateFormat.parse(startDateStr) ?: return false
            val challengeEndDate = dbDateFormat.parse(endDateStr) ?: return false

            val calToCheck = Calendar.getInstance().apply { time = dateToCheck; clearTime() }
            val calStart = Calendar.getInstance().apply { time = challengeStartDate; clearTime() }
            val calEnd = Calendar.getInstance().apply { time = challengeEndDate; clearTime() }

            return !calToCheck.before(calStart) && !calToCheck.after(calEnd)
        } catch (e: Exception) {
            Log.e("ChallengeDetails", "Error parsing challenge dates for range check: ${e.message}")
            return false
        }
    }

    private fun updateCalendar() {
        if (!isAdded || _binding == null) return

        binding.monthYearTextView.text = monthYearFormat.format(displayedCalendar.time)
        val days = generateCalendarDaysForMonth(displayedCalendar)
        val challengeStartDate = currentChallenge?.startDate?.let { dbDateFormat.parse(it) }
        val challengeEndDate = currentChallenge?.endDate?.let { dbDateFormat.parse(it) }
        calendarAdapter.updateData(days, challengeStartDate, challengeEndDate)
    }

    private fun generateCalendarDaysForMonth(calendar: Calendar): List<CalendarDay> {
        val daysList = ArrayList<CalendarDay>()
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfMonth = monthCalendar.get(Calendar.DAY_OF_WEEK)
        val daysToOffset = (firstDayOfMonth - Calendar.MONDAY + 7) % 7
        monthCalendar.add(Calendar.DAY_OF_MONTH, -daysToOffset)

        val challengeStartDateCal = currentChallenge?.startDate?.let {
            dbDateFormat.parse(it)
                ?.let { d -> Calendar.getInstance().apply { time = d; clearTime() } }
        }
        val challengeEndDateCal = currentChallenge?.endDate?.let {
            dbDateFormat.parse(it)
                ?.let { d -> Calendar.getInstance().apply { time = d; clearTime() } }
        }

        val latestLogsForCalendarDisplay = allLogsForChallenge
            .groupBy { it.logDate }
            .mapValues { entry ->
                entry.value.maxByOrNull { parseLogTime(it.lastUpdatedTime) ?: Date(0) }
            }

        repeat(42) {
            val currentDate = monthCalendar.time
            val dayOfMonthStr = SimpleDateFormat("d", Locale.getDefault()).format(currentDate)
            val isCurrentDisplayMonth =
                monthCalendar.get(Calendar.MONTH) == displayedCalendar.get(Calendar.MONTH) &&
                        monthCalendar.get(Calendar.YEAR) == displayedCalendar.get(Calendar.YEAR)

            var isChallengeDay = false
            if (challengeStartDateCal != null && challengeEndDateCal != null) {
                val currentDayCalStripped =
                    Calendar.getInstance().apply { time = currentDate; clearTime() }
                isChallengeDay =
                    !currentDayCalStripped.before(challengeStartDateCal) && !currentDayCalStripped.after(
                        challengeEndDateCal
                    )
            }

            val logDateString = dbDateFormat.format(currentDate)
            val logStatusForCalendarCell =
                if (isChallengeDay) latestLogsForCalendarDisplay[logDateString]?.status else null

            val isThisDaySelected = selectedCalendarDayForHistory?.let {
                dbDateFormat.format(it.date) == logDateString
            } ?: false

            daysList.add(
                CalendarDay(
                    date = currentDate,
                    dayOfMonth = dayOfMonthStr,
                    isCurrentMonth = isCurrentDisplayMonth,
                    isChallengeDay = isChallengeDay,
                    logStatus = logStatusForCalendarCell,
                    isSelected = isThisDaySelected
                )
            )
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return daysList
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onResume() {
        super.onResume()
        if (challengeId != -1L) {
            binding.loadingContainerDetails.visibility = View.VISIBLE
            binding.contentGroupDetails.visibility = View.GONE

            Log.d(
                "ChallengeDetailsFragment",
                "onResume: My instance's definitive challengeId is $challengeId. Calling fetchChallengeById."
            )
            viewModel.fetchChallengeById(challengeId)
        } else {
            Log.w("ChallengeDetailsFragment", "onResume: challengeId is -1L, not refreshing.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}