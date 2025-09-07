package com.amitmatth.challengemonitor.model

data class ChallengeWithStreakInfo(
    val challenge: Challenge,
    val currentStreak: Int,
    val longestStreak: Int
)