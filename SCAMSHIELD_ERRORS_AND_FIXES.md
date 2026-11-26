# ScamShield — list of errors and how to fix them (state at the end of step 2/5)

**Legend:** FIXED = changed in source and covered by a check that ran. "NOT COMPILED" = Kotlin code that no Gradle build has seen yet. DEVICE = needs your phone. YOU = only you can do it.

The ZIP `ScamShieldApk-Source__1_.zip` you uploaded is byte-identical in content to the CI-run-#25 source (SHA-256 `a5fb3ef3…ab0a0`); step 1 and step 2 work continues from my tree.

## A. Errors from the review — verified one by one
| # | Error | Verified? | Fix | Status |
|---|---|---|---|---|
| 1 | Local numbers always get +381; `mask()` only knew 381 | Yes (`target()`, `mask()`) | `normPhone(s, country)` with rs 381, ba 387, me 382, mk 389, hr 385; `+`/`00` numbers kept; `(0)` trunk digit removed; `maskPii()` hides any international number, 00-numbers and bare 381/387/382/389/385 | FIXED, 9 Node tests + checker guard |
| 2 | Green for "not enough data" / "no obvious signs" | Yes (CSS `.none`) | Neutral grey `.none/.unknown`; green only for password "Strong" (`.strong`); wording "No obvious warning signs found" + "not proof it is safe" | FIXED, checker guard |
| 3 | Serbian CERT notices have no date | Yes (`makeItem(title,"","",…)`) | New `AdvisoryParsing.parseCertRsListing` reads the date line before each notice (Latin/Cyrillic/English month names, `dd.MM.yyyy.`), tolerant of `/rs/`, `/sr/`, `/en/` link prefixes; falls back to `https://cert.rs/obavestenja.html` | FIXED in code; NOT COMPILED; sample HTML is reconstructed, see D |
| 4 | Advisories de-duplicated by title, sorted by date text | Yes | Identity = normalised URL (host without www, language prefix, utm_ and #fragment removed), title only when there is no link; numeric `ts` field; old cached items get `ts` by parsing their date text; undated last | FIXED in code; NOT COMPILED; logic covered by `AdvisoryParsingTest` (not run) |
| 5 | Native QR screen English-only | Yes | Language sent by the page; Serbian Latin, Cyrillic, English | FIXED in step 1; NOT COMPILED |
| 6 | Deprecated APIs | Yes | ComponentActivity + Activity Result API + OnBackPressedDispatcher + ResolutionSelector | FIXED in step 1; NOT COMPILED |
| 7 | targetSdk 34 | Yes | `targetSdk = 36`; `enableEdgeToEdge()` in both activities; the web page is padded by system-bar, cut-out and keyboard insets; page theme colour is sent to native (`setChrome`) so bars match light/dark | FIXED in code; NOT COMPILED; DEVICE test on Android 15/16 |
| 8 | Workflow pins Gradle 8.9 | Yes | `gradle-version: 8.13`; checker fails if any workflow is below 8.13 | FIXED |
| 9 | APK signing not persistent | **Partly wrong.** `build-apk.yml` already signs with the four secrets when all four exist and validates them; without them it uses the runner debug key | Nothing to change in code | YOU: create keystore and add the 4 secrets (section D) |
| 10 | Croatia beside the main four countries | Yes | Main four chips + "More regions" toggle that reveals Croatia and says other countries are not supported yet | FIXED |
| 11 | Native error texts, deprecated list, no tests | see step 1 | — | FIXED (see AUDIT_REPORT) |

## B. Other things I found in step 2
- `build-apk.yml` in the ZIP had NO check steps although run #25 reportedly ran web checks, so the ZIP's workflow is stale compared to the branch. I added the Node checks (web, Kotlin-regression, QR analysis, QR decode, phone) before the build. Please compare with the workflow on the branch before overwriting it.
- The old Serbian parser required links containing `/rs/obavestenje/`. The live Cyrillic site links notices as `https://cert.rs/obavestenje/<id>-….html` (no `/rs/`). If the Latin page differs the same way the old parser would have found nothing. Fixed by tolerant matching plus a fallback URL.
- "Likely scam" in the local checker can still come from DEMO entries with invented report counts (`DIR` list). It must be driven only by genuine independent reports (>= 5). OPEN → step 3/4.
- `WebSettings.allowContentAccess = true`, `isMinifyEnabled = false` still to review (step 3).
- Android 16 ignores `screenOrientation="portrait"` on large screens (tablets/foldables), so the QR screen may rotate there. Layout works in landscape but is untested. DEVICE.
- Release job still publishes a GitHub Release on every run. YOU decide.

## C. Not code errors, but unfinished (need data, a backend or a decision)
| Feature | Honest state | What it needs |
|---|---|---|
| Phone/email/URL reputation | Local heuristics + labelled DEMO entries | A real provider/dataset + privacy design |
| Community, reports | One phone only | Backend, rate limits, moderation, deletion, 5-independent-reports rule (step 4) |
| Scam map | No verified data | Verified aggregate dataset; until then an explanation, no pins |
| Notifications | No real alert source | Opt-in settings + real source |
| Antivirus | Not implemented; must never be claimed | Keep as a security guide |
| Family protection | Checklists + message generator | Must not be called call blocking/parental control |
| VPN | Deliberately absent | — |
| Montenegro / MKD-CIRT / CERT RS (Republika Srpska) feeds | Not re-verified in this step | Send me saved pages/feeds (section D) |

## D. Only YOU can do these
1. **Compile**: run GitHub Actions (`Build ScamShield APK` and `Static checks and unit tests`) and send me the complete log. I cannot compile Kotlin here, so every Kotlin change is unproven until your build passes. If the log shows an error, send it and I will fix it.
2. **Device test** on the Xiaomi/Redmi with `TEST_CHECKLIST.md` (step 1 QR list + step 2 list). A green build proves nothing about the camera, gallery, edge-to-edge layout or advisories.
3. **Persistent signing key** (once): `keytool -genkeypair -v -keystore scamshield.jks -alias scamshield -keyalg RSA -keysize 4096 -validity 10000`; then GitHub → Settings → Secrets → Actions: `ANDROID_KEYSTORE_BASE64` (`base64 -w0 scamshield.jks`), `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Never commit the file; keep two backups. The first APK with the new key needs a one-time uninstall of the old test build.
4. **Save real pages** for the other feeds: open `https://cert.aikt.rs/feed/`, `https://rss.gov.me/atom/me/gov-cirt`, `https://mkd-cirt.mk/feed/` and `https://www.cert.rs/rs/obavestenja.html` in a browser, "Save page as", and send the files (the Serbian page raw HTML matters most: I only saw a rendered text view).
5. Decide about Release-on-every-run and whether Croatia stays under "More regions".

## E. How to fix if something fails when you run it
| Symptom | Likely cause | Fix |
|---|---|---|
| Gradle: "Unresolved reference enableEdgeToEdge / ViewCompat" | Missing AndroidX artifact version | Make sure `androidx.activity:activity-ktx:1.13.0` is present; add `androidx.core:core-ktx` if needed |
| Gradle: error in `QrImageDecoder`/`QrScannerActivity` | I could not compile it | Send the exact error lines |
| Build fails with HTTP 429 | Maven Central rate limit | Re-run the job |
| Page content hidden under the status/navigation bar | Insets not applied | Test on the device, send screenshot; the padding is in `MainActivity.setOnApplyWindowInsetsListener` |
| Serbian CERT list empty on the phone | Site markup differs from my reconstruction | Send the saved page (D4); the app then shows "unavailable" and keeps cached items |
| Dates missing for some notices | Date text is further than 600 characters from the link in the page HTML | Send the saved page; raise `MAX_DATE_DISTANCE_CHARS` or adapt the rule |


## Independent review addendum — after the step-2 source was inspected and patched

The following additional defects were found and fixed after running the supplied Node checks and compiling/running the pure Kotlin JVM test sources locally:

| Finding | Fix | Verification |
|---|---|---|
| Numeric Serbian dates such as `14.08.2026.` were supported by `parseSerbianDate()` but were not collected by the CERT listing parser. | The listing parser now collects numeric and month-name dates, sorts candidates by position, and associates the nearest preceding date with each notice. | Added JVM test `parsesNumericDateFromListing`; all 25 pure JVM test methods passed in a local Kotlin harness. |
| CERT parser accepted any HTTPS host if its URL contained `/obavestenje/<id>-`. | Notice links are now restricted to `cert.rs` and `www.cert.rs`. | Added JVM test `rejectsNoticeLinksOutsideOfficialCertHost`. |
| DEMO entries contained invented report counts (12, 7, 3, 1), and those counts could produce a “Likely scam” verdict. | Removed the counts. DEMO matches are now explanatory examples only and never affect the verdict. Local warning-signal heuristics can produce only “Suspicious”, “Be careful”, or “Not enough data”; “Likely scam” requires a real report backend that does not exist yet. | Added `scripts/test-verdict.js`; verifies a DEMO match cannot produce a scam verdict or report count. |
| Six-digit numeric tokens could be normalised as phone numbers. | Phone normalisation now requires 7–15 digits and rejects shorter tokens. | Updated phone test; all 9 phone tests pass. |
| Build workflow published a GitHub Release for every push. | Release publishing is now limited to manually dispatched runs; ordinary pushes still build and upload an Actions artifact. | Workflow source inspection. |
| APK build workflow did not run the JVM unit-test task itself. | APK workflow now runs `testDebugUnitTest` before `assembleRelease`. | Workflow source inspection; full Android Gradle build still needs GitHub Actions. |
| Keyboard inset handling was implicit. | MainActivity explicitly uses `windowSoftInputMode=adjustResize`; `allowContentAccess=true` is documented as needed for the user-selected file input used by local file hashing. | Manifest XML validation and static checks. |

### Verification limits

- Node checks passed: `check-web.js`, `check-kotlin.js`, `test-phone-norm.js` (9), `test-qr-analyze.js` (20), `test-qr-decode.js` (11 fixtures), and `test-verdict.js` (2 assertions).
- 25 JVM unit-test methods from `AdvisoryParsingTest`, `QrSamplingTest`, and `QrStringsTest` passed using the installed Kotlin compiler and a small local JUnit-compatible assertion harness. This is a useful source-level test, but it is not a substitute for the Android Gradle test task.
- Kotlin Android activities, Android resources, CameraX integration and APK packaging have **not** been compiled in this environment. Run the GitHub Actions build.
- The CERT HTML fixture remains reconstructed, not a byte-for-byte saved copy of the current live page. Date extraction still needs validation against the real page saved by the user.
- Physical Xiaomi/Redmi camera, gallery, HEIC, rotation, permission and edge-to-edge tests remain outstanding.

### GitHub repository layout warning

The uploaded standalone project contains `.github/workflows/` at its project root. In the existing `ljubanicluka/ScamShield-Android` repository, the Kotlin project is nested under `ScamShieldApk/`, while the repository root README and active project describe the separate .NET MAUI application. GitHub does not run workflow files nested under `ScamShieldApk/.github/workflows/`. Do not assume pushing this ZIP into that subfolder will trigger its workflow. Put the contents of this standalone project at the root of a dedicated repository, or deliberately create a root-level workflow that builds from the subdirectory after confirming the intended repository layout.
