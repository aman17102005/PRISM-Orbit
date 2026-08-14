
package com.prismorbit.app

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlin.math.roundToInt

private data class GrowthSnapshot(
    val academics: Int,
    val dsa: Int,
    val projects: Int,
    val internships: Int,
    val skills: Int,
    val achievements: Int,
    val certifications: Int,
    val learning: Int,
    val dsaSolved: Int,
    val projectCount: Int,
    val completedProjects: Int,
    val internshipCount: Int,
    val interviews: Int,
    val selected: Int,
    val githubProfileAdded: Boolean
) {
    val overall: Int
        get() = weightedGrowthScore(
            academics = academics,
            dsa = dsa,
            projects = projects,
            internships = internships,
            skills = skills,
            achievements = achievements
        )
}

data class GrowthDashboardSummary(
    val growthScore: Int,
    val placementScore: Int,
    val internshipCount: Int
)

fun loadGrowthDashboardSummary(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (GrowthDashboardSummary) -> Unit,
    onError: (Exception) -> Unit
) {
    val userRef = firestore.collection("users").document(uid)
    val tasks = listOf(
        userRef.get(),
        userRef.collection("dsaProblems").get(),
        userRef.collection("projects").get(),
        userRef.collection("internships").get(),
        userRef.collection("placementSkills").get(),
        userRef.collection("placementAchievements").get(),
        userRef.collection("placementCertifications").get(),
        userRef.collection("placementLearning").get()
    )

    Tasks.whenAllSuccess<Any>(tasks)
        .addOnSuccessListener { results ->
            try {
                val snapshot = buildGrowthSnapshot(results)
                onSuccess(
                    GrowthDashboardSummary(
                        growthScore = snapshot.overall,
                        placementScore = placementReadinessScore(snapshot),
                        internshipCount = snapshot.internshipCount
                    )
                )
            } catch (exception: Exception) {
                onError(exception)
            }
        }
        .addOnFailureListener { exception -> onError(exception) }
}

/**
 * Loads the same persisted user data used by the other desktops and calculates
 * the Growth score without creating a second copy of that data.
 */
fun loadGrowthScore(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (Int) -> Unit,
    onError: (Exception) -> Unit
) {
    loadGrowthDashboardSummary(
        firestore = firestore,
        uid = uid,
        onSuccess = { summary -> onSuccess(summary.growthScore) },
        onError = onError
    )
}

private fun buildGrowthSnapshot(results: List<Any>): GrowthSnapshot {
    val userDocument = results[0] as DocumentSnapshot
    val dsaDocuments = (results[1] as QuerySnapshot).documents
    val projectDocuments = (results[2] as QuerySnapshot).documents
    val internshipDocuments = (results[3] as QuerySnapshot).documents
    val skillDocuments = (results[4] as QuerySnapshot).documents
    val achievementDocuments = (results[5] as QuerySnapshot).documents
    val certificationDocuments = (results[6] as QuerySnapshot).documents
    val learningDocuments = (results[7] as QuerySnapshot).documents
    val githubProfileAdded = userDocument.getString("githubProfileUrl").orEmpty().isNotBlank()

    val cgpa = userDocument.get("currentCgpa")
        .toString()
        .toFloatOrNull()
        ?.coerceIn(0f, 10f)
        ?: 0f

    val academics = (cgpa * 10f).roundToInt()

    val dsaWeightedScore = dsaDocuments.sumOf { document ->
        val storedScore = document.get("score").asFloatOrNull()
        val difficulty = document.getString("difficulty").orEmpty()
        val topic = document.getString("topic").orEmpty()
        (storedScore ?: growthProblemScore(difficulty, topic)).toDouble()
    }.toFloat()

    // Uses the same 1000-point DSA target already used by the app's DSA score.
    val dsa = (dsaWeightedScore / 1000f * 100f)
        .coerceIn(0f, 100f)
        .roundToInt()

    val projectCount = projectDocuments.size
    val completedProjects = projectDocuments.count { document ->
        val progress = document.get("progress").asFloatOrNull() ?: 0f
        val status = document.getString("status").orEmpty().uppercase()
        status == "COMPLETED" || progress >= 100f
    }

    val projectProgress = if (projectCount == 0) {
        0f
    } else {
        projectDocuments.sumOf { document ->
            (document.get("progress").asFloatOrNull() ?: 0f)
                .coerceIn(0f, 100f)
                .toDouble()
        }.toFloat() / projectCount
    }

    val projects = projectProgress.roundToInt()

    val internshipCount = internshipDocuments.size
    val interviews = internshipDocuments.count { document ->
        val status = document.getString("status").orEmpty().uppercase()
        status == "INTERVIEW" || status == "SELECTED"
    }
    val selected = internshipDocuments.count { document ->
        document.getString("status").orEmpty().uppercase() == "SELECTED"
    }

    val internships = internshipDevelopmentScore(
        total = internshipCount,
        interviews = interviews,
        selected = selected
    )

    val skillRatings = skillDocuments.mapNotNull { document ->
        document.get("rating").asFloatOrNull()
    }
    val skills = if (skillRatings.isEmpty()) {
        0
    } else {
        (skillRatings.average().toFloat() * 10f)
            .coerceIn(0f, 100f)
            .roundToInt()
    }

    val achievements = achievementDocuments.size
    val certifications = certificationDocuments.size
    val learning = learningDevelopmentScore(learningDocuments)

    val achievementScore = profileEvidenceScore(
        achievements = achievements,
        certifications = certifications,
        learningScore = learning
    )

    return GrowthSnapshot(
        academics = academics,
        dsa = dsa,
        projects = projects,
        internships = internships,
        skills = skills,
        achievements = achievementScore,
        certifications = certifications,
        learning = learning,
        dsaSolved = dsaDocuments.size,
        projectCount = projectCount,
        completedProjects = completedProjects,
        internshipCount = internshipCount,
        interviews = interviews,
        selected = selected,
        githubProfileAdded = githubProfileAdded
    )
}

private fun placementReadinessScore(snapshot: GrowthSnapshot): Int {
    return weightedGrowthScore(
        academics = snapshot.academics,
        dsa = snapshot.dsa,
        projects = snapshot.projects,
        internships = snapshot.internships,
        skills = snapshot.skills,
        achievements = snapshot.achievements
    )
}

private fun weightedGrowthScore(
    academics: Int,
    dsa: Int,
    projects: Int,
    internships: Int,
    skills: Int,
    achievements: Int
): Int {
    return (
            academics * 0.15f +
                    dsa * 0.25f +
                    projects * 0.20f +
                    internships * 0.15f +
                    skills * 0.15f +
                    achievements * 0.10f
            ).coerceIn(0f, 100f).roundToInt()
}

private fun internshipDevelopmentScore(
    total: Int,
    interviews: Int,
    selected: Int
): Int {
    if (total == 0) return 0

    val applicationSignal = (total.toFloat() / (total + 5f)) * 100f
    val interviewSignal = (interviews.toFloat() / total) * 100f
    val selectionSignal = (selected.toFloat() / total) * 100f

    return (
            applicationSignal * 0.45f +
                    interviewSignal * 0.35f +
                    selectionSignal * 0.20f
            ).coerceIn(0f, 100f).roundToInt()
}

private fun profileEvidenceScore(
    achievements: Int,
    certifications: Int,
    learningScore: Int
): Int {
    val achievementSignal = percentageFromCount(achievements)
    val certificationSignal = percentageFromCount(certifications)

    return (
            achievementSignal * 0.40f +
                    certificationSignal * 0.30f +
                    learningScore * 0.30f
            ).coerceIn(0f, 100f).roundToInt()
}

private fun percentageFromCount(count: Int): Float {
    if (count <= 0) return 0f
    return (100f * count / (count + 4f)).coerceIn(0f, 100f)
}

private fun learningDevelopmentScore(
    documents: List<DocumentSnapshot>
): Int {
    if (documents.isEmpty()) return 0

    val numericLevels = documents.mapNotNull { document ->
        document.get("level").asFloatOrNull()
    }

    if (numericLevels.isNotEmpty()) {
        return (numericLevels.average().toFloat() * 10f)
            .coerceIn(0f, 100f)
            .roundToInt()
    }

    return percentageFromCount(documents.size).roundToInt()
}

private fun Any?.asFloatOrNull(): Float? = when (this) {
    is Number -> toFloat()
    is String -> toFloatOrNull()
    else -> null
}

private fun growthProblemScore(
    difficulty: String,
    topic: String
): Float {
    val difficultyWeight = when (difficulty.trim()) {
        "Easy" -> 1f
        "Medium" -> 2f
        "Hard" -> 3f
        else -> 1f
    }

    val topicMultiplier = when (topic.trim()) {
        "Dynamic Programming" -> 1.40f
        "Graphs", "Backtracking", "Tries" -> 1.30f
        "Recursion", "Trees / BST", "Heap", "Greedy", "Bit Manipulation" -> 1.20f
        "Searching & Sorting", "Linked List", "Stack & Queue", "Hashing" -> 1.10f
        else -> 1.00f
    }

    return difficultyWeight * topicMultiplier
}

@Composable
fun GrowthTrackerScreen(
    onBack: () -> Unit,
    onGrowthChanged: (Int) -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var data by remember { mutableStateOf<GrowthSnapshot?>(null) }

    fun reload() {
        if (uid == null) {
            loading = false
            error = "No signed-in user found."
            return
        }

        loading = true
        error = ""

        val tasks = listOf(
            firestore.collection("users").document(uid).get(),
            firestore.collection("users").document(uid).collection("dsaProblems").get(),
            firestore.collection("users").document(uid).collection("projects").get(),
            firestore.collection("users").document(uid).collection("internships").get(),
            firestore.collection("users").document(uid).collection("placementSkills").get(),
            firestore.collection("users").document(uid).collection("placementAchievements").get(),
            firestore.collection("users").document(uid).collection("placementCertifications").get(),
            firestore.collection("users").document(uid).collection("placementLearning").get()
        )

        Tasks.whenAllSuccess<Any>(tasks)
            .addOnSuccessListener { results ->
                try {
                    val snapshot = buildGrowthSnapshot(results)
                    data = snapshot
                    onGrowthChanged(snapshot.overall)
                    loading = false
                } catch (exception: Exception) {
                    error = exception.message ?: "Unable to calculate growth."
                    loading = false
                }
            }
            .addOnFailureListener { exception ->
                error = exception.message ?: "Unable to load growth data."
                loading = false
            }
    }

    LaunchedEffect(uid) {
        reload()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF4FD8))
            }

            data == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (error.isBlank()) "No growth data available yet." else error,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            else -> GrowthContent(
                data = data!!,
                onBack = onBack,
                onRefresh = { reload() }
            )
        }
    }
}

@Composable
private fun GrowthContent(
    data: GrowthSnapshot,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val areas = listOf(
        "Academics" to data.academics,
        "DSA" to data.dsa,
        "Projects" to data.projects,
        "Internships" to data.internships,
        "Skills" to data.skills,
        "Achievements" to data.achievements
    )
    val weakest = areas.minByOrNull { it.second } ?: ("—" to 0)
    val strongest = areas.maxByOrNull { it.second } ?: ("—" to 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 40.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "GROWTH",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "CONNECTED DEVELOPMENT",
                    color = Color(0xFFFF4FD8),
                    fontSize = 8.sp,
                    letterSpacing = 1.6.sp
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OVERALL GROWTH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 13.dp.toPx()
                        drawArc(
                            color = Color(0xFF292930),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF4FD8),
                                    Color(0xFFB76CFF),
                                    Color(0xFF00D9FF),
                                    Color(0xFFFF4FD8)
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * (data.overall / 100f),
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${data.overall}%",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = growthLabel(data.overall),
                            color = growthColor(data.overall),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "CONNECTED AREAS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        areas.forEach { (title, score) ->
            GrowthMetric(title = title, score = score)
            Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "ACTIVITY FROM EXISTING DESKTOPS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            GrowthStat("DSA", data.dsaSolved.toString(), Modifier.weight(1f))
            GrowthStat("PROJECTS", data.projectCount.toString(), Modifier.weight(1f))
            GrowthStat("INTERNSHIPS", data.internshipCount.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            GrowthStat("COMPLETED", data.completedProjects.toString(), Modifier.weight(1f))
            GrowthStat("INTERVIEWS", data.interviews.toString(), Modifier.weight(1f))
            GrowthStat("SELECTED", data.selected.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GITHUB PROFILE",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Placement evidence",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp
                    )
                }
                Text(
                    text = if (data.githubProfileAdded) "ADDED" else "NOT ADDED",
                    color = if (data.githubProfileAdded) Color(0xFF65E572) else Color(0xFFFF7B72),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "✦  GROWTH INSIGHT",
                    color = Color(0xFFFF4FD8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${strongest.first} is currently your strongest connected area. ${weakest.first} needs the most attention based on the data already stored in PRISMOrbit.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Growth is calculated from your existing desktop data. Nothing is entered again here.",
            color = Color(0xFF66666F),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "REFRESH",
            color = Color(0xFF00D9FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onRefresh() }
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun GrowthMetric(
    title: String,
    score: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$score%",
                    color = growthColor(score),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((score / 100f).coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(growthColor(score), RoundedCornerShape(50.dp))
                )
            }
        }
    }
}

@Composable
private fun GrowthStat(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun growthLabel(score: Int): String = when {
    score >= 90 -> "EXCEPTIONAL"
    score >= 80 -> "EXCELLENT"
    score >= 65 -> "STRONG"
    score >= 50 -> "DEVELOPING"
    else -> "NEEDS WORK"
}

private fun growthColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF65E572)
    score >= 65 -> Color(0xFF00D9FF)
    score >= 50 -> Color(0xFFFFD23F)
    else -> Color(0xFFFF7B72)
}
