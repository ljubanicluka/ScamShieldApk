package rs.scamshield.app

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Runs the real gallery decoder (ImageDecoder/BitmapFactory + ML Kit) against the PNG fixtures in
 * app/src/androidTest/assets/qr. Needs a device or emulator: `./gradlew connectedDebugAndroidTest`.
 * It is NOT run by GitHub Actions unless an emulator job is added.
 */
class QrImageDecoderInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val testContext = instrumentation.context
    private val appContext = instrumentation.targetContext

    private fun fixture(name: String): Uri {
        val out = File(appContext.cacheDir, "fixture-$name")
        testContext.assets.open("qr/$name").use { input -> out.outputStream().use { input.copyTo(it) } }
        return Uri.fromFile(out)
    }

    private fun decode(uri: Uri): QrImageResult {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        try {
            return QrImageDecoder(appContext, scanner).decode(uri)
        } finally {
            scanner.close()
        }
    }

    @Test fun decodesEveryFixtureAsExpected() {
        val expected = JSONObject(testContext.assets.open("qr/expected.json").bufferedReader().readText())
        for (name in expected.keys()) {
            val want = expected.getJSONArray(name)
            val got = decode(fixture(name))
            if (want.length() == 0) {
                assertEquals("$name should contain no QR code", QrImageResult.NoQr, got)
            } else {
                assertTrue("$name should be found, got $got", got is QrImageResult.Found)
                val payloads = (got as QrImageResult.Found).payloads
                for (i in 0 until want.length()) {
                    assertTrue("$name missing ${want.getString(i)} in $payloads", payloads.contains(want.getString(i)))
                }
            }
        }
    }

    @Test fun corruptFileIsReportedAsUnreadable() {
        val bad = File(appContext.cacheDir, "corrupt.png").apply { writeBytes(ByteArray(2048) { (it * 7).toByte() }) }
        assertEquals(QrImageResult.Unreadable, decode(Uri.fromFile(bad)))
    }

    @Test fun missingFileIsReportedAsUnreadable() {
        assertEquals(QrImageResult.Unreadable, decode(Uri.fromFile(File(appContext.cacheDir, "does-not-exist.png"))))
    }
}
