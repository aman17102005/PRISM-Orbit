package com.prismorbit.app

// ============================================================
// INTERNSHIP LEVEL ENGINE (rules only, no AI, no Android)
// ============================================================
// Level score (0-100) = sum of parameter (0-10) / 10 x weight.
// 0-24 = L1 Small, 25-44 = L2 Local, 45-64 = L3 Growing,
// 65-84 = L4 Strong, 85-100 = L5 Top.
// The status gate (full effect only for SELECTED) is applied in
// PrismScoring, not here.
// ============================================================

/** Result of reading a proof photo or PDF on the phone. */
data class ProofSummary(
    /** "OK", "PARTIAL" or "UNREADABLE" */
    val status: String = "",
    val companyMatch: Boolean = false,
    val nameMatch: Boolean = false,
    val keywordHits: List<String> = emptyList(),
    val datesFound: Int = 0
) {
    val isPresent: Boolean get() = status.isNotBlank()
}

object WorkMode {
    const val ONSITE = "ONSITE"
    const val HYBRID = "HYBRID"
    const val REMOTE = "REMOTE"

    val ALL = listOf(ONSITE to "On-site", HYBRID to "Hybrid", REMOTE to "Remote")

    fun share(mode: String): Double = when (mode.trim().uppercase()) {
        ONSITE -> 1.0
        HYBRID -> 0.70
        REMOTE -> 0.50
        else -> 0.0
    }
}

data class InternshipLevelInput(
    val company: String = "",
    val companyType: String = "",
    val role: String = "",
    val duration: String = "",
    val technicalSkillsUsed: String = "",
    val responsibilities: String = "",
    val problemSolved: String = "",
    val outcomeImpact: String = "",
    val interviewDate: String = "",
    val offerDate: String = "",
    val stipend: String = "",
    val status: String = "INTERESTED",
    val workMode: String = "",
    val proof: ProofSummary = ProofSummary(),
    val hasProofLink: Boolean = false
)

object InternshipLevelEngine {

    const val COMPANY_STRENGTH = "company_strength"
    const val ROLE_WORK_DEPTH = "role_work_depth"
    const val RESULT_IMPACT = "result_impact"
    const val DURATION = "duration"
    const val PROOF = "proof"
    const val SELECTION_DIFFICULTY = "selection_difficulty"
    const val PAID_UNPAID = "paid_unpaid"
    const val REMOTE_ONSITE = "remote_onsite"

    val PARAMETERS = listOf(
        LevelParameter(COMPANY_STRENGTH, "Company", 30, "pick the right company type, or add a well-known company"),
        LevelParameter(ROLE_WORK_DEPTH, "Work depth", 20, "describe the work you did and the tools you used"),
        LevelParameter(RESULT_IMPACT, "Result", 15, "write what changed because of your work, with numbers"),
        LevelParameter(DURATION, "Duration", 10, "add how long the internship was"),
        LevelParameter(PROOF, "Proof", 10, "add a photo or PDF of your offer letter or certificate"),
        LevelParameter(SELECTION_DIFFICULTY, "Selection steps", 5, "add the interview and offer dates"),
        LevelParameter(PAID_UNPAID, "Paid", 5, "add the stipend if it was paid"),
        LevelParameter(REMOTE_ONSITE, "Work place", 5, "pick on-site, hybrid or remote")
    )

    /** The AI may only score these two for internships. */
    val AI_PARAMETERS = setOf(ROLE_WORK_DEPTH, RESULT_IMPACT)

    private val TECH_WORDS = listOf(
        "kotlin", "java", "python", "javascript", "typescript", "react", "node", "android", "ios", "flutter",
        "firebase", "sql", "mongodb", "api", "rest", "docker", "kubernetes", "aws", "azure", "gcp", "git",
        "machine learning", "ml", "ai ", "tensorflow", "pytorch", "pandas", "spring", "django", "flask",
        "backend", "frontend", "database", "testing", "unit test", "ci/cd", "cloud", "deploy", "c++",
        "html", "css", "figma", "data", "model", "algorithm", "microservice", "compose", "swift"
    )

    private val RESULT_WORDS = listOf(
        "improved", "reduced", "increased", "launched", "shipped", "delivered", "saved", "built",
        "fixed", "automated", "deployed", "released", "faster", "boosted", "grew", "achieved",
        "completed", "optimised", "optimized", "users", "customers"
    )

    // --------------------------------------------------------
    // Public API
    // --------------------------------------------------------

    fun compute(
        input: InternshipLevelInput,
        aiScores: Map<String, Double>? = null,
        aiSummary: String? = null
    ): LevelResult {
        val rules = rulesParameters(input)
        val merged = mergeAi(rules, aiScores)
        val score = LevelMath.weightedScore(merged, PARAMETERS)
        val level = PrismScoring.levelFromScore(score)
        val usedAi = aiScores != null && merged != rules

        val reason = aiSummary?.let { LevelMath.cleanText(it, 120) }?.takeIf { it.isNotBlank() && usedAi }
            ?: LevelMath.oneLineReason(merged, PARAMETERS, level)

        return LevelResult(
            params = merged,
            score = score.toInt(),
            level = level,
            name = PrismScoring.internshipLevelName(level),
            reason = reason,
            source = if (usedAi) "ai+rules" else "rules",
            tips = LevelMath.lowestTwo(merged, PARAMETERS, level)
        )
    }

    fun mergeAi(rules: Map<String, Double>, ai: Map<String, Double>?): Map<String, Double> {
        if (ai.isNullOrEmpty()) return rules
        val result = rules.toMutableMap()
        for ((key, raw) in ai) {
            if (key in AI_PARAMETERS) result[key] = raw.coerceIn(0.0, 10.0)
        }
        return result
    }

    fun rulesParameters(input: InternshipLevelInput): Map<String, Double> = mapOf(
        COMPANY_STRENGTH to companyStrength(input),
        ROLE_WORK_DEPTH to roleWorkDepth(input),
        RESULT_IMPACT to resultImpact(input),
        DURATION to durationScore(input.duration),
        PROOF to proofScore(input),
        SELECTION_DIFFICULTY to selectionDifficulty(input),
        PAID_UNPAID to paidScore(input.stipend),
        REMOTE_ONSITE to WorkMode.share(input.workMode) * 10.0
    )

    // --------------------------------------------------------
    // Parameters (each returns 0-10)
    // --------------------------------------------------------

    fun companyStrength(input: InternshipLevelInput): Double {
        val chip = CompanyMatcher.CompanyType.fromName(input.companyType)
        return CompanyMatcher.companyStrengthShare(input.company, chip) * 10.0
    }

    fun roleWorkDepth(input: InternshipLevelInput): Double {
        val text = listOf(input.responsibilities, input.problemSolved, input.technicalSkillsUsed)
            .joinToString(" ")
        if (text.isBlank()) return 0.0
        val lengthShare = (text.trim().length / 400.0).coerceIn(0.0, 1.0)
        val techHits = LevelMath.containsAnyTerm(" $text ", TECH_WORDS)
        val techShare = (techHits / 5.0).coerceIn(0.0, 1.0)
        return ((lengthShare * 0.5 + techShare * 0.5) * 10.0).coerceIn(0.0, 10.0)
    }

    fun resultImpact(input: InternshipLevelInput): Double {
        val text = input.outcomeImpact.trim()
        if (text.isBlank()) return 0.0
        val lengthShare = (text.length / 150.0).coerceIn(0.0, 1.0)
        val numberShare = if (Regex("\\d").containsMatchIn(text)) 1.0 else 0.0
        val resultShare = (LevelMath.containsAnyTerm(text, RESULT_WORDS) / 2.0).coerceIn(0.0, 1.0)
        return ((lengthShare * 0.4 + numberShare * 0.3 + resultShare * 0.3) * 10.0).coerceIn(0.0, 10.0)
    }

    fun durationScore(duration: String): Double {
        val days = parseDurationDays(duration) ?: return 0.0
        val share = when {
            days < 14 -> 0.10
            days < 30 -> 0.40
            days < 84 -> 0.70
            else -> 1.00
        }
        return share * 10.0
    }

    fun proofScore(input: InternshipLevelInput): Double {
        val share = when (input.proof.status.uppercase()) {
            "OK" -> 1.0
            "PARTIAL" -> 0.6
            else -> if (input.hasProofLink) 0.5 else 0.0
        }
        return share * 10.0
    }

    fun selectionDifficulty(input: InternshipLevelInput): Double {
        var share = 0.0
        if (input.interviewDate.isNotBlank()) share += 0.35
        if (input.offerDate.isNotBlank()) share += 0.35
        share += when (input.status.trim().uppercase()) {
            "SELECTED" -> 0.30
            "INTERVIEW" -> 0.15
            "ASSESSMENT" -> 0.10
            else -> 0.0
        }
        return (share * 10.0).coerceIn(0.0, 10.0)
    }

    /** Blank, 0, "unpaid", "none" = 0. Anything else = full. Money amounts are never judged or shown. */
    fun paidScore(stipend: String): Double {
        val t = stipend.trim().lowercase()
        if (t.isEmpty()) return 0.0
        if (t in setOf("no", "none", "nil", "na", "n/a", "-")) return 0.0
        if (t.contains("unpaid") || t.contains("no stipend") || t.contains("not paid")) return 0.0
        val digits = t.filter { it.isDigit() }
        if (digits.isNotEmpty() && digits.all { it == '0' }) return 0.0
        return 10.0
    }

    // --------------------------------------------------------
    // Duration text -> days
    // --------------------------------------------------------

    /**
     * Understands "3 months", "6 mo", "8 weeks", "1.5 months", "2-3 months",
     * "45 days". For a range it takes the average. Returns null if unreadable.
     */
    fun parseDurationDays(text: String): Double? {
        val t = text.lowercase().trim()
        if (t.isEmpty()) return null

        val match = Regex(
            "(\\d+(?:\\.\\d+)?)(?:\\s*(?:-|to)\\s*(\\d+(?:\\.\\d+)?))?\\s*(months?|mos?|weeks?|wks?|w|days?|d|years?|yrs?)\\b"
        ).find(t) ?: return null

        val first = match.groupValues[1].toDoubleOrNull() ?: return null
        val second = match.groupValues[2].toDoubleOrNull()
        val amount = if (second != null) (first + second) / 2.0 else first

        val unit = match.groupValues[3]
        val perUnit = when {
            unit.startsWith("mo") -> 30.0
            unit.startsWith("w") -> 7.0
            unit.startsWith("d") -> 1.0
            unit.startsWith("y") -> 365.0
            else -> return null
        }
        return amount * perUnit
    }
}
