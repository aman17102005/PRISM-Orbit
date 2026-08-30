package com.prismorbit.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
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
// PRISM REMINDER WORKER
// ============================================================
//
// Runs periodically in the background (scheduled from MainActivity).
// Checks the signed-in user's academicEvents in Firestore and posts
// a local notification for anything due within the next 1-2 days.
//
// This does NOT modify SmartAiEngine.kt's academic-urgency logic —
// it's a separate, simpler check purely for notifications.
// ============================================================

class PrismReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success() // not signed in — nothing to check

        val firestore = FirebaseFirestore.getInstance()

        return try {
            val userDoc = awaitGet(
                firestore.collection("users").document(uid).get()
            )

            val settings = userDoc.get("settings") as? Map<*, *>
            val notificationsEnabled = settings?.get("notificationsEnabled") as? Boolean ?: true

            if (!notificationsEnabled) {
                return Result.success() // user turned notifications off
            }

            val eventsSnapshot = awaitGetCollection(
                firestore.collection("users").document(uid)
                    .collection("academicEvents").get()
            )

            val today = todayStartOfDay()

            eventsSnapshot.documents.forEach { document ->
                val status = document.getString("status")?.trim()?.uppercase(Locale.ROOT) ?: "UPCOMING"
                if (status == "COMPLETED" || status == "CANCELLED") return@forEach

                val dateText = document.getString("date")?.trim().orEmpty()
                val eventDate = parseDate(dateText) ?: return@forEach

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

                // Only notify for events 1-2 days away.
                if (daysUntil in 1..2) {
                    val title = document.getString("title")?.trim().orEmpty()
                    val subject = document.getString("subject")?.trim().orEmpty()

                    val displayTitle = if (title.isNotBlank()) title else "Upcoming academic event"
                    val whenText = if (daysUntil == 1) "tomorrow" else "in $daysUntil days"
                    val message = if (subject.isNotBlank()) {
                        "$subject is $whenText. Time to prepare."
                    } else {
                        "This event is $whenText. Time to prepare."
                    }

                    val notificationId = document.id.hashCode()

                    PrismNotifications.showAcademicReminder(
                        context = applicationContext,
                        notificationId = notificationId,
                        title = displayTitle,
                        message = message
                    )
                }
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun todayStartOfDay(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun parseDate(value: String): java.util.Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    // Small bridge so we can use Firestore's Task-based API inside a suspend function
    // without adding the kotlinx-coroutines-play-services dependency.
    private suspend fun awaitGet(
        task: com.google.android.gms.tasks.Task<DocumentSnapshot>
    ): DocumentSnapshot = suspendCancellableCoroutine { continuation ->
        task
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    private suspend fun awaitGetCollection(
        task: com.google.android.gms.tasks.Task<QuerySnapshot>
    ): QuerySnapshot = suspendCancellableCoroutine { continuation ->
        task
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }
}