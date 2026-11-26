package rs.scamshield.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

/** Outcome of decoding one gallery image. Nothing here leaves the device. */
sealed class QrImageResult {
    data class Found(val payloads: List<String>) : QrImageResult()
    object NoQr : QrImageResult()
    object Unreadable : QrImageResult()
    object TooLarge : QrImageResult()
}

/**
 * Decodes a content:// image with the bundled ML Kit model.
 *
 * - The picture is decoded down-sampled (long side <= 2048 px first), never at full resolution,
 *   so a 50 MP photo does not exhaust memory.
 * - If nothing is found it retries at smaller sizes and finally with a white border, which
 *   helps screenshots whose QR code is cropped tight to the edge.
 * - API 28+ uses ImageDecoder (JPEG, PNG, WebP, HEIC where the device supports it);
 *   older devices use BitmapFactory plus manual EXIF rotation. QR detection itself does not
 *   depend on rotation.
 *
 * Call from a background thread: it blocks while ML Kit runs.
 */
class QrImageDecoder(
    private val context: Context,
    private val scanner: BarcodeScanner
) {
    private class LoadFailure(val result: QrImageResult) : Exception()

    fun decode(uri: Uri): QrImageResult {
        var lastBitmapSize: Pair<Int, Int>? = null
        var failure: QrImageResult? = null
        var scannerFailed = false
        var anyScanned = false

        for (maxSide in LONG_SIDES) {
            val bitmap = try {
                load(uri, maxSide)
            } catch (e: LoadFailure) {
                failure = e.result
                if (e.result === QrImageResult.TooLarge) continue else break
            }
            try {
                val size = bitmap.width to bitmap.height
                if (size == lastBitmapSize) continue
                lastBitmapSize = size
                when (val outcome = scan(bitmap)) {
                    is Scan.Hit -> return QrImageResult.Found(outcome.payloads)
                    Scan.Miss -> anyScanned = true
                    Scan.Error -> scannerFailed = true
                }
            } finally {
                bitmap.recycle()
            }
        }

        if (!anyScanned && failure != null) return failure
        if (!anyScanned && scannerFailed) return QrImageResult.Unreadable

        // Last attempt: add a quiet-zone border to a mid-size copy.
        try {
            val base = load(uri, PADDED_LONG_SIDE)
            val padded = try {
                addWhiteBorder(base)
            } finally {
                base.recycle()
            }
            try {
                val outcome = scan(padded)
                if (outcome is Scan.Hit) return QrImageResult.Found(outcome.payloads)
            } finally {
                padded.recycle()
            }
        } catch (_: LoadFailure) {
            // The earlier attempts already decided the result.
        } catch (_: OutOfMemoryError) {
            // Skip the padded attempt on very low-memory devices.
        }
        return QrImageResult.NoQr
    }

    private sealed class Scan {
        class Hit(val payloads: List<String>) : Scan()
        object Miss : Scan()
        object Error : Scan()
    }

    private fun scan(bitmap: Bitmap): Scan {
        return try {
            val barcodes = Tasks.await(
                scanner.process(InputImage.fromBitmap(bitmap, 0)),
                SCAN_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            val ordered = barcodes.sortedWith(
                compareBy<Barcode>({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 })
            )
            val payloads = QrSampling.cleanPayloads(ordered.map { it.rawValue })
            if (payloads.isEmpty()) Scan.Miss else Scan.Hit(payloads)
        } catch (_: Exception) {
            Scan.Error
        }
    }

    private fun load(uri: Uri, maxSide: Int): Bitmap {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                loadWithImageDecoder(uri, maxSide)
            } else {
                loadWithBitmapFactory(uri, maxSide)
            }
        } catch (e: LoadFailure) {
            throw e
        } catch (_: OutOfMemoryError) {
            throw LoadFailure(QrImageResult.TooLarge)
        } catch (_: IOException) {
            throw LoadFailure(QrImageResult.Unreadable)
        } catch (_: SecurityException) {
            throw LoadFailure(QrImageResult.Unreadable)
        } catch (_: RuntimeException) {
            throw LoadFailure(QrImageResult.Unreadable)
        }
    }

    @Suppress("NewApi")
    private fun loadWithImageDecoder(uri: Uri, maxSide: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // ML Kit reads pixels, so a hardware bitmap would fail.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longSide = max(info.size.width, info.size.height)
            val sample = QrSampling.sampleSize(longSide, maxSide)
            if (sample > 1) decoder.setTargetSampleSize(sample)
        }
    }

    private fun loadWithBitmapFactory(uri: Uri, maxSide: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: throw LoadFailure(QrImageResult.Unreadable)
        // With inJustDecodeBounds the call returns null by design; only outWidth/outHeight matter.
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw LoadFailure(QrImageResult.Unreadable)
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = QrSampling.sampleSize(max(bounds.outWidth, bounds.outHeight), maxSide)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw LoadFailure(QrImageResult.Unreadable)

        val rotation = exifRotation(uri)
        if (rotation == 0) return decoded
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private fun exifRotation(uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (_: Exception) {
        0
    }

    private fun addWhiteBorder(source: Bitmap): Bitmap {
        val border = max(source.width, source.height) / 8
        val padded = Bitmap.createBitmap(
            source.width + 2 * border,
            source.height + 2 * border,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(padded)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, border.toFloat(), border.toFloat(), null)
        return padded
    }

    companion object {
        private val LONG_SIDES = intArrayOf(2048, 1280, 768)
        private const val PADDED_LONG_SIDE = 1280
        private const val SCAN_TIMEOUT_SECONDS = 10L
    }
}
