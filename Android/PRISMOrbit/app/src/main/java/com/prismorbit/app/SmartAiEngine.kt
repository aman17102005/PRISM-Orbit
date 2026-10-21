package com.prismorbit.app

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// ============================================================
// PRISM ORBIT — SMART AI v0.3 ENGINE
// ============================================================
//
// SMART AI uses only data already stored by PRISM Orbit.
//
// FINAL DESIGN WEIGHTS (read from PrismScoring - the single source)
// Technical Skills                         15%
// Projects / Practical Evidence            20%
// DSA / Problem Solving                    25%
// Internship / Real Experience             15%
// Academics / CGPA                         15%
// Achievements + Certifications + Learning 10%
// Growth / Consistency                      0% (coaching signal only)
//
// v0.3 ARCHITECTURE
// • Base weights are the PrismScoring readiness weights.
// • Optional role-aware multipliers are normalized back to 100%.
// • Trend only activates when existing timestamp data is present.
// • Target CGPA affects context, never the academic weight.
//
// IMPORTANT RULES
// • Test / Demo / Live URL is completely ignored.
// • A raw GitHub URL is not scored; analyzed repository evidence can contribute to Project practical evidence.
// • GitHub authorship / AI-code usage is not evaluated.
// • AI Engineering is not used because PRISM has no such dataset.
// • Missing data is not silently treated as poor performance.
// • The old "CGPA gap >= 1.0 => priority 95" rule is removed.
// • Priority is an action-urgency signal, NOT a hiring probability.
// • The percentages above are PRISM design weights, not empirical
//   hiring coefficients claimed by research.
// ============================================================


// ============================================================
// PUBLIC RESULT MODEL
// ============================================================

data class SmartAiInsight(
    val title: String,
    val message: String,
    val reason: String,
    val action: String,
    val priority: Int,
    val category: String
)


// ============================================================
// INTERNAL DATA
// ============================================================

private data class SmartAiSnapshot(
    val profile: DocumentSnapshot,
    val currentCgpa: Float?,
    val targetCgpa: Float?,
    val academicEvents: List<DocumentSnapshot>,
    val dsaProblems: List<DocumentSnapshot>,
    val projects: List<DocumentSnapshot>,
    val internships: List<DocumentSnapshot>,
    val skills: List<DocumentSnapshot>,
    val achievements: List<DocumentSnapshot>,
    val certifications: List<DocumentSnapshot>,
    val learning: List<DocumentSnapshot>,
    val breakdown: PrismScoring.Breakdown
) {
    val existingGrowthScore: Int get() = breakdown.readiness
}

private data class FactorScore(
    val key: String,
    val label: String,
    val baseWeight: Float,
    val weight: Float,
    val score: Float?,
    val observed: Boolean,
    val trend: Float,
    val reason: String
)

private data class Candidate(
    val label: String,
    val weightedWeakness: Float,
    val priority: Int,
    val title: String,
    val message: String,
    val reason: String,
    val action: String
)

private data class AcademicEventCandidate(
    val events: List<DocumentSnapshot>,
    val daysUntil: Int
)


// ============================================================
// PUBLIC LOADER
// ============================================================

fun loadSmartAiInsight(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (SmartAiInsight) -> Unit,
    onError: (Exception) -> Unit
) {
    val userRef = firestore
        .collection("users")
        .document(uid)

    val tasks = listOf(
        userRef.get(),
        userRef.collection("academicEvents").get(),
        userRef.collection("dsaProblems").get(),
        userRef.collection("projects").get(),
        userRef.collection("internships").get(),
        userRef.collection("placementSkills").get(),
        userRef.collection("placementAchievements").get(),
        userRef.collection("placementCertifications").get(),
        userRef.collection("placementLearning").get()
    )

    Tasks.whenAllSuccess<Any>(tasks)
        .addOnSuccessListener { results ->

            try {
                val profile =
                    results[0] as DocumentSnapshot

                val academicEvents =
                    (results[1] as QuerySnapshot).documents

                val dsaProblems =
                    (results[2] as QuerySnapshot).documents

                val projects =
                    (results[3] as QuerySnapshot).documents

                val internships =
                    (results[4] as QuerySnapshot).documents

                val skills =
                    (results[5] as QuerySnapshot).documents

                val achievements =
                    (results[6] as QuerySnapshot).documents

                val certifications =
                    (results[7] as QuerySnapshot).documents

                val learning =
                    (results[8] as QuerySnapshot).documents

                val currentCgpa =
                    parseFloatOrNull(
                        profile.get("currentCgpa")
                    )

                val targetCgpa =
                    parseFloatOrNull(
                        profile.get("targetCgpa")
                    )

                val breakdown =
                    PrismScoreLoader.buildSummary(
                        listOf(results[0]) + results.drop(2)
                    ).breakdown

                val snapshot =
                    SmartAiSnapshot(
                        profile = profile,
                        currentCgpa = currentCgpa,
                        targetCgpa = targetCgpa,
                        academicEvents = academicEvents,
                        dsaProblems = dsaProblems,
                        projects = projects,
                        internships = internships,
                        skills = skills,
                        achievements = achievements,
                        certifications = certifications,
                        learning = learning,
                        breakdown = breakdown
                    )

                onSuccess(
                    generateSmartAiInsight(snapshot)
                )

            } catch (exception: Exception) {
                onError(exception)
            }
        }
        .addOnFailureListener { exception ->
            onError(exception)
        }
}


// ============================================================
// MAIN DECISION ENGINE
// ============================================================

private fun generateSmartAiInsight(
    data: SmartAiSnapshot
): SmartAiInsight {

    val factors =
        applyRoleAwareWeights(
            baseFactors = listOf(
                technicalSkillsFactor(data),
                projectsFactor(data),
                dsaFactor(data),
                internshipFactor(data),
                academicsFactor(data),
                profileEvidenceFactor(data),
                growthFactor(data)
            ),
            targetRole = readTargetRole(data.profile)
        )

    val observedFactors =
        factors.filter {
            it.observed && it.score != null
        }

    // --------------------------------------------------------
    // A near-term academic event is contextual urgency.
    // It does NOT change the 15% academic model weight.
    // --------------------------------------------------------

    val urgentAcademicEvent =
        findUrgentAcademicEvents(
            data.academicEvents
        )

    if (urgentAcademicEvent != null) {

        val events =
            urgentAcademicEvent.events

        val days =
            urgentAcademicEvent.daysUntil

        val displayTime =
            when {
                days == 0 -> "today"
                days == 1 -> "tomorrow"
                else -> "in $days days"
            }

        val eventNames =
            events.mapNotNull { event ->
                val title =
                    event.getString("title")
                        ?.trim()
                        .orEmpty()
                        .ifBlank {
                            event.getString("subject")
                                ?.trim()
                                .orEmpty()
                                .ifBlank {
                                    null
                                }
                        }

                title
            }.distinct()

        val eventCount =
            eventNames.size

        val title =
            when {
                eventCount == 1 ->
                    "Prepare for ${eventNames.first()}."

                eventCount > 1 ->
                    "Prepare for $eventCount academic events."

                else ->
                    "Prepare for your upcoming academic events."
            }

        val message =
            when {
                eventNames.isEmpty() ->
                    "You have upcoming academic events $displayTime."

                eventNames.size == 1 ->
                    "${eventNames.first()} is your next important academic event. It is $displayTime."

                else ->
                    "These academic events are coming up $displayTime: ${eventNames.joinToString(" • ")}."
            }

        // Deliberately capped below 95 so the old arbitrary
        // "95/100" academic rule can never reappear.
        val priority =
            when {
                days <= 2 -> 88
                days <= 7 -> 84
                else -> 78
            }

        return SmartAiInsight(
            title = title,
            message = message,
            reason = if (eventCount > 1) {
                "Multiple near-term academic deadlines fall on the same date, so SMART AI groups them into one action instead of hiding all but one."
            } else {
                "A near-term academic deadline can temporarily become the most useful next action."
            },
            action = if (eventCount > 1) {
                "Prioritize preparation for these upcoming events before spending extra time on lower-urgency work."
            } else {
                "Prioritize preparation for this event before spending extra time on lower-urgency work."
            },
            priority = priority,
            category = "ACADEMICS"
        )
    }

    // --------------------------------------------------------
    // If too little information is available, do not invent
    // a performance score.
    // --------------------------------------------------------

    if (observedFactors.size < 4) {

        val missing =
            factors
                .filter { !it.observed }
                .joinToString(", ") {
                    it.label
                }

        return SmartAiInsight(
            title = "Build a clearer profile.",
            message = "SMART AI currently has limited recorded evidence for a strong career recommendation.",
            reason = "Only ${observedFactors.size} of ${factors.size} decision factors currently have usable data.",
            action = if (missing.isBlank()) {
                "Keep PRISM updated whenever your progress changes."
            } else {
                "Keep PRISM updated. Currently missing: $missing."
            },
            priority = 55,
            category = "DATA"
        )
    }

    // --------------------------------------------------------
    // Weighted profile score.
    // Missing factors are excluded and the remaining weights
    // are automatically normalized.
    // --------------------------------------------------------

    // Same number as the Growth, Placement and Home screens.
    val overallScore = data.existingGrowthScore

    // --------------------------------------------------------
    // Find the largest weighted weakness.
    //
    // weakness = factor weight × (100 - factor score)
    //
    // This is fundamentally different from:
    // "CGPA gap >= 1.0 -> priority 95"
    // --------------------------------------------------------

    val totalWeightedWeakness =
        observedFactors.sumOf { factor ->

            (
                    factor.weight *
                            (100f - ((factor.score ?: 100f) + factor.trend).coerceIn(0f, 100f))
                    ).toDouble()
        }.toFloat()

    val candidates =
        observedFactors.mapNotNull { factor ->

            val score =
                factor.score
                    ?: return@mapNotNull null

            val adjustedScore =
                (score + factor.trend).coerceIn(0f, 100f)

            val weightedWeakness =
                factor.weight *
                        (100f - adjustedScore)

            if (weightedWeakness <= 0f) {
                null
            } else {

                val weaknessShare =
                    if (totalWeightedWeakness > 0f) {
                        weightedWeakness /
                                totalWeightedWeakness
                    } else {
                        0f
                    }

                buildCandidate(
                    factor = factor,
                    score = score,
                    weaknessShare = weaknessShare,
                    weightedWeakness = weightedWeakness,
                    trend = factor.trend
                )
            }
        }

    val bestCandidate =
        candidates.maxByOrNull {
            it.weightedWeakness
        }

    if (bestCandidate == null) {

        return SmartAiInsight(
            title = "Keep your momentum.",
            message = "Your recorded profile does not currently show a major weakness.",
            reason = "SMART AI found no meaningful weighted gap across the available data.",
            action = "Keep improving consistently and update PRISM whenever your progress changes.",
            priority = 30,
            category = "BALANCED"
        )
    }

    val confidence = recommendationConfidence(observedFactors, bestCandidate)

    return SmartAiInsight(
        title = bestCandidate.title,
        message =
            "${bestCandidate.message} Overall profile score: $overallScore/100.",
        reason = "${bestCandidate.reason} Recommendation confidence: $confidence%.",
        action = bestCandidate.action,
        priority = bestCandidate.priority,
        category = bestCandidate.label.uppercase(Locale.ROOT)
    )
}


// ============================================================
// v0.3 ROLE-AWARE WEIGHTS
// ============================================================
// Base distribution remains exactly 20/20/18/15/12/10/5.
// If an existing profile role field is available, small multipliers
// reflect role demand signals and are normalized back to 100%.
// If no role is available, weights remain exactly the v0.2 baseline.
// ============================================================

private fun applyRoleAwareWeights(
    baseFactors: List<FactorScore>,
    targetRole: String?
): List<FactorScore> {

    val role = normalize(targetRole)
    if (role.isBlank()) return baseFactors

    val multipliers = when {
        containsAny(role, "AI", "ML", "MACHINE LEARNING", "DATA SCIENCE") ->
            mapOf("SKILLS" to 1.10f, "PROJECTS" to 1.10f, "DSA" to 0.90f, "INTERNSHIPS" to 1.00f, "ACADEMICS" to 0.95f, "EVIDENCE" to 1.00f, "GROWTH" to 0.95f)
        containsAny(role, "BACKEND", "BACK-END", "SERVER") ->
            mapOf("SKILLS" to 1.08f, "PROJECTS" to 1.08f, "DSA" to 1.02f, "INTERNSHIPS" to 1.00f, "ACADEMICS" to 0.92f, "EVIDENCE" to 0.95f, "GROWTH" to 0.95f)
        containsAny(role, "FRONTEND", "FRONT-END", "WEB", "UI") ->
            mapOf("SKILLS" to 1.10f, "PROJECTS" to 1.12f, "DSA" to 0.90f, "INTERNSHIPS" to 1.00f, "ACADEMICS" to 0.92f, "EVIDENCE" to 0.96f, "GROWTH" to 0.95f)
        containsAny(role, "DEVOPS", "SRE", "CLOUD", "PLATFORM") ->
            mapOf("SKILLS" to 1.12f, "PROJECTS" to 1.10f, "DSA" to 0.92f, "INTERNSHIPS" to 1.00f, "ACADEMICS" to 0.90f, "EVIDENCE" to 0.96f, "GROWTH" to 0.96f)
        containsAny(role, "CYBER", "SECURITY", "INFOSEC") ->
            mapOf("SKILLS" to 1.12f, "PROJECTS" to 1.10f, "DSA" to 0.92f, "INTERNSHIPS" to 1.02f, "ACADEMICS" to 0.90f, "EVIDENCE" to 1.00f, "GROWTH" to 0.94f)
        else -> emptyMap()
    }

    if (multipliers.isEmpty()) return baseFactors

    val adjusted = baseFactors.map { factor ->
        factor.copy(weight = factor.baseWeight * (multipliers[factor.key] ?: 1f))
    }

    val total = adjusted.sumOf { it.weight.toDouble() }.toFloat()
    if (total <= 0f) return baseFactors

    return adjusted.map { it.copy(weight = it.weight / total) }
}

private fun readTargetRole(profile: DocumentSnapshot): String? {
    listOf("targetRole", "careerGoal", "desiredRole", "targetCareer", "role").forEach { field ->
        val value = profile.getString(field)?.trim().orEmpty()
        if (value.isNotBlank()) return value
    }
    return null
}

private fun containsAny(value: String, vararg terms: String): Boolean =
    terms.any { value.contains(normalize(it), ignoreCase = true) }

private fun timestampTrend(documents: List<DocumentSnapshot>): Float {
    val dates = documents.mapNotNull { extractTimestampDate(it) }
    if (dates.size < 2) return 0f
    val newest = dates.maxOrNull() ?: return 0f
    val oldest = dates.minOrNull() ?: return 0f
    val spanDays = (newest.time - oldest.time) / (24L * 60L * 60L * 1000L)
    if (spanDays < 7L) return 0f
    val cutoff = newest.time - (spanDays / 2L) * 24L * 60L * 60L * 1000L
    val recent = dates.count { it.time >= cutoff }
    val older = dates.size - recent
    if (older <= 0) return 0f
    val ratio = recent.toFloat() / older.toFloat()
    return when {
        ratio >= 2.0f -> 8f
        ratio >= 1.5f -> 5f
        ratio <= 0.5f -> -8f
        ratio <= 0.75f -> -5f
        else -> 0f
    }
}

private fun extractTimestampDate(document: DocumentSnapshot): java.util.Date? {
    val values = listOf(document.get("updatedAt"), document.get("createdAt"), document.get("timestamp"))
    for (value in values) {
        when (value) {
            is com.google.firebase.Timestamp -> return value.toDate()
            is java.util.Date -> return value
            is Number -> return java.util.Date(value.toLong())
            is String -> parseDateTime(value)?.let { return it }
        }
    }
    return null
}

private fun parseDateTime(value: String): java.util.Date? {
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
    for (pattern in patterns) {
        try {
            return SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: Exception) { }
    }
    return null
}

// ============================================================
// FACTORS
// ============================================================
// Every score comes from PrismScoring (via PrismScoreLoader), so
// Smart AI can never show a different number from another screen.
// ============================================================

private fun technicalSkillsFactor(
    data: SmartAiSnapshot
): FactorScore {

    val observed =
        data.skills.any {
            parseFloatOrNull(it.get("rating"))?.let { value -> value in 0f..10f } == true
        }

    return FactorScore(
        key = "SKILLS",
        label = "Technical Skills",
        baseWeight = PrismScoring.WEIGHT_SKILLS.toFloat(),
        weight = PrismScoring.WEIGHT_SKILLS.toFloat(),
        score = if (observed) data.breakdown.skills.toFloat() else null,
        observed = observed,
        trend = if (observed) timestampTrend(data.skills) else 0f,
        reason = if (observed) {
            "Based on the average of the recorded placement skill ratings."
        } else {
            "No placement skill ratings are currently recorded."
        }
    )
}

private fun projectsFactor(
    data: SmartAiSnapshot
): FactorScore {

    val verifiedCount =
        data.projects.count { project ->
            PrismScoreLoader.projectInput(project).githubStatus == PrismScoring.GithubStatus.VERIFIED
        }

    return FactorScore(
        key = "PROJECTS",
        label = "Projects",
        baseWeight = PrismScoring.WEIGHT_PROJECTS.toFloat(),
        weight = PrismScoring.WEIGHT_PROJECTS.toFloat(),
        score = data.breakdown.projects.toFloat(),
        observed = true,
        trend = if (data.projects.isEmpty()) 0f else timestampTrend(data.projects),
        reason = when {
            data.projects.isEmpty() -> "No projects are currently recorded in PRISM."
            verifiedCount > 0 -> "Based on project progress, verified links and project levels. $verifiedCount project(s) have a verified GitHub repository."
            else -> "Based on project progress, links and project levels. A verified GitHub link raises this score."
        }
    )
}

private fun dsaFactor(
    data: SmartAiSnapshot
): FactorScore {

    return FactorScore(
        key = "DSA",
        label = "DSA",
        baseWeight = PrismScoring.WEIGHT_DSA.toFloat(),
        weight = PrismScoring.WEIGHT_DSA.toFloat(),
        score = data.breakdown.dsa.toFloat(),
        observed = true,
        trend = if (data.dsaProblems.isEmpty()) 0f else timestampTrend(data.dsaProblems),
        reason = if (data.dsaProblems.isEmpty()) {
            "No solved DSA problems are currently recorded."
        } else {
            "Based on the recorded DSA scores, difficulty and topic weighting."
        }
    )
}

private fun internshipFactor(
    data: SmartAiSnapshot
): FactorScore {

    return FactorScore(
        key = "INTERNSHIPS",
        label = "Internships",
        baseWeight = PrismScoring.WEIGHT_INTERNSHIPS.toFloat(),
        weight = PrismScoring.WEIGHT_INTERNSHIPS.toFloat(),
        score = data.breakdown.internships.toFloat(),
        observed = true,
        trend = if (data.internships.isEmpty()) 0f else timestampTrend(data.internships),
        reason = if (data.internships.isEmpty()) {
            "No internship records are currently recorded in PRISM."
        } else {
            "Based on recorded applications, interview-stage progress, selections and internship levels."
        }
    )
}

private fun academicsFactor(
    data: SmartAiSnapshot
): FactorScore {

    val cgpa =
        data.currentCgpa
            ?.takeIf {
                it in 0f..10f
            }

    return FactorScore(
        key = "ACADEMICS",
        label = "Academics",
        baseWeight = PrismScoring.WEIGHT_ACADEMICS.toFloat(),
        weight = PrismScoring.WEIGHT_ACADEMICS.toFloat(),
        score = if (cgpa == null) null else data.breakdown.academics.toFloat(),
        observed = cgpa != null,
        trend = 0f,
        reason = if (cgpa == null) {
            "Current CGPA is not recorded."
        } else {
            "Based on current CGPA. Target CGPA is contextual information, not a fixed priority score."
        }
    )
}

private fun profileEvidenceFactor(
    data: SmartAiSnapshot
): FactorScore {

    val empty =
        data.achievements.isEmpty() &&
                data.certifications.isEmpty() &&
                data.learning.isEmpty()

    return FactorScore(
        key = "EVIDENCE",
        label = "Achievements & Learning",
        baseWeight = PrismScoring.WEIGHT_EVIDENCE.toFloat(),
        weight = PrismScoring.WEIGHT_EVIDENCE.toFloat(),
        score = data.breakdown.evidence.toFloat(),
        observed = true,
        trend = if (empty) 0f else timestampTrend(data.achievements + data.certifications + data.learning),
        reason = if (empty) {
            "No achievements, certifications or learning records are currently recorded."
        } else {
            "Based on recorded achievements, certifications and additional learning."
        }
    )
}

private fun growthFactor(
    data: SmartAiSnapshot
): FactorScore {

    return FactorScore(
        key = "GROWTH",
        label = "Growth",
        baseWeight = 0f,
        weight = 0f,
        score = data.existingGrowthScore.toFloat(),
        observed = true,
        trend = 0f,
        reason = "Uses PRISM's Growth score. It is the same number as the Home and Placement screens."
    )
}


// ============================================================
// CANDIDATE GENERATION
// ============================================================

private fun buildCandidate(
    factor: FactorScore,
    score: Float,
    weaknessShare: Float,
    weightedWeakness: Float,
    trend: Float = 0f
): Candidate {

    val roundedScore =
        score.roundToInt()

    val priority =
        priorityFromWeakness(
            share = weaknessShare,
            score = score,
            trend = trend
        )

    return when (factor.key) {

        "SKILLS" ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = "Strengthen your technical skills.",
                message = "Your recorded technical-skill score is $roundedScore/100.",
                reason = "Technical skills carry 15% of your readiness score because role-relevant ability is central to skills-based hiring.",
                action = "Pick one important technical skill and improve it through focused practice plus a project."
            )

        "PROJECTS" ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = if (score <= 0f) {
                    "Build your first serious project."
                } else {
                    "Strengthen your project portfolio."
                },
                message = if (score <= 0f) {
                    "No project progress is currently recorded."
                } else {
                    "Your recorded project-progress score is $roundedScore/100."
                },
                reason = "Projects provide practical evidence of the ability to build and apply technical skills.",
                action = if (score <= 0f) {
                    "Build one meaningful project and take it from planning to completion."
                } else {
                    "Choose your highest-value unfinished project and push its implementation toward completion."
                }
            )

        "DSA" ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = "Strengthen your DSA.",
                message = "Your weighted DSA score is $roundedScore/100.",
                reason = "Problem-solving is an important technical signal, and DSA currently has a meaningful weighted gap.",
                action = if (score < 35f) {
                    "Build consistency with Easy problems first, then gradually increase Medium-level problems."
                } else {
                    "Keep solving consistently and increase coverage of Medium and advanced topics."
                }
            )

        "INTERNSHIPS" ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = "Strengthen your internship pipeline.",
                message = "Your recorded internship-development score is $roundedScore/100.",
                reason = "Real-world experience is an important practical signal, and your current recorded pipeline has a meaningful gap.",
                action = "Target relevant opportunities, keep applications tracked, and work toward interview-stage experience."
            )

        "ACADEMICS" ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = "Improve your academic performance.",
                message = "Your current academic score is $roundedScore/100.",
                reason = "Academics remain a useful supporting signal, but they are deliberately limited to 15% of the SMART AI model.",
                action = "Focus on upcoming academic work and the subjects where you can raise your semester performance."
            )

        "EVIDENCE" ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = "Build stronger supporting evidence.",
                message = "Your recorded achievements, certifications and learning score is $roundedScore/100.",
                reason = "These are supporting signals that strengthen the profile when backed by meaningful work.",
                action = "Keep PRISM updated with meaningful certifications, competitions and structured learning that you actually complete."
            )

        else ->
            Candidate(
                label = factor.label,
                weightedWeakness = weightedWeakness,
                priority = priority,
                title = "Keep improving consistently.",
                message = "Your recorded Growth score is $roundedScore/100.",
                reason = "Growth is a coaching signal with no extra weight in the SMART AI model.",
                action = "Maintain consistent progress across your weakest meaningful area."
            )
    }
}


// ============================================================
// RECOMMENDATION CONFIDENCE
// ============================================================

private fun recommendationConfidence(
    factors: List<FactorScore>,
    candidate: Candidate
): Int {
    val coverage = factors.size / 7f * 100f
    val totalWeakness = factors.sumOf {
        (it.weight * (100f - (it.score ?: 100f))).toDouble()
    }.toFloat()
    val share = if (totalWeakness > 0f) {
        candidate.weightedWeakness / totalWeakness
    } else 0f
    return (coverage * 0.45f + share * 100f * 0.55f)
        .roundToInt()
        .coerceIn(50, 95)
}

// ============================================================
// PRIORITY
// ============================================================
//
// Priority is deliberately NOT a factor weight.
//
// It combines:
// 1. How much of the total weighted weakness this factor represents.
// 2. How weak the factor itself is.
//
// The result is capped at 88 so the old arbitrary 95/100
// academic priority cannot return.
// ============================================================

private fun priorityFromWeakness(
    share: Float,
    score: Float,
    trend: Float = 0f
): Int {

    val shareComponent =
        share.coerceIn(
            0f,
            1f
        ) * 100f

    val weaknessComponent =
        (100f - score.coerceIn(0f, 100f))

    val trendUrgency = when {
        trend <= -10f -> 7f
        trend <= -5f -> 4f
        trend >= 10f -> -4f
        trend >= 5f -> -2f
        else -> 0f
    }

    return (
            shareComponent * 0.55f +
                    weaknessComponent * 0.35f +
                    trendUrgency
            )
        .roundToInt()
        .coerceIn(
            35,
            90
        )
}


// ============================================================
// ACADEMIC EVENT URGENCY
// ============================================================
//
// Uses the actual date field instead of trusting a stale stored
// "UPCOMING" value alone.
// ============================================================

private fun findUrgentAcademicEvents(
    events: List<DocumentSnapshot>
): AcademicEventCandidate? {

    val today =
        Calendar.getInstance().apply {
            set(
                Calendar.HOUR_OF_DAY,
                0
            )
            set(
                Calendar.MINUTE,
                0
            )
            set(
                Calendar.SECOND,
                0
            )
            set(
                Calendar.MILLISECOND,
                0
            )
        }

    val candidates =
        events.mapNotNull { event ->

            // Completed/cancelled events are hard exclusions.
            // SMART AI must never treat them as upcoming work.
            val status =
                normalize(
                    event.getString("status")
                )

            if (
                status == "COMPLETED" ||
                status == "CANCELLED"
            ) {
                return@mapNotNull null
            }

            val dateText =
                event.getString("date")
                    ?.trim()
                    .orEmpty()

            if (dateText.isBlank()) {
                return@mapNotNull null
            }

            val eventDate =
                parseDate(
                    dateText
                ) ?: return@mapNotNull null

            val eventCalendar =
                Calendar.getInstance().apply {
                    time = eventDate

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )
                    set(
                        Calendar.MINUTE,
                        0
                    )
                    set(
                        Calendar.SECOND,
                        0
                    )
                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            val diffMillis =
                eventCalendar.timeInMillis -
                        today.timeInMillis

            val days =
                (
                        diffMillis /
                                (24L * 60L * 60L * 1000L)
                        ).toInt()

            // Only genuinely upcoming events in the next 14 days.
            if (days in 0..14) {

                AcademicEventCandidate(
                    events = listOf(event),
                    daysUntil = days
                )

            } else {
                null
            }
        }

    if (candidates.isEmpty()) {
        return null
    }

    val earliestDays =
        candidates.minOf {
            it.daysUntil
        }

    val earliestEvents =
        candidates
            .filter {
                it.daysUntil == earliestDays
            }
            .flatMap {
                it.events
            }

    return AcademicEventCandidate(
        events = earliestEvents,
        daysUntil = earliestDays
    )
}

// ============================================================
// DATE / PARSING / STRING HELPERS
// ============================================================

private fun parseDate(
    value: String
): java.util.Date? {

    return try {

        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }.parse(
            value
        )

    } catch (_: Exception) {

        null
    }
}

private fun parseFloatOrNull(
    value: Any?
): Float? {

    return when (value) {

        is Number ->
            value.toFloat()

        is String ->
            value
                .trim()
                .toFloatOrNull()

        else ->
            null
    }
}

private fun normalize(
    value: String?
): String {

    return value
        ?.trim()
        ?.uppercase(
            Locale.ROOT
        )
        .orEmpty()
}