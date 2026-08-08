package com.prismorbit.app

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
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


data class ProjectTask(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val title: String,
    val completed: Boolean = false
)

data class ProjectItem(
    val id: Long = System.currentTimeMillis(),
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


data class InternshipItem(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val company: String,
    val role: String,
    val location: String,
    val applicationDate: String,
    val interviewDate: String,
    val offerDate: String,
    val stipend: String,
    val status: String,
    val jobUrl: String,
    val notes: String
)


data class PlacementSkill(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val name: String,
    val category: String,
    val rating: Int
)

data class PlacementAchievement(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val title: String,
    val type: String,
    val position: String,
    val year: String
)

data class PlacementCertification(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val name: String,
    val issuer: String,
    val date: String,
    val credentialUrl: String
)

data class PlacementLearning(
    val id: Long = System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000),
    val name: String,
    val category: String,
    val level: Int
)


// =========================================================
// HOME SCREEN
// =========================================================

@Composable
fun HomeScreen() {

    var showCgpaScreen by remember {
        mutableStateOf(false)
    }

    var showAcademicsScreen by remember {
        mutableStateOf(false)
    }

    var returnToAcademics by remember {
        mutableStateOf(false)
    }

    var showDsaScreen by remember {
        mutableStateOf(false)
    }

    var showProjectsScreen by remember {
        mutableStateOf(false)
    }

    val projects = remember {
        mutableStateListOf<ProjectItem>()
    }

    var showInternshipsScreen by remember {
        mutableStateOf(false)
    }

    val internships = remember {
        mutableStateListOf<InternshipItem>()
    }

    var showPlacementScreen by remember {
        mutableStateOf(false)
    }

    var showGrowthScreen by remember {
        mutableStateOf(false)
    }

    val placementSkills = remember { mutableStateListOf<PlacementSkill>() }
    val placementAchievements = remember { mutableStateListOf<PlacementAchievement>() }
    val placementCertifications = remember { mutableStateListOf<PlacementCertification>() }
    val placementLearning = remember { mutableStateListOf<PlacementLearning>() }

    val academicEvents = remember {
        mutableStateListOf<AcademicEvent>()
    }

    /*
     * DSA problems are kept in memory for now.
     * Later we will connect this to persistent storage.
     */

    val dsaProblems = remember {
        mutableStateListOf(
            DSAProblem(
                name = "Two Sum",
                topic = "Arrays",
                difficulty = "Easy",
                score = 1.0f
            ),
            DSAProblem(
                name = "Binary Search",
                topic = "Searching & Sorting",
                difficulty = "Easy",
                score = 1.2f
            ),
            DSAProblem(
                name = "LRU Cache",
                topic = "Hashing",
                difficulty = "Medium",
                score = 4.0f
            ),
            DSAProblem(
                name = "Number of Islands",
                topic = "Graphs",
                difficulty = "Medium",
                score = 2.6f
            ),
            DSAProblem(
                name = "Word Ladder",
                topic = "Graphs",
                difficulty = "Hard",
                score = 3.9f
            )
        )
    }

    if (showAcademicsScreen) {

        AcademicsScreen(
            currentCgpa = 8.7f,
            events = academicEvents,
            onBack = {
                showAcademicsScreen = false
            },
            onCgpaClick = {
                showAcademicsScreen = false
                showCgpaScreen = true
                returnToAcademics = true
            },
            onAddEvent = { event ->
                academicEvents.add(event)
            },
            onUpdateEvent = { updated ->
                val index = academicEvents.indexOfFirst { it.id == updated.id }
                if (index >= 0) academicEvents[index] = updated
            },
            onDeleteEvent = { id ->
                academicEvents.removeAll { it.id == id }
            },
            onCompleteEvent = { id ->
                val index = academicEvents.indexOfFirst { it.id == id }
                if (index >= 0) academicEvents[index] = academicEvents[index].copy(status = "COMPLETED")
            },
            onCancelEvent = { id ->
                val index = academicEvents.indexOfFirst { it.id == id }
                if (index >= 0) academicEvents[index] = academicEvents[index].copy(status = "CANCELLED")
            }
        )

    } else if (showCgpaScreen) {

        CGPAScreen(
            onBack = {
                showCgpaScreen = false
                if (returnToAcademics) {
                    showAcademicsScreen = true
                    returnToAcademics = false
                }
            }
        )

    } else if (showGrowthScreen) {

        GrowthScreen(
            currentCgpa = 8.7f,
            dsaProblems = dsaProblems,
            projects = projects,
            internships = internships,
            skills = placementSkills,
            achievements = placementAchievements,
            certifications = placementCertifications,
            learning = placementLearning,
            onBack = { showGrowthScreen = false }
        )

    } else if (showPlacementScreen) {

        PlacementScreen(
            currentCgpa = 8.7f,
            dsaProblems = dsaProblems,
            projects = projects,
            internships = internships,
            skills = placementSkills,
            achievements = placementAchievements,
            certifications = placementCertifications,
            learning = placementLearning,
            onBack = { showPlacementScreen = false },
            onAddSkill = { placementSkills.add(it) },
            onUpdateSkill = { updated ->
                val index = placementSkills.indexOfFirst { it.id == updated.id }
                if (index >= 0) placementSkills[index] = updated
            },
            onDeleteSkill = { id -> placementSkills.removeAll { it.id == id } },
            onAddAchievement = { placementAchievements.add(it) },
            onUpdateAchievement = { updated ->
                val index = placementAchievements.indexOfFirst { it.id == updated.id }
                if (index >= 0) placementAchievements[index] = updated
            },
            onDeleteAchievement = { id -> placementAchievements.removeAll { it.id == id } },
            onAddCertification = { placementCertifications.add(it) },
            onUpdateCertification = { updated ->
                val index = placementCertifications.indexOfFirst { it.id == updated.id }
                if (index >= 0) placementCertifications[index] = updated
            },
            onDeleteCertification = { id -> placementCertifications.removeAll { it.id == id } },
            onAddLearning = { placementLearning.add(it) },
            onUpdateLearning = { updated ->
                val index = placementLearning.indexOfFirst { it.id == updated.id }
                if (index >= 0) placementLearning[index] = updated
            },
            onDeleteLearning = { id -> placementLearning.removeAll { it.id == id } }
        )

    } else if (showInternshipsScreen) {

        InternshipsScreen(
            internships = internships,
            projectCount = projects.size,
            onBack = { showInternshipsScreen = false },
            onAddInternship = { internships.add(it) },
            onUpdateInternship = { updated ->
                val index = internships.indexOfFirst { it.id == updated.id }
                if (index >= 0) internships[index] = updated
            },
            onDeleteInternship = { id -> internships.removeAll { it.id == id } }
        )

    } else if (showProjectsScreen) {

        ProjectsScreen(
            projects = projects,
            onBack = { showProjectsScreen = false },
            onAddProject = { projects.add(it) },
            onUpdateProject = { updated ->
                val index = projects.indexOfFirst { it.id == updated.id }
                if (index >= 0) projects[index] = updated
            },
            onDeleteProject = { id -> projects.removeAll { it.id == id } }
        )

    } else if (showDsaScreen) {

        DSAScreen(
            problems = dsaProblems,
            onBack = {
                showDsaScreen = false
            },
            onAddProblem = { problem ->
                dsaProblems.add(problem)
            }
        )

    } else {

        DashboardScreen(
            dsaProblems = dsaProblems,
            onCgpaClick = {
                showAcademicsScreen = true
            },
            onDsaClick = {
                showDsaScreen = true
            },
            onProjectsClick = {
                showProjectsScreen = true
            },
            onInternshipsClick = {
                showInternshipsScreen = true
            },
            onPlacementClick = {
                showPlacementScreen = true
            },
            onGrowthClick = {
                showGrowthScreen = true
            },
            projectCount = projects.size,
            internshipCount = internships.size,
            skillCount = placementSkills.size
        )
    }
}


// =========================================================
// DASHBOARD
// =========================================================

@Composable
private fun DashboardScreen(
    dsaProblems: List<DSAProblem>,
    projectCount: Int,
    internshipCount: Int,
    skillCount: Int,
    onCgpaClick: () -> Unit,
    onDsaClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onInternshipsClick: () -> Unit,
    onPlacementClick: () -> Unit,
    onGrowthClick: () -> Unit
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
                value = "8.7",
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
                subtitle = "Tap to explore",
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
                value = "68%",
                subtitle = "Readiness",
                accent = Color(0xFFFF7B72)
            )

            val dashboardGrowth = calculateGrowthScore(
                currentCgpa = 8.7f,
                dsaProblems = dsaProblems,
                projectCount = projectCount,
                internshipCount = internshipCount,
                skillCount = skillCount
            )

            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onGrowthClick() },
                title = "GROWTH",
                value = "${dashboardGrowth}%",
                subtitle = "Tap to explore",
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
// GROWTH SCREEN
// =========================================================

@Composable
private fun GrowthScreen(
    currentCgpa: Float,
    dsaProblems: List<DSAProblem>,
    projects: List<ProjectItem>,
    internships: List<InternshipItem>,
    skills: List<PlacementSkill>,
    achievements: List<PlacementAchievement>,
    certifications: List<PlacementCertification>,
    learning: List<PlacementLearning>,
    onBack: () -> Unit
) {

    val dsaScore = calculateDsaProgress(dsaProblems)
    val projectScore = calculateProjectGrowthScore(projects)
    val internshipScore = calculateInternshipGrowthScore(internships)
    val skillScore = calculateGrowthSkillScore(skills, learning)
    val academicScore = (currentCgpa / 10f * 100f).coerceIn(0f, 100f)
    val achievementScore = calculateAchievementScore(
        achievements,
        certifications,
        learning
    )

    val overall = calculateGrowthScore(
        currentCgpa = currentCgpa,
        dsaProblems = dsaProblems,
        projectCount = projects.size,
        internshipCount = internships.size,
        skillCount = skills.size
    )

    val strongest = listOf(
        "Academics" to academicScore,
        "DSA" to dsaScore,
        "Projects" to projectScore,
        "Internships" to internshipScore,
        "Skills" to skillScore,
        "Achievements" to achievementScore.toFloat()
    ).maxByOrNull { it.second }

    val weakest = listOf(
        "Academics" to academicScore,
        "DSA" to dsaScore,
        "Projects" to projectScore,
        "Internships" to internshipScore,
        "Skills" to skillScore,
        "Achievements" to achievementScore.toFloat()
    ).minByOrNull { it.second }

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
                    text = "GROWTH",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "YOUR DEVELOPMENT OVERVIEW",
                    color = Color(0xFFFF4FD8),
                    fontSize = 8.sp,
                    letterSpacing = 1.6.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "OVERALL GROWTH",
                    color = Color(0xFF888891),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$overall",
                        color = Color.White,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/ 100",
                        color = Color(0xFF686871),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                ProgressBar(progress = overall / 100f)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when {
                        overall >= 85 -> "Exceptional development profile. Keep pushing your strongest areas."
                        overall >= 70 -> "Good development profile. Consistency will move you to the next level."
                        overall >= 50 -> "Your journey is taking shape. Focus on the weakest area first."
                        else -> "Start building consistent progress across academics, skills and career preparation."
                    },
                    color = Color(0xFF777780),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = "GROWTH GRAPH")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "CURRENT DEVELOPMENT INDEX",
                    color = Color(0xFF777780),
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                GrowthBarGraph(
                    values = listOf(
                        academicScore,
                        dsaScore,
                        projectScore,
                        internshipScore,
                        skillScore,
                        achievementScore.toFloat()
                    ),
                    labels = listOf("ACA", "DSA", "PRO", "INT", "SKL", "ACH")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Growth is calculated from the data currently available in your PRISM modules.",
                    color = Color(0xFF66666F),
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = "AREA PERFORMANCE")
        Spacer(modifier = Modifier.height(12.dp))

        GrowthMetricCard("ACADEMICS", academicScore, Color(0xFFB76CFF))
        GrowthMetricCard("DSA", dsaScore, Color(0xFF00D9FF))
        GrowthMetricCard("PROJECTS", projectScore, Color(0xFF65E572))
        GrowthMetricCard("INTERNSHIPS", internshipScore, Color(0xFFFFD23F))
        GrowthMetricCard("SKILLS", skillScore, Color(0xFFFF7B72))
        GrowthMetricCard("ACHIEVEMENTS", achievementScore.toFloat(), Color(0xFFFF4FD8))

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = "MILESTONES")
        Spacer(modifier = Modifier.height(12.dp))

        if (dsaProblems.size >= 10) {
            MilestoneCard("100-STYLE DSA MILESTONE", "Your DSA problem count has crossed 10.", true)
        }
        if (projects.any { it.status == "Completed" }) {
            MilestoneCard("PROJECT COMPLETED", "You have completed at least one project.", true)
        }
        if (internships.any { it.status == "Selected" }) {
            MilestoneCard("INTERNSHIP SELECTED", "You have recorded a successful internship outcome.", true)
        }
        if (currentCgpa >= 8.5f) {
            MilestoneCard("CGPA 8.5+", "Your current CGPA has crossed 8.5.", true)
        }
        if (skills.size >= 5) {
            MilestoneCard("SKILL FOUNDATION", "You have added at least five placement skills.", true)
        }
        if (
            dsaProblems.size < 10 &&
            projects.none { it.status == "Completed" } &&
            internships.none { it.status == "Selected" } &&
            currentCgpa < 8.5f &&
            skills.size < 5
        ) {
            MilestoneCard("YOUR FIRST MILESTONE", "Keep building your PRISM profile. Milestones will appear automatically.", false)
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = "GROWTH SIGNALS")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "✦  PRISM GROWTH INSIGHT",
                    color = Color(0xFFFF4FD8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Strongest area: ${strongest?.first ?: "Not enough data"}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Priority area: ${weakest?.first ?: "Not enough data"}",
                    color = Color(0xFFFF7B72),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = growthRecommendation(weakest?.first),
                    color = Color(0xFF777780),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun GrowthBarGraph(values: List<Float>, labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${value.roundToInt()}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(25.dp)
                        .height((110f * (value / 100f).coerceIn(0f, 1f)).dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFF4FD8), Color(0xFFB76CFF))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = labels[index],
                    color = Color(0xFF777780),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GrowthMetricCard(title: String, value: Float, accent: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("${value.roundToInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ProgressBar(progress = value / 100f)
        }
    }
}

@Composable
private fun MilestoneCard(title: String, description: String, achieved: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (achieved) "✓" else "○",
                color = if (achieved) Color(0xFF65E572) else Color(0xFF777780),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color(0xFF777780), fontSize = 9.sp)
            }
        }
    }
}

private fun calculateProjectGrowthScore(projects: List<ProjectItem>): Float {
    if (projects.isEmpty()) return 0f
    val averageProgress = projects.map { it.progress }.average().toFloat()
    val completedBonus = projects.count { it.status == "Completed" }.coerceAtMost(3) * 5f
    return (averageProgress + completedBonus).coerceIn(0f, 100f)
}

private fun calculateInternshipGrowthScore(internships: List<InternshipItem>): Float {
    if (internships.isEmpty()) return 0f
    val applicationActivity = (internships.size.coerceAtMost(10) / 10f) * 45f
    val interviewActivity = (internships.count { it.status == "Interview" || it.status == "Selected" }.coerceAtMost(5) / 5f) * 30f
    val selectedBonus = (internships.count { it.status == "Selected" }.coerceAtMost(2) / 2f) * 25f
    return (applicationActivity + interviewActivity + selectedBonus).coerceIn(0f, 100f)
}

private fun calculateGrowthSkillScore(
    skills: List<PlacementSkill>,
    learning: List<PlacementLearning>
): Float {
    val skillPart = if (skills.isEmpty()) 0f else (skills.map { it.rating }.average().toFloat() / 10f) * 70f
    val learningPart = if (learning.isEmpty()) 0f else (learning.map { it.level }.average().toFloat() / 10f) * 30f
    return (skillPart + learningPart).coerceIn(0f, 100f)
}

private fun calculateGrowthScore(
    currentCgpa: Float,
    dsaProblems: List<DSAProblem>,
    projectCount: Int,
    internshipCount: Int,
    skillCount: Int
): Int {
    val academic = (currentCgpa / 10f * 100f).coerceIn(0f, 100f)
    val dsa = calculateDsaProgress(dsaProblems)
    val projects = (projectCount / 6f * 100f).coerceIn(0f, 100f)
    val internships = (internshipCount / 5f * 100f).coerceIn(0f, 100f)
    val skills = (skillCount / 10f * 100f).coerceIn(0f, 100f)
    return (
            academic * .25f +
                    dsa * .25f +
                    projects * .15f +
                    internships * .15f +
                    skills * .20f
            ).roundToInt().coerceIn(0, 100)
}

private fun growthRecommendation(area: String?): String = when (area) {
    "Academics" -> "Keep your CGPA moving upward and maintain consistency across semesters."
    "DSA" -> "Increase problem-solving consistency and strengthen medium and hard topics."
    "Projects" -> "Complete more projects and increase their depth, documentation and deployment."
    "Internships" -> "Increase relevant applications and build interview exposure."
    "Skills" -> "Add relevant skills and keep improving both technical and soft-skill proficiency."
    "Achievements" -> "Build evidence through competitions, certifications and meaningful accomplishments."
    else -> "Keep adding meaningful progress to your PRISM profile."
}

// =========================================================
// ACADEMICS SCREEN
// =========================================================

@Composable
private fun AcademicsScreen(
    currentCgpa: Float,
    events: List<AcademicEvent>,
    onBack: () -> Unit,
    onCgpaClick: () -> Unit,
    onAddEvent: (AcademicEvent) -> Unit,
    onUpdateEvent: (AcademicEvent) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onCompleteEvent: (Long) -> Unit,
    onCancelEvent: (Long) -> Unit
) {

    var showAddEvent by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AcademicEvent?>(null) }

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
                    text = "ACADEMICS",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PERFORMANCE + PLANNING",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    letterSpacing = 1.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ACADEMIC OVERVIEW",
                    color = Color(0xFF888891),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${"%.1f".format(currentCgpa)}",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CURRENT CGPA",
                            color = Color(0xFF777780),
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "TARGET  9.0",
                        color = Color(0xFF00D9FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        SectionTitle(text = "SEMESTER TRACKER")
        Spacer(modifier = Modifier.height(12.dp))

        SemesterRow("SEM 1", "8.4", true)
        SemesterRow("SEM 2", "8.7", true)
        SemesterRow("SEM 3", "—", false)
        SemesterRow("SEM 4", "—", false)
        SemesterRow("SEM 5", "—", false)
        SemesterRow("SEM 6", "—", false)
        SemesterRow("SEM 7", "—", false)
        SemesterRow("SEM 8", "—", false)

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle(text = "SEMESTER GROWTH")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SGPA TREND",
                    color = Color(0xFF777780),
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                AcademicGrowthGraph()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = "ACADEMIC PLANNER")
        Spacer(modifier = Modifier.height(12.dp))

        AcademicCalendar(
            events = events,
            onAddEvent = { showAddEvent = true }
        )

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
                    Text(
                        "Add your assignments, tests, viva, practicals and exams here.",
                        color = Color(0xFF777780),
                        fontSize = 10.sp
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
private fun AcademicGrowthGraph() {
    val points = listOf(7.8f, 8.1f, 8.4f, 8.7f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val max = 10f
        val min = 6f
        val step = size.width / (points.size - 1)
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
// PLACEMENT SCREEN
// =========================================================

@Composable
private fun PlacementScreen(
    currentCgpa: Float,
    dsaProblems: List<DSAProblem>,
    projects: List<ProjectItem>,
    internships: List<InternshipItem>,
    skills: MutableList<PlacementSkill>,
    achievements: MutableList<PlacementAchievement>,
    certifications: MutableList<PlacementCertification>,
    learning: MutableList<PlacementLearning>,
    onBack: () -> Unit,
    onAddSkill: (PlacementSkill) -> Unit,
    onUpdateSkill: (PlacementSkill) -> Unit,
    onDeleteSkill: (Long) -> Unit,
    onAddAchievement: (PlacementAchievement) -> Unit,
    onUpdateAchievement: (PlacementAchievement) -> Unit,
    onDeleteAchievement: (Long) -> Unit,
    onAddCertification: (PlacementCertification) -> Unit,
    onUpdateCertification: (PlacementCertification) -> Unit,
    onDeleteCertification: (Long) -> Unit,
    onAddLearning: (PlacementLearning) -> Unit,
    onUpdateLearning: (PlacementLearning) -> Unit,
    onDeleteLearning: (Long) -> Unit
) {
    var editor by remember { mutableStateOf<String?>(null) }
    var editingSkill by remember { mutableStateOf<PlacementSkill?>(null) }
    var editingAchievement by remember { mutableStateOf<PlacementAchievement?>(null) }
    var editingCertification by remember { mutableStateOf<PlacementCertification?>(null) }
    var editingLearning by remember { mutableStateOf<PlacementLearning?>(null) }

    val skillScore = calculateSkillReadiness(skills)
    val dsaScore = calculateDsaProgress(dsaProblems)
    val projectScore = calculatePortfolioScore(projects)
    val internshipScore = calculateInternshipProfileStrength(internships, projects.size)
    val academicScore = (currentCgpa / 10f * 100f).coerceIn(0f, 100f)
    val achievementScore = calculateAchievementScore(achievements, certifications, learning)
    val readiness = calculatePlacementReadiness(
        academicScore, dsaScore, projectScore, internshipScore, skillScore, achievementScore
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("PLACEMENT", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("BUILD • MEASURE • GET READY", color = Color(0xFFFF7B72), fontSize = 8.sp, letterSpacing = 1.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PLACEMENT READINESS", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$readiness / 100", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text(placementLabel(readiness), color = placementColor(readiness), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(14.dp))
                ProgressBar(readiness / 100f)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Calculated from your academic, DSA, project, internship and profile data.", color = Color(0xFF777780), fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle("READINESS BREAKDOWN")
        Spacer(modifier = Modifier.height(12.dp))
        ReadinessRow("ACADEMICS", academicScore.roundToInt(), Color(0xFFB76CFF))
        ReadinessRow("DSA", dsaScore.roundToInt(), Color(0xFF00D9FF))
        ReadinessRow("PROJECTS", projectScore, Color(0xFF65E572))
        ReadinessRow("INTERNSHIPS", internshipScore, Color(0xFFFFD23F))
        ReadinessRow("SKILLS", skillScore, Color(0xFFFF4FD8))
        ReadinessRow("ACHIEVEMENTS", achievementScore, Color(0xFFFF7B72))

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle("🧠 SKILLS & COMPETENCIES")
        Spacer(modifier = Modifier.height(10.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
            Column(modifier = Modifier.padding(17.dp)) {
                Text("SKILLS READINESS  $skillScore / 100", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(9.dp))
                ProgressBar(skillScore / 100f)
                Spacer(modifier = Modifier.height(12.dp))
                if (skills.isEmpty()) Text("Add technical, core CS, soft skills and aptitude ratings.", color = Color(0xFF777780), fontSize = 10.sp)
                else skills.forEach { skill ->
                    PlacementItemRow(
                        title = skill.name,
                        subtitle = "${skill.category} • ${skill.rating}/10",
                        onEdit = { editingSkill = skill; editor = "SKILL" },
                        onDelete = { onDeleteSkill(skill.id) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { editingSkill = null; editor = "SKILL" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)), shape = RoundedCornerShape(14.dp)) { Text("+ ADD SKILL", fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("🏆 ACHIEVEMENTS")
        Spacer(modifier = Modifier.height(10.dp))
        PlacementCollectionCard(
            emptyText = "Add hackathon wins, competition ranks, academic ranks and other achievements.",
            buttonText = "+ ADD ACHIEVEMENT",
            empty = achievements.isEmpty(),
            onAdd = { editingAchievement = null; editor = "ACHIEVEMENT" }
        ) {
            achievements.forEach { item ->
                PlacementItemRow(item.title, "${item.type} • ${item.position.ifBlank { "Recognition" }} • ${item.year}", { editingAchievement = item; editor = "ACHIEVEMENT" }, { onDeleteAchievement(item.id) })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("📜 CERTIFICATIONS")
        Spacer(modifier = Modifier.height(10.dp))
        PlacementCollectionCard(
            emptyText = "Add basic and professional certifications with their issuing organization.",
            buttonText = "+ ADD CERTIFICATION",
            empty = certifications.isEmpty(),
            onAdd = { editingCertification = null; editor = "CERTIFICATION" }
        ) {
            certifications.forEach { item ->
                PlacementItemRow(item.name, "${item.issuer} • ${item.date.ifBlank { "Date not added" }}", { editingCertification = item; editor = "CERTIFICATION" }, { onDeleteCertification(item.id) })
                if (item.credentialUrl.isNotBlank()) Text(item.credentialUrl, color = Color(0xFF00D9FF), fontSize = 8.sp, modifier = Modifier.padding(start = 8.dp, bottom = 5.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("🚀 ADDITIONAL LEARNING")
        Spacer(modifier = Modifier.height(10.dp))
        PlacementCollectionCard(
            emptyText = "Track newly learned skills like communication, aptitude, leadership or any technical skill.",
            buttonText = "+ ADD LEARNING",
            empty = learning.isEmpty(),
            onAdd = { editingLearning = null; editor = "LEARNING" }
        ) {
            learning.forEach { item ->
                PlacementItemRow(item.name, "${item.category} • ${item.level}/10", { editingLearning = item; editor = "LEARNING" }, { onDeleteLearning(item.id) })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("📰 PLACEMENT UPDATES")
        Spacer(modifier = Modifier.height(10.dp))
        PlacementNewsCard("INTERNSHIP / JOB SEARCH", "Keep your application links and deadlines in the Internship module. Live placement-news integration can be connected later.")
        PlacementNewsCard("PROFILE TIP", "Relevant projects, measurable achievements and strong communication can improve your placement profile.")

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("⚠ AREAS TO IMPROVE")
        Spacer(modifier = Modifier.height(10.dp))
        val weakAreas = placementWeakAreas(academicScore, dsaScore, projectScore, internshipScore, skillScore, achievementScore)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
            Column(modifier = Modifier.padding(17.dp)) {
                weakAreas.forEachIndexed { index, text ->
                    Text("${index + 1}. $text", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF15151B))) {
            Column(modifier = Modifier.padding(19.dp)) {
                Text("✦  PRISM PLACEMENT INSIGHT", color = Color(0xFFB76CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(generatePlacementInsight(readiness, dsaScore, projectScore, internshipScore, skillScore), color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        Spacer(modifier = Modifier.height(25.dp))
    }

    when (editor) {
        "SKILL" -> PlacementSkillEditor(editingSkill, { editor = null }, { onUpdateOrAddSkill(it, editingSkill, onUpdateSkill, onAddSkill); editor = null })
        "ACHIEVEMENT" -> PlacementAchievementEditor(editingAchievement, { editor = null }, { onUpdateOrAddAchievement(it, editingAchievement, onUpdateAchievement, onAddAchievement); editor = null })
        "CERTIFICATION" -> PlacementCertificationEditor(editingCertification, { editor = null }, { onUpdateOrAddCertification(it, editingCertification, onUpdateCertification, onAddCertification); editor = null })
        "LEARNING" -> PlacementLearningEditor(editingLearning, { editor = null }, { onUpdateOrAddLearning(it, editingLearning, onUpdateLearning, onAddLearning); editor = null })
    }
}

@Composable
private fun ReadinessRow(title: String, score: Int, accent: Color) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("$score%", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(5.dp))
        ProgressBar(score / 100f)
    }
}

@Composable
private fun PlacementItemRow(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF777780), fontSize = 8.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(9.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("EDIT", fontSize = 7.sp) }
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(9.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("DEL", fontSize = 7.sp) }
            }
        }
    }
}

@Composable
private fun PlacementCollectionCard(emptyText: String, buttonText: String, empty: Boolean, onAdd: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
        Column(modifier = Modifier.padding(17.dp)) {
            if (empty) Text(emptyText, color = Color(0xFF777780), fontSize = 10.sp)
            else content()
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)), shape = RoundedCornerShape(14.dp)) { Text(buttonText, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PlacementNewsCard(title: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(title, color = Color(0xFFFFD23F), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(text, color = Color.White, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun PlacementSkillEditor(initial: PlacementSkill?, onBack: () -> Unit, onSave: (PlacementSkill) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Technical") }
    var rating by remember { mutableStateOf(initial?.rating?.toString() ?: "5") }
    var error by remember { mutableStateOf("") }
    val categories = listOf("Technical", "Core CS", "Soft Skill", "Aptitude")
    SimplePlacementEditor(title = if (initial == null) "ADD SKILL" else "EDIT SKILL", subtitle = "SKILLS & COMPETENCIES", onBack = onBack) {
        OutlinedTextField(name, { name = it; error = "" }, Modifier.fillMaxWidth(), label = { Text("Skill Name") }, placeholder = { Text("Java, Communication, DBMS...") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Text("CATEGORY", color = Color(0xFF888891), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        categories.forEach { ChoiceButton(it, category == it, { category = it }) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(rating, { rating = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Skill Level (1-10)") }, singleLine = true)
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF6B6B), fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { val r = rating.toIntOrNull(); if (name.isBlank() || r == null || r !in 1..10) error = "Enter a skill and a rating from 1 to 10." else onSave(PlacementSkill(initial?.id ?: System.currentTimeMillis(), name.trim(), category, r)) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)), shape = RoundedCornerShape(14.dp)) { Text("SAVE SKILL", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PlacementAchievementEditor(initial: PlacementAchievement?, onBack: () -> Unit, onSave: (PlacementAchievement) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Competition") }
    var position by remember { mutableStateOf(initial?.position ?: "") }
    var year by remember { mutableStateOf(initial?.year ?: "") }
    SimplePlacementEditor(title = if (initial == null) "ADD ACHIEVEMENT" else "EDIT ACHIEVEMENT", subtitle = "ACHIEVEMENTS", onBack = onBack) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Achievement") }, placeholder = { Text("Hackathon Winner, University Rank...") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        listOf("Competition", "Academic Rank", "Hackathon", "Other").forEach { ChoiceButton(it, type == it, { type = it }) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(position, { position = it }, Modifier.fillMaxWidth(), label = { Text("Position / Recognition") }, placeholder = { Text("Winner, Rank 3, Finalist...") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(year, { year = it }, Modifier.fillMaxWidth(), label = { Text("Year") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (title.isNotBlank()) onSave(PlacementAchievement(initial?.id ?: System.currentTimeMillis(), title.trim(), type, position.trim(), year.trim())) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)), shape = RoundedCornerShape(14.dp)) { Text("SAVE ACHIEVEMENT", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PlacementCertificationEditor(initial: PlacementCertification?, onBack: () -> Unit, onSave: (PlacementCertification) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var issuer by remember { mutableStateOf(initial?.issuer ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var link by remember { mutableStateOf(initial?.credentialUrl ?: "") }
    SimplePlacementEditor(title = if (initial == null) "ADD CERTIFICATION" else "EDIT CERTIFICATION", subtitle = "CERTIFICATIONS", onBack = onBack) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Certification") }, placeholder = { Text("Python, AWS, Google...") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(issuer, { issuer = it }, Modifier.fillMaxWidth(), label = { Text("Issuing Organization") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Date") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth(), label = { Text("Credential / Certificate Link") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (name.isNotBlank()) onSave(PlacementCertification(initial?.id ?: System.currentTimeMillis(), name.trim(), issuer.trim(), date.trim(), link.trim())) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)), shape = RoundedCornerShape(14.dp)) { Text("SAVE CERTIFICATION", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PlacementLearningEditor(initial: PlacementLearning?, onBack: () -> Unit, onSave: (PlacementLearning) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Technical") }
    var level by remember { mutableStateOf(initial?.level?.toString() ?: "5") }
    SimplePlacementEditor(title = if (initial == null) "ADD LEARNING" else "EDIT LEARNING", subtitle = "ADDITIONAL LEARNING", onBack = onBack) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Skill / Learning") }, placeholder = { Text("Aptitude, Communication, Kotlin...") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        listOf("Technical", "Communication", "Aptitude", "Leadership", "Other").forEach { ChoiceButton(it, category == it, { category = it }) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(level, { level = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Current Level (1-10)") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { val l = level.toIntOrNull()?.coerceIn(1,10) ?: 5; if (name.isNotBlank()) onSave(PlacementLearning(initial?.id ?: System.currentTimeMillis(), name.trim(), category, l)) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)), shape = RoundedCornerShape(14.dp)) { Text("SAVE LEARNING", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SimplePlacementEditor(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050507))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Color(0xFFFF7B72), fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            content()
            Spacer(Modifier.height(30.dp))
        }
    }
}

private fun onUpdateOrAddSkill(item: PlacementSkill, old: PlacementSkill?, update: (PlacementSkill) -> Unit, add: (PlacementSkill) -> Unit) { if (old == null) add(item) else update(item) }
private fun onUpdateOrAddAchievement(item: PlacementAchievement, old: PlacementAchievement?, update: (PlacementAchievement) -> Unit, add: (PlacementAchievement) -> Unit) { if (old == null) add(item) else update(item) }
private fun onUpdateOrAddCertification(item: PlacementCertification, old: PlacementCertification?, update: (PlacementCertification) -> Unit, add: (PlacementCertification) -> Unit) { if (old == null) add(item) else update(item) }
private fun onUpdateOrAddLearning(item: PlacementLearning, old: PlacementLearning?, update: (PlacementLearning) -> Unit, add: (PlacementLearning) -> Unit) { if (old == null) add(item) else update(item) }

private fun calculateSkillReadiness(skills: List<PlacementSkill>): Int {
    if (skills.isEmpty()) return 0
    return (skills.map { it.rating }.average() * 10).roundToInt().coerceIn(0, 100)
}

private fun calculateAchievementScore(achievements: List<PlacementAchievement>, certifications: List<PlacementCertification>, learning: List<PlacementLearning>): Int {
    val achievementPart = (achievements.size.coerceAtMost(5) / 5f) * 40f
    val certPart = (certifications.size.coerceAtMost(5) / 5f) * 30f
    val learningPart = if (learning.isEmpty()) 0f else (learning.map { it.level }.average().toFloat() / 10f) * 30f
    return (achievementPart + certPart + learningPart).roundToInt().coerceIn(0, 100)
}

private fun calculatePlacementReadiness(academic: Float, dsa: Float, projects: Int, internships: Int, skills: Int, achievements: Int): Int {
    return (academic * .20f + dsa * .20f + projects * .20f + internships * .15f + skills * .15f + achievements * .10f).roundToInt().coerceIn(0,100)
}

private fun placementLabel(score: Int): String = when {
    score >= 90 -> "EXCEPTIONAL"
    score >= 80 -> "STRONG"
    score >= 65 -> "GOOD"
    score >= 50 -> "DEVELOPING"
    else -> "NEEDS WORK"
}

private fun placementColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF65E572)
    score >= 65 -> Color(0xFF00D9FF)
    score >= 50 -> Color(0xFFFFD23F)
    else -> Color(0xFFFF7B72)
}

private fun placementWeakAreas(academic: Float, dsa: Float, projects: Int, internships: Int, skills: Int, achievements: Int): List<String> {
    val areas = mutableListOf<Pair<String, Int>>()
    areas += "Academics" to academic.roundToInt()
    areas += "DSA" to dsa.roundToInt()
    areas += "Projects" to projects
    areas += "Internships" to internships
    areas += "Skills" to skills
    areas += "Achievements & certifications" to achievements
    return areas.sortedBy { it.second }.take(3).map { it.first + " needs more attention." }
}

private fun generatePlacementInsight(readiness: Int, dsa: Float, projects: Int, internships: Int, skills: Int): String {
    return when {
        readiness < 50 -> "Your placement profile is still developing. Start with DSA consistency, one strong project and a broader set of relevant skills."
        dsa < 60 -> "Your project profile is a useful base, but DSA is currently limiting your placement readiness. Focus on medium and hard problems consistently."
        projects < 2 -> "Your preparation is progressing, but your project portfolio needs more depth. Build at least one strong, documented real-world project."
        internships < 50 -> "Your internship activity is currently limited. Increase relevant applications and build interview exposure while maintaining your technical preparation."
        skills < 60 -> "Your placement profile would benefit from stronger skill coverage. Add core CS, aptitude and communication preparation alongside technical skills."
        else -> "Your profile is becoming placement-ready. Keep improving the weakest area, maintain DSA consistency and continue adding measurable achievements."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("PROJECTS", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("BUILD • DOCUMENT • SHOWCASE", color = Color(0xFF65E572), fontSize = 8.sp, letterSpacing = 1.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val completedProjects = projects.count { it.status == "COMPLETED" }
        val averageProgress = if (projects.isEmpty()) 0 else projects.map { it.progress }.average().roundToInt()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStatCard(Modifier.weight(1f), "PROJECTS", projects.size.toString())
            SmallStatCard(Modifier.weight(1f), "COMPLETED", completedProjects.toString())
            SmallStatCard(Modifier.weight(1f), "AVG PROGRESS", "$averageProgress%")
        }

        Spacer(modifier = Modifier.height(22.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("PORTFOLIO SCORE", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${calculatePortfolioScore(projects)} / 100", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                ProgressBar(calculatePortfolioScore(projects) / 100f)
                Spacer(modifier = Modifier.height(7.dp))
                Text("Based on completion, tasks, tech stack, GitHub, live demo, documentation and photos.", color = Color(0xFF777780), fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle("YOUR PROJECTS")
        Spacer(modifier = Modifier.height(12.dp))

        if (projects.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NO PROJECTS YET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add your projects, tasks, links and project photos here.", color = Color(0xFF777780), fontSize = 10.sp)
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

        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = { showAddProject = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF65A96B)), shape = RoundedCornerShape(16.dp)) {
            Text("+  ADD PROJECT", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(25.dp))
    }

    if (showAddProject) {
        ProjectEditorScreen(
            initialProject = null,
            onBack = { showAddProject = false },
            onSave = { onAddProject(it); showAddProject = false }
        )
    }

    editingProject?.let { project ->
        ProjectEditorScreen(
            initialProject = project,
            onBack = { editingProject = null },
            onSave = { onUpdateProject(it); editingProject = null }
        )
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

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(project.status, color = Color(0xFF65E572), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Text("${project.progress}%", color = Color(0xFF65E572), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (project.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(project.description, color = Color(0xFF888891), fontSize = 10.sp, lineHeight = 15.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            ProgressBar(project.progress / 100f)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Tech: ${project.techStack.ifBlank { "Not added" }}", color = Color(0xFFB76CFF), fontSize = 9.sp)
            Text("Started: ${project.startDate.ifBlank { "Not added" }}", color = Color(0xFF777780), fontSize = 9.sp)
            Text("Tasks: $completedTasks / $taskTotal", color = Color(0xFF777780), fontSize = 9.sp)
            Text("Photos: ${project.photos.size}", color = Color(0xFF777780), fontSize = 9.sp)

            if (project.photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ProjectPhotoGrid(project.photos, onRemove = {})
            }

            if (project.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                project.tasks.forEach { task ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onToggleTask(task.id) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (task.completed) "☑" else "☐", color = if (task.completed) Color(0xFF65E572) else Color(0xFF777780), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(task.title, color = if (task.completed) Color(0xFF777780) else Color.White, fontSize = 9.sp)
                    }
                }
            }

            if (project.githubUrl.isNotBlank() || project.liveUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Links: ${listOf(project.githubUrl, project.liveUrl).filter { it.isNotBlank() }.joinToString("  •  ")}", color = Color(0xFF00D9FF), fontSize = 8.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("EDIT", fontSize = 9.sp) }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("DELETE", fontSize = 9.sp) }
            }
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
    var photos by remember { mutableStateOf(initialProject?.photos ?: emptyList()) }
    var error by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) photos = (photos + uris.map(Uri::toString)).distinct()
    }

    val statuses = listOf("IDEA", "PLANNING", "IN PROGRESS", "COMPLETED", "ARCHIVED")
    val progress = progressText.toIntOrNull()?.coerceIn(0, 100) ?: 0

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050507))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(if (initialProject == null) "ADD PROJECT" else "EDIT PROJECT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
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

            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle("PROJECT PHOTOS")
            Spacer(modifier = Modifier.height(5.dp))
            Text("Add screenshots, UI photos, certificates or project pictures. You can select multiple photos at once.", color = Color(0xFF777780), fontSize = 9.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15151B)), shape = RoundedCornerShape(14.dp)) {
                Text("+  ADD PROJECT PHOTOS", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            ProjectPhotoGrid(photos, onRemove = { photo -> photos = photos.filterNot { it == photo } })

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
                                name = name.trim(),
                                description = description.trim(),
                                techStack = techStack.trim(),
                                startDate = startDate.trim(),
                                status = status,
                                githubUrl = githubUrl.trim(),
                                liveUrl = liveUrl.trim(),
                                progress = progress,
                                tasks = tasks,
                                photos = photos
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

@Composable
private fun ProjectPhotoGrid(photos: List<String>, onRemove: (String) -> Unit) {
    if (photos.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
            Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                Text("No project photos added", color = Color(0xFF777780), fontSize = 10.sp)
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.chunked(2).forEach { rowPhotos ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPhotos.forEach { uriString ->
                    ProjectPhotoItem(uriString, Modifier.weight(1f), { onRemove(uriString) })
                }
                if (rowPhotos.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProjectPhotoItem(uriString: String, modifier: Modifier, onRemove: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(uriString) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    Box(modifier = modifier.height(130.dp).background(Color(0xFF111116), RoundedCornerShape(14.dp))) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Project photo", modifier = Modifier.fillMaxSize())
        } else {
            Text("Photo unavailable", color = Color(0xFF777780), fontSize = 9.sp, modifier = Modifier.align(Alignment.Center))
        }
        Text("×", color = Color.White, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).clickable { onRemove() })
    }
}

private fun calculatePortfolioScore(projects: List<ProjectItem>): Int {
    if (projects.isEmpty()) return 0
    val averageProgress = projects.map { it.progress }.average()
    val completionScore = projects.count { it.status == "COMPLETED" }.toFloat() / projects.size * 20f
    val techScore = projects.count { it.techStack.isNotBlank() }.toFloat() / projects.size * 15f
    val githubScore = projects.count { it.githubUrl.isNotBlank() }.toFloat() / projects.size * 15f
    val liveScore = projects.count { it.liveUrl.isNotBlank() }.toFloat() / projects.size * 15f
    val photoScore = projects.count { it.photos.isNotEmpty() }.toFloat() / projects.size * 5f
    val progressScore = averageProgress * 0.30f
    return (completionScore + techScore + githubScore + liveScore + photoScore + progressScore).roundToInt().coerceIn(0, 100)
}

// =========================================================
// INTERNSHIPS SCREEN
// =========================================================

@Composable
private fun InternshipsScreen(
    internships: MutableList<InternshipItem>,
    projectCount: Int,
    onBack: () -> Unit,
    onAddInternship: (InternshipItem) -> Unit,
    onUpdateInternship: (InternshipItem) -> Unit,
    onDeleteInternship: (Long) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<InternshipItem?>(null) }

    val successRate = calculateInternshipSuccessRate(internships)
    val profileStrength = calculateInternshipProfileStrength(internships, projectCount)
    val selected = internships.count { it.status == "SELECTED" }
    val rejected = internships.count { it.status == "REJECTED" }
    val active = internships.count {
        it.status == "INTERESTED" ||
                it.status == "APPLIED" ||
                it.status == "ASSESSMENT" ||
                it.status == "INTERVIEW"
    }
    val interviews = internships.count { it.status == "INTERVIEW" || it.status == "SELECTED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("INTERNSHIPS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("APPLY • INTERVIEW • GROW", color = Color(0xFFFFD23F), fontSize = 8.sp, letterSpacing = 1.7.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("INTERNSHIP PROFILE STRENGTH", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("$profileStrength / 100", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(profileStrengthLabel(profileStrength), color = profileStrengthColor(profileStrength), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProgressBar(profileStrength / 100f)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Calculated by PRISM from application activity, interview progress, outcomes, profile completeness and project readiness.",
                    color = Color(0xFF777780),
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallStatCard(Modifier.weight(1f), "APPLICATIONS", internships.size.toString())
            SmallStatCard(Modifier.weight(1f), "ACTIVE", active.toString())
            SmallStatCard(Modifier.weight(1f), "INTERVIEWS", interviews.toString())
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallStatCard(Modifier.weight(1f), "SELECTED", selected.toString())
            SmallStatCard(Modifier.weight(1f), "REJECTED", rejected.toString())
            SmallStatCard(Modifier.weight(1f), "SUCCESS RATE", if (selected + rejected == 0) "—" else "$successRate%")
        }

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("HOW PRISM READS YOUR PROFILE", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(10.dp))
                InternshipInsightLine("Applications submitted", internships.size >= 3)
                InternshipInsightLine("Interview exposure", interviews >= 2)
                InternshipInsightLine("Project foundation", projectCount >= 2)
                InternshipInsightLine("Profile completeness", internships.any { it.role.isNotBlank() && it.applicationDate.isNotBlank() && it.jobUrl.isNotBlank() })
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    generateInternshipInsight(internships, projectCount),
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionTitle("YOUR APPLICATIONS")
        Spacer(modifier = Modifier.height(12.dp))

        if (internships.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NO APPLICATIONS YET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add internships and PRISM will calculate your success rate and profile strength automatically.", color = Color(0xFF777780), fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        } else {
            internships.forEach { internship ->
                InternshipCard(
                    internship = internship,
                    onEdit = { editing = internship },
                    onDelete = { onDeleteInternship(internship.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB28A2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("+  ADD INTERNSHIP", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(25.dp))
    }

    if (showAdd) {
        InternshipEditorScreen(
            initialInternship = null,
            onBack = { showAdd = false },
            onSave = { onAddInternship(it); showAdd = false }
        )
    }

    editing?.let { internship ->
        InternshipEditorScreen(
            initialInternship = internship,
            onBack = { editing = null },
            onSave = { onUpdateInternship(it); editing = null }
        )
    }
}

@Composable
private fun InternshipCard(
    internship: InternshipItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (internship.status) {
        "SELECTED" -> Color(0xFF65E572)
        "REJECTED" -> Color(0xFFFF7B72)
        "INTERVIEW" -> Color(0xFF00D9FF)
        else -> Color(0xFFFFD23F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(internship.company, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(internship.role.ifBlank { "Role not added" }, color = Color(0xFF888891), fontSize = 10.sp)
                }
                Text(internship.status, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Applied: ${internship.applicationDate.ifBlank { "Not added" }}", color = Color(0xFF777780), fontSize = 9.sp)
            if (internship.location.isNotBlank()) Text("Location: ${internship.location}", color = Color(0xFF777780), fontSize = 9.sp)
            if (internship.stipend.isNotBlank()) Text("Stipend: ${internship.stipend}", color = Color(0xFF777780), fontSize = 9.sp)
            if (internship.interviewDate.isNotBlank()) Text("Interview: ${internship.interviewDate}", color = Color(0xFF00D9FF), fontSize = 9.sp)
            if (internship.offerDate.isNotBlank()) Text("Offer: ${internship.offerDate}", color = Color(0xFF65E572), fontSize = 9.sp)
            if (internship.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(internship.notes, color = Color(0xFF888891), fontSize = 9.sp, lineHeight = 14.sp)
            }
            if (internship.jobUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text("Job link saved", color = Color(0xFFB76CFF), fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("EDIT", fontSize = 9.sp) }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("DELETE", fontSize = 9.sp) }
            }
        }
    }
}

@Composable
private fun InternshipEditorScreen(
    initialInternship: InternshipItem?,
    onBack: () -> Unit,
    onSave: (InternshipItem) -> Unit
) {
    var company by remember { mutableStateOf(initialInternship?.company ?: "") }
    var role by remember { mutableStateOf(initialInternship?.role ?: "") }
    var location by remember { mutableStateOf(initialInternship?.location ?: "") }
    var applicationDate by remember { mutableStateOf(initialInternship?.applicationDate ?: "") }
    var interviewDate by remember { mutableStateOf(initialInternship?.interviewDate ?: "") }
    var offerDate by remember { mutableStateOf(initialInternship?.offerDate ?: "") }
    var stipend by remember { mutableStateOf(initialInternship?.stipend ?: "") }
    var status by remember { mutableStateOf(initialInternship?.status ?: "INTERESTED") }
    var jobUrl by remember { mutableStateOf(initialInternship?.jobUrl ?: "") }
    var notes by remember { mutableStateOf(initialInternship?.notes ?: "") }
    var error by remember { mutableStateOf("") }

    val statuses = listOf("INTERESTED", "APPLIED", "ASSESSMENT", "INTERVIEW", "SELECTED", "REJECTED", "WITHDRAWN")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF050507))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(if (initialInternship == null) "ADD INTERNSHIP" else "EDIT INTERNSHIP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("INTERNSHIP TRACKER", color = Color(0xFFFFD23F), fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(company, { company = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Company") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(role, { role = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Role") }, placeholder = { Text("Android Intern, SDE Intern...") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(location, { location = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Location / Remote") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(applicationDate, { applicationDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Application Date") }, placeholder = { Text("DD/MM/YYYY") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(interviewDate, { interviewDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Interview Date (optional)") }, placeholder = { Text("DD/MM/YYYY") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(offerDate, { offerDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Offer Date (optional)") }, placeholder = { Text("DD/MM/YYYY") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(stipend, { stipend = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Stipend (optional)") }, singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("APPLICATION STATUS")
            Spacer(modifier = Modifier.height(8.dp))
            statuses.forEach { option ->
                ChoiceButton(option, status == option) { status = option }
                Spacer(modifier = Modifier.height(7.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(jobUrl, { jobUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Job / Application URL") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Notes") }, placeholder = { Text("Rounds, requirements, preparation notes...") }, minLines = 4)

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(error, color = Color(0xFFFF6B6B), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (company.trim().isEmpty()) {
                        error = "Please enter a company name."
                    } else {
                        onSave(
                            InternshipItem(
                                id = initialInternship?.id ?: (System.currentTimeMillis() + kotlin.random.Random.nextLong(0, 100000)),
                                company = company.trim(),
                                role = role.trim(),
                                location = location.trim(),
                                applicationDate = applicationDate.trim(),
                                interviewDate = interviewDate.trim(),
                                offerDate = offerDate.trim(),
                                stipend = stipend.trim(),
                                status = status,
                                jobUrl = jobUrl.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB28A2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE INTERNSHIP", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun InternshipInsightLine(label: String, achieved: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (achieved) "✓" else "○", color = if (achieved) Color(0xFF65E572) else Color(0xFF777780), fontSize = 13.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = if (achieved) Color.White else Color(0xFF777780), fontSize = 10.sp)
    }
}

private fun calculateInternshipSuccessRate(internships: List<InternshipItem>): Int {
    val selected = internships.count { it.status == "SELECTED" }
    val rejected = internships.count { it.status == "REJECTED" }
    val decided = selected + rejected
    if (decided == 0) return 0
    return (selected.toFloat() / decided.toFloat() * 100f).roundToInt().coerceIn(0, 100)
}

private fun calculateInternshipProfileStrength(
    internships: List<InternshipItem>,
    projectCount: Int
): Int {
    if (internships.isEmpty() && projectCount == 0) return 0

    val applicationActivity = (internships.size / 10f).coerceIn(0f, 1f) * 25f
    val interviewExposure = (internships.count { it.status == "INTERVIEW" || it.status == "SELECTED" } / 5f).coerceIn(0f, 1f) * 20f
    val outcomeExperience = (internships.count { it.status == "SELECTED" } / 2f).coerceIn(0f, 1f) * 20f
    val projectFoundation = (projectCount / 4f).coerceIn(0f, 1f) * 20f

    val completeProfiles = internships.count {
        it.company.isNotBlank() &&
                it.role.isNotBlank() &&
                it.applicationDate.isNotBlank() &&
                it.jobUrl.isNotBlank()
    }
    val profileCompleteness = if (internships.isEmpty()) 0f else (completeProfiles.toFloat() / internships.size) * 15f

    return (applicationActivity + interviewExposure + outcomeExperience + projectFoundation + profileCompleteness)
        .roundToInt()
        .coerceIn(0, 100)
}

private fun profileStrengthLabel(score: Int): String = when {
    score >= 85 -> "EXCELLENT"
    score >= 70 -> "STRONG"
    score >= 50 -> "GOOD"
    score >= 30 -> "DEVELOPING"
    else -> "STARTING"
}

private fun profileStrengthColor(score: Int): Color = when {
    score >= 70 -> Color(0xFF65E572)
    score >= 50 -> Color(0xFFFFD23F)
    else -> Color(0xFFFF7B72)
}

private fun generateInternshipInsight(
    internships: List<InternshipItem>,
    projectCount: Int
): String {
    if (internships.isEmpty()) {
        return if (projectCount >= 2) {
            "Your project foundation is ready. Start applying consistently so PRISM can build a meaningful internship profile."
        } else {
            "Start by adding a few real applications and building project experience. PRISM will analyse your profile as your data grows."
        }
    }

    val selected = internships.count { it.status == "SELECTED" }
    val rejected = internships.count { it.status == "REJECTED" }
    val interviews = internships.count { it.status == "INTERVIEW" || it.status == "SELECTED" }

    return when {
        selected > 0 -> "You have a successful internship outcome. Keep applying to stronger roles and turn the experience into measurable portfolio value."
        interviews == 0 -> "Your next priority is interview exposure. Keep applying and prepare your DSA, projects and interview fundamentals alongside applications."
        rejected > selected && interviews > 0 -> "You are getting interview exposure. Review rejected rounds, improve weak areas and keep the application pipeline active."
        else -> "Your internship pipeline is developing. Keep your applications, interview activity and project profile moving together."
    }
}

// =========================================================
// DSA SCREEN
// =========================================================

@Composable
private fun DSAScreen(
    problems: List<DSAProblem>,
    onBack: () -> Unit,
    onAddProblem: (DSAProblem) -> Unit
) {

    var showAddProblem by remember {
        mutableStateOf(false)
    }

    val dsaScore = calculateDsaProgress(problems)

    val easyCount = problems.count {
        it.difficulty == "Easy"
    }

    val mediumCount = problems.count {
        it.difficulty == "Medium"
    }

    val hardCount = problems.count {
        it.difficulty == "Hard"
    }

    val weightedScore = problems.sumOf {
        it.score.toDouble()
    }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        // =====================================================
        // TOP BAR
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(
                modifier = Modifier.size(10.dp)
            )

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

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // =====================================================
        // CIRCULAR PROGRESS
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111116)
            )
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

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                CircularProgress(
                    progress = dsaScore / 100f,
                    percentage = dsaScore.roundToInt()
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Weighted Score  ${"%.1f".format(weightedScore)}",
                    color = Color(0xFF9A9AA4),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // =====================================================
        // QUICK STATS
        // =====================================================

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
                value = "7 🔥"
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        // =====================================================
        // DIFFICULTY
        // =====================================================

        SectionTitle(
            text = "DIFFICULTY BREAKDOWN"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        DifficultyRow(
            title = "EASY",
            count = easyCount,
            color = Color(0xFF65E572)
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DifficultyRow(
            title = "MEDIUM",
            count = mediumCount,
            color = Color(0xFFFFD23F)
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DifficultyRow(
            title = "HARD",
            count = hardCount,
            color = Color(0xFFFF7B72)
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // =====================================================
        // TOPIC MASTERY
        // =====================================================

        SectionTitle(
            text = "TOPIC MASTERY"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        TopicProgressRow(
            topic = "Arrays",
            progress = topicProgress(
                problems,
                "Arrays"
            )
        )

        TopicProgressRow(
            topic = "Strings",
            progress = topicProgress(
                problems,
                "Strings"
            )
        )

        TopicProgressRow(
            topic = "Linked List",
            progress = topicProgress(
                problems,
                "Linked List"
            )
        )

        TopicProgressRow(
            topic = "Trees / BST",
            progress = topicProgress(
                problems,
                "Trees / BST"
            )
        )

        TopicProgressRow(
            topic = "Graphs",
            progress = topicProgress(
                problems,
                "Graphs"
            )
        )

        TopicProgressRow(
            topic = "Dynamic Programming",
            progress = topicProgress(
                problems,
                "Dynamic Programming"
            )
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // =====================================================
        // GROWTH GRAPH
        // =====================================================

        SectionTitle(
            text = "DSA GROWTH"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111116)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "LAST 6 CHECKPOINTS",
                    color = Color(0xFF777780),
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                GrowthGraph(
                    currentScore = dsaScore
                )
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // =====================================================
        // RECENT PROBLEMS
        // =====================================================

        SectionTitle(
            text = "RECENT PROBLEMS"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (problems.isEmpty()) {

            Text(
                text = "No problems added yet.",
                color = Color(0xFF777780),
                fontSize = 12.sp
            )

        } else {

            problems.takeLast(5).reversed().forEach { problem ->

                ProblemRow(
                    problem = problem
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        // =====================================================
        // ADD PROBLEM
        // =====================================================

        Button(
            onClick = {
                showAddProblem = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B4DFF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {

            Text(
                text = "+  ADD PROBLEM",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =====================================================
        // PRISM INSIGHT
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
                    text = "✦  PRISM INSIGHT",
                    color = Color(0xFFB76CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = generateDsaInsight(problems),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )
    }

    // =====================================================
    // ADD PROBLEM OVERLAY
    // =====================================================

    if (showAddProblem) {

        AddProblemScreen(
            onBack = {
                showAddProblem = false
            },
            onSave = { problem ->

                onAddProblem(problem)

                showAddProblem = false
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
    onSave: (DSAProblem) -> Unit
) {

    var problemName by remember {
        mutableStateOf("")
    }

    var selectedTopic by remember {
        mutableStateOf("Arrays")
    }

    var selectedDifficulty by remember {
        mutableStateOf("Easy")
    }

    var error by remember {
        mutableStateOf("")
    }

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

    val difficulties = listOf(
        "Easy",
        "Medium",
        "Hard"
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

            // =================================================
            // TOP BAR
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "‹",
                    color = Color.White,
                    fontSize = 38.sp,
                    modifier = Modifier.clickable {
                        onBack()
                    }
                )

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

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

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // =================================================
            // PROBLEM NAME
            // =================================================

            OutlinedTextField(
                value = problemName,
                onValueChange = {
                    problemName = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Problem Name")
                },
                placeholder = {
                    Text("e.g. Two Sum")
                },
                singleLine = true,
                isError = error.isNotEmpty()
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // =================================================
            // DIFFICULTY
            // =================================================

            Text(
                text = "DIFFICULTY",
                color = Color(0xFF888891),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            difficulties.forEach { difficulty ->

                ChoiceButton(
                    text = difficulty,
                    selected = selectedDifficulty == difficulty,
                    onClick = {
                        selectedDifficulty = difficulty
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // =================================================
            // TOPIC
            // =================================================

            Text(
                text = "TOPIC",
                color = Color(0xFF888891),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            topics.forEach { topic ->

                ChoiceButton(
                    text = topic,
                    selected = selectedTopic == topic,
                    onClick = {
                        selectedTopic = topic
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            if (error.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = error,
                    color = Color(0xFFFF6B6B),
                    fontSize = 11.sp
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================
            // PREVIEW SCORE
            // =================================================

            val previewScore = calculateProblemScore(
                selectedDifficulty,
                selectedTopic
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111116)
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "PRISM WEIGHT",
                        color = Color(0xFF888891),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text = "${"%.1f".format(previewScore)} points",
                        color = Color(0xFF00D9FF),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "Difficulty + topic importance",
                        color = Color(0xFF777780),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================
            // SAVE
            // =================================================

            Button(
                onClick = {

                    if (problemName.trim().isEmpty()) {

                        error = "Please enter a problem name."

                    } else {

                        onSave(
                            DSAProblem(
                                name = problemName.trim(),
                                topic = selectedTopic,
                                difficulty = selectedDifficulty,
                                score = previewScore
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B4DFF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {

                Text(
                    text = "SAVE PROBLEM",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
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
    problem: DSAProblem
) {

    val difficultyColor = when (
        problem.difficulty
    ) {

        "Easy" ->
            Color(0xFF65E572)

        "Medium" ->
            Color(0xFFFFD23F)

        else ->
            Color(0xFFFF7B72)
    }

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

            Spacer(
                modifier = Modifier.size(11.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = problem.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = problem.topic,
                    color = Color(0xFF777780),
                    fontSize = 9.sp
                )
            }

            Text(
                text = problem.difficulty,
                color = difficultyColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
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
    onBack: () -> Unit
) {

    var currentCgpa by remember {
        mutableStateOf("8.7")
    }

    var targetCgpa by remember {
        mutableStateOf("9.0")
    }

    var editMode by remember {
        mutableStateOf(false)
    }

    var validationError by remember {
        mutableStateOf("")
    }

    val current = currentCgpa.toFloatOrNull()
    val target = targetCgpa.toFloatOrNull()

    val validCurrent =
        current != null && current in 4f..10f

    val validTarget =
        target != null && target in 4f..10f

    val progress = if (
        validCurrent &&
        validTarget &&
        target!! > 0f
    ) {

        (current!! / target)
            .coerceIn(0f, 1f)

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

        // =====================================================
        // TOP BAR
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "‹",
                color = Color.White,
                fontSize = 38.sp,
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(
                modifier = Modifier.size(10.dp)
            )

            Column {

                Text(
                    text = "CGPA",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "ACADEMIC PERFORMANCE",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    letterSpacing = 1.8.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // =====================================================
        // CURRENT CGPA
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111116)
            )
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "CURRENT CGPA",
                    color = Color(0xFF888891),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = currentCgpa,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "VALID RANGE: 4.0 – 10.0",
                    color = Color(0xFF777780),
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                ProgressBar(
                    progress = progress
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = when {

                        !validCurrent ->
                            "Enter a valid CGPA"

                        !validTarget ->
                            "Set a valid target"

                        current!! >= target!! ->
                            "Target achieved 🎯"

                        else ->
                            "${"%.1f".format(target - current)} points to target"
                    },
                    color = Color(0xFF9999A3),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =====================================================
        // TARGET
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
                    text = "YOUR TARGET",
                    color = Color(0xFF888891),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text = targetCgpa,
                    color = Color(0xFF00D9FF),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =====================================================
        // EDIT SECTION
        // =====================================================

        if (editMode) {

            OutlinedTextField(
                value = currentCgpa,
                onValueChange = {
                    currentCgpa = it
                    validationError = ""
                },
                label = {
                    Text("Current CGPA")
                },
                supportingText = {
                    Text("Allowed range: 4.0 – 10.0")
                },
                isError =
                    currentCgpa.isNotEmpty() &&
                            !validCurrent,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = targetCgpa,
                onValueChange = {
                    targetCgpa = it
                    validationError = ""
                },
                label = {
                    Text("Target CGPA")
                },
                supportingText = {
                    Text("Allowed range: 4.0 – 10.0")
                },
                isError =
                    targetCgpa.isNotEmpty() &&
                            !validTarget,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            if (validationError.isNotEmpty()) {

                Text(
                    text = validationError,
                    color = Color(0xFFFF6B6B),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        start = 4.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            Button(
                onClick = {

                    if (!validCurrent || !validTarget) {

                        validationError =
                            "CGPA must be between 4.0 and 10.0."

                    } else {

                        validationError = ""
                        editMode = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B4DFF)
                ),
                shape = RoundedCornerShape(15.dp)
            ) {

                Text(
                    text = "SAVE CHANGES",
                    fontWeight = FontWeight.Bold
                )
            }

        } else {

            Button(
                onClick = {
                    editMode = true
                    validationError = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF15151B)
                ),
                shape = RoundedCornerShape(15.dp)
            ) {

                Text(
                    text = "EDIT CGPA",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // =====================================================
        // PRISM INSIGHT
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
                    text = "✦  PRISM INSIGHT",
                    color = Color(0xFFB76CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = when {

                        !validCurrent ->
                            "Enter a valid CGPA between 4.0 and 10.0."

                        current!! >= 9f ->
                            "Excellent academic performance. Keep maintaining your consistency."

                        current >= 8f ->
                            "You're building a strong academic foundation. Keep pushing toward your target."

                        else ->
                            "Focus on consistency and steady improvement toward your target."
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )
    }
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

    val targetWeightedScore = 200f

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
     * 10 solved problems = 100%
     */

    return (
            topicCount / 10f
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