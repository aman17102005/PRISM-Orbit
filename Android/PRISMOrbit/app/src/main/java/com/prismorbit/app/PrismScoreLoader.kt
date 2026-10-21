package com.prismorbit.app

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

// ============================================================
// PRISM SCORE LOADER
// ============================================================
// Reads the student's Firestore data once and turns it into the
// inputs of PrismScoring. Growth, Placement, the Home dashboard
// and Smart AI all use this, so every screen shows the same
// numbers. Old documents without level fields count as Level 3.
// ============================================================

data class PrismScoreSummary(
    val breakdown: PrismScoring.Breakdown,
    val dsaSolved: Int,
    val projectCount: Int,
    val completedProjects: Int,
    val internshipCount: Int,
    val interviews: Int,
    val selected: Int,
    val achievementCount: Int,
    val certificationCount: Int,
    val learningScore: Int,
    val githubProfileUrl: String
) {
    val readiness: Int get() = breakdown.readiness
    val githubProfileAdded: Boolean get() = githubProfileUrl.isNotBlank()
}

object PrismScoreLoader {

    // --------------------------------------------------------
    // Small readers for plain maps (also used by unit tests)
    // --------------------------------------------------------

    fun number(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    fun stringMap(value: Any?): Map<String, Any?>? =
        (value as? Map<*, *>)
            ?.mapNotNull { (k, v) -> (k as? String)?.let { it to v } }
            ?.toMap()

    /** A missing or broken level counts as Level 3. */
    fun levelOf(value: Any?): Int =
        number(value)?.toInt()?.coerceIn(1, 5) ?: PrismScoring.DEFAULT_LEVEL

    fun liveStatus(url: String, liveCheck: Map<String, Any?>?): PrismScoring.LiveStatus {
        if (url.isBlank()) return PrismScoring.LiveStatus.NONE
        return when (liveCheck?.get("status")?.toString()?.uppercase()) {
            "WORKS" -> PrismScoring.LiveStatus.WORKS
            "NOT_REACHABLE" -> PrismScoring.LiveStatus.NOT_REACHABLE
            else -> PrismScoring.LiveStatus.UNCHECKED
        }
    }

    fun projectInput(
        status: String,
        progress: Int,
        techStack: String,
        githubUrl: String,
        githubEvidence: Map<String, Any?>?,
        liveUrl: String,
        liveCheck: Map<String, Any?>?,
        hasTasks: Boolean,
        level: Int
    ) = PrismScoring.ProjectScoreInput(
        status = status,
        progress = progress,
        techStackFilled = techStack.isNotBlank(),
        githubStatus = GithubEvidenceInput.githubStatus(githubUrl, githubEvidence),
        liveStatus = liveStatus(liveUrl, liveCheck),
        hasTasks = hasTasks,
        level = level
    )

    fun projectInput(doc: DocumentSnapshot): PrismScoring.ProjectScoreInput {
        val progress = number(doc.get("progress"))?.toInt()?.coerceIn(0, 100) ?: 0
        return projectInput(
            status = doc.getString("status").orEmpty(),
            progress = progress,
            techStack = doc.getString("techStack") ?: doc.getString("codingLanguage") ?: "",
            githubUrl = doc.getString("githubUrl").orEmpty(),
            githubEvidence = stringMap(doc.get("githubEvidence")),
            liveUrl = doc.getString("liveUrl").orEmpty(),
            liveCheck = stringMap(doc.get("liveCheck")),
            hasTasks = (doc.get("tasks") as? List<*>)?.isNotEmpty() == true,
            level = levelOf(doc.get("levelFinal"))
        )
    }

    fun internshipInput(doc: DocumentSnapshot): PrismScoring.InternshipScoreInput {
        fun text(key: String) = doc.get(key)?.toString().orEmpty()
        return PrismScoring.InternshipScoreInput(
            status = text("status"),
            detailsComplete = PrismScoring.internshipDetailsComplete(
                company = text("company"),
                role = text("role"),
                applicationDate = text("applicationDate"),
                jobUrl = text("jobUrl")
            ),
            level = levelOf(doc.get("levelFinal"))
        )
    }

    fun dsaPoints(doc: DocumentSnapshot): Double =
        PrismScoring.dsaPoints(
            storedScore = number(doc.get("score")),
            difficulty = doc.getString("difficulty").orEmpty(),
            topic = doc.getString("topic").orEmpty()
        )

    fun learningLevels(docs: List<DocumentSnapshot>): List<Double> =
        docs.mapNotNull { number(it.get("level")) }

    // --------------------------------------------------------
    // Loading
    // --------------------------------------------------------

    /** The 8 reads, in the order buildSummary expects. */
    fun readTasks(firestore: FirebaseFirestore, uid: String): List<com.google.android.gms.tasks.Task<*>> {
        val userRef = firestore.collection("users").document(uid)
        return listOf(
            userRef.get(),
            userRef.collection("dsaProblems").get(),
            userRef.collection("projects").get(),
            userRef.collection("internships").get(),
            userRef.collection("placementSkills").get(),
            userRef.collection("placementAchievements").get(),
            userRef.collection("placementCertifications").get(),
            userRef.collection("placementLearning").get()
        )
    }

    fun buildSummary(results: List<Any?>): PrismScoreSummary {
        val profile = results[0] as DocumentSnapshot
        val dsaDocs = (results[1] as QuerySnapshot).documents
        val projectDocs = (results[2] as QuerySnapshot).documents
        val internshipDocs = (results[3] as QuerySnapshot).documents
        val skillDocs = (results[4] as QuerySnapshot).documents
        val achievementDocs = (results[5] as QuerySnapshot).documents
        val certificationDocs = (results[6] as QuerySnapshot).documents
        val learningDocs = (results[7] as QuerySnapshot).documents

        val academics = PrismScoring.academicsScore(number(profile.get("currentCgpa")))
        val dsa = PrismScoring.dsaScore(dsaDocs.sumOf { dsaPoints(it) })
        val projects = PrismScoring.projectsScore(projectDocs.map { projectInput(it) })
        val internshipInputs = internshipDocs.map { internshipInput(it) }
        val internships = PrismScoring.internshipsScore(internshipInputs, projectDocs.size)
        val skills = PrismScoring.skillsScore(skillDocs.mapNotNull { number(it.get("rating")) })
        val learning = PrismScoring.learningScore(learningLevels(learningDocs), learningDocs.size)
        val evidence = PrismScoring.evidenceScore(
            achievements = achievementDocs.size,
            certifications = certificationDocs.size,
            learningScore = learning
        )

        return PrismScoreSummary(
            breakdown = PrismScoring.Breakdown(academics, dsa, projects, internships, skills, evidence),
            dsaSolved = dsaDocs.size,
            projectCount = projectDocs.size,
            completedProjects = projectDocs.count {
                it.getString("status").orEmpty().uppercase() == "COMPLETED"
            },
            internshipCount = internshipDocs.size,
            interviews = internshipInputs.count { it.isInterviewOrSelected },
            selected = internshipInputs.count { it.isSelected },
            achievementCount = achievementDocs.size,
            certificationCount = certificationDocs.size,
            learningScore = learning,
            githubProfileUrl = profile.getString("githubProfileUrl").orEmpty()
        )
    }

    fun load(
        firestore: FirebaseFirestore,
        uid: String,
        onSuccess: (PrismScoreSummary) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Tasks.whenAllSuccess<Any>(readTasks(firestore, uid))
            .addOnSuccessListener { results ->
                try {
                    onSuccess(buildSummary(results))
                } catch (exception: Exception) {
                    onError(exception)
                }
            }
            .addOnFailureListener { onError(it) }
    }
}
