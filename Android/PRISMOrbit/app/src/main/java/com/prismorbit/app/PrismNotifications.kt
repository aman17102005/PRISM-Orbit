package com.prismorbit.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// ============================================================
// NOTIFICATION HELPER
// ============================================================
//
// Single place that owns the notification channel and knows how
// to post a reminder notification. Nothing else in the app should
// build notifications directly — call showAcademicReminder() from
// wherever it's needed (currently: PrismReminderWorker).
// ============================================================

object PrismNotifications {

    const val CHANNEL_ID = "prism_academic_reminders"
    private const val CHANNEL_NAME = "Academic Reminders"
    private const val CHANNEL_DESCRIPTION =
        "Reminders for upcoming exams, vivas, tests and practicals"

    /**
     * Must be called once before any notification is shown.
     * Safe to call multiple times — creating an existing channel is a no-op.
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a reminder notification for an academic event.
     * [notificationId] should be stable per event (e.g. event.id.toInt())
     * so the same event doesn't stack duplicate notifications.
     */
    // Lint's flow analysis can't trace the permission check through the
    // `hasPermission` variable below — the check genuinely happens, lint
    // just can't see it. This suppression is safe given that.
    @SuppressLint("MissingPermission")
    fun showAcademicReminder(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // replace with your app icon if you have one
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // On Android 13+, POST_NOTIFICATIONS must be explicitly granted.
        // Checking it here (instead of just relying on try/catch) is what
        // satisfies Android Studio's lint check on the notify() call below.
        val hasPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
        // If permission isn't granted, nothing happens here — the user will
        // be prompted again the next time they open the app (see MainActivity).
    }
}