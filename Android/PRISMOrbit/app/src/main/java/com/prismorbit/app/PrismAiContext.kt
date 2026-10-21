package com.prismorbit.app

// ============================================================
// PRISM AI CONTEXT — DATA MODEL
// ============================================================
//
// This is the small, structured summary of the student's real
// PRISM data that gets handed to Gemini. It is intentionally
// compact — counts and short summaries, not raw Firestore dumps.
//
// placementInsight reuses the EXISTING SmartAiInsight from
// SmartAiEngine.kt unchanged — this is the "ground truth" the
// LLM must defer to, never invent its own version of.
// ============================================================

data class PrismAiContext(
    val todayDateText: String,
    val upcomingEvents: List<UpcomingEventSummary>,
    val dsaSummary: DsaSummary,
    val projectsSummary: ProjectsSummary,
    val internshipCount: Int,
    val placementInsight: SmartAiInsight?,
    val internshipItems: List<InternshipLevelSummary> = emptyList(),
    val scores: ScoreSummary? = null
)

/** The same numbers every PRISM screen shows (from PrismScoring). */
data class ScoreSummary(
    val readiness: Int,
    val academics: Int,
    val dsa: Int,
    val projects: Int,
    val internships: Int,
    val skills: Int,
    val evidence: Int
)

data class ProjectLevelSummary(
    val name: String,
    val status: String,
    val levelLabel: String,
    val reason: String,
    val githubCheck: String,
    val liveCheck: String
)

data class InternshipLevelSummary(
    val company: String,
    val role: String,
    val status: String,
    val levelLabel: String,
    val reason: String,
    val proof: String
)

data class UpcomingEventSummary(
    val title: String,
    val subject: String,
    val type: String,
    val daysUntil: Int
)

data class DsaSummary(
    val totalSolved: Int,
    val easyCount: Int,
    val mediumCount: Int,
    val hardCount: Int,
    val weightedScorePercent: Int
)

data class ProjectsSummary(
    val totalProjects: Int,
    val averageProgressPercent: Int,
    val completedCount: Int,
    val items: List<ProjectLevelSummary> = emptyList()
)

// ============================================================
// Converts the context into a compact plain-text block for the
// Gemini prompt. Kept as plain sentences, not JSON — cheaper in
// tokens and easier for the model to read directly.
// ============================================================

fun PrismAiContext.toPromptText(): String {
    val eventsText = if (upcomingEvents.isEmpty()) {
        "No upcoming academic events in the next 7 days."
    } else {
        upcomingEvents.joinToString("\n") { event ->
            val whenText = when (event.daysUntil) {
                0 -> "today"
                1 -> "tomorrow"
                else -> "in ${event.daysUntil} days"
            }
            "- ${event.type}: ${event.title} (${event.subject}) — $whenText"
        }
    }

    val placementText = placementInsight?.let { insight ->
        """
        Placement/priority insight (from PRISM's scoring engine — treat as fact, do not recalculate):
        - Category: ${insight.category}
        - Priority: ${insight.priority}/100
        - Summary: ${insight.title} ${insight.message}
        - Reason: ${insight.reason}
        - Recommended action: ${insight.action}
        """.trimIndent()
    } ?: "Placement insight not currently available."

    val scoresText = scores?.let {
        """
        PRISM SCORES (the same numbers shown on the Home, Placement and Growth screens):
        - Overall readiness: ${it.readiness}/100
        - Academics ${it.academics}, DSA ${it.dsa}, Projects ${it.projects}, Internships ${it.internships}, Skills ${it.skills}, Achievements ${it.evidence}
        """.trimIndent()
    } ?: ""

    val projectLines = if (projectsSummary.items.isEmpty()) {
        ""
    } else {
        "\n" + projectsSummary.items.joinToString("\n") { item ->
            "- ${item.name} (${item.status}): ${item.levelLabel}. ${item.reason} GitHub: ${item.githubCheck}. Live link: ${item.liveCheck}."
        }
    }

    val internshipLines = if (internshipItems.isEmpty()) {
        ""
    } else {
        "\n" + internshipItems.joinToString("\n") { item ->
            "- ${item.company}${if (item.role.isBlank()) "" else " - ${item.role}"} (${item.status}): ${item.levelLabel}. ${item.reason} Proof: ${item.proof}."
        }
    }

    return """
        Today's date: $todayDateText

        $scoresText

        UPCOMING ACADEMIC EVENTS (next 7 days):
        $eventsText

        DSA PROGRESS:
        - Total solved: ${dsaSummary.totalSolved}
        - Easy: ${dsaSummary.easyCount}, Medium: ${dsaSummary.mediumCount}, Hard: ${dsaSummary.hardCount}
        - Weighted progress toward target: ${dsaSummary.weightedScorePercent}%

        PROJECTS:
        - Total: ${projectsSummary.totalProjects}
        - Completed: ${projectsSummary.completedCount}
        - Average progress: ${projectsSummary.averageProgressPercent}%
        Project levels (1-5, worked out by PRISM's rules; the level changes the project marks, treat as fact):$projectLines

        INTERNSHIPS:
        - Tracked applications: $internshipCount
        Internship levels (1-5, worked out by PRISM's rules; the level fully counts only when SELECTED):$internshipLines

        $placementText
    """.trimIndent()
}