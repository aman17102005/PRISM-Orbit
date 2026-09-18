package com.prismorbit.app

enum class AiProvider(
    val displayName: String,
    val defaultModel: String
) {
    GEMINI(
        "Gemini (Google)",
        "gemini-3.6-flash"
    ),

    OPENAI(
        "ChatGPT (OpenAI)",
        "gpt-4o-mini"
    ),

    ANTHROPIC(
        "Claude (Anthropic)",
        "claude-haiku-4-5-20251001"
    ),

    PERPLEXITY(
        "Perplexity",
        "sonar"
    ),

    GROK(
        "Grok (xAI)",
        "grok-4.6"
    )
}