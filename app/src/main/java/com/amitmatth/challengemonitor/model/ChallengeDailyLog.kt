package com.amitmatth.challengemonitor.model

data class ChallengeDailyLog(
    val id: Long = 0,
    val challengeId: Long,
    val logDate: String,
    var status: String,
    val notes: String? = null,
    val lastUpdatedTime: String = ""
)