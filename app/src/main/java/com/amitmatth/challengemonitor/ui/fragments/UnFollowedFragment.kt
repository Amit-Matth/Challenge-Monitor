package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentUnFollowedBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.HistoricalChallengeItem
import com.amitmatth.challengemonitor.ui.adapter.ChallengeAdapter
import com.amitmatth.challengemonitor.ui.adapter.HistoricalChallengesAdapter
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UnFollowedFragment : Fragment() {

    private var _binding: FragmentUnFollowedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var todayUnFollowedAdapter: ChallengeAdapter
    private lateinit var historicalUnFollowedAdapter: HistoricalChallengesAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnFollowedBinding.inflate(inflater, container, false)
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
        viewModel.fetchUnFollowedData(currentDate)
        binding.loadingProgressBarUnFollowed.visibility = View.VISIBLE
    }

    private fun setupRecyclerViews() {
        todayUnFollowedAdapter = ChallengeAdapter(
            emptyList(),
            onItemClick = { challenge -> navigateToDetails(challenge) },
            onChallengeMarkedDone = { _, _ -> },
            onChallengeMarkedNotDone = { _, _ -> },
            selectedDateString = dateFormat.format(Calendar.getInstance().time),
            dailyLogStatusForSelectedDate = emptyMap(),
            challengeFollowedCounts = emptyMap(),
            showFollowButtons = false
        )
        binding.recyclerViewTodayUnFollowed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayUnFollowedAdapter
        }

        historicalUnFollowedAdapter = HistoricalChallengesAdapter { challenge ->
            navigateToDetails(challenge)
        }
        binding.recyclerViewHistoricalUnFollowed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historicalUnFollowedAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.todayUnFollowedChallenges.observe(viewLifecycleOwner) { challenges ->
            binding.loadingProgressBarUnFollowed.visibility = View.GONE
            if (challenges.isNullOrEmpty()) {
                binding.emptyStateTextViewTodayUnFollowed.visibility = View.VISIBLE
                binding.recyclerViewTodayUnFollowed.visibility = View.GONE
            } else {
                binding.emptyStateTextViewTodayUnFollowed.visibility = View.GONE
                binding.recyclerViewTodayUnFollowed.visibility = View.VISIBLE
                val statusMap =
                    challenges.associate { it.id to com.amitmatth.challengemonitor.data.ChallengeDbHelper.STATUS_NOT_FOLLOWED }
                todayUnFollowedAdapter.updateChallenges(
                    challenges,
                    dateFormat.format(Calendar.getInstance().time),
                    statusMap,
                    emptyMap()
                )
            }
        }

        viewModel.historicalUnFollowedChallengesByDate.observe(viewLifecycleOwner) { historicalData ->
            binding.loadingProgressBarUnFollowed.visibility = View.GONE
            val historicalItems = mutableListOf<HistoricalChallengeItem>()
            if (historicalData.isNullOrEmpty()) {
                binding.emptyStateTextViewHistoricalUnFollowed.visibility = View.VISIBLE
                binding.recyclerViewHistoricalUnFollowed.visibility = View.GONE
            } else {
                binding.emptyStateTextViewHistoricalUnFollowed.visibility = View.GONE
                binding.recyclerViewHistoricalUnFollowed.visibility = View.VISIBLE
                historicalData.toSortedMap(compareByDescending { it })
                    .forEach { (date, challenges) ->
                        historicalItems.add(HistoricalChallengeItem.DateHeaderItem(date))
                        challenges.forEach {
                            historicalItems.add(HistoricalChallengeItem.ChallengeContentItem(it))
                        }
                    }
            }
            historicalUnFollowedAdapter.submitList(historicalItems)
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