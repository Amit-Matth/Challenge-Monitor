package com.amitmatth.challengemonitor.ui.adapter

import java.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.model.DateModel
import androidx.core.graphics.toColorInt

class MonthCalendarAdapter(
    private var dates: List<DateModel>,
    private val onDateSelected: (DateModel) -> Unit
) : RecyclerView.Adapter<MonthCalendarAdapter.DateViewHolder>() {

    private var selectedPosition = -1
    private var itemWidth: Int = 0

    init {
        selectedPosition = dates.indexOfFirst { it.isSelected && it.isCurrentMonth }
    }

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val ivDotIndicator: ImageView = itemView.findViewById(R.id.ivDotIndicator)
        val calendarItemContainer: View = itemView.findViewById(R.id.calendar_item_container)

        fun bind(dateModel: DateModel, position: Int) {
            val context = itemView.context
            tvDay.text = dateModel.day
            tvDate.text = dateModel.date

            if (dateModel.isCurrentMonth) {
                itemView.alpha = 1.0f
                itemView.isClickable = true
                tvDay.setTextColor("#AEAEAE".toColorInt())
                tvDate.setTextColor(ContextCompat.getColor(context, android.R.color.white))

                if (position == selectedPosition) {
                    calendarItemContainer.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_calendar_selected_new)
                    tvDay.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.calendar_selected_color
                        )
                    )
                    tvDate.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.calendar_selected_color
                        )
                    )
                    ivDotIndicator.visibility = View.VISIBLE
                } else {
                    calendarItemContainer.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_calendar_unselected_new)
                    ivDotIndicator.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    if (selectedPosition != position) {
                        val previousSelectedPosition = selectedPosition
                        selectedPosition = position

                        if (previousSelectedPosition != -1 && previousSelectedPosition < dates.size) {
                            dates[previousSelectedPosition].isSelected = false
                        }
                        dates[selectedPosition].isSelected = true

                        if (previousSelectedPosition != -1 && previousSelectedPosition < dates.size) {
                            notifyItemChanged(previousSelectedPosition)
                        }
                        notifyItemChanged(selectedPosition)

                        onDateSelected(dates[selectedPosition])
                    }
                }
            } else {
                itemView.alpha = 0.4f
                itemView.isClickable = false
                calendarItemContainer.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_calendar_unselected_new)
                tvDay.setTextColor("#AEAEAE".toColorInt())
                tvDate.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                ivDotIndicator.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_date, parent, false)
        if (itemWidth == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            itemWidth = view.measuredWidth
            if (itemWidth == 0) {
                val density = parent.context.resources.displayMetrics.density
                itemWidth = (56 * density).toInt()
            }
        }
        return DateViewHolder(view)
    }

    fun updateDates(newDates: List<DateModel>) {
        val diffCallback = DateModelDiffCallback(this.dates, newDates)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.dates = newDates
        this.selectedPosition = this.dates.indexOfFirst { it.isSelected && it.isCurrentMonth }
        diffResult.dispatchUpdatesTo(this)
    }

    fun setSelectedDate(newSelectedDate: Calendar) {
        val newSelectedNormalized = (newSelectedDate.clone() as Calendar).apply { clearTime() }

        var newPosition = -1
        for (i in dates.indices) {
            if (!dates[i].isCurrentMonth) continue
            val modelCalendarNormalized =
                (dates[i].calendar.clone() as Calendar).apply { clearTime() }
            if (modelCalendarNormalized.timeInMillis == newSelectedNormalized.timeInMillis) {
                newPosition = i
                break
            }
        }

        if (newPosition != -1 && newPosition != selectedPosition) {
            val previousSelectedPosition = selectedPosition
            selectedPosition = newPosition

            if (previousSelectedPosition != -1 && previousSelectedPosition < dates.size) {
                dates[previousSelectedPosition].isSelected = false
                notifyItemChanged(previousSelectedPosition)
            }
            if (selectedPosition < dates.size) {
                dates[selectedPosition].isSelected = true
                notifyItemChanged(selectedPosition)
            }
        } else if (newPosition != -1) {
            if (selectedPosition < dates.size && !dates[selectedPosition].isSelected) {
                dates[selectedPosition].isSelected = true
                notifyItemChanged(selectedPosition)
            } else if (selectedPosition < dates.size) {
                notifyItemChanged(selectedPosition)
            }
        }
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dates[position], position)
    }

    override fun getItemCount() = dates.size

    fun findPositionForDate(dateToFind: Calendar): Int {
        val normalizedDateToFind = (dateToFind.clone() as Calendar).apply { clearTime() }
        return dates.indexOfFirst { dateModel ->
            if (!dateModel.isCurrentMonth) return@indexOfFirst false
            val modelCalendarNormalized =
                (dateModel.calendar.clone() as Calendar).apply { clearTime() }
            modelCalendarNormalized.timeInMillis == normalizedDateToFind.timeInMillis
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private class DateModelDiffCallback(
        private val oldList: List<DateModel>,
        private val newList: List<DateModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            val oldCalNorm = (oldItem.calendar.clone() as Calendar).apply { clearTime() }
            val newCalNorm = (newItem.calendar.clone() as Calendar).apply { clearTime() }
            return oldCalNorm.timeInMillis == newCalNorm.timeInMillis && oldItem.isCurrentMonth == newItem.isCurrentMonth
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            val oldCalNorm = (oldItem.calendar.clone() as Calendar).apply { clearTime() }
            val newCalNorm = (newItem.calendar.clone() as Calendar).apply { clearTime() }
            return oldItem.day == newItem.day &&
                    oldItem.date == newItem.date &&
                    oldCalNorm.timeInMillis == newCalNorm.timeInMillis &&
                    oldItem.isSelected == newItem.isSelected &&
                    oldItem.isCurrentMonth == newItem.isCurrentMonth
        }

        private fun Calendar.clearTime() {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}