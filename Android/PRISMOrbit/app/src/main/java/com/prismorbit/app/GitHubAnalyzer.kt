package com.prismorbit.app

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Public GitHub repository evidence.
 *
 * IMPORTANT:
 * This class only collects repository evidence.
 * It does NOT calculate SMART AI scores or priorities.
 */
data class GitHubRepositoryEvidence(
    val isValidUrl: Boolean = false,
    val isPublic: Boolean = false,
    val isAccessible: Boolean = false,
    val owner: String = "",
    val repository: String = "",
    val description: String = "",
    val defaultBranch: String = "",
    val stars: Int = 0,
    val forks: Int = 0,
    val hasReadme: Boolean = false,

    // Existing repository-level evidence
    val totalFileCount: Int = 0,
    val sourceFileCount: Int = 0,
    val recentCommitSampleSize: Int = 0,
    val languageCount: Int = 0,
    val languages: List<String> = emptyList(),
    val pushedAt: String? = null,

    // -------------------------------------------------------------
    // NEW: ACTUAL CONTENT EVIDENCE
    // -------------------------------------------------------------

    /**
     * Number of characters successfully read from README content.
     */
    val readmeContentLength: Int = 0,

    /**
     * Whether README contains meaningful non-empty content.
     */
    val readmeHasMeaningfulContent: Boolean = false,

    /**
     * Number of actual source files whose contents were sampled.
     */
    val sourceContentSampleCount: Int = 0,

    /**
     * Number of sampled source files containing non-empty content.
     */
    val sourceContentNonEmptyCount: Int = 0,

    /**
     * Total number of characters successfully read
     * from sampled source files.
     */
    val sourceContentCharacters: Int = 0,

    /**
     * Whether sampled source content appears to contain
     * meaningful implementation rather than being effectively empty.
     */
    val sourceContentHasMeaningfulCode: Boolean = false,

    /**
     * Names/paths of source files whose contents were sampled.
     *
     * This is evidence only. It is not used for scoring here.
     */
    val sampledSourceFiles: List<String> = emptyList(),

    /**
     * Features found from file names and a few manifest files:
     * auth, database, backend_api, ai_ml, payments, realtime,
     * mobile_app, deployment, tests. Evidence only.
     */
    val detectedFeatures: List<String> = emptyList(),

    /**
     * Why the check did not work. Empty when it worked.
     * One of: INVALID_URL, NOT_FOUND, RATE_LIMITED, NETWORK, OTHER.
     * RATE_LIMITED, NETWORK and OTHER mean "could not check", never "failed".
     */
    val failure: String = "",

    val errorMessage: String? = null
)

/** A GitHub answer that was not 2xx. */
class GitHubHttpException(
    val code: Int,
    message: String,
    val rateLimited: Boolean
) : IllegalStateException(message)

object GitHubAnalyzer {

    private const val API_BASE = "https://api.github.com"

    // -------------------------------------------------------------
    // CONTENT ANALYSIS LIMITS
    // -------------------------------------------------------------

    /**
     * Maximum number of source files whose contents are actually read
     * (kept small because of GitHub's anonymous rate limit).
     *
     * This keeps GitHub API usage controlled.
     */
    private const val MAX_SOURCE_CONTENT_SAMPLES = 3

    /**
     * Feature detection reads at most this many file paths and 2 manifest files.
     * GitHub allows about 60 anonymous requests per hour, so one check is
     * kept to about 11 requests.
     */
    private const val MAX_PATHS_FOR_FEATURES = 6000
    private const val MAX_MANIFEST_CHARS = 20_000

    /**
     * Maximum README characters considered by the analyzer.
     */
    private const val MAX_README_CONTENT_CHARS = 20_000

    /**
     * Maximum characters considered from each source file.
     */
    private const val MAX_SOURCE_CONTENT_CHARS_PER_FILE = 12_000

    /**
     * Analyses a PUBLIC GitHub repository.
     *
     * No score is calculated here.
     * This function only returns evidence that SMART AI
     * can use later.
     */
    suspend fun analyzeRepository(
        repositoryUrl: String
    ): GitHubRepositoryEvidence = withContext(Dispatchers.IO) {

        val parsedRepository = parseRepositoryUrl(repositoryUrl)

        if (parsedRepository == null) {
            return@withContext GitHubRepositoryEvidence(
                isValidUrl = false,
                failure = "INVALID_URL",
                errorMessage = "Invalid GitHub repository URL."
            )
        }

        val owner = parsedRepository.first
        val repository = parsedRepository.second

        try {
            // ---------------------------------------------------------
            // 1. Repository metadata
            // ---------------------------------------------------------

            val repositoryJson = getJson(
                "$API_BASE/repos/$owner/$repository"
            )

            val defaultBranch =
                repositoryJson.optString("default_branch", "main")

            val description =
                repositoryJson.optString("description", "")

            val stars =
                repositoryJson.optInt("stargazers_count", 0)

            val forks =
                repositoryJson.optInt("forks_count", 0)

            val pushedAt =
                repositoryJson.optString("pushed_at", null)

            // GitHub API returning repository metadata means
            // the public repository is accessible.
            val isPublic =
                !repositoryJson.optBoolean("private", true)

            // ---------------------------------------------------------
            // 2. Repository root contents
            // ---------------------------------------------------------

            val rootContents = try {
                getJsonArray(
                    "$API_BASE/repos/$owner/$repository/contents"
                )
            } catch (e: GitHubHttpException) {
                // An empty repository answers 404 here. The repository exists.
                if (e.code == 404) JSONArray() else throw e
            }

            var hasReadme = false
            var readmeFileName = ""

            for (index in 0 until rootContents.length()) {

                val item = rootContents.optJSONObject(index)
                    ?: continue

                val name =
                    item.optString("name", "")

                if (
                    name.equals("README.md", ignoreCase = true) ||
                    name.equals("README", ignoreCase = true) ||
                    name.startsWith("README.", ignoreCase = true)
                ) {
                    hasReadme = true
                    readmeFileName = name
                    break
                }
            }

            // ---------------------------------------------------------
            // 3. Languages
            // ---------------------------------------------------------

            val languagesJson = getJson(
                "$API_BASE/repos/$owner/$repository/languages"
            )

            val languages = mutableListOf<String>()

            val languageKeys =
                languagesJson.keys()

            while (languageKeys.hasNext()) {
                languages.add(languageKeys.next())
            }

            // ---------------------------------------------------------
            // 4. Repository tree
            // ---------------------------------------------------------

            var totalFileCount = 0
            var sourceFileCount = 0

            /**
             * We retain source paths separately so that actual content
             * can be sampled later.
             */
            val sourceFilePaths = mutableListOf<String>()
            val allFilePaths = mutableListOf<String>()

            try {

                val treeJson = getJson(
                    "$API_BASE/repos/$owner/$repository/git/trees/$defaultBranch?recursive=1"
                )

                val tree =
                    treeJson.optJSONArray("tree")

                if (tree != null) {

                    for (index in 0 until tree.length()) {

                        val item =
                            tree.optJSONObject(index)
                                ?: continue

                        if (item.optString("type") != "blob") {
                            continue
                        }

                        totalFileCount++

                        val path =
                            item.optString("path", "")

                        if (allFilePaths.size < MAX_PATHS_FOR_FEATURES) {
                            allFilePaths.add(path)
                        }

                        if (isSourceFile(path)) {
                            sourceFileCount++

                            if (
                                sourceFilePaths.size <
                                MAX_SOURCE_CONTENT_SAMPLES
                            ) {
                                sourceFilePaths.add(path)
                            }
                        }
                    }
                }

            } catch (_: Exception) {
                // Tree analysis is supplementary evidence.
                // Repository access itself remains valid even
                // if the tree request fails or is truncated.
            }

            // ---------------------------------------------------------
            // 5. Recent commits
            // ---------------------------------------------------------

            var recentCommitSampleSize = 0

            try {

                val commitsJson = getJsonArray(
                    "$API_BASE/repos/$owner/$repository/commits?per_page=10"
                )

                recentCommitSampleSize =
                    commitsJson.length()

            } catch (_: Exception) {
                // Commit information is supplementary evidence.
            }

            // ---------------------------------------------------------
            // 6. ACTUAL README CONTENT ANALYSIS
            // ---------------------------------------------------------

            var readmeContentLength = 0
            var readmeHasMeaningfulContent = false

            if (hasReadme && readmeFileName.isNotBlank()) {

                try {

                    val readmeJson = getJson(
                        "$API_BASE/repos/$owner/$repository/contents/${Uri.encode(readmeFileName)}?ref=$defaultBranch"
                    )

                    val readmeContent =
                        decodeGitHubContent(
                            readmeJson.optString("content", "")
                        )

                    val limitedReadmeContent =
                        readmeContent.take(
                            MAX_README_CONTENT_CHARS
                        )

                    readmeContentLength =
                        limitedReadmeContent.length

                    readmeHasMeaningfulContent =
                        hasMeaningfulText(
                            limitedReadmeContent
                        )

                } catch (_: Exception) {
                    // README content is supplementary evidence.
                    // README existence remains valid even if
                    // content retrieval fails.
                }
            }

            // ---------------------------------------------------------
            // 7. ACTUAL SOURCE CODE CONTENT ANALYSIS
            // ---------------------------------------------------------

            var sourceContentSampleCount = 0
            var sourceContentNonEmptyCount = 0
            var sourceContentCharacters = 0
            var sourceContentHasMeaningfulCode = false

            val sampledSourceFiles =
                mutableListOf<String>()

            for (path in sourceFilePaths) {

                if (
                    sourceContentSampleCount >=
                    MAX_SOURCE_CONTENT_SAMPLES
                ) {
                    break
                }

                try {

                    val encodedPath =
                        Uri.encode(path)

                    val sourceJson = getJson(
                        "$API_BASE/repos/$owner/$repository/contents/$encodedPath?ref=$defaultBranch"
                    )

                    val content =
                        decodeGitHubContent(
                            sourceJson.optString(
                                "content",
                                ""
                            )
                        )

                    val limitedContent =
                        content.take(
                            MAX_SOURCE_CONTENT_CHARS_PER_FILE
                        )

                    sourceContentSampleCount++

                    if (
                        limitedContent
                            .trim()
                            .isNotEmpty()
                    ) {
                        sourceContentNonEmptyCount++
                    }

                    sourceContentCharacters +=
                        limitedContent.length

                    if (
                        hasMeaningfulSourceCode(
                            limitedContent
                        )
                    ) {
                        sourceContentHasMeaningfulCode = true
                    }

                    sampledSourceFiles.add(path)

                } catch (_: Exception) {
                    // Individual source-file content is supplementary.
                    // Continue analysing the remaining samples.
                }
            }

            // ---------------------------------------------------------
            // 8. FEATURE DETECTION (cheap: paths + at most 3 small files)
            // ---------------------------------------------------------

            var detectedFeatures: List<String> = emptyList()

            try {

                val manifests = mutableMapOf<String, String>()

                for (path in ProjectFeatureDetector.pickManifests(allFilePaths, limit = 2)) {
                    try {
                        val manifestJson = getJson(
                            "$API_BASE/repos/$owner/$repository/contents/${Uri.encode(path, "/")}?ref=$defaultBranch"
                        )
                        manifests[path] = decodeGitHubContent(
                            manifestJson.optString("content", "")
                        ).take(MAX_MANIFEST_CHARS)
                    } catch (_: Exception) {
                        // One unreadable manifest does not stop detection.
                    }
                }

                detectedFeatures = ProjectFeatureDetector
                    .detect(allFilePaths, manifests)
                    .toList()

            } catch (_: Exception) {
                // Feature detection is supplementary evidence.
            }

            // ---------------------------------------------------------
            // Final evidence object
            // ---------------------------------------------------------

            GitHubRepositoryEvidence(
                isValidUrl = true,
                isPublic = isPublic,
                isAccessible = true,
                owner = owner,
                repository = repository,
                description = description,
                defaultBranch = defaultBranch,
                stars = stars,
                forks = forks,
                hasReadme = hasReadme,
                totalFileCount = totalFileCount,
                sourceFileCount = sourceFileCount,
                recentCommitSampleSize = recentCommitSampleSize,
                languageCount = languages.size,
                languages = languages.sorted(),
                pushedAt = pushedAt,

                // NEW content evidence
                readmeContentLength = readmeContentLength,
                readmeHasMeaningfulContent =
                    readmeHasMeaningfulContent,
                sourceContentSampleCount =
                    sourceContentSampleCount,
                sourceContentNonEmptyCount =
                    sourceContentNonEmptyCount,
                sourceContentCharacters =
                    sourceContentCharacters,
                sourceContentHasMeaningfulCode =
                    sourceContentHasMeaningfulCode,
                sampledSourceFiles =
                    sampledSourceFiles.toList(),

                detectedFeatures = detectedFeatures,

                errorMessage = null
            )

        } catch (exception: Exception) {

            GitHubRepositoryEvidence(
                isValidUrl = true,
                isPublic = false,
                isAccessible = false,
                owner = owner,
                repository = repository,
                failure = classifyFailure(exception),
                errorMessage =
                    exception.message
                        ?: "Unable to access GitHub repository."
            )
        }
    }

    // ================================================================
    // GITHUB CONTENT DECODING
    // ================================================================

    /**
     * GitHub Contents API normally returns file content as Base64.
     *
     * GitHub may include line breaks inside the Base64 response,
     * therefore whitespace is removed before decoding.
     */
    private fun decodeGitHubContent(
        encodedContent: String
    ): String {

        if (encodedContent.isBlank()) {
            return ""
        }

        return try {

            val cleanedContent =
                encodedContent
                    .replace("\\n", "")
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace(" ", "")

            val decodedBytes =
                Base64.decode(
                    cleanedContent,
                    Base64.DEFAULT
                )

            String(
                decodedBytes,
                Charsets.UTF_8
            )

        } catch (_: Exception) {
            ""
        }
    }

    // ================================================================
    // README CONTENT CHECK
    // ================================================================

    /**
     * Determines whether text contains meaningful content.
     *
     * This is evidence detection only.
     * No score is calculated here.
     */
    private fun hasMeaningfulText(
        content: String
    ): Boolean {

        val cleaned =
            content
                .replace("\u0000", "")
                .trim()

        if (cleaned.length < 80) {
            return false
        }

        val meaningfulLines =
            cleaned
                .lines()
                .count {
                    it.trim().length >= 8
                }

        return meaningfulLines >= 3
    }

    // ================================================================
    // SOURCE CODE CONTENT CHECK
    // ================================================================

    /**
     * Determines whether sampled source content appears to contain
     * meaningful implementation.
     *
     * This deliberately remains lightweight.
     * It does NOT attempt to judge code quality or calculate scores.
     */
    private fun hasMeaningfulSourceCode(
        content: String
    ): Boolean {

        val cleaned =
            content
                .replace("\u0000", "")
                .trim()

        if (cleaned.length < 120) {
            return false
        }

        val lines =
            cleaned.lines()

        val nonEmptyLines =
            lines.count {
                it.trim().isNotEmpty()
            }

        if (nonEmptyLines < 5) {
            return false
        }

        val lower =
            cleaned.lowercase()

        val implementationSignals =
            listOf(
                "{",
                "}",
                "(",
                ")",
                "class ",
                "fun ",
                "function ",
                "def ",
                "import ",
                "return ",
                "if ",
                "for ",
                "while ",
                "const ",
                "let ",
                "var ",
                "public ",
                "private "
            )

        val signalCount =
            implementationSignals.count {
                lower.contains(it)
            }

        return signalCount >= 2
    }

    // ================================================================
    // URL PARSING
    // ================================================================

    private fun parseRepositoryUrl(
        repositoryUrl: String
    ): Pair<String, String>? =
        GitHubUrls.parseRepository(repositoryUrl)

    /**
     * 404 = the repository was not found (or is private).
     * Rate limit, no internet and anything else = we could not check.
     */
    internal fun classifyFailure(exception: Exception): String = when {
        exception is GitHubHttpException && exception.rateLimited -> "RATE_LIMITED"
        exception is GitHubHttpException && exception.code == 404 -> "NOT_FOUND"
        exception is java.io.IOException -> "NETWORK"
        else -> "OTHER"
    }

    // ================================================================
    // HTTP HELPERS
    // ================================================================

    private fun getJson(
        endpoint: String
    ): JSONObject {

        val response =
            executeGet(endpoint)

        return JSONObject(response)
    }

    private fun getJsonArray(
        endpoint: String
    ): JSONArray {

        val response =
            executeGet(endpoint)

        return JSONArray(response)
    }

    private fun executeGet(
        endpoint: String
    ): String {

        val connection =
            URL(endpoint)
                .openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"

            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000

            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )

            connection.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )

            connection.setRequestProperty(
                "User-Agent",
                "PRISM-Orbit"
            )

            val responseCode =
                connection.responseCode

            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseBody =
                stream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    } ?: ""

            if (responseCode !in 200..299) {

                val remaining =
                    connection.getHeaderField("X-RateLimit-Remaining")

                val message =
                    try {

                        JSONObject(responseBody)
                            .optString(
                                "message",
                                "GitHub request failed."
                            )

                    } catch (_: Exception) {

                        "GitHub request failed."
                    }

                val rateLimited =
                    responseCode == 429 ||
                            (responseCode == 403 &&
                                    (remaining == "0" ||
                                            message.contains("rate limit", ignoreCase = true)))

                throw GitHubHttpException(
                    code = responseCode,
                    message = "GitHub API $responseCode: $message",
                    rateLimited = rateLimited
                )
            }

            return responseBody

        } finally {

            connection.disconnect()
        }
    }

    // ================================================================
    // SOURCE FILE DETECTION
    // ================================================================

    private fun isSourceFile(
        path: String
    ): Boolean {

        val lowerPath =
            path.lowercase()

        return lowerPath.endsWith(".kt") ||
                lowerPath.endsWith(".java") ||
                lowerPath.endsWith(".kts") ||
                lowerPath.endsWith(".gradle") ||
                lowerPath.endsWith(".xml") ||
                lowerPath.endsWith(".json") ||
                lowerPath.endsWith(".js") ||
                lowerPath.endsWith(".jsx") ||
                lowerPath.endsWith(".ts") ||
                lowerPath.endsWith(".tsx") ||
                lowerPath.endsWith(".py") ||
                lowerPath.endsWith(".c") ||
                lowerPath.endsWith(".cpp") ||
                lowerPath.endsWith(".h") ||
                lowerPath.endsWith(".hpp") ||
                lowerPath.endsWith(".cs") ||
                lowerPath.endsWith(".swift") ||
                lowerPath.endsWith(".go") ||
                lowerPath.endsWith(".rs") ||
                lowerPath.endsWith(".php") ||
                lowerPath.endsWith(".rb") ||
                lowerPath.endsWith(".dart") ||
                lowerPath.endsWith(".sql")
    }
}