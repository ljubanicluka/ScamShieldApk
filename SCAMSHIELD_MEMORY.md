# ScamShield Android — project memory and handover

## What this project is
This is the Kotlin Android app in `ScamShieldApk/`, separate from the older .NET MAUI branch of the repository. It hosts a local HTML/CSS/JavaScript interface in a restricted Android WebView. The UI is Serbian-first with English and Latin/Cyrillic options.

## Current work in this branch
- QR camera scanning has been moved out of WebView into a native Android `QrScannerActivity`.
- CameraX provides the preview and image frames; bundled Google ML Kit barcode scanning decodes QR codes on-device, including without a network connection.
- Gallery QR image selection uses Android's document picker and the same bundled ML Kit model. QR contents are passed back into the existing local checker. Nothing is uploaded.
- The HTML QR decoder remains as a fallback for non-Android browsers. It now falls back from `createImageBitmap` to an HTML image element.
- External anchors no longer use `target="_blank"`, because the Android WebView deliberately has multiple windows disabled. External links are opened by Android.
- The file hash page now computes a local SHA-256 fingerprint and explicitly says it is not a reputation verdict.
- The old “Antivirus & VPN” label has been replaced with a device-security guide. There is no VPN, antivirus engine, installed-app scan, or remote threat-intelligence lookup in this project.
- Added a static JavaScript/action-wiring check for CI.

## Important files
- `app/src/main/java/rs/scamshield/app/MainActivity.kt`: WebView host, native bridge, gallery picker, local QR image decode, official advisory bridge.
- `app/src/main/java/rs/scamshield/app/QrScannerActivity.kt`: native CameraX + ML Kit real-time QR scan.
- `app/src/main/java/rs/scamshield/app/LiveAdvisoryService.kt`: read-only official CERT/CIRT advisory fetcher and local cache.
- `web/scamshield-app.html`: complete UI and local-only tools.
- `app/src/main/AndroidManifest.xml`: permissions and activity declarations.
- `app/build.gradle.kts`: Android dependencies and SDK settings.
- `scripts/check-web.js`: syntax and click-action smoke check.
- `AUDIT_REPORT.md`: known defects, limitations, and next implementation priorities.
- `TEST_CHECKLIST.md`: automated and device test plan.

## Build
The CI workflow is `.github/workflows/build-scamshield-kotlin-apk.yml`. It uses Java 17, Android API 36, Android Gradle Plugin 8.11.1, Gradle 8.13, Kotlin 2.2.21, CameraX 1.6.2, Activity 1.13.0, and bundled ML Kit barcode scanning 17.3.0. The CI workflow builds and verifies the release APK.

Local build from the `ScamShieldApk` folder:
```bash
gradle assembleRelease
```
The CI workflow bundles `jsQR.js` into Android assets before building. Do not reintroduce an external CDN dependency into the Android runtime: the WebView blocks remote subresources.

## Project operating rules
1. Read `AUDIT_REPORT.md` and `TEST_CHECKLIST.md` before changing code.
2. Preserve the split between native Android QR functions and the local WebView UI.
3. Do not claim a test passed unless you actually ran it. Static syntax checks and a successful Gradle build are not substitutes for testing on a physical Android phone.
4. Do not add a cosmetic “VPN connected” toggle. A real VPN requires a separate Android `VpnService`, user consent, tunnel configuration, servers, security review, and an operating provider. This project intentionally has no VPN.
5. Do not label local heuristic checks as real-time threat intelligence. DEMO data must remain visibly labelled.
6. Do not invent community users, public reports, report counts, map locations, source availability, or ratings. Reports and community notes are currently local to one device.
7. Do not transmit scanned QR payloads, user-entered URLs, messages, passwords, evidence, or local reports. The native network service is for public CERT/CIRT advisories only.
8. Prioritize actual broken interactions, lifecycle handling, permissions, accessibility, error states, and reproducible tests over adding more menu tiles.
9. Keep external links opening in the external browser. Do not weaken WebView origin restrictions or allow arbitrary remote JavaScript.
10. Before another APK release, run the CI workflow and update this handoff with the real build number, checksum, and any failures.

## Suggested next work
- Test camera permission, rear camera, flashlight, cancellation, and repeated scans on multiple Android devices.
- Test gallery imports for JPEG, PNG, and HEIC images, including high-resolution photos and rotated images.
- Add instrumentation tests for QR activity lifecycle and gallery decoding.
- Improve the local URL analyzer with more robust URL normalization and clear confidence language; do not claim a clean result is proof of safety.
- Verify each CERT/CIRT feed endpoint and parser against its current official source.
- Consider a privacy-preserving backend only after defining consent, retention, moderation, abuse prevention, and data-protection requirements.
- Add an explicit local backup/export feature with a warning that evidence may contain personal data.


## Follow-up fixes (2026-11-19)

The latest CI failure before these fixes was Kotlin compilation. ML Kit's `Barcode` import was corrected in `MainActivity.kt` and `QrScannerActivity.kt`. The old permission callback was replaced with the ComponentActivity Activity Result API. A denied camera permission now returns an explicit error to the QR page rather than leaving the user on a scanner-only screen. Camera frame creation/processing is guarded so exceptional frames are closed.

The JavaScript smoke checker now discovers all static `data-a` actions and requires each to have a delegated handler. It also requires the bundled Android decoder reference to be `jsQR.js` and rejects an external jsQR CDN reference. The CI result following these commits must be checked before claiming the APK builds.

Do not tell the user that camera/image QR is physically verified based only on compilation. Test camera permission grant/denial, rear-camera preview, autofocus, flashlight, scan result, cancel/reopen, and gallery imports (JPEG, PNG, HEIC, rotated images, compressed screenshots and no-QR images) on a real device. The QR contents must remain local and must not be opened automatically.

## Current honest feature boundaries

The local checker is heuristic and uses clearly labelled DEMO examples; it is not connected to a phone-number, URL, email, malware or phishing reputation provider. The “community” is a local notes view, not a server-backed community. The map has no verified city dataset. Report counts cannot be called independent or shared. Family controls are consent-based checklists and message generation, not remote device management. Device security is a guide, not antivirus. There is no VPN implementation.

Before adding cloud features, design consent, minimization, abuse prevention, moderation, retention and deletion. Never invent fake reports, user accounts, map pins, source statuses or security verdicts.

---

# STEP 1/5 handoff

Done: audit of architecture; QR camera + gallery rework; QR content types; deprecations; tests and static checks; this documentation.
Source of truth for the UI is `web/scamshield-app.html`; `app/src/main/assets/index.html` is generated from it (CDN jsQR URL replaced by `jsQR.js`). **Edit the web/ file, then regenerate** (`cp` + `sed`, see `.github/workflows/static-checks.yml`).

New files: `QrImageDecoder.kt`, `QrSampling.kt`, `QrStrings.kt`; tests under `app/src/test` and `app/src/androidTest`; `scripts/check-kotlin.js`, `scripts/test-qr-analyze.js`, `scripts/test-qr-decode.js`, `scripts/lib/*`; `tests/make_qr_fixtures.py` + `tests/fixtures/qr/`; `.github/workflows/static-checks.yml`.

Bridge (`ScamShieldNative`): `getCachedAdvisories`, `refreshAdvisories`, `setLanguage(code)`, `launchQrScanner`, `pickQrImage`, `openExternalUrl(url)`, `openAppSettings`. Callbacks into the page: `onNativeQrScanResult`, `onNativeQrImageResult`, `onNativeQrState`, `onLiveAdvisories`. Error codes on the wire: `cancelled`, `camera_denied`, `no_camera`, `camera_start`, `empty`, `scanner_open`, `picker_open`, `no_qr`, `unreadable`, `too_large`, `blocked_page`.

Open for later steps: step 2 — every screen/button/validation, URL & message heuristics, wording of verdicts; step 3 — advisories, device security, history/backup; step 4 — community/report/map/backend design; step 5 — docs, final audit, CI run.
Do not mark QR scanning "fixed" until the Xiaomi checklist above passes. Compile status is unknown until CI builds this source.

---

# STEP 2/5 handoff

Done: review findings verified and fixed (see SCAMSHIELD_ERRORS_AND_FIXES.md), `AdvisoryParsing.kt` (pure; dates, URL identity, numeric sort), phone normalisation block `<phone-norm>`, verdict colours, regions UI, targetSdk 36 + edge-to-edge, Gradle 8.13, Node checks in `build-apk.yml`.
Bridge additions: `setChrome(background, dark)`. Web markers: `<qr-analyze>`, `<phone-norm>` (loaded by `scripts/lib/load-qr-analyze.js`).
Next (step 3/5): advisory sources other than cert.rs (needs saved feeds), advisory status honesty in the UI, device-security wording, scan/search history with deletion, evidence export, backup/restore, the DEMO-driven "Likely scam" logic, notification settings, accessibility options, `allowContentAccess`, minify.
Blocked on you: CI log of the first build with these changes; device test; signing secrets.


---

# Project state update — 2026-11-27

## Supabase backend
- Supabase project ref: `zrobvuscdgbisaqvliil`; URL: `https://zrobvuscdgbisaqvliil.supabase.co`.
- Branch: `supabase-backend-integration`; draft PR #3 remains unmerged and is stacked on PR #1. Do not merge either PR without the user's explicit approval.
- Initial schema migration: `supabase/migrations/202611270001_initial_schema.sql`.
- The first SQL version failed because the username trigger function was truncated and policy SQL was duplicated. The migration was repaired: one complete `handle_new_user()` function and trigger, a valid username fallback condition, and one RLS policy block.
- User ran the corrected migration in Supabase SQL Editor and reported: **“Success. No rows returned.”** Treat schema application as successful based on that user confirmation.
- Auth and app login have not yet been live-tested. Email provider/redirect configuration may still be needed.
- Authentication is implemented through the Supabase provider, but community feed/posts/comments/votes/reposts/reports still use local demo state. Cross-device community sync is NOT implemented.
- Client code must use only the publishable key. Never request, repeat, commit, or use Supabase secret/service-role keys. The user previously exposed a secret key; it should be revoked/rotated in Supabase. Never place secret keys in SQL Editor or app assets.

## Next technical steps
1. Configure Supabase email auth and redirect URLs.
2. Test account registration, confirmation email, sign-in, sign-out, password reset and session restore on web and Android.
3. Then implement remote community data access with correct RLS and controlled aggregate RPCs; do not claim cross-device sync before tests.
4. Run CI and device tests. Record exact results; do not infer physical device verification from compilation.

## Documentation policy
This file is the persistent project memory/handover for future ChatGPT work. Keep it current after meaningful code, backend, CI, security, or product-scope changes. Do not reintroduce tool-specific handoff or prompt files.
