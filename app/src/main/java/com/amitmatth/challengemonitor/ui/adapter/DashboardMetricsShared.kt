package com.amitmatth.challengemonitor.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.amitmatth.challengemonitor.model.DashboardMetricItem

class DashboardMetricItemDiffCallback : DiffUtil.ItemCallback<DashboardMetricItem>() {
    override fun areItemsTheSame(
        oldItem: DashboardMetricItem,
        newItem: DashboardMetricItem
    ): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(
        oldItem: DashboardMetricItem,
        newItem: DashboardMetricItem
    ): Boolean {
        return oldItem == newItem
    }
}