# ScamShield — test checklist

## CI / static checks
- [ ] Build the Android release variant with Java 17 and API 36.
- [ ] Run `node scripts/check-web.js app/src/main/assets/index.html`.
- [ ] Verify APK ZIP integrity and APK signature.
- [ ] Verify the source bundle and SHA-256 file are included in the GitHub release.
- [ ] Confirm the APK and source bundle checksums match the CI logs.

## QR camera — physical Android device required
- [ ] First launch asks for camera permission only after pressing “Scan QR with camera”.
- [ ] Granting permission opens the native rear-camera preview.
- [ ] A normal QR code is detected and its text appears in the ScamShield checker.
- [ ] QR containing an HTTPS URL is analyzed but never auto-opened.
- [ ] Wi-Fi, phone, SMS, email, and plain-text QR payloads are displayed as text and not executed.
- [ ] Flashlight toggles on devices with flash hardware.
- [ ] Cancel closes the camera and returns to the app.
- [ ] Denying permission shows a usable message and the gallery option still works.
- [ ] Scanning, cancelling, and scanning again does not leave the camera active or crash.

## QR gallery — physical Android device required
- [ ] JPEG image with QR decodes.
- [ ] PNG image with QR decodes.
- [ ] HEIC image with QR decodes on supported devices.
- [ ] Rotated image is decoded correctly.
- [ ] High-resolution and compressed screenshots are tested.
- [ ] Image without a QR shows an understandable error.
- [ ] Cancelling the picker does not crash or show a false failure.
- [ ] QR content stays local and is not sent to the advisory sources.

## Other interactions
- [ ] Complete onboarding and open every home tile.
- [ ] Toggle English/Serbian and Latin/Cyrillic.
- [ ] Toggle light/dark theme and large text.
- [ ] Check local URL heuristics and verify wording does not claim “safe”.
- [ ] Create, view, and delete a local report note.
- [ ] Tick help and family checklists and confirm they persist.
- [ ] Generate and copy the family warning message.
- [ ] Save and delete evidence notes.
- [ ] Calculate SHA-256 from a selected file and copy the result.
- [ ] Open external official links in the Android browser.
- [ ] Open ScamShield's Android app settings shortcut.
- [ ] Refresh CERT/CIRT advisories and inspect online/cached/unavailable states.
- [ ] Delete all local app data only after the two-step confirmation.

## Device matrix
Test at minimum: one Xiaomi/Redmi device (the user's intended target), one Android emulator, and one other Android manufacturer/version if available. Record Android version, WebView version, device model, image format, and outcome. Do not mark a test complete based only on a successful Gradle build.


## Additional QR regression checks after the 2026-11-19 fixes

- [ ] Build #14 or later completes all Gradle Kotlin compilation, APK validation, and source-bundle upload steps.
- [ ] Camera permission prompt appears only after the user taps the camera-scan button.
- [ ] Denying permission returns to the QR page and shows a clear message; gallery selection remains available.
- [ ] Granting permission starts the native rear-camera preview.
- [ ] Scanner handles a blurry frame without freezing or leaving the camera analyzer blocked.
- [ ] Scanner can be cancelled and reopened repeatedly.
- [ ] Image picker cancellation is harmless and does not display a stale failure.
- [ ] Gallery scanning reports decode errors clearly and does not auto-open scanned URLs.
- [ ] The same QR is tested on a physical Xiaomi/Redmi phone and Android emulator.
- [ ] The static web checker reports no rendered action without a matching click handler.
- [ ] The built Android assets contain local `jsQR.js` and no external CDN reference.

## Features explicitly not considered complete

- [ ] Live phone/email/URL reputation source and an audited privacy model.
- [ ] Shared reporting service with independent-report deduplication, rate limiting and moderation.
- [ ] Verified scam map dataset with aggregation and privacy protections.
- [ ] Real notification opt-in and settings.
- [ ] Export/import backup with a clear warning about personally identifying evidence.

---

# STEP 1/5 additions — Xiaomi/Redmi device test (QR)

Nothing below has been run. Mark each line PASS/FAIL and note the phone model, Android/HyperOS version and Gallery app. Please try Serbian Latin, Cyrillic and English at least once each.

## Camera
- [ ] First launch: tap "Skeniraj QR kamerom" → system camera permission prompt appears; allow → preview shows, status text in the chosen language.
- [ ] Deny once → error card with "Otvori podešavanja…"; button opens Android app settings. Grant there, return, "Try the camera again" works.
- [ ] Deny with "Don't ask again" / MIUI permission manager "Deny" → same card, no crash, no blank screen.
- [ ] Scan a QR on another phone screen, a printed QR, and a screenshot on a laptop. Time to detect: ____ s.
- [ ] Low light: flashlight button turns on/off, label follows the real state; works after leaving and re-entering the screen.
- [ ] Tap the preview to focus.
- [ ] Two QR codes in the frame → result card shows "Several QR codes found" and numbered chips.
- [ ] Press Home during scanning, return; lock/unlock the screen; rotate (screen is portrait-locked); press Back; press Cancel → no frozen preview, no stuck "Starting camera", app still responsive.
- [ ] Hold the camera at nothing for 15 s → help hint appears.
- [ ] Open another camera app, then ScamShield scanner (camera busy) → "Try again" appears or scanning works; no crash.
- [ ] Leave the scanner running 2 min, then check the Xiaomi battery/heat; device does not stay awake after leaving (screen timeout works again).

## Gallery
- [ ] JPEG photo of a QR; PNG screenshot; WhatsApp-compressed copy; Telegram copy.
- [ ] HEIC/HEIF image if your Xiaomi camera or Gallery can save one (Camera settings). Result: ____ (an "unsupported format" message is acceptable on old Android).
- [ ] Photo rotated by the Gallery app (rotate 90°) and a sideways camera photo.
- [ ] 108 MP / highest-resolution photo (Redmi Note series "108MP" mode) → no crash, either a result or "image too large" message; the app stays responsive while reading ("Čitam QR sa slike…" appears).
- [ ] Image with two QR codes → both offered.
- [ ] Image without a QR code → "No QR code was found…" card (not just a toast).
- [ ] Corrupted file (rename a .txt to .jpg) → "cannot be opened … damaged" message.
- [ ] Open the picker and cancel → no error, no stuck loading card.
- [ ] Pick via the system Files app and via Gallery (different providers).

## Content types and safety (use a QR generator to make each)
- [ ] https link → "Web address", heuristic verdict, "Open in browser (asks first)" shows a native dialog with the full URL; "Do not open" does nothing.
- [ ] `tel:`, `sms:`, `mailto:`, `geo:` → labelled, nothing is dialled/sent/opened.
- [ ] `WIFI:T:WPA;S:Test;P:pw;;` → shows network name and security, does not join, does not show the password.
- [ ] Serbian IPS payment QR (from a bank app) → recipient/account/amount shown, nothing paid.
- [ ] `javascript:alert(1)` → treated as text, nothing runs.
- [ ] Switch language mid-session; open the "Open link" dialog → dialog text follows the language.
- [ ] Airplane mode: camera and gallery scanning both still work (all local).

## Regression
- [ ] Back button inside the app navigates pages; at the first page it closes the app (no double-press needed, no crash).
- [ ] Other screens still open from the home grid (full per-screen test follows in step 2).

---

# STEP 2/5 additions — Xiaomi/Redmi device test

- [ ] **Layout (target API 36):** on Android 15/16 the top bar, bottom navigation and keyboard do not cover page content; the status/navigation bar colour follows the light/dark switch in the app, icons are readable in both themes; camera screen buttons clear the navigation bar. Gesture and 3-button navigation both.
- [ ] Phone check: pick Serbia, type `060 555 0101`; pick Bosnia, same digits → checker/duplicate key shows +387…; Croatia → +385…; type `+381 60 555 0101` while Bosnia is selected → stays +381…
- [ ] Report form: write a description containing a Bosnian (`061 555 010`) and an international number → the saved report text shows `[tel]`, not digits.
- [ ] Verdict colours: a random unknown phone number → grey "NOT ENOUGH DATA" (no green anywhere). A password of 5+ strength points → green "Strong". Link check with no flags → grey "No obvious warning signs found" with the "not proof" note.
- [ ] Regions: four country chips + "More regions"; tapping it reveals Croatia and the text that other countries are unsupported; selecting Croatia keeps the row open; switching back works.
- [ ] Official advisories screen: pull refresh with Wi-Fi on → Serbia shows items WITH dates, newest first, no duplicates; each source has an honest status (online / unavailable / cached) and "last updated"; airplane mode → cached items remain, status says unavailable, nothing is called "live".
- [ ] Open one Serbian notice → the official page opens in the browser.
- [ ] Re-test step 1 QR list (camera, gallery, deny permission, language switch) after this build: the insets change touched the camera screen too.
