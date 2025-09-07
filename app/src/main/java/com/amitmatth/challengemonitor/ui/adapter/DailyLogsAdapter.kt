package com.amitmatth.challengemonitor.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.ItemNotableDateBinding
import com.amitmatth.challengemonitor.model.DailyLogDisplayItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyLogsAdapter :
    ListAdapter<DailyLogDisplayItem, DailyLogsAdapter.LogViewHolder>(DailyLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemNotableDateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemNotableDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val timeParser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat(
            "hh:mm:aa ", Locale.getDefault()
        )

        fun bind(logItem: DailyLogDisplayItem) {
            binding.logItemChallengeTitle.text = logItem.challengeTitle
            val context = binding.root.context

            if (logItem.logTime.isNotBlank() && logItem.logTime != "N/A") {
                try {
                    val parsedTime: Date? = timeParser.parse(logItem.logTime)
                    if (parsedTime != null) {
                        binding.logItemTime.text = timeFormatter.format(parsedTime)
                    } else {
                        binding.logItemTime.text =
                            logItem.logTime
                    }
                } catch (_: Exception) {
                    binding.logItemTime.text =
                        logItem.logTime
                }
            } else {
                binding.logItemTime.text = logItem.logTime
            }

            binding.logItemStatus.text =
                context.getString(R.string.log_item_status, logItem.status)

            if (logItem.notes.isNullOrBlank()) {
                binding.logItemNotes.visibility = View.GONE
            } else {
                binding.logItemNotes.visibility = View.VISIBLE
                binding.logItemNotes.text =
                    context.getString(R.string.log_item_notes, logItem.notes)
            }
        }
    }

    class DailyLogDiffCallback : DiffUtil.ItemCallback<DailyLogDisplayItem>() {
        override fun areItemsTheSame(
            oldItem: DailyLogDisplayItem,
            newItem: DailyLogDisplayItem
        ): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(
            oldItem: DailyLogDisplayItem,
            newItem: DailyLogDisplayItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}