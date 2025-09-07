package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentCompletedBinding
import com.amitmatth.challengemonitor.ui.adapter.ChallengeAdapter
import com.amitmatth.challengemonitor.viewmodel.ChallengeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CompletedFragment : Fragment() {

    private var _binding: FragmentCompletedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChallengeViewModel
    private lateinit var challengeAdapter: ChallengeAdapter

    private val dummySelectedDateString =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ChallengeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.refreshCompletedChallenges()
    }

    private fun setupRecyclerView() {
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
            selectedDateString = dummySelectedDateString,
            dailyLogStatusForSelectedDate = emptyMap(),
            challengeFollowedCounts = emptyMap(),
            showFollowButtons = false
        )

        binding.completedChallengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = challengeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.completedChallenges.observe(viewLifecycleOwner) { challenges ->
            if (challenges.isNullOrEmpty()) {
                binding.emptyStateTextViewCompleted.visibility = View.VISIBLE
                binding.completedChallengesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateTextViewCompleted.visibility = View.GONE
                binding.completedChallengesRecyclerView.visibility = View.VISIBLE
                challengeAdapter.updateChallenges(
                    challenges,
                    dummySelectedDateString,
                    emptyMap(),
                    emptyMap()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCompletedChallenges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}