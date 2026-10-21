package com.prismorbit.app

// ============================================================
// PROOF TEXT CHECKER (plain Kotlin)
// ============================================================
// Takes the text that ML Kit read from a photo or PDF and checks
// that it matches what the student typed. It never stores the
// text. Only the small result is saved.
// ============================================================

data class ProofCheckResult(
    /** "OK", "PARTIAL" or "UNREADABLE" */
    val status: String,
    val companyMatch: Boolean,
    val nameMatch: Boolean,
    val keywordHits: List<String>,
    val datesFound: Int
) {
    fun toSummary() = ProofSummary(status, companyMatch, nameMatch, keywordHits, datesFound)
}

object ProofTextChecker {

    const val OK = "OK"
    const val PARTIAL = "PARTIAL"
    const val UNREADABLE = "UNREADABLE"

    private const val MIN_READABLE_LETTERS = 25

    private val KEYWORDS = listOf(
        "offer letter", "offer", "internship", "intern", "certificate", "completion",
        "completed", "joining", "stipend", "appointment", "letter of", "employment", "training"
    )

    private val MONTHS = "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|" +
            "aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?"

    private val DATE_PATTERNS = listOf(
        Regex("\\b\\d{1,2}[/.-]\\d{1,2}[/.-](?:\\d{4}|\\d{2})\\b"),
        Regex("\\b\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}\\b"),
        Regex("\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:$MONTHS)\\.?,?\\s+\\d{2,4}\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:$MONTHS)\\.?\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{4}\\b", RegexOption.IGNORE_CASE)
    )

    fun check(text: String, companyTyped: String, userName: String): ProofCheckResult {
        val letters = text.count { it.isLetter() }
        if (letters < MIN_READABLE_LETTERS) {
            return ProofCheckResult(UNREADABLE, false, false, emptyList(), 0)
        }

        val lower = text.lowercase()

        val companyMatch = companyTyped.isNotBlank() &&
                CompanyMatcher.nameAppearsInText(companyTyped, text)
        val nameMatch = nameAppearsInText(userName, lower)
        val hits = KEYWORDS.filter { lower.contains(it) }
            // "offer letter" already covers "offer"; keep the list short and simple
            .filterNot { it == "offer" && "offer letter" in lower }
            .distinct()
        val dates = countDates(text)

        // Points: company 2, name 1, keywords 1, dates 1.
        var points = 0
        if (companyMatch) points += 2
        if (nameMatch) points += 1
        if (hits.isNotEmpty()) points += 1
        if (dates > 0) points += 1

        // The text was readable, so it is at least PARTIAL.
        val status = if (companyMatch && points >= 4) OK else PARTIAL
        return ProofCheckResult(status, companyMatch, nameMatch, hits, dates)
    }

    /** Every word of the name (2+ letters) must appear. A one-word name needs 3+ letters. */
    fun nameAppearsInText(userName: String, lowerText: String): Boolean {
        val parts = userName.lowercase()
            .replace(Regex("[^a-z ]+"), " ")
            .split(' ')
            .filter { it.length >= 2 }
        if (parts.isEmpty()) return false
        if (parts.size == 1 && parts[0].length < 3) return false
        val words = lowerText.replace(Regex("[^a-z]+"), " ").split(' ').toSet()
        return parts.all { it in words }
    }

    fun countDates(text: String): Int =
        DATE_PATTERNS.flatMap { it.findAll(text).map { m -> m.value }.toList() }
            .distinct()
            .size

    /** Simple words for the screen: "Company matched, your name matched, dates found". */
    fun describe(summary: ProofSummary): String {
        if (!summary.isPresent) return "No proof added."
        if (summary.status.uppercase() == UNREADABLE) {
            return "We could not read this file. Try a clearer photo or a PDF."
        }
        val parts = mutableListOf<String>()
        parts += if (summary.companyMatch) "Company matched" else "Company not found"
        parts += if (summary.nameMatch) "your name matched" else "your name not found"
        if (summary.keywordHits.isNotEmpty()) parts += "internship words found"
        parts += if (summary.datesFound > 0) "dates found" else "no dates found"
        return parts.joinToString(", ") + "."
    }
}
