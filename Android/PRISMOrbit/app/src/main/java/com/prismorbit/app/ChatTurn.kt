package com.prismorbit.app

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)