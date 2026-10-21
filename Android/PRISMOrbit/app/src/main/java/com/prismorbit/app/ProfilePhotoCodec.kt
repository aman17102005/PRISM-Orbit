package com.prismorbit.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

// ============================================================
// PROFILE PHOTO CODEC
// ============================================================
// The profile photo is made small (about 256 px, JPEG quality about 70)
// and saved as Base64 text in the student's own Firestore profile
// document. This keeps everything free: no Firebase Storage.
// ============================================================

object ProfilePhotoCodec {

    private const val MAX_SIDE_PX = 256
    private const val START_QUALITY = 70

    /** Keeps the profile document far below the 1 MiB Firestore limit. */
    private const val MAX_BASE64_CHARS = 60_000

    /** Reads the picked image, shrinks it and returns Base64 text. Null if it cannot be read. */
    fun encode(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2

            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val scale = minOf(1f, MAX_SIDE_PX.toFloat() / max(decoded.width, decoded.height))
            val small = Bitmap.createScaledBitmap(
                decoded,
                max(1, (decoded.width * scale).toInt()),
                max(1, (decoded.height * scale).toInt()),
                true
            )

            var quality = START_QUALITY
            var result: String? = null
            while (quality >= 30) {
                val bytes = ByteArrayOutputStream().also {
                    small.compress(Bitmap.CompressFormat.JPEG, quality, it)
                }.toByteArray()
                val text = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (text.length <= MAX_BASE64_CHARS) {
                    result = text
                    break
                }
                quality -= 10
            }

            if (small !== decoded) small.recycle()
            decoded.recycle()
            result
        } catch (_: Exception) {
            null
        }
    }

    fun decode(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
