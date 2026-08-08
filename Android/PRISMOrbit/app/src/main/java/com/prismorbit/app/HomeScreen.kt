package com.prismorbit.app

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {

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

            // Profile Circle

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

                // Progress background

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(
                            Color(0xFF292930),
                            RoundedCornerShape(10.dp)
                        )
                ) {

                    // Progress

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
        // ROW 1
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "CGPA",
                value = "8.7",
                subtitle = "Academic",
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
        // ROW 2
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
        // ROW 3
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