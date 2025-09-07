package com.amitmatth.challengemonitor.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.ItemMetricDetailBinding
import com.amitmatth.challengemonitor.model.DashboardMetricItem

class MetricDetailsAdapter :
    ListAdapter<DashboardMetricItem, MetricDetailsAdapter.MetricDetailViewHolder>(
        DashboardMetricItemDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricDetailViewHolder {
        val binding = ItemMetricDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MetricDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetricDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: MetricDetailViewHolder) {
        super.onViewRecycled(holder)
        holder.clearAnimation()
    }

    class MetricDetailViewHolder(private val binding: ItemMetricDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var rotationAnimation: Animation? = null

        fun bind(metric: DashboardMetricItem) {
            binding.metricDetailTitle.text = metric.title
            binding.metricDetailValueText.text = metric.valueText

            val detailColor = ContextCompat.getColor(itemView.context, metric.progressColorRes)
            binding.metricDetailIcon.imageTintList = ColorStateList.valueOf(detailColor)
            binding.metricDetailTitle.setTextColor(detailColor)
            binding.metricDetailValueText.setTextColor(detailColor)

            if (rotationAnimation == null) {
                rotationAnimation =
                    AnimationUtils.loadAnimation(itemView.context, R.anim.rotate_indefinitely)
            }
            binding.metricDetailIcon.startAnimation(rotationAnimation)
        }

        fun clearAnimation() {
            binding.metricDetailIcon.clearAnimation()
        }
    }
}