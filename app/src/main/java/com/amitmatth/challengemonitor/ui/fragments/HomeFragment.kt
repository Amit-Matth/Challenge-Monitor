package com.amitmatth.challengemonitor.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.databinding.FragmentHomeBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.DateModel
import com.amitmatth.challengemonitor.ui.adapter.ChallengeAdapter
import com.amitmatth.challengemonitor.ui.adapter.MonthCalendarAdapter
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.drawable.toDrawable

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: MonthCalendarAdapter
    private lateinit var viewModel: ChallengeViewModel

    private lateinit var unloggedChallengeAdapter: ChallengeAdapter
    private lateinit var loggedChallengeAdapter: ChallengeAdapter
    private lateinit var concludingChallengeAdapter: ChallengeAdapter

    private var isAutoChangingMonth = false
    private lateinit var selectedDate: Calendar
    private lateinit var currentDisplayedMonthCalendar: Calendar

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private var loggedDataLoaded = false
    private var unloggedDataLoaded = false
    private var concludingTodayDataLoaded = false
    private var isLoadingData = false

    companion object {
        private const val SELECTED_DATE_KEY = "selected_date"
        private const val DISPLAYED_MONTH_KEY = "displayed_month"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            val selectedTime = savedInstanceState.getLong(SELECTED_DATE_KEY, -1L)
            val displayedMonthTime = savedInstanceState.getLong(DISPLAYED_MONTH_KEY, -1L)

            selectedDate = Calendar.getInstance().apply {
                if (selectedTime != -1L) timeInMillis = selectedTime else clearTime()
            }
            currentDisplayedMonthCalendar = Calendar.getInstance().apply {
                if (displayedMonthTime != -1L) timeInMillis = displayedMonthTime else clearTime()
            }
        } else {
            selectedDate = Calendar.getInstance().apply { clearTime() }
            currentDisplayedMonthCalendar = Calendar.getInstance().apply { clearTime() }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::selectedDate.isInitialized) {
            outState.putLong(SELECTED_DATE_KEY, selectedDate.timeInMillis)
        }
        if (::currentDisplayedMonthCalendar.isInitialized) {
            outState.putLong(DISPLAYED_MONTH_KEY, currentDisplayedMonthCalendar.timeInMillis)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSectionRecyclerViews()
        val isInitialViewCreation = savedInstanceState == null && !::calendarAdapter.isInitialized
        setupMonthCalendarView(isInitial = isInitialViewCreation)
        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.loggedChallenges.observe(viewLifecycleOwner) { loggedList ->
            viewLifecycleOwner.lifecycleScope.launch {
                val currentSelectedDateStr = dbDateFormat.format(selectedDate.time)
                val statusMap = mutableMapOf<Long, String>()
                loggedList?.forEach { challenge ->
                    statusMap[challenge.id] =
                        viewModel.getLatestStatusForDate(challenge.id, currentSelectedDateStr)
                            ?: ChallengeDbHelper.STATUS_PENDING
                }
                _binding?.let { b ->
                    if (isAdded && ::loggedChallengeAdapter.isInitialized) {
                        b.emptyStateLoggedTextView.text = getString(
                            R.string.no_logged_challenges_for_date,
                            displayDateFormat.format(selectedDate.time)
                        )
                        val isEmpty = loggedList.isNullOrEmpty()
                        b.emptyStateLoggedTextView.visibility =
                            if (isEmpty) View.VISIBLE else View.GONE
                        b.loggedChallengesRecyclerView.visibility =
                            if (isEmpty) View.GONE else View.VISIBLE
                        loggedChallengeAdapter.updateChallenges(
                            loggedList ?: emptyList(),
                            currentSelectedDateStr,
                            statusMap,
                            emptyMap()
                        )
                    }
                }
                loggedDataLoaded = true
                checkAndShowContent()
            }
        }

        viewModel.notLoggedChallenges.observe(viewLifecycleOwner) { unloggedList ->
            val currentSelectedDateStr = dbDateFormat.format(selectedDate.time)
            val statusMap = mutableMapOf<Long, String>()
            unloggedList?.forEach { challenge ->
                statusMap[challenge.id] = ChallengeDbHelper.STATUS_PENDING
            }
            _binding?.let { b ->
                if (isAdded && ::unloggedChallengeAdapter.isInitialized) {
                    b.emptyStateUnloggedTextView.text = getString(
                        R.string.no_unlogged_challenges_for_date,
                        displayDateFormat.format(selectedDate.time)
                    )
                    val isEmpty = unloggedList.isNullOrEmpty()
                    b.emptyStateUnloggedTextView.visibility =
                        if (isEmpty) View.VISIBLE else View.GONE
                    b.unloggedChallengesRecyclerView.visibility =
                        if (isEmpty) View.GONE else View.VISIBLE
                    unloggedChallengeAdapter.updateChallenges(
                        unloggedList ?: emptyList(),
                        currentSelectedDateStr,
                        statusMap,
                        emptyMap()
                    )
                }
            }
            unloggedDataLoaded = true
            checkAndShowContent()
        }

        viewModel.concludingChallengesForDate.observe(viewLifecycleOwner) { concludingList ->
            viewLifecycleOwner.lifecycleScope.launch {
                val currentSelectedDateStr = dbDateFormat.format(selectedDate.time)
                val dailyStatusMapForConcluding = mutableMapOf<Long, String>()
                concludingList?.forEach { challenge ->
                    dailyStatusMapForConcluding[challenge.id] =
                        viewModel.getLatestStatusForDate(challenge.id, currentSelectedDateStr)
                            ?: ChallengeDbHelper.STATUS_PENDING
                }

                _binding?.let { b ->
                    if (isAdded && ::concludingChallengeAdapter.isInitialized) {
                        b.emptyStateCompletedTextView.text = getString(
                            R.string.no_completed_challenges_for_date,
                            displayDateFormat.format(selectedDate.time)
                        )
                        val isEmpty = concludingList.isNullOrEmpty()
                        b.emptyStateCompletedTextView.visibility =
                            if (isEmpty) View.VISIBLE else View.GONE
                        b.completedChallengesRecyclerView.visibility =
                            if (isEmpty) View.GONE else View.VISIBLE
                        concludingChallengeAdapter.updateChallenges(
                            concludingList ?: emptyList(),
                            currentSelectedDateStr,
                            dailyStatusMapForConcluding,
                            emptyMap()
                        )
                    }
                }
                concludingTodayDataLoaded = true
                checkAndShowContent()
            }
        }
    }

    private fun checkAndShowContent() {
        if (loggedDataLoaded && unloggedDataLoaded && concludingTodayDataLoaded) {
            _binding?.loadingContainer?.visibility = View.GONE
            _binding?.contentGroup?.visibility = View.VISIBLE
            isLoadingData = false
        } else {
            _binding?.contentGroup?.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (::calendarAdapter.isInitialized) {
            Log.d(
                "HomeFragment",
                "onResume: Calendar adapter initialized, re-setting up month calendar view for persisted date."
            )
            setupMonthCalendarView(isInitial = false)
        } else {
            Log.d(
                "HomeFragment",
                "onResume: Calendar adapter NOT initialized. Load should have been triggered by onViewCreated."
            )
        }
    }

    override fun onPause() {
        super.onPause()
        isLoadingData = false
        Log.d("HomeFragment", "onPause: isLoadingData reset to false.")
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
    }

    private fun setupClickListeners() {
        _binding?.prevMonthButton?.setOnClickListener { navigateToPreviousMonth() }
        _binding?.nextMonthButton?.setOnClickListener { navigateToNextMonth() }
        _binding?.unloggedInfoIcon?.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Challenges for the selected date that haven\'t been marked Done, Not Done, or Skipped yet.",
                5000
            )
        }
        _binding?.loggedInfoIcon?.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Challenges for the selected date that have already been marked Done, Not Done, or Skipped.",
                5000
            )
        }
        _binding?.completedInfoIcon?.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Active challenges whose scheduled end date is today.",
                5000
            )
        }
    }

    private fun updateMonthYearTextView() {
        if (!::currentDisplayedMonthCalendar.isInitialized) return
        _binding?.monthYearTextView?.text =
            monthYearFormat.format(currentDisplayedMonthCalendar.time)
    }

    private fun navigateToPreviousMonth() {
        if (isAutoChangingMonth) return
        isAutoChangingMonth = true
        currentDisplayedMonthCalendar.add(Calendar.MONTH, -1)
        selectedDate = currentDisplayedMonthCalendar.clone() as Calendar
        selectedDate.set(Calendar.DAY_OF_MONTH, 1)
        selectedDate.clearTime()
        setupMonthCalendarView()
        _binding?.monthCalendarRecyclerView?.postDelayed({ isAutoChangingMonth = false }, 200)
    }

    private fun navigateToNextMonth() {
        if (isAutoChangingMonth) return
        isAutoChangingMonth = true
        currentDisplayedMonthCalendar.add(Calendar.MONTH, 1)
        selectedDate = currentDisplayedMonthCalendar.clone() as Calendar
        selectedDate.set(Calendar.DAY_OF_MONTH, 1)
        selectedDate.clearTime()
        setupMonthCalendarView()
        _binding?.monthCalendarRecyclerView?.postDelayed({ isAutoChangingMonth = false }, 200)
    }

    private fun setupMonthCalendarView(isInitial: Boolean = false) {
        if (!::currentDisplayedMonthCalendar.isInitialized || !::selectedDate.isInitialized) {
            selectedDate = Calendar.getInstance().apply { clearTime() }
            currentDisplayedMonthCalendar = (selectedDate.clone() as Calendar).apply { clearTime() }
            Log.w(
                "HomeFragment",
                "setupMonthCalendarView: currentDisplayedMonthCalendar or selectedDate was not initialized. Resetting to today."
            )
        }
        updateMonthYearTextView()
        val (dateList, _) = generateCalendarDays(currentDisplayedMonthCalendar, selectedDate)

        if (!::calendarAdapter.isInitialized || isInitial) {
            calendarAdapter = MonthCalendarAdapter(dateList) { selectedDateModel ->
                val newSelectedDate = selectedDateModel.calendar.clone() as Calendar
                newSelectedDate.clearTime()
                this.selectedDate = newSelectedDate
                calendarAdapter.setSelectedDate(this.selectedDate)

                if (selectedDateModel.isCurrentMonth) {
                    lifecycleScope.launch { displayChallengesForSelectedDate() }
                    centerCurrentDateInCalendar()
                } else {
                    isAutoChangingMonth = true
                    currentDisplayedMonthCalendar = newSelectedDate.clone() as Calendar
                    setupMonthCalendarView()
                    _binding?.monthCalendarRecyclerView?.postDelayed({
                        isAutoChangingMonth = false
                    }, 250)
                }
            }
            Log.d("HomeFragment", "setupMonthCalendarView: CalendarAdapter instance (re)created.")
        } else {
            calendarAdapter.updateDates(dateList)
            Log.d("HomeFragment", "setupMonthCalendarView: Existing CalendarAdapter data updated.")
        }

        _binding?.monthCalendarRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        _binding?.monthCalendarRecyclerView?.adapter = calendarAdapter
        Log.d(
            "HomeFragment",
            "setupMonthCalendarView: Adapter and LayoutManager (re)set on RecyclerView."
        )

        if (::calendarAdapter.isInitialized) {
            calendarAdapter.setSelectedDate(this.selectedDate)
        Log.d(
                "HomeFragment",
                "setupMonthCalendarView: setSelectedDate called on adapter."
            )
        }

        lifecycleScope.launch { displayChallengesForSelectedDate() }

        if (isInitial) {
            _binding?.monthCalendarRecyclerView?.post {
                if (_binding != null && isAdded && ::calendarAdapter.isInitialized) {
                    centerCurrentDateInCalendar()
                }
            }
        }
    }

    private fun generateCalendarDays(
        monthToDisplay: Calendar,
        currentSelectedDate: Calendar
    ): Pair<List<DateModel>, Int> {
        val days = mutableListOf<DateModel>()
        var todayDateIndexInList = -1
        val realTodayCal = Calendar.getInstance().apply { clearTime() }
        val tempCalendar = monthToDisplay.clone() as Calendar
        tempCalendar.clearTime()
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val currentDisplayMonthValue = tempCalendar.get(Calendar.MONTH)
        val currentDisplayYearValue = tempCalendar.get(Calendar.YEAR)
        val firstDayOfMonthWeekValue = tempCalendar.get(Calendar.DAY_OF_WEEK)
        tempCalendar.add(Calendar.DAY_OF_MONTH, -(firstDayOfMonthWeekValue - Calendar.SUNDAY))
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateNumFormat = SimpleDateFormat("d", Locale.getDefault())
        val normalizedSelectedDateForComparison =
            (currentSelectedDate.clone() as Calendar).apply { clearTime() }

        repeat(42) {
            val dayCal = tempCalendar.clone() as Calendar
            dayCal.clearTime()
            val isCurrentMonthFlag =
                dayCal.get(Calendar.MONTH) == currentDisplayMonthValue && dayCal.get(Calendar.YEAR) == currentDisplayYearValue
            val isSelectedFlag =
                dayCal.timeInMillis == normalizedSelectedDateForComparison.timeInMillis
            val isTodayFlag = dayCal.timeInMillis == realTodayCal.timeInMillis
            days.add(
                DateModel(
                    dayNameFormat.format(dayCal.time),
                    dateNumFormat.format(dayCal.time),
                    dayCal.clone() as Calendar,
                    isSelectedFlag,
                    isCurrentMonthFlag,
                    isTodayFlag
                )
            )
            if (isTodayFlag && isCurrentMonthFlag) {
                todayDateIndexInList = days.size - 1
            }
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return Pair(days, todayDateIndexInList)
    }

    private fun centerCurrentDateInCalendar(attempt: Int = 0) {
        val currentBinding = _binding
        if (currentBinding == null || !isAdded || !::calendarAdapter.isInitialized) return
        val layoutManager =
            currentBinding.monthCalendarRecyclerView.layoutManager as? LinearLayoutManager ?: return
        val targetPosition = calendarAdapter.findPositionForDate(selectedDate)
        if (targetPosition == -1 || targetPosition >= calendarAdapter.itemCount) return

        val viewToCenter = layoutManager.findViewByPosition(targetPosition)
        if (viewToCenter != null) {
            val itemWidth = viewToCenter.width
            val recyclerViewWidth = currentBinding.monthCalendarRecyclerView.width
            if (itemWidth > 0 && recyclerViewWidth > 0) {
                val offset = (recyclerViewWidth / 2) - (itemWidth / 2)
                layoutManager.scrollToPositionWithOffset(targetPosition, offset)
            } else if (attempt < 2) {
                layoutManager.scrollToPosition(targetPosition)
                currentBinding.monthCalendarRecyclerView.postDelayed({
                    centerCurrentDateInCalendar(
                        attempt + 1
                    )
                }, 100)
            }
        } else if (attempt < 2) {
            layoutManager.scrollToPosition(targetPosition)
            currentBinding.monthCalendarRecyclerView.postDelayed({
                centerCurrentDateInCalendar(
                    attempt + 1
                )
            }, 100)
        }
    }

    private fun setupSectionRecyclerViews() {
        if (!::selectedDate.isInitialized) {
            selectedDate = Calendar.getInstance().apply { clearTime() }
        }
        val currentSelectedDateStr = dbDateFormat.format(selectedDate.time)
        val onItemClickAction: (Challenge) -> Unit = { challenge ->
            Log.d(
                "HomeFragment",
                "HomeFragment onItemClick: Challenge ID ${challenge.id}. Navigating to details."
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, ChallengeDetailsFragment.newInstance(challenge.id))
                .addToBackStack(ChallengeDetailsFragment::class.java.simpleName)
                .commit()
        }
        val onChallengeMarkedDone: (Long, String) -> Unit = { challengeId, dateStr ->
            handleChallengeAction(challengeId, dateStr, ChallengeDbHelper.STATUS_FOLLOWED)
        }
        val onChallengeMarkedNotDone: (Challenge, String) -> Unit = { challenge, dateStr ->
            handleChallengeAction(
                challenge.id,
                dateStr,
                ChallengeDbHelper.STATUS_NOT_FOLLOWED,
                challenge.title
            )
        }

        _binding?.let { b ->
            unloggedChallengeAdapter = ChallengeAdapter(
                emptyList(),
                onItemClickAction,
                onChallengeMarkedDone,
                onChallengeMarkedNotDone,
                currentSelectedDateStr,
                emptyMap(),
                emptyMap(),
                showFollowButtons = true
            )
            b.unloggedChallengesRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext()); adapter =
                unloggedChallengeAdapter; isNestedScrollingEnabled = false
            }

            loggedChallengeAdapter = ChallengeAdapter(
                emptyList(),
                onItemClickAction,
                onChallengeMarkedDone,
                onChallengeMarkedNotDone,
                currentSelectedDateStr,
                emptyMap(),
                emptyMap(),
                showFollowButtons = true
            )
            b.loggedChallengesRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext()); adapter =
                loggedChallengeAdapter; isNestedScrollingEnabled = false
            }

            concludingChallengeAdapter = ChallengeAdapter(
                emptyList(),
                onItemClickAction,
                { _, _ -> },
                { _, _ -> },
                currentSelectedDateStr,
                emptyMap(),
                emptyMap(),
                showFollowButtons = false
            )
            b.completedChallengesRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext()); adapter =
                concludingChallengeAdapter; isNestedScrollingEnabled = false
            }
        }
    }

    private fun handleChallengeAction(
        challengeId: Long,
        dateStr: String,
        newStatus: String,
        challengeTitle: String? = null
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null || !isAdded) return@launch
            val dateToLogCal = Calendar.getInstance()
            try {
                dbDateFormat.parse(dateStr)?.let { dateToLogCal.time = it }
                    ?: run {
                        Log.e(
                            "HomeFragment",
                            "Error parsing dateStr for future check: $dateStr"
                        ); return@launch
                    }
            } catch (e: Exception) {
                Log.e(
                    "HomeFragment",
                    "Exception parsing dateStr for future check: $dateStr",
                    e
                ); return@launch
            }
            dateToLogCal.clearTime()
            val todayCal = Calendar.getInstance().apply { clearTime() }

            if (dateToLogCal.after(todayCal)) {
                if (!isAdded) return@launch
                val dialogView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_input, null)
                val dialogTitleTextView =
                    dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
                val dialogSubtitleTextView =
                    dialogView.findViewById<TextView>(R.id.dialogSubTitleTextView)
                val notesInputLayout =
                    dialogView.findViewById<TextInputLayout>(R.id.titleInputLayout)
                val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)
                val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)

                notesInputLayout.visibility = View.GONE
                cancelButton.visibility = View.GONE
                dialogSubtitleTextView.visibility = View.VISIBLE
                dialogTitleTextView.text = getString(R.string.future_date_log_error_title)
                dialogSubtitleTextView.text = getString(R.string.future_date_log_error_message)

                saveButton.text = getString(R.string.action_ok)

                val alertDialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
                alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

                saveButton.setOnClickListener {
                    alertDialog.dismiss()
                }
                alertDialog.show()
                return@launch
            }

            if (dateToLogCal.before(todayCal)) {
                val dialogView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_input, null)
                val dialogTitleTextView =
                    dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
                val dialogSubtitleTextView =
                    dialogView.findViewById<TextView>(R.id.dialogSubTitleTextView)
                val notesInputLayout =
                    dialogView.findViewById<TextInputLayout>(R.id.titleInputLayout)
                val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)
                val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)

                dialogTitleTextView.text = getString(R.string.past_date_log_error_title)
                dialogSubtitleTextView.text = getString(R.string.past_date_log_error_message)
                dialogSubtitleTextView.visibility = View.VISIBLE
                notesInputLayout.visibility = View.GONE
                cancelButton.visibility = View.GONE
                saveButton.text = getString(R.string.action_ok)

                val alertDialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

                saveButton.setOnClickListener { alertDialog.dismiss() }
                alertDialog.show()
                return@launch
            }

            val currentActualStatus = viewModel.getLatestStatusForDate(challengeId, dateStr)
            if (_binding == null || !isAdded) return@launch

            val proceedWithAction: suspend (String?) -> Unit = { notes ->
                viewModel.markChallengeDayStatus(challengeId, dateStr, newStatus, notes)
                if (isAdded) {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        when (newStatus) {
                            ChallengeDbHelper.STATUS_FOLLOWED -> getString(R.string.message_marked_done_simple)
                            ChallengeDbHelper.STATUS_NOT_FOLLOWED -> getString(
                                R.string.message_marked_not_followed,
                                challengeTitle ?: "Challenge"
                            )

                            ChallengeDbHelper.STATUS_SKIPPED -> getString(R.string.message_marked_skipped)
                            else -> "Status Updated"
                        }
                    )
                }
                displayChallengesForSelectedDate()
            }

            val showNotesDialogIfNeeded: suspend () -> Unit = showNotesDialogIfNeeded@{
                if (_binding == null || !isAdded) return@showNotesDialogIfNeeded
                when (newStatus) {
                    ChallengeDbHelper.STATUS_NOT_FOLLOWED -> {
                        val dialogView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.dialog_notes_input, null)
                        val notesEditText =
                            dialogView.findViewById<TextInputEditText>(R.id.notesEditText)
                        val dialogTitleTextView =
                            dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
                        val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)
                        val cancelButton =
                            dialogView.findViewById<MaterialButton>(R.id.cancelButton)

                        saveButton.text = getString(R.string.mark_not_followed)
                        dialogTitleTextView.text = getString(
                            R.string.confirm_not_followed_title,
                            challengeTitle ?: "Challenge",
                            formatDateForDisplay(dateStr)
                        )

                        val notesDialog =
                            AlertDialog.Builder(requireContext()).setView(dialogView).create()
                        notesDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                        saveButton.setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                proceedWithAction(notesEditText.text.toString().trim())
                            }
                            notesDialog.dismiss()
                        }
                        cancelButton.setOnClickListener { notesDialog.dismiss() }
                        notesDialog.show()
                    }

                    ChallengeDbHelper.STATUS_SKIPPED -> {
                        proceedWithAction(getString(R.string.notes_skipped_by_user))
                    }

                    else -> {
                        proceedWithAction(null)
                    }
                }
            }

            val isExistingLogActionable =
                currentActualStatus == ChallengeDbHelper.STATUS_FOLLOWED ||
                        currentActualStatus == ChallengeDbHelper.STATUS_NOT_FOLLOWED ||
                        currentActualStatus == ChallengeDbHelper.STATUS_SKIPPED

            if (isExistingLogActionable && newStatus != currentActualStatus) {
                val overrideDialogView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notes_input, null)
                overrideDialogView.findViewById<TextInputLayout>(R.id.titleInputLayout).visibility =
                    View.GONE
                val titleResId = when (newStatus) {
                    ChallengeDbHelper.STATUS_FOLLOWED -> R.string.override_log_prompt
                    ChallengeDbHelper.STATUS_NOT_FOLLOWED -> R.string.override_log_prompt_not_followed
                    ChallengeDbHelper.STATUS_SKIPPED -> R.string.override_log_prompt_skip
                    else -> R.string.override_log_prompt
                }
                overrideDialogView.findViewById<TextView>(R.id.dialogTitleTextView).text =
                    getString(titleResId)
                val overrideSaveButton =
                    overrideDialogView.findViewById<MaterialButton>(R.id.saveButton)
                overrideSaveButton.text = getString(R.string.action_yes)
                val overrideCancelButton =
                    overrideDialogView.findViewById<MaterialButton>(R.id.cancelButton)

                val overrideAlertDialog =
                    AlertDialog.Builder(requireContext()).setView(overrideDialogView).create()
                overrideAlertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                overrideSaveButton.setOnClickListener {
                    viewLifecycleOwner.lifecycleScope.launch {
                        showNotesDialogIfNeeded()
                    }
                    overrideAlertDialog.dismiss()
                }
                overrideCancelButton.setOnClickListener { overrideAlertDialog.dismiss() }
                overrideAlertDialog.show()
            } else if (newStatus == currentActualStatus && newStatus != ChallengeDbHelper.STATUS_PENDING) {
                if (isAdded) SnackbarUtils.showCustomSnackbar(
                    requireActivity(),
                    getString(R.string.status_already_set)
                )
            } else {
                showNotesDialogIfNeeded()
            }
        }
    }

    private fun formatDateForDisplay(dbDate: String): String {
        return try {
            dbDateFormat.parse(dbDate)?.let { displayDateFormat.format(it) } ?: dbDate
        } catch (_: Exception) {
            dbDate
        }
    }

    private fun displayChallengesForSelectedDate() {
        if (!isAdded || !::selectedDate.isInitialized) {
            Log.d(
                "HomeFragment",
                "displayChallengesForSelectedDate: Fragment not added or selectedDate not initialized. Aborting."
            )
            return
        }

        if (isLoadingData) {
            Log.d(
                "HomeFragment",
                "displayChallengesForSelectedDate: Already loading data for ${
                    dbDateFormat.format(selectedDate.time)
                }. Aborting new request."
            )
            return
        }
        isLoadingData = true

        _binding?.loadingContainer?.visibility = View.VISIBLE
        _binding?.contentGroup?.visibility = View.GONE

        loggedDataLoaded = false
        unloggedDataLoaded = false
        concludingTodayDataLoaded = false

        val currentSelectedDateStr = dbDateFormat.format(selectedDate.time)
        Log.d("HomeFragment", "Displaying challenges for date: $currentSelectedDateStr")

        viewModel.refreshLoggedChallenges(currentSelectedDateStr)
        viewModel.refreshNotLoggedChallenges(currentSelectedDateStr)
        viewModel.refreshConcludingChallengesForDate(currentSelectedDateStr)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.monthCalendarRecyclerView?.adapter = null
        _binding?.unloggedChallengesRecyclerView?.adapter = null
        _binding?.loggedChallengesRecyclerView?.adapter = null
        _binding?.completedChallengesRecyclerView?.adapter = null
        _binding = null
    }
}