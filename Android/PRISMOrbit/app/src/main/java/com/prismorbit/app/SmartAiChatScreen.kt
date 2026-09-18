package com.prismorbit.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================
// SMART AI CHAT SCREEN
// ============================================================

private data class DisplayMessage(
    val role: String,
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
        mutableStateOf(false)
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
    // BYOK
    // ========================================================

    var storedKey by remember {
        mutableStateOf<StoredAiKey?>(null)
    }

    var apiKeyChecked by remember {
        mutableStateOf(false)
    }

    var showApiKeySetup by remember {
        mutableStateOf(false)
    }

    // ========================================================
    // LOAD USER API KEY
    // ========================================================

    LaunchedEffect(
        uid,
        AiKeyRefreshSignal.version
    ) {

        val currentUid = uid

        storedKey =
            if (currentUid != null) {

                AiApiKeyManager.getKey(
                    localContext,
                    currentUid
                )

            } else {

                null
            }

        apiKeyChecked = true
    }

    // ========================================================
    // TEXT TO SPEECH
    // ========================================================

    var ttsEngine by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    var ttsReady by remember {
        mutableStateOf(false)
    }

    var voiceOutputEnabled by remember {
        mutableStateOf(true)
    }

    var isSpeaking by remember {
        mutableStateOf(false)
    }

    // Latest successful AI response.
    var latestAiReply by remember {
        mutableStateOf("")
    }

    // If TTS isn't ready when a reply arrives, keep it here.
    var pendingSpeech by remember {
        mutableStateOf("")
    }

    // ========================================================
    // VOICE INPUT
    // ========================================================

    var voiceError by remember {
        mutableStateOf("")
    }

    var isListening by remember {
        mutableStateOf(false)
    }

    // ========================================================
    // TTS INITIALIZATION
    // ========================================================

    val mainHandler =
        remember {
            Handler(Looper.getMainLooper())
        }

    DisposableEffect(localContext) {

        lateinit var engine: TextToSpeech

        engine =
            TextToSpeech(
                localContext
            ) { status ->

                // TTS callbacks can happen asynchronously.
                // Always update/use the engine on the main thread.
                mainHandler.post {

                    if (status != TextToSpeech.SUCCESS) {

                        ttsReady = false
                        ttsEngine = null

                        return@post
                    }

                    try {

                        var languageResult =
                            engine.setLanguage(
                                Locale("en", "IN")
                            )

                        // If Indian English isn't available, try US English.
                        if (
                            languageResult ==
                            TextToSpeech.LANG_MISSING_DATA ||
                            languageResult ==
                            TextToSpeech.LANG_NOT_SUPPORTED
                        ) {

                            languageResult =
                                engine.setLanguage(
                                    Locale.US
                                )
                        }

                        // Last fallback: device default language.
                        if (
                            languageResult ==
                            TextToSpeech.LANG_MISSING_DATA ||
                            languageResult ==
                            TextToSpeech.LANG_NOT_SUPPORTED
                        ) {

                            languageResult =
                                engine.setLanguage(
                                    Locale.getDefault()
                                )
                        }

                        if (
                            languageResult ==
                            TextToSpeech.LANG_MISSING_DATA ||
                            languageResult ==
                            TextToSpeech.LANG_NOT_SUPPORTED
                        ) {

                            ttsReady = false
                            ttsEngine = null

                            return@post
                        }

                        ttsEngine = engine
                        ttsReady = true

                        // If an AI reply arrived before TTS finished
                        // initializing, speak it now.
                        val queuedText =
                            pendingSpeech.trim()

                        if (
                            voiceOutputEnabled &&
                            queuedText.isNotBlank()
                        ) {

                            pendingSpeech = ""

                            val result =
                                engine.speak(
                                    queuedText,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "prism_smart_ai_reply"
                                )

                            if (
                                result ==
                                TextToSpeech.ERROR
                            ) {

                                pendingSpeech = queuedText
                                isSpeaking = false
                            }
                        }

                    } catch (_: Exception) {

                        ttsReady = false
                        ttsEngine = null
                        isSpeaking = false
                    }
                }
            }

        // Set the listener immediately so no utterance-progress
        // callback is missed.
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {

                    mainHandler.post {
                        isSpeaking = true
                    }
                }

                override fun onDone(
                    utteranceId: String?
                ) {

                    mainHandler.post {
                        isSpeaking = false
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(
                    utteranceId: String?
                ) {

                    mainHandler.post {
                        isSpeaking = false
                    }
                }
            }
        )

        ttsEngine = engine

        onDispose {

            mainHandler.post {

                try {
                    engine.stop()
                } catch (_: Exception) {
                }

                try {
                    engine.shutdown()
                } catch (_: Exception) {
                }

                if (ttsEngine === engine) {
                    ttsEngine = null
                }

                ttsReady = false
                isSpeaking = false
                pendingSpeech = ""
            }
        }
    }

    // ========================================================
    // SPEAK
    // ========================================================

    fun speak(text: String) {

        val cleanText =
            text
                .trim()
                .replace(Regex("\\*\\*"), "")
                .replace(Regex("(?m)^#+\\s*"), "")
                .replace(Regex("`"), "")

        if (
            cleanText.isBlank() ||
            !voiceOutputEnabled
        ) {
            return
        }

        val engine =
            ttsEngine

        if (
            engine == null ||
            !ttsReady
        ) {

            pendingSpeech = cleanText
            return
        }

        mainHandler.post {

            if (!voiceOutputEnabled) {
                return@post
            }

            try {

                var languageResult =
                    engine.setLanguage(
                        Locale("en", "IN")
                    )

                if (
                    languageResult ==
                    TextToSpeech.LANG_MISSING_DATA ||
                    languageResult ==
                    TextToSpeech.LANG_NOT_SUPPORTED
                ) {

                    languageResult =
                        engine.setLanguage(
                            Locale.US
                        )
                }

                if (
                    languageResult ==
                    TextToSpeech.LANG_MISSING_DATA ||
                    languageResult ==
                    TextToSpeech.LANG_NOT_SUPPORTED
                ) {

                    languageResult =
                        engine.setLanguage(
                            Locale.getDefault()
                        )
                }

                if (
                    languageResult ==
                    TextToSpeech.LANG_MISSING_DATA ||
                    languageResult ==
                    TextToSpeech.LANG_NOT_SUPPORTED
                ) {

                    pendingSpeech = cleanText
                    isSpeaking = false
                    voiceError =
                        "Text-to-speech language isn't available on this device."
                    return@post
                }

                pendingSpeech = ""

                val result =
                    engine.speak(
                        cleanText,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "prism_smart_ai_reply_${System.currentTimeMillis()}"
                    )

                if (
                    result ==
                    TextToSpeech.ERROR
                ) {

                    pendingSpeech = cleanText
                    isSpeaking = false
                    voiceError =
                        "Text-to-speech couldn't start on this device."
                }
            } catch (_: Exception) {

                pendingSpeech = cleanText
                isSpeaking = false
                voiceError =
                    "Text-to-speech couldn't start on this device."
            }
        }
    }

    // ========================================================
    // STOP SPEAKING
    // ========================================================

    fun stopSpeaking() {

        pendingSpeech = ""
        isSpeaking = false

        mainHandler.post {

            try {
                ttsEngine?.stop()
            } catch (_: Exception) {
            }
        }
    }

    // ========================================================
    // TOGGLE SPEAKER
    // ========================================================

    fun toggleSpeaker() {

        if (voiceOutputEnabled) {

            // Speaker currently ON -> turn OFF.
            voiceOutputEnabled = false
            stopSpeaking()

        } else {

            // Speaker currently OFF -> turn ON.
            voiceOutputEnabled = true

            val reply =
                latestAiReply.trim()

            if (reply.isNotBlank()) {
                speak(reply)
            }
        }
    }

    // ========================================================
    // SPEECH RESULT LAUNCHER
    // ========================================================

    val speechLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->

            isListening = false

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                        )
                        ?.firstOrNull()
                        ?.trim()

                if (
                    !spokenText.isNullOrBlank()
                ) {

                    inputText = spokenText
                    voiceError = ""

                } else {

                    voiceError =
                        "Didn't catch that — try again or type instead."
                }

            } else {

                voiceError = ""
            }
        }

    // ========================================================
    // MICROPHONE PERMISSION
    // ========================================================

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
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

                } catch (
                    _: ActivityNotFoundException
                ) {

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
            contextLoading ||
            !apiKeyChecked ||
            storedKey == null
        ) {
            return
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                localContext,
                Manifest.permission.RECORD_AUDIO
            ) ==
                    PackageManager.PERMISSION_GRANTED

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

        } catch (
            _: ActivityNotFoundException
        ) {

            isListening = false

            voiceError =
                "Voice input isn't available on this device — you can still type."
        }
    }

    // ========================================================
    // STOP VOICE INPUT
    // ========================================================

    fun stopVoiceInput() {

        isListening = false
    }

    // ========================================================
    // LOAD PRISM CONTEXT
    // ========================================================

    fun loadContext() {

        val currentUid =
            uid ?: return

        if (
            !apiKeyChecked ||
            storedKey == null
        ) {
            return
        }

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
                    when {

                        e is java.net.UnknownHostException ||
                                e is java.io.IOException ->

                            "No internet connection — check your connection and try again."

                        e is com.google.firebase.firestore.FirebaseFirestoreException &&
                                e.code ==
                                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->

                            "Smart AI couldn't access your PRISM data — try signing out and back in."

                        e is com.google.firebase.firestore.FirebaseFirestoreException ->

                            "Unable to load your PRISM data right now — try again in a moment."

                        else ->

                            e.message
                                ?: "Unable to load your PRISM data right now."
                    }

            } finally {

                contextLoading = false
            }
        }
    }

    // ========================================================
    // LOAD CONTEXT AFTER API KEY EXISTS
    // ========================================================

    LaunchedEffect(
        uid,
        apiKeyChecked,
        storedKey
    ) {

        if (
            apiKeyChecked &&
            storedKey != null
        ) {

            loadContext()
        }
    }

    // ========================================================
    // API KEY MANAGEMENT SCREEN
    // ========================================================

    if (showApiKeySetup) {

        SmartAiApiKeySetupScreen(
            onBack = {
                showApiKeySetup = false
            }
        )

        return
    }

    // ========================================================
    // MANDATORY API KEY GATE
    // ========================================================

    if (
        apiKeyChecked &&
        storedKey == null
    ) {

        SmartAiApiKeySetupScreen(
            onBack = onBack
        )

        return
    }

    // ========================================================
    // WAIT WHILE CHECKING KEY
    // ========================================================

    if (!apiKeyChecked) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
                .statusBarsPadding(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            CircularProgressIndicator()

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text =
                    "Checking Smart AI configuration...",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        return
    }

    // ========================================================
    // SEND MESSAGE
    // ========================================================

    fun sendMessage(
        text: String,
        displayText: String = text
    ) {

        val trimmed =
            text.trim()

        val currentContext =
            context

        val currentKey =
            storedKey

        if (
            trimmed.isBlank() ||
            isSending
        ) {
            return
        }

        if (isListening) {
            stopVoiceInput()
        }

        if (
            !apiKeyChecked ||
            currentKey == null
        ) {
            return
        }

        if (currentContext == null) {

            messages.add(
                DisplayMessage(
                    role = "model",
                    text =
                        "I couldn't load your PRISM data yet — try again in a moment.",
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
                text =
                    displayText
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
                AiChatService.sendMessage(
                    provider =
                        currentKey.provider,
                    apiKey =
                        currentKey.apiKey,
                    history =
                        historyBeforeThisMessage,
                    newUserMessage =
                        trimmed,
                    prismContext =
                        currentContext
                )

            result
                .onSuccess { reply ->

                    messages.add(
                        DisplayMessage(
                            role = "model",
                            text = reply
                        )
                    )

                    // Store latest response for speaker replay.
                    latestAiReply = reply

                    // Automatically speak when speaker is ON.
                    if (voiceOutputEnabled) {

                        speak(reply)
                    }
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
            .statusBarsPadding()
    ) {

        // ====================================================
        // HEADER
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // =================================================
            // BACK BUTTON
            // =================================================

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
                    .clickable(
                        onClickLabel =
                            "Go back"
                    ) {

                        stopSpeaking()
                        onBack()
                    },
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "‹",
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp,
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
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
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
                    letterSpacing = 1.1.sp
                )
            }

            // =================================================
            // HEADER BUTTONS
            // =================================================

            Row(
                modifier = Modifier
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // =============================================
                // API BUTTON
                // =============================================

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(54.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            enabled = !isSending,
                            onClickLabel =
                                "Manage AI API key"
                        ) {

                            stopSpeaking()
                            showApiKeySetup = true
                        },
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "API",
                        color = Color(0xFFB76CFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // =============================================
                // REPORT
                // =============================================

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(72.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            enabled =
                                !isSending &&
                                        !contextLoading,
                            onClickLabel =
                                "Generate daily report"
                        ) {

                            sendMessage(
                                text =
                                    SmartAiPrompts
                                        .DAILY_REPORT_PROMPT,
                                displayText =
                                    SmartAiPrompts
                                        .DAILY_REPORT_DISPLAY_TEXT
                            )
                        },
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "REPORT",
                        color = Color(0xFF00D9FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // =============================================
                // STOP
                // =============================================

                if (isSpeaking) {

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(64.dp)
                            .clip(
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                onClickLabel =
                                    "Stop speaking"
                            ) {

                                stopSpeaking()
                            },
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "STOP",
                            color = Color(0xFFFF7B72),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // =============================================
                // SPEAKER
                // =============================================

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            onClickLabel =
                                if (voiceOutputEnabled) {
                                    "Turn speaker off"
                                } else {
                                    "Turn speaker on"
                                }
                        ) {

                            toggleSpeaker()
                        },
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            if (voiceOutputEnabled) {
                                "🔊"
                            } else {
                                "🔇"
                            },
                        fontSize = 16.sp
                    )
                }

                // =============================================
                // CLEAR
                // =============================================

                if (messages.isNotEmpty()) {

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(68.dp)
                            .clip(
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                enabled = !isSending,
                                onClickLabel =
                                    "Clear chat"
                            ) {

                                messages.clear()
                                inputText = ""
                                voiceError = ""
                            },
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "CLEAR",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
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
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        text = contextError,
                        color = Color(0xFFFF7B72),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                loadContext()
                            }
                            .padding(
                                horizontal = 6.dp
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "TAP TO RETRY",
                            color = Color(0xFFFF7B72),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                    }
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

            if (
                messages.isEmpty() &&
                !contextLoading
            ) {

                item {

                    Column(
                        modifier = Modifier.padding(
                            top = 16.dp
                        )
                    ) {

                        Text(
                            text =
                                "Ask about your deadlines, DSA progress, projects, or placement readiness.",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        SUGGESTED_PROMPTS.forEach { prompt ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        bottom = 8.dp
                                    )
                                    .clickable(
                                        enabled =
                                            !isSending &&
                                                    !contextLoading,
                                        onClickLabel =
                                            "Use suggested prompt"
                                    ) {

                                        sendMessage(prompt)
                                    },
                                shape =
                                    RoundedCornerShape(14.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme
                                                .surface
                                    ),
                                elevation =
                                    CardDefaults.cardElevation(
                                        defaultElevation = 1.dp
                                    )
                            ) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 15.dp
                                        )
                                ) {

                                    Text(
                                        text = prompt,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurface,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(4.dp)
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
                                                MaterialTheme.colorScheme
                                                    .surface
                                            )
                                            .clickable(
                                                enabled =
                                                    !isSending &&
                                                            !contextLoading,
                                                onClickLabel =
                                                    "Plan my day for ${option.first}"
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
                                                horizontal = 18.dp,
                                                vertical = 14.dp
                                            )
                                    ) {

                                        Text(
                                            text = option.first,
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurface,
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
                        modifier =
                            Modifier.padding(
                                vertical = 2.dp
                            ),
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
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
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
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
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

                    Text(
                        text = "Ask Smart AI...",
                        fontSize = 13.sp
                    )
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
            // MICROPHONE
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
                                    !contextLoading,
                        onClickLabel =
                            "Voice input"
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
            // SEND
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
                                    !isSending,
                        onClickLabel =
                            "Send message"
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment =
                    Alignment.CenterStart
            ) {

                Text(
                    text =
                        "Listening... speak now",
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp
                        ),
                    color = Color(0xFFB76CFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

        } else if (voiceError.isNotBlank()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment =
                    Alignment.CenterStart
            ) {

                Text(
                    text = voiceError,
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp
                        ),
                    color = Color(0xFFFF7B72),
                    fontSize = 10.sp
                )
            }
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
                    max = 300.dp
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
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
        ) {

            Text(
                text = message.text,
                modifier =
                    Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 12.dp
                    ),
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
