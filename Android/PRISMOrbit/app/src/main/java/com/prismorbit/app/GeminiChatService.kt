package com.prismorbit.app

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

// ============================================================
// GEMINI CHAT SERVICE
// ============================================================
//
// Sends the conversation so far + PRISM context to Gemini and
// returns the reply as plain text. This is free-form conversation
// (no responseSchema) — unlike GeminiSmartAiEnhancer.kt, which
// forces structured JSON for the dashboard card.
//
// The PRISM context (built by PrismContextBuilder) is sent as a
// systemInstruction — it's the model's grounding, refreshed each
// message, and the model is explicitly told not to override it.
// ============================================================

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)

private data class GeminiChatRequest(
    val systemInstruction: SystemInstruction,
    val contents: List<ChatContent>
)

private data class SystemInstruction(
    val parts: List<ChatPart>
)

private data class ChatContent(
    val role: String,
    val parts: List<ChatPart>
)

private data class ChatPart(
    val text: String
)

private data class GeminiChatResponse(
    val candidates: List<ChatCandidate>?
)

private data class ChatCandidate(
    val content: ChatContent?
)

private interface GeminiChatApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String = "gemini-3.6-flash",
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiChatRequest
    ): GeminiChatResponse
}

private object GeminiChatNetwork {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GeminiChatApiService = retrofit.create(GeminiChatApiService::class.java)
}

object GeminiChatService {

    private const val SYSTEM_PERSONA = """
        You are Smart AI, a helpful assistant inside PRISM Orbit, an academic and
        career-development app for college students. Below is the student's REAL,
        current PRISM data. Treat every fact in it as ground truth — never invent,
        guess, or override any number, date, or fact it contains. Any placement
        readiness score, priority number, or category listed there comes from
        PRISM's own scoring engine and must not be recalculated or contradicted.

        Be warm, direct and practical — like a good mentor, not a generic chatbot.
        Keep responses reasonably concise unless the student asks for detail.
        If asked "why" about a recommendation, explain using the reason given below.
    """

    suspend fun sendMessage(
        history: List<ChatTurn>,
        newUserMessage: String,
        prismContext: PrismAiContext,
        apiKey: String
    ): Result<String> {

        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Missing Gemini API key"))
        }

        return try {
            val systemText = SYSTEM_PERSONA.trimIndent() + "\n\n" + prismContext.toPromptText()

            val contents = history.map { turn ->
                ChatContent(role = turn.role, parts = listOf(ChatPart(text = turn.text)))
            } + ChatContent(role = "user", parts = listOf(ChatPart(text = newUserMessage)))

            val request = GeminiChatRequest(
                systemInstruction = SystemInstruction(parts = listOf(ChatPart(text = systemText))),
                contents = contents
            )

            val response = GeminiChatNetwork.service.generateContent(
                apiKey = apiKey,
                request = request
            )

            val replyText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (replyText.isNullOrBlank()) {
                Result.failure(Exception("Empty response from Smart AI"))
            } else {
                Result.success(replyText)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}