package com.amitmatth.challengemonitor.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.ItemChallengeLayoutBinding
import com.amitmatth.challengemonitor.databinding.ItemHistoricalDateHeaderBinding
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.HistoricalChallengeItem

class HistoricalChallengesAdapter(
    private val onItemClick: (Challenge) -> Unit
) : ListAdapter<HistoricalChallengeItem, RecyclerView.ViewHolder>(HistoricalChallengeDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_CHALLENGE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoricalChallengeItem.DateHeaderItem -> VIEW_TYPE_DATE_HEADER
            is HistoricalChallengeItem.ChallengeContentItem -> VIEW_TYPE_CHALLENGE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemHistoricalDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DateHeaderViewHolder(binding)
            }

            VIEW_TYPE_CHALLENGE_ITEM -> {
                val binding = ItemChallengeLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ChallengeContentViewHolder(binding, onItemClick)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateHeaderViewHolder -> {
                val item = getItem(position) as HistoricalChallengeItem.DateHeaderItem
                holder.bind(item)
            }

            is ChallengeContentViewHolder -> {
                val item = getItem(position) as HistoricalChallengeItem.ChallengeContentItem
                holder.bind(item.challenge)
            }
        }
    }

    class DateHeaderViewHolder(private val binding: ItemHistoricalDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoricalChallengeItem.DateHeaderItem) {
            binding.textViewDateHeader.text = item.date
        }
    }

    class ChallengeContentViewHolder(
        private val binding: ItemChallengeLayoutBinding,
        private val onItemClick: (Challenge) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(challenge: Challenge) {
            binding.challengeTitle.text = challenge.title
            val statusText = "${challenge.daysLogged}/${challenge.durationDays} Days Overall"
            binding.challengeStatus.text = statusText

            binding.progressBar.setMaxProgress(challenge.durationDays)
            val progressColor = ContextCompat.getColor(binding.root.context, R.color.neon_cyan)
            binding.progressBar.setProgressColor(progressColor)

            val targetProgress = challenge.daysLogged

            binding.progressBar.post {
                if (binding.progressBar.isAttachedToWindow) {
                    binding.progressBar.setProgressImmediate(0)
                    binding.progressBar.setProgress(targetProgress)
                }
            }

            binding.followBtn.visibility = ViewGroup.GONE
            binding.unfollowBtn.visibility = ViewGroup.GONE

            itemView.setOnClickListener {
                onItemClick(challenge)
            }
        }
    }

    class HistoricalChallengeDiffCallback : DiffUtil.ItemCallback<HistoricalChallengeItem>() {
        override fun areItemsTheSame(
            oldItem: HistoricalChallengeItem,
            newItem: HistoricalChallengeItem
        ): Boolean {
            return if (oldItem is HistoricalChallengeItem.DateHeaderItem && newItem is HistoricalChallengeItem.DateHeaderItem) {
                oldItem.date == newItem.date
            } else if (oldItem is HistoricalChallengeItem.ChallengeContentItem && newItem is HistoricalChallengeItem.ChallengeContentItem) {
                oldItem.challenge.id == newItem.challenge.id
            } else {
                false
            }
        }

        override fun areContentsTheSame(
            oldItem: HistoricalChallengeItem,
            newItem: HistoricalChallengeItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}