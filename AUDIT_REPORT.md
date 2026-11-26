# ScamShield Android — code audit

Audit scope: the Kotlin WebView Android project in `ScamShieldApk/` on branch `scamshield-apk-ci`. This is a source-level review. A prior CI build succeeded, but the previous APK was not physically tested on the user's phone.

## Defects found and changes made

### 1. QR camera depended on WebView camera support
**Finding:** live scanning used `navigator.mediaDevices.getUserMedia()` inside WebView, with Android `PermissionRequest` bridging. This is more fragile across WebView/device implementations than a native camera screen and had a custom polling/timeout loop.

**Change:** added `QrScannerActivity` using CameraX preview and ML Kit QR-only frame decoding. Camera permission is requested by the native activity. The scanner supports flashlight where the device has one, stops when the activity closes, and returns the decoded text to the existing checker. It does not open the QR destination.

### 2. Gallery QR decoding relied on `createImageBitmap`
**Finding:** the JavaScript image path called `createImageBitmap(file)` and returned null on any exception. Some Android WebView/image combinations do not support that path reliably.

**Change:** Android gallery selection now uses `ACTION_OPEN_DOCUMENT` and the bundled ML Kit model to decode the selected content URI locally. A browser fallback remains and now tries `Image` + `canvas` if `createImageBitmap` fails.

### 3. External links used new-window targets while WebView multiple windows were disabled
**Finding:** the HTML used `target="_blank"` while the wrapper had `setSupportMultipleWindows(false)` and no `onCreateWindow` handler. That combination can make links appear clickable but fail to open consistently.

**Change:** removed the new-window targets. Same-window external navigation reaches the WebView client, which launches the external Android browser or relevant app.

### 4. SHA-256 page implied a demo reputation comparison
**Finding:** the old code contained a placeholder all-zero hash in a DEMO list. That was not a real malware hash feed and could produce a misleading security message.

**Change:** the page now computes only the SHA-256 fingerprint locally, offers copying it, and clearly says no reputation lookup is connected. It does not claim the file is safe.

### 5. Antivirus/VPN section overstated actual capabilities
**Finding:** the old page showed recommendations (including a VPN product) but did not scan installed apps, scan the whole device, or establish a VPN tunnel.

**Change:** renamed it to “Device security”. It contains practical instructions, a Play Protect help link, and a working Android app-settings shortcut. It explicitly says it is not an antivirus scanner and does not run a VPN.

### 6. Static app had no action-wiring check
**Finding:** a large single-page interface uses delegated `data-a` click handlers. A button could be rendered without a corresponding handler and the Gradle build would not detect that.

**Change:** added `scripts/check-web.js` to check inline JavaScript syntax, required action branches, native QR bridge hooks, and external-link target policy. CI runs it on the bundled HTML.

## Remaining limitations (not bugs that can be honestly fixed with UI-only code)

- **No backend:** reports, evidence, and community notes are local to one device. Other people cannot see them and they do not increase a shared report count.
- **No real reputation service:** phone, email, message, and URL checks use local heuristics and visibly labelled DEMO examples. They are not a live anti-phishing or malware database.
- **No geographic dataset:** the scam map intentionally does not invent city statistics or pins.
- **No antivirus engine:** the app does not inspect installed apps or scan the Android filesystem.
- **No VPN:** no `VpnService`, tunnel provider, server infrastructure, or VPN connection exists.
- **Password checker is a simple local heuristic:** it is not a breach check, password audit service, or formal entropy estimator.
- **Local storage is not an encrypted vault:** app data stays on-device under Android app storage, but the local HTML state is not end-to-end encrypted. Avoid entering card numbers, PINs, passwords, or unnecessary personal data in notes.
- **CERT/CIRT integration is read-only and parser-dependent:** feeds and the Serbia HTML listing can change. Their current availability and completeness must be rechecked.
- **No physical-device test yet:** build success only confirms compilation and packaging. Camera behavior, file-provider interactions, rendering, and system settings must be checked on actual devices.

## Follow-up priorities

1. Physical-device QA of native camera and gallery QR paths.
2. Add automated Android instrumentation tests for scanner start/stop, permission denial, scanner result, and image decode.
3. Verify official advisory endpoints and parsing against their current publisher pages.
4. Improve URL normalization, IDN display, redirect warnings, and false-positive controls without auto-opening links.
5. Define a privacy and moderation design before creating a shared reporting backend.
6. Only then consider anonymized aggregate map data and opt-in notifications.


## Follow-up build investigation — 2026-11-19

The latest CI run before this update was build #13 and failed during Kotlin compilation (not during Gradle setup, Java setup, SDK installation, or the web asset checks). The compiler reported:

- `MainActivity.kt` and `QrScannerActivity.kt` imported `Barcode` from `com.google.mlkit.vision.barcode`. The model's `Barcode` type is in `com.google.mlkit.vision.barcode.common`.
- `QrScannerActivity.onRequestPermissionsResult` used a Kotlin array type that did not match the Android callback signature.

Fixes have been committed on `scamshield-apk-ci`:

- Corrected the ML Kit `Barcode` import in both Kotlin files.
- Replaced the legacy camera permission callback with `ActivityResultContracts.RequestPermission`.
- Camera denial now returns to the QR page with an explicit message, where the user can still choose an image.
- Added exception handling around per-frame ML Kit input creation/processing so a failed camera frame is closed rather than left outstanding.
- Expanded the web smoke check so it derives actions from rendered `data-a` attributes and requires a matching delegated handler. It also checks that the Android asset uses local `jsQR.js`, not a remote CDN URL.

A green CI build after these commits is still required. No source-level audit can verify autofocus, camera hardware, Android document-provider behavior, HEIC decoding, or device-specific WebView rendering. The user must test the resulting APK on a physical phone before these are marked as working.

## Functional status by area

- **QR camera:** native CameraX preview and bundled on-device ML Kit QR decoding are implemented. Permission allow/deny and repeat-scan behavior require physical-device QA.
- **QR image:** Android document picker and ML Kit local image decoding are implemented. JPEG/PNG/HEIC, rotated photos, large screenshots and provider-specific URIs require device QA.
- **URL/number/message checks:** local heuristics and labelled DEMO examples only. There is no live reputation service and a clean result is not proof of safety.
- **Official notices:** native read-only CERT/CIRT fetching with a local cache. Endpoint availability, parser accuracy and date handling need periodic validation.
- **Reports/community:** private notes stored locally on one phone. There is no public feed, cross-device sync, verified independent report count or moderation backend.
- **Map:** intentionally empty until a verified, privacy-preserving city dataset exists.
- **Device security:** educational guidance and Android settings shortcut only. No antivirus engine, installed-app scan, call blocking or VPN is present.
- **Password check and SHA-256:** local-only utilities. Neither is a breach lookup or malware-reputation verdict.

## Next implementation priorities

1. Complete the post-fix CI build and inspect the produced APK and Claude source ZIP checksums.
2. Run the QR test matrix in `TEST_CHECKLIST.md` on the target Xiaomi/Redmi phone and at least one emulator.
3. Add Android instrumentation tests for camera permission, successful scan, cancellation, analyzer lifecycle and gallery decoding.
4. Add robust URL normalization and clear handling of internationalized domains, user-info fields, redirects, shortened links and non-HTTP QR payloads. Never auto-open scanned content.
5. Validate CERT/CIRT feeds against official source pages; make stale/cache/unavailable states explicit.
6. Design an opt-in backend only if cross-device community is needed. It needs abuse controls, reporting thresholds, consent, retention limits and moderation before it can power public reports or a map.
7. Keep VPN, antivirus and call-blocking labels out unless real supported platform services and infrastructure are implemented.

---

# STEP 1/5 — Audit and QR camera/gallery rework

Scope of this step: Phase 1 (audit of source and architecture) and Phase 2 (QR scanning, QR content analysis, the two deprecation warnings).
**Not verified in this step:** nothing was compiled with Gradle, no APK was built, nothing was run on a phone. See "What was and was not verified".

## A. How the app is put together (as found)

| Piece | File | Role |
|---|---|---|
| UI | `web/scamshield-app.html` (source of truth) → copied to `app/src/main/assets/index.html` by CI, CDN `jsQR` URL rewritten to the bundled `jsQR.js` | One HTML page, delegated `data-a` click handlers, state in `localStorage` key `ss` |
| Shell | `MainActivity.kt` | WebView on `https://appassets.androidplatform.net/assets/`; all other requests get an empty response; `ScamShieldNative` JavaScript bridge |
| Camera QR | `QrScannerActivity.kt` | CameraX preview + ImageAnalysis → ML Kit (bundled model, QR only) |
| Gallery QR | `QrImageDecoder.kt` (new) | ImageDecoder/BitmapFactory → ML Kit |
| Advisories | `LiveAdvisoryService.kt` | Native fetch of public CERT/CIRT feeds (reviewed in step 3) |

Data flow for a QR scan: page → `ScamShieldNative.launchQrScanner()` / `pickQrImage()` → native decode on device → `window.onNativeQrScanResult / onNativeQrImageResult({ok,text,all,code,error})` → `qrParse()` classifies the text → `lkRender()` shows it. **The decoded text is never opened, dialled, sent or joined.** The only network-capable action is the explicit "Open in browser" button, which goes through a native confirmation dialog (`confirmAndOpen`).
Nothing in the QR path uses the network. (`INTERNET` permission exists only for the advisory fetcher.)

## B. Previously reported problems — re-inspected in the actual source

| # | Reported problem | Result of inspection | Status |
|---|---|---|---|
| 1 | Wrong ML Kit `Barcode` import | All Kotlin files import `...barcode.common.Barcode`. | Still fixed. Regression check added (`scripts/check-kotlin.js`). Compile not verified here. |
| 2 | Permission callback type mismatch | `QrScannerActivity` uses `ActivityResultContracts.RequestPermission()`. | Still fixed + regression check. |
| 3 | Stale `requestPermissions()` call | Not present; `cameraPermissionLauncher.launch(Manifest.permission.CAMERA)` is used. | Still fixed + regression check. |
| 4 | Web action checker incomplete / bad regex | Checker compared `data-a` vs handlers correctly. It did **not** verify that `ScamShieldNative.*` methods exist in Kotlin. | Extended (bridge-reference check, unsafe-pattern check). Proven with deliberately broken copies (4 negative tests all failed as intended). |
| 5 | `target="_blank"` in WebView | None present; checker enforces it. | Fixed, enforced. |
| 6 | Gallery relied on `createImageBitmap` | Android path was already native (`InputImage.fromFilePath`), but see N1 — it still had real defects. | Reworked (below). |
| 7 | Misleading antivirus/reputation features | Not reviewed in this step. | OPEN → step 3 (device security) and step 2 (checker wording). |
| W1 | Deprecated `onBackPressed()` | `MainActivity` now extends `ComponentActivity` and uses `OnBackPressedDispatcher`. | Fixed in code; compile not verified. |
| W2 | Deprecated `setTargetResolution()` | Replaced by `ResolutionSelector` + `ResolutionStrategy(Size(1280,720), CLOSEST_HIGHER_THEN_LOWER)`. Same requested size as before. | Fixed in code; compile not verified. |
| CI | Maven Central HTTP 429 | Not a code issue; new unit-test job notes it. | n/a |

## C. New problems found in this step, with cause and fix

**N1 — Gallery decode could freeze or crash on large photos.** Cause: `InputImage.fromFilePath()` was called on the main thread and loads the full-resolution bitmap (a 108 MP Xiaomi photo is ~430 MB as ARGB_8888). Fix: `QrImageDecoder` runs on a background executor, decodes down-sampled (long side ≤ 2048, then 1280, then 768; plus a padded copy with a white border for tightly cropped codes), recycles every bitmap, and maps `OutOfMemoryError` to a clear "image too large" message. API 28+ uses `ImageDecoder` (software allocator, needed for ML Kit; handles HEIC where the device supports it); API 24–27 uses `BitmapFactory` with manual EXIF rotation. QR detection itself is rotation-invariant; mirrored EXIF flips are not corrected.

**N2 — Only the first QR code was ever returned.** Cause: `firstNotNullOfOrNull`. Fix: all distinct codes (max 10, sorted top-left to bottom-right) are returned; the page shows "Several QR codes found" with a picker (`data-a=qrpick`).

**N3 — Errors were English-only in native code and vanished after 2.2 s.** Fix: native text is localised (Serbian Latin, Cyrillic, English) via `QrStrings`, language is sent by the page through `setLanguage()`; errors now render as a persistent card with "Try the camera again", "Choose image from gallery" and, for a denied permission, "Open ScamShield app settings".

**N4 — Browser fallback hid every error and failed on pictures with several codes.** Found by the new fixture test: `multi.png` returned nothing. Cause: `jsQR` can fail when finder patterns of two codes are in one frame, and `decodeFile` swallowed all exceptions. Fix: gallery images are tried whole and then as four overlapping halves (`qrTiles`); real load/decoder errors are shown.

**N5 — QR content types were too coarse.** Phone, SMS, email and geo were one "action" bucket, no Serbian IPS payment parsing. Fix: `qrParse()` distinguishes URL, Wi-Fi, phone, SMS, email, location, payment (Serbian NBS IPS, EPC, crypto/UPI URIs), 2FA setup data (`otpauth`) and text. Wi-Fi passwords are not copied into display fields. `javascript:` and `data:` content is plain text.

**N6 — No safe way to act on a QR link.** Fix: native "Open this link?" dialog showing the full address, http/https only, length ≤ 2048, never for tel/sms/mailto/geo/payment.

**N7 — No feedback when the picker was cancelled or while a large image was being read.** Fix: `cancelled` and `reading` states.

**N8 — Camera screen gaps.** Fix: explicit "no camera" path, fall back to the front camera if there is no rear one, **Try again** button after a start failure, 12-second help hint, torch label follows the real torch state, tap-to-focus, accessibility live region, analyzer cleared before the scanner is closed, `ImageProxy` closed on every path (`finally` + task completion).

**N9 — Bridge callable from any loaded page.** Defense in depth: `isTrustedPage()` check on the UI thread for scanner, picker, settings and open-link calls. (The WebView already refuses to load non-asset pages.)

**N10 — Results were injected as hand-built JS object text.** Now a JSON string passed through `JSON.parse(...)`; only three whitelisted callback names are accepted.

**N11 — A scan result was lost if the Activity had been recreated** (the checker screen no longer existed). The page now navigates to the Link & QR screen before showing the result.

**N12 — Test coverage was zero for QR logic.** Added: Node tests (`test-qr-analyze.js`: 20 cases; `test-qr-decode.js`: 11 image fixtures through the bundled jsQR), Kotlin JVM unit tests (`QrSamplingTest`, `QrStringsTest`), an instrumented test for the real decoder (`QrImageDecoderInstrumentedTest`).

## D. Found but not fixed in this step

- **Workflow Gradle version:** `build-apk.yml` in the ZIP pins Gradle **8.9**, while AGP **8.11.1** requires Gradle ≥ 8.13 and the task brief says 8.13. Run #25 reportedly succeeded, so the branch's workflow probably differs from the ZIP. Please check the branch file. The new `static-checks.yml` uses 8.13.
- `build-apk.yml` runs only on `main` push or manual dispatch and creates a GitHub Release on every run.
- `WebSettings.allowContentAccess = true` is probably unnecessary (step 2).
- `isMinifyEnabled = false`, `targetSdk = 34` while `compileSdk = 36` (step 2/3 review).
- The Toast text for "Could not open Android app settings" and the file-picker error is still English-only.
- Live camera in the browser fallback returns only the first code, and runs single-tile for speed.
- Cyrillic transliteration in native code relies on the message set being free of ambiguous letter pairs; a test guards the current set.

## E. What was and was not verified

Run here (real results): `check-web.js` on both HTML copies; 4 negative tests of the checker; `check-kotlin.js` + 2 negative tests; 20 `qrParse` tests; 11 jsQR fixture tests (fixtures also cross-checked with OpenCV's decoder, except the inverted one which OpenCV cannot read); Python re-implementation of the Cyrillic transliteration run over all 26 native messages (no Latin letters left).
**Not run, cannot be run in this environment:** Gradle/Kotlin compilation (no SDK or network), the Kotlin JVM unit tests, the instrumented test, ML Kit and CameraX behaviour, anything on a phone.

---

# STEP 2/5 — review findings, advisories, phone numbers, verdict colours, target API 36

See `SCAMSHIELD_ERRORS_AND_FIXES.md` for the complete table (what was verified, what changed, what is unproven). Summary of causes and fixes:

1. **Phone normalisation (web `target()`/`mask()`).** Cause: hard-coded `'381'`. Effect: Bosnian, Montenegrin, Macedonian and Croatian numbers got Serbian codes, so duplicate detection could merge or split reports and `mask()` left non-381 numbers readable in saved report text (privacy). Fix: `normPhone`/`maskPii` in a marked pure block, tested by `scripts/test-phone-norm.js` (9 cases).
2. **Green "not enough data".** Cause: one `.none` class used for "no data", "no obvious signs" and password strength, coloured `--ok`. Fix: neutral `--unk` grey for `.none/.unknown`; `.strong` green only for passwords. `check-web.js` now fails if `.none` uses `--ok`.
3. **Advisory dates, sorting, de-duplication (`AdvisoryParsing.kt`, `LiveAdvisoryService.kt`).** Causes: empty date passed for every Serbian item; sort on date strings; title-only identity. Also found: the `/rs/obavestenje/` link filter would not match the live Cyrillic page's `https://cert.rs/obavestenje/<id>-….html`. Fixes as in the table. I read the live cert.rs notice list and the home page through a text view to learn the real structure (date line, heading link, "Detaljnije" link; dates such as "14. Avgust 2026" in the page's script). I did NOT see the raw HTML, so `AdvisoryParsingTest` uses reconstructed samples and is only a structural guarantee.
4. **Target API 36.** Cause: `targetSdk = 34`. Fix: 36 plus edge-to-edge handling (insets for bars, cut-outs, keyboard), theme-aware bars through the new bridge call `setChrome`. Untested on a device.
5. **Workflows.** Gradle 8.13 in `build-apk.yml`; Node checks added; `static-checks.yml` also runs `test-phone-norm.js`. Persistent signing already existed in `build-apk.yml`; only the secrets are missing.
6. **Regions.** Main four countries, "More regions" toggle for Croatia.

Verified here (real runs): `check-web.js` (+3 negative tests), `check-kotlin.js` (+4 negative tests incl. Gradle 8.9, targetSdk 34), 20 + 9 Node unit tests, 11 jsQR fixtures, Python port of the CERT listing logic on the reconstructed sample. NOT verified: any Kotlin compilation, JVM unit tests, anything on a phone.
