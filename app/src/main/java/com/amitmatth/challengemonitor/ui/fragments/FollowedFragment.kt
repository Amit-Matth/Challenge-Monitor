package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentFollowedBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.HistoricalChallengeItem
import com.amitmatth.challengemonitor.ui.adapter.ChallengeAdapter
import com.amitmatth.challengemonitor.ui.adapter.HistoricalChallengesAdapter
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FollowedFragment : Fragment() {

    private var _binding: FragmentFollowedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var todayFollowedAdapter: ChallengeAdapter
    private lateinit var historicalFollowedAdapter: HistoricalChallengesAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        val currentDate = dateFormat.format(Calendar.getInstance().time)
        viewModel.fetchFollowedData(currentDate)
        binding.loadingProgressBarFollowed.visibility = View.VISIBLE
    }

    private fun setupRecyclerViews() {
        todayFollowedAdapter = ChallengeAdapter(
            emptyList(),
            onItemClick = { challenge -> navigateToDetails(challenge) },
            onChallengeMarkedDone = { _, _ -> },
            onChallengeMarkedNotDone = { _, _ -> },
            selectedDateString = dateFormat.format(Calendar.getInstance().time),
            dailyLogStatusForSelectedDate = emptyMap(),
            challengeFollowedCounts = emptyMap(),
            showFollowButtons = false
        )
        binding.recyclerViewTodayFollowed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayFollowedAdapter
        }

        historicalFollowedAdapter = HistoricalChallengesAdapter { challenge ->
            navigateToDetails(challenge)
        }
        binding.recyclerViewHistoricalFollowed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historicalFollowedAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.todayFollowedChallenges.observe(viewLifecycleOwner) { challenges ->
            binding.loadingProgressBarFollowed.visibility = View.GONE
            if (challenges.isNullOrEmpty()) {
                binding.emptyStateTextViewTodayFollowed.visibility = View.VISIBLE
                binding.recyclerViewTodayFollowed.visibility = View.GONE
            } else {
                binding.emptyStateTextViewTodayFollowed.visibility = View.GONE
                binding.recyclerViewTodayFollowed.visibility = View.VISIBLE

                val statusMap =
                    challenges.associate { it.id to com.amitmatth.challengemonitor.data.ChallengeDbHelper.STATUS_FOLLOWED }
                todayFollowedAdapter.updateChallenges(
                    challenges,
                    dateFormat.format(Calendar.getInstance().time),
                    statusMap,
                    emptyMap()
                )
            }
        }

        viewModel.historicalFollowedChallengesByDate.observe(viewLifecycleOwner) { historicalData ->
            binding.loadingProgressBarFollowed.visibility = View.GONE
            val historicalItems = mutableListOf<HistoricalChallengeItem>()
            if (historicalData.isNullOrEmpty()) {
                binding.emptyStateTextViewHistoricalFollowed.visibility = View.VISIBLE
                binding.recyclerViewHistoricalFollowed.visibility = View.GONE
            } else {
                binding.emptyStateTextViewHistoricalFollowed.visibility = View.GONE
                binding.recyclerViewHistoricalFollowed.visibility = View.VISIBLE
                historicalData.toSortedMap(compareByDescending { it })
                    .forEach { (date, challenges) ->
                        historicalItems.add(HistoricalChallengeItem.DateHeaderItem(date))
                        challenges.forEach {
                            historicalItems.add(HistoricalChallengeItem.ChallengeContentItem(it))
                        }
                    }
            }
            historicalFollowedAdapter.submitList(historicalItems)
        }
    }

    private fun navigateToDetails(challenge: Challenge) {
        val challengeDetailsFragment = ChallengeDetailsFragment().apply {
            arguments = Bundle().apply {
                putLong("challengeId", challenge.id)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, challengeDetailsFragment)
            .addToBackStack(ChallengeDetailsFragment::class.java.simpleName)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}