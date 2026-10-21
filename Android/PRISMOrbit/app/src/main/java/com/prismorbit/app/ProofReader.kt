package com.prismorbit.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

// ============================================================
// PROOF READER (on the phone, free, no key, no Firebase)
// ============================================================
// - PDF: only the first page is drawn, with the built-in PdfRenderer.
// - Big photos are made smaller (max about 1600 px) before reading.
// - Text is read with Google ML Kit (bundled model, works offline).
// - Only a tiny thumbnail and the check result are kept. The original
//   file and the full text are never stored.
// ============================================================

data class ProofReadResult(
    /** The text found. Kept in memory only, never saved. */
    val text: String,
    /** Tiny JPEG as Base64, or null if it could not be made small enough. */
    val thumbnailBase64: String?
)

object ProofReader {

    private const val MAX_SIDE_PX = 1600
    private const val THUMB_SIDE_PX = 360
    private const val THUMB_START_QUALITY = 60
    /** Keeps the Firestore document far below its 1 MiB limit. */
    const val MAX_THUMBNAIL_BASE64_CHARS = 40_000

    class ProofReadException(message: String) : Exception(message)

    suspend fun read(context: Context, uri: Uri): ProofReadResult = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            bitmap = loadBitmap(context, uri)
                ?: throw ProofReadException("We could not open this file.")

            val text = recognise(bitmap)
            val thumbnail = makeThumbnail(bitmap)
            ProofReadResult(text, thumbnail)
        } finally {
            bitmap?.recycle()
        }
    }

    // --------------------------------------------------------
    // Loading
    // --------------------------------------------------------

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase()
        val isPdf = mime == "application/pdf" || uri.toString().lowercase().endsWith(".pdf")
        return if (isPdf) renderFirstPdfPage(context, uri) else decodeImage(context, uri)
    }

    private fun renderFirstPdfPage(context: Context, uri: Uri): Bitmap? {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    // PDF pages are 72 dpi. Draw at 2x, but never above the size limit.
                    val scale = minOf(2f, MAX_SIDE_PX.toFloat() / max(page.width, page.height))
                    val width = max(1, (page.width * scale).toInt())
                    val height = max(1, (page.height * scale).toInt())
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // PdfRenderer draws on a transparent canvas, so paint white first.
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    private fun decodeImage(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX * 2) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotated = rotateByExif(context, uri, decoded)
        return scaleDown(rotated, MAX_SIDE_PX)
    }

    private fun rotateByExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = try {
            context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            max(1, (bitmap.width * scale).toInt()),
            max(1, (bitmap.height * scale).toInt()),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    // --------------------------------------------------------
    // Reading text
    // --------------------------------------------------------

    private suspend fun recognise(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult().text
        } finally {
            recognizer.close()
        }
    }

    // --------------------------------------------------------
    // Thumbnail (about 360 px, JPEG quality about 60, max about 40 KB)
    // --------------------------------------------------------

    private fun makeThumbnail(source: Bitmap): String? {
        var side = THUMB_SIDE_PX
        var quality = THUMB_START_QUALITY

        repeat(6) {
            val longest = max(source.width, source.height)
            val scale = minOf(1f, side.toFloat() / longest)
            val small = Bitmap.createScaledBitmap(
                source,
                max(1, (source.width * scale).toInt()),
                max(1, (source.height * scale).toInt()),
                true
            )
            val bytes = ByteArrayOutputStream().also {
                small.compress(Bitmap.CompressFormat.JPEG, quality, it)
            }.toByteArray()
            if (small !== source) small.recycle()

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (base64.length <= MAX_THUMBNAIL_BASE64_CHARS) return base64

            side = (side * 0.8f).toInt()
            quality = max(30, quality - 10)
        }
        // Better no thumbnail than a document that is too large.
        return null
    }

    /** Turns the stored Base64 thumbnail back into a picture. Null if it is missing or broken. */
    fun decodeThumbnail(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

/** The small result saved in the internship document under "proof". */
object ProofStorage {

    fun toMap(
        result: ProofCheckResult,
        thumbnailBase64: String?,
        checkedCompany: String,
        now: Long = System.currentTimeMillis()
    ): Map<String, Any?> = buildMap {
        put("status", result.status)
        put("companyMatch", result.companyMatch)
        put("nameMatch", result.nameMatch)
        put("keywordHits", result.keywordHits)
        put("datesFound", result.datesFound)
        put("checkedAt", now)
        put("checkedCompany", checkedCompany)
        // Never more than about 40 KB, so the 1 MiB document limit is safe.
        if (!thumbnailBase64.isNullOrBlank() && thumbnailBase64.length <= ProofReader.MAX_THUMBNAIL_BASE64_CHARS) {
            put("thumbnail", thumbnailBase64)
        }
    }

    fun summaryOf(map: Map<String, Any?>?): ProofSummary {
        if (map.isNullOrEmpty()) return ProofSummary()
        return ProofSummary(
            status = map["status"]?.toString().orEmpty(),
            companyMatch = map["companyMatch"] as? Boolean ?: false,
            nameMatch = map["nameMatch"] as? Boolean ?: false,
            keywordHits = (map["keywordHits"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            datesFound = (map["datesFound"] as? Number)?.toInt() ?: 0
        )
    }
}
