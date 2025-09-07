package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.amitmatth.challengemonitor.databinding.FragmentHelpBinding
import com.amitmatth.challengemonitor.model.HelpTopic
import com.amitmatth.challengemonitor.ui.adapter.HelpAdapter

class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    private lateinit var helpAdapter: HelpAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadHelpTopics()
    }

    private fun setupRecyclerView() {
        helpAdapter = HelpAdapter()
        binding.helpRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = helpAdapter
        }
    }

    private fun loadHelpTopics() {
        val newHelpTopics = listOf(
            HelpTopic(
                "Home Screen",
                "The Home screen is your main dashboard. It displays a monthly calendar at the top. You can tap on any date to see challenges active on that day. Below the calendar, you'll find a list of these challenges. You can mark them as followed, not followed (with optional notes), or skipped directly from here. Swipe the calendar left or right to navigate between months. The FAB (Floating Action Button) at the bottom lets you quickly create a new challenge."
            ),
            HelpTopic(
                "Skipped Screen",
                "If you not logged a challenge for a current day than it automatically skipped at the days end. And show here only skipped challenges that are manually or automatically skipped."
            ),
            HelpTopic(
                "Streaks Screen",
                "Shows challenge streaks overview that is which challenge is regularly followed. If the consistency breaks than the streaks reset to zero and the last streaks saved."
            ),
            HelpTopic(
                "Create Challenge Screen",
                "Access this screen by tapping the '+' FAB on the Home screen or the 'Edit' button on the Challenge Details screen. Here, you define your challenge: give it a title, an optional description, set a start date, and choose a duration (e.g., 7 days, 30 days, or a custom number of days). The end date is calculated automatically based on the start date and duration. For custom durations, you can also set the end date manually if you prefer."
            ),
            HelpTopic(
                "Challenge Details Screen",
                "Tap on any challenge card on the Home screen to open its details. This screen shows all information about the challenge, including its title, description, start/end dates, and overall duration. From that you can edit , skipped or delete a challenge. A calendar view highlights your progress for the selected month (days followed, not followed, skipped, or pending). Below the calendar, you'll see a history of your daily logs for the selected date on the calendar, including any notes you've added."
            ),
            HelpTopic(
                "Dashboard Screen",
                "Accessible from the bottom navigation bar, the Dashboard provides an overview of your progress across all challenges. It features metrics like your current day progres that is how many challenges logged, not logged, followed, not followed for today. And also a complete history of the a selected particular date."
            ),
        )
        helpAdapter.submitList(newHelpTopics)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}