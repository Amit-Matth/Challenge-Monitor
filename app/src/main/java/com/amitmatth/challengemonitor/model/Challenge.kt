package com.amitmatth.challengemonitor.model

import java.time.LocalDateTime

data class Challenge(
    val id: Long = 0,
    val title: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val durationDays: Int,
    var isActive: Boolean = true,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var isCompleted: Boolean = false,
    var daysLogged: Int,
)