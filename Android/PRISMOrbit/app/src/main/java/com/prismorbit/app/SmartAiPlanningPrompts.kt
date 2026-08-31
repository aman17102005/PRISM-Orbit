package com.prismorbit.app

// ============================================================
// SMART AI — DYNAMIC PLANNING PROMPTS (Phase 7)
// ============================================================
//
// Generates the natural-language message sent to Gemini when the
// student taps a duration quick-action chip.
// ============================================================

object SmartAiPlanningPrompts {

    // Chip label -> natural phrase used inside the message sent to Gemini
    val DURATION_OPTIONS = listOf(
        "30 min" to "30 minutes",
        "1 hr" to "1 hour",
        "2 hr" to "2 hours",
        "4 hr" to "4 hours"
    )

    fun buildPlanningMessage(durationPhrase: String): String {
        return "I have about $durationPhrase available today. Based on my real " +
                "PRISM data above (today's events, DSA progress, project status, and " +
                "placement priorities), what should I prioritize, and suggest a " +
                "realistic time-blocked plan for that time. Do not invent tasks or " +
                "deadlines that aren't in the data — if there isn't enough real, " +
                "concrete work to fill the time, say so honestly instead of padding " +
                "the plan."
    }
}