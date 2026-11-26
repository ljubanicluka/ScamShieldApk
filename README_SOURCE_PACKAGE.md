# Source package contents

This is the `ScamShieldApk/` project directory from branch `scamshield-apk-ci` at commit `80087c266f85f971f306de3384aed4835d519e0d`.

It contains the Kotlin Android application, WebView UI source, bundled web assets, native QR scanner, Gradle configuration, CI smoke checker, audit, Claude handoff, hardening prompt, and device test checklist.

Important files:

- `app/src/main/java/rs/scamshield/app/MainActivity.kt`
- `app/src/main/java/rs/scamshield/app/QrScannerActivity.kt`
- `app/src/main/java/rs/scamshield/app/LiveAdvisoryService.kt`
- `web/scamshield-app.html`
- `app/src/main/assets/index.html`
- `app/src/main/assets/jsQR.js`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `scripts/check-web.js`
- `AUDIT_REPORT.md`
- `TEST_CHECKLIST.md`
- `CLAUDE_HANDOFF.md`
- `CLAUDE_PROMPT.md`
- `CI_RUN_25_RESULT.md`

The root repository workflow that publishes the APK is outside this project folder at `.github/workflows/build-scamshield-kotlin-apk.yml`.
