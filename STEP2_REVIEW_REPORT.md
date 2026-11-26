# ScamShield — independent review of the Step 2 source

## Result

The supplied Step 2 archive was inspected and patched. The following additional defects were found beyond the changes already described in `SCAMSHIELD_ERRORS_AND_FIXES.md`:

1. **Numeric dates in the Serbian CERT listing were silently ignored.** `parseSerbianDate()` understood `14.08.2026.`, but `parseCertRsListing()` only collected month-name dates. The parser now collects both formats and associates the nearest preceding date with each notice.
2. **The CERT listing parser accepted matching notice URLs from arbitrary HTTPS hosts.** It now accepts only `cert.rs` and `www.cert.rs` notice links.
3. **DEMO report counts were fabricated and could produce a “Likely scam” result.** The counts have been removed. A matching DEMO sample is now clearly labelled as an example and does not affect the verdict. The local heuristic can say “Suspicious”, “Be careful”, or “Not enough data”; it cannot claim “Likely scam”. That result must wait for genuine, deduplicated independent reports from a backend.
4. **Six-digit numeric tokens could be treated as phone numbers.** Phone normalization now requires 7–15 digits.
5. **Every APK workflow run published a GitHub Release.** Releases are now published only when the workflow is manually dispatched. Push builds still upload an Actions artifact.
6. **The APK workflow did not run JVM unit tests.** It now runs `testDebugUnitTest` before `assembleRelease`.
7. **Keyboard inset handling depended on implicit window behaviour.** MainActivity now declares `adjustResize`. The WebView's `allowContentAccess=true` is retained and documented because the explicit file input used by the local SHA-256 utility needs access to the user-selected content URI; `allowFileAccess` remains disabled.

## Verification performed

| Check | Result |
|---|---|
| `scripts/check-web.js` | PASS |
| `scripts/check-kotlin.js` | PASS — static regression checks only, not Kotlin Android compilation |
| `scripts/test-phone-norm.js` | PASS — 9 tests |
| `scripts/test-qr-analyze.js` | PASS — 20 tests |
| `scripts/test-qr-decode.js` | PASS — 11 image fixtures |
| `scripts/test-verdict.js` | PASS — DEMO data cannot create a verdict or fake report count |
| AndroidManifest XML parse | PASS |
| GitHub Actions workflow YAML parse | PASS — both workflow files |
| Pure Kotlin JVM tests | PASS — 25 methods across `AdvisoryParsingTest`, `QrSamplingTest`, and `QrStringsTest` compiled and passed using the installed Kotlin compiler and a small local JUnit-compatible assertion harness |

The pure Kotlin test result is useful source-level verification, but it is not a substitute for the Android Gradle test task. The Kotlin Android activities, Android resources, CameraX integration and APK packaging have not been compiled in this environment because Gradle and the Android SDK are not installed here.

## Still needs your action

1. **Run GitHub Actions.** Run `Build ScamShield APK` and `Static checks and unit tests`. Send the complete failing log if either workflow fails. The first Android Gradle build remains the compile gate for Kotlin/Android APIs.
2. **Test on the Xiaomi/Redmi.** Follow `TEST_CHECKLIST.md`: camera permission allow/deny, rear camera start, flashlight, retry/cancel/reopen, multi-QR frame, gallery JPEG/PNG/HEIC, rotated image, large image, no-QR image, keyboard layout and Android 15/16 edge-to-edge. Report results and screenshots for any failures.
3. **Save the real CERT page.** The parser tests currently use reconstructed HTML, not a byte-for-byte saved page. Save the live Serbian CERT notices page as HTML and provide it. Also provide saved feed/page samples for CERT RS (Republika Srpska), Montenegro Government CIRT and MKD-CIRT so their endpoints and parsers can be checked against actual responses.
4. **Set permanent signing secrets if you want install-over updates.** Generate and securely back up one keystore; add all four `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` secrets. Do not commit the keystore. The first APK signed with the new key requires one uninstall of the old differently signed test APK.
5. **Use a dedicated repository for this Kotlin project, or deliberately restructure the existing repository.** The current `ljubanicluka/ScamShield-Android` repository root describes a separate .NET MAUI project; this Kotlin project is nested under `ScamShieldApk/`. GitHub only discovers workflows in the repository-root `.github/workflows/` directory, not `ScamShieldApk/.github/workflows/`. The standalone ZIP has its own root-level workflow when extracted as a project. Do not just push this folder into the existing repository and expect the nested workflow to run.

## Product limitations — intentionally not misrepresented

- Phone, email and URL checks remain local heuristics plus DEMO examples; there is no live reputation provider.
- Community reports remain on one phone. No server, cross-device sync, user accounts, moderation or server-side deduplication is connected.
- The map has no verified city-level dataset and therefore displays no invented pins or statistics.
- Notifications do not yet have a real verified alert source.
- The device-security page is guidance, not antivirus. Family features are checklists/message generation, not remote parental controls or call blocking.
- VPN is intentionally absent.
- Serbia's parser still needs the saved live HTML sample; the other three feeds need fresh endpoint verification. The app should continue to show source availability and cached-data status rather than pretending a failed feed is live.

## Decisions applied in this package

- Croatia remains under **More regions**.
- Ordinary push builds create an Actions artifact but do not publish a GitHub Release. A manual `workflow_dispatch` run publishes a Release.
- No changes were pushed to the existing GitHub repository because its root currently targets the separate MAUI project and the Kotlin workflow is nested/inactive there. This archive is the reviewed, patched standalone Kotlin project.
