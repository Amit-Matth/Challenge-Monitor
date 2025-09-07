package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.databinding.FragmentDashBoardBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.DashboardMetricItem
import com.amitmatth.challengemonitor.ui.adapter.DailyLogsAdapter
import com.amitmatth.challengemonitor.ui.adapter.MetricDetailsAdapter
import com.amitmatth.challengemonitor.ui.adapter.ProgressBarAdapter
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private var allChallenges: List<Challenge> = emptyList()

    private lateinit var progressBarAdapter: ProgressBarAdapter
    private lateinit var metricDetailsAdapter: MetricDetailsAdapter
    private lateinit var dailyLogsAdapter: DailyLogsAdapter

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private lateinit var currentSelectedDate: Calendar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.loadingProgressBarText.visibility = View.VISIBLE
        binding.contentGroup.visibility = View.GONE

        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]

        setupRecyclerViews()
        setupDailyLogControls()

        viewModel.allChallenges.observe(viewLifecycleOwner) { challenges ->
            if (_binding == null || !isAdded) return@observe
            allChallenges = challenges ?: emptyList()
            updateDashboardUI()
        }

        viewModel.dailyLogsForSelectedDate.observe(viewLifecycleOwner) { logs ->
            if (_binding == null || !isAdded) return@observe
            Log.d(
                "DashBoardFragment",
                "Observer triggered for daily logs. Logs: ${logs?.size ?: 0}"
            )

            val logsList = logs ?: emptyList()

            if (logsList.isEmpty()) {
                binding.dailyLogsTitle.text = getString(
                    R.string.dashboard_no_activity_on_date,
                    displayDateFormat.format(currentSelectedDate.time)
                )
            } else {
                binding.dailyLogsTitle.text = getString(
                    R.string.dashboard_activity_on_date,
                    displayDateFormat.format(currentSelectedDate.time)
                )
            }

            dailyLogsAdapter.submitList(logsList)
            binding.dailyLogsRecyclerView.post {
                if (_binding == null || !isAdded) return@post
                binding.dailyLogsRecyclerView.requestLayout()
            }
            Log.d(
                "DashBoardFragment",
                "Daily logs updated for ${dbDateFormat.format(currentSelectedDate.time)}. Items: ${logsList.size}"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshChallenges()
        }
        if (::currentSelectedDate.isInitialized) {
            fetchLogsForCurrentDate()
            updateSelectedDateDisplay()
        }
    }

    private fun setupRecyclerViews() {
        progressBarAdapter = ProgressBarAdapter()
        binding.progressBarsRecyclerView.apply {
            adapter = progressBarAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        }

        metricDetailsAdapter = MetricDetailsAdapter()
        binding.metricDetailsRecyclerView.apply {
            adapter = metricDetailsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(false)
        }

        dailyLogsAdapter = DailyLogsAdapter()
        binding.dailyLogsRecyclerView.apply {
            adapter = dailyLogsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupDailyLogControls() {
        currentSelectedDate = Calendar.getInstance()
        currentSelectedDate.clearTime()
        updateSelectedDateDisplay()

        binding.prevDateButton.setOnClickListener {
            currentSelectedDate.add(Calendar.DAY_OF_YEAR, -1)
            updateSelectedDateDisplay()
            fetchLogsForCurrentDate()
        }

        binding.nextDateButton.setOnClickListener {
            val today = Calendar.getInstance()
            today.clearTime()

            if (currentSelectedDate.before(today)) {
                currentSelectedDate.add(Calendar.DAY_OF_YEAR, 1)
                updateSelectedDateDisplay()
                fetchLogsForCurrentDate()
            } else {
                SnackbarUtils.showCustomSnackbar(requireActivity(), "Cannot select future dates.")
            }
        }
    }

    private fun updateSelectedDateDisplay() {
        if (_binding == null || !isAdded) return
        binding.selectedDateTextView.text = displayDateFormat.format(currentSelectedDate.time)

        val today = Calendar.getInstance()
        today.clearTime()

        if (currentSelectedDate.before(today)) {
            binding.nextDateButton.alpha = 1.0f
        } else {
            binding.nextDateButton.alpha = 0.5f
        }
    }

    private fun fetchLogsForCurrentDate() {
        val dateStr = dbDateFormat.format(currentSelectedDate.time)
        Log.d("DashBoardFragment", "Fetching logs for date: $dateStr")
        if (::viewModel.isInitialized) {
            viewModel.fetchLogsForDate(dateStr)
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun getLogsFromRepository(): List<com.amitmatth.challengemonitor.model.ChallengeDailyLog> {
        return viewModel.currentChallengeDailyLogs.value ?: emptyList()
    }

    private fun updateDashboardUI() {
        if (_binding == null || !isAdded) return

        val totalChallenges = allChallenges.size
        val completedChallengesCount = allChallenges.count { !it.isActive }
        val overallSuccessRate = if (totalChallenges > 0) {
            (completedChallengesCount.toFloat() / totalChallenges.toFloat()) * 100
        } else {
            0f
        }

        val overallMetricsItems = mutableListOf<DashboardMetricItem>()
        overallMetricsItems.add(
            DashboardMetricItem(
                title = getString(R.string.dashboard_metric_success_rate),
                progress = overallSuccessRate,
                progressColorRes = R.color.neon_pink,
                valueText = "${String.format(Locale.US, "%.0f", overallSuccessRate)}%"
            )
        )
        overallMetricsItems.add(
            DashboardMetricItem(
                title = getString(R.string.dashboard_metric_total_challenges),
                progress = 100f,
                progressColorRes = R.color.green_light,
                valueText = "$totalChallenges"
            )
        )
        overallMetricsItems.add(
            DashboardMetricItem(
                title = getString(R.string.dashboard_metric_completed_overall),
                progress = if (totalChallenges > 0) (completedChallengesCount.toFloat() / totalChallenges.toFloat()) * 100 else 0f,
                progressColorRes = R.color.pink_shadowed,
                valueText = "$completedChallengesCount"
            )
        )

        Log.d("DashBoardFragment", "Overall stats updated. Items: ${overallMetricsItems.size}")

        val todayDbDateStr = dbDateFormat.format(Date())
        val relevantChallenges = allChallenges.filter { challenge ->
            challenge.startDate <= todayDbDateStr && challenge.endDate >= todayDbDateStr
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null || !isAdded) return@launch

            var challengesFollowedAsOfToday = 0
            var challengesNotFollowedAsOfToday = 0
            var challengesSkippedAsOfToday = 0
            var loggedTodayCount = 0

            Log.d("DashBoardFragment", "Processing stats for date: $todayDbDateStr")

            for (challenge in relevantChallenges) {
                val logs = getLogsFromRepository()

                Log.d(
                    "DashBoardFragment",
                    "Challenge: ${challenge.title} (ID: ${challenge.id}), All Logs count: ${logs.size}"
                )

                val todayLogs = logs.filter { it.logDate == todayDbDateStr }
                Log.d(
                    "DashBoardFragment",
                    "Challenge: ${challenge.title}, Today's Logs (logDate == $todayDbDateStr) count: ${todayLogs.size}"
                )
                if (todayLogs.isNotEmpty()) {
                    todayLogs.forEach { log ->
                        Log.d(
                            "DashBoardFragment",
                            "Challenge: ${challenge.title}, Today Log Entry: Status='${log.status}', Time='${log.lastUpdatedTime}', Notes='${log.notes}'"
                        )
                    }
                }

                val latestActionableLogForToday = todayLogs
                    .filter {
                        (it.status == ChallengeDbHelper.STATUS_FOLLOWED ||
                                it.status == ChallengeDbHelper.STATUS_NOT_FOLLOWED ||
                                it.status == ChallengeDbHelper.STATUS_SKIPPED)
                    }
                    .maxByOrNull { it.lastUpdatedTime }

                if (latestActionableLogForToday != null) {
                    Log.d(
                        "DashBoardFragment",
                        "Challenge: ${challenge.title}, Latest Actionable Log for Today: Status='${latestActionableLogForToday.status}', Time='${latestActionableLogForToday.lastUpdatedTime}'"
                    )
                    loggedTodayCount++
                    when (latestActionableLogForToday.status.trim()) {
                        ChallengeDbHelper.STATUS_FOLLOWED -> {
                            challengesFollowedAsOfToday++
                            Log.d(
                                "DashBoardFragment",
                                "Challenge: ${challenge.title} - STATUS_FOLLOWED. Count=$challengesFollowedAsOfToday"
                            )
                        }

                        ChallengeDbHelper.STATUS_NOT_FOLLOWED -> {
                            challengesNotFollowedAsOfToday++
                            Log.d(
                                "DashBoardFragment",
                                "Challenge: ${challenge.title} - STATUS_NOT_FOLLOWED. Count=$challengesNotFollowedAsOfToday"
                            )
                        }

                        ChallengeDbHelper.STATUS_SKIPPED -> {
                            challengesSkippedAsOfToday++
                            Log.d(
                                "DashBoardFragment",
                                "Challenge: ${challenge.title} - STATUS_SKIPPED. Count=$challengesSkippedAsOfToday"
                            )
                        }
                    }
                } else {
                    Log.d(
                        "DashBoardFragment",
                        "Challenge: ${challenge.title}, No actionable log for today."
                    )
                }
            }

            val totalRelevantChallenges = relevantChallenges.size

            val followedTodayPercentage =
                if (totalRelevantChallenges > 0) (challengesFollowedAsOfToday.toFloat() / totalRelevantChallenges) * 100 else 0f
            val followedTodayValueText = getString(
                R.string.dashboard_metric_fraction_format,
                challengesFollowedAsOfToday,
                totalRelevantChallenges,
                String.format(Locale.US, "%.0f", followedTodayPercentage)
            )
            overallMetricsItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_followed_today),
                    progress = followedTodayPercentage,
                    progressColorRes = R.color.green_success,
                    valueText = followedTodayValueText
                )
            )

            val notFollowedTodayPercentage =
                if (totalRelevantChallenges > 0) (challengesNotFollowedAsOfToday.toFloat() / totalRelevantChallenges) * 100 else 0f
            val notFollowedTodayValueText = getString(
                R.string.dashboard_metric_fraction_format,
                challengesNotFollowedAsOfToday,
                totalRelevantChallenges,
                String.format(Locale.US, "%.0f", notFollowedTodayPercentage)
            )
            overallMetricsItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_not_followed_today),
                    progress = notFollowedTodayPercentage,
                    progressColorRes = R.color.red_unfollowed,
                    valueText = notFollowedTodayValueText
                )
            )

            val skippedTodayPercentage =
                if (totalRelevantChallenges > 0) (challengesSkippedAsOfToday.toFloat() / totalRelevantChallenges) * 100 else 0f
            val skippedTodayValueText = getString(
                R.string.dashboard_metric_fraction_format,
                challengesSkippedAsOfToday,
                totalRelevantChallenges,
                String.format(Locale.US, "%.0f", skippedTodayPercentage)
            )
            overallMetricsItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_skipped_today),
                    progress = skippedTodayPercentage,
                    progressColorRes = R.color.orange_skipped,
                    valueText = skippedTodayValueText
                )
            )

            val unloggedTodayCount = totalRelevantChallenges - loggedTodayCount
            val loggedTodayPercentageValue =
                if (totalRelevantChallenges > 0) (loggedTodayCount.toFloat() / totalRelevantChallenges) * 100 else 0f
            val loggedTodayValueTextValue = getString(
                R.string.dashboard_metric_fraction_format,
                loggedTodayCount,
                totalRelevantChallenges,
                String.format(Locale.US, "%.0f", loggedTodayPercentageValue)
            )
            overallMetricsItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_logged_today),
                    progress = loggedTodayPercentageValue,
                    progressColorRes = R.color.blue_light,
                    valueText = loggedTodayValueTextValue
                )
            )

            val unloggedTodayPercentage =
                if (totalRelevantChallenges > 0) (unloggedTodayCount.toFloat() / totalRelevantChallenges) * 100 else 0f
            val unloggedTodayValueText = getString(
                R.string.dashboard_metric_fraction_format,
                unloggedTodayCount,
                totalRelevantChallenges,
                String.format(Locale.US, "%.0f", unloggedTodayPercentage)
            )
            overallMetricsItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_unlogged_today),
                    progress = unloggedTodayPercentage,
                    progressColorRes = R.color.grey_light,
                    valueText = unloggedTodayValueText
                )
            )

            val currentMetricsList = overallMetricsItems.toList()
            progressBarAdapter.submitList(currentMetricsList)
            metricDetailsAdapter.submitList(currentMetricsList)

            binding.progressBarsRecyclerView.post {
                if (_binding == null || !isAdded) return@post
                binding.progressBarsRecyclerView.requestLayout()
            }
            binding.metricDetailsRecyclerView.post {
                if (_binding == null || !isAdded) return@post
                binding.metricDetailsRecyclerView.requestLayout()
            }

            Log.d(
                "DashBoardFragment",
                "Dashboard UI fully updated. Total metrics items: ${currentMetricsList.size}"
            )

            binding.loadingProgressBar.visibility = View.GONE
            binding.loadingProgressBarText.visibility = View.GONE
            binding.contentGroup.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::progressBarAdapter.isInitialized && _binding?.progressBarsRecyclerView?.adapter != null) {
            _binding?.progressBarsRecyclerView?.adapter = null
        }
        if (::metricDetailsAdapter.isInitialized && _binding?.metricDetailsRecyclerView?.adapter != null) {
            _binding?.metricDetailsRecyclerView?.adapter = null
        }
        if (::dailyLogsAdapter.isInitialized && _binding?.dailyLogsRecyclerView?.adapter != null) {
            _binding?.dailyLogsRecyclerView?.adapter = null
        }
        _binding = null
        Log.d("DashBoardFragment", "onDestroyView: _binding set to null")
    }
}