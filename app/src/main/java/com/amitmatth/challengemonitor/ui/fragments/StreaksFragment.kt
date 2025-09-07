package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.databinding.FragmentStreaksBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.ChallengeDailyLog
import com.amitmatth.challengemonitor.model.ChallengeWithStreakInfo
import com.amitmatth.challengemonitor.ui.adapter.StreakAdapter
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StreaksFragment : Fragment() {

    private var _binding: FragmentStreaksBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var streakAdapter: StreakAdapter

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dbDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val historyTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStreaksBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.refreshStreakChallenges()
    }

    private fun setupRecyclerView() {
        streakAdapter = StreakAdapter { challengeWithStreakInfo ->
            val challengeDetailsFragment = ChallengeDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong("challengeId", challengeWithStreakInfo.challenge.id)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, challengeDetailsFragment)
                .addToBackStack(ChallengeDetailsFragment::class.java.simpleName)
                .commit()
        }

        binding.streaksChallengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = streakAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.streakChallenges.observe(viewLifecycleOwner) { challenges ->
            if (challenges.isNullOrEmpty()) {
                binding.emptyStateTextViewStreaks.text =
                    getString(R.string.no_challenges_found_to_calculate_streaks)
                binding.emptyStateTextViewStreaks.visibility = View.VISIBLE
                binding.streaksChallengesRecyclerView.visibility = View.GONE
                streakAdapter.submitList(emptyList())
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val challengesWithStreaks = mutableListOf<ChallengeWithStreakInfo>()
                    for (challenge in challenges) {
                        if (challenge.id > 0) {
                            val logs = viewModel.getLogsForChallengeId(challenge.id)
                            val streakInfo = calculateStreakInfo(challenge, logs)
                            challengesWithStreaks.add(streakInfo)
                        } else {
                            Log.w(
                                "StreaksFragment",
                                "Challenge '${challenge.title}' has invalid ID ${challenge.id}, skipping."
                            )
                        }
                    }

                    if (challengesWithStreaks.isEmpty()) {
                        binding.emptyStateTextViewStreaks.text =
                            if (challenges.isNotEmpty()) "Could not calculate streaks for any challenges." else "No challenges found to calculate streaks."
                        binding.emptyStateTextViewStreaks.visibility = View.VISIBLE
                        binding.streaksChallengesRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyStateTextViewStreaks.visibility = View.GONE
                        binding.streaksChallengesRecyclerView.visibility = View.VISIBLE
                    }

                    streakAdapter.submitList(
                        challengesWithStreaks.sortedWith(
                        compareByDescending<ChallengeWithStreakInfo> { it.currentStreak }
                            .thenByDescending { it.longestStreak }
                            .thenBy { it.challenge.title }
                    ))
                }
            }
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
                            "StreaksFragment",
                            "Could not parse timeString for sorting: $timeString",
                            e4
                        )
                        null
                    }
                }
            }
        }
    }

    private fun calculateStreakInfo(
        challenge: Challenge,
        logs: List<ChallengeDailyLog>
    ): ChallengeWithStreakInfo {
        var longestStreak = 0
        var currentOngoingStreak = 0

        if (challenge.startDate.isBlank() || challenge.endDate.isBlank()) {
            Log.w(
                "StreaksFragment",
                "Challenge '${challenge.title}' has invalid start/end dates. Skipping streak calculation."
            )
            return ChallengeWithStreakInfo(challenge, 0, 0)
        }

        fun Calendar.normalize(): Calendar {
            this.set(Calendar.HOUR_OF_DAY, 0)
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.SECOND, 0)
            this.set(Calendar.MILLISECOND, 0)
            return this
        }

        val sortedLogsByDateAndTime = logs.sortedWith(
            compareBy(
            { it.logDate },
            { parseLogTime(it.lastUpdatedTime) }
        ))

        val dailyStatusMap = mutableMapOf<String, String>()
        for (log in sortedLogsByDateAndTime) {
            if (log.status == ChallengeDbHelper.STATUS_FOLLOWED ||
                log.status == ChallengeDbHelper.STATUS_NOT_FOLLOWED ||
                log.status == ChallengeDbHelper.STATUS_SKIPPED
            ) {
                dailyStatusMap[log.logDate] = log.status
            }
        }

        val todayCal = Calendar.getInstance().normalize()
        val challengeStartDateCal: Calendar
        val challengeEndDateCal: Calendar

        try {
            challengeStartDateCal =
                Calendar.getInstance().apply { time = dbDateFormat.parse(challenge.startDate)!! }
                    .normalize()
            challengeEndDateCal =
                Calendar.getInstance().apply { time = dbDateFormat.parse(challenge.endDate)!! }
                    .normalize()
        } catch (e: Exception) {
            Log.e(
                "StreaksFragment",
                "Error parsing challenge dates for '${challenge.title}': ${e.message}"
            )
            return ChallengeWithStreakInfo(challenge, 0, 0)
        }

        val iterationEndCal =
            if (challengeEndDateCal.before(todayCal)) challengeEndDateCal else todayCal

        if (challengeStartDateCal.after(todayCal)) {
            return ChallengeWithStreakInfo(challenge, 0, 0)
        }

        val currentDayCal = challengeStartDateCal.clone() as Calendar

        while (!currentDayCal.after(iterationEndCal)) {
            val currentDayFormatted = dbDateFormat.format(currentDayCal.time)
            val statusForDay = dailyStatusMap[currentDayFormatted]

            if (statusForDay == ChallengeDbHelper.STATUS_FOLLOWED) {
                currentOngoingStreak++
            } else {
                if (currentOngoingStreak > longestStreak) {
                    longestStreak = currentOngoingStreak
                }
                currentOngoingStreak = 0
            }
            currentDayCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (currentOngoingStreak > longestStreak) {
            longestStreak = currentOngoingStreak
        }

        Log.d(
            "StreaksFragment",
            "Calculated for ${challenge.title}: Current $currentOngoingStreak, Longest $longestStreak. Iterated up to ${
                dbDateFormat.format(iterationEndCal.time)
            }"
        )
        return ChallengeWithStreakInfo(challenge, currentOngoingStreak, longestStreak)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStreakChallenges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}