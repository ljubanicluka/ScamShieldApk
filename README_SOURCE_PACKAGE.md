# Source package notes

This repository contains the ScamShield Kotlin Android application, WebView interface, bundled assets, native QR scanner, Gradle configuration, automated checks, audit notes, project memory and device test checklist.

The canonical editable UI is `web/scamshield-app.html`. The Android WebView copy is `app/src/main/assets/index.html` and is synchronized by CI.

Build the APK with the **Build ScamShield APK** workflow. Download the APK from the workflow artifact or release created by a manually dispatched run. Do not commit signing keys or passwords.

See `SCAMSHIELD_MEMORY.md` for current project architecture, verified status, constraints and next steps.
