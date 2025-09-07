package com.amitmatth.challengemonitor.ui.adapter

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.ItemStreakChallengeBinding
import com.amitmatth.challengemonitor.model.ChallengeWithStreakInfo

class StreakAdapter(
    private val onItemClick: (ChallengeWithStreakInfo) -> Unit
) : ListAdapter<ChallengeWithStreakInfo, StreakAdapter.StreakViewHolder>(StreakDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreakViewHolder {
        val binding =
            ItemStreakChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StreakViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: StreakViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StreakViewHolder(
        private val binding: ItemStreakChallengeBinding,
        private val onItemClick: (ChallengeWithStreakInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(streakInfo: ChallengeWithStreakInfo) {
            binding.textViewChallengeTitle.text = streakInfo.challenge.title

            val currentText = "Current Streak: ${streakInfo.currentStreak} days"
            val currentSpannable = SpannableString(currentText)
            val currentStart = currentText.indexOf("${streakInfo.currentStreak}")
            val currentEnd = currentStart + "${streakInfo.currentStreak} days".length
            currentSpannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.pink_shadowed
                    )
                ),
                currentStart,
                currentEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.textViewCurrentStreak.text = currentSpannable

            val longestText = "Longest Streak: ${streakInfo.longestStreak} days"
            val longestSpannable = SpannableString(longestText)
            val longestStart = longestText.indexOf("${streakInfo.longestStreak}")
            val longestEnd = longestStart + "${streakInfo.longestStreak} days".length
            longestSpannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.pink_shadowed
                    )
                ),
                longestStart,
                longestEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.textViewLongestStreak.text = longestSpannable

            itemView.setOnClickListener {
                onItemClick(streakInfo)
            }
        }
    }

    class StreakDiffCallback : DiffUtil.ItemCallback<ChallengeWithStreakInfo>() {
        override fun areItemsTheSame(
            oldItem: ChallengeWithStreakInfo,
            newItem: ChallengeWithStreakInfo
        ): Boolean {
            return oldItem.challenge.id == newItem.challenge.id
        }

        override fun areContentsTheSame(
            oldItem: ChallengeWithStreakInfo,
            newItem: ChallengeWithStreakInfo
        ): Boolean {
            return oldItem == newItem
        }
    }
}