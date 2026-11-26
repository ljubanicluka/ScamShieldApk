#!/usr/bin/env node
"use strict";
// Static regression checks for the Kotlin sources. Kotlin cannot be compiled here without Gradle and
// the Android SDK, so this script only guards against known, previously seen defects. A green result
// does NOT mean the code compiles; only the GitHub Actions Gradle build proves that.
const fs = require("node:fs");
const path = require("node:path");

const dir = path.join(__dirname, "..", "app/src/main/java/rs/scamshield/app");
const files = fs.readdirSync(dir).filter((f) => f.endsWith(".kt"));
let failed = 0;
const fail = (file, msg) => { console.error(file + ": " + msg); failed++; };

for (const f of files) {
  const src = fs.readFileSync(path.join(dir, f), "utf8");
  const code = src.replace(/\/\*[\s\S]*?\*\//g, "").replace(/^\s*\/\/.*$/gm, "");

  // Error 1: Barcode must come from the .common package.
  if (/import\s+com\.google\.mlkit\.vision\.barcode\.Barcode\b/.test(code)) fail(f, "imports com.google.mlkit.vision.barcode.Barcode (must be ...barcode.common.Barcode)");
  // Errors 2 and 3: legacy permission API must not return.
  if (/\brequestPermissions\s*\(/.test(code)) fail(f, "uses legacy requestPermissions(); use ActivityResultContracts.RequestPermission");
  if (/REQUEST_CAMERA_PERMISSION/.test(code)) fail(f, "references the removed REQUEST_CAMERA_PERMISSION constant");
  if (/override\s+fun\s+onRequestPermissionsResult/.test(code)) fail(f, "overrides onRequestPermissionsResult (migrated to the Activity Result API)");
  // Deprecated APIs reported by CI run #25.
  if (/\bsetTargetResolution\s*\(/.test(code)) fail(f, "uses deprecated setTargetResolution(); use ResolutionSelector");
  if (/override\s+fun\s+onBackPressed\s*\(/.test(code)) fail(f, "overrides deprecated onBackPressed(); use OnBackPressedDispatcher");
  if (/\bstartActivityForResult\s*\(|override\s+fun\s+onActivityResult/.test(code)) fail(f, "uses startActivityForResult/onActivityResult; use registerForActivityResult");
  // Never log decoded QR content.
  if (/Log\.\w\([^)]*(payload|rawValue|qrText)/i.test(code)) fail(f, "logs QR content");
}

const scanner = fs.readFileSync(path.join(dir, "QrScannerActivity.kt"), "utf8");
if (!/imageProxy\.close\(\)/.test(scanner) || !/finally\s*\{[^}]*imageProxy\.close\(\)/s.test(scanner))
  fail("QrScannerActivity.kt", "ImageProxy must be closed in a finally block on every path");
if (!/addOnCompleteListener\s*\{\s*imageProxy\.close\(\)/.test(scanner))
  fail("QrScannerActivity.kt", "ImageProxy must also be closed when the ML Kit task completes");
if (!/clearAnalyzer\(\)/.test(scanner) || !/unbindAll\(\)/.test(scanner) || !/scanner\.close\(\)/.test(scanner) || !/cameraExecutor\.shutdown\(\)/.test(scanner))
  fail("QrScannerActivity.kt", "onDestroy must clear the analyzer, unbind the camera, close ML Kit and stop the executor");
if (!/Manifest\.permission\.CAMERA\)/.test(scanner) || !/cameraPermissionLauncher\.launch\(Manifest\.permission\.CAMERA\)/.test(scanner))
  fail("QrScannerActivity.kt", "camera permission must be requested through cameraPermissionLauncher");

const main = fs.readFileSync(path.join(dir, "MainActivity.kt"), "utf8");
for (const need of ["isTrustedPage()", "confirmAndOpen", "ACTION_VIEW"]) {
  if (!main.includes(need)) fail("MainActivity.kt", "missing " + need);
}
// A QR payload may only reach ACTION_VIEW through confirmAndOpen (explicit user confirmation).
const views = [...main.matchAll(/Intent\.ACTION_VIEW/g)].length;
if (views > 2) fail("MainActivity.kt", "unexpected extra ACTION_VIEW launches (" + views + "); QR content must never open automatically");
for (const bad of ["ACTION_CALL", "ACTION_DIAL", "ACTION_SENDTO", "WifiManager", "ACTION_SEND"]) {
  for (const f of files) if (fs.readFileSync(path.join(dir, f), "utf8").includes(bad)) fail(f, "uses " + bad + ": QR content must never place calls, send messages or join Wi-Fi");
}
const manifest = fs.readFileSync(path.join(__dirname, "..", "app/src/main/AndroidManifest.xml"), "utf8");
if (!/android:windowSoftInputMode="adjustResize"/.test(manifest)) fail("AndroidManifest.xml", "MainActivity must use adjustResize so the keyboard does not cover inputs under edge-to-edge");
for (const bad of ["CALL_PHONE", "SEND_SMS", "CHANGE_WIFI_STATE", "READ_EXTERNAL_STORAGE", "BIND_VPN_SERVICE"]) {
  if (manifest.includes(bad)) fail("AndroidManifest.xml", "declares " + bad);
}

// Build configuration regressions (step 2).
const gradle = fs.readFileSync(path.join(__dirname, "..", "app/build.gradle.kts"), "utf8");
const target = Number((gradle.match(/targetSdk\s*=\s*(\d+)/) || [])[1]);
if (!(target >= 35)) fail("app/build.gradle.kts", "targetSdk must be 35 or higher (found " + target + ")");
const wfDir = path.join(__dirname, "..", ".github/workflows");
if (fs.existsSync(wfDir)) {
  for (const wf of fs.readdirSync(wfDir)) {
    const text = fs.readFileSync(path.join(wfDir, wf), "utf8");
    const v = text.match(/gradle-version:\s*([\d.]+)/);
    if (v) {
      const [maj, min] = v[1].split(".").map(Number); // compare as numbers per part: 8.9 is OLDER than 8.13
      if (maj < 8 || (maj === 8 && min < 13)) fail(wf, "Gradle " + v[1] + " is too old for AGP 8.11.1 (needs 8.13+)");
    }
  }
}
const adv = fs.readFileSync(path.join(dir, "LiveAdvisoryService.kt"), "utf8");
if (/makeItem\([^)]*,\s*""\s*,\s*""\s*,/.test(adv)) fail("LiveAdvisoryService.kt", "an advisory is created with an empty date again");
if (/optString\("date"\)\s*\}\s*\)/.test(adv) || /thenByDescending\s*\{\s*it\.optString\("date"\)/.test(adv)) fail("LiveAdvisoryService.kt", "advisories are sorted by date text; sort by numeric timestamp");
if (/distinctBy\s*\{[^}]*titleKey[^}]*\.ifBlank/s.test(adv)) fail("LiveAdvisoryService.kt", "advisories are de-duplicated by title first; use the URL");
const parser = fs.readFileSync(path.join(dir, "AdvisoryParsing.kt"), "utf8");
if (!parser.includes("numericDateInText.findAll(page)")) fail("AdvisoryParsing.kt", "CERT listing parser must collect numeric dates as well as month-name dates");
if (!parser.includes("isOfficialCertLink(link)")) fail("AdvisoryParsing.kt", "CERT parser must reject non-cert.rs notice links");

if (failed) { console.error(failed + " Kotlin/static check failure(s)"); process.exit(1); }
console.log("OK: " + files.length + " Kotlin files passed the regression checks (this is not a compile check).");
