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
    val title: String,
    val subject: String,
    val type: String,
    val date: String,
    val day: String,
    val syllabus: String,
    val notes: String
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
            }
        )
    }
}


// =========================================================
// DASHBOARD
// =========================================================

@Composable
private fun DashboardScreen(
    dsaProblems: List<DSAProblem>,
    onCgpaClick: () -> Unit,
    onDsaClick: () -> Unit
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
    events: List<AcademicEvent>,
    onBack: () -> Unit,
    onCgpaClick: () -> Unit,
    onAddEvent: (AcademicEvent) -> Unit
) {

    var showAddEvent by remember { mutableStateOf(false) }

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
            SectionTitle(text = "UPCOMING EVENTS")
            Spacer(modifier = Modifier.height(12.dp))
            events.sortedBy { it.date }.take(10).forEach { event ->
                AcademicEventRow(event)
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
            onBack = { showAddEvent = false },
            onSave = {
                onAddEvent(it)
                showAddEvent = false
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
private fun AcademicEventRow(event: AcademicEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(event.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(event.type.uppercase(), color = Color(0xFFB76CFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text("${event.subject}  •  ${event.date}  •  ${event.day}", color = Color(0xFF00D9FF), fontSize = 9.sp)
            if (event.syllabus.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text("Syllabus: ${event.syllabus}", color = Color(0xFF888891), fontSize = 9.sp)
            }
            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(event.notes, color = Color(0xFF777780), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AddAcademicEventScreen(
    onBack: () -> Unit,
    onSave: (AcademicEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Assignment") }
    var date by remember { mutableStateOf("") }
    var syllabus by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val types = listOf("Assignment", "Class Test", "Viva", "Practical", "Quiz", "MST", "Exam", "Project", "Other")

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
                Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text("ADD EVENT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("ACADEMIC PLANNER", color = Color(0xFF00D9FF), fontSize = 8.sp, letterSpacing = 1.5.sp)
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
            Text("EVENT TYPE", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            types.forEach { item ->
                ChoiceButton(text = item, selected = type == item, onClick = { type = item })
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it.filter { c -> c.isDigit() || c == '-' }; error = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                supportingText = { Text("Enter date as YYYY-MM-DD. Day is calculated automatically.") },
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
                        else -> onSave(
                            AcademicEvent(
                                title = title.trim(),
                                subject = subject.trim(),
                                type = type,
                                date = date,
                                day = parsed.first,
                                syllabus = syllabus.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4DFF)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("SAVE EVENT", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
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
