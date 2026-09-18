package com.prismorbit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// ============================================================
// SMART AI API KEY SETUP / MANAGEMENT SCREEN
// ============================================================
// Lets the current signed-in user:
//
// - Choose an AI provider
// - Paste their own API key
// - Validate the key
// - Save the key securely
// - Replace an existing key at any time
// - Remove the saved key
//
// Keys are stored locally and are never written to Firestore.
// ============================================================

// Bumping this value tells SmartAiChatScreen to reload the
// currently configured key immediately after save/delete.
object AiKeyRefreshSignal {
    var version by mutableStateOf(0)
}

@Composable
fun SmartAiApiKeySetupScreen(
    onBack: () -> Unit
) {
    val localContext = LocalContext.current

    val uid = FirebaseAuth
        .getInstance()
        .currentUser
        ?.uid

    val coroutineScope = rememberCoroutineScope()

    var selectedProvider by remember {
        mutableStateOf(AiProvider.GEMINI)
    }

    var inputKey by remember {
        mutableStateOf("")
    }

    var showKey by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    var messageIsError by remember {
        mutableStateOf(false)
    }

    var currentlyConfigured by remember {
        mutableStateOf<StoredAiKey?>(null)
    }

    // ========================================================
    // LOAD CURRENT CONFIGURATION
    // ========================================================

    LaunchedEffect(uid) {

        val currentUid = uid ?: return@LaunchedEffect

        currentlyConfigured =
            AiApiKeyManager.getKey(
                localContext,
                currentUid
            )

        currentlyConfigured?.let {
            selectedProvider = it.provider
        }
    }

    // ========================================================
    // SAVE / REPLACE API KEY
    // ========================================================

    fun saveKey() {

        val currentUid = uid

        if (currentUid == null) {
            message = "No signed-in user found."
            messageIsError = true
            return
        }

        val trimmed = inputKey.trim()

        if (trimmed.isBlank()) {
            message = "Please enter your API key."
            messageIsError = true
            return
        }

        isSaving = true
        message = "Checking your key..."
        messageIsError = false

        coroutineScope.launch {

            when (
                val result =
                    AiApiKeyManager.validateKey(
                        selectedProvider,
                        trimmed
                    )
            ) {

                is AiApiKeyManager.ValidationResult.Valid -> {

                    // Save the new key locally.
                    // This replaces the previously configured key.
                    AiApiKeyManager.saveKey(
                        context = localContext,
                        uid = currentUid,
                        provider = selectedProvider,
                        apiKey = trimmed
                    )

                    // Tell Smart AI Chat to reload immediately.
                    AiKeyRefreshSignal.version++

                    currentlyConfigured =
                        StoredAiKey(
                            provider = selectedProvider,
                            apiKey = trimmed
                        )

                    // Never keep the raw key in the input field.
                    inputKey = ""
                    showKey = false
                    isSaving = false

                    message =
                        "${selectedProvider.displayName} key saved — Smart AI is ready."

                    messageIsError = false
                }

                is AiApiKeyManager.ValidationResult.Invalid -> {

                    isSaving = false

                    message = result.message
                    messageIsError = true
                }
            }
        }
    }

    // ========================================================
    // REMOVE CURRENT API KEY
    // ========================================================

    fun removeKey() {

        val currentUid = uid ?: return

        AiApiKeyManager.deleteKey(
            localContext,
            currentUid
        )

        AiKeyRefreshSignal.version++

        currentlyConfigured = null
        inputKey = ""
        showKey = false

        message = "API key removed."
        messageIsError = false
    }

    // ========================================================
    // UI
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 20.dp,
                vertical = 22.dp
            )
    ) {

        // ====================================================
        // HEADER
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
                    .clickable(
                        onClickLabel = "Go back",
                        onClick = onBack
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "‹",
                    modifier = Modifier.padding(
                        start = 15.dp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "SMART AI SETUP",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "API KEY MANAGEMENT",
                    color = Color(0xFFB76CFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // ====================================================
        // CURRENT CONFIGURATION
        // ====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            if (currentlyConfigured != null) {
                                "✓"
                            } else {
                                "⚠"
                            },
                        color =
                            if (currentlyConfigured != null) {
                                Color(0xFF65E572)
                            } else {
                                Color(0xFFFFD23F)
                            },
                        fontSize = 18.sp
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text =
                                if (currentlyConfigured != null) {
                                    "AI provider configured"
                                } else {
                                    "No AI key configured yet"
                                },
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        currentlyConfigured?.let {

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Text(
                                text = it.provider.displayName,
                                color = Color(0xFFB76CFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (currentlyConfigured != null) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "You can replace this key anytime by selecting a provider and saving a new key.",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // ====================================================
        // PROVIDER PICKER
        // ====================================================

        Text(
            text = "Choose which company you got a key from:",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        AiProvider.entries.forEach { provider ->

            val selected =
                selectedProvider == provider

            OutlinedButton(
                onClick = {
                    selectedProvider = provider
                    message = ""
                    messageIsError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp),
                shape = RoundedCornerShape(12.dp),
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
                            MaterialTheme.colorScheme.onSurface
                        }
                )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = provider.displayName,
                        fontSize = 12.sp
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

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        // ====================================================
        // API KEY INPUT
        // ====================================================

        OutlinedTextField(
            value = inputKey,
            onValueChange = {
                inputKey = it
                message = ""
                messageIsError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "${selectedProvider.displayName} API key"
                )
            },
            placeholder = {
                Text("Paste your key here")
            },
            singleLine = true,
            enabled = !isSaving,
            visualTransformation =
                if (showKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {

                Text(
                    text =
                        if (showKey) {
                            "HIDE"
                        } else {
                            "SHOW"
                        },
                    color = Color(0xFF00D9FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            showKey = !showKey
                        }
                        .padding(12.dp)
                )
            }
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        // ====================================================
        // SAVE / REPLACE BUTTON
        // ====================================================

        Button(
            onClick = {
                saveKey()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B4DFF)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text =
                    if (isSaving) {
                        "CHECKING..."
                    } else if (currentlyConfigured != null) {
                        "REPLACE API KEY"
                    } else {
                        "SAVE API KEY"
                    },
                fontWeight = FontWeight.Bold
            )
        }

        // ====================================================
        // REMOVE BUTTON
        // ====================================================

        if (currentlyConfigured != null) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = {
                    removeKey()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = "REMOVE API KEY",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ====================================================
        // STATUS / ERROR MESSAGE
        // ====================================================

        if (message.isNotBlank()) {

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (messageIsError) {
                            Color(0xFF241416)
                        } else {
                            Color(0xFF14251A)
                        }
                )
            ) {

                Text(
                    text = message,
                    color =
                        if (messageIsError) {
                            Color(0xFFFF7B72)
                        } else {
                            Color(0xFF65E572)
                        },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(13.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}