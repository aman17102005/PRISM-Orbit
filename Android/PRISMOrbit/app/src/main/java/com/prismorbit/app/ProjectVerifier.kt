package com.prismorbit.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// ============================================================
// PROJECT VERIFIER
// ============================================================
// Checks a project's GitHub link and live link on a background
// thread. Rules:
//  - Rate limit or no internet = "could not check". It is never
//    marked as failed, it never blocks saving, and the last good
//    evidence is kept.
//  - The repo owner must match the student's GitHub profile URL
//    (ignoring case). A missing profile URL gives a neutral note.
// ============================================================

/** What the chips on the project card show. */
enum class GithubChip(val text: String) {
    NONE(""),
    NOT_CHECKED("GITHUB NOT CHECKED YET"),
    VERIFIED("GITHUB VERIFIED"),
    NOT_FOUND("GITHUB NOT FOUND"),
    NOT_YOURS("NOT YOUR REPO"),
    COULD_NOT_CHECK("COULD NOT CHECK (try later)")
}

enum class LiveChip(val text: String) {
    NONE(""),
    NOT_CHECKED("LIVE LINK NOT CHECKED YET"),
    WORKS("LIVE LINK WORKS"),
    STORE_WORKS("STORE LINK WORKS"),
    NOT_REACHABLE("LINK NOT REACHABLE")
}

object ProjectVerifier {

    const val NEUTRAL_OWNER_NOTE = "Add your GitHub profile URL in Placement to verify ownership"

    private val liveClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // --------------------------------------------------------
    // Chips
    // --------------------------------------------------------

    fun githubChip(url: String, evidence: Map<String, Any?>): GithubChip {
        if (url.isBlank()) return GithubChip.NONE
        if (evidence.isEmpty()) return GithubChip.NOT_CHECKED
        if (evidence["lastAttempt"]?.toString() == "COULD_NOT_CHECK") return GithubChip.COULD_NOT_CHECK
        return when (GithubEvidenceInput.githubStatus(url, evidence)) {
            PrismScoring.GithubStatus.VERIFIED -> GithubChip.VERIFIED
            PrismScoring.GithubStatus.NOT_FOUND -> GithubChip.NOT_FOUND
            PrismScoring.GithubStatus.NOT_YOURS -> GithubChip.NOT_YOURS
            PrismScoring.GithubStatus.COULD_NOT_CHECK -> GithubChip.COULD_NOT_CHECK
            else -> GithubChip.NOT_CHECKED
        }
    }

    /** True when the repo is fine but the student has no profile URL to compare with. */
    fun needsProfileUrlNote(url: String, evidence: Map<String, Any?>): Boolean =
        url.isNotBlank() &&
                evidence["isAccessible"] == true &&
                evidence["ownerMatch"]?.toString() == "unknown"

    fun liveChip(url: String, liveCheck: Map<String, Any?>): LiveChip {
        if (url.isBlank()) return LiveChip.NONE
        return when (liveCheck["status"]?.toString()?.uppercase()) {
            "WORKS" -> if (liveCheck["isStore"] == true) LiveChip.STORE_WORKS else LiveChip.WORKS
            "NOT_REACHABLE" -> LiveChip.NOT_REACHABLE
            else -> LiveChip.NOT_CHECKED
        }
    }

    // --------------------------------------------------------
    // Owner check (local, no network)
    // --------------------------------------------------------

    /** Sets ownerMatch and checkStatus from the saved evidence and the current profile URL. */
    fun applyOwnerCheck(evidence: Map<String, Any?>, profileUrl: String): Map<String, Any?> {
        if (evidence.isEmpty() || evidence["isAccessible"] != true) return evidence
        val owner = evidence["owner"]?.toString().orEmpty()
        val match = GitHubUrls.ownerMatches(owner, profileUrl)
        val ownerMatch = when (match) {
            true -> "yes"
            false -> "no"
            null -> "unknown"
        }
        return evidence + mapOf(
            "ownerMatch" to ownerMatch,
            "checkStatus" to if (match == false) "NOT_YOURS" else "VERIFIED"
        )
    }

    // --------------------------------------------------------
    // GitHub
    // --------------------------------------------------------

    /**
     * Runs the analyzer and returns the map to save as githubEvidence.
     * [lastGood] is the evidence saved before: it is kept when GitHub
     * cannot be reached right now.
     */
    suspend fun checkGithub(
        url: String,
        profileUrl: String,
        lastGood: Map<String, Any?>,
        now: Long = System.currentTimeMillis()
    ): Map<String, Any?> {
        val normalized = GitHubUrls.normalizedRepositoryUrl(url)
            ?: return mapOf(
                "isValidUrl" to false,
                "isAccessible" to false,
                "checkStatus" to "NOT_FOUND",
                "checkedUrl" to url.trim(),
                "checkedAt" to now,
                "failure" to "INVALID_URL"
            )

        val evidence = try {
            GitHubAnalyzer.analyzeRepository(normalized)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            GitHubRepositoryEvidence(
                isValidUrl = true,
                failure = GitHubAnalyzer.classifyFailure(e),
                errorMessage = "Could not check."
            )
        }

        return when {
            evidence.isAccessible -> applyOwnerCheck(
                evidence.toStoredMap() + mapOf("checkedUrl" to normalized, "checkedAt" to now),
                profileUrl
            )

            evidence.failure == "NOT_FOUND" || evidence.failure == "INVALID_URL" -> mapOf(
                "isValidUrl" to evidence.isValidUrl,
                "isAccessible" to false,
                "isPublic" to false,
                "owner" to evidence.owner,
                "repository" to evidence.repository,
                "checkStatus" to "NOT_FOUND",
                "checkedUrl" to normalized,
                "checkedAt" to now,
                "failure" to evidence.failure,
                "errorMessage" to evidence.errorMessage
            )

            else -> couldNotCheck(lastGood, normalized, profileUrl, now)
        }
    }

    /** Keeps the last good evidence if it belongs to this same repository. */
    private fun couldNotCheck(
        lastGood: Map<String, Any?>,
        normalizedUrl: String,
        profileUrl: String,
        now: Long
    ): Map<String, Any?> {
        val sameRepo = lastGood["isAccessible"] == true &&
                lastGood["checkedUrl"]?.toString()?.equals(normalizedUrl, ignoreCase = true) == true
        return if (sameRepo) {
            applyOwnerCheck(lastGood, profileUrl) + mapOf(
                "lastAttempt" to "COULD_NOT_CHECK",
                "lastAttemptAt" to now
            )
        } else {
            mapOf(
                "isValidUrl" to true,
                "isAccessible" to false,
                "checkStatus" to "COULD_NOT_CHECK",
                "lastAttempt" to "COULD_NOT_CHECK",
                "lastAttemptAt" to now,
                "checkedUrl" to normalizedUrl
            )
        }
    }

    /** Same map format the old portfolio screen wrote, plus the new keys. */
    internal fun GitHubRepositoryEvidence.toStoredMap(): Map<String, Any?> = mapOf(
        "isValidUrl" to isValidUrl,
        "isPublic" to isPublic,
        "isAccessible" to isAccessible,
        "owner" to owner,
        "repository" to repository,
        "description" to description,
        "defaultBranch" to defaultBranch,
        "stars" to stars,
        "forks" to forks,
        "hasReadme" to hasReadme,
        "totalFileCount" to totalFileCount,
        "sourceFileCount" to sourceFileCount,
        "recentCommitSampleSize" to recentCommitSampleSize,
        "languageCount" to languageCount,
        "languages" to languages,
        "pushedAt" to pushedAt,
        "readmeContentLength" to readmeContentLength,
        "readmeHasMeaningfulContent" to readmeHasMeaningfulContent,
        "sourceContentSampleCount" to sourceContentSampleCount,
        "sourceContentNonEmptyCount" to sourceContentNonEmptyCount,
        "sourceContentCharacters" to sourceContentCharacters,
        "sourceContentHasMeaningfulCode" to sourceContentHasMeaningfulCode,
        "sampledSourceFiles" to sampledSourceFiles,
        "detectedFeatures" to detectedFeatures,
        "errorMessage" to errorMessage
    )

    // --------------------------------------------------------
    // Live link
    // --------------------------------------------------------

    /** Returns the map to save as liveCheck: status, isStore, checkedAt. */
    suspend fun checkLive(url: String, now: Long = System.currentTimeMillis()): Map<String, Any?> =
        withContext(Dispatchers.IO) {
            val isStore = LiveLinks.isStoreLink(url)
            val reachable = LiveLinks.isWebUrl(url) && isReachable(LiveLinks.normalize(url))
            mapOf(
                "status" to if (reachable) "WORKS" else "NOT_REACHABLE",
                "isStore" to isStore,
                "checkedAt" to now
            )
        }

    private fun isReachable(url: String): Boolean {
        fun attempt(target: String): Boolean {
            // Some servers refuse HEAD, so a failed HEAD is tried again with GET.
            for (method in listOf("HEAD", "GET")) {
                try {
                    val request = Request.Builder()
                        .url(target)
                        .method(method, null)
                        .header("User-Agent", "Mozilla/5.0 (Android) PRISM-Orbit")
                        .build()
                    liveClient.newCall(request).execute().use { response ->
                        if (response.code in 200..399) return true
                    }
                } catch (_: Exception) {
                    // try the next method
                }
            }
            return false
        }

        if (attempt(url)) return true
        // Plain http is blocked on modern phones, so also try https.
        if (url.startsWith("http://", ignoreCase = true)) {
            return attempt("https://" + url.substring(7))
        }
        return false
    }
}
