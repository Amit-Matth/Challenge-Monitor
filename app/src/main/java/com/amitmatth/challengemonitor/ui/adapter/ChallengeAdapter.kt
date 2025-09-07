package com.amitmatth.challengemonitor.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.databinding.ItemChallengeLayoutBinding
import com.amitmatth.challengemonitor.model.Challenge

class ChallengeAdapter(
    private var challenges: List<Challenge>,
    private val onItemClick: (Challenge) -> Unit,
    private val onChallengeMarkedDone: (challengeId: Long, date: String) -> Unit,
    private val onChallengeMarkedNotDone: (challenge: Challenge, date: String) -> Unit,
    private var selectedDateString: String,
    private var dailyLogStatusForSelectedDate: Map<Long, String>,
    private var challengeFollowedCounts: Map<Long, Int>,
    private val showFollowButtons: Boolean = true,
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding =
            ItemChallengeLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.bind(challenge)
    }

    override fun getItemCount(): Int = challenges.size

    fun updateChallenges(
        newChallenges: List<Challenge>,
        newSelectedDateString: String,
        newDailyLogStatus: Map<Long, String>,
        newChallengeFollowedCounts: Map<Long, Int>
    ) {
        val diffCallback = ChallengeDiffCallback(
            this.challenges,
            newChallenges,
            this.dailyLogStatusForSelectedDate,
            newDailyLogStatus,
            this.challengeFollowedCounts,
            newChallengeFollowedCounts
        )
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.challenges = newChallenges
        this.selectedDateString = newSelectedDateString
        this.dailyLogStatusForSelectedDate = newDailyLogStatus
        this.challengeFollowedCounts = newChallengeFollowedCounts
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ChallengeViewHolder(private val binding: ItemChallengeLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedChallenge = challenges[position]
                    onItemClick(clickedChallenge)
                }
            }
        }

        fun bind(challenge: Challenge) {
            binding.challengeTitle.text = challenge.title
            val statusText = "${challenge.daysLogged}/${challenge.durationDays} Days Overall"
            binding.challengeStatus.text = statusText

            binding.progressBar.setMaxProgress(challenge.durationDays)
            val progressColor = ContextCompat.getColor(
                binding.root.context,
                R.color.neon_cyan
            )
            binding.progressBar.setProgressColor(progressColor)

            val targetProgress = challenge.daysLogged

            binding.progressBar.post {
                if (binding.progressBar.isAttachedToWindow) {
                    binding.progressBar.setProgressImmediate(0)
                    binding.progressBar.setProgress(targetProgress)
                }
            }

            if (showFollowButtons) {
                binding.followBtn.visibility = View.VISIBLE
                binding.unfollowBtn.visibility = View.VISIBLE

                val currentStatus = dailyLogStatusForSelectedDate[challenge.id]
                binding.followBtn.alpha =
                    if (currentStatus == ChallengeDbHelper.STATUS_FOLLOWED) 1.0f else 0.5f
                binding.unfollowBtn.alpha =
                    if (currentStatus == ChallengeDbHelper.STATUS_NOT_FOLLOWED) 1.0f else 0.5f

                binding.followBtn.isEnabled = true
                binding.unfollowBtn.isEnabled = true

                binding.followBtn.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onChallengeMarkedDone(
                            challenges[bindingAdapterPosition].id,
                            selectedDateString
                        )
                    }
                }

                binding.unfollowBtn.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onChallengeMarkedNotDone(
                            challenges[bindingAdapterPosition],
                            selectedDateString
                        )
                    }
                }

            } else {
                binding.followBtn.visibility = View.GONE
                binding.unfollowBtn.visibility = View.GONE
            }
        }
    }

    private class ChallengeDiffCallback(
        private val oldList: List<Challenge>,
        private val newList: List<Challenge>,
        private val oldDailyLogStatus: Map<Long, String>,
        private val newDailyLogStatus: Map<Long, String>,
        private val oldChallengeFollowedCounts: Map<Long, Int>,
        private val newChallengeFollowedCounts: Map<Long, Int>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldChallenge = oldList[oldItemPosition]
            val newChallenge = newList[newItemPosition]
            return oldChallenge == newChallenge &&
                    oldDailyLogStatus[oldChallenge.id] == newDailyLogStatus[newChallenge.id] &&
                    oldChallengeFollowedCounts[oldChallenge.id] == newChallengeFollowedCounts[newChallenge.id]
        }
    }
}