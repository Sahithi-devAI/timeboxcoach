package com.example.timeboxcoach

object ReasonEvaluator {

    private val longTermSignals = listOf(
        // Work / career
        "work", "deadline", "project", "meeting", "client", "job", "career", "task", "report",
        // Education
        "study", "exam", "homework", "assignment", "research", "course", "learn", "class", "lecture",
        // Health
        "health", "doctor", "medical", "appointment", "exercise", "fitness", "diet", "therapy",
        // Relationships / commitments
        "family", "friend", "birthday", "event", "coordinate", "emergency", "urgent",
        // Personal growth
        "goal", "habit", "improve", "practice", "progress", "plan", "commit"
    )

    private val impulseSignals = listOf(
        "bored", "just checking", "timepass", "scroll", "nothing", "kill time",
        "just want", "just to see", "no reason", "curious", "random", "fun", "pass time"
    )

    // Returns true only if the reason shows real long-term life impact.
    fun evaluate(reason: String): Boolean {
        if (reason.length < 15) return false
        val lowered = reason.lowercase()
        if (impulseSignals.any { lowered.contains(it) }) return false
        return longTermSignals.any { lowered.contains(it) }
    }
}
