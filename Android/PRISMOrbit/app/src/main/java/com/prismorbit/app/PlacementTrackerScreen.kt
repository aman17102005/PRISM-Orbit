package com.prismorbit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID
import kotlin.math.roundToInt

// =========================================================
// PLACEMENT DATA
// =========================================================

data class PlacementSkillRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Technical",
    val rating: Int = 5
)

data class PlacementAchievementRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val type: String = "Competition",
    val position: String = "",
    val year: String = ""
)

data class PlacementCertificationRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val issuer: String = "",
    val date: String = "",
    val credentialUrl: String = ""
)

data class PlacementLearningRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Technical",
    val level: Int = 5
)

private data class PlacementSourceScores(
    val academic: Int = 0,
    val dsa: Int = 0,
    val projects: Int = 0,
    val internships: Int = 0,
    val skills: Int = 0,
    val achievements: Int = 0
) {
    val readiness: Int
        get() = (
                academic * 0.15f +
                        dsa * 0.25f +
                        projects * 0.20f +
                        internships * 0.15f +
                        skills * 0.15f +
                        achievements * 0.10f
                ).roundToInt().coerceIn(0, 100)
}

private fun placementCollection(
    firestore: FirebaseFirestore,
    uid: String,
    name: String
) = firestore.collection("users").document(uid).collection(name)

private fun PlacementSkillRecord.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "category" to category,
    "rating" to rating
)

private fun PlacementAchievementRecord.toMap() = mapOf(
    "id" to id,
    "title" to title,
    "type" to type,
    "position" to position,
    "year" to year
)

private fun PlacementCertificationRecord.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "issuer" to issuer,
    "date" to date,
    "credentialUrl" to credentialUrl
)

private fun PlacementLearningRecord.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "category" to category,
    "level" to level
)

private fun documentToSkill(doc: com.google.firebase.firestore.DocumentSnapshot) =
    PlacementSkillRecord(
        id = doc.getString("id") ?: doc.id,
        name = doc.getString("name").orEmpty(),
        category = doc.getString("category") ?: "Technical",
        rating = ((doc.get("rating") as? Number)?.toInt() ?: 5).coerceIn(1, 10)
    ).takeIf { it.name.isNotBlank() }

private fun documentToAchievement(doc: com.google.firebase.firestore.DocumentSnapshot) =
    PlacementAchievementRecord(
        id = doc.getString("id") ?: doc.id,
        title = doc.getString("title").orEmpty(),
        type = doc.getString("type") ?: "Competition",
        position = doc.getString("position").orEmpty(),
        year = doc.getString("year").orEmpty()
    ).takeIf { it.title.isNotBlank() }

private fun documentToCertification(doc: com.google.firebase.firestore.DocumentSnapshot) =
    PlacementCertificationRecord(
        id = doc.getString("id") ?: doc.id,
        name = doc.getString("name").orEmpty(),
        issuer = doc.getString("issuer").orEmpty(),
        date = doc.getString("date").orEmpty(),
        credentialUrl = doc.getString("credentialUrl").orEmpty()
    ).takeIf { it.name.isNotBlank() }

private fun documentToLearning(doc: com.google.firebase.firestore.DocumentSnapshot) =
    PlacementLearningRecord(
        id = doc.getString("id") ?: doc.id,
        name = doc.getString("name").orEmpty(),
        category = doc.getString("category") ?: "Technical",
        level = ((doc.get("level") as? Number)?.toInt() ?: 5).coerceIn(1, 10)
    ).takeIf { it.name.isNotBlank() }

private fun calculatePlacementSkillScore(items: List<PlacementSkillRecord>): Int =
    if (items.isEmpty()) 0
    else (items.map { it.rating }.average() * 10).roundToInt().coerceIn(0, 100)

private fun calculatePlacementAchievementScore(
    achievements: List<PlacementAchievementRecord>,
    certifications: List<PlacementCertificationRecord>,
    learning: List<PlacementLearningRecord>
): Int {
    val achievementPart = (achievements.size.coerceAtMost(5) / 5f) * 40f
    val certificationPart = (certifications.size.coerceAtMost(5) / 5f) * 30f
    val learningPart =
        if (learning.isEmpty()) 0f
        else (learning.map { it.level }.average().toFloat() / 10f) * 30f

    return (achievementPart + certificationPart + learningPart)
        .roundToInt()
        .coerceIn(0, 100)
}

private fun calculatePlacementDsaScore(snapshot: com.google.firebase.firestore.QuerySnapshot): Int {
    if (snapshot.isEmpty) return 0

    val weighted = snapshot.documents.sumOf { doc ->
        (doc.get("score") as? Number)?.toDouble() ?: 0.0
    }

    return (weighted / 1000.0 * 100.0)
        .coerceIn(0.0, 100.0)
        .roundToInt()
}

private fun calculatePlacementProjectScore(snapshot: com.google.firebase.firestore.QuerySnapshot): Int {
    if (snapshot.isEmpty) return 0

    val projects = snapshot.documents
    val averageProgress = projects.map {
        when (val raw = it.get("progress")) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }.coerceIn(0.0, 100.0)
    }.average()

    val size = projects.size.toFloat()
    val completed = projects.count {
        it.getString("status")?.uppercase() == "COMPLETED"
    }.toFloat() / size * 20f

    val tech = projects.count {
        (it.getString("techStack") ?: it.getString("codingLanguage") ?: "").isNotBlank()
    }.toFloat() / size * 15f

    val github = projects.count {
        it.getString("githubUrl").orEmpty().isNotBlank()
    }.toFloat() / size * 15f

    val live = projects.count {
        it.getString("liveUrl").orEmpty().isNotBlank()
    }.toFloat() / size * 15f

    val tasks = projects.count {
        (it.get("tasks") as? List<*>)?.isNotEmpty() == true
    }.toFloat() / size * 5f

    val progress = averageProgress * 0.30f

    return (completed + tech + github + live + tasks + progress)
        .roundToInt()
        .coerceIn(0, 100)
}

private fun calculatePlacementInternshipScore(
    snapshot: com.google.firebase.firestore.QuerySnapshot,
    projectCount: Int
): Int {
    if (snapshot.isEmpty && projectCount == 0) return 0

    val internships = snapshot.documents
    val applicationActivity =
        (internships.size / 10f).coerceIn(0f, 1f) * 25f

    val interviewExposure =
        (
                internships.count {
                    val status = it.getString("status")?.uppercase()
                    status == "INTERVIEW" || status == "SELECTED"
                } / 5f
                ).coerceIn(0f, 1f) * 20f

    val outcomeExperience =
        (
                internships.count {
                    it.getString("status")?.uppercase() == "SELECTED"
                } / 2f
                ).coerceIn(0f, 1f) * 20f

    val projectFoundation =
        (projectCount / 4f).coerceIn(0f, 1f) * 20f

    val completeProfiles = internships.count {
        it.getString("company").orEmpty().isNotBlank() &&
                it.getString("role").orEmpty().isNotBlank() &&
                it.getString("applicationDate").orEmpty().isNotBlank() &&
                it.getString("jobUrl").orEmpty().isNotBlank()
    }

    val profileCompleteness =
        if (internships.isEmpty()) 0f
        else completeProfiles.toFloat() / internships.size * 15f

    return (
            applicationActivity +
                    interviewExposure +
                    outcomeExperience +
                    projectFoundation +
                    profileCompleteness
            )
        .roundToInt()
        .coerceIn(0, 100)
}

@Composable
fun PlacementTrackerScreen(
    onBack: () -> Unit,
    onReadinessChanged: (Int) -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var skills by remember { mutableStateOf<List<PlacementSkillRecord>>(emptyList()) }
    var achievements by remember { mutableStateOf<List<PlacementAchievementRecord>>(emptyList()) }
    var certifications by remember { mutableStateOf<List<PlacementCertificationRecord>>(emptyList()) }
    var learning by remember { mutableStateOf<List<PlacementLearningRecord>>(emptyList()) }
    var githubProfileUrl by remember { mutableStateOf("") }
    var githubSaving by remember { mutableStateOf(false) }

    var scores by remember { mutableStateOf(PlacementSourceScores()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var editor by remember { mutableStateOf<String?>(null) }
    var editingSkill by remember { mutableStateOf<PlacementSkillRecord?>(null) }
    var editingAchievement by remember { mutableStateOf<PlacementAchievementRecord?>(null) }
    var editingCertification by remember { mutableStateOf<PlacementCertificationRecord?>(null) }
    var editingLearning by remember { mutableStateOf<PlacementLearningRecord?>(null) }

    fun recomputeAchievementScore() {
        scores = scores.copy(
            achievements = calculatePlacementAchievementScore(
                achievements,
                certifications,
                learning
            )
        )
    }

    fun refresh() {
        if (uid == null) {
            loading = false
            errorMessage = "No signed-in user found."
            return
        }

        loading = true
        errorMessage = ""

        var finished = 0
        var academic = 0
        var dsa = 0
        var projects = 0
        var projectCountForInternship = 0
        var internshipSnapshot: com.google.firebase.firestore.QuerySnapshot? = null
        var internships = 0

        fun recalculateInternshipScore() {
            internshipSnapshot?.let {
                internships = calculatePlacementInternshipScore(it, projectCountForInternship)
            }
        }

        fun finishedOne() {
            finished += 1
            if (finished == 4) {
                scores = scores.copy(
                    academic = academic,
                    dsa = dsa,
                    projects = projects,
                    internships = internships
                )
                loading = false
            }
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                githubProfileUrl = doc.getString("githubProfileUrl").orEmpty()
                val cgpa = doc.get("currentCgpa")?.toString()?.toFloatOrNull() ?: 0f
                academic = (cgpa / 10f * 100f).roundToInt().coerceIn(0, 100)
                finishedOne()
            }
            .addOnFailureListener {
                finishedOne()
            }

        firestore.collection("users").document(uid).collection("dsaProblems").get()
            .addOnSuccessListener {
                dsa = calculatePlacementDsaScore(it)
                finishedOne()
            }
            .addOnFailureListener { finishedOne() }

        firestore.collection("users").document(uid).collection("projects").get()
            .addOnSuccessListener {
                projectCountForInternship = it.size()
                projects = calculatePlacementProjectScore(it)
                recalculateInternshipScore()
                finishedOne()
            }
            .addOnFailureListener { finishedOne() }

        firestore.collection("users").document(uid).collection("internships").get()
            .addOnSuccessListener {
                internshipSnapshot = it
                recalculateInternshipScore()
                finishedOne()
            }
            .addOnFailureListener { finishedOne() }

        placementCollection(firestore, uid, "placementSkills").get()
            .addOnSuccessListener { snap ->
                skills = snap.documents.mapNotNull(::documentToSkill)
                scores = scores.copy(skills = calculatePlacementSkillScore(skills))
            }

        placementCollection(firestore, uid, "placementAchievements").get()
            .addOnSuccessListener { snap ->
                achievements = snap.documents.mapNotNull(::documentToAchievement)
                recomputeAchievementScore()
            }

        placementCollection(firestore, uid, "placementCertifications").get()
            .addOnSuccessListener { snap ->
                certifications = snap.documents.mapNotNull(::documentToCertification)
                recomputeAchievementScore()
            }

        placementCollection(firestore, uid, "placementLearning").get()
            .addOnSuccessListener { snap ->
                learning = snap.documents.mapNotNull(::documentToLearning)
                recomputeAchievementScore()
            }
    }

    fun recomputeAndNotify() {
        val updated = scores.readiness
        onReadinessChanged(updated)
    }

    LaunchedEffect(uid) {
        refresh()
    }

    LaunchedEffect(scores) {
        recomputeAndNotify()
    }

    val weakAreas = listOf(
        "Academics" to scores.academic,
        "DSA" to scores.dsa,
        "Projects" to scores.projects,
        "Internships" to scores.internships,
        "Skills" to scores.skills,
        "Achievements" to scores.achievements
    ).sortedBy { it.second }.take(3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (editor != null) {
            when (editor) {
                "SKILL" -> PlacementSkillEditor(
                    initial = editingSkill,
                    saving = saving,
                    onBack = {
                        if (!saving) {
                            editor = null
                            editingSkill = null
                        }
                    },
                    onSave = { item ->
                        savePlacementItem(
                            firestore,
                            uid,
                            "placementSkills",
                            item.id,
                            item.toMap(),
                            onSuccess = {
                                skills = skills.upsert(item)
                                scores = scores.copy(skills = calculatePlacementSkillScore(skills))
                                saving = false
                                editor = null
                                editingSkill = null
                            },
                            onError = {
                                saving = false
                                errorMessage = it.message ?: "Unable to save skill."
                            }
                        )
                    },
                    setSaving = { saving = it }
                )

                "ACHIEVEMENT" -> PlacementAchievementEditor(
                    initial = editingAchievement,
                    saving = saving,
                    onBack = {
                        if (!saving) {
                            editor = null
                            editingAchievement = null
                        }
                    },
                    onSave = { item ->
                        savePlacementItem(
                            firestore,
                            uid,
                            "placementAchievements",
                            item.id,
                            item.toMap(),
                            onSuccess = {
                                achievements = achievements.upsert(item)
                                scores = scores.copy(
                                    achievements = calculatePlacementAchievementScore(
                                        achievements,
                                        certifications,
                                        learning
                                    )
                                )
                                saving = false
                                editor = null
                                editingAchievement = null
                            },
                            onError = {
                                saving = false
                                errorMessage = it.message ?: "Unable to save achievement."
                            }
                        )
                    },
                    setSaving = { saving = it }
                )

                "CERTIFICATION" -> PlacementCertificationEditor(
                    initial = editingCertification,
                    saving = saving,
                    onBack = {
                        if (!saving) {
                            editor = null
                            editingCertification = null
                        }
                    },
                    onSave = { item ->
                        savePlacementItem(
                            firestore,
                            uid,
                            "placementCertifications",
                            item.id,
                            item.toMap(),
                            onSuccess = {
                                certifications = certifications.upsert(item)
                                scores = scores.copy(
                                    achievements = calculatePlacementAchievementScore(
                                        achievements,
                                        certifications,
                                        learning
                                    )
                                )
                                saving = false
                                editor = null
                                editingCertification = null
                            },
                            onError = {
                                saving = false
                                errorMessage = it.message ?: "Unable to save certification."
                            }
                        )
                    },
                    setSaving = { saving = it }
                )

                "LEARNING" -> PlacementLearningEditor(
                    initial = editingLearning,
                    saving = saving,
                    onBack = {
                        if (!saving) {
                            editor = null
                            editingLearning = null
                        }
                    },
                    onSave = { item ->
                        savePlacementItem(
                            firestore,
                            uid,
                            "placementLearning",
                            item.id,
                            item.toMap(),
                            onSuccess = {
                                learning = learning.upsert(item)
                                scores = scores.copy(
                                    achievements = calculatePlacementAchievementScore(
                                        achievements,
                                        certifications,
                                        learning
                                    )
                                )
                                saving = false
                                editor = null
                                editingLearning = null
                            },
                            onError = {
                                saving = false
                                errorMessage = it.message ?: "Unable to save learning."
                            }
                        )
                    },
                    setSaving = { saving = it }
                )
            }
        } else {
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
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "‹",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light
                        )
                    }

                    Spacer(Modifier.size(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PLACEMENT",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "BUILD • MEASURE • GET READY",
                            color = Color(0xFFFF7B72),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF2A1820),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "READINESS",
                            color = Color(0xFFFF7B72),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "PLACEMENT READINESS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (loading) "CALCULATING..." else "${scores.readiness} / 100",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        )
                        if (!loading) {
                            Text(
                                placementLabelFinal(scores.readiness),
                                color = placementColorFinal(scores.readiness),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(13.dp))
                            PlacementProgress(scores.readiness / 100f)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "Overall readiness across your PRISM profile",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "READINESS BREAKDOWN",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.size(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0xFF292930))
                    )
                }
                Spacer(Modifier.height(11.dp))

                ReadinessFinalRow("ACADEMICS", scores.academic, "15% weight")
                ReadinessFinalRow("DSA", scores.dsa, "25% weight")
                ReadinessFinalRow("PROJECTS", scores.projects, "20% weight")
                ReadinessFinalRow("INTERNSHIPS", scores.internships, "15% weight")
                ReadinessFinalRow("SKILLS", scores.skills, "15% weight")
                ReadinessFinalRow("ACHIEVEMENTS", scores.achievements, "10% weight")

                Spacer(Modifier.height(20.dp))
                Text(
                    "⚠ AREAS TO IMPROVE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Text(
                            "LOWEST CURRENT READINESS AREAS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(Modifier.height(7.dp))

                        weakAreas.forEachIndexed { index, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            Color(0xFFFF7B72),
                                            RoundedCornerShape(50)
                                        )
                                )
                                Spacer(Modifier.size(9.dp))
                                Text(
                                    "${index + 1}. ${pair.first}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${pair.second}%",
                                    color = Color(0xFFFF7B72),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "🧠 SKILLS & COMPETENCIES",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))
                PlacementCollectionFinal(
                    emptyText = "Add technical, Core CS, communication, soft-skill and aptitude ratings.",
                    addText = "+ ADD SKILL",
                    empty = skills.isEmpty(),
                    onAdd = {
                        editingSkill = null
                        editor = "SKILL"
                        errorMessage = ""
                    }
                ) {
                    skills.forEach { item ->
                        PlacementItemFinal(
                            item.name,
                            "${item.category} • ${item.rating}/10",
                            onEdit = {
                                editingSkill = item
                                editor = "SKILL"
                            },
                            onDelete = {
                                deletePlacementItem(
                                    firestore, uid, "placementSkills", item.id,
                                    onSuccess = {
                                        skills = skills.filterNot { it.id == item.id }
                                        scores = scores.copy(
                                            skills = calculatePlacementSkillScore(skills)
                                        )
                                    },
                                    onError = {
                                        errorMessage = it.message ?: "Unable to delete skill."
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "🌐 GITHUB PROFILE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        OutlinedTextField(
                            value = githubProfileUrl,
                            onValueChange = { githubProfileUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("GitHub Profile URL") },
                            placeholder = { Text("https://github.com/username") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (uid != null) {
                                    githubSaving = true
                                    firestore.collection("users").document(uid)
                                        .set(
                                            mapOf("githubProfileUrl" to githubProfileUrl.trim()),
                                            SetOptions.merge()
                                        )
                                        .addOnSuccessListener {
                                            githubProfileUrl = githubProfileUrl.trim()
                                            githubSaving = false
                                        }
                                        .addOnFailureListener {
                                            errorMessage = it.message ?: "Unable to save GitHub profile."
                                            githubSaving = false
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uid != null && !githubSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (githubSaving) "SAVING..." else "SAVE GITHUB PROFILE")
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("🏆 ACHIEVEMENTS", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                PlacementCollectionFinal(
                    emptyText = "Add hackathon wins, ranks, competitions and other achievements.",
                    addText = "+ ADD ACHIEVEMENT",
                    empty = achievements.isEmpty(),
                    onAdd = {
                        editingAchievement = null
                        editor = "ACHIEVEMENT"
                    }
                ) {
                    achievements.forEach { item ->
                        PlacementItemFinal(
                            item.title,
                            "${item.type} • ${item.position.ifBlank { "Recognition" }} • ${item.year}",
                            onEdit = {
                                editingAchievement = item
                                editor = "ACHIEVEMENT"
                            },
                            onDelete = {
                                deletePlacementItem(
                                    firestore, uid, "placementAchievements", item.id,
                                    onSuccess = {
                                        achievements = achievements.filterNot { it.id == item.id }
                                        scores = scores.copy(
                                            achievements = calculatePlacementAchievementScore(
                                                achievements, certifications, learning
                                            )
                                        )
                                    },
                                    onError = {
                                        errorMessage = it.message ?: "Unable to delete achievement."
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("📜 CERTIFICATIONS", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                PlacementCollectionFinal(
                    emptyText = "Add certifications and credential links.",
                    addText = "+ ADD CERTIFICATION",
                    empty = certifications.isEmpty(),
                    onAdd = {
                        editingCertification = null
                        editor = "CERTIFICATION"
                    }
                ) {
                    certifications.forEach { item ->
                        PlacementItemFinal(
                            item.name,
                            "${item.issuer} • ${item.date.ifBlank { "Date not added" }}",
                            onEdit = {
                                editingCertification = item
                                editor = "CERTIFICATION"
                            },
                            onDelete = {
                                deletePlacementItem(
                                    firestore, uid, "placementCertifications", item.id,
                                    onSuccess = {
                                        certifications = certifications.filterNot { it.id == item.id }
                                        scores = scores.copy(
                                            achievements = calculatePlacementAchievementScore(
                                                achievements, certifications, learning
                                            )
                                        )
                                    },
                                    onError = {
                                        errorMessage = it.message ?: "Unable to delete certification."
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("🚀 ADDITIONAL LEARNING", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                PlacementCollectionFinal(
                    emptyText = "Track communication, aptitude, leadership and technical learning.",
                    addText = "+ ADD LEARNING",
                    empty = learning.isEmpty(),
                    onAdd = {
                        editingLearning = null
                        editor = "LEARNING"
                    }
                ) {
                    learning.forEach { item ->
                        PlacementItemFinal(
                            item.name,
                            "${item.category} • ${item.level}/10",
                            onEdit = {
                                editingLearning = item
                                editor = "LEARNING"
                            },
                            onDelete = {
                                deletePlacementItem(
                                    firestore, uid, "placementLearning", item.id,
                                    onSuccess = {
                                        learning = learning.filterNot { it.id == item.id }
                                        scores = scores.copy(
                                            achievements = calculatePlacementAchievementScore(
                                                achievements, certifications, learning
                                            )
                                        )
                                    },
                                    onError = {
                                        errorMessage = it.message ?: "Unable to delete learning."
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("✦ PRISM PLACEMENT INSIGHT", color = Color(0xFFB76CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15151B))
                ) {
                    Text(
                        generateFinalPlacementInsight(scores),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(17.dp)
                    )
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(errorMessage, color = Color(0xFFFF6B6B), fontSize = 10.sp)
                }

                Spacer(Modifier.height(25.dp))
            }
        }
    }
}

private fun List<PlacementSkillRecord>.upsert(item: PlacementSkillRecord): List<PlacementSkillRecord> =
    filterNot { it.id == item.id } + item

private fun List<PlacementAchievementRecord>.upsert(item: PlacementAchievementRecord): List<PlacementAchievementRecord> =
    filterNot { it.id == item.id } + item

private fun List<PlacementCertificationRecord>.upsert(item: PlacementCertificationRecord): List<PlacementCertificationRecord> =
    filterNot { it.id == item.id } + item

private fun List<PlacementLearningRecord>.upsert(item: PlacementLearningRecord): List<PlacementLearningRecord> =
    filterNot { it.id == item.id } + item

private fun savePlacementItem(
    firestore: FirebaseFirestore,
    uid: String?,
    collection: String,
    id: String,
    data: Map<String, Any>,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    if (uid == null) {
        onError(IllegalStateException("No signed-in user found."))
        return
    }
    firestore.collection("users").document(uid).collection(collection).document(id)
        .set(data, SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onError)
}

private fun deletePlacementItem(
    firestore: FirebaseFirestore,
    uid: String?,
    collection: String,
    id: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    if (uid == null) {
        onError(IllegalStateException("No signed-in user found."))
        return
    }
    firestore.collection("users").document(uid).collection(collection).document(id)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onError)
}

@Composable
private fun PlacementProgress(value: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(9.dp)
            .background(Color(0xFF25252C), RoundedCornerShape(50.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(9.dp)
                .background(Color(0xFFB76CFF), RoundedCornerShape(50.dp))
        )
    }
}

@Composable
private fun ReadinessFinalRow(title: String, score: Int, weight: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("$score% • $weight", color = Color(0xFFB76CFF), fontSize = 8.sp)
        }
        Spacer(Modifier.height(5.dp))
        PlacementProgress(score / 100f)
    }
}

@Composable
private fun PlacementCollectionFinal(
    emptyText: String,
    addText: String,
    empty: Boolean,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(17.dp)) {
            if (empty) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            } else {
                content()
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(addText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlacementItemFinal(
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(9.dp)
                ) { Text("EDIT", fontSize = 7.sp) }
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(9.dp)
                ) { Text("DEL", fontSize = 7.sp) }
            }
        }
    }
}

@Composable
private fun PlacementSkillEditor(
    initial: PlacementSkillRecord?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (PlacementSkillRecord) -> Unit,
    setSaving: (Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Technical") }
    var rating by remember { mutableStateOf(initial?.rating?.toString() ?: "5") }
    var error by remember { mutableStateOf("") }

    PlacementEditorShell(
        title = if (initial == null) "ADD SKILL" else "EDIT SKILL",
        subtitle = "SKILLS & COMPETENCIES",
        onBack = onBack
    ) {
        OutlinedTextField(name, { name = it; error = "" }, Modifier.fillMaxWidth(), label = { Text("Skill Name") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        listOf("Technical", "Core CS", "Soft Skill", "Communication", "Aptitude").forEach {
            PlacementChoice(it, category == it) { category = it }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(rating, { rating = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Skill Rating (1-10)") }, singleLine = true)
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF6B6B), fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val r = rating.toIntOrNull()
                if (name.isBlank() || r == null || r !in 1..10) {
                    error = "Enter a skill and a rating from 1 to 10."
                } else {
                    setSaving(true)
                    onSave(PlacementSkillRecord(initial?.id ?: UUID.randomUUID().toString(), name.trim(), category, r))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF))
        ) { Text(if (saving) "SAVING..." else "SAVE SKILL") }
    }
}

@Composable
private fun PlacementAchievementEditor(
    initial: PlacementAchievementRecord?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (PlacementAchievementRecord) -> Unit,
    setSaving: (Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Competition") }
    var position by remember { mutableStateOf(initial?.position ?: "") }
    var year by remember { mutableStateOf(initial?.year ?: "") }

    PlacementEditorShell(
        title = if (initial == null) "ADD ACHIEVEMENT" else "EDIT ACHIEVEMENT",
        subtitle = "ACHIEVEMENTS",
        onBack = onBack
    ) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Achievement") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        listOf("Competition", "Academic Rank", "Hackathon", "Other").forEach {
            PlacementChoice(it, type == it) { type = it }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(position, { position = it }, Modifier.fillMaxWidth(), label = { Text("Position / Recognition") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(year, { year = it }, Modifier.fillMaxWidth(), label = { Text("Year") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (title.isBlank()) return@Button
                setSaving(true)
                onSave(PlacementAchievementRecord(initial?.id ?: UUID.randomUUID().toString(), title.trim(), type, position.trim(), year.trim()))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF))
        ) { Text(if (saving) "SAVING..." else "SAVE ACHIEVEMENT") }
    }
}

@Composable
private fun PlacementCertificationEditor(
    initial: PlacementCertificationRecord?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (PlacementCertificationRecord) -> Unit,
    setSaving: (Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var issuer by remember { mutableStateOf(initial?.issuer ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var link by remember { mutableStateOf(initial?.credentialUrl ?: "") }

    PlacementEditorShell(
        title = if (initial == null) "ADD CERTIFICATION" else "EDIT CERTIFICATION",
        subtitle = "CERTIFICATIONS",
        onBack = onBack
    ) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Certification") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(issuer, { issuer = it }, Modifier.fillMaxWidth(), label = { Text("Issuing Organization") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Date") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth(), label = { Text("Credential / Certificate Link") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (name.isBlank()) return@Button
                setSaving(true)
                onSave(PlacementCertificationRecord(initial?.id ?: UUID.randomUUID().toString(), name.trim(), issuer.trim(), date.trim(), link.trim()))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF))
        ) { Text(if (saving) "SAVING..." else "SAVE CERTIFICATION") }
    }
}

@Composable
private fun PlacementLearningEditor(
    initial: PlacementLearningRecord?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (PlacementLearningRecord) -> Unit,
    setSaving: (Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Technical") }
    var level by remember { mutableStateOf(initial?.level?.toString() ?: "5") }

    PlacementEditorShell(
        title = if (initial == null) "ADD LEARNING" else "EDIT LEARNING",
        subtitle = "ADDITIONAL LEARNING",
        onBack = onBack
    ) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Skill / Learning") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        listOf("Technical", "Communication", "Aptitude", "Leadership", "Other").forEach {
            PlacementChoice(it, category == it) { category = it }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(level, { level = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Current Level (1-10)") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (name.isBlank()) return@Button
                val l = level.toIntOrNull()?.coerceIn(1, 10) ?: 5
                setSaving(true)
                onSave(PlacementLearningRecord(initial?.id ?: UUID.randomUUID().toString(), name.trim(), category, l))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF))
        ) { Text(if (saving) "SAVING..." else "SAVE LEARNING") }
    }
}

@Composable
private fun PlacementEditorShell(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.size(10.dp))
            Column {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFFFF7B72), fontSize = 8.sp, letterSpacing = 1.5.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        content()
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PlacementChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF24163A))
        else ButtonDefaults.outlinedButtonColors(),
        shape = RoundedCornerShape(12.dp)
    ) { Text(text) }
}

private fun placementLabelFinal(score: Int): String = when {
    score >= 90 -> "EXCEPTIONAL"
    score >= 80 -> "STRONG"
    score >= 65 -> "GOOD"
    score >= 50 -> "DEVELOPING"
    else -> "NEEDS WORK"
}

private fun placementColorFinal(score: Int): Color = when {
    score >= 80 -> Color(0xFF65E572)
    score >= 65 -> Color(0xFF00D9FF)
    score >= 50 -> Color(0xFFFFD23F)
    else -> Color(0xFFFF7B72)
}

private fun generateFinalPlacementInsight(scores: PlacementSourceScores): String = when {
    scores.readiness < 50 ->
        "Your placement profile is still developing. Build DSA consistency, strengthen your project portfolio and add relevant skills."
    scores.dsa < 60 ->
        "DSA is currently your biggest technical gap. Focus on consistent problem solving, especially medium and hard problems."
    scores.projects < 60 ->
        "Your project portfolio needs more depth. Strengthen a few real projects with clear documentation, GitHub links and measurable progress."
    scores.internships < 60 ->
        "Your internship profile can improve through more relevant applications, interview exposure and complete application records."
    scores.skills < 60 ->
        "Your skills profile needs broader coverage. Add core CS, aptitude, communication and technical skills with honest ratings."
    scores.achievements < 60 ->
        "Your achievements profile is the current gap. Add meaningful certifications, competitions and structured learning as you complete them."
    else ->
        "Your profile is becoming placement-ready. Keep improving the weakest area and maintain consistency across DSA, projects and practical experience."
}