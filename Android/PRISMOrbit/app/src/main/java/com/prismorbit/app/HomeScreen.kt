
package com.prismorbit.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlin.math.roundToInt


// =========================================================
// DATA MODEL
// =========================================================

data class DSAProblem(
    val name: String,
    val topic: String,
    val difficulty: String,
    val score: Float
)


data class AcademicRecord(
    val currentCgpa: String = "8.7",
    val targetCgpa: String = "9.0",
    val sem1: String = "8.4",
    val sem2: String = "8.7",
    val sem3: String = "",
    val sem4: String = "",
    val sem5: String = "",
    val sem6: String = "",
    val sem7: String = "",
    val sem8: String = ""
)

private fun AcademicRecord.toMap(): Map<String, Any> = mapOf(
    "currentCgpa" to currentCgpa,
    "targetCgpa" to targetCgpa,
    "sem1" to sem1,
    "sem2" to sem2,
    "sem3" to sem3,
    "sem4" to sem4,
    "sem5" to sem5,
    "sem6" to sem6,
    "sem7" to sem7,
    "sem8" to sem8
)

private fun loadAcademicRecord(document: com.google.firebase.firestore.DocumentSnapshot): AcademicRecord {
    fun value(key: String, fallback: String): String {
        val raw = document.get(key) ?: return fallback
        return raw.toString().trim().ifBlank { fallback }
    }

    return AcademicRecord(
        currentCgpa = value("currentCgpa", "8.7"),
        targetCgpa = value("targetCgpa", "9.0"),
        sem1 = value("sem1", "8.4"),
        sem2 = value("sem2", "8.7"),
        sem3 = value("sem3", ""),
        sem4 = value("sem4", ""),
        sem5 = value("sem5", ""),
        sem6 = value("sem6", ""),
        sem7 = value("sem7", ""),
        sem8 = value("sem8", "")
    )
}

private fun saveAcademicRecord(
    firestore: FirebaseFirestore,
    uid: String,
    record: AcademicRecord,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    firestore
        .collection("users")
        .document(uid)
        .set(record.toMap(), SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

// =========================================================
// DSA - FIRESTORE PERSISTENCE
// =========================================================

private class DsaDuplicateProblemException :
    Exception("This problem is already added.")

private fun DSAProblem.toMap(): Map<String, Any> = mapOf(
    "name" to name,
    "topic" to topic,
    "difficulty" to difficulty,
    "score" to score.toDouble()
)

private fun dsaProblemDocumentId(problemName: String): String {
    val normalized = problemName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    return normalized.ifBlank { "problem" }
}

private fun documentToDsaProblem(
    document: com.google.firebase.firestore.DocumentSnapshot
): DSAProblem? {
    val name = document.getString("name")?.trim().orEmpty()
    if (name.isBlank()) return null

    val topic = document.getString("topic")?.trim().orEmpty()
    val difficulty = document.getString("difficulty")?.trim().orEmpty()
    val score = document.getDouble("score")?.toFloat()
        ?: document.getLong("score")?.toFloat()
        ?: calculateProblemScore(difficulty, topic)

    return DSAProblem(
        name = name,
        topic = topic.ifBlank { "Other" },
        difficulty = difficulty.ifBlank { "Easy" },
        score = score
    )
}

private fun loadDsaProblems(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (List<DSAProblem>) -> Unit,
    onError: (Exception) -> Unit
) {
    firestore
        .collection("users")
        .document(uid)
        .collection("dsaProblems")
        .get()
        .addOnSuccessListener { snapshot ->
            val problems = snapshot.documents
                .mapNotNull { documentToDsaProblem(it) }
                .sortedBy { it.name.lowercase() }

            onSuccess(problems)
        }
        .addOnFailureListener { error ->
            onError(error)
        }
}

private fun saveDsaProblem(
    firestore: FirebaseFirestore,
    uid: String,
    problem: DSAProblem,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    val problemRef = firestore
        .collection("users")
        .document(uid)
        .collection("dsaProblems")
        .document(dsaProblemDocumentId(problem.name))

    firestore.runTransaction { transaction ->
        val existing = transaction.get(problemRef)

        if (existing.exists()) {
            throw DsaDuplicateProblemException()
        }

        transaction.set(problemRef, problem.toMap())
    }.addOnSuccessListener {
        onSuccess()
    }.addOnFailureListener { error ->
        onError(error)
    }
}

private fun deleteDsaProblem(
    firestore: FirebaseFirestore,
    uid: String,
    problem: DSAProblem,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    firestore
        .collection("users")
        .document(uid)
        .collection("dsaProblems")
        .document(dsaProblemDocumentId(problem.name))
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

// =========================================================
// ACADEMIC EVENTS - FIRESTORE PERSISTENCE
// =========================================================

private fun AcademicEvent.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "title" to title,
    "subject" to subject,
    "type" to type,
    "date" to date,
    "day" to day,
    "syllabus" to syllabus,
    "notes" to notes,
    "status" to status
)

private fun documentToAcademicEvent(
    document: com.google.firebase.firestore.DocumentSnapshot
): AcademicEvent? {
    val id = document.getLong("id")
        ?: document.id.toLongOrNull()
        ?: return null

    return AcademicEvent(
        id = id,
        title = document.getString("title") ?: "",
        subject = document.getString("subject") ?: "",
        type = document.getString("type") ?: "Other",
        date = document.getString("date") ?: "",
        day = document.getString("day") ?: "",
        syllabus = document.getString("syllabus") ?: "",
        notes = document.getString("notes") ?: "",
        status = document.getString("status") ?: "UPCOMING"
    )
}

private fun loadAcademicEvents(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (List<AcademicEvent>) -> Unit,
    onError: (Exception) -> Unit
) {
    firestore
        .collection("users")
        .document(uid)
        .collection("academicEvents")
        .get()
        .addOnSuccessListener { snapshot ->
            val events = snapshot.documents
                .mapNotNull { documentToAcademicEvent(it) }
                .sortedBy { it.date }

            onSuccess(events)
        }
        .addOnFailureListener { error ->
            onError(error)
        }
}

private fun saveAcademicEvent(
    firestore: FirebaseFirestore,
    uid: String,
    event: AcademicEvent,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    firestore
        .collection("users")
        .document(uid)
        .collection("academicEvents")
        .document(event.id.toString())
        .set(event.toMap())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

private fun deleteAcademicEvent(
    firestore: FirebaseFirestore,
    uid: String,
    eventId: Long,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    firestore
        .collection("users")
        .document(uid)
        .collection("academicEvents")
        .document(eventId.toString())
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

data class AcademicEvent(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val subject: String,
    val type: String,
    val date: String,
    val day: String,
    val syllabus: String,
    val notes: String,
    val status: String = "UPCOMING"
)

// =========================================================
// PROJECTS
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
    val photos: List<String> = emptyList()
)

private fun ProjectItem.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name,
    "description" to description,
    "techStack" to techStack,
    "startDate" to startDate,
    "status" to status,
    "githubUrl" to githubUrl,
    "liveUrl" to liveUrl,
    "progress" to progress,
    "tasks" to tasks.map { task ->
        mapOf(
            "id" to task.id,
            "title" to task.title,
            "completed" to task.completed
        )
    },
    "photos" to photos
)


private fun documentToProjectItem(
    document: com.google.firebase.firestore.DocumentSnapshot
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

        photos = photos
    )
}


private fun loadProjects(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (List<ProjectItem>) -> Unit,
    onError: (Exception) -> Unit
) {

    firestore
        .collection("users")
        .document(uid)
        .collection("projects")
        .get()
        .addOnSuccessListener { snapshot ->

            val loadedProjects: List<ProjectItem> =
                snapshot.documents
                    .mapNotNull { document ->
                        documentToProjectItem(document)
                    }
                    .sortedBy {
                        it.name.lowercase()
                    }

            onSuccess(loadedProjects)
        }
        .addOnFailureListener { exception ->

            onError(exception)
        }
}


private fun saveProject(
    firestore: FirebaseFirestore,
    uid: String,
    project: ProjectItem,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {

    firestore
        .collection("users")
        .document(uid)
        .collection("projects")
        .document(
            project.firestoreId.ifBlank {
                project.id.toString()
            }
        )
        .set(project.toMap())
        .addOnSuccessListener {

            onSuccess()
        }
        .addOnFailureListener { exception ->

            onError(exception)
        }
}


private fun deleteProject(
    firestore: FirebaseFirestore,
    uid: String,
    projectId: Long,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {

    val collection =
        firestore
            .collection("users")
            .document(uid)
            .collection("projects")

    collection
        .get()
        .addOnSuccessListener { snapshot ->

            val matchingDocument =
                snapshot.documents.firstOrNull { document ->

                    val rawId =
                        document.get("id")

                    when (rawId) {
                        is Number ->
                            rawId.toLong() == projectId

                        is String ->
                            rawId.toLongOrNull() == projectId

                        else ->
                            false
                    } ||
                            document.id == projectId.toString() ||
                            document.id.hashCode().toLong() == projectId
                }

            if (matchingDocument == null) {
                onError(
                    Exception("Project record could not be found in Firestore.")
                )
                return@addOnSuccessListener
            }

            matchingDocument.reference
                .delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        }
        .addOnFailureListener { exception ->
            onError(exception)
        }
}

// =========================================================
// HOME SCREEN
// =========================================================

private fun displayInitials(fullName: String): String {
    val parts = fullName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first().first().uppercaseChar().toString()
        else -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit = {}) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val signedInUser = auth.currentUser

    var showCgpaScreen by remember { mutableStateOf(false) }
    var showAcademicsScreen by remember { mutableStateOf(false) }
    var returnToAcademics by remember { mutableStateOf(false) }
    var showDsaScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showProjectsScreen by remember { mutableStateOf(false) }
    var showInternshipsScreen by remember { mutableStateOf(false) }
    var showPlacementScreen by remember { mutableStateOf(false) }
    var showGrowthScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var userFirstName by remember { mutableStateOf("") }

    var internshipCount by remember { mutableStateOf(0) }
    var placementReadiness by remember { mutableStateOf<Int?>(null) }
    var growthScore by remember { mutableStateOf<Int?>(null) }
    var showSmartAiChatScreen by remember { mutableStateOf(false) }

// =====================================================
// SMART AI STATE
// =====================================================

    var smartAiInsight by remember {
        mutableStateOf<SmartAiInsight?>(null)
    }

    var smartAiLoading by remember {
        mutableStateOf(true)
    }

    var smartAiError by remember {
        mutableStateOf("")
    }

    var academicRecord by remember { mutableStateOf(AcademicRecord()) }
    var academicLoadError by remember { mutableStateOf("") }

    val academicEvents = remember { mutableStateListOf<AcademicEvent>() }

    // DSA is now user-specific Firestore data. No sample/hardcoded problems.
    val dsaProblems = remember { mutableStateListOf<DSAProblem>() }
    val projects = remember { mutableStateListOf<ProjectItem>() }
    var dsaLoading by remember { mutableStateOf(false) }
    var dsaLoadError by remember { mutableStateOf("") }

    LaunchedEffect(signedInUser?.uid) {
        val uid = signedInUser?.uid
        if (uid != null) {
            firestore
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    academicRecord = loadAcademicRecord(document)
                    val fullName = document
                        .getString("name")
                        ?.trim()
                        .orEmpty()

                    userFirstName = fullName
                        .split(Regex("\\s+"))
                        .firstOrNull()
                        .orEmpty()
                    academicLoadError = ""
                }
                .addOnFailureListener { error ->
                    academicLoadError = error.message ?: "Unable to load academic data."
                }
        }
    }

    LaunchedEffect(signedInUser?.uid) {
        val uid = signedInUser?.uid ?: return@LaunchedEffect

        loadAcademicEvents(
            firestore = firestore,
            uid = uid,
            onSuccess = { loadedEvents ->
                academicEvents.clear()
                academicEvents.addAll(loadedEvents)
                academicLoadError = ""
            },
            onError = { error ->
                academicLoadError =
                    error.message ?: "Unable to load academic events."
            }
        )
    }

    LaunchedEffect(signedInUser?.uid) {
        val uid = signedInUser?.uid ?: return@LaunchedEffect

        dsaLoading = true
        dsaLoadError = ""
        dsaProblems.clear()

        loadDsaProblems(
            firestore = firestore,
            uid = uid,
            onSuccess = { loadedProblems ->
                dsaProblems.clear()
                dsaProblems.addAll(loadedProblems)
                dsaLoading = false
                dsaLoadError = ""
            },
            onError = { error ->
                dsaLoading = false
                dsaLoadError = error.message ?: "Unable to load DSA problems."
            }
        )
    }

    LaunchedEffect(signedInUser?.uid) {
        val uid = signedInUser?.uid ?: return@LaunchedEffect
        loadProjects(
            firestore = firestore,
            uid = uid,
            onSuccess = { loaded ->
                projects.clear()
                projects.addAll(loaded)
            },
            onError = { }
        )
    }

    LaunchedEffect(signedInUser?.uid) {
        val uid = signedInUser?.uid ?: return@LaunchedEffect

        loadGrowthDashboardSummary(
            firestore = firestore,
            uid = uid,
            onSuccess = { summary ->
                growthScore = summary.growthScore
                placementReadiness = summary.placementScore
                internshipCount = summary.internshipCount
            },
            onError = {
                growthScore = null
                placementReadiness = null
                internshipCount = 0
            }
        )
    }

    // =====================================================
// SMART AI DATA LOADER
// =====================================================

    LaunchedEffect(signedInUser?.uid) {

        val uid =
            signedInUser?.uid
                ?: return@LaunchedEffect

        smartAiLoading = true
        smartAiError = ""

        loadSmartAiInsight(
            firestore = firestore,
            uid = uid,

            onSuccess = { insight ->

                smartAiInsight = insight
                smartAiLoading = false
                smartAiError = ""
            },

            onError = { error ->

                smartAiLoading = false

                smartAiError =
                    error.message
                        ?: "Unable to generate SMART AI insight."
            }
        )
    }

    fun persistAcademicRecord(
        updated: AcademicRecord,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = signedInUser?.uid
        if (uid == null) {
            onError("No signed-in user found.")
            return
        }

        saveAcademicRecord(
            firestore = firestore,
            uid = uid,
            record = updated,
            onSuccess = {
                academicRecord = updated
                academicLoadError = ""
                onSuccess()
            },
            onError = { error ->
                academicLoadError = error.message ?: "Unable to save academic data."
                onError(academicLoadError)
            }
        )
    }

    if (showSettingsScreen) {
        SettingsScreen(
            onBack = { showSettingsScreen = false },
            onLogout = onLogout
        )
    } else if (showProfileScreen) {
        ProfileScreen(
            onBack = {
                showProfileScreen = false

                val uid = signedInUser?.uid
                if (uid != null) {
                    firestore
                        .collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            val fullName = document
                                .getString("name")
                                ?.trim()
                                .orEmpty()

                            userFirstName = fullName
                                .split(Regex("\\s+"))
                                .firstOrNull()
                                .orEmpty()
                        }
                }
            }
        )
    } else if (showInternshipsScreen) {
        InternshipTrackerScreen(
            projectCount = projects.size,
            onBack = { showInternshipsScreen = false },
            onCountChanged = { count ->
                internshipCount = count
            }
        )
    } else if (showPlacementScreen) {
        PlacementTrackerScreen(
            onBack = { showPlacementScreen = false },
            onReadinessChanged = { }
        )
    } else if (showProjectsScreen) {
        ProjectsScreen(
            projects = projects,
            onBack = { showProjectsScreen = false },
            onAddProject = { project ->
                val uid = signedInUser?.uid
                if (uid != null) {
                    saveProject(firestore, uid, project,
                        onSuccess = { projects.add(project) },
                        onError = { }
                    )
                }
            },
            onUpdateProject = { updated ->
                val uid = signedInUser?.uid
                if (uid != null) {
                    saveProject(firestore, uid, updated,
                        onSuccess = {
                            val index = projects.indexOfFirst { it.id == updated.id }
                            if (index >= 0) projects[index] = updated else projects.add(updated)
                        },
                        onError = { }
                    )
                }
            },
            onDeleteProject = { id ->
                val uid = signedInUser?.uid
                if (uid != null) {
                    deleteProject(firestore, uid, id,
                        onSuccess = { projects.removeAll { it.id == id } },
                        onError = { }
                    )
                }
            }
        )
    } else if (showAcademicsScreen) {
        AcademicsScreen(
            currentCgpa = academicRecord.currentCgpa.toFloatOrNull() ?: 0f,
            targetCgpa = academicRecord.targetCgpa.toFloatOrNull() ?: 0f,
            semesterValues = listOf(
                academicRecord.sem1, academicRecord.sem2, academicRecord.sem3, academicRecord.sem4,
                academicRecord.sem5, academicRecord.sem6, academicRecord.sem7, academicRecord.sem8
            ),
            events = academicEvents,
            onBack = { showAcademicsScreen = false },
            onCgpaClick = {
                showAcademicsScreen = false
                showCgpaScreen = true
                returnToAcademics = true
            },
            onSaveSemesters = { values ->
                persistAcademicRecord(
                    academicRecord.copy(
                        sem1 = values.getOrElse(0) { "" },
                        sem2 = values.getOrElse(1) { "" },
                        sem3 = values.getOrElse(2) { "" },
                        sem4 = values.getOrElse(3) { "" },
                        sem5 = values.getOrElse(4) { "" },
                        sem6 = values.getOrElse(5) { "" },
                        sem7 = values.getOrElse(6) { "" },
                        sem8 = values.getOrElse(7) { "" }
                    )
                )
            },
            onAddEvent = { event ->
                val uid = signedInUser?.uid

                if (uid == null) {
                    academicLoadError = "No signed-in user found."
                } else {
                    saveAcademicEvent(
                        firestore = firestore,
                        uid = uid,
                        event = event,
                        onSuccess = {
                            academicEvents.removeAll { it.id == event.id }
                            academicEvents.add(event)
                            academicLoadError = ""
                        },
                        onError = { error ->
                            academicLoadError =
                                error.message ?: "Unable to save academic event."
                        }
                    )
                }
            },

            onUpdateEvent = { updated ->
                val uid = signedInUser?.uid

                if (uid == null) {
                    academicLoadError = "No signed-in user found."
                } else {
                    saveAcademicEvent(
                        firestore = firestore,
                        uid = uid,
                        event = updated,
                        onSuccess = {
                            val index =
                                academicEvents.indexOfFirst { it.id == updated.id }

                            if (index >= 0) {
                                academicEvents[index] = updated
                            } else {
                                academicEvents.add(updated)
                            }

                            academicLoadError = ""
                        },
                        onError = { error ->
                            academicLoadError =
                                error.message ?: "Unable to update academic event."
                        }
                    )
                }
            },

            onDeleteEvent = { id ->
                val uid = signedInUser?.uid

                if (uid == null) {
                    academicLoadError = "No signed-in user found."
                } else {
                    deleteAcademicEvent(
                        firestore = firestore,
                        uid = uid,
                        eventId = id,
                        onSuccess = {
                            academicEvents.removeAll { it.id == id }
                            academicLoadError = ""
                        },
                        onError = { error ->
                            academicLoadError =
                                error.message ?: "Unable to delete academic event."
                        }
                    )
                }
            },
            onCompleteEvent = { id ->
                val uid = signedInUser?.uid
                val index = academicEvents.indexOfFirst { it.id == id }

                if (uid == null) {
                    academicLoadError = "No signed-in user found."
                } else if (index >= 0) {
                    val updated = academicEvents[index].copy(status = "COMPLETED")

                    saveAcademicEvent(
                        firestore = firestore,
                        uid = uid,
                        event = updated,
                        onSuccess = {
                            academicEvents[index] = updated
                            academicLoadError = ""
                        },
                        onError = { error ->
                            academicLoadError =
                                error.message ?: "Unable to complete academic event."
                        }
                    )
                }
            },
            onCancelEvent = { id ->
                val uid = signedInUser?.uid
                val index = academicEvents.indexOfFirst { it.id == id }

                if (uid == null) {
                    academicLoadError = "No signed-in user found."
                } else if (index >= 0) {
                    val updated = academicEvents[index].copy(status = "CANCELLED")

                    saveAcademicEvent(
                        firestore = firestore,
                        uid = uid,
                        event = updated,
                        onSuccess = {
                            academicEvents[index] = updated
                            academicLoadError = ""
                        },
                        onError = { error ->
                            academicLoadError =
                                error.message ?: "Unable to cancel academic event."
                        }
                    )
                }
            }
        )
    } else if (showCgpaScreen) {
        CGPAScreen(
            initialCurrentCgpa = academicRecord.currentCgpa,
            initialTargetCgpa = academicRecord.targetCgpa,
            saveError = academicLoadError,
            onSave = { current, target, result ->
                persistAcademicRecord(
                    updated = academicRecord.copy(
                        currentCgpa = current,
                        targetCgpa = target
                    ),
                    onSuccess = { result(true, "") },
                    onError = { message -> result(false, message) }
                )
            },
            onBack = {
                showCgpaScreen = false
                if (returnToAcademics) {
                    showAcademicsScreen = true
                    returnToAcademics = false
                }
            }
        )
    } else if (showDsaScreen) {
        DSAScreen(
            problems = dsaProblems,
            isLoading = dsaLoading,
            errorMessage = dsaLoadError,
            onBack = { showDsaScreen = false },
            onAddProblem = { problem, result ->
                val uid = signedInUser?.uid

                if (uid == null) {
                    val message = "No signed-in user found."
                    dsaLoadError = message
                    result(false, message)
                } else {
                    saveDsaProblem(
                        firestore = firestore,
                        uid = uid,
                        problem = problem,
                        onSuccess = {
                            dsaProblems.add(problem)
                            dsaProblems.sortBy { it.name.lowercase() }
                            dsaLoadError = ""
                            result(true, "")
                        },
                        onError = { error ->
                            val message = error.message ?: "Unable to save DSA problem."
                            dsaLoadError = message
                            result(false, message)
                        }
                    )
                }
            },
            onDeleteProblem = { problem ->
                val uid = signedInUser?.uid

                if (uid == null) {
                    dsaLoadError = "No signed-in user found."
                } else {
                    deleteDsaProblem(
                        firestore = firestore,
                        uid = uid,
                        problem = problem,
                        onSuccess = {
                            dsaProblems.removeAll { it.name.equals(problem.name, ignoreCase = true) }
                            dsaLoadError = ""
                        },
                        onError = { error ->
                            dsaLoadError = error.message ?: "Unable to delete DSA problem."
                        }
                    )
                }
            }
        )
    } else if (showGrowthScreen) {
        GrowthTrackerScreen(
            onBack = { showGrowthScreen = false },
            onGrowthChanged = { score ->
                growthScore = score
            }
        )

    }
    else if (showSmartAiChatScreen) {
        SmartAiChatScreen(
            onBack = { showSmartAiChatScreen = false }
        )
    }
    else {
        DashboardScreen(
            dsaProblems = dsaProblems,
            currentCgpa = academicRecord.currentCgpa,
            onCgpaClick = { showAcademicsScreen = true },
            onDsaClick = {
                dsaLoadError = ""
                showDsaScreen = true
            },
            onProfileClick = { showProfileScreen = true },
            userFirstName = userFirstName,
            projectCount = projects.size,
            onProjectsClick = { showProjectsScreen = true },
            internshipCount = internshipCount,
            onInternshipsClick = { showInternshipsScreen = true },
            onPlacementClick = { showPlacementScreen = true },
            growthScore = growthScore,
            placementReadiness = placementReadiness,
            onGrowthClick = { showGrowthScreen = true },
            onSettingsClick = {
                showSettingsScreen = true
            },

            smartAiInsight = smartAiInsight,
            smartAiLoading = smartAiLoading,
            smartAiError = smartAiError,
            onSmartAiChatClick = { showSmartAiChatScreen = true }
        )
    }
}


// =========================================================
// DASHBOARD
// =========================================================

@Composable
private fun DashboardScreen(
    dsaProblems: List<DSAProblem>,
    currentCgpa: String,
    onCgpaClick: () -> Unit,
    onDsaClick: () -> Unit,
    onProfileClick: () -> Unit,
    userFirstName: String,
    projectCount: Int,
    onProjectsClick: () -> Unit,
    internshipCount: Int,
    onInternshipsClick: () -> Unit,
    onPlacementClick: () -> Unit,
    growthScore: Int?,
    placementReadiness: Int?,
    onGrowthClick: () -> Unit,
    onSettingsClick: () -> Unit,
    smartAiInsight: SmartAiInsight?,
    smartAiLoading: Boolean,
    smartAiError: String,
    onSmartAiChatClick: () -> Unit
) {

    val dsaScore = calculateDsaProgress(dsaProblems)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 24.dp
            )
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "PRISM",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 5.sp
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "SEE BEYOND THE GRINDS",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    letterSpacing = 2.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onProfileClick() }
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFB76CFF),
                                Color(0xFF00D9FF)
                            )
                        ),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = displayInitials(userFirstName).takeIf { it.isNotEmpty() } ?: "?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // =====================================================
        // GREETING
        // =====================================================

        Text(
            text = if (userFirstName.isBlank()) {
                "Welcome back."
            } else {
                "Welcome back, $userFirstName."
            },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Your journey is taking shape.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MY PROFILE",
                        color = Color(0xFFB76CFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "View and edit your personal information",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "›",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =====================================================
        // YOUR PRISM
        // =====================================================

        Text(
            text = "YOUR PRISM",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(
            modifier = Modifier.height(13.dp)
        )

        // =====================================================
        // CGPA + DSA
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onCgpaClick()
                    },
                title = "CGPA",
                value = currentCgpa,
                subtitle = "Academics",
                accent = Color(0xFFB76CFF)
            )

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onDsaClick()
                    },
                title = "DSA",
                value = "${dsaScore.roundToInt()}%",
                subtitle = "Tap to explore",
                accent = Color(0xFF00D9FF)
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // =====================================================
        // PROJECTS + INTERNSHIPS
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProjectsClick() },
                title = "PROJECTS",
                value = projectCount.toString(),
                subtitle = "Tap to explore",
                accent = Color(0xFF65E572)
            )

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInternshipsClick() },
                title = "INTERNSHIPS",
                value = internshipCount.toString(),
                subtitle = "Experience",
                accent = Color(0xFFFFD23F)
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // =====================================================
        // PLACEMENT + GROWTH
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlacementClick() },
                title = "PLACEMENT",
                value = placementReadiness?.let { "$it%" } ?: "—",
                subtitle = "Readiness",
                accent = Color(0xFFFF7B72)
            )

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onGrowthClick() },
                title = "GROWTH",
                value = growthScore?.let { "$it%" } ?: "Tap",
                subtitle = "Development",
                accent = Color(0xFFFF4FD8)
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // =====================================================
        // SMART AI
        // =====================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSmartAiChatClick() },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "✦  SMART AI",
                    color = Color(0xFFB76CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(
                    modifier = Modifier.height(13.dp)
                )

                when {
                    smartAiLoading -> {
                        Text(
                            text = "Analyzing your PRISM data...",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = "Checking your academics, DSA, projects, internships and placement progress.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    smartAiError.isNotBlank() -> {
                        Text(
                            text = "SMART AI is temporarily unavailable.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = smartAiError,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    smartAiInsight != null -> {
                        Text(
                            text = smartAiInsight!!.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        Text(
                            text = smartAiInsight!!.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )

                        if (smartAiInsight!!.reason.isNotBlank()) {
                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = smartAiInsight!!.reason,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(13.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = "NEXT ACTION",
                                    color = Color(0xFF00D9FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = smartAiInsight!!.action,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(11.dp)
                        )

                        Text(
                            text = "${smartAiInsight!!.category} • PRIORITY ${smartAiInsight!!.priority}/100",
                            color = Color(0xFFB76CFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    else -> {
                        Text(
                            text = "SMART AI insight is not available yet.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================
        // SETTINGS
        // =====================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsClick() },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "⚙  SETTINGS",
                        color = Color(0xFFB76CFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Account, preferences & security",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "›",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )
    }
}


// =========================================================
// ACADEMICS SCREEN
// =========================================================

@Composable
private fun AcademicsScreen(
    currentCgpa: Float,
    targetCgpa: Float,
    semesterValues: List<String>,
    events: List<AcademicEvent>,
    onBack: () -> Unit,
    onCgpaClick: () -> Unit,
    onSaveSemesters: (List<String>) -> Unit,
    onAddEvent: (AcademicEvent) -> Unit,
    onUpdateEvent: (AcademicEvent) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onCompleteEvent: (Long) -> Unit,
    onCancelEvent: (Long) -> Unit
) {
    var showAddEvent by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AcademicEvent?>(null) }
    var editSemesters by remember { mutableStateOf(false) }
    var semesterDrafts by remember(semesterValues) {
        mutableStateOf(semesterValues.toMutableList())
    }
    var semesterError by remember { mutableStateOf("") }

    val semesterNames = listOf(
        "SEM 1", "SEM 2", "SEM 3", "SEM 4",
        "SEM 5", "SEM 6", "SEM 7", "SEM 8"
    )

    val completedSemesters = semesterValues.count { it.isNotBlank() }
    val cgpaProgress = (currentCgpa / 10f).coerceIn(0f, 1f)
    val targetGap = (targetCgpa - currentCgpa).coerceAtLeast(0f)
    val upcomingEvents = events.count {
        eventStatus(it) == "UPCOMING" || eventStatus(it) == "TODAY"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACADEMICS",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PERFORMANCE + PLANNING",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1D1730))
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$completedSemesters/8",
                    color = Color(0xFFB76CFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // =====================================================
        // ACADEMIC HERO
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(21.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "ACADEMIC OVERVIEW",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "%.1f".format(currentCgpa),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CURRENT CGPA",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TARGET",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.1f".format(targetCgpa),
                            color = Color(0xFF00D9FF),
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (targetGap > 0f) {
                                "${"%.1f".format(targetGap)} TO GO"
                            } else {
                                "TARGET ACHIEVED"
                            },
                            color = if (targetGap > 0f) {
                                Color(0xFFB76CFF)
                            } else {
                                Color(0xFF65E572)
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(17.dp))

                ProgressBar(progress = cgpaProgress)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0.0",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp
                    )
                    Text(
                        text = "10.0",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp
                    )
                }

                Spacer(modifier = Modifier.height(17.dp))

                Button(
                    onClick = onCgpaClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B4DFF)
                    ),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text(
                        text = "VIEW / EDIT CGPA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // =====================================================
        // QUICK STATS
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AcademicMiniStat(
                modifier = Modifier.weight(1f),
                label = "SEMESTERS",
                value = "$completedSemesters / 8",
                accent = Color(0xFFB76CFF)
            )
            AcademicMiniStat(
                modifier = Modifier.weight(1f),
                label = "EVENTS",
                value = upcomingEvents.toString(),
                accent = Color(0xFF00D9FF)
            )
            AcademicMiniStat(
                modifier = Modifier.weight(1f),
                label = "TARGET",
                value = "%.1f".format(targetCgpa),
                accent = Color(0xFF65E572)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // =====================================================
        // SEMESTER TRACKER
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionTitle(text = "SEMESTER TRACKER")
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Keep every semester in one place.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            OutlinedButton(
                onClick = {
                    if (!editSemesters) {
                        semesterDrafts = semesterValues.toMutableList()
                    }
                    semesterError = ""
                    editSemesters = !editSemesters
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (editSemesters) "CANCEL" else "EDIT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (editSemesters) {
            semesterNames.forEachIndexed { index, name ->
                OutlinedTextField(
                    value = semesterDrafts.getOrElse(index) { "" },
                    onValueChange = { value ->
                        val cleaned = value.filter { it.isDigit() || it == '.' }
                        val updated = semesterDrafts.toMutableList()
                        while (updated.size < 8) updated.add("")
                        updated[index] = cleaned
                        semesterDrafts = updated
                        semesterError = ""
                    },
                    label = { Text("$name SGPA") },
                    placeholder = {
                        Text(
                            if (index < 2) {
                                "e.g. 8.4"
                            } else {
                                "Leave blank until completed"
                            }
                        )
                    },
                    supportingText = {
                        Text("0.0 – 10.0; blank = not completed")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp),
                    singleLine = true
                )
            }

            if (semesterError.isNotEmpty()) {
                Text(
                    text = semesterError,
                    color = Color(0xFFFF6B6B),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val valid = semesterDrafts.all {
                        it.isBlank() ||
                                (it.toFloatOrNull()?.let { value ->
                                    value in 0f..10f
                                } == true)
                    }

                    if (!valid) {
                        semesterError = "Each SGPA must be between 0.0 and 10.0."
                    } else {
                        onSaveSemesters(semesterDrafts.map { it.trim() })
                        semesterError = ""
                        editSemesters = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B4DFF)
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    text = "SAVE SEMESTERS",
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val firstIndex = row * 2
                    val secondIndex = firstIndex + 1

                    SemesterRow(
                        name = semesterNames[firstIndex],
                        sgpa = semesterValues.getOrElse(firstIndex) { "" }.trim(),
                        completed = semesterValues.getOrElse(firstIndex) { "" }.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )

                    SemesterRow(
                        name = semesterNames[secondIndex],
                        sgpa = semesterValues.getOrElse(secondIndex) { "" }.trim(),
                        completed = semesterValues.getOrElse(secondIndex) { "" }.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // =====================================================
        // SEMESTER GROWTH
        // =====================================================

        Column {
            SectionTitle(text = "SEMESTER GROWTH")
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Your SGPA trend across completed semesters.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SGPA TREND",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = if (completedSemesters > 0) {
                            "$completedSemesters CHECKPOINTS"
                        } else {
                            "NO DATA YET"
                        },
                        color = Color(0xFF00D9FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                AcademicGrowthGraph(semesterValues)
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // =====================================================
        // ACADEMIC PLANNER
        // =====================================================

        Column {
            SectionTitle(text = "ACADEMIC PLANNER")
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Plan tests, vivas, practicals and exams.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AcademicCalendar(
            events = events,
            onAddEvent = { showAddEvent = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // =====================================================
        // ACADEMIC EVENTS
        // =====================================================

        if (events.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    SectionTitle(text = "ACADEMIC EVENTS")
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Your saved academic timeline.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                Text(
                    text = "${events.size} TOTAL",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            events
                .sortedWith(
                    compareBy<AcademicEvent> {
                        eventStatus(it) == "EXPIRED" || eventStatus(it) == "CANCELLED"
                    }.thenBy { it.date }
                )
                .take(20)
                .forEach { event ->
                    AcademicEventRow(
                        event = event,
                        onEdit = { editingEvent = it },
                        onDelete = { onDeleteEvent(it) },
                        onComplete = { onCompleteEvent(it) },
                        onCancel = { onCancelEvent(it) }
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(19.dp)) {
                    Text(
                        text = "NO ACADEMIC EVENTS",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.7.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add assignments, tests, viva, practicals and exams to keep your semester organised.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Use + ADD ACADEMIC EVENT above to get started.",
                        color = Color(0xFF00D9FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))
    }

    if (showAddEvent) {
        AddAcademicEventScreen(
            initialEvent = null,
            onBack = { showAddEvent = false },
            onSave = {
                onAddEvent(it)
                showAddEvent = false
            }
        )
    }

    editingEvent?.let { event ->
        AddAcademicEventScreen(
            initialEvent = event,
            onBack = { editingEvent = null },
            onSave = {
                onUpdateEvent(it)
                editingEvent = null
            }
        )
    }
}

@Composable
private fun AcademicMiniStat(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SemesterRow(
    name: String,
    sgpa: String,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (completed) sgpa else "—",
                color = if (completed) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color(0xFF66666F)
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (completed) "COMPLETED" else "NOT COMPLETED",
                color = if (completed) {
                    Color(0xFF65E572)
                } else {
                    Color(0xFF66666F)
                },
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun AcademicGrowthGraph(semesterValues: List<String>) {
    val parsed = semesterValues.mapNotNull {
        it.toFloatOrNull()?.takeIf { value -> value in 0f..10f }
    }

    val points = when {
        parsed.size >= 2 -> parsed
        parsed.size == 1 -> listOf(parsed.first(), parsed.first())
        else -> listOf(8f, 8f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val max = 10f
        val min = 0f
        val step = size.width / (points.size - 1).coerceAtLeast(1)

        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(
                color = Color(0xFF202027),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val offsets = points.mapIndexed { index, value ->
            Offset(
                x = index * step,
                y = size.height -
                        ((value - min) / (max - min)).coerceIn(0f, 1f) * size.height
            )
        }

        for (i in 0 until offsets.size - 1) {
            drawLine(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFB76CFF),
                        Color(0xFF00D9FF)
                    )
                ),
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        offsets.forEach {
            drawCircle(
                color = Color(0xFF00D9FF),
                radius = 5.dp.toPx(),
                center = it
            )
        }
    }
}

@Composable
private fun AcademicCalendar(
    events: List<AcademicEvent>,
    onAddEvent: () -> Unit
) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val today = calendar.get(Calendar.DAY_OF_MONTH)

    val first = Calendar.getInstance().apply {
        set(year, month, 1)
    }

    val firstDay = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val eventDays = events
        .mapNotNull { it.date.substringAfterLast('-').toIntOrNull() }
        .toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = first.getDisplayName(
                            Calendar.MONTH,
                            Calendar.LONG,
                            java.util.Locale.getDefault()
                        ).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = year.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1D1730))
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "${events.size} EVENTS",
                        color = Color(0xFF00D9FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(
                        text = it,
                        color = Color(0xFF66666F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val totalCells = ((firstDay + daysInMonth + 6) / 7) * 7

            for (weekStart in 0 until totalCells step 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (cell in weekStart until weekStart + 7) {
                        val day = cell - firstDay + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day in 1..daysInMonth) {
                                val isToday = day == today
                                val hasEvent = day in eventDays

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(
                                            color = when {
                                                isToday -> Color(0xFF8B4DFF)
                                                hasEvent -> Color(0xFF1D1730)
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isToday || hasEvent) {
                                            Color.White
                                        } else {
                                            Color(0xFFAAAAAF)
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = if (isToday || hasEvent) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B4DFF)
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    text = "+  ADD ACADEMIC EVENT",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AcademicEventRow(
    event: AcademicEvent,
    onEdit: (AcademicEvent) -> Unit,
    onDelete: (Long) -> Unit,
    onComplete: (Long) -> Unit,
    onCancel: (Long) -> Unit
) {
    val status = eventStatus(event)

    val statusColor = when (status) {
        "COMPLETED" -> Color(0xFF65E572)
        "CANCELLED" -> Color(0xFFFF7B72)
        "EXPIRED" -> Color(0xFFFF7B72)
        "TODAY" -> Color(0xFFFFD23F)
        else -> Color(0xFF00D9FF)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "${event.subject}  •  ${event.date}  •  ${event.day}",
                        color = Color(0xFF00D9FF),
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = status,
                        color = statusColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = event.type.uppercase(),
                color = Color(0xFFB76CFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (event.syllabus.isNotBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "SYLLABUS  •  ${event.syllabus}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }

            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.notes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(event) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text("EDIT", fontSize = 8.sp)
                }

                OutlinedButton(
                    onClick = { onDelete(event.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text("DELETE", fontSize = 8.sp)
                }
            }

            if (status != "COMPLETED" &&
                status != "CANCELLED" &&
                status != "EXPIRED"
            ) {
                Spacer(modifier = Modifier.height(7.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Button(
                        onClick = { onComplete(event.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF17351F)
                        ),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Text(
                            text = "✓ COMPLETE",
                            fontSize = 8.sp,
                            color = Color(0xFF65E572)
                        )
                    }

                    OutlinedButton(
                        onClick = { onCancel(event.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Text("CANCEL", fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAcademicEventScreen(
    initialEvent: AcademicEvent?,
    onBack: () -> Unit,
    onSave: (AcademicEvent) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var subject by remember { mutableStateOf(initialEvent?.subject ?: "") }
    var type by remember { mutableStateOf(initialEvent?.type ?: "Assignment") }
    var date by remember { mutableStateOf(initialEvent?.date ?: "") }
    var syllabus by remember { mutableStateOf(initialEvent?.syllabus ?: "") }
    var notes by remember { mutableStateOf(initialEvent?.notes ?: "") }
    var error by remember { mutableStateOf("") }

    val types = listOf(
        "Assignment", "Class Test", "Viva", "Practical", "Quiz",
        "MST", "Exam", "Project", "Other"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (initialEvent == null) "ADD EVENT" else "EDIT EVENT",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ACADEMIC PLANNER",
                        color = Color(0xFF00D9FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "EVENT DETAILS",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Event Title") },
                placeholder = { Text("e.g. Hall Effect Viva") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Subject") },
                placeholder = { Text("e.g. Engineering Physics") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "EVENT TYPE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            types.forEach { item ->
                ChoiceButton(
                    text = item,
                    selected = type == item,
                    onClick = { type = item }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = date,
                onValueChange = {
                    date = it.filter { c -> c.isDigit() || c == '-' }
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                supportingText = {
                    Text("Day is calculated automatically.")
                },
                singleLine = true,
                isError = error.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = syllabus,
                onValueChange = { syllabus = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Syllabus") },
                placeholder = { Text("Topics / units to prepare") },
                minLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                placeholder = { Text("Any extra preparation details") },
                minLines = 2
            )

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF6B6B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val parsed = parseAcademicDate(date)

                    when {
                        title.trim().isEmpty() ->
                            error = "Please enter an event title."

                        subject.trim().isEmpty() ->
                            error = "Please enter the subject."

                        parsed == null ->
                            error = "Use a valid date in YYYY-MM-DD format."

                        else -> {
                            onSave(
                                AcademicEvent(
                                    id = initialEvent?.id ?: System.currentTimeMillis(),
                                    title = title.trim(),
                                    subject = subject.trim(),
                                    type = type,
                                    date = date,
                                    day = parsed.first,
                                    syllabus = syllabus.trim(),
                                    notes = notes.trim(),
                                    status = when (initialEvent?.status) {
                                        "COMPLETED" -> "COMPLETED"
                                        "CANCELLED" -> "CANCELLED"
                                        else -> "UPCOMING"
                                    }
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B4DFF)
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    text = if (initialEvent == null) {
                        "SAVE EVENT"
                    } else {
                        "SAVE CHANGES"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun todayDateKey(): String {
    val calendar = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

private fun eventStatus(event: AcademicEvent): String {
    if (event.status == "COMPLETED" || event.status == "CANCELLED") {
        return event.status
    }

    val today = todayDateKey()

    return when {
        event.date < today -> "EXPIRED"
        event.date == today -> "TODAY"
        else -> "UPCOMING"
    }
}

private fun parseAcademicDate(value: String): Pair<String, String>? {
    val parts = value.split("-")
    if (parts.size != 3) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31 || parts[0].length != 4) {
        return null
    }

    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.setLenient(false)

    return try {
        calendar.set(year, month - 1, day)
        calendar.time

        val weekday = calendar.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            java.util.Locale.getDefault()
        ) ?: return null

        weekday to value
    } catch (_: Exception) {
        null
    }
}

// =========================================================
// PROJECTS SCREEN
// =========================================================

@Composable
private fun ProjectsScreen(
    projects: MutableList<ProjectItem>,
    onBack: () -> Unit,
    onAddProject: (ProjectItem) -> Unit,
    onUpdateProject: (ProjectItem) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    var showAddProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectItem?>(null) }

    val completedProjects = projects.count { it.status == "COMPLETED" }
    val averageProgress = if (projects.isEmpty()) 0 else projects.map { it.progress }.average().roundToInt()
    val projectsWithGithub = projects.count { it.githubUrl.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Light)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PROJECTS", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("BUILD • DOCUMENT • SHOWCASE", color = Color(0xFF65E572), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF14251A))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("${projects.size} TOTAL", color = Color(0xFF65E572), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(23.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PORTFOLIO OVERVIEW", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                        Spacer(modifier = Modifier.height(7.dp))
                        Text("${projects.size} projects", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("Keep building evidence that shows what you can actually do.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 14.sp)
                    }
                    Text("$averageProgress%", color = Color(0xFF65E572), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))
                ProgressBar(averageProgress / 100f)
                Spacer(modifier = Modifier.height(7.dp))
                Text("AVERAGE PROJECT PROGRESS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        Spacer(modifier = Modifier.height(13.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStatCard(Modifier.weight(1f), "PROJECTS", projects.size.toString())
            SmallStatCard(Modifier.weight(1f), "COMPLETED", completedProjects.toString())
            SmallStatCard(Modifier.weight(1f), "GITHUB", projectsWithGithub.toString())
        }

        Spacer(modifier = Modifier.height(23.dp))
        SectionTitle("YOUR PROJECTS")
        Spacer(modifier = Modifier.height(11.dp))

        if (projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NO PROJECTS YET", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = .7.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add your projects, tasks, links and project photos here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(modifier = Modifier.height(13.dp))
                    Text("Start with one project and build your portfolio from there.", color = Color(0xFF65E572), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            projects.forEach { project ->
                ProjectCard(
                    project = project,
                    onEdit = { editingProject = project },
                    onDelete = { onDeleteProject(project.id) },
                    onToggleTask = { taskId ->
                        onUpdateProject(project.copy(tasks = project.tasks.map { task ->
                            if (task.id == taskId) task.copy(completed = !task.completed) else task
                        }))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(13.dp))
        Button(
            onClick = { showAddProject = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF65A96B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("+  ADD PROJECT", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(25.dp))
    }

    if (showAddProject) {
        ProjectEditorScreen(initialProject = null, onBack = { showAddProject = false }, onSave = { onAddProject(it); showAddProject = false })
    }

    editingProject?.let { project ->
        ProjectEditorScreen(initialProject = project, onBack = { editingProject = null }, onSave = { onUpdateProject(it); editingProject = null })
    }
}

@Composable
private fun ProjectCard(
    project: ProjectItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTask: (Long) -> Unit
) {
    val completedTasks = project.tasks.count { it.completed }
    val taskTotal = project.tasks.size
    val statusColor = when (project.status) {
        "COMPLETED" -> Color(0xFF65E572)
        "ARCHIVED" -> MaterialTheme.colorScheme.onSurfaceVariant
        "PLANNING" -> Color(0xFFFFD23F)
        "ONGOING" -> Color(0xFF00D9FF)
        else -> Color(0xFFB76CFF)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = .12f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(project.status, color = statusColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("${project.progress}%", color = statusColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (project.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
            }

            Spacer(modifier = Modifier.height(11.dp))
            ProgressBar(project.progress / 100f)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectMetaPill("TECH", project.techStack.ifBlank { "Not added" }, Modifier.weight(1f))
                ProjectMetaPill("TASKS", "$completedTasks / $taskTotal", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(7.dp))
            ProjectMetaPill("STARTED", project.startDate.ifBlank { "Not added" }, Modifier.fillMaxWidth())

            if (project.githubUrl.isNotBlank() || project.liveUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (project.githubUrl.isNotBlank()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xFF17172A)).padding(horizontal = 9.dp, vertical = 6.dp)) {
                            Text("GITHUB LINK SAVED", color = Color(0xFFB76CFF), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (project.liveUrl.isNotBlank()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xFF14251A)).padding(horizontal = 9.dp, vertical = 6.dp)) {
                            Text("LIVE LINK SAVED", color = Color(0xFF65E572), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (project.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("TASKS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(5.dp))
                project.tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTask(task.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (task.completed) "☑" else "☐", color = if (task.completed) Color(0xFF65E572) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(task.title, color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) {
                    Text("EDIT", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) {
                    Text("DELETE", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProjectMetaPill(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProjectEditorScreen(
    initialProject: ProjectItem?,
    onBack: () -> Unit,
    onSave: (ProjectItem) -> Unit
) {
    var name by remember { mutableStateOf(initialProject?.name ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var techStack by remember { mutableStateOf(initialProject?.techStack ?: "") }
    var startDate by remember { mutableStateOf(initialProject?.startDate ?: "") }
    var status by remember { mutableStateOf(initialProject?.status ?: "IDEA") }
    var githubUrl by remember { mutableStateOf(initialProject?.githubUrl ?: "") }
    var liveUrl by remember { mutableStateOf(initialProject?.liveUrl ?: "") }
    var progressText by remember { mutableStateOf(initialProject?.progress?.toString() ?: "0") }
    var taskText by remember { mutableStateOf(initialProject?.tasks?.joinToString("\n") { it.title } ?: "") }
    var error by remember { mutableStateOf("") }

    // Project status is intentionally a real selectable list, not a hardcoded single option.
    val statuses = listOf("IDEA", "PLANNING", "ONGOING", "COMPLETED", "ARCHIVED")
    val progress = progressText.toIntOrNull()?.coerceIn(0, 100) ?: 0

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(if (initialProject == null) "ADD PROJECT" else "EDIT PROJECT", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("PROJECT PORTFOLIO", color = Color(0xFF65E572), fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(name, { name = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Project Name") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(description, { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 3)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(techStack, { techStack = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tech Stack") }, placeholder = { Text("Kotlin, Compose, Firebase...") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(startDate, { startDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Start Date") }, placeholder = { Text("DD/MM/YYYY") }, singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("STATUS")
            Spacer(modifier = Modifier.height(8.dp))
            statuses.forEach { option ->
                ChoiceButton(option, status == option) { status = option }
                Spacer(modifier = Modifier.height(7.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(githubUrl, { githubUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("GitHub URL") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(liveUrl, { liveUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Live Demo URL") }, singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("PROGRESS")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(progressText, { progressText = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text("Progress (0–100)") }, singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            ProgressBar(progress / 100f)

            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle("TASKS")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(taskText, { taskText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tasks — one per line") }, placeholder = { Text("Design UI\nBuild backend\nTesting\nDeploy") }, minLines = 4)

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(error, color = Color(0xFFFF6B6B), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "Please enter a project name."
                    } else {
                        val tasks = taskText.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapIndexed { index, title ->
                            val old = initialProject?.tasks?.getOrNull(index)
                            ProjectTask(id = old?.id ?: (System.currentTimeMillis() + index), title = title, completed = old?.completed ?: false)
                        }
                        onSave(
                            ProjectItem(
                                id = initialProject?.id ?: System.currentTimeMillis(),
                                firestoreId = initialProject?.firestoreId.orEmpty(),
                                name = name.trim(),
                                description = description.trim(),
                                techStack = techStack.trim(),
                                startDate = startDate.trim(),
                                status = status,
                                githubUrl = githubUrl.trim(),
                                liveUrl = liveUrl.trim(),
                                progress = progress,
                                tasks = tasks
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF65A96B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE PROJECT", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun calculatePortfolioScore(projects: List<ProjectItem>): Int {
    if (projects.isEmpty()) return 0
    val averageProgress = projects.map { it.progress }.average()
    val completionScore = projects.count { it.status == "COMPLETED" }.toFloat() / projects.size * 20f
    val techScore = projects.count { it.techStack.isNotBlank() }.toFloat() / projects.size * 15f
    val githubScore = projects.count { it.githubUrl.isNotBlank() }.toFloat() / projects.size * 15f
    val liveScore = projects.count { it.liveUrl.isNotBlank() }.toFloat() / projects.size * 15f
    val taskScore = projects.count { it.tasks.isNotEmpty() }.toFloat() / projects.size * 5f
    val progressScore = averageProgress * 0.30f
    return (completionScore + techScore + githubScore + liveScore + taskScore + progressScore).roundToInt().coerceIn(0, 100)
}

// =========================================================
// DSA SCREEN
// =========================================================

@Composable
private fun DSAScreen(
    problems: List<DSAProblem>,
    isLoading: Boolean,
    errorMessage: String,
    onBack: () -> Unit,
    onAddProblem: (DSAProblem, (Boolean, String) -> Unit) -> Unit,
    onDeleteProblem: (DSAProblem) -> Unit
) {
    var showAddProblem by remember { mutableStateOf(false) }

    val dsaScore = calculateDsaProgress(problems)
    val easyCount = problems.count { it.difficulty == "Easy" }
    val mediumCount = problems.count { it.difficulty == "Medium" }
    val hardCount = problems.count { it.difficulty == "Hard" }
    val weightedScore = problems.sumOf { it.score.toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Light)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("DSA", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("PROBLEM SOLVING", color = Color(0xFF00D9FF), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF101F24))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("${problems.size} SOLVED", color = Color(0xFF00D9FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OVERALL DSA SCORE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${dsaScore.roundToInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("Weighted progress toward your PRISM target", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF17172A))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("1000 TARGET", color = Color(0xFFB76CFF), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))
                ProgressBar(dsaScore / 100f)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("${"%.1f".format(weightedScore)} weighted points", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("100%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStatCard(Modifier.weight(1f), "SOLVED", problems.size.toString())
            SmallStatCard(Modifier.weight(1f), "TARGET", "100")
            SmallStatCard(Modifier.weight(1f), "WEIGHTED", "${"%.0f".format(weightedScore)}")
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("DIFFICULTY BREAKDOWN")
        Spacer(modifier = Modifier.height(11.dp))
        DifficultyRow("EASY", easyCount, Color(0xFF65E572))
        Spacer(modifier = Modifier.height(9.dp))
        DifficultyRow("MEDIUM", mediumCount, Color(0xFFFFD23F))
        Spacer(modifier = Modifier.height(9.dp))
        DifficultyRow("HARD", hardCount, Color(0xFFFF7B72))

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("TOPIC MASTERY")
        Spacer(modifier = Modifier.height(11.dp))
        TopicProgressRow("Arrays", topicProgress(problems, "Arrays"))
        TopicProgressRow("Strings", topicProgress(problems, "Strings"))
        TopicProgressRow("Linked List", topicProgress(problems, "Linked List"))
        TopicProgressRow("Trees / BST", topicProgress(problems, "Trees / BST"))
        TopicProgressRow("Graphs", topicProgress(problems, "Graphs"))
        TopicProgressRow("Dynamic Programming", topicProgress(problems, "Dynamic Programming"))

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("DSA GROWTH")
        Spacer(modifier = Modifier.height(11.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("LAST 6 CHECKPOINTS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text("CONSISTENCY", color = Color(0xFF00D9FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(13.dp))
                GrowthGraph(currentScore = dsaScore)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("RECENT PROBLEMS")
        Spacer(modifier = Modifier.height(11.dp))

        when {
            isLoading -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("LOADING YOUR DSA DATA...", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Restoring your saved problem history.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }

            problems.isEmpty() -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("NO PROBLEMS YET", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = .6.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add the problems you have solved. They will be saved to your PRISM account and restored when you reopen the app.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }

            else -> problems.takeLast(5).reversed().forEach { problem ->
                ProblemRow(problem = problem, onDelete = { onDeleteProblem(problem) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(9.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF241416))
            ) {
                Text(errorMessage, color = Color(0xFFFF7B72), fontSize = 10.sp, modifier = Modifier.padding(13.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { showAddProblem = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("+  ADD PROBLEM", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(19.dp)) {
                Text("✦  PRISM INSIGHT", color = Color(0xFFB76CFF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(9.dp))
                Text(generateDsaInsight(problems), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(25.dp))
    }

    if (showAddProblem) {
        AddProblemScreen(
            onBack = { showAddProblem = false },
            onSave = { problem, result ->
                onAddProblem(problem) { success, message ->
                    result(success, message)
                    if (success) showAddProblem = false
                }
            }
        )
    }
}

// =========================================================
// ADD PROBLEM SCREEN
// =========================================================

@Composable
private fun AddProblemScreen(
    onBack: () -> Unit,
    onSave: (DSAProblem, (Boolean, String) -> Unit) -> Unit
) {
    var problemName by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("Arrays") }
    var selectedDifficulty by remember { mutableStateOf("Easy") }
    var error by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val topics = listOf(
        "Arrays",
        "Strings",
        "Searching & Sorting",
        "Linked List",
        "Stack & Queue",
        "Hashing",
        "Recursion",
        "Trees / BST",
        "Heap",
        "Greedy",
        "Graphs",
        "Backtracking",
        "Tries",
        "Dynamic Programming",
        "Bit Manipulation"
    )

    val difficulties = listOf("Easy", "Medium", "Hard")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 38.sp,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = "ADD PROBLEM",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "UPDATE YOUR DSA JOURNEY",
                        color = Color(0xFF00D9FF),
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = problemName,
                onValueChange = {
                    problemName = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Problem Name") },
                placeholder = { Text("e.g. Two Sum") },
                singleLine = true,
                isError = error.isNotEmpty(),
                enabled = !isSaving
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "DIFFICULTY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            difficulties.forEach { difficulty ->
                ChoiceButton(
                    text = difficulty,
                    selected = selectedDifficulty == difficulty,
                    onClick = {
                        if (!isSaving) selectedDifficulty = difficulty
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "TOPIC",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            topics.forEach { topic ->
                ChoiceButton(
                    text = topic,
                    selected = selectedTopic == topic,
                    onClick = {
                        if (!isSaving) selectedTopic = topic
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF6B6B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val previewScore = calculateProblemScore(
                selectedDifficulty,
                selectedTopic
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PRISM WEIGHT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "${"%.1f".format(previewScore)} points",
                        color = Color(0xFF00D9FF),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Difficulty + topic importance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val cleanName = problemName.trim()

                    if (cleanName.isEmpty()) {
                        error = "Please enter a problem name."
                        return@Button
                    }

                    error = ""
                    isSaving = true

                    onSave(
                        DSAProblem(
                            name = cleanName,
                            topic = selectedTopic,
                            difficulty = selectedDifficulty,
                            score = previewScore
                        )
                    ) { success, message ->
                        isSaving = false
                        if (!success) {
                            error = message.ifBlank { "Unable to save problem." }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isSaving) "SAVING..." else "SAVE PROBLEM",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}


// =========================================================
// CIRCULAR PROGRESS
// =========================================================

@Composable
private fun CircularProgress(
    progress: Float,
    percentage: Int
) {

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val strokeWidth = 13.dp.toPx()

            drawArc(
                color = Color(0xFF292930),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth
                )
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFB76CFF),
                        Color(0xFF00D9FF),
                        Color(0xFFB76CFF)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(
                    0f,
                    1f
                ),
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "$percentage%",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "DSA SCORE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}


// =========================================================
// GROWTH GRAPH
// =========================================================

@Composable
private fun GrowthGraph(
    currentScore: Float
) {

    val points = listOf(
        18f,
        25f,
        31f,
        39f,
        48f,
        currentScore
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {

        val widthStep =
            size.width / (points.size - 1)

        val maxValue = 100f

        // Grid lines

        for (i in 0..4) {

            val y =
                size.height * i / 4f

            drawLine(
                color = Color(0xFF202027),
                start = Offset(
                    0f,
                    y
                ),
                end = Offset(
                    size.width,
                    y
                ),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Graph points

        val offsets = points.mapIndexed { index, value ->

            Offset(
                x = widthStep * index,
                y = size.height -
                        (value / maxValue) *
                        size.height
            )
        }

        // Graph line

        for (i in 0 until offsets.size - 1) {

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB76CFF),
                        Color(0xFF00D9FF)
                    )
                ),
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Points

        offsets.forEach { point ->

            drawCircle(
                color = Color(0xFF00D9FF),
                radius = 5.dp.toPx(),
                center = point
            )
        }
    }
}


// =========================================================
// DIFFICULTY ROW
// =========================================================

@Composable
private fun DifficultyRow(
    title: String,
    count: Int,
    color: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(
                modifier = Modifier.size(10.dp)
            )

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = count.toString(),
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// =========================================================
// TOPIC PROGRESS
// =========================================================

@Composable
private fun TopicProgressRow(
    topic: String,
    progress: Float
) {

    Column(
        modifier = Modifier.padding(
            vertical = 5.dp
        )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = topic,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp
            )

            Text(
                text = "${(progress * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        ProgressBar(
            progress = progress
        )
    }
}


// =========================================================
// PROBLEM ROW
// =========================================================

@Composable
private fun ProblemRow(
    problem: DSAProblem,
    onDelete: () -> Unit
) {
    val difficultyColor = when (problem.difficulty) {
        "Easy" -> Color(0xFF65E572)
        "Medium" -> Color(0xFFFFD23F)
        else -> Color(0xFFFF7B72)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = Color(0xFF1A1A21),
                        shape = RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color(0xFF65E572),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = problem.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = problem.topic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = problem.difficulty,
                    color = difficultyColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 0.dp
                    ),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = "DELETE",
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}


// =========================================================
// SMALL STAT CARD
// =========================================================

@Composable
private fun SmallStatCard(
    modifier: Modifier,
    title: String,
    value: String
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// =========================================================
// CHOICE BUTTON
// =========================================================

@Composable
private fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor =
                if (selected) {
                    Color(0xFF1D1730)
                } else {
                    Color.Transparent
                },
            contentColor =
                if (selected) {
                    Color(0xFFB76CFF)
                } else {
                    Color.White
                }
        )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = text,
                fontSize = 11.sp
            )

            if (selected) {

                Text(
                    text = "✓",
                    color = Color(0xFF00D9FF),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// =========================================================
// PROGRESS BAR
// =========================================================

@Composable
private fun ProgressBar(
    progress: Float
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .background(
                Color(0xFF292930),
                RoundedCornerShape(10.dp)
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(
                    progress.coerceIn(
                        0f,
                        1f
                    )
                )
                .height(7.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB76CFF),
                            Color(0xFF00D9FF)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
        )
    }
}


// =========================================================
// SECTION TITLE
// =========================================================

@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp
    )
}


// =========================================================
// CGPA SCREEN
// =========================================================

@Composable
private fun CGPAScreen(
    initialCurrentCgpa: String,
    initialTargetCgpa: String,
    saveError: String,
    onSave: (String, String, (Boolean, String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var currentCgpa by remember(initialCurrentCgpa) { mutableStateOf(initialCurrentCgpa) }
    var targetCgpa by remember(initialTargetCgpa) { mutableStateOf(initialTargetCgpa) }
    var editMode by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val current = currentCgpa.toFloatOrNull()
    val target = targetCgpa.toFloatOrNull()
    val validCurrent = current != null && current in 4f..10f
    val validTarget = target != null && target in 4f..10f
    val progress = if (validCurrent && validTarget && target!! > 0f) {
        (current!! / target).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 38.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("CGPA", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("ACADEMIC PERFORMANCE", color = Color(0xFFB76CFF), fontSize = 8.sp, letterSpacing = 1.8.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT CGPA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentCgpa, color = MaterialTheme.colorScheme.onSurface, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text("VALID RANGE: 4.0 – 10.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(22.dp))
                ProgressBar(progress = progress)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when {
                        !validCurrent -> "Enter a valid CGPA"
                        !validTarget -> "Set a valid target"
                        current!! >= target!! -> "Target achieved 🎯"
                        else -> "${"%.1f".format(target - current)} points to target"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("YOUR TARGET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(7.dp))
                Text(targetCgpa, color = Color(0xFF00D9FF), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (editMode) {
            OutlinedTextField(
                value = currentCgpa,
                onValueChange = { currentCgpa = it.filter { c -> c.isDigit() || c == '.' }; validationError = ""; savedMessage = "" },
                label = { Text("Current CGPA") },
                supportingText = { Text("Allowed range: 4.0 – 10.0") },
                isError = currentCgpa.isNotEmpty() && !validCurrent,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = targetCgpa,
                onValueChange = { targetCgpa = it.filter { c -> c.isDigit() || c == '.' }; validationError = ""; savedMessage = "" },
                label = { Text("Target CGPA") },
                supportingText = { Text("Allowed range: 4.0 – 10.0") },
                isError = targetCgpa.isNotEmpty() && !validTarget,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (validationError.isNotEmpty()) {
                Text(validationError, color = Color(0xFFFF6B6B), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (saveError.isNotEmpty()) {
                Text(saveError, color = Color(0xFFFF6B6B), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    if (!validCurrent || !validTarget) {
                        validationError = "CGPA must be between 4.0 and 10.0."
                    } else {
                        validationError = ""
                        savedMessage = ""
                        isSaving = true
                        onSave("%.1f".format(current), "%.1f".format(target)) { success, message ->
                            isSaving = false
                            if (success) {
                                savedMessage = "Academic data saved successfully."
                                editMode = false
                            } else {
                                validationError = message.ifBlank { "Unable to save academic data." }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(if (isSaving) "SAVING..." else "SAVE CHANGES", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = { editMode = true; validationError = ""; savedMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("EDIT CGPA", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }

        if (savedMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(savedMessage, color = Color(0xFF65E572), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("✦  PRISM INSIGHT", color = Color(0xFFB76CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when {
                        !validCurrent -> "Enter a valid CGPA between 4.0 and 10.0."
                        current!! >= 9f -> "Excellent academic performance. Keep maintaining your consistency."
                        current >= 8f -> "You're building a strong academic foundation. Keep pushing toward your target."
                        else -> "Focus on consistency and steady improvement toward your target."
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))
    }
}


// =========================================================
// PROFILE SCREEN
// =========================================================

@Composable
private fun ProfileScreen(
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val user = auth.currentUser
    val context = LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var college by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var localPhoto by remember { mutableStateOf<Bitmap?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null || user == null) return@rememberLauncherForActivityResult

        try {
            localPhoto = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) {
            localPhoto = null
        }

        message = "Uploading photo..."
        messageIsError = false

        val photoRef = storage.reference.child("users/${user.uid}/profile_photo.jpg")
        photoRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Photo upload failed.")
                photoRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                photoUrl = downloadUri.toString()
                firestore.collection("users").document(user.uid)
                    .set(mapOf("photoUrl" to photoUrl), SetOptions.merge())
                    .addOnSuccessListener {
                        message = "Profile photo uploaded successfully."
                        messageIsError = false
                    }
                    .addOnFailureListener { exception ->
                        message = exception.message ?: "Photo saved, but profile update failed."
                        messageIsError = true
                    }
            }
            .addOnFailureListener { exception ->
                message = exception.message ?: "Unable to upload profile photo."
                messageIsError = true
            }
    }

    if (user == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No signed-in account found.", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("BACK") }
        }
        return
    }

    LaunchedEffect(user.uid) {
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                fullName = document.getString("name") ?: ""
                email = document.getString("email") ?: (user.email ?: "")
                college = document.getString("college") ?: ""
                course = document.getString("course") ?: ""
                semester = document.getString("semester") ?: ""
                cgpa = document.getString("cgpa") ?: ""
                skills = document.getString("skills") ?: ""
                about = document.getString("about") ?: ""
                photoUrl = document.getString("photoUrl") ?: ""
                isLoading = false
            }
            .addOnFailureListener { exception ->
                message = exception.message ?: "Unable to load your profile."
                messageIsError = true
                isLoading = false
            }
    }

    LaunchedEffect(photoUrl) {
        if (photoUrl.isBlank() || localPhoto != null) return@LaunchedEffect
        localPhoto = withContext(Dispatchers.IO) {
            try { URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it) } }
            catch (_: Exception) { null }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Loading your profile...", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Light) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MY PROFILE", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("YOUR PRISM ACCOUNT", color = Color(0xFFB76CFF), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(Color(0xFF17172A)).padding(horizontal = 9.dp, vertical = 7.dp)) {
                Text(if (isEditing) "EDITING" else "PROFILE", color = Color(0xFFB76CFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(23.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(21.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (localPhoto != null) {
                    Image(bitmap = localPhoto!!.asImageBitmap(), contentDescription = "Profile photo", modifier = Modifier.size(108.dp).clip(RoundedCornerShape(54.dp)).background(Color(0xFF202027)))
                } else {
                    Box(
                        modifier = Modifier.size(108.dp).background(Brush.linearGradient(listOf(Color(0xFFB76CFF), Color(0xFF00D9FF))), RoundedCornerShape(54.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(fullName.firstOrNull()?.uppercase() ?: "A", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(11.dp))
                Text(fullName.ifBlank { "PRISM USER" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(email.ifBlank { "No email available" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(14.dp))
                if (isEditing) {
                    OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("UPLOAD PHOTO", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                } else {
                    Text(if (photoUrl.isNotBlank()) "PROFILE PHOTO SAVED" else "NO PROFILE PHOTO YET", color = Color(0xFF00D9FF), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle("PERSONAL INFORMATION")
        Spacer(modifier = Modifier.height(11.dp))

        ProfileField("FULL NAME", fullName, isEditing, { fullName = it })
        Spacer(modifier = Modifier.height(9.dp))
        ProfileField("EMAIL", email, false, {})
        Spacer(modifier = Modifier.height(9.dp))
        ProfileField("COLLEGE / UNIVERSITY", college, isEditing, { college = it })
        Spacer(modifier = Modifier.height(9.dp))
        ProfileField("COURSE / BRANCH", course, isEditing, { course = it })
        Spacer(modifier = Modifier.height(9.dp))
        ProfileField("SEMESTER", semester, isEditing, { semester = it })
        Spacer(modifier = Modifier.height(9.dp))
        ProfileField("CGPA", cgpa, isEditing, { cgpa = it })

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("ABOUT YOUR JOURNEY")
        Spacer(modifier = Modifier.height(11.dp))
        ProfileField("SKILLS", skills, isEditing, { skills = it }, minLines = 2)
        Spacer(modifier = Modifier.height(9.dp))
        ProfileField("ABOUT YOU", about, isEditing, { about = it }, minLines = 4)

        Spacer(modifier = Modifier.height(20.dp))

        if (isEditing) {
            Button(
                onClick = {
                    if (fullName.trim().isBlank()) { message = "Please enter your full name."; messageIsError = true; return@Button }
                    if (college.trim().isBlank()) { message = "Please enter your college / university."; messageIsError = true; return@Button }
                    if (course.trim().isBlank()) { message = "Please enter your course / branch."; messageIsError = true; return@Button }

                    isSaving = true
                    message = "Saving profile..."
                    messageIsError = false

                    val profile = mapOf(
                        "uid" to user.uid,
                        "name" to fullName.trim(),
                        "email" to (user.email ?: email),
                        "college" to college.trim(),
                        "course" to course.trim(),
                        "semester" to semester.trim(),
                        "cgpa" to cgpa.trim(),
                        "skills" to skills.trim(),
                        "about" to about.trim(),
                        "profileCompleted" to true
                    )

                    firestore.collection("users").document(user.uid).set(profile, SetOptions.merge())
                        .addOnSuccessListener {
                            isSaving = false
                            isEditing = false
                            message = "Profile updated successfully."
                            messageIsError = false
                        }
                        .addOnFailureListener { exception ->
                            isSaving = false
                            message = exception.message ?: "Unable to update your profile."
                            messageIsError = true
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(15.dp)
            ) { Text(if (isSaving) "SAVING..." else "SAVE CHANGES", fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.height(9.dp))
            OutlinedButton(onClick = { isEditing = false; message = "" }, modifier = Modifier.fillMaxWidth(), enabled = !isSaving, shape = RoundedCornerShape(15.dp)) {
                Text("CANCEL", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = { message = ""; isEditing = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(15.dp)
            ) { Text("EDIT PROFILE", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (messageIsError) Color(0xFF241416) else Color(0xFF14251A))
            ) {
                Text(message, color = if (messageIsError) Color(0xFFFF7B72) else Color(0xFF65E572), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(13.dp))
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        minLines = minLines,
        singleLine = minLines == 1
    )
}


// =========================================================
// FEATURE CARD
// =========================================================

@Composable
private fun FeatureCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Text(
                text = title,
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}


// =========================================================
// SCORING ENGINE
// =========================================================

private fun calculateProblemScore(
    difficulty: String,
    topic: String
): Float {

    val difficultyWeight = when (difficulty) {

        "Easy" -> 1.0f
        "Medium" -> 2.0f
        "Hard" -> 3.0f

        else -> 1.0f
    }

    val topicMultiplier = when (topic) {

        "Arrays" -> 1.00f
        "Strings" -> 1.00f

        "Searching & Sorting" -> 1.10f
        "Linked List" -> 1.10f
        "Stack & Queue" -> 1.10f
        "Hashing" -> 1.10f

        "Recursion" -> 1.20f
        "Trees / BST" -> 1.20f
        "Heap" -> 1.20f
        "Greedy" -> 1.20f
        "Bit Manipulation" -> 1.20f

        "Graphs" -> 1.30f
        "Backtracking" -> 1.30f
        "Tries" -> 1.30f

        "Dynamic Programming" -> 1.40f

        else -> 1.00f
    }

    return difficultyWeight * topicMultiplier
}


// =========================================================
// DSA PROGRESS CALCULATOR
// =========================================================

private fun calculateDsaProgress(
    problems: List<DSAProblem>
): Float {

    if (problems.isEmpty()) {
        return 0f
    }

    val weightedScore =
        problems.sumOf {
            it.score.toDouble()
        }

    /*
     * 100 problems is the current PRISM target.
     *
     * Average target score is approximately 2.0.
     * Therefore 100 × 2 = 200 weighted points.
     */

    val targetWeightedScore = 1000f

    return (
            weightedScore.toFloat() /
                    targetWeightedScore *
                    100f
            ).coerceIn(
            0f,
            100f
        )
}


// =========================================================
// TOPIC PROGRESS
// =========================================================

private fun topicProgress(
    problems: List<DSAProblem>,
    topic: String
): Float {

    val topicCount =
        problems.count {
            it.topic == topic
        }

    /*
     * Current topic milestone:
     * 30 solved problems = 100%
     */

    return (
            topicCount / 30f
            ).coerceIn(
            0f,
            1f
        )
}


// =========================================================
// DSA INSIGHT
// =========================================================

private fun generateDsaInsight(
    problems: List<DSAProblem>
): String {

    if (problems.isEmpty()) {

        return "Start solving problems and PRISM will begin analysing your DSA journey."
    }

    val graphCount =
        problems.count {
            it.topic == "Graphs"
        }

    val dpCount =
        problems.count {
            it.topic == "Dynamic Programming"
        }

    val hardCount =
        problems.count {
            it.difficulty == "Hard"
        }

    return when {

        graphCount == 0 && dpCount == 0 ->
            "Your foundation is growing. Start exploring Graphs and Dynamic Programming to increase your advanced-topic coverage."

        dpCount == 0 ->
            "Your DSA foundation is taking shape. Dynamic Programming is your next major area to explore."

        hardCount < 3 ->
            "Try adding more challenging problems. A stronger Hard-problem mix will improve your weighted DSA score."

        else ->
            "Your DSA profile is becoming well balanced. Keep maintaining consistency across different topics and difficulty levels."
    }
}