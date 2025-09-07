package com.amitmatth.challengemonitor.model

import java.util.Calendar

data class DateModel(
    val day: String,
    val date: String,
    val calendar: Calendar,
    var isSelected: Boolean = false,
    var isCurrentMonth: Boolean = true,
    var isToday: Boolean = false
)