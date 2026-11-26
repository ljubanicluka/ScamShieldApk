package rs.scamshield.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen, native QR reader. The bundled ML Kit model means decoding works offline
 * immediately after installation; it does not depend on WebView camera support.
 *
 * Contract with MainActivity (see companion constants):
 * - RESULT_OK + EXTRA_QR_TEXT (+ EXTRA_QR_ALL) when a code was read. The text is never opened.
 * - RESULT_CANCELED + EXTRA_QR_ERROR_CODE when permission was denied or no camera works.
 * - RESULT_CANCELED with no extras when the user pressed Cancel or Back.
 */
class QrScannerActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusView: TextView
    private lateinit var torchButton: Button
    private lateinit var retryButton: Button
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scanner: BarcodeScanner
    private lateinit var lang: String
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var analysis: ImageAnalysis? = null
    private var torchLive: LiveData<Int>? = null
    private val resultSent = AtomicBoolean(false)

    private val slowHint = Runnable {
        if (!resultSent.get() && !isFinishing && retryButton.visibility != View.VISIBLE) {
            statusView.text = QrStrings.text(QrStrings.Msg.SLOW_HINT, lang)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            finishWithError("camera_denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lang = QrStrings.normalizeLang(intent?.getStringExtra(EXTRA_LANG))
        cameraExecutor = Executors.newSingleThreadExecutor()
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        scanner = BarcodeScanning.getClient(options)

        setContentView(createContentView())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            finishWithError("no_camera")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun createContentView(): FrameLayout {
        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(Color.BLACK)
            contentDescription = QrStrings.text(QrStrings.Msg.POINT_CAMERA, lang)
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    focusAt(event.x, event.y)
                    view.performClick()
                }
                true
            }
        }

        statusView = TextView(this).apply {
            text = QrStrings.text(QrStrings.Msg.POINT_CAMERA, lang)
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.argb(210, 18, 28, 46))
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

        torchButton = Button(this).apply {
            text = QrStrings.text(QrStrings.Msg.FLASH, lang)
            isEnabled = false
            setOnClickListener { toggleTorch() }
        }
        retryButton = Button(this).apply {
            text = QrStrings.text(QrStrings.Msg.RETRY, lang)
            visibility = View.GONE
            setOnClickListener { restartScanning() }
        }
        val cancelButton = Button(this).apply {
            text = QrStrings.text(QrStrings.Msg.CANCEL, lang)
            setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(12))
            setBackgroundColor(Color.argb(190, 12, 18, 28))
            addView(
                statusView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            val buttons = LinearLayout(this@QrScannerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                addView(torchButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(retryButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(cancelButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(
                buttons,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        // Edge-to-edge: keep the buttons clear of the navigation bar and display cut-outs.
        ViewCompat.setOnApplyWindowInsetsListener(controls) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(dp(8) + bars.left, dp(8), dp(8) + bars.right, dp(12) + bars.bottom)
            insets
        }

        return FrameLayout(this).apply {
            addView(previewView)
            addView(
                controls,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )
        }
    }

    private fun startCamera() {
        retryButton.visibility = View.GONE
        statusView.text = QrStrings.text(QrStrings.Msg.STARTING, lang)
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            if (isFinishing || isDestroyed) return@addListener
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                // Rear camera by default; fall back to any camera so a tablet without one still works.
                val selector = when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> {
                        showError("no_camera")
                        return@addListener
                    }
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Replaces the deprecated setTargetResolution(Size(1280, 720)): ask for 1280x720,
                // and let CameraX pick the closest supported size if the device lacks it.
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy -> analyzeFrame(imageProxy) }
                analysis = imageAnalysis

                provider.unbindAll()
                val camera = provider.bindToLifecycle(this, selector, preview, imageAnalysis)
                activeCamera = camera

                val hasFlash = camera.cameraInfo.hasFlashUnit()
                torchLive?.removeObservers(this)
                torchButton.isEnabled = hasFlash
                if (hasFlash) {
                    val live = camera.cameraInfo.torchState
                    torchLive = live
                    live.observe(this) { state -> updateTorchLabel(state == TorchState.ON) }
                } else {
                    torchButton.text = QrStrings.text(QrStrings.Msg.NO_FLASH, lang)
                }

                statusView.text = QrStrings.text(QrStrings.Msg.HOLD_STEADY, lang)
                mainHandler.removeCallbacks(slowHint)
                mainHandler.postDelayed(slowHint, SLOW_HINT_DELAY_MS)
            } catch (e: Exception) {
                // Log the failure type only; never log scanned content.
                Log.w(TAG, "Camera start failed: " + e.javaClass.simpleName)
                showError("camera_start")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun restartScanning() {
        if (resultSent.get()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showError(code: String) {
        val msg = QrStrings.errorMsg(code) ?: QrStrings.Msg.ERR_CAMERA_START
        statusView.text = QrStrings.text(msg, lang)
        retryButton.visibility = View.VISIBLE
        torchButton.isEnabled = false
        mainHandler.removeCallbacks(slowHint)
    }

    private fun finishWithError(code: String) {
        if (resultSent.compareAndSet(false, true)) {
            setResult(Activity.RESULT_CANCELED, Intent().putExtra(EXTRA_QR_ERROR_CODE, code))
        }
        finish()
    }

    private fun focusAt(x: Float, y: Float) {
        val camera = activeCamera ?: return
        try {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            camera.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
        } catch (_: Exception) {
            // Tap-to-focus is optional; continuous autofocus keeps running.
        }
    }

    /**
     * Every path closes the ImageProxy: immediately when the frame is skipped or fails before the
     * ML Kit task starts, otherwise in the task's completion listener.
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeFrame(imageProxy: ImageProxy) {
        var handedToTask = false
        try {
            if (resultSent.get()) return
            val mediaImage = imageProxy.image ?: return
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val ordered = barcodes.sortedWith(
                        compareBy<Barcode>({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 })
                    )
                    val payloads = QrSampling.cleanPayloads(ordered.map { it.rawValue })
                    if (payloads.isNotEmpty() && resultSent.compareAndSet(false, true)) {
                        runOnUiThread {
                            statusView.text = QrStrings.text(QrStrings.Msg.FOUND, lang)
                            setResult(
                                Activity.RESULT_OK,
                                Intent()
                                    .putExtra(EXTRA_QR_TEXT, payloads.first())
                                    .putStringArrayListExtra(EXTRA_QR_ALL, ArrayList(payloads))
                            )
                            finish()
                        }
                    }
                }
                .addOnFailureListener {
                    // Keep scanning. A single blurry frame should not stop the scanner.
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
            handedToTask = true
        } catch (_: Exception) {
            // Skip this frame; the next one is analysed.
        } finally {
            if (!handedToTask) imageProxy.close()
        }
    }

    private fun toggleTorch() {
        val camera = activeCamera ?: return
        if (!camera.cameraInfo.hasFlashUnit()) return
        val turnOn = camera.cameraInfo.torchState.value != TorchState.ON
        camera.cameraControl.enableTorch(turnOn)
    }

    private fun updateTorchLabel(on: Boolean) {
        torchButton.text = QrStrings.text(
            if (on) QrStrings.Msg.FLASH_ON else QrStrings.Msg.FLASH_OFF,
            lang
        )
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        analysis?.clearAnalyzer()
        torchLive?.removeObservers(this)
        cameraProvider?.unbindAll()
        if (::scanner.isInitialized) scanner.close()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ScamShieldQr"
        private const val SLOW_HINT_DELAY_MS = 12_000L

        const val EXTRA_LANG = "rs.scamshield.app.QR_LANG"
        const val EXTRA_QR_TEXT = "rs.scamshield.app.QR_TEXT"
        const val EXTRA_QR_ALL = "rs.scamshield.app.QR_ALL"
        const val EXTRA_QR_ERROR_CODE = "rs.scamshield.app.QR_ERROR_CODE"
    }
}
