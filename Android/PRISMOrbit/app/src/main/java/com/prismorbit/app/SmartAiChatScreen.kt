package com.prismorbit.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext


// ============================================================
// SMART AI CHAT SCREEN
// ============================================================

private data class DisplayMessage(
    val role: String, // "user" or "model"
    val text: String,
    val isError: Boolean = false,
    val promptText: String = text
)

private val SUGGESTED_PROMPTS = listOf(
    "What should I do today?",
    "How's my placement prep going?",
    "I have 2 hours today, what should I focus on?"
)


// ============================================================
// COMPOSABLE
// ============================================================

@Composable
fun SmartAiChatScreen(
    onBack: () -> Unit
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val uid = auth.currentUser?.uid
    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var context by remember {
        mutableStateOf<PrismAiContext?>(null)
    }

    var contextError by remember {
        mutableStateOf("")
    }

    var contextLoading by remember {
        mutableStateOf(true)
    }

    val messages = remember {
        mutableStateListOf<DisplayMessage>()
    }

    var inputText by remember {
        mutableStateOf("")
    }

    var isSending by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // VOICE STATE
    // ========================================================

    var voiceError by remember {
        mutableStateOf("")
    }

    var isListening by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // SPEECH RESULT LAUNCHER
    // ========================================================

    val speechLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->

            isListening = false

            if (result.resultCode == Activity.RESULT_OK) {

                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                        )
                        ?.firstOrNull()
                        ?.trim()

                if (!spokenText.isNullOrBlank()) {

                    inputText = spokenText
                    voiceError = ""

                } else {

                    voiceError =
                        "Didn't catch that — try again or type instead."
                }

            } else {

                // User cancelled the speech dialog.
                voiceError = ""
            }
        }


    // ========================================================
    // MICROPHONE PERMISSION
    // ========================================================

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                try {

                    voiceError = ""
                    isListening = true

                    val intent =
                        Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                        ).apply {

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                "en-US"
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                                "en-US"
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_MAX_RESULTS,
                                3
                            )
                        }

                    speechLauncher.launch(intent)

                } catch (_: ActivityNotFoundException) {

                    isListening = false

                    voiceError =
                        "Voice input isn't available on this device — you can still type."
                }

            } else {

                isListening = false

                voiceError =
                    "Microphone permission denied — you can still type your question."
            }
        }


    // ========================================================
    // START VOICE INPUT
    // ========================================================

    fun startVoiceInput() {

        if (
            isSending ||
            contextLoading
        ) {
            return
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                localContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {

            micPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )

            return
        }

        try {

            voiceError = ""
            isListening = true

            val intent =
                Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        "en-US"
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                        "en-US"
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_MAX_RESULTS,
                        3
                    )
                }

            speechLauncher.launch(intent)

        } catch (_: ActivityNotFoundException) {

            isListening = false

            voiceError =
                "Voice input isn't available on this device — you can still type."
        }
    }


    // ========================================================
    // STOP VOICE INPUT
    // ========================================================

    fun stopVoiceInput() {

        // Path A is controlled by the system speech screen.
        // There is no SpeechRecognizer session to manually stop.
        isListening = false
    }


    // ========================================================
    // LOAD PRISM CONTEXT
    // ========================================================

    fun loadContext() {

        val currentUid = uid ?: return

        contextLoading = true
        contextError = ""

        coroutineScope.launch {

            try {

                context =
                    PrismContextBuilder.buildContext(
                        firestore,
                        currentUid
                    )

            } catch (e: Exception) {

                contextError =
                    e.message
                        ?: "Unable to load your PRISM data right now."

            } finally {

                contextLoading = false
            }
        }
    }


    LaunchedEffect(uid) {
        loadContext()
    }


    // ========================================================
    // SEND MESSAGE
    // ========================================================

    fun sendMessage(
        text: String,
        displayText: String = text
    ) {

        val trimmed = text.trim()
        val currentContext = context

        if (
            trimmed.isBlank() ||
            isSending
        ) {
            return
        }

        if (isListening) {
            stopVoiceInput()
        }

        if (currentContext == null) {

            messages.add(
                DisplayMessage(
                    role = "model",
                    text = "I couldn't load your PRISM data yet — try again in a moment.",
                    isError = true
                )
            )

            return
        }

        val historyBeforeThisMessage =
            messages
                .filterNot {
                    it.isError
                }
                .map {

                    ChatTurn(
                        role = it.role,
                        text = it.promptText
                    )
                }

        messages.add(
            DisplayMessage(
                role = "user",
                text = displayText
                    .trim()
                    .ifBlank {
                        trimmed
                    },
                promptText = trimmed
            )
        )

        inputText = ""
        voiceError = ""
        isSending = true

        coroutineScope.launch {

            val result =
                GeminiChatService.sendMessage(
                    history = historyBeforeThisMessage,
                    newUserMessage = trimmed,
                    prismContext = currentContext,
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

            result
                .onSuccess { reply ->

                    messages.add(
                        DisplayMessage(
                            role = "model",
                            text = reply
                        )
                    )
                }
                .onFailure { error ->

                    messages.add(
                        DisplayMessage(
                            role = "model",
                            text =
                                "Smart AI couldn't respond just now (${error.message ?: "unknown error"}). Try again in a moment.",
                            isError = true
                        )
                    )
                }

            isSending = false

            if (messages.isNotEmpty()) {

                listState.animateScrollToItem(
                    messages.size - 1
                )
            }
        }
    }


    // ========================================================
    // MAIN SCREEN
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        // ====================================================
        // HEADER
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
                    .clickable {
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "‹",
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "SMART AI",
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text =
                        if (contextLoading) {
                            "Loading your PRISM data..."
                        } else {
                            "Grounded in your real PRISM data"
                        },
                    color = Color(0xFFB76CFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }


            // =================================================
            // REPORT BUTTON
            // =================================================

            Text(
                text = "REPORT",
                color = Color(0xFF00D9FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clickable(
                        enabled =
                            !isSending &&
                                    !contextLoading
                    ) {

                        sendMessage(
                            text =
                                SmartAiPrompts.DAILY_REPORT_PROMPT,
                            displayText =
                                SmartAiPrompts.DAILY_REPORT_DISPLAY_TEXT
                        )
                    }
                    .padding(8.dp)
            )


            // =================================================
            // CLEAR BUTTON
            // =================================================

            if (messages.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(
                            enabled = !isSending
                        ) {

                            messages.clear()
                            inputText = ""
                            voiceError = ""

                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                ) {

                    Text(
                        text = "CLEAR",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }


        // ====================================================
        // CONTEXT ERROR
        // ====================================================

        if (contextError.isNotBlank()) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp
                    ),
                shape =
                    RoundedCornerShape(14.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF241416)
                    )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        text = contextError,
                        color = Color(0xFFFF7B72),
                        fontSize = 11.sp
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "TAP TO RETRY",
                        color = Color(0xFFFF7B72),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            loadContext()
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }


        // ====================================================
        // MESSAGE LIST
        // ====================================================

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            // =================================================
            // SUGGESTED PROMPTS
            // =================================================

            if (
                messages.isEmpty() &&
                !contextLoading
            ) {

                item {

                    Column(
                        modifier = Modifier.padding(
                            top = 20.dp
                        )
                    ) {

                        Text(
                            text =
                                "Ask about your deadlines, DSA progress, projects, or placement readiness.",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        SUGGESTED_PROMPTS.forEach { prompt ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        bottom = 8.dp
                                    )
                                    .clickable {
                                        sendMessage(prompt)
                                    },
                                shape =
                                    RoundedCornerShape(14.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.surface
                                    )
                            ) {

                                Text(
                                    text = prompt,
                                    modifier =
                                        Modifier.padding(14.dp),
                                    color =
                                        MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }


                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "PLAN MY DAY",
                            color = Color(0xFFB76CFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(
                                    rememberScrollState()
                                ),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            SmartAiPlanningPrompts
                                .DURATION_OPTIONS
                                .forEach { option ->

                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                MaterialTheme.colorScheme.surface
                                            )
                                            .clickable(
                                                enabled =
                                                    !isSending &&
                                                            !contextLoading
                                            ) {

                                                sendMessage(
                                                    text =
                                                        SmartAiPlanningPrompts
                                                            .buildPlanningMessage(
                                                                option.second
                                                            ),
                                                    displayText =
                                                        "Plan my day for ${option.first}"
                                                )
                                            }
                                            .padding(
                                                horizontal = 14.dp,
                                                vertical = 10.dp
                                            )
                                    ) {

                                        Text(
                                            text = option.first,
                                            color =
                                                MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight =
                                                FontWeight.Medium
                                        )
                                    }
                                }
                        }
                    }
                }
            }


            // =================================================
            // CHAT MESSAGES
            // =================================================

            items(messages) { message ->

                ChatBubble(message)
            }


            // =================================================
            // THINKING INDICATOR
            // =================================================

            if (isSending) {

                item {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Smart AI is thinking...",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }


        // ====================================================
        // INPUT ROW
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = inputText,
                onValueChange = {

                    inputText = it
                    voiceError = ""
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask Smart AI...")
                },
                enabled =
                    !isSending &&
                            !contextLoading &&
                            !isListening,
                singleLine = true
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )


            // =================================================
            // MICROPHONE BUTTON
            // =================================================

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (isListening) {
                            Color(0xFFB3261E)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .clickable(
                        enabled =
                            !isSending &&
                                    !contextLoading
                    ) {

                        if (isListening) {

                            stopVoiceInput()

                        } else {

                            startVoiceInput()
                        }
                    },
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (isListening) {
                            "■"
                        } else {
                            "🎤"
                        },
                    color =
                        if (isListening) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    fontSize = 18.sp
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )


            // =================================================
            // SEND BUTTON
            // =================================================

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (
                            inputText.isBlank() ||
                            isSending
                        ) {

                            Color(0xFF3A3A42)

                        } else {

                            Color(0xFF8B4DFF)
                        }
                    )
                    .clickable(
                        enabled =
                            inputText.isNotBlank() &&
                                    !isSending
                    ) {

                        sendMessage(inputText)
                    },
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "➤",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }


        // ====================================================
        // VOICE STATUS / ERROR
        // ====================================================

        if (isListening) {

            Text(
                text = "Listening... speak now",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 4.dp
                    ),
                color = Color(0xFFB76CFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

        } else if (voiceError.isNotBlank()) {

            Text(
                text = voiceError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 4.dp
                    ),
                color = Color(0xFFFF7B72),
                fontSize = 10.sp
            )
        }
    }
}


// ============================================================
// CHAT BUBBLE
// ============================================================

@Composable
private fun ChatBubble(
    message: DisplayMessage
) {

    val isUser =
        message.role == "user"

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {

        Card(
            modifier =
                Modifier.widthIn(
                    max = 280.dp
                ),
            shape =
                RoundedCornerShape(16.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        when {

                            isUser ->
                                Color(0xFF8B4DFF)

                            message.isError ->
                                Color(0xFF241416)

                            else ->
                                MaterialTheme.colorScheme.surface
                        }
                )
        ) {

            Text(
                text = message.text,
                modifier =
                    Modifier.padding(12.dp),
                color =
                    if (isUser) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}