package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.databinding.FragmentSkippedBinding
import com.amitmatth.challengemonitor.ui.adapter.ChallengeAdapter
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SkippedFragment : Fragment() {

    private var _binding: FragmentSkippedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var challengeAdapter: ChallengeAdapter
    private fun getCurrentDateString(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkippedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.refreshSkippedChallengesForDate(getCurrentDateString())
    }

    private fun setupRecyclerView() {
        val currentDateString = getCurrentDateString()
        challengeAdapter = ChallengeAdapter(
            emptyList(),
            onItemClick = { challenge ->
                val challengeDetailsFragment = ChallengeDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putLong("challengeId", challenge.id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, challengeDetailsFragment)
                    .addToBackStack(ChallengeDetailsFragment::class.java.simpleName)
                    .commit()
            },
            onChallengeMarkedDone = { _, _ -> },
            onChallengeMarkedNotDone = { _, _ -> },
            selectedDateString = currentDateString,
            dailyLogStatusForSelectedDate = emptyMap(),
            challengeFollowedCounts = emptyMap(),
            showFollowButtons = false
        )

        binding.skippedChallengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = challengeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.skippedChallengesForDate.observe(viewLifecycleOwner) { challenges ->
            val currentDateString = getCurrentDateString()
            if (challenges.isNullOrEmpty()) {
                binding.emptyStateTextViewSkipped.visibility = View.VISIBLE
                binding.skippedChallengesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateTextViewSkipped.visibility = View.GONE
                binding.skippedChallengesRecyclerView.visibility = View.VISIBLE
                val dailyStatusMap = challenges.associate {
                    it.id to ChallengeDbHelper.STATUS_SKIPPED
                }
                challengeAdapter.updateChallenges(
                    challenges,
                    currentDateString,
                    dailyStatusMap,
                    emptyMap()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSkippedChallengesForDate(getCurrentDateString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}