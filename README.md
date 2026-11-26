# ScamShield APK
A local-first Kotlin Android application that hosts the ScamShield interface in a WebView and downloads public CERT/CIRT advisories through a native read-only service. Built by GitHub Actions.

1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open Actions > "Build ScamShield APK" > Run workflow. Pushes to `main` also build and upload an Actions artifact.
3. A manually dispatched run publishes a GitHub Release. Ordinary push builds do not publish a Release; download their APK from the `ScamShield-APK` Actions artifact.
4. On the phone: allow "install unknown apps" for your browser or file manager, then open the APK.

Notes: the APK has CAMERA and INTERNET permissions. Camera access is requested only for QR scanning. Internet access is used by the native read-only advisory service to download public notices from Serbia National CERT, CERT RS (Republika Srpska only), Montenegro Government CIRT and North Macedonia MKD-CIRT. The service does not transmit checked URLs, messages, QR payloads, local reports or evidence. Advisory providers will receive normal network metadata such as the device IP address. Community notes and reports remain local-only. Official advisory titles, short excerpts, dates and source links are shown in the app; full articles remain on their original websites. Items are heuristically labelled for ordinary users, administrators or both. This classification can be imperfect.
To change the app, edit `web/scamshield-app.html` and push; the workflow rebuilds the APK.

## Optional: one permanent signing key (so updates install over the old app)
1. On your PC: `keytool -genkeypair -v -keystore release.keystore -alias scamshield -keyalg RSA -keysize 2048 -validity 10000`
2. Back up `release.keystore` and its passwords somewhere safe. NEVER commit them. If you lose the key you must uninstall and reinstall.
3. Encode it: in PowerShell run `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))` and copy the output.
4. GitHub repo > Settings > Secrets and variables > Actions > add: `ANDROID_KEYSTORE_BASE64` (the output), `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS` (scamshield), `ANDROID_KEY_PASSWORD`.
5. Run the workflow again. The first APK signed with the new key still needs the old differently-signed build uninstalled once; after that, updates signed with the same permanent key can install over it. Configure all four secrets together. The workflow now fails if only some signing secrets are supplied.


The workflow assigns an increasing `versionCode` from the GitHub Actions run number, validates both the keystore password and key password, and verifies the final APK signature before publishing. Do not lose the permanent keystore: Android updates require the same signing certificate. Keep a secure offline backup.

## QR scanning (updated in step 1/5)

- Camera: native CameraX + bundled ML Kit, reads on the device. Gallery: system picker, down-sampled decode on a background thread, bundled ML Kit. Both work offline.
- Decoded content is **never opened automatically**. The app shows its type (web address, Wi-Fi, phone, SMS, email, location, payment, 2FA setup data, text), explains the risk and lets you copy it. Web addresses can be opened only after a confirmation dialog that shows the full address.
- The local link check is a heuristic, not a threat database. "No obvious signs" is not proof that something is safe.
- Status: code-level changes only; not yet compiled or tested on a phone. See `TEST_CHECKLIST.md`.

### Local checks
```
node scripts/check-web.js app/src/main/assets/index.html
node scripts/check-kotlin.js
node scripts/test-qr-analyze.js
node scripts/test-qr-decode.js
```

## Step 2/5 notes
- Phone numbers are normalised with the selected country's calling code (RS 381, BA 387, ME 382, MK 389, HR 385); numbers typed with `+` or `00` keep their own prefix.
- Grey = not enough data / no obvious warning signs (never proof of safety). Green is used only for the password strength result.
- Official advisories: items are identified by URL and sorted by real publication time; Serbian CERT notices now carry their dates. A source is shown as online only when it was actually retrieved and parsed.
- Target API 36 with edge-to-edge layout (not yet tested on a device).
- Extra check: `node scripts/test-phone-norm.js`.


## Stage 1/3 — six-page UI and demo community

- Six swipeable main pages with a persistent bottom navigation: Device Security, Home, Community, QR Scanner, I Got Scammed and Family.
- Profile/settings panel lets the user reorder the six pages and choose the startup page.
- Community currently runs as an offline DEMO sandbox: 100 synthetic accounts and 1,200 generated demo posts. Every item is labelled DEMO and is excluded from real scam verdicts or incident counts.
- Demo reputation is based on net votes (upvotes minus downvotes): 40, 80, 120, 160 and 200 unlock Levels 1–5. A net score of 100 is Level 2.
- Admin glow is a separate manual flag. The demo Admin console can grant/revoke it for any account and remove demo users/posts. Private demo profiles are masked from public and Moderator views; Admin and Owner can inspect them.
- Demo role switching is for UI testing only. It is not real authentication or permission enforcement.
- `web/scamshield-app.html` is the canonical editable UI source; CI copies it into the Android assets.

Run `node scripts/test-demo-community.js` to check the demo account/post counts, net-vote thresholds, private-profile visibility and independent Admin glow.

The next stage is the real backend and security model. It requires a Supabase project and its public project URL/anon key; never put a service-role key in the Android app. Until that is configured, community interactions remain local demo data.
