# ScamShield Android CI — Run #25

- Repository: `ljubanicluka/ScamShield-Android`
- Branch: `scamshield-apk-ci`
- Commit: `80087c266f85f971f306de3384aed4835d519e0d`
- Workflow run: https://github.com/ljubanicluka/ScamShield-Android/actions/runs/44611167687
- Result: **success**
- Build date: 2026-11-19

## Build result

Gradle `assembleRelease` completed successfully: `BUILD SUCCESSFUL in 2m 20s`.
The Kotlin compile task passed. There were no Kotlin compiler errors in this run.

The build emitted two Kotlin deprecation warnings:

- `MainActivity.kt`: `onBackPressed()` is deprecated.
- `QrScannerActivity.kt`: `ImageAnalysis.Builder.setTargetResolution()` is deprecated.

These warnings did not fail the build.

The Android web UI static check also passed. It checked that each rendered `data-a` action has a handler, that the QR decoder is bundled locally as `jsQR.js`, that native QR bridge hooks exist, and that the WebView does not rely on `_blank` windows.

## APK validation

- APK archive integrity: passed (`unzip -t` reported no errors).
- APK signature: verified using Android APK Signature Scheme v2.
- APK SHA-256: `3ddcd29952e865f280dce43258cf37e58720c77ecec0c66a91e5c9c3f35a2151`
- APK size: 27,648,356 bytes.

This was a CI build and signature verification only. It does **not** establish that camera scanning, gallery decoding, or any other feature has been physically tested on a phone. Those checks remain in `TEST_CHECKLIST.md`.
