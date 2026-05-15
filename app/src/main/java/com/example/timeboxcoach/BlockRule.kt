package com.example.timeboxcoach

data class BlockRule(
    val packageName: String,
    val appName: String,
    val allowedStart: String,  // "HH:mm"
    val allowedEnd: String     // "HH:mm"
)
