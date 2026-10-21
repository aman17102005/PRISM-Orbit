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
    val summary: PrismScoreSummary
) {
    val academics: Int get() = summary.breakdown.academics
    val dsa: Int get() = summary.breakdown.dsa
    val projects: Int get() = summary.breakdown.projects
    val internships: Int get() = summary.breakdown.internships
    val skills: Int get() = summary.breakdown.skills
    val achievements: Int get() = summary.breakdown.evidence
    val dsaSolved: Int get() = summary.dsaSolved
    val projectCount: Int get() = summary.projectCount
    val completedProjects: Int get() = summary.completedProjects
    val internshipCount: Int get() = summary.internshipCount
    val interviews: Int get() = summary.interviews
    val selected: Int get() = summary.selected
    val githubProfileAdded: Boolean get() = summary.githubProfileAdded

    // Same formula as every other screen: see PrismScoring.
    val overall: Int get() = summary.readiness
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
    PrismScoreLoader.load(
        firestore = firestore,
        uid = uid,
        onSuccess = { summary ->
            onSuccess(
                GrowthDashboardSummary(
                    growthScore = summary.readiness,
                    placementScore = summary.readiness,
                    internshipCount = summary.internshipCount
                )
            )
        },
        onError = onError
    )
}

/**
 * Loads the same persisted user data used by the other screens and calculates
 * the Growth score through PrismScoring.
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

        PrismScoreLoader.load(
            firestore = firestore,
            uid = uid,
            onSuccess = { summary ->
                val snapshot = GrowthSnapshot(summary)
                data = snapshot
                onGrowthChanged(snapshot.overall)
                loading = false
            },
            onError = { exception ->
                error = exception.message ?: "Unable to load growth data."
                loading = false
            }
        )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
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
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
            }

            Text(
                text = "LIVE",
                color = Color(0xFF65E572),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(15.dp)
                )
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↻  REFRESH GROWTH DATA",
                color = Color(0xFF00D9FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }
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
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
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
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
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