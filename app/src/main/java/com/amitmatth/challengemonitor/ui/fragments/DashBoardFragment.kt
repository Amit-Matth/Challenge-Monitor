package com.amitmatth.challengemonitor.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentDashBoardBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.DashboardMetricItem
import com.amitmatth.challengemonitor.ui.adapter.DailyLogsAdapter
import com.amitmatth.challengemonitor.ui.adapter.MetricDetailsAdapter
import com.amitmatth.challengemonitor.ui.adapter.ProgressBarAdapter
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private var allChallengesForOverallMetrics: List<Challenge> = emptyList()

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
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        val todayDbDateStr = dbDateFormat.format(Date())

        _binding?.let {
            Log.d(
                "DashBoardFragment",
                "onResume: Forcing loading indicators to visible before refresh."
            )
            it.loadingProgressBar.visibility = View.VISIBLE
            it.loadingProgressBarText.visibility = View.VISIBLE
            it.contentGroup.visibility = View.GONE
        }

        if (::viewModel.isInitialized) {
            viewModel.refreshChallenges()
            viewModel.fetchFollowedData(todayDbDateStr)
            viewModel.fetchUnFollowedData(todayDbDateStr)
            viewModel.refreshLoggedChallenges(todayDbDateStr)
            viewModel.refreshSkippedChallengesForDate(todayDbDateStr)
            viewModel.refreshCompletedChallenges()
        }

        if (::currentSelectedDate.isInitialized) {
            fetchLogsForCurrentDate()
            updateSelectedDateDisplay()
        }
    }

    private fun setupRecyclerViews() {
        metricDetailsAdapter = MetricDetailsAdapter()
        binding.metricDetailsRecyclerView.apply {
            adapter = metricDetailsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(false)
        }

        progressBarAdapter = ProgressBarAdapter()
        binding.progressBarsRecyclerView.apply {
            adapter = progressBarAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
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
        fetchLogsForCurrentDate()

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
                SnackbarUtils.showCustomSnackbar(requireActivity(), "Can't select future dates")
            }
        }
    }

    private fun setupObservers() {
        viewModel.allChallenges.observe(viewLifecycleOwner) { challenges ->
            if (_binding == null || !isAdded) {
                Log.d("DashBoardFragment", "allChallenges observer: binding null or not added.")
                return@observe
            }
            Log.d(
                "DashBoardFragment",
                "allChallenges observer triggered. Challenges: ${challenges?.size ?: "0"}"
            )
            allChallengesForOverallMetrics = challenges ?: emptyList()
            updateDashboardMetrics()
        }

        viewModel.completedChallenges.observe(viewLifecycleOwner) { _ ->
            if (_binding == null || !isAdded) return@observe
            updateDashboardMetrics()
        }

        viewModel.todayFollowedChallenges.observe(viewLifecycleOwner) { _ ->
            if (_binding == null || !isAdded) return@observe
            updateDashboardMetrics()
        }
        viewModel.todayUnFollowedChallenges.observe(viewLifecycleOwner) { _ ->
            if (_binding == null || !isAdded) return@observe
            updateDashboardMetrics()
        }
        viewModel.loggedChallenges.observe(viewLifecycleOwner) { _ ->
            if (_binding == null || !isAdded) return@observe
            updateDashboardMetrics()
        }
        viewModel.skippedChallengesForDate.observe(viewLifecycleOwner) { _ ->
            if (_binding == null || !isAdded) return@observe
            updateDashboardMetrics()
        }

        viewModel.dailyLogsForSelectedDate.observe(viewLifecycleOwner) { logs ->
            if (_binding == null || !isAdded) return@observe
            val logsList = logs ?: emptyList()
            binding.dailyLogsTitle.text = if (logsList.isEmpty()) {
                getString(
                    R.string.dashboard_no_activity_on_date,
                    displayDateFormat.format(currentSelectedDate.time)
                )
            } else {
                getString(
                    R.string.dashboard_activity_on_date,
                    displayDateFormat.format(currentSelectedDate.time)
                )
            }
            dailyLogsAdapter.submitList(logsList)
        }
    }

    private fun updateDashboardMetrics() {
        if (_binding == null || !isAdded || !isResumed) {
            Log.d(
                "DashBoardFragment",
                "updateDashboardMetrics: binding null, not added, or not resumed. Returning."
            )
            return
        }

        if (viewModel.allChallenges.value != null) {
            Log.d("DashBoardFragment", "updateDashboardMetrics: Hiding loading indicator.")
            binding.loadingProgressBar.visibility = View.GONE
            binding.loadingProgressBarText.visibility = View.GONE
            binding.contentGroup.visibility = View.VISIBLE

            val overallMetricItems = mutableListOf<DashboardMetricItem>()
            val progressMetricItems = mutableListOf<DashboardMetricItem>()

            val totalChallenges = allChallengesForOverallMetrics.size
            val completedChallengesCount = viewModel.completedChallenges.value?.size
                ?: allChallengesForOverallMetrics.count { !it.isActive }
            val overallSuccessRate = if (totalChallenges > 0) {
                (completedChallengesCount.toFloat() / totalChallenges.toFloat()) * 100
            } else {
                0f
            }

            overallMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_total_challenges),
                    progress = 100f,
                    progressColorRes = R.color.colorTotalChallenges,
                    valueText = "$totalChallenges"
                )
            )
            overallMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_completed_overall),
                    progress = if (totalChallenges > 0) (completedChallengesCount.toFloat() / totalChallenges.toFloat()) * 100 else 0f,
                    progressColorRes = R.color.colorCompletedOverall,
                    valueText = "$completedChallengesCount"
                )
            )
            overallMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_success_rate),
                    progress = overallSuccessRate,
                    progressColorRes = R.color.colorSuccessRate,
                    valueText = "${String.format(Locale.US, "%.0f", overallSuccessRate)}%"
                )
            )

            val sharedPrefs = requireActivity().getSharedPreferences(
                SettingsFragment.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val deletedChallengesCount =
                sharedPrefs.getInt(SettingsFragment.KEY_DELETED_CHALLENGES_COUNT, 0)
            overallMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_deleted_challenges),
                    progress = 100f,
                    progressColorRes = R.color.colorDeletedChallenges,
                    valueText = "$deletedChallengesCount"
                )
            )

            val todayDbDateStr = dbDateFormat.format(Date())
            val activeChallengesToday = allChallengesForOverallMetrics.filter {
                it.isActive && it.startDate <= todayDbDateStr && it.endDate >= todayDbDateStr
            }
            val totalActiveToday = activeChallengesToday.size

            val followedTodayCount = viewModel.todayFollowedChallenges.value?.size ?: 0
            val notFollowedTodayCount = viewModel.todayUnFollowedChallenges.value?.size ?: 0
            val skippedTodayCount = viewModel.skippedChallengesForDate.value?.size ?: 0
            val loggedTodayCount = (viewModel.loggedChallenges.value?.size ?: 0)
            val unloggedTodayCount = totalActiveToday - loggedTodayCount

            progressMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_followed_today),
                    valueText = "$followedTodayCount/$totalActiveToday",
                    progress = calculateProgress(followedTodayCount, totalActiveToday),
                    progressColorRes = R.color.colorFollowedToday
                )
            )
            progressMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_not_followed_today),
                    valueText = "$notFollowedTodayCount/$totalActiveToday",
                    progress = calculateProgress(notFollowedTodayCount, totalActiveToday),
                    progressColorRes = R.color.colorNotFollowedToday
                )
            )
            progressMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_skipped_today),
                    valueText = "$skippedTodayCount/$totalActiveToday",
                    progress = calculateProgress(skippedTodayCount, totalActiveToday),
                    progressColorRes = R.color.colorSkippedToday
                )
            )
            progressMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_logged_today),
                    valueText = "$loggedTodayCount/$totalActiveToday",
                    progress = calculateProgress(loggedTodayCount, totalActiveToday),
                    progressColorRes = R.color.colorLoggedToday
                )
            )
            progressMetricItems.add(
                DashboardMetricItem(
                    title = getString(R.string.dashboard_metric_unlogged_today),
                    valueText = "$unloggedTodayCount/$totalActiveToday",
                    progress = calculateProgress(unloggedTodayCount, totalActiveToday),
                    progressColorRes = R.color.colorUnloggedToday
                )
            )

            metricDetailsAdapter.submitList(overallMetricItems.toList())
            progressBarAdapter.submitList(progressMetricItems.toList())

            Log.d(
                "DashBoardFragment",
                "Dashboard metrics updated. Overall: ${overallMetricItems.size}, Progress: ${progressMetricItems.size}"
            )
        } else {
            Log.d(
                "DashBoardFragment",
                "updateDashboardMetrics: allChallenges.value is null, showing loading indicator."
            )
            binding.loadingProgressBar.visibility = View.VISIBLE
            binding.loadingProgressBarText.visibility = View.VISIBLE
            binding.contentGroup.visibility = View.GONE
        }
    }

    private fun calculateProgress(value: Int, total: Int): Float {
        return if (total > 0) (value.toFloat() / total.toFloat()) * 100 else 0f
    }

    private fun updateSelectedDateDisplay() {
        if (_binding == null || !isAdded) return
        binding.selectedDateTextView.text = displayDateFormat.format(currentSelectedDate.time)
        val today = Calendar.getInstance().apply { clearTime() }
        binding.nextDateButton.alpha = if (currentSelectedDate.before(today)) 1.0f else 0.5f
    }

    private fun fetchLogsForCurrentDate() {
        if (!::viewModel.isInitialized) return
        val dateStr = dbDateFormat.format(currentSelectedDate.time)
        viewModel.fetchLogsForDate(dateStr)
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.metricDetailsRecyclerView?.adapter = null
        _binding?.dailyLogsRecyclerView?.adapter = null
        _binding?.progressBarsRecyclerView?.adapter = null
        _binding = null
        Log.d("DashBoardFragment", "onDestroyView: _binding set to null and adapters cleared")
    }
}