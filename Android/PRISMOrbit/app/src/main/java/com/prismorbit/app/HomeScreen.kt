package com.prismorbit.app

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {

    var showCgpaScreen by remember {
        mutableStateOf(false)
    }

    if (showCgpaScreen) {

        CGPAScreen(
            onBack = {
                showCgpaScreen = false
            }
        )

    } else {

        DashboardScreen(
            onCgpaClick = {
                showCgpaScreen = true
            }
        )
    }
}


// =========================================================
// DASHBOARD
// =========================================================

@Composable
private fun DashboardScreen(
    onCgpaClick: () -> Unit
) {

    val background = Color(0xFF050507)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
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
                            .fillMaxWidth(0.78f)
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
                subtitle = "Tap to view",
                accent = Color(0xFFB76CFF)
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "DSA",
                value = "72%",
                subtitle = "Problem Solving",
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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFB76CFF),
                                        Color(0xFF00D9FF)
                                    )
                                ),
                                shape = RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "✦",
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text = "SMART AI",
                        color = Color(0xFFB76CFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Color(0xFF292930),
                            RoundedCornerShape(10.dp)
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
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

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

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
                isError = currentCgpa.isNotEmpty() && !validCurrent,
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
                isError = targetCgpa.isNotEmpty() && !validTarget,
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