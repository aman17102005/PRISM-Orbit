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
// HOME SCREEN
// =========================================================

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

    var academicRecord by remember { mutableStateOf(AcademicRecord()) }
    var academicLoadError by remember { mutableStateOf("") }

    val academicEvents = remember { mutableStateListOf<AcademicEvent>() }

    // DSA is now user-specific Firestore data. No sample/hardcoded problems.
    val dsaProblems = remember { mutableStateListOf<DSAProblem>() }
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

    if (showProfileScreen) {
        ProfileScreen(
            onBack = { showProfileScreen = false }
        )
    } else if (showAcademicsScreen) {
        AcademicsScreen(
            currentCgpa = academicRecord.currentCgpa.toFloatOrNull() ?: 8.7f,
            targetCgpa = academicRecord.targetCgpa.toFloatOrNull() ?: 9.0f,
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
    } else {
        DashboardScreen(
            dsaProblems = dsaProblems,
            currentCgpa = academicRecord.currentCgpa,
            onCgpaClick = { showAcademicsScreen = true },
            onDsaClick = {
                dsaLoadError = ""
                showDsaScreen = true
            },
            onProfileClick = { showProfileScreen = true }
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
    onProfileClick: () -> Unit
) {

    val dsaScore = calculateDsaProgress(dsaProblems)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
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
                    color = Color.White,
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
                    text = "A",
                    color = Color.White,
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
            text = "Welcome back, Aman.",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Your journey is taking shape.",
            color = Color(0xFF85858F),
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
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
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "›",
                    color = Color.White,
                    fontSize = 28.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =====================================================
        // CAREER SCORE
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111116)
            )
        ) {

            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Text(
                    text = "CAREER SCORE",
                    color = Color(0xFF8B8B95),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {

                    Text(
                        text = "78",
                        color = Color.White,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "/ 100",
                        color = Color(0xFF686871),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(
                            bottom = 8.dp
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                ProgressBar(
                    progress = 0.78f
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "You're on the right track.",
                    color = Color(0xFF777780),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // YOUR PRISM
        // =====================================================

        Text(
            text = "YOUR PRISM",
            color = Color.White,
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
                modifier = Modifier.weight(1f),
                title = "PROJECTS",
                value = "6",
                subtitle = "Portfolio",
                accent = Color(0xFF65E572)
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "INTERNSHIPS",
                value = "2",
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
                modifier = Modifier.weight(1f),
                title = "PLACEMENT",
                value = "68%",
                subtitle = "Readiness",
                accent = Color(0xFFFF7B72)
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "GROWTH",
                value = "81%",
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111116)
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

                Text(
                    text = "Focus on DSA this week.",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "You're close to your next milestone.",
                    color = Color(0xFF777780),
                    fontSize = 11.sp
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
    var semesterDrafts by remember(semesterValues) { mutableStateOf(semesterValues.toMutableList()) }
    var semesterError by remember { mutableStateOf("") }

    val semesterNames = listOf("SEM 1", "SEM 2", "SEM 3", "SEM 4", "SEM 5", "SEM 6", "SEM 7", "SEM 8")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("ACADEMICS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("PERFORMANCE + PLANNING", color = Color(0xFFB76CFF), fontSize = 8.sp, letterSpacing = 1.8.sp)
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ACADEMIC OVERVIEW", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${"%.1f".format(currentCgpa)}", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        Text("CURRENT CGPA", color = Color(0xFF777780), fontSize = 9.sp, letterSpacing = 1.2.sp)
                    }
                    Text("TARGET  ${"%.1f".format(targetCgpa)}", color = Color(0xFF00D9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(15.dp))
                ProgressBar(progress = (currentCgpa / 10f).coerceIn(0f, 1f))
                Spacer(modifier = Modifier.height(15.dp))
                Button(
                    onClick = onCgpaClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15151B)),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text("VIEW / EDIT CGPA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(text = "SEMESTER TRACKER")
            OutlinedButton(
                onClick = {
                    if (!editSemesters) semesterDrafts = semesterValues.toMutableList()
                    semesterError = ""
                    editSemesters = !editSemesters
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editSemesters) "CANCEL" else "EDIT", fontSize = 9.sp)
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
                    placeholder = { Text(if (index < 2) "e.g. 8.4" else "Leave blank until completed") },
                    supportingText = { Text("0.0 – 10.0; blank = not completed") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp),
                    singleLine = true
                )
            }

            if (semesterError.isNotEmpty()) {
                Text(semesterError, color = Color(0xFFFF6B6B), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val valid = semesterDrafts.all { it.isBlank() || (it.toFloatOrNull()?.let { value -> value in 0f..10f } == true) }
                    if (!valid) {
                        semesterError = "Each SGPA must be between 0.0 and 10.0."
                    } else {
                        onSaveSemesters(semesterDrafts.map { it.trim() })
                        semesterError = ""
                        editSemesters = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("SAVE SEMESTERS", fontWeight = FontWeight.Bold)
            }
        } else {
            semesterNames.forEachIndexed { index, name ->
                val value = semesterValues.getOrElse(index) { "" }.trim()
                SemesterRow(name, value, value.isNotBlank())
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle(text = "SEMESTER GROWTH")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("SGPA TREND", color = Color(0xFF777780), fontSize = 9.sp, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(12.dp))
                AcademicGrowthGraph(semesterValues)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = "ACADEMIC PLANNER")
        Spacer(modifier = Modifier.height(12.dp))

        AcademicCalendar(events = events, onAddEvent = { showAddEvent = true })
        Spacer(modifier = Modifier.height(22.dp))

        if (events.isNotEmpty()) {
            SectionTitle(text = "ACADEMIC EVENTS")
            Spacer(modifier = Modifier.height(12.dp))
            events
                .sortedWith(compareBy<AcademicEvent> { eventStatus(it) == "EXPIRED" || eventStatus(it) == "CANCELLED" }.thenBy { it.date })
                .take(20)
                .forEach { event ->
                    AcademicEventRow(
                        event = event,
                        onEdit = { editingEvent = it },
                        onDelete = { onDeleteEvent(it) },
                        onComplete = { onCompleteEvent(it) },
                        onCancel = { onCancelEvent(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("NO UPCOMING EVENTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Add your assignments, tests, viva, practicals and exams here.", color = Color(0xFF777780), fontSize = 10.sp)
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
private fun SemesterRow(name: String, sgpa: String, completed: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (completed) sgpa else "NOT COMPLETED",
                color = if (completed) Color(0xFF00D9FF) else Color(0xFF66666F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AcademicGrowthGraph(semesterValues: List<String>) {
    val parsed = semesterValues.mapNotNull { it.toFloatOrNull()?.takeIf { value -> value in 0f..10f } }
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
                y = size.height - ((value - min) / (max - min)).coerceIn(0f, 1f) * size.height
            )
        }

        for (i in 0 until offsets.size - 1) {
            drawLine(
                brush = Brush.linearGradient(listOf(Color(0xFFB76CFF), Color(0xFF00D9FF))),
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        offsets.forEach {
            drawCircle(Color(0xFF00D9FF), 5.dp.toPx(), it)
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
    val eventDays = events.mapNotNull { it.date.substringAfterLast('-').toIntOrNull() }.toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = first.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault()).uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(year.toString(), color = Color(0xFF777780), fontSize = 9.sp)
                }
                Text(
                    text = "${events.size} EVENTS",
                    color = Color(0xFF00D9FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
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
                                        color = if (isToday || hasEvent) Color.White else Color(0xFFAAAAAF),
                                        fontSize = 10.sp,
                                        fontWeight = if (isToday || hasEvent) FontWeight.Bold else FontWeight.Normal
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("+  ADD ACADEMIC EVENT", fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    event.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    status,
                    color = statusColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "${event.subject}  •  ${event.date}  •  ${event.day}",
                color = Color(0xFF00D9FF),
                fontSize = 9.sp
            )
            Text(
                event.type.uppercase(),
                color = Color(0xFFB76CFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )

            if (event.syllabus.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    "Syllabus: ${event.syllabus}",
                    color = Color(0xFF888891),
                    fontSize = 9.sp
                )
            }
            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    event.notes,
                    color = Color(0xFF777780),
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(event) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("EDIT", fontSize = 9.sp)
                }

                OutlinedButton(
                    onClick = { onDelete(event.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("DELETE", fontSize = 9.sp)
                }
            }

            if (status != "COMPLETED" && status != "CANCELLED" && status != "EXPIRED") {
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✓ COMPLETE", fontSize = 9.sp, color = Color(0xFF65E572))
                    }

                    OutlinedButton(
                        onClick = { onCancel(event.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CANCEL", fontSize = 9.sp)
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
            .background(Color(0xFF050507))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    color = Color.White,
                    fontSize = 38.sp,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        if (initialEvent == null) "ADD EVENT" else "EDIT EVENT",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "ACADEMIC PLANNER",
                        color = Color(0xFF00D9FF),
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Event Title") },
                placeholder = { Text("e.g. Hall Effect Viva") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Subject") },
                placeholder = { Text("e.g. Engineering Physics") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "EVENT TYPE",
                color = Color(0xFF888891),
                fontSize = 10.sp,
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
                Text(error, color = Color(0xFFFF6B6B), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val parsed = parseAcademicDate(date)
                    when {
                        title.trim().isEmpty() -> error = "Please enter an event title."
                        subject.trim().isEmpty() -> error = "Please enter the subject."
                        parsed == null -> error = "Use a valid date in YYYY-MM-DD format."
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    if (initialEvent == null) "SAVE EVENT" else "SAVE CHANGES",
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
    if (month !in 1..12 || day !in 1..31 || parts[0].length != 4) return null
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.setLenient(false)
    return try {
        calendar.set(year, month - 1, day)
        calendar.time
        val weekday = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, java.util.Locale.getDefault()) ?: return null
        weekday to value
    } catch (_: Exception) {
        null
    }
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
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = "DSA",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PROBLEM SOLVING",
                    color = Color(0xFF00D9FF),
                    fontSize = 8.sp,
                    letterSpacing = 1.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OVERALL DSA SCORE",
                    color = Color(0xFF888891),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(15.dp))
                CircularProgress(
                    progress = dsaScore / 100f,
                    percentage = dsaScore.roundToInt()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Weighted Score  ${"%.1f".format(weightedScore)}",
                    color = Color(0xFF9A9AA4),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallStatCard(
                modifier = Modifier.weight(1f),
                title = "SOLVED",
                value = problems.size.toString()
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                title = "TARGET",
                value = "100"
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                title = "STREAK",
                value = "100 🔥"
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        SectionTitle(text = "DIFFICULTY BREAKDOWN")
        Spacer(modifier = Modifier.height(12.dp))

        DifficultyRow(title = "EASY", count = easyCount, color = Color(0xFF65E572))
        Spacer(modifier = Modifier.height(10.dp))
        DifficultyRow(title = "MEDIUM", count = mediumCount, color = Color(0xFFFFD23F))
        Spacer(modifier = Modifier.height(10.dp))
        DifficultyRow(title = "HARD", count = hardCount, color = Color(0xFFFF7B72))

        Spacer(modifier = Modifier.height(25.dp))

        SectionTitle(text = "TOPIC MASTERY")
        Spacer(modifier = Modifier.height(12.dp))

        TopicProgressRow(topic = "Arrays", progress = topicProgress(problems, "Arrays"))
        TopicProgressRow(topic = "Strings", progress = topicProgress(problems, "Strings"))
        TopicProgressRow(topic = "Linked List", progress = topicProgress(problems, "Linked List"))
        TopicProgressRow(topic = "Trees / BST", progress = topicProgress(problems, "Trees / BST"))
        TopicProgressRow(topic = "Graphs", progress = topicProgress(problems, "Graphs"))
        TopicProgressRow(topic = "Dynamic Programming", progress = topicProgress(problems, "Dynamic Programming"))

        Spacer(modifier = Modifier.height(25.dp))

        SectionTitle(text = "DSA GROWTH")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "LAST 6 CHECKPOINTS",
                    color = Color(0xFF777780),
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(15.dp))
                GrowthGraph(currentScore = dsaScore)
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        SectionTitle(text = "RECENT PROBLEMS")
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
                ) {
                    Text(
                        text = "LOADING YOUR DSA DATA...",
                        color = Color(0xFF777780),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            problems.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "NO PROBLEMS YET",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Add the problems you have solved. They will be saved to your PRISM account and restored when you reopen the app.",
                            color = Color(0xFF777780),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            else -> {
                problems.takeLast(5).reversed().forEach { problem ->
                    ProblemRow(
                        problem = problem,
                        onDelete = { onDeleteProblem(problem) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFFF7B72),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Button(
            onClick = { showAddProblem = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "+  ADD PROBLEM", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "✦  PRISM INSIGHT",
                    color = Color(0xFFB76CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = generateDsaInsight(problems),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
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
                    if (success) {
                        showAddProblem = false
                    }
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
            .background(Color(0xFF050507))
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
                    color = Color.White,
                    fontSize = 38.sp,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = "ADD PROBLEM",
                        color = Color.White,
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
                color = Color(0xFF888891),
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
                color = Color(0xFF888891),
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PRISM WEIGHT",
                        color = Color(0xFF888891),
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
                        color = Color(0xFF777780),
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
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "DSA SCORE",
                color = Color(0xFF777780),
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
            containerColor = Color(0xFF111116)
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
                color = Color.White,
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
                color = Color.White,
                fontSize = 10.sp
            )

            Text(
                text = "${(progress * 100).roundToInt()}%",
                color = Color(0xFF9999A3),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
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
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = problem.topic,
                    color = Color(0xFF777780),
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
            containerColor = Color(0xFF111116)
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = title,
                color = Color(0xFF777780),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = value,
                color = Color.White,
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
        color = Color.White,
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
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("CGPA", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("ACADEMIC PERFORMANCE", color = Color(0xFFB76CFF), fontSize = 8.sp, letterSpacing = 1.8.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT CGPA", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentCgpa, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text("VALID RANGE: 4.0 – 10.0", color = Color(0xFF777780), fontSize = 9.sp, letterSpacing = 1.2.sp)
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
                    color = Color(0xFF9999A3),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("YOUR TARGET", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15151B)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("EDIT CGPA", color = Color.White, fontWeight = FontWeight.Bold)
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
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
                    color = Color.White,
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
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Photo upload failed.")
                }
                photoRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                photoUrl = downloadUri.toString()
                firestore.collection("users")
                    .document(user.uid)
                    .set(
                        mapOf("photoUrl" to photoUrl),
                        SetOptions.merge()
                    )
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050507))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No signed-in account found.", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("BACK") }
        }
        return
    }

    LaunchedEffect(user.uid) {
        firestore.collection("users")
            .document(user.uid)
            .get()
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

    // Load the saved photo on another device.
    LaunchedEffect(photoUrl) {
        if (photoUrl.isBlank() || localPhoto != null) return@LaunchedEffect
        localPhoto = withContext(Dispatchers.IO) {
            try {
                URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050507)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading your profile...", color = Color.White)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MY PROFILE",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "YOUR PRISM ACCOUNT",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    letterSpacing = 1.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (localPhoto != null) {
                    Image(
                        bitmap = localPhoto!!.asImageBitmap(),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFF202027))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFB76CFF), Color(0xFF00D9FF))
                                ),
                                RoundedCornerShape(50.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fullName.firstOrNull()?.uppercase() ?: "A",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isEditing) {
                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("UPLOAD PHOTO", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = if (photoUrl.isNotBlank()) "Profile photo saved" else "No profile photo yet",
                        color = Color(0xFF777780),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        ProfileField(
            label = "FULL NAME",
            value = fullName,
            enabled = isEditing,
            onValueChange = { fullName = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "EMAIL",
            value = email,
            enabled = false,
            onValueChange = {}
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "COLLEGE / UNIVERSITY",
            value = college,
            enabled = isEditing,
            onValueChange = { college = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "COURSE / BRANCH",
            value = course,
            enabled = isEditing,
            onValueChange = { course = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "SEMESTER",
            value = semester,
            enabled = isEditing,
            onValueChange = { semester = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "CGPA",
            value = cgpa,
            enabled = isEditing,
            onValueChange = { cgpa = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "SKILLS",
            value = skills,
            enabled = isEditing,
            minLines = 2,
            onValueChange = { skills = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        ProfileField(
            label = "ABOUT YOU",
            value = about,
            enabled = isEditing,
            minLines = 4,
            onValueChange = { about = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isEditing) {
            Button(
                onClick = {
                    if (fullName.trim().isBlank()) {
                        message = "Please enter your full name."
                        messageIsError = true
                        return@Button
                    }
                    if (college.trim().isBlank()) {
                        message = "Please enter your college / university."
                        messageIsError = true
                        return@Button
                    }
                    if (course.trim().isBlank()) {
                        message = "Please enter your course / branch."
                        messageIsError = true
                        return@Button
                    }

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

                    firestore.collection("users")
                        .document(user.uid)
                        .set(profile, SetOptions.merge())
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
            ) {
                Text(if (isSaving) "SAVING..." else "SAVE CHANGES", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { isEditing = false; message = "" },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text("CANCEL")
            }
        } else {
            Button(
                onClick = {
                    message = ""
                    isEditing = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15151B)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("EDIT PROFILE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                color = if (messageIsError) Color(0xFFFF8A80) else Color(0xFFB9F6CA),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "Your profile is linked to your PRISM account, not to a single device.",
            color = Color.Gray,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(30.dp))
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
            containerColor = Color(0xFF111116)
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
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = Color(0xFF777780),
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