package com.prismorbit.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// ============================================================
// PRISM AI CONTEXT BUILDER
// ============================================================
//
// Assembles a PrismAiContext for a given user by querying only
// the Firestore collections it needs, and by calling the EXISTING
// loadSmartAiInsight() from SmartAiEngine.kt unchanged for the
// placement/priority numbers.
//
// This is intentionally simple for Phase 3 — it builds one
// general-purpose context. Per-intent filtering (e.g. only fetch
// DSA data for a DSA-specific question) can be added later without
// changing the shape of PrismAiContext itself.
// ============================================================

object PrismContextBuilder {

    suspend fun buildContext(
        firestore: FirebaseFirestore,
        uid: String
    ): PrismAiContext {

        val eventsSnapshot = awaitGetCollection(
            firestore.collection("users").document(uid)
                .collection("academicEvents").get()
        )

        val dsaSnapshot = awaitGetCollection(
            firestore.collection("users").document(uid)
                .collection("dsaProblems").get()
        )

        val projectsSnapshot = awaitGetCollection(
            firestore.collection("users").document(uid)
                .collection("projects").get()
        )

        val internshipsSnapshot = awaitGetCollection(
            firestore.collection("users").document(uid)
                .collection("internships").get()
        )

        val placementInsight = awaitSmartAiInsight(firestore, uid)

        return PrismAiContext(
            todayDateText = todayDateText(),
            upcomingEvents = buildUpcomingEvents(eventsSnapshot),
            dsaSummary = buildDsaSummary(dsaSnapshot),
            projectsSummary = buildProjectsSummary(projectsSnapshot),
            internshipCount = internshipsSnapshot.documents.size,
            placementInsight = placementInsight
        )
    }

    // --------------------------------------------------------
    // ACADEMIC EVENTS — next 7 days, excluding completed/cancelled
    // --------------------------------------------------------

    private fun buildUpcomingEvents(snapshot: QuerySnapshot): List<UpcomingEventSummary> {
        val today = todayStartOfDay()

        return snapshot.documents.mapNotNull { document ->
            val status = document.getString("status")?.trim()?.uppercase(Locale.ROOT) ?: "UPCOMING"
            if (status == "COMPLETED" || status == "CANCELLED") return@mapNotNull null

            val dateText = document.getString("date")?.trim().orEmpty()
            val eventDate = parseDate(dateText) ?: return@mapNotNull null

            val eventCalendar = Calendar.getInstance().apply {
                time = eventDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val daysUntil = (
                    (eventCalendar.timeInMillis - today.timeInMillis) /
                            (24L * 60L * 60L * 1000L)
                    ).toInt()

            if (daysUntil !in 0..7) return@mapNotNull null

            UpcomingEventSummary(
                title = document.getString("title")?.trim().orEmpty().ifBlank { "Untitled event" },
                subject = document.getString("subject")?.trim().orEmpty(),
                type = document.getString("type")?.trim().orEmpty().ifBlank { "Event" },
                daysUntil = daysUntil
            )
        }.sortedBy { it.daysUntil }
    }

    // --------------------------------------------------------
    // DSA SUMMARY
    // --------------------------------------------------------
    // Note: the weighted-score formula mirrors calculateDsaProgress()
    // in HomeScreen.kt (1000-point target). That function is private
    // to its file, so it's intentionally re-implemented here rather
    // than duplicated via a shared dependency, to avoid touching
    // existing files. If the target ever changes, update both places.

    private fun buildDsaSummary(snapshot: QuerySnapshot): DsaSummary {
        val documents = snapshot.documents

        val easy = documents.count { it.getString("difficulty") == "Easy" }
        val medium = documents.count { it.getString("difficulty") == "Medium" }
        val hard = documents.count { it.getString("difficulty") == "Hard" }

        val weightedScore = documents.sumOf { document ->
            (document.getDouble("score") ?: document.getLong("score")?.toDouble() ?: 0.0)
        }

        val weightedPercent = ((weightedScore / 1000.0) * 100.0)
            .coerceIn(0.0, 100.0)
            .toInt()

        return DsaSummary(
            totalSolved = documents.size,
            easyCount = easy,
            mediumCount = medium,
            hardCount = hard,
            weightedScorePercent = weightedPercent
        )
    }

    // --------------------------------------------------------
    // PROJECTS SUMMARY
    // --------------------------------------------------------

    private fun buildProjectsSummary(snapshot: QuerySnapshot): ProjectsSummary {
        val documents = snapshot.documents
        if (documents.isEmpty()) {
            return ProjectsSummary(totalProjects = 0, averageProgressPercent = 0, completedCount = 0)
        }

        val progressValues = documents.map { document ->
            when (val raw = document.get("progress")) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull() ?: 0
                else -> 0
            }.coerceIn(0, 100)
        }

        val completed = documents.count {
            it.getString("status")?.trim()?.uppercase(Locale.ROOT) == "COMPLETED"
        }

        return ProjectsSummary(
            totalProjects = documents.size,
            averageProgressPercent = progressValues.average().toInt(),
            completedCount = completed
        )
    }

    // --------------------------------------------------------
    // Reuses the EXISTING loadSmartAiInsight() from SmartAiEngine.kt
    // (callback-based) via a suspend wrapper — no changes to that file.
    // --------------------------------------------------------

    private suspend fun awaitSmartAiInsight(
        firestore: FirebaseFirestore,
        uid: String
    ): SmartAiInsight? = suspendCancellableCoroutine { continuation ->
        loadSmartAiInsight(
            firestore = firestore,
            uid = uid,
            onSuccess = { insight -> continuation.resume(insight) },
            onError = { continuation.resume(null) } // context builder degrades gracefully
        )
    }

    // --------------------------------------------------------
    // Date helpers
    // --------------------------------------------------------

    private fun todayStartOfDay(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun todayDateText(): String {
        return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Calendar.getInstance().time)
    }

    private fun parseDate(value: String): java.util.Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    // --------------------------------------------------------
    // Small Task-to-suspend bridge (same pattern used in PrismReminderWorker)
    // --------------------------------------------------------

    private suspend fun awaitGetCollection(
        task: com.google.android.gms.tasks.Task<QuerySnapshot>
    ): QuerySnapshot = suspendCancellableCoroutine { continuation ->
        task
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }
}