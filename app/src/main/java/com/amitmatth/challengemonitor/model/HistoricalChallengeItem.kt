package com.amitmatth.challengemonitor.model

sealed class HistoricalChallengeItem {
    data class DateHeaderItem(val date: String) : HistoricalChallengeItem()
    data class ChallengeContentItem(val challenge: Challenge) : HistoricalChallengeItem()
}