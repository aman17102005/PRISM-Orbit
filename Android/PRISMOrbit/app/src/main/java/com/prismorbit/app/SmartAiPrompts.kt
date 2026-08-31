package com.prismorbit.app

// ============================================================
// SMART AI PROMPT TEMPLATES
// ============================================================
//
// Centralizes quick-action prompt text so SmartAiChatScreen.kt
// stays focused on UI. Phase 7 (dynamic planning) will add its
// prompt template here too.
// ============================================================

object SmartAiPrompts {

    const val DAILY_REPORT_DISPLAY_TEXT = "What's my report for today?"

    val DAILY_REPORT_PROMPT = """
        Give me my report for today. Structure your answer using exactly
        these three headers: HIGH PRIORITY, MEDIUM PRIORITY, OPTIONAL.

        Base every item strictly on the real PRISM data provided above —
        today's and upcoming academic events, current DSA progress,
        project status, internship count, and the placement insight.

        Rules:
        - If a category has nothing relevant, write "Nothing here today"
          under that header instead of inventing filler.
        - Do not list an event, deadline, or statistic that isn't explicitly
          present in the data above.
        - Keep each bullet to one short line.
        - End with one sentence naming the single most important thing to
          do first today, drawn from HIGH PRIORITY if it has any items.
    """.trimIndent()
}