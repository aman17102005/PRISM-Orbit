package com.prismorbit.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class KnownDsaProblem(
    val name: String,
    val platform: String,
    val topic: String,
    val difficulty: String
)

object KnownDsaProblems {

    private var cached: List<KnownDsaProblem>? = null

    /**
     * Loads the problem list from assets/dsa_problems.json the first
     * time this is called. Safe to call every time the Add Problem
     * screen opens — after the first successful load, it does nothing
     * (so it's fast every time after the first).
     */
    fun loadIfNeeded(context: Context) {
        if (cached != null) return

        cached = try {
            val json = context.assets.open("dsa_problems.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<KnownDsaProblem>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            // If the file is missing or broken, autocomplete just shows
            // nothing instead of crashing the app.
            emptyList()
        }
    }

    fun search(query: String): List<KnownDsaProblem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val list = cached ?: return emptyList()

        return list
            .filter { it.name.lowercase().contains(q) }
            .distinctBy { "${it.name}|${it.platform}|${it.topic}" }
            .take(6)
    }
}