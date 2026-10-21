package com.prismorbit.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ============================================================
// PROJECT LEVEL ENGINE (rules only, no AI, no Android)
// ============================================================
// Level score (0-100) = sum of parameter (0-10) / 10 x weight.
// 0-24 = L1 Practice, 25-44 = L2 Basic, 45-64 = L3 Solid,
// 65-84 = L4 Advanced, 85-100 = L5 Professional.
// ============================================================

/** Features the app can find in a repository or the student can tap. */
object ProjectFeatures {
    const val AUTH = "auth"
    const val DATABASE = "database"
    const val BACKEND_API = "backend_api"
    const val AI_ML = "ai_ml"
    const val PAYMENTS = "payments"
    const val REALTIME = "realtime"
    const val MOBILE_APP = "mobile_app"
    const val DEPLOYMENT = "deployment"
    const val TESTS = "tests"

    val ALL = listOf(AUTH, DATABASE, BACKEND_API, AI_ML, PAYMENTS, REALTIME, MOBILE_APP, DEPLOYMENT, TESTS)

    /** The tap-only chips shown on the project screen: key to label. */
    val CHIPS = listOf(
        AUTH to "Login",
        DATABASE to "Database",
        BACKEND_API to "Backend/API",
        AI_ML to "AI/ML",
        PAYMENTS to "Payments",
        REALTIME to "Real-time",
        DEPLOYMENT to "Deployed",
        TESTS to "Has tests"
    )

    fun label(key: String): String = CHIPS.firstOrNull { it.first == key }?.second
        ?: when (key) {
            MOBILE_APP -> "Mobile app"
            else -> key
        }
}

/** How many real people use the project. */
object RealUsersTier {
    const val NONE = "none"
    const val UNDER_10 = "under_10"
    const val FROM_10 = "10_100"
    const val FROM_100 = "100_1000"
    const val FROM_1000 = "1000_plus"

    val ALL = listOf(
        NONE to "No users yet",
        UNDER_10 to "Under 10",
        FROM_10 to "10 - 100",
        FROM_100 to "100 - 1000",
        FROM_1000 to "1000+"
    )

    fun share(tier: String): Double = when (tier) {
        UNDER_10 -> 0.25
        FROM_10 -> 0.50
        FROM_100 -> 0.75
        FROM_1000 -> 1.00
        else -> 0.0
    }

    fun label(tier: String): String = ALL.firstOrNull { it.first == tier }?.second ?: "No users yet"
}

/** GitHub proof, read from the saved githubEvidence map (old and new format). */
data class GithubEvidenceInput(
    val isValidUrl: Boolean = false,
    val isPublic: Boolean = false,
    val isAccessible: Boolean = false,
    val hasReadme: Boolean = false,
    val readmeMeaningful: Boolean = false,
    val meaningfulCode: Boolean = false,
    val sourceSamples: Int = 0,
    val sourceFiles: Int = 0,
    val recentCommits: Int = 0,
    val languageCount: Int = 0,
    val sourceCharacters: Int = 0,
    val detectedFeatures: Set<String> = emptySet(),
    /** true = owner matches profile, false = different owner, null = profile URL missing. */
    val ownerMatches: Boolean? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>?): GithubEvidenceInput? {
            if (map == null || map.isEmpty()) return null

            fun bool(key: String) = map[key] as? Boolean ?: false
            fun int(key: String) = (map[key] as? Number)?.toInt() ?: 0

            val owner = when (map["ownerMatch"]?.toString()?.lowercase()) {
                "yes" -> true
                "no" -> false
                else -> null
            }
            val features = (map["detectedFeatures"] as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?.filter { it in ProjectFeatures.ALL }
                ?.toSet()
                ?: emptySet()

            return GithubEvidenceInput(
                isValidUrl = bool("isValidUrl"),
                isPublic = bool("isPublic"),
                isAccessible = bool("isAccessible"),
                hasReadme = bool("hasReadme"),
                readmeMeaningful = bool("readmeHasMeaningfulContent"),
                meaningfulCode = bool("sourceContentHasMeaningfulCode"),
                sourceSamples = int("sourceContentSampleCount"),
                sourceFiles = int("sourceFileCount"),
                recentCommits = int("recentCommitSampleSize"),
                languageCount = int("languageCount"),
                sourceCharacters = int("sourceContentCharacters"),
                detectedFeatures = features,
                ownerMatches = owner
            )
        }

        /** Reads the check result saved with the evidence (or works it out for old data). */
        fun githubStatus(url: String, map: Map<String, Any?>?): PrismScoring.GithubStatus {
            if (url.isBlank()) return PrismScoring.GithubStatus.NONE
            if (map == null || map.isEmpty()) return PrismScoring.GithubStatus.UNCHECKED

            when (map["checkStatus"]?.toString()?.uppercase()) {
                "VERIFIED" -> return PrismScoring.GithubStatus.VERIFIED
                "NOT_FOUND" -> return PrismScoring.GithubStatus.NOT_FOUND
                "NOT_YOURS" -> return PrismScoring.GithubStatus.NOT_YOURS
                "COULD_NOT_CHECK" -> return PrismScoring.GithubStatus.COULD_NOT_CHECK
            }

            // Old evidence from the earlier portfolio screen has no checkStatus.
            val accessible = map["isAccessible"] as? Boolean ?: false
            if (accessible) return PrismScoring.GithubStatus.VERIFIED
            val error = map["errorMessage"]?.toString().orEmpty()
            return if (error.contains("404") || error.contains("Invalid", ignoreCase = true)) {
                PrismScoring.GithubStatus.NOT_FOUND
            } else {
                PrismScoring.GithubStatus.COULD_NOT_CHECK
            }
        }
    }
}

data class ProjectLevelInput(
    val name: String = "",
    val description: String = "",
    val techStack: String = "",
    val startDate: String = "",
    val status: String = "IDEA",
    val progress: Int = 0,
    val tasksTotal: Int = 0,
    val tasksDone: Int = 0,
    val liveStatus: PrismScoring.LiveStatus = PrismScoring.LiveStatus.NONE,
    val isStoreLink: Boolean = false,
    val realUsers: String = RealUsersTier.NONE,
    val teamSize: Int = 0,
    val featureChips: Set<String> = emptySet(),
    val evidence: GithubEvidenceInput? = null,
    val githubStatus: PrismScoring.GithubStatus = PrismScoring.GithubStatus.NONE
)

object ProjectLevelEngine {

    const val GITHUB_PROOF = "github_proof"
    const val TECHNICAL_DIFFICULTY = "technical_difficulty"
    const val REAL_WORLD_USE = "real_world_use"
    const val PROBLEM_VALUE = "problem_value_originality"
    const val COMPLETENESS = "completeness"
    const val EFFORT_SCALE = "effort_scale"
    const val STACK_RELEVANCE = "stack_relevance"
    const val DESCRIPTION_CLARITY = "description_clarity"

    val PARAMETERS = listOf(
        LevelParameter(GITHUB_PROOF, "GitHub proof", 20, "add a public GitHub link that you own, with a README"),
        LevelParameter(TECHNICAL_DIFFICULTY, "Technical difficulty", 20, "add features like login, a database or an API"),
        LevelParameter(REAL_WORLD_USE, "Real-world use", 15, "add a working live link"),
        LevelParameter(PROBLEM_VALUE, "Problem value", 15, "explain the real problem your project solves"),
        LevelParameter(COMPLETENESS, "Completeness", 10, "finish your tasks and update your progress"),
        LevelParameter(EFFORT_SCALE, "Effort and size", 10, "keep building and save your work to GitHub often"),
        LevelParameter(STACK_RELEVANCE, "Tech stack", 5, "list the technology you used, like Kotlin or React"),
        LevelParameter(DESCRIPTION_CLARITY, "Description", 5, "write 2 or 3 clear sentences about what it does")
    )

    /** Parameters the AI is allowed to score (technical difficulty only by +-2). */
    val AI_PARAMETERS = setOf(PROBLEM_VALUE, DESCRIPTION_CLARITY, TECHNICAL_DIFFICULTY)
    const val AI_TECHNICAL_MAX_MOVE = 2.0

    // --------------------------------------------------------
    // Bundled list of in-demand technologies
    // --------------------------------------------------------

    private val IN_DEMAND_TECH = setOf(
        "kotlin", "java", "python", "javascript", "typescript", "react", "react native", "node", "nodejs",
        "node.js", "express", "next", "nextjs", "next.js", "angular", "vue", "svelte", "flutter", "dart",
        "swift", "swiftui", "android", "jetpack", "compose", "firebase", "firestore", "supabase", "mongodb",
        "mongo", "postgres", "postgresql", "mysql", "sql", "sqlite", "redis", "graphql", "rest", "api",
        "docker", "kubernetes", "aws", "azure", "gcp", "terraform", "linux", "git", "github", "ci/cd",
        "spring", "springboot", "django", "flask", "fastapi", "tensorflow", "pytorch", "keras", "sklearn",
        "scikit-learn", "pandas", "numpy", "opencv", "langchain", "llm", "openai", "gemini", "c++", "cpp",
        "c#", "dotnet", ".net", "go", "golang", "rust", "tailwind", "html", "css", "retrofit", "okhttp",
        "room", "hilt", "dagger", "coroutines", "kafka", "rabbitmq", "nginx", "stripe", "razorpay",
        "websocket", "socket.io", "pytest", "junit", "jest", "selenium", "figma", "mlkit"
    )

    private val TUTORIAL_WORDS = listOf(
        "tutorial", "clone", "todo", "to-do", "to do", "calculator", "practice", "learning project",
        "hello world", "demo app", "sample", "follow along", "course project", "basic app", "copy of"
    )

    private val REAL_PROBLEM_WORDS = listOf(
        "problem", "solve", "solves", "helps", "help ", "students", "farmers", "patients", "hospital",
        "teachers", "small business", "shop owners", "users struggle", "real users", "customers",
        "community", "accessibility", "reduce", "save time", "saves time", "automate", "automates",
        "pain point", "so that", "in order to"
    )

    // --------------------------------------------------------
    // Public API
    // --------------------------------------------------------

    fun compute(
        input: ProjectLevelInput,
        today: LocalDate = LocalDate.now(),
        aiScores: Map<String, Double>? = null,
        aiSummary: String? = null
    ): LevelResult {
        val rules = rulesParameters(input, today)
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
            name = PrismScoring.projectLevelName(level),
            reason = reason,
            source = if (usedAi) "ai+rules" else "rules",
            tips = LevelMath.lowestTwo(merged, PARAMETERS, level)
        )
    }

    /** The AI may only move the allowed parameters. Everything else stays from the rules. */
    fun mergeAi(rules: Map<String, Double>, ai: Map<String, Double>?): Map<String, Double> {
        if (ai.isNullOrEmpty()) return rules
        val result = rules.toMutableMap()
        for ((key, raw) in ai) {
            if (key !in AI_PARAMETERS) continue
            val value = raw.coerceIn(0.0, 10.0)
            val rulesValue = rules[key] ?: 0.0
            result[key] = if (key == TECHNICAL_DIFFICULTY) {
                value.coerceIn(rulesValue - AI_TECHNICAL_MAX_MOVE, rulesValue + AI_TECHNICAL_MAX_MOVE)
                    .coerceIn(0.0, 10.0)
            } else {
                value
            }
        }
        return result
    }

    fun rulesParameters(input: ProjectLevelInput, today: LocalDate = LocalDate.now()): Map<String, Double> =
        mapOf(
            GITHUB_PROOF to githubProof(input),
            TECHNICAL_DIFFICULTY to technicalDifficulty(input),
            REAL_WORLD_USE to realWorldUse(input),
            PROBLEM_VALUE to problemValue(input),
            COMPLETENESS to completeness(input),
            EFFORT_SCALE to effortScale(input, today),
            STACK_RELEVANCE to stackRelevance(input.techStack),
            DESCRIPTION_CLARITY to descriptionClarity(input.description)
        )

    // --------------------------------------------------------
    // Parameters (each returns 0-10)
    // --------------------------------------------------------

    fun githubProof(input: ProjectLevelInput): Double {
        val e = input.evidence ?: return 0.0
        if (input.githubStatus == PrismScoring.GithubStatus.NOT_YOURS) return 0.0
        if (!e.isAccessible) return 0.0

        var points = 0.0
        if (e.isValidUrl) points += 10
        if (e.isPublic) points += 10
        if (e.hasReadme) points += 5
        if (e.readmeMeaningful) points += 15
        if (e.meaningfulCode) points += 25
        if (e.sourceSamples > 0) points += 10
        if (e.sourceFiles > 0) points += 5
        if (e.recentCommits > 0) points += 10
        if (e.languageCount > 0) points += 5
        points += when (e.ownerMatches) {
            true -> 5.0
            null -> 2.5 // profile URL missing: half credit for the owner part
            false -> 0.0
        }
        return (points / 10.0).coerceIn(0.0, 10.0)
    }

    fun technicalDifficulty(input: ProjectLevelInput): Double {
        val detected = input.evidence?.detectedFeatures.orEmpty()
        val chipsOnly = input.featureChips.filter { it in ProjectFeatures.ALL && it !in detected }
        val count = detected.size + chipsOnly.size * 0.5
        return (count / 5.0 * 10.0).coerceIn(0.0, 10.0) // full score at 5+ features
    }

    fun realWorldUse(input: ProjectLevelInput): Double {
        var share = 0.0
        if (input.liveStatus == PrismScoring.LiveStatus.WORKS) {
            share += 0.60
            if (input.isStoreLink) share += 0.20
        }
        share += RealUsersTier.share(input.realUsers) * 0.20
        if (ProjectFeatures.DEPLOYMENT in input.featureChips) share += 0.05
        return (share * 10.0).coerceIn(0.0, 10.0)
    }

    fun problemValue(input: ProjectLevelInput): Double {
        val text = (input.name + " " + input.description).lowercase()
        val tutorialHits = LevelMath.containsAnyTerm(text, TUTORIAL_WORDS)
        val realHits = LevelMath.containsAnyTerm(text, REAL_PROBLEM_WORDS)
        var score = 5.0
        score -= (tutorialHits * 2.0).coerceAtMost(4.0)
        score += (realHits * 0.8).coerceAtMost(2.5)
        return score.coerceIn(1.0, 8.0)
    }

    fun completeness(input: ProjectLevelInput): Double {
        val statusShare = when (input.status.trim().uppercase()) {
            "COMPLETED" -> 1.0
            "ONGOING" -> 0.60
            "ARCHIVED" -> 0.70
            "PLANNING" -> 0.30
            else -> 0.15
        }
        val progress = input.progress.coerceIn(0, 100) / 100.0
        val value = if (input.tasksTotal > 0) {
            val tasks = input.tasksDone.toDouble() / input.tasksTotal
            statusShare * 0.5 + progress * 0.3 + tasks * 0.2
        } else {
            statusShare * 0.6 + progress * 0.4
        }
        return (value * 10.0).coerceIn(0.0, 10.0)
    }

    fun effortScale(input: ProjectLevelInput, today: LocalDate = LocalDate.now()): Double {
        val days = daysSince(input.startDate, today)
        val daysShare = ((days ?: 0L) / 120.0).coerceIn(0.0, 1.0)

        val e = input.evidence
        val commitShare = ((e?.recentCommits ?: 0) / 10.0).coerceIn(0.0, 1.0)
        val sizeShare = maxOf(
            ((e?.sourceFiles ?: 0) / 40.0),
            ((e?.sourceCharacters ?: 0) / 30000.0)
        ).coerceIn(0.0, 1.0)
        val teamShare = ((input.teamSize - 1).coerceAtLeast(0) / 3.0).coerceIn(0.0, 1.0)

        val value = daysShare * 0.40 + commitShare * 0.20 + sizeShare * 0.25 + teamShare * 0.15
        return (value * 10.0).coerceIn(0.0, 10.0)
    }

    fun stackRelevance(techStack: String): Double {
        val items = techStack.lowercase()
            .split(Regex("[,/|;\\n&+]+|\\s+and\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (items.isEmpty()) return 0.0
        val known = items.count { item ->
            item in IN_DEMAND_TECH || LevelMath.words(item).any { it in IN_DEMAND_TECH }
        }
        return (known.toDouble() / items.size * 10.0).coerceIn(0.0, 10.0)
    }

    fun descriptionClarity(description: String): Double {
        val text = description.trim()
        val length = text.length
        if (length < 10) return 0.0

        val lengthShare = when {
            length < 40 -> length / 40.0 * 0.5
            length <= 600 -> 0.5 + 0.3 * ((length - 40) / 200.0).coerceAtMost(1.0)
            else -> 0.8 - ((length - 600) / 1500.0).coerceAtMost(0.3)
        }
        val sentences = text.split(Regex("[.!?]+\\s*")).count { it.trim().length >= 8 }
        val sentenceShare = (sentences / 3.0).coerceIn(0.0, 1.0) * 0.2
        return ((lengthShare + sentenceShare) * 10.0).coerceIn(0.0, 10.0)
    }

    // --------------------------------------------------------
    // Dates
    // --------------------------------------------------------

    /** Understands DD/MM/YYYY, DD-MM-YYYY and YYYY-MM-DD. Returns null if unreadable. */
    fun parseDate(text: String): LocalDate? {
        val t = text.trim()
        Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})$").matchEntire(t)?.let {
            return safeDate(it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())
        }
        Regex("^(\\d{4})[/.-](\\d{1,2})[/.-](\\d{1,2})$").matchEntire(t)?.let {
            return safeDate(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        return null
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        null
    }

    fun daysSince(startDate: String, today: LocalDate): Long? {
        val start = parseDate(startDate) ?: return null
        return ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)
    }
}
