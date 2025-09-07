package com.amitmatth.challengemonitor.model

import java.util.Date

data class DailyLogDisplayItem(
    val logId: Long,
    val challengeId: Long,
    val challengeTitle: String,
    val status: String,
    val notes: String?,
    val logTime: String,
    val logFullDateTime: Date
)