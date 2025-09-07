package com.amitmatth.challengemonitor.model

import java.util.Date

data class CalendarDay(
    val date: Date,
    val dayOfMonth: String,
    val isCurrentMonth: Boolean,
    val isChallengeDay: Boolean,
    var logStatus: String? = null,
    var isSelected: Boolean = false
)