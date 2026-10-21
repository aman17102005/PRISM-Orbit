package com.prismorbit.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

// =========================================================
// PROJECT MODEL
// =========================================================
// users/{uid}/projects/{id}. Every new field is optional, so
// old documents (and documents written by the old portfolio
// screen) still load.
// =========================================================

data class ProjectTask(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val title: String,
    val completed: Boolean = false
)

data class ProjectItem(
    val id: Long = System.currentTimeMillis(),
    // Real Firestore document ID. Kept separately so older projects
    // whose document ID is not numeric can still be edited/deleted.
    val firestoreId: String = "",
    val name: String,
    val description: String,
    val techStack: String,
    val startDate: String,
    val status: String,
    val githubUrl: String,
    val liveUrl: String,
    val progress: Int,
    val tasks: List<ProjectTask> = emptyList(),
    val photos: List<String> = emptyList(),

    // ---- link proof (written by the background check)
    val githubEvidence: Map<String, Any?> = emptyMap(),
    val liveCheck: Map<String, Any?> = emptyMap(),

    // ---- optional details typed or tapped by the student
    val realUsers: String = RealUsersTier.NONE,
    val teamSize: Int = 0,
    val featureChips: List<String> = emptyList(),

    // ---- level (missing = Level 3, worked out later in the background)
    val levelParams: Map<String, Double> = emptyMap(),
    val levelScore: Int? = null,
    val levelFinal: Int? = null,
    val levelReason: String = "",
    val levelSource: String = "",
    val levelHash: String = "",
    val levelUpdatedAt: Long = 0L,
    /** The AI's own scores, kept so the AI does not run again for the same text. */
    val levelAiScores: Map<String, Double> = emptyMap(),
    val levelAiSummary: String = ""
) {
    val documentId: String get() = firestoreId.ifBlank { id.toString() }

    val hasLevel: Boolean get() = levelFinal != null

    /** Missing level counts as Level 3 (factor 1.00). */
    val level: Int get() = (levelFinal ?: PrismScoring.DEFAULT_LEVEL).coerceIn(1, 5)

    val levelName: String get() = PrismScoring.projectLevelName(level)

    val githubStatus: PrismScoring.GithubStatus
        get() = GithubEvidenceInput.githubStatus(githubUrl, githubEvidence)

    val liveStatus: PrismScoring.LiveStatus
        get() = PrismScoreLoader.liveStatus(liveUrl, liveCheck)

    fun toScoreInput(): PrismScoring.ProjectScoreInput = PrismScoreLoader.projectInput(
        status = status,
        progress = progress,
        techStack = techStack,
        githubUrl = githubUrl,
        githubEvidence = githubEvidence,
        liveUrl = liveUrl,
        liveCheck = liveCheck,
        hasTasks = tasks.isNotEmpty(),
        level = level
    )

    fun toLevelInput(): ProjectLevelInput = ProjectLevelInput(
        name = name,
        description = description,
        techStack = techStack,
        startDate = startDate,
        status = status,
        progress = progress,
        tasksTotal = tasks.size,
        tasksDone = tasks.count { it.completed },
        liveStatus = liveStatus,
        isStoreLink = (liveCheck["isStore"] as? Boolean) ?: LiveLinks.isStoreLink(liveUrl),
        realUsers = realUsers,
        teamSize = teamSize,
        featureChips = featureChips.toSet(),
        evidence = GithubEvidenceInput.fromMap(githubEvidence),
        githubStatus = githubStatus
    )

    /** Takes only the fields the background job owns, so a newer edit is never undone. */
    fun withBackgroundFrom(other: ProjectItem): ProjectItem = copy(
        githubEvidence = other.githubEvidence,
        liveCheck = other.liveCheck,
        levelParams = other.levelParams,
        levelScore = other.levelScore,
        levelFinal = other.levelFinal,
        levelReason = other.levelReason,
        levelSource = other.levelSource,
        levelHash = other.levelHash,
        levelUpdatedAt = other.levelUpdatedAt,
        levelAiScores = other.levelAiScores,
        levelAiSummary = other.levelAiSummary
    )

    fun withLevel(
        result: LevelResult,
        aiScores: Map<String, Double>,
        aiSummary: String,
        hash: String,
        now: Long
    ): ProjectItem =
        copy(
            levelParams = result.params,
            levelScore = result.score,
            levelFinal = result.level,
            levelReason = result.reason,
            levelSource = result.source,
            levelHash = hash,
            levelUpdatedAt = now,
            levelAiScores = aiScores,
            levelAiSummary = aiSummary
        )
}

/** The fields the background job owns. Written with update(), never with the form fields. */
internal fun ProjectItem.backgroundFieldsMap(): Map<String, Any?> = buildMap {
    put("githubEvidence", githubEvidence)
    put("liveCheck", liveCheck)
    levelFinal?.let { final ->
        put("levelParams", levelParams)
        put("levelScore", levelScore ?: 0)
        put("levelFinal", final)
        put("levelReason", levelReason)
        put("levelSource", levelSource)
        put("levelHash", levelHash)
        put("levelUpdatedAt", levelUpdatedAt)
        put("levelAiScores", levelAiScores)
        put("levelAiSummary", levelAiSummary)
    }
}

internal fun ProjectItem.toMap(): Map<String, Any?> = buildMap {
    put("id", id)
    put("name", name)
    put("description", description)
    put("techStack", techStack)
    put("startDate", startDate)
    put("status", status)
    put("githubUrl", githubUrl)
    put("liveUrl", liveUrl)
    put("progress", progress)
    put(
        "tasks",
        tasks.map { task ->
            mapOf(
                "id" to task.id,
                "title" to task.title,
                "completed" to task.completed
            )
        }
    )
    put("photos", photos)
    put("realUsers", realUsers)
    put("teamSize", teamSize)
    put("featureChips", featureChips)
    // The proof and level fields are only written when the item has them.
    // An empty proof (link removed or changed) deletes the old proof, so a
    // changed link never keeps the proof of the old link.
    put("githubEvidence", if (githubEvidence.isNotEmpty()) githubEvidence else FieldValue.delete())
    put("liveCheck", if (liveCheck.isNotEmpty()) liveCheck else FieldValue.delete())
    if (levelFinal != null) putAll(backgroundFieldsMap())
}

internal fun documentToProjectItem(
    document: DocumentSnapshot
): ProjectItem? {

    val name = document.getString("name")
        ?.trim()
        .orEmpty()

    if (name.isBlank()) {
        return null
    }

    val taskList: List<ProjectTask> =
        (document.get("tasks") as? List<*>)
            ?.mapNotNull { rawTask ->

                val taskMap =
                    rawTask as? Map<*, *>
                        ?: return@mapNotNull null

                val title =
                    taskMap["title"]
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                if (title.isBlank()) {
                    return@mapNotNull null
                }

                val taskId =
                    when (val rawId = taskMap["id"]) {

                        is Number ->
                            rawId.toLong()

                        is String ->
                            rawId.toLongOrNull()

                        else ->
                            null
                    }
                        ?: System.currentTimeMillis()

                val completed =
                    taskMap["completed"] as? Boolean
                        ?: false

                ProjectTask(
                    id = taskId,
                    title = title,
                    completed = completed
                )
            }
            ?: emptyList()

    val photos: List<String> =
        (document.get("photos") as? List<*>)
            ?.mapNotNull { photo ->
                photo?.toString()
            }
            ?: emptyList()

    /*
     * Firebase id can be:
     * - Number  -> old project data
     * - String  -> newer project data
     *
     * Never call document.getLong("id") directly,
     * because it crashes when Firebase contains a String.
     */

    val projectId: Long =
        when (val rawId = document.get("id")) {

            is Number ->
                rawId.toLong()

            is String ->
                rawId.toLongOrNull()
                    ?: document.id.toLongOrNull()
                    ?: document.id.hashCode().toLong()

            else ->
                document.id.toLongOrNull()
                    ?: document.id.hashCode().toLong()
        }

    val progressValue: Int =
        when (val rawProgress = document.get("progress")) {

            is Number ->
                rawProgress.toInt()

            is String ->
                rawProgress.toIntOrNull()
                    ?: 0

            else ->
                0
        }.coerceIn(0, 100)

    val params = PrismScoreLoader.stringMap(document.get("levelParams"))
        ?.mapNotNull { (key, value) -> PrismScoreLoader.number(value)?.let { key to it } }
        ?.toMap()
        ?: emptyMap()

    val aiScores = PrismScoreLoader.stringMap(document.get("levelAiScores"))
        ?.mapNotNull { (key, value) -> PrismScoreLoader.number(value)?.let { key to it } }
        ?.toMap()
        ?: emptyMap()

    val levelFinalValue = PrismScoreLoader.number(document.get("levelFinal"))
        ?.toInt()
        ?.coerceIn(1, 5)

    return ProjectItem(
        id = projectId,
        firestoreId = document.id,

        name = name,

        description =
            document.getString("description")
                ?: "",

        techStack =
            document.getString("techStack")
                ?: document.getString("codingLanguage")
                ?: "",

        startDate =
            document.getString("startDate")
                ?: "",

        status =
            document.getString("status")
                ?: "IDEA",

        githubUrl =
            document.getString("githubUrl")
                ?: "",

        liveUrl =
            document.getString("liveUrl")
                ?: "",

        progress = progressValue,

        tasks = taskList,

        photos = photos,

        githubEvidence = PrismScoreLoader.stringMap(document.get("githubEvidence")) ?: emptyMap(),
        liveCheck = PrismScoreLoader.stringMap(document.get("liveCheck")) ?: emptyMap(),

        realUsers = document.getString("realUsers")
            ?.takeIf { value -> RealUsersTier.ALL.any { it.first == value } }
            ?: RealUsersTier.NONE,
        teamSize = (PrismScoreLoader.number(document.get("teamSize"))?.toInt() ?: 0).coerceIn(0, 999),
        featureChips = (document.get("featureChips") as? List<*>)
            ?.mapNotNull { it?.toString() }
            ?.filter { it in ProjectFeatures.ALL }
            ?: emptyList(),

        levelParams = params,
        levelScore = PrismScoreLoader.number(document.get("levelScore"))?.toInt(),
        levelFinal = levelFinalValue,
        levelReason = document.getString("levelReason").orEmpty(),
        levelSource = document.getString("levelSource").orEmpty(),
        levelHash = document.getString("levelHash").orEmpty(),
        levelUpdatedAt = PrismScoreLoader.number(document.get("levelUpdatedAt"))?.toLong() ?: 0L,
        levelAiScores = aiScores,
        levelAiSummary = document.getString("levelAiSummary").orEmpty()
    )
}
