package com.example.timeboxcoach

data class ReasonLog(
    val timestamp: Long,
    val blockedApp: String,
    val allowedWindow: String,
    val actualTime: String,
    val reason: String,
    val decision: String
)
