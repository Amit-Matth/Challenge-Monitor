package com.amitmatth.challengemonitor.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.databinding.ItemMetricProgressBarOnlyBinding
import com.amitmatth.challengemonitor.model.DashboardMetricItem

class ProgressBarAdapter :
    ListAdapter<DashboardMetricItem, ProgressBarAdapter.ProgressBarViewHolder>(
        DashboardMetricItemDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressBarViewHolder {
        val binding = ItemMetricProgressBarOnlyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgressBarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressBarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProgressBarViewHolder(private val binding: ItemMetricProgressBarOnlyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(metric: DashboardMetricItem) {
            val targetProgress = metric.progress.toInt()
            val progressColor = ContextCompat.getColor(itemView.context, metric.progressColorRes)

            binding.metricProgressBar.setProgressColor(progressColor)

            binding.metricProgressBar.post {
                if (binding.metricProgressBar.isAttachedToWindow) {
                    binding.metricProgressBar.setProgressImmediate(0)
                    binding.metricProgressBar.setProgress(targetProgress)
                }
            }

            binding.metricValueTextView.text = metric.valueText
            binding.metricValueTextView.setTextColor(progressColor)
        }
    }
}