package com.prismorbit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismorbit.app.ui.theme.PRISMOrbitTheme
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var showHomeScreen by remember {
                mutableStateOf(false)
            }

            PRISMOrbitTheme {

                if (showHomeScreen) {

                    HomeScreen()

                } else {

                    OpeningScreen(
                        onFinished = {
                            showHomeScreen = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OpeningScreen(onFinished: () -> Unit) {

    val animation = remember {
        Animatable(0f)
    }

    LaunchedEffect(Unit) {

        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 7000,
                easing = FastOutSlowInEasing
            )
        )

        delay(1000)
        onFinished()
    }

    val progress = animation.value

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // =========================================================
        // 7 FEATURES
        // =========================================================

        val features = listOf(
            "CGPA",
            "DSA",
            "PROJECTS",
            "INTERNSHIPS",
            "PLACEMENT",
            "GROWTH",
            "SMART AI"
        )

        val colors = listOf(
            Color(0xFFFF1744),
            Color(0xFFFF9100),
            Color(0xFFFFEA00),
            Color(0xFF64DD17),
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            Color(0xFFD500F9)
        )

        /*
         * SAME Y VALUES are used for:
         *
         * Ray
         * Node
         * Text
         *
         * Therefore alignment stays exact.
         */

        val featureY = listOf(
            0.215f,
            0.270f,
            0.325f,
            0.380f,
            0.435f,
            0.490f,
            0.545f
        )

        // =========================================================
        // GRAPHICS
        // =========================================================

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val width = size.width
            val height = size.height

            // =====================================================
            // BACKGROUND
            // =====================================================

            drawRect(
                color = Color.Black
            )

            // =====================================================
            // PRISM
            // =====================================================

            val centerX = width * 0.38f
            val centerY = height * 0.39f

            val prismWidth = width * 0.34f
            val prismHeight = height * 0.25f

            val topX = centerX
            val topY = centerY - prismHeight / 2f

            val leftX = centerX - prismWidth / 2f
            val leftY = centerY + prismHeight / 2f

            val rightX = centerX + prismWidth / 2f
            val rightY = centerY + prismHeight / 2f

            // =====================================================
            // PRISM GLOW
            // =====================================================

            drawLine(
                color = Color(0xFF9EEAFF).copy(alpha = 0.13f),
                start = androidx.compose.ui.geometry.Offset(
                    topX,
                    topY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    leftX,
                    leftY
                ),
                strokeWidth = width * 0.040f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF9EEAFF).copy(alpha = 0.13f),
                start = androidx.compose.ui.geometry.Offset(
                    topX,
                    topY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    rightX,
                    rightY
                ),
                strokeWidth = width * 0.040f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF9EEAFF).copy(alpha = 0.13f),
                start = androidx.compose.ui.geometry.Offset(
                    leftX,
                    leftY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    rightX,
                    rightY
                ),
                strokeWidth = width * 0.040f,
                cap = StrokeCap.Round
            )

            // =====================================================
            // THICK GLASS
            // =====================================================

            drawLine(
                color = Color(0xFF7282B0).copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(
                    topX,
                    topY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    leftX,
                    leftY
                ),
                strokeWidth = width * 0.020f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF7282B0).copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(
                    topX,
                    topY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    rightX,
                    rightY
                ),
                strokeWidth = width * 0.020f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF7282B0).copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(
                    leftX,
                    leftY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    rightX,
                    rightY
                ),
                strokeWidth = width * 0.020f,
                cap = StrokeCap.Round
            )

            // =====================================================
            // SHINY OUTLINE
            // =====================================================

            drawLine(
                color = Color.White.copy(alpha = 0.96f),
                start = androidx.compose.ui.geometry.Offset(
                    topX,
                    topY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    leftX,
                    leftY
                ),
                strokeWidth = width * 0.0045f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.White.copy(alpha = 0.96f),
                start = androidx.compose.ui.geometry.Offset(
                    topX,
                    topY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    rightX,
                    rightY
                ),
                strokeWidth = width * 0.0045f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.White.copy(alpha = 0.96f),
                start = androidx.compose.ui.geometry.Offset(
                    leftX,
                    leftY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    rightX,
                    rightY
                ),
                strokeWidth = width * 0.0045f,
                cap = StrokeCap.Round
            )

            // =====================================================
            // INNER PRISM SHINE
            // =====================================================

            drawLine(
                color = Color(0xFFBDEFFF).copy(alpha = 0.75f),
                start = androidx.compose.ui.geometry.Offset(
                    topX + width * 0.008f,
                    topY + height * 0.008f
                ),
                end = androidx.compose.ui.geometry.Offset(
                    leftX + width * 0.014f,
                    leftY - height * 0.008f
                ),
                strokeWidth = width * 0.002f,
                cap = StrokeCap.Round
            )

            // =====================================================
            // WHITE LIGHT
            // =====================================================

            val whiteProgress =
                ((progress - 0.00f) / 0.25f)
                    .coerceIn(0f, 1f)

            val whiteStartX = -width * 0.08f
            val whiteStartY = centerY

            val whiteEndX =
                leftX + prismWidth * 0.35f

            val whiteCurrentX =
                whiteStartX +
                        (whiteEndX - whiteStartX) *
                        whiteProgress

            // Glow
            drawLine(
                color = Color.White.copy(
                    alpha = whiteProgress * 0.18f
                ),
                start = androidx.compose.ui.geometry.Offset(
                    whiteStartX,
                    whiteStartY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    whiteCurrentX,
                    whiteStartY
                ),
                strokeWidth = width * 0.025f,
                cap = StrokeCap.Round
            )

            // Main beam
            drawLine(
                color = Color.White.copy(
                    alpha = whiteProgress
                ),
                start = androidx.compose.ui.geometry.Offset(
                    whiteStartX,
                    whiteStartY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    whiteCurrentX,
                    whiteStartY
                ),
                strokeWidth = width * 0.0045f,
                cap = StrokeCap.Round
            )

            // =====================================================
            // RAINBOW DISPERSION
            // =====================================================

            val rainbowProgress =
                ((progress - 0.20f) / 0.42f)
                    .coerceIn(0f, 1f)

            val originX =
                rightX - prismWidth * 0.16f

            val originY = centerY

            for (i in 0..6) {

                // -------------------------------------------------
                // TARGET
                // -------------------------------------------------

                val targetX = width * 0.755f
                val targetY = height * featureY[i]

                // -------------------------------------------------
                // ANIMATED RAY
                // -------------------------------------------------

                val currentX =
                    originX +
                            (targetX - originX) *
                            rainbowProgress

                val currentY =
                    originY +
                            (targetY - originY) *
                            rainbowProgress

                // -------------------------------------------------
                // GLOW
                // -------------------------------------------------

                drawLine(
                    color = colors[i].copy(
                        alpha = rainbowProgress * 0.20f
                    ),
                    start = androidx.compose.ui.geometry.Offset(
                        originX,
                        originY
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        currentX,
                        currentY
                    ),
                    strokeWidth = width * 0.023f,
                    cap = StrokeCap.Round
                )

                // -------------------------------------------------
                // MAIN RAY
                // -------------------------------------------------

                drawLine(
                    color = colors[i].copy(
                        alpha = rainbowProgress
                    ),
                    start = androidx.compose.ui.geometry.Offset(
                        originX,
                        originY
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        currentX,
                        currentY
                    ),
                    strokeWidth = width * 0.0045f,
                    cap = StrokeCap.Round
                )

                // -------------------------------------------------
                // WHITE CORE
                // -------------------------------------------------

                drawLine(
                    color = Color.White.copy(
                        alpha = rainbowProgress * 0.28f
                    ),
                    start = androidx.compose.ui.geometry.Offset(
                        originX,
                        originY
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        currentX,
                        currentY
                    ),
                    strokeWidth = width * 0.0012f,
                    cap = StrokeCap.Round
                )

                // -------------------------------------------------
                // NODE
                // -------------------------------------------------

                if (rainbowProgress > 0.80f) {

                    drawCircle(
                        color = colors[i].copy(alpha = 0.14f),
                        radius = width * 0.027f,
                        center = androidx.compose.ui.geometry.Offset(
                            targetX,
                            targetY
                        )
                    )

                    drawCircle(
                        color = colors[i],
                        radius = width * 0.014f,
                        center = androidx.compose.ui.geometry.Offset(
                            targetX,
                            targetY
                        )
                    )

                    drawCircle(
                        color = Color.Black,
                        radius = width * 0.009f,
                        center = androidx.compose.ui.geometry.Offset(
                            targetX,
                            targetY
                        )
                    )
                }
            }

            // =====================================================
            // DISPERSION POINT
            // =====================================================

            if (rainbowProgress > 0f) {

                drawCircle(
                    color = Color.White.copy(
                        alpha = rainbowProgress * 0.18f
                    ),
                    radius = width * 0.035f,
                    center = androidx.compose.ui.geometry.Offset(
                        originX,
                        originY
                    )
                )

                drawCircle(
                    color = Color.White,
                    radius = width * 0.005f,
                    center = androidx.compose.ui.geometry.Offset(
                        originX,
                        originY
                    )
                )
            }

            // =====================================================
            // LOADING BAR
            // =====================================================

            val loadingProgress =
                ((progress - 0.55f) / 0.45f)
                    .coerceIn(0f, 1f)

            val barStartX = width * 0.16f
            val barEndX = width * 0.84f
            val barY = height * 0.775f

            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = androidx.compose.ui.geometry.Offset(
                    barStartX,
                    barY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    barEndX,
                    barY
                ),
                strokeWidth = width * 0.008f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.White.copy(
                    alpha = loadingProgress * 0.85f
                ),
                start = androidx.compose.ui.geometry.Offset(
                    barStartX,
                    barY
                ),
                end = androidx.compose.ui.geometry.Offset(
                    barStartX +
                            (barEndX - barStartX) *
                            loadingProgress,
                    barY
                ),
                strokeWidth = width * 0.0025f,
                cap = StrokeCap.Round
            )
        }

        // =========================================================
        // FEATURE TEXT
        // =========================================================

        val labelAlpha =
            ((progress - 0.48f) / 0.20f)
                .coerceIn(0f, 1f)

        for (i in 0..6) {

            Text(
                text = features[i],
                color = Color.White,
                fontSize = if (features[i].length > 9) {
                    8.sp
                } else {
                    9.sp
                },
                letterSpacing = 1.4.sp,
                modifier = Modifier
                    .padding(
                        start = screenWidth * 0.775f,
                        top = screenHeight * featureY[i]
                    )
                    .alpha(labelAlpha)
            )
        }

        // =========================================================
        // PRISM LOGO
        // =========================================================

        val logoAlpha =
            ((progress - 0.48f) / 0.25f)
                .coerceIn(0f, 1f)

        Text(
            text = "PRISM",
            color = Color.White,
            fontSize = 40.sp,
            letterSpacing = 4.sp,
            modifier = Modifier
                .padding(
                    start = screenWidth * 0.20f,
                    top = screenHeight * 0.635f
                )
                .alpha(logoAlpha)
        )

        // =========================================================
        // TAGLINE
        // =========================================================

        Text(
            text = "SEE BEYOND THE GRINDS",
            color = Color(0xFFD8C8FF),
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
            modifier = Modifier
                .padding(
                    start = screenWidth * 0.22f,
                    top = screenHeight * 0.690f
                )
                .alpha(logoAlpha)
        )

        // =========================================================
        // LOADING TEXT
        // =========================================================

        val loadingAlpha =
            ((progress - 0.55f) / 0.30f)
                .coerceIn(0f, 1f)

        Text(
            text = "LOADING NEW POSSIBILITIES...",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 8.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .padding(
                    start = screenWidth * 0.28f,
                    top = screenHeight * 0.805f
                )
                .alpha(loadingAlpha)
        )
    }
}