package com.prismorbit.app

// ============================================================
// LEVEL MODELS (shared by the project and internship engines)
// ============================================================
// The app keeps all weights in code. Rules compute every
// parameter first (0-10). The optional AI can only replace a few
// text-based parameters (see LevelAi). The user can never set a
// level by hand.
// ============================================================

data class LevelParameter(
    val key: String,
    val label: String,
    val weight: Int,
    val tip: String
)

data class LevelTip(
    val key: String,
    val label: String,
    val text: String
)

data class LevelResult(
    /** parameter key -> 0-10 */
    val params: Map<String, Double>,
    /** 0-100 */
    val score: Int,
    /** 1-5 */
    val level: Int,
    val name: String,
    val reason: String,
    /** "rules" or "ai+rules" */
    val source: String,
    /** The 2 lowest scoring parameters, as simple tips. */
    val tips: List<LevelTip>
) {
    val label: String get() = PrismScoring.levelLabel(level, name)
}

internal object LevelMath {

    /** weighted sum of (param / 10 x weight). Result is 0-100. */
    fun weightedScore(params: Map<String, Double>, definitions: List<LevelParameter>): Double =
        definitions.sumOf { def ->
            (params[def.key] ?: 0.0).coerceIn(0.0, 10.0) / 10.0 * def.weight
        }.coerceIn(0.0, 100.0)

    /** The two lowest scoring parameters. A bigger weight wins a tie. */
    fun lowestTwo(
        params: Map<String, Double>,
        definitions: List<LevelParameter>,
        level: Int,
        maxTips: Int = 2
    ): List<LevelTip> {
        val target = (level + 1).coerceAtMost(5)
        return definitions
            .sortedWith(
                compareBy<LevelParameter> { params[it.key] ?: 0.0 }
                    .thenByDescending { it.weight }
            )
            .take(maxTips)
            .map {
                LevelTip(
                    key = it.key,
                    label = it.label,
                    text = if (level >= 5) "Keep it up: ${it.tip}"
                    else "To reach Level $target: ${it.tip}"
                )
            }
    }

    fun bestParameter(params: Map<String, Double>, definitions: List<LevelParameter>): LevelParameter? =
        definitions.maxWithOrNull(
            compareBy<LevelParameter> { (params[it.key] ?: 0.0) }.thenBy { it.weight }
        )?.takeIf { (params[it.key] ?: 0.0) >= 6.0 }

    fun oneLineReason(
        params: Map<String, Double>,
        definitions: List<LevelParameter>,
        level: Int
    ): String {
        val best = bestParameter(params, definitions)
        val tip = lowestTwo(params, definitions, level, 1).firstOrNull()
        val good = best?.let { "Strong ${it.label.lowercase()}." }
        val next = tip?.let { it.text.replaceFirstChar { c -> c.uppercase() } + "." }
        return listOfNotNull(good, next).joinToString(" ").ifBlank { "Add more details to grow your level." }
    }

    fun cleanText(text: String, maxLength: Int): String =
        text.replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)

    private val WORD_SPLIT = Regex("[^a-z0-9+#.]+")

    fun words(text: String): List<String> =
        text.lowercase().split(WORD_SPLIT).filter { it.isNotBlank() }

    fun containsAnyTerm(text: String, terms: Collection<String>): Int {
        val lower = text.lowercase()
        return terms.count { lower.contains(it) }
    }
}
