package rs.scamshield.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hosts the bundled ScamShield web UI.
 *
 * QR scanning is deliberately native instead of relying on WebView getUserMedia:
 * - Camera scanning runs in QrScannerActivity using CameraX + the bundled ML Kit model.
 * - Gallery images are opened with Android's system document picker and decoded locally
 *   (QrImageDecoder). The image is never uploaded.
 * - QR content is passed back to the page as data; it is never opened, dialled, sent or joined.
 *
 * External links are opened by Android, not loaded inside the restricted WebView, and only
 * after the user confirms in a native dialog.
 */
class MainActivity : ComponentActivity() {
    private val assetHost = "appassets.androidplatform.net"
    private val assetPrefix = "https://appassets.androidplatform.net/assets/"
    private lateinit var web: WebView
    private lateinit var rootView: FrameLayout
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var advisoryService: LiveAdvisoryService
    private val feedRefreshRunning = AtomicBoolean(false)
    private lateinit var imageExecutor: ExecutorService

    /** Language chosen in the web page ("en", "sr-lat", "sr-cyr"), used for native text. */
    @Volatile
    private var uiLang: String = QrStrings.LANG_SR_LAT

    private val webFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val parsed = if (result.resultCode == RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else {
            null
        }
        fileCallback?.onReceiveValue(parsed)
        fileCallback = null
    }

    private val qrCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> handleCameraResult(result.resultCode, result.data) }

    private val qrImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            // The user closed the picker. Tell the page so it can clear any loading state.
            sendQrResult(CB_IMAGE, null, "cancelled")
        } else {
            decodeQrImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Target API 35+ draws edge-to-edge. Opt in on every API level so behaviour is the same,
        // then pad the page by the system-bar and keyboard insets below.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        imageExecutor = Executors.newSingleThreadExecutor()
        web = WebView(this)
        advisoryService = LiveAdvisoryService(this)
        rootView = FrameLayout(this).apply {
            setBackgroundColor(defaultChromeColor())
            addView(
                web,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }
        setContentView(rootView)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            WindowInsetsCompat.CONSUMED
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::web.isInitialized && web.canGoBack()) {
                    web.goBack()
                } else {
                    // Let the system handle Back (finish the activity) from here on.
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        // Required for the explicit user-selected HTML file input used by the local SHA-256 tool.
        // File-scheme access remains disabled; the QR gallery picker is handled natively.
        web.settings.allowContentAccess = true
        web.settings.mediaPlaybackRequiresUserGesture = true
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.setSupportMultipleWindows(false)
        web.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            web.settings.safeBrowsingEnabled = true
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val uri = request.url
                return if (uri.scheme == "https" &&
                    uri.host.equals(assetHost, ignoreCase = true) &&
                    uri.path?.startsWith("/assets/") == true
                ) {
                    loader.shouldInterceptRequest(uri)
                } else {
                    // All UI assets are bundled. Public advisories are fetched by the native service.
                    WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        java.io.ByteArrayInputStream(ByteArray(0))
                    )
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase() ?: return true
                if (scheme == "https" &&
                    uri.host.equals(assetHost, ignoreCase = true) &&
                    uri.path?.startsWith("/assets/") == true
                ) return false

                if (scheme !in setOf("https", "http", "tel", "mailto")) return true
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        QrStrings.text(QrStrings.Msg.OPEN_FAILED, uiLang),
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
            }
        }

        web.addJavascriptInterface(object {
            @JavascriptInterface
            fun getCachedAdvisories(): String = advisoryService.cachedSnapshot().toString()

            @JavascriptInterface
            fun refreshAdvisories() {
                if (!feedRefreshRunning.compareAndSet(false, true)) return
                Thread {
                    val result = try {
                        advisoryService.refresh().toString()
                    } catch (_: Exception) {
                        advisoryService.cachedSnapshot().toString()
                    }
                    runOnUiThread {
                        feedRefreshRunning.set(false)
                        if (::web.isInitialized && !isFinishing && !isDestroyed) {
                            val quoted = JSONObject.quote(result)
                            web.evaluateJavascript(
                                "window.onLiveAdvisories && window.onLiveAdvisories(JSON.parse($quoted));",
                                null
                            )
                        }
                    }
                }.start()
            }

            /** The page tells native code which language to use for native screens and dialogs. */
            @JavascriptInterface
            fun setLanguage(code: String?) {
                uiLang = QrStrings.normalizeLang(code)
            }

            /**
             * The page reports its background colour and whether it is dark, so the area behind the
             * system bars matches the theme and the status-bar icons stay readable.
             */
            @JavascriptInterface
            fun setChrome(background: String?, dark: Boolean) {
                runOnUiThread {
                    if (!isTrustedPage()) return@runOnUiThread
                    if (background == null || !Regex("^#[0-9a-fA-F]{6}$").matches(background)) return@runOnUiThread
                    rootView.setBackgroundColor(Color.parseColor(background))
                    val controller = WindowCompat.getInsetsController(window, rootView)
                    controller.isAppearanceLightStatusBars = !dark
                    controller.isAppearanceLightNavigationBars = !dark
                }
            }

            @JavascriptInterface
            fun launchQrScanner() {
                runOnUiThread {
                    if (!isTrustedPage()) {
                        sendQrResult(CB_SCAN, null, "blocked_page")
                        return@runOnUiThread
                    }
                    try {
                        qrCameraLauncher.launch(
                            Intent(this@MainActivity, QrScannerActivity::class.java)
                                .putExtra(QrScannerActivity.EXTRA_LANG, uiLang)
                        )
                    } catch (_: Exception) {
                        sendQrResult(CB_SCAN, null, "scanner_open")
                    }
                }
            }

            @JavascriptInterface
            fun pickQrImage() {
                runOnUiThread {
                    if (!isTrustedPage()) {
                        sendQrResult(CB_IMAGE, null, "blocked_page")
                        return@runOnUiThread
                    }
                    try {
                        qrImageLauncher.launch(arrayOf("image/*"))
                    } catch (_: Exception) {
                        sendQrResult(CB_IMAGE, null, "picker_open")
                    }
                }
            }

            /**
             * Opens a web link only after the user confirms in a native dialog that shows the full
             * address. Only http(s) is accepted; tel:, sms:, mailto:, geo: and payment links are never
             * launched from QR content.
             */
            @JavascriptInterface
            fun openExternalUrl(url: String?) {
                runOnUiThread {
                    if (!isTrustedPage()) return@runOnUiThread
                    confirmAndOpen(url)
                }
            }

            @JavascriptInterface
            fun openAppSettings() {
                runOnUiThread {
                    if (!isTrustedPage()) return@runOnUiThread
                    try {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + packageName)
                        )
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "Could not open Android app settings.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }, "ScamShieldNative")

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    webFileLauncher.launch(params.createIntent())
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    callback.onReceiveValue(null)
                    Toast.makeText(
                        this@MainActivity,
                        "Could not open the file picker.",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
            }
        }

        web.loadUrl(assetPrefix + "index.html")
    }

    private fun defaultChromeColor(): Int {
        val night = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        return Color.parseColor(if (night) "#0f1420" else "#f4f6fb")
    }

    /** Bridge calls are honoured only while the bundled page is showing. Call on the UI thread. */
    private fun isTrustedPage(): Boolean {
        if (!::web.isInitialized) return false
        return web.url?.startsWith(assetPrefix) == true
    }

    private fun confirmAndOpen(url: String?) {
        if (isFinishing || isDestroyed) return
        val text = url?.trim().orEmpty()
        val uri = try {
            Uri.parse(text)
        } catch (_: Exception) {
            null
        }
        val scheme = uri?.scheme?.lowercase()
        if (uri == null || text.length > MAX_OPEN_URL_CHARS ||
            (scheme != "https" && scheme != "http") || uri.host.isNullOrBlank()
        ) {
            Toast.makeText(this, QrStrings.text(QrStrings.Msg.OPEN_FAILED, uiLang), Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(QrStrings.text(QrStrings.Msg.OPEN_TITLE, uiLang))
            .setMessage(QrStrings.text(QrStrings.Msg.OPEN_BODY, uiLang, text))
            .setPositiveButton(QrStrings.text(QrStrings.Msg.OPEN_YES, uiLang)) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        QrStrings.text(QrStrings.Msg.OPEN_FAILED, uiLang),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(QrStrings.text(QrStrings.Msg.OPEN_NO, uiLang), null)
            .show()
    }

    private fun handleCameraResult(resultCode: Int, data: Intent?) {
        val all = data?.getStringArrayListExtra(QrScannerActivity.EXTRA_QR_ALL)
        val first = data?.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT)
        val errorCode = data?.getStringExtra(QrScannerActivity.EXTRA_QR_ERROR_CODE)
        when {
            !first.isNullOrBlank() -> {
                val list = QrSampling.cleanPayloads(listOf(first) + (all ?: emptyList()))
                sendQrResult(CB_SCAN, list, null)
            }
            !errorCode.isNullOrBlank() -> sendQrResult(CB_SCAN, null, errorCode)
            resultCode == RESULT_OK -> sendQrResult(CB_SCAN, null, "empty")
            // A normal Cancel or Back press returns RESULT_CANCELED with no extras.
            else -> sendQrResult(CB_SCAN, null, "cancelled")
        }
    }

    private fun decodeQrImage(uri: Uri) {
        sendQrState("reading")
        imageExecutor.execute {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner: BarcodeScanner = BarcodeScanning.getClient(options)
            val result: QrImageResult = try {
                QrImageDecoder(applicationContext, scanner).decode(uri)
            } catch (_: OutOfMemoryError) {
                QrImageResult.TooLarge
            } catch (_: Exception) {
                QrImageResult.Unreadable
            } finally {
                scanner.close()
            }
            when (result) {
                is QrImageResult.Found -> sendQrResult(CB_IMAGE, result.payloads, null)
                QrImageResult.NoQr -> sendQrResult(CB_IMAGE, null, "no_qr")
                QrImageResult.TooLarge -> sendQrResult(CB_IMAGE, null, "too_large")
                QrImageResult.Unreadable -> sendQrResult(CB_IMAGE, null, "unreadable")
            }
        }
    }

    /**
     * Sends either payloads or an error code to the page. The page receives a parsed object
     * ({ok, text, all} or {ok:false, code, error}); the decoded text is never evaluated as code.
     */
    private fun sendQrResult(callback: String, payloads: List<String>?, errorCode: String?) {
        val json = JSONObject()
        if (!payloads.isNullOrEmpty()) {
            json.put("ok", true)
            json.put("text", payloads.first())
            json.put("all", JSONArray(payloads))
        } else {
            val code = errorCode ?: "no_qr"
            json.put("ok", false)
            json.put("code", code)
            val msg = QrStrings.errorMsg(code)
            if (msg != null) json.put("error", QrStrings.text(msg, uiLang))
        }
        deliverToPage(callback, json)
    }

    private fun sendQrState(state: String) {
        deliverToPage(CB_STATE, JSONObject().put("state", state))
    }

    private fun deliverToPage(callback: String, json: JSONObject) {
        if (callback != CB_SCAN && callback != CB_IMAGE && callback != CB_STATE) return
        val quoted = JSONObject.quote(json.toString())
        runOnUiThread {
            if (::web.isInitialized && !isFinishing && !isDestroyed) {
                web.evaluateJavascript(
                    "window.$callback && window.$callback(JSON.parse($quoted));",
                    null
                )
            }
        }
    }

    override fun onPause() {
        if (::web.isInitialized) web.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.onResume()
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        if (::imageExecutor.isInitialized) imageExecutor.shutdown()
        if (::web.isInitialized) {
            web.stopLoading()
            web.webChromeClient = null
            web.removeAllViews()
            web.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val CB_SCAN = "onNativeQrScanResult"
        private const val CB_IMAGE = "onNativeQrImageResult"
        private const val CB_STATE = "onNativeQrState"
        private const val MAX_OPEN_URL_CHARS = 2048
    }
}
