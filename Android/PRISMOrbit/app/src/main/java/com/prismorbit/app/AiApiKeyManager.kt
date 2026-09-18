package com.prismorbit.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// ============================================================
// AI API KEY MANAGER
// ============================================================
// Stores each signed-in user's own AI key locally.
//
// - Keys are encrypted using Android Keystore.
// - Keys are never stored in Firestore.
// - Each key is scoped to the Firebase UID.
// - Different signed-in users therefore have separate keys.
// ============================================================

data class StoredAiKey(
    val provider: AiProvider,
    val apiKey: String
)

object AiApiKeyManager {

    private const val PREFS_FILE_NAME = "prism_ai_keys"
    private const val KEYSTORE_ALIAS = "prism_ai_key_encryption"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_FILE_NAME,
            Context.MODE_PRIVATE
        )

    // --------------------------------------------------------
    // Encryption
    // --------------------------------------------------------

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let {
            return it
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()
        )

        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val cipherBytes = cipher.doFinal(
            plainText.toByteArray(Charsets.UTF_8)
        )

        val iv = cipher.iv

        return Base64.encodeToString(
            iv,
            Base64.NO_WRAP
        ) + ":" +
                Base64.encodeToString(
                    cipherBytes,
                    Base64.NO_WRAP
                )
    }

    private fun decrypt(stored: String): String? {
        return try {
            val parts = stored.split(":")

            if (parts.size != 2) {
                return null
            }

            val iv = Base64.decode(
                parts[0],
                Base64.NO_WRAP
            )

            val cipherBytes = Base64.decode(
                parts[1],
                Base64.NO_WRAP
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(128, iv)
            )

            String(
                cipher.doFinal(cipherBytes),
                Charsets.UTF_8
            )

        } catch (_: Exception) {
            null
        }
    }

    // --------------------------------------------------------
    // Public key storage API
    // --------------------------------------------------------

    fun getKey(
        context: Context,
        uid: String
    ): StoredAiKey? {

        val providerName = prefs(context)
            .getString("provider_$uid", null)
            ?: return null

        val encryptedKey = prefs(context)
            .getString("key_$uid", null)
            ?: return null

        val provider = AiProvider.entries.find {
            it.name == providerName
        } ?: return null

        val apiKey = decrypt(encryptedKey)
            ?: return null

        return StoredAiKey(
            provider = provider,
            apiKey = apiKey
        )
    }

    fun hasKey(
        context: Context,
        uid: String
    ): Boolean {
        return getKey(context, uid) != null
    }

    fun saveKey(
        context: Context,
        uid: String,
        provider: AiProvider,
        apiKey: String
    ) {
        prefs(context)
            .edit()
            .putString(
                "provider_$uid",
                provider.name
            )
            .putString(
                "key_$uid",
                encrypt(apiKey.trim())
            )
            .apply()
    }

    fun deleteKey(
        context: Context,
        uid: String
    ) {
        prefs(context)
            .edit()
            .remove("provider_$uid")
            .remove("key_$uid")
            .apply()
    }

    // --------------------------------------------------------
    // API key validation
    // --------------------------------------------------------

    sealed class ValidationResult {
        data object Valid : ValidationResult()

        data class Invalid(
            val message: String
        ) : ValidationResult()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun validateKey(
        provider: AiProvider,
        apiKey: String
    ): ValidationResult {

        val trimmed = apiKey.trim()

        if (trimmed.isBlank()) {
            return ValidationResult.Invalid(
                "Please enter an API key."
            )
        }

        return withContext(Dispatchers.IO) {

            try {

                val request = when (provider) {

                    // ------------------------------------------------
                    // GEMINI
                    // ------------------------------------------------
                    AiProvider.GEMINI -> {
                        Request.Builder()
                            .url(
                                "https://generativelanguage.googleapis.com/v1beta/models"
                            )
                            .addHeader(
                                "x-goog-api-key",
                                trimmed
                            )
                            .get()
                            .build()
                    }

                    // ------------------------------------------------
                    // OPENAI
                    // ------------------------------------------------
                    AiProvider.OPENAI -> {
                        Request.Builder()
                            .url(
                                "https://api.openai.com/v1/models"
                            )
                            .addHeader(
                                "Authorization",
                                "Bearer $trimmed"
                            )
                            .get()
                            .build()
                    }

                    // ------------------------------------------------
                    // ANTHROPIC
                    // ------------------------------------------------
                    AiProvider.ANTHROPIC -> {
                        Request.Builder()
                            .url(
                                "https://api.anthropic.com/v1/models"
                            )
                            .addHeader(
                                "x-api-key",
                                trimmed
                            )
                            .addHeader(
                                "anthropic-version",
                                "2023-06-01"
                            )
                            .get()
                            .build()
                    }

                    // ------------------------------------------------
                    // GROK
                    // ------------------------------------------------
                    AiProvider.GROK -> {
                        Request.Builder()
                            .url(
                                "https://api.x.ai/v1/models"
                            )
                            .addHeader(
                                "Authorization",
                                "Bearer $trimmed"
                            )
                            .get()
                            .build()
                    }

                    // ------------------------------------------------
                    // PERPLEXITY
                    // ------------------------------------------------
                    AiProvider.PERPLEXITY -> {

                        val mediaType =
                            "application/json".toMediaType()

                        val requestBody =
                            """
                            {
                              "model": "${provider.defaultModel}",
                              "messages": [
                                {
                                  "role": "user",
                                  "content": "hi"
                                }
                              ],
                              "max_tokens": 1
                            }
                            """.trimIndent()
                                .toRequestBody(mediaType)

                        Request.Builder()
                            .url(
                                "https://api.perplexity.ai/chat/completions"
                            )
                            .addHeader(
                                "Authorization",
                                "Bearer $trimmed"
                            )
                            .addHeader(
                                "Content-Type",
                                "application/json"
                            )
                            .post(requestBody)
                            .build()
                    }
                }

                val response =
                    httpClient.newCall(request).execute()

                response.use {

                    when {

                        it.isSuccessful -> {
                            ValidationResult.Valid
                        }

                        it.code == 401 ||
                                it.code == 403 -> {

                            ValidationResult.Invalid(
                                "This key was rejected — check it was copied correctly."
                            )
                        }

                        it.code == 429 -> {

                            ValidationResult.Invalid(
                                "This key is rate-limited right now — it may still be valid, try again shortly."
                            )
                        }

                        else -> {

                            ValidationResult.Invalid(
                                "Unexpected response (code ${it.code}) — try again."
                            )
                        }
                    }
                }

            } catch (_: java.net.UnknownHostException) {

                ValidationResult.Invalid(
                    "No internet connection."
                )

            } catch (e: Exception) {

                ValidationResult.Invalid(
                    e.message
                        ?: "Unable to check this key right now."
                )
            }
        }
    }
}