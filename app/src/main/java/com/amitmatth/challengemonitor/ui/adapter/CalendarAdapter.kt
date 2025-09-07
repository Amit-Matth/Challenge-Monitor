package com.amitmatth.challengemonitor.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.model.CalendarDay
import java.util.Calendar
import java.util.Date

class CalendarAdapter(
    private val context: Context,
    private var days: List<CalendarDay>,
    private val onDateClickListener: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    @Suppress("unused")
    private var challengeStartDate: Date? = null

    @Suppress("unused")
    private var challengeEndDate: Date? = null

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayNumberTextView: TextView = itemView.findViewById(R.id.dayNumberTextView)
        val logStatusIndicatorImageView: ImageView =
            itemView.findViewById(R.id.logStatusIndicatorImageView)
        val dayCellContainer: View = itemView.findViewById(R.id.dayCellContainer)

        fun bind(calendarDay: CalendarDay) {
            dayNumberTextView.text = calendarDay.dayOfMonth

            var currentTextColor = ContextCompat.getColor(context, R.color.white_shadowed)
            var currentBackground =
                ContextCompat.getDrawable(context, R.drawable.bg_calendar_unselected_new)
            dayNumberTextView.alpha = 1f
            logStatusIndicatorImageView.visibility = View.GONE
            dayNumberTextView.setTypeface(null, Typeface.NORMAL)

            val todayCalendar = Calendar.getInstance()
            val cellCalendar = Calendar.getInstance().apply { time = calendarDay.date }
            val isTodayFlag = todayCalendar.get(Calendar.YEAR) == cellCalendar.get(Calendar.YEAR) &&
                    todayCalendar.get(Calendar.DAY_OF_YEAR) == cellCalendar.get(Calendar.DAY_OF_YEAR)

            if (calendarDay.isCurrentMonth) {
                if (calendarDay.isChallengeDay) {
                    dayCellContainer.setOnClickListener { onDateClickListener(calendarDay) }
                    dayNumberTextView.setTypeface(null, Typeface.BOLD)
                    logStatusIndicatorImageView.visibility = View.VISIBLE

                    when (calendarDay.logStatus) {
                        ChallengeDbHelper.STATUS_FOLLOWED -> {
                            logStatusIndicatorImageView.setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    R.color.neonGreen
                                )
                            )
                            currentBackground =
                                ContextCompat.getDrawable(context, R.drawable.bg_outline_followed)
                        }

                        ChallengeDbHelper.STATUS_NOT_FOLLOWED -> {
                            logStatusIndicatorImageView.setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    R.color.neonRed
                                )
                            )
                            currentBackground = ContextCompat.getDrawable(
                                context,
                                R.drawable.bg_outline_not_followed
                            )
                        }

                        ChallengeDbHelper.STATUS_SKIPPED -> {
                            logStatusIndicatorImageView.setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    R.color.orange_skipped
                                )
                            )
                            currentBackground =
                                ContextCompat.getDrawable(context, R.drawable.bg_outline_skipped)
                        }

                        else -> {
                            logStatusIndicatorImageView.setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    R.color.neonBlue
                                )
                            )
                            currentBackground =
                                ContextCompat.getDrawable(context, R.drawable.bg_outline_pending)
                        }
                    }

                    if (isTodayFlag) {
                        currentTextColor = ContextCompat.getColor(context, R.color.neonPink)
                        val isExplicitStatus =
                            calendarDay.logStatus == ChallengeDbHelper.STATUS_FOLLOWED ||
                                    calendarDay.logStatus == ChallengeDbHelper.STATUS_NOT_FOLLOWED ||
                                    calendarDay.logStatus == ChallengeDbHelper.STATUS_SKIPPED
                        if (!isExplicitStatus) {
                            logStatusIndicatorImageView.setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    R.color.neonPink
                                )
                            )
                        }
                    }

                    if (calendarDay.isSelected) {
                        currentBackground =
                            ContextCompat.getDrawable(context, R.drawable.bg_outline_selected)
                        currentTextColor = ContextCompat.getColor(context, R.color.neonPink)
                        logStatusIndicatorImageView.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.neonPink
                            )
                        )
                    }

                } else {
                    dayNumberTextView.alpha = 0.5f
                    currentTextColor = ContextCompat.getColor(context, R.color.grey_light)
                    dayCellContainer.setOnClickListener(null)
                    logStatusIndicatorImageView.visibility = View.GONE

                    if (isTodayFlag) {
                        currentTextColor = ContextCompat.getColor(context, R.color.neonPink)
                        dayNumberTextView.alpha = 0.7f
                        currentBackground =
                            ContextCompat.getDrawable(context, R.drawable.bg_outline_pending)
                    }
                }
            } else {
                dayNumberTextView.alpha = 0.5f
                currentTextColor = ContextCompat.getColor(context, R.color.grey_light_transparent)
                dayCellContainer.setOnClickListener(null)
                logStatusIndicatorImageView.visibility = View.GONE

                if (isTodayFlag) {
                    currentTextColor = ContextCompat.getColor(context, R.color.neonPink)
                    dayNumberTextView.alpha = 0.6f
                }
            }

            dayNumberTextView.setTextColor(currentTextColor)
            dayCellContainer.background = currentBackground
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateData(newDays: List<CalendarDay>, challengeStart: Date?, challengeEnd: Date?) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = days.size
            override fun getNewListSize() = newDays.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return days[oldItemPosition].date == newDays[newItemPosition].date
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return days[oldItemPosition] == newDays[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        days = newDays
        challengeStartDate = challengeStart
        challengeEndDate = challengeEnd
        diffResult.dispatchUpdatesTo(this)
    }
}