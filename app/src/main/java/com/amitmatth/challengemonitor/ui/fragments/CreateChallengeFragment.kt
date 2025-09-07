package com.amitmatth.challengemonitor.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentCreateChallengeBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.ui.MainActivity
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class CreateChallengeFragment : Fragment() {

    private var _binding: FragmentCreateChallengeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var currentSelectedStartDateDb: String? = null
    private var currentSelectedEndDateDb: String? = null

    private var challengeIdToEdit: Long = -1L
    private var isEditMode: Boolean = false
    private var currentChallengeForEdit: Challenge? = null

    private var isUpdatingEndDateProgrammatically = false
    private var isUpdatingDurationProgrammatically = false
    private var isUpdatingRadioGroupProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            challengeIdToEdit = it.getLong("challengeIdToEdit", -1L)
            isEditMode = challengeIdToEdit != -1L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateChallengeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isEditMode) {
            val todayCal = Calendar.getInstance()
            currentSelectedStartDateDb = dbDateFormat.format(todayCal.time)
            binding.startDateInputLayout.editText?.setText(displayDateFormat.format(todayCal.time))
            binding.endDateInputLayout.editText?.isEnabled = true
            binding.customDurationDaysInputLayout.visibility = View.GONE
        } else {
            binding.customDurationDaysInputLayout.visibility = View.GONE
        }

        setupDurationRadioGroupListener()
        setupDatePickers()
        setupValidationListeners()
        setupCustomDurationListener()

        if (isEditMode) {
            binding.createChallengeButton.text = getString(R.string.update_challenge)
            viewModel.fetchChallengeById(challengeIdToEdit)
            observeChallengeForEdit()
        } else {
            binding.createChallengeButton.text = getString(R.string.create_challenge)
        }

        binding.createChallengeButton.setOnClickListener {
            submitChallengeData()
        }

        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeChallengeForEdit() {
        viewModel.currentChallenge.observe(viewLifecycleOwner) { challenge ->
            if (isEditMode && challenge != null && challenge.id == challengeIdToEdit && currentChallengeForEdit == null) {
                currentChallengeForEdit = challenge
                populateUIForEdit(challenge)
            }
        }
    }

    private fun populateUIForEdit(challenge: Challenge) {
        binding.titleInputLayout.editText?.setText(challenge.title)
        binding.descriptionInputLayout.editText?.setText(challenge.description ?: "")

        try {
            isUpdatingDurationProgrammatically = true
            isUpdatingEndDateProgrammatically = true
            isUpdatingRadioGroupProgrammatically = true

            challenge.startDate.let { dbDateStr ->
                currentSelectedStartDateDb = dbDateStr
                dbDateFormat.parse(dbDateStr)?.let { parsedDate ->
                    binding.startDateInputLayout.editText?.setText(
                        displayDateFormat.format(
                            parsedDate
                        )
                    )
                }
            }

            challenge.endDate.let { dbDateStr ->
                currentSelectedEndDateDb = dbDateStr
                dbDateFormat.parse(dbDateStr)?.let { parsedDate ->
                    binding.endDateInputLayout.editText?.setText(displayDateFormat.format(parsedDate))
                }
            }

            val durationDays = challenge.durationDays
            var matchedPredefined = false
            when (durationDays) {
                7 -> {
                    binding.duration7DaysRadioButton.isChecked = true; matchedPredefined = true
                }

                14 -> {
                    binding.duration14DaysRadioButton.isChecked = true; matchedPredefined = true
                }

                21 -> {
                    binding.duration21DaysRadioButton.isChecked = true; matchedPredefined = true
                }

                30 -> {
                    binding.duration30DaysRadioButton.isChecked = true; matchedPredefined = true
                }
            }

            if (matchedPredefined) {
                binding.customDurationDaysInputLayout.visibility = View.GONE
                binding.endDateInputLayout.editText?.isEnabled = false
            } else {
                binding.durationCustomRadioButton.isChecked = true
                binding.customDurationDaysInputLayout.visibility = View.VISIBLE
                binding.customDurationDaysInputLayout.editText?.setText(durationDays.toString())
                binding.endDateInputLayout.editText?.isEnabled = true
                if (currentSelectedStartDateDb != null && currentSelectedEndDateDb != null) {
                    val calStart = Calendar.getInstance()
                        .apply { time = dbDateFormat.parse(currentSelectedStartDateDb!!)!! }
                    calStart.add(Calendar.DAY_OF_YEAR, durationDays - 1)
                    val calculatedEndDate = dbDateFormat.format(calStart.time)
                    if (calculatedEndDate == currentSelectedEndDateDb) {
                        binding.endDateInputLayout.editText?.isEnabled = false
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("CreateChallengeFragment", "Error populating UI for edit: ${e.message}")
            SnackbarUtils.showCustomSnackbar(requireActivity(), "Error loading challenge details.")
        } finally {
            isUpdatingDurationProgrammatically = false
            isUpdatingEndDateProgrammatically = false
            isUpdatingRadioGroupProgrammatically = false
            clearAllValidationErrors()
        }
    }

    private fun setupDurationRadioGroupListener() {
        binding.durationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isUpdatingRadioGroupProgrammatically || isUpdatingDurationProgrammatically) return@setOnCheckedChangeListener

            binding.customDurationDaysInputLayout.error = null
            binding.durationRadioGroup.children.filterIsInstance<RadioButton>()
                .forEach { it.error = null }

            when (checkedId) {
                R.id.duration7DaysRadioButton -> handlePredefinedDuration(7)
                R.id.duration14DaysRadioButton -> handlePredefinedDuration(14)
                R.id.duration21DaysRadioButton -> handlePredefinedDuration(21)
                R.id.duration30DaysRadioButton -> handlePredefinedDuration(30)
                R.id.durationCustomRadioButton -> handleCustomDurationSelection()
            }
            validateDuration()
            validateEndDate()
        }
    }

    private fun handlePredefinedDuration(days: Int) {
        isUpdatingDurationProgrammatically = true
        isUpdatingEndDateProgrammatically = true

        binding.customDurationDaysInputLayout.visibility = View.GONE
        binding.customDurationDaysInputLayout.editText?.text?.clear()

        if (currentSelectedStartDateDb != null) {
            updateEndDateFromDuration(days)
            binding.endDateInputLayout.editText?.isEnabled = false
        } else {
            currentSelectedEndDateDb = null
            binding.endDateInputLayout.editText?.text?.clear()
            binding.endDateInputLayout.editText?.isEnabled = true
        }
        isUpdatingDurationProgrammatically = false
        isUpdatingEndDateProgrammatically = false
    }

    private fun handleCustomDurationSelection() {
        isUpdatingDurationProgrammatically = true
        isUpdatingEndDateProgrammatically = true

        binding.customDurationDaysInputLayout.visibility = View.VISIBLE
        currentSelectedEndDateDb = null
        binding.endDateInputLayout.editText?.text?.clear()
        binding.endDateInputLayout.editText?.isEnabled = true
        binding.customDurationDaysInputLayout.editText?.requestFocus()

        isUpdatingDurationProgrammatically = false
        isUpdatingEndDateProgrammatically = false
    }

    private fun setupCustomDurationListener() {
        binding.customDurationDaysInputLayout.editText?.addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingDurationProgrammatically || !binding.durationCustomRadioButton.isChecked) return

                val daysStr = s.toString()
                if (daysStr.isNotEmpty()) {
                    try {
                        val days = daysStr.toInt()
                        if (days > 0 && currentSelectedStartDateDb != null) {
                            updateEndDateFromDuration(days, true)
                            binding.endDateInputLayout.editText?.isEnabled = false
                        } else if (days > 0) {
                            binding.endDateInputLayout.editText?.isEnabled = true
                            currentSelectedEndDateDb = null
                            binding.endDateInputLayout.editText?.text = null
                        } else {
                            binding.customDurationDaysInputLayout.error = "Days must be > 0"
                            binding.endDateInputLayout.editText?.isEnabled = true
                            currentSelectedEndDateDb = null
                            binding.endDateInputLayout.editText?.text = null
                        }
                    } catch (_: NumberFormatException) {
                        binding.customDurationDaysInputLayout.error = "Invalid number"
                        binding.endDateInputLayout.editText?.isEnabled = true
                    }
                } else {
                    binding.customDurationDaysInputLayout.error = null
                    binding.endDateInputLayout.editText?.isEnabled = true
                    if (currentSelectedStartDateDb != null) {
                        currentSelectedEndDateDb = null
                        binding.endDateInputLayout.editText?.text = null
                    }
                }
                validateCustomDuration()
                validateEndDate()
            }
        })
    }

    private fun updateEndDateFromDuration(days: Int, fromCustomInput: Boolean = false) {
        if (currentSelectedStartDateDb == null || days <= 0) {
            currentSelectedEndDateDb = null
            binding.endDateInputLayout.editText?.setText("")
            if (!fromCustomInput || binding.durationCustomRadioButton.isChecked) {
                binding.endDateInputLayout.editText?.isEnabled = true
            }
            return
        }

        isUpdatingEndDateProgrammatically = true
        try {
            val startCal = Calendar.getInstance()
                .apply { time = dbDateFormat.parse(currentSelectedStartDateDb!!)!! }
            startCal.add(Calendar.DAY_OF_YEAR, days - 1)
            currentSelectedEndDateDb = dbDateFormat.format(startCal.time)
            binding.endDateInputLayout.editText?.setText(displayDateFormat.format(startCal.time))
            binding.endDateInputLayout.editText?.isEnabled = false
            binding.endDateInputLayout.error = null
        } catch (e: Exception) {
            Log.e("CreateChallengeFragment", "Error calculating end date: ${e.message}")
            currentSelectedEndDateDb = null
            binding.endDateInputLayout.editText?.setText("")
            binding.endDateInputLayout.editText?.isEnabled = true
        } finally {
            isUpdatingEndDateProgrammatically = false
        }
    }

    private fun updateDurationFromDates() {
        if (currentSelectedStartDateDb == null || currentSelectedEndDateDb == null) return
        if (isUpdatingDurationProgrammatically || isUpdatingRadioGroupProgrammatically) return

        isUpdatingDurationProgrammatically = true
        isUpdatingRadioGroupProgrammatically = true

        try {
            val d1 = dbDateFormat.parse(currentSelectedStartDateDb!!)!!
            val d2 = dbDateFormat.parse(currentSelectedEndDateDb!!)!!

            if (d2.before(d1)) {
                binding.endDateInputLayout.error = "End date < start date"
                binding.durationRadioGroup.clearCheck()
                binding.customDurationDaysInputLayout.editText?.setText("")
                binding.customDurationDaysInputLayout.visibility = View.GONE
                return
            } else {
                binding.endDateInputLayout.error = null
            }

            val durationMillis = d2.time - d1.time
            val calculatedDurationDays = TimeUnit.MILLISECONDS.toDays(durationMillis).toInt() + 1

            if (calculatedDurationDays > 0) {
                var matchedPredefined = false
                when (calculatedDurationDays) {
                    7 -> {
                        binding.duration7DaysRadioButton.isChecked = true; matchedPredefined = true
                    }

                    14 -> {
                        binding.duration14DaysRadioButton.isChecked = true; matchedPredefined = true
                    }

                    21 -> {
                        binding.duration21DaysRadioButton.isChecked = true; matchedPredefined = true
                    }

                    30 -> {
                        binding.duration30DaysRadioButton.isChecked = true; matchedPredefined = true
                    }
                }

                if (matchedPredefined) {
                    binding.customDurationDaysInputLayout.visibility = View.GONE
                    binding.customDurationDaysInputLayout.editText?.setText("")
                    binding.endDateInputLayout.editText?.isEnabled = false
                } else {
                    binding.durationCustomRadioButton.isChecked = true
                    binding.customDurationDaysInputLayout.visibility = View.VISIBLE
                    binding.customDurationDaysInputLayout.editText?.setText(calculatedDurationDays.toString())
                    binding.endDateInputLayout.editText?.isEnabled = false
                }
            } else {
                binding.durationRadioGroup.clearCheck()
                binding.customDurationDaysInputLayout.editText?.setText("")
                binding.customDurationDaysInputLayout.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("CreateChallengeFragment", "Error in updateDurationFromDates: ${e.message}")
            binding.durationRadioGroup.clearCheck()
            binding.customDurationDaysInputLayout.editText?.setText("")
            binding.customDurationDaysInputLayout.visibility = View.GONE
        } finally {
            isUpdatingDurationProgrammatically = false
            isUpdatingRadioGroupProgrammatically = false
        }
    }

    private fun setupDatePickers() {
        binding.startDateInputLayout.editText?.setOnClickListener {
            showDatePickerDialog(true) { selectedDbDate, displayDateString ->
                if (isUpdatingEndDateProgrammatically || isUpdatingDurationProgrammatically) return@showDatePickerDialog

                currentSelectedStartDateDb = selectedDbDate
                binding.startDateInputLayout.editText?.setText(displayDateString)
                binding.startDateInputLayout.error = null

                val checkedRadioId = binding.durationRadioGroup.checkedRadioButtonId
                if (checkedRadioId != -1 && checkedRadioId != R.id.durationCustomRadioButton) {
                    val days = when (checkedRadioId) {
                        R.id.duration7DaysRadioButton -> 7
                        R.id.duration14DaysRadioButton -> 14
                        R.id.duration21DaysRadioButton -> 21
                        R.id.duration30DaysRadioButton -> 30
                        else -> 0
                    }
                    updateEndDateFromDuration(days)
                } else if (binding.durationCustomRadioButton.isChecked && binding.customDurationDaysInputLayout.editText?.text.toString()
                        .isNotEmpty()
                ) {
                    val days = binding.customDurationDaysInputLayout.editText?.text.toString()
                        .toIntOrNull() ?: 0
                    if (days > 0) updateEndDateFromDuration(days, true)
                    else binding.endDateInputLayout.editText?.isEnabled = true
                } else if (currentSelectedEndDateDb != null) {
                    updateDurationFromDates()
                } else {
                    binding.endDateInputLayout.editText?.isEnabled = true
                }
                validateStartDate()
                validateEndDate()
                validateDuration()
            }
        }

        binding.endDateInputLayout.editText?.setOnClickListener {
            showDatePickerDialog(false) { selectedDbDate, displayDateString ->
                if (isUpdatingEndDateProgrammatically || isUpdatingDurationProgrammatically) return@showDatePickerDialog

                currentSelectedEndDateDb = selectedDbDate
                binding.endDateInputLayout.editText?.setText(displayDateString)
                binding.endDateInputLayout.error = null

                if (currentSelectedStartDateDb != null) {
                    updateDurationFromDates()
                }
                validateEndDate()
                validateDuration()
            }
        }
    }

    private fun showDatePickerDialog(
        isStartDate: Boolean,
        onDateSelected: (dbDate: String, displayDate: String) -> Unit
    ) {
        val tempCalendar = Calendar.getInstance()
        val existingDateStr =
            if (isStartDate) currentSelectedStartDateDb else currentSelectedEndDateDb
        if (existingDateStr != null) {
            try {
                tempCalendar.time = dbDateFormat.parse(existingDateStr)!!
            } catch (_: Exception) {
            }
        }

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                tempCalendar.set(Calendar.YEAR, year)
                tempCalendar.set(Calendar.MONTH, month)
                tempCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSelected(
                    dbDateFormat.format(tempCalendar.time),
                    displayDateFormat.format(tempCalendar.time)
                )
            },
            tempCalendar.get(Calendar.YEAR),
            tempCalendar.get(Calendar.MONTH),
            tempCalendar.get(Calendar.DAY_OF_MONTH)
        )

        if (isStartDate && !isEditMode) {
            dialog.datePicker.minDate = Calendar.getInstance().timeInMillis
        } else if (!isStartDate && currentSelectedStartDateDb != null) {
            try {
                val minDateCal = Calendar.getInstance()
                    .apply { time = dbDateFormat.parse(currentSelectedStartDateDb!!)!! }
                dialog.datePicker.minDate = minDateCal.timeInMillis
            } catch (_: Exception) {
                Log.e("DatePicker", "Error setting min date for end date")
            }
        }
        if (isStartDate && currentSelectedEndDateDb != null) {
            try {
                val maxDateCal = Calendar.getInstance()
                    .apply { time = dbDateFormat.parse(currentSelectedEndDateDb!!)!! }
                dialog.datePicker.maxDate = maxDateCal.timeInMillis
            } catch (_: Exception) {
                Log.e("DatePicker", "Error setting max date for start date")
            }
        }
        dialog.show()
    }

    private fun validateTitle(): Boolean {
        return if (binding.titleInputLayout.editText?.text.toString().trim().isEmpty()) {
            binding.titleInputLayout.error = "Title cannot be empty"
            false
        } else {
            binding.titleInputLayout.error = null
            true
        }
    }

    private fun validateStartDate(): Boolean {
        if (currentSelectedStartDateDb == null) {
            binding.startDateInputLayout.error = "Start date cannot be empty"
            return false
        }
        if (!isEditMode) {
            try {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(
                    Calendar.MINUTE,
                    0
                ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startDateCal = Calendar.getInstance()
                    .apply { time = dbDateFormat.parse(currentSelectedStartDateDb!!)!! }
                if (startDateCal.before(today)) {
                    binding.startDateInputLayout.error = "Start date cannot be in the past"
                    return false
                }
            } catch (_: Exception) {
            }
        }
        binding.startDateInputLayout.error = null
        return true
    }

    private fun validateEndDate(): Boolean {
        if (currentSelectedEndDateDb == null) {
            binding.endDateInputLayout.error = "End date cannot be empty"
            return false
        }
        if (currentSelectedStartDateDb != null) {
            try {
                val d1 = dbDateFormat.parse(currentSelectedStartDateDb!!)!!
                val d2 = dbDateFormat.parse(currentSelectedEndDateDb!!)!!
                if (d2.before(d1)) {
                    binding.endDateInputLayout.error = "End date cannot be before start date"
                    return false
                }
            } catch (_: Exception) {
            }
        }
        binding.endDateInputLayout.error = null
        return true
    }

    private fun validateDuration(): Boolean {
        val checkedRadioId = binding.durationRadioGroup.checkedRadioButtonId
        if (checkedRadioId == -1) {
            binding.duration7DaysRadioButton.error = "Duration must be selected"
            return false
        }
        binding.duration7DaysRadioButton.error = null

        if (checkedRadioId == R.id.durationCustomRadioButton) {
            return validateCustomDuration()
        }
        binding.customDurationDaysInputLayout.error = null
        return true
    }

    private fun validateCustomDuration(): Boolean {
        val customDaysText = binding.customDurationDaysInputLayout.editText?.text.toString()
        if (binding.durationCustomRadioButton.isChecked) {
            if (customDaysText.isEmpty()) {
                binding.customDurationDaysInputLayout.error = "Custom days cannot be empty"
                return false
            }
            try {
                val days = customDaysText.toInt()
                if (days <= 0) {
                    binding.customDurationDaysInputLayout.error = "Days must be > 0"
                    return false
                }
            } catch (_: NumberFormatException) {
                binding.customDurationDaysInputLayout.error = "Invalid number"
                return false
            }
        }
        binding.customDurationDaysInputLayout.error = null
        return true
    }

    private fun setupValidationListeners() {
        binding.titleInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateTitle()
            }
        })
    }

    private fun clearAllValidationErrors() {
        binding.titleInputLayout.error = null
        binding.descriptionInputLayout.error = null
        binding.startDateInputLayout.error = null
        binding.endDateInputLayout.error = null
        binding.customDurationDaysInputLayout.error = null
        binding.durationRadioGroup.children.filterIsInstance<RadioButton>()
            .forEach { it.error = null }
    }

    private fun submitChallengeData() {
        val isTitleValid = validateTitle()
        val isStartDateValid = validateStartDate()
        val isEndDateValid = validateEndDate()
        val isDurationValid = validateDuration()

        if (!isTitleValid || !isStartDateValid || !isEndDateValid || !isDurationValid) {
            SnackbarUtils.showCustomSnackbar(requireActivity(), "Please correct the errors.")
            return
        }

        val title = binding.titleInputLayout.editText?.text.toString().trim()
        val description =
            binding.descriptionInputLayout.editText?.text.toString().trim().ifEmpty { "" }

        var durationInDays: Int
        try {
            val d1 = dbDateFormat.parse(currentSelectedStartDateDb!!)!!
            val d2 = dbDateFormat.parse(currentSelectedEndDateDb!!)!!
            val diff = d2.time - d1.time
            durationInDays = (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1).toInt()
        } catch (e: Exception) {
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Error calculating duration from dates."
            )
            Log.e("SubmitChallenge", "Error in duration calculation: ${e.message}")
            return
        }

        if (durationInDays <= 0) {
            SnackbarUtils.showCustomSnackbar(
                requireActivity(),
                "Calculated duration must be at least 1 day."
            )
            if (binding.durationCustomRadioButton.isChecked) {
                binding.customDurationDaysInputLayout.error = "Resulting duration is invalid."
            } else {
                binding.endDateInputLayout.error = "Resulting duration from dates is invalid."
            }
            return
        }

        val challenge = Challenge(
            id = if (isEditMode) challengeIdToEdit else 0,
            title = title,
            description = description,
            startDate = currentSelectedStartDateDb!!,
            endDate = currentSelectedEndDateDb!!,
            durationDays = durationInDays,
            isActive = true,
            daysLogged = 0
        )

        lifecycleScope.launch {
            if (isEditMode) {
                val rowsAffected = viewModel.updateChallenge(challenge)
                if (rowsAffected > 0) {
                    SnackbarUtils.showCustomSnackbar(requireActivity(), "Challenge updated!")
                    (requireActivity() as MainActivity).loadFragment(
                        ChallengeDetailsFragment(),
                        ChallengeDetailsFragment::class.java.name,
                        true
                    )
                } else {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Failed to update challenge."
                    )
                }
            } else {
                val newChallengeId = viewModel.insertChallenge(challenge)
                if (newChallengeId > -1) {
                    SnackbarUtils.showCustomSnackbar(requireActivity(), "Challenge created!")
                    (requireActivity() as MainActivity).loadFragment(
                        HomeFragment(),
                        HomeFragment::class.java.name
                    )
                } else {
                    SnackbarUtils.showCustomSnackbar(
                        requireActivity(),
                        "Failed to create challenge."
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentChallengeForEdit = null
        _binding = null
    }
}