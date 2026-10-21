package com.prismorbit.app

import kotlin.math.roundToInt

// ============================================================
// PRISM SCORING - THE ONE PLACE FOR EVERY FORMULA
// ============================================================
//
// Home dashboard, Placement, Growth, Smart AI and the Internship
// screen all call this file. No screen keeps its own formula.
// This file is plain Kotlin (no Android classes) so it is easy
// to unit test.
//
// OVERALL READINESS (0-100)
//   academics 15% + DSA 25% + projects 20% + internships 15%
//   + skills 15% + achievements/evidence 10%
//
// ACADEMICS   = CGPA x 10
// SKILLS      = average rating (1-10) x 10
// DSA         = total points / 1000 x 100, capped at 100
//   points per problem = stored "score" if present, otherwise
//   difficulty (Easy 1, Medium 2, Hard 3) x topic multiplier
//   (see dsaTopicMultiplier)
// EVIDENCE    = achievements 40% + certifications 30% + learning 30%
//   counts use the smooth curve count / (count + 4)
//   learning = average level x 10, or the count curve if no levels
//
// PROJECT BASE SCORE (per project, 0-100)
//   completed 20 + tech stack filled 15 + GitHub 15 + live link 15
//   + tasks added 5 + progress 30 (progress% x 0.30)
//   GitHub / Live link parts count 100% if VERIFIED,
//   40% if saved but not verified / could not be checked,
//   0% if empty (and 0% for a repo that is not yours).
//
// INTERNSHIP BASE SCORE (all internships together, 0-100)
//   applications 25 (10 = full) + interview exposure 20 (5 = full)
//   + selected outcomes 20 (2 = full) + project foundation 20
//   (4 projects = full) + complete details 15 (company, role,
//   application date, job URL). Zero internships = 0.
//
// LEVELS (1-5)
//   Every project and internship has a level. The level factor
//   (L1 0.70, L2 0.85, L3 1.00, L4 1.25, L5 1.50) multiplies the
//   item's own contribution, then the result is clamped to 0-100.
//   Missing level = Level 3 (factor 1.00).
//   Internship level effect is gated by status: full for SELECTED,
//   only 30% for every other status.
// ============================================================

object PrismScoring {

    // --------------------------------------------------------
    // Readiness weights (fixed)
    // --------------------------------------------------------

    const val WEIGHT_ACADEMICS = 0.15
    const val WEIGHT_DSA = 0.25
    const val WEIGHT_PROJECTS = 0.20
    const val WEIGHT_INTERNSHIPS = 0.15
    const val WEIGHT_SKILLS = 0.15
    const val WEIGHT_EVIDENCE = 0.10

    const val DSA_TARGET_POINTS = 1000.0
    const val COUNT_CURVE_OFFSET = 4.0

    const val EVIDENCE_ACHIEVEMENTS_WEIGHT = 0.40
    const val EVIDENCE_CERTIFICATIONS_WEIGHT = 0.30
    const val EVIDENCE_LEARNING_WEIGHT = 0.30

    // --------------------------------------------------------
    // Project base score parts
    // --------------------------------------------------------

    const val PROJECT_PART_COMPLETED = 20.0
    const val PROJECT_PART_TECH = 15.0
    const val PROJECT_PART_GITHUB = 15.0
    const val PROJECT_PART_LIVE = 15.0
    const val PROJECT_PART_TASKS = 5.0
    const val PROJECT_PART_PROGRESS = 30.0

    const val LINK_CREDIT_VERIFIED = 1.0
    const val LINK_CREDIT_SAVED = 0.4
    const val LINK_CREDIT_NONE = 0.0

    // --------------------------------------------------------
    // Internship base score parts
    // --------------------------------------------------------

    const val INTERNSHIP_PART_APPLICATIONS = 25.0
    const val INTERNSHIP_PART_INTERVIEWS = 20.0
    const val INTERNSHIP_PART_OUTCOMES = 20.0
    const val INTERNSHIP_PART_PROJECTS = 20.0
    const val INTERNSHIP_PART_DETAILS = 15.0

    const val INTERNSHIP_FULL_APPLICATIONS = 10.0
    const val INTERNSHIP_FULL_INTERVIEWS = 5.0
    const val INTERNSHIP_FULL_OUTCOMES = 2.0
    const val INTERNSHIP_FULL_PROJECTS = 4.0

    /** Level effect for internships that are not SELECTED yet. */
    const val INTERNSHIP_UNSELECTED_GATE = 0.30

    // --------------------------------------------------------
    // Levels
    // --------------------------------------------------------

    const val DEFAULT_LEVEL = 3

    private val LEVEL_FACTORS = doubleArrayOf(0.70, 0.85, 1.00, 1.25, 1.50)

    val PROJECT_LEVEL_NAMES = listOf("Practice", "Basic", "Solid", "Advanced", "Professional")
    val INTERNSHIP_LEVEL_NAMES = listOf("Small", "Local", "Growing", "Strong", "Top")

    fun clampLevel(level: Int): Int = level.coerceIn(1, 5)

    fun levelFactor(level: Int): Double = LEVEL_FACTORS[clampLevel(level) - 1]

    /** 0-24 = L1, 25-44 = L2, 45-64 = L3, 65-84 = L4, 85-100 = L5. */
    fun levelFromScore(score: Double): Int {
        val s = score.coerceIn(0.0, 100.0).roundToInt()
        return when {
            s >= 85 -> 5
            s >= 65 -> 4
            s >= 45 -> 3
            s >= 25 -> 2
            else -> 1
        }
    }

    fun projectLevelName(level: Int): String = PROJECT_LEVEL_NAMES[clampLevel(level) - 1]

    fun internshipLevelName(level: Int): String = INTERNSHIP_LEVEL_NAMES[clampLevel(level) - 1]

    fun levelLabel(level: Int, name: String): String = "Level ${clampLevel(level)} - $name"

    /** How much of the level effect counts for an internship with this status. */
    fun internshipGate(status: String): Double =
        if (status.trim().uppercase() == "SELECTED") 1.0 else INTERNSHIP_UNSELECTED_GATE

    /** Level factor after the status gate: 1 + (factor - 1) x gate. */
    fun effectiveFactor(level: Int, gate: Double = 1.0): Double =
        1.0 + (levelFactor(level) - 1.0) * gate.coerceIn(0.0, 1.0)

    // --------------------------------------------------------
    // Readiness
    // --------------------------------------------------------

    fun readiness(
        academics: Int,
        dsa: Int,
        projects: Int,
        internships: Int,
        skills: Int,
        evidence: Int
    ): Int = (
            academics * WEIGHT_ACADEMICS +
                    dsa * WEIGHT_DSA +
                    projects * WEIGHT_PROJECTS +
                    internships * WEIGHT_INTERNSHIPS +
                    skills * WEIGHT_SKILLS +
                    evidence * WEIGHT_EVIDENCE
            ).coerceIn(0.0, 100.0).roundToInt()

    // --------------------------------------------------------
    // Academics / skills
    // --------------------------------------------------------

    fun academicsScore(cgpa: Double?): Int =
        ((cgpa ?: 0.0).coerceIn(0.0, 10.0) * 10.0).roundToInt()

    fun skillsScore(ratings: List<Double>): Int {
        val valid = ratings.filter { it in 0.0..10.0 }
        if (valid.isEmpty()) return 0
        return (valid.average() * 10.0).coerceIn(0.0, 100.0).roundToInt()
    }

    // --------------------------------------------------------
    // DSA
    // --------------------------------------------------------

    fun dsaTopicMultiplier(topic: String): Double = when (topic.trim()) {
        "Dynamic Programming" -> 1.4
        "Graphs", "Backtracking", "Tries" -> 1.3
        "Recursion", "Trees / BST", "Heap", "Greedy", "Bit Manipulation" -> 1.2
        "Searching & Sorting", "Linked List", "Stack & Queue", "Hashing" -> 1.1
        else -> 1.0
    }

    fun dsaDifficultyWeight(difficulty: String): Double = when (difficulty.trim()) {
        "Easy" -> 1.0
        "Medium" -> 2.0
        "Hard" -> 3.0
        else -> 1.0
    }

    fun dsaProblemPoints(difficulty: String, topic: String): Double =
        dsaDifficultyWeight(difficulty) * dsaTopicMultiplier(topic)

    /** The stored "score" field comes first; difficulty x topic is the fallback. */
    fun dsaPoints(storedScore: Double?, difficulty: String, topic: String): Double =
        storedScore ?: dsaProblemPoints(difficulty, topic)

    fun dsaScoreExact(totalPoints: Double): Double =
        (totalPoints / DSA_TARGET_POINTS * 100.0).coerceIn(0.0, 100.0)

    fun dsaScore(totalPoints: Double): Int = dsaScoreExact(totalPoints).roundToInt()

    // --------------------------------------------------------
    // Achievements / evidence
    // --------------------------------------------------------

    /** Smooth curve: count / (count + 4) x 100. */
    fun countCurve(count: Int): Double {
        if (count <= 0) return 0.0
        return (100.0 * count / (count + COUNT_CURVE_OFFSET)).coerceIn(0.0, 100.0)
    }

    /** Average learning level (1-10) x 10, or the count curve when no levels exist. */
    fun learningScore(levels: List<Double>, recordCount: Int): Int {
        if (recordCount <= 0) return 0
        if (levels.isNotEmpty()) {
            return (levels.average() * 10.0).coerceIn(0.0, 100.0).roundToInt()
        }
        return countCurve(recordCount).roundToInt()
    }

    fun evidenceScore(achievements: Int, certifications: Int, learningScore: Int): Int = (
            countCurve(achievements) * EVIDENCE_ACHIEVEMENTS_WEIGHT +
                    countCurve(certifications) * EVIDENCE_CERTIFICATIONS_WEIGHT +
                    learningScore * EVIDENCE_LEARNING_WEIGHT
            ).coerceIn(0.0, 100.0).roundToInt()

    // --------------------------------------------------------
    // Projects
    // --------------------------------------------------------

    enum class GithubStatus { NONE, UNCHECKED, VERIFIED, NOT_FOUND, NOT_YOURS, COULD_NOT_CHECK }

    enum class LiveStatus { NONE, UNCHECKED, WORKS, NOT_REACHABLE }

    data class ProjectScoreInput(
        val status: String = "IDEA",
        val progress: Int = 0,
        val techStackFilled: Boolean = false,
        val githubStatus: GithubStatus = GithubStatus.NONE,
        val liveStatus: LiveStatus = LiveStatus.NONE,
        val hasTasks: Boolean = false,
        val level: Int = DEFAULT_LEVEL
    )

    fun githubCredit(status: GithubStatus): Double = when (status) {
        GithubStatus.NONE -> LINK_CREDIT_NONE
        GithubStatus.VERIFIED -> LINK_CREDIT_VERIFIED
        GithubStatus.NOT_YOURS -> LINK_CREDIT_NONE
        else -> LINK_CREDIT_SAVED
    }

    fun liveCredit(status: LiveStatus): Double = when (status) {
        LiveStatus.NONE -> LINK_CREDIT_NONE
        LiveStatus.WORKS -> LINK_CREDIT_VERIFIED
        else -> LINK_CREDIT_SAVED
    }

    /** Project score before the level factor is applied (0-100). */
    fun projectBaseScore(p: ProjectScoreInput): Double {
        val completed = if (p.status.trim().uppercase() == "COMPLETED") PROJECT_PART_COMPLETED else 0.0
        val tech = if (p.techStackFilled) PROJECT_PART_TECH else 0.0
        val github = PROJECT_PART_GITHUB * githubCredit(p.githubStatus)
        val live = PROJECT_PART_LIVE * liveCredit(p.liveStatus)
        val tasks = if (p.hasTasks) PROJECT_PART_TASKS else 0.0
        val progress = p.progress.coerceIn(0, 100) * (PROJECT_PART_PROGRESS / 100.0)
        return (completed + tech + github + live + tasks + progress).coerceIn(0.0, 100.0)
    }

    /** One project's contribution: base x level factor, then clamped to 0-100. */
    fun projectContribution(p: ProjectScoreInput): Double =
        (projectBaseScore(p) * effectiveFactor(p.level)).coerceIn(0.0, 100.0)

    /** Portfolio score = average of every project's contribution. */
    fun projectsScore(projects: List<ProjectScoreInput>): Int {
        if (projects.isEmpty()) return 0
        return projects.map { projectContribution(it) }.average()
            .coerceIn(0.0, 100.0).roundToInt()
    }

    // --------------------------------------------------------
    // Internships
    // --------------------------------------------------------

    data class InternshipScoreInput(
        val status: String = "INTERESTED",
        val detailsComplete: Boolean = false,
        val level: Int = DEFAULT_LEVEL
    ) {
        private val s: String get() = status.trim().uppercase()
        val isSelected: Boolean get() = s == "SELECTED"
        val isInterviewOrSelected: Boolean get() = s == "INTERVIEW" || s == "SELECTED"

        /** Level factor for this item after the status gate. */
        val factor: Double get() = effectiveFactor(level, internshipGate(status))
    }

    fun internshipDetailsComplete(
        company: String,
        role: String,
        applicationDate: String,
        jobUrl: String
    ): Boolean = company.isNotBlank() && role.isNotBlank() &&
            applicationDate.isNotBlank() && jobUrl.isNotBlank()

    /** Internship score (0-100). Zero internships = 0, whatever the projects are. */
    fun internshipsScore(items: List<InternshipScoreInput>, projectCount: Int): Int {
        if (items.isEmpty()) return 0

        val applications = (items.sumOf { it.factor } / INTERNSHIP_FULL_APPLICATIONS)
            .coerceIn(0.0, 1.0) * INTERNSHIP_PART_APPLICATIONS

        val interviews = (items.filter { it.isInterviewOrSelected }.sumOf { it.factor } /
                INTERNSHIP_FULL_INTERVIEWS).coerceIn(0.0, 1.0) * INTERNSHIP_PART_INTERVIEWS

        val outcomes = (items.filter { it.isSelected }.sumOf { it.factor } /
                INTERNSHIP_FULL_OUTCOMES).coerceIn(0.0, 1.0) * INTERNSHIP_PART_OUTCOMES

        val projectFoundation = (projectCount / INTERNSHIP_FULL_PROJECTS)
            .coerceIn(0.0, 1.0) * INTERNSHIP_PART_PROJECTS

        val details = (items.filter { it.detailsComplete }.sumOf { it.factor } / items.size)
            .coerceIn(0.0, 1.0) * INTERNSHIP_PART_DETAILS

        return (applications + interviews + outcomes + projectFoundation + details)
            .coerceIn(0.0, 100.0).roundToInt()
    }

    // --------------------------------------------------------
    // Everything together
    // --------------------------------------------------------

    data class Breakdown(
        val academics: Int = 0,
        val dsa: Int = 0,
        val projects: Int = 0,
        val internships: Int = 0,
        val skills: Int = 0,
        val evidence: Int = 0
    ) {
        val readiness: Int
            get() = readiness(academics, dsa, projects, internships, skills, evidence)
    }
}
