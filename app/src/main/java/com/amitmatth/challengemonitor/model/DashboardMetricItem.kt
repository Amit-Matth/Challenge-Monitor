package com.amitmatth.challengemonitor.model

data class DashboardMetricItem(
    val title: String,
    val progress: Float,
    val progressColorRes: Int,
    val valueText: String,
)