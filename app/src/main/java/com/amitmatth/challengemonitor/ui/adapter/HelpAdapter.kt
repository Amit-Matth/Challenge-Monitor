package com.amitmatth.challengemonitor.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.model.HelpTopic

class HelpAdapter : ListAdapter<HelpTopic, HelpAdapter.HelpViewHolder>(HelpTopicDiffCallback()) {

    class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val screenNameTextView: TextView = itemView.findViewById(R.id.textViewScreenName)
        val screenDescriptionTextView: TextView = itemView.findViewById(R.id.textViewScreenDescription)

        fun bind(topic: HelpTopic) {
            screenNameTextView.text = topic.screenName
            screenDescriptionTextView.text = topic.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_topic, parent, false)
        return HelpViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HelpTopicDiffCallback : DiffUtil.ItemCallback<HelpTopic>() {
        override fun areItemsTheSame(oldItem: HelpTopic, newItem: HelpTopic): Boolean {
            // Assuming screenName is a unique identifier for HelpTopic,
            // or if HelpTopic is a data class and instances are identical.
            // If HelpTopic has a unique ID field, that would be better to compare here.
            return oldItem.screenName == newItem.screenName 
        }

        override fun areContentsTheSame(oldItem: HelpTopic, newItem: HelpTopic): Boolean {
            return oldItem == newItem // Relies on HelpTopic being a data class or having a proper equals()
        }
    }
}