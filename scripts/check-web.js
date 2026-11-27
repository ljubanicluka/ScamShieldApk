#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const vm = require("node:vm");
const path = process.argv[2] || "app/src/main/assets/index.html";

if (!fs.existsSync(path)) {
  console.error("Bundled HTML file not found: " + path);
  process.exit(1);
}

const html = fs.readFileSync(path, "utf8");
const scripts = [...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)];
let inlineCount = 0;

for (const match of scripts) {
  const attrs = match[1] || "";
  const body = match[2] || "";
  if (/\bsrc\s*=/.test(attrs) || !body.trim()) continue;
  inlineCount += 1;
  try {
    new vm.Script(body, { filename: path + "#inline-script-" + inlineCount });
  } catch (error) {
    console.error("Inline JavaScript syntax error:", error.message);
    process.exit(1);
  }
}

if (inlineCount === 0) {
  console.error("No inline application script was found.");
  process.exit(1);
}

// Discover every action rendered in markup and require a matching delegated handler.
const renderedActions = new Set(
  [...html.matchAll(/\bdata-a\s*=\s*["']?([A-Za-z][A-Za-z0-9_-]*)/g)]
    .map((match) => match[1])
);
const handledActions = new Set(
  [...html.matchAll(/\ba\s*===\s*["']([A-Za-z][A-Za-z0-9_-]*)["']/g)]
    .map((match) => match[1])
);

if (renderedActions.size === 0) {
  console.error("No data-a actions were found in the interface.");
  process.exit(1);
}

for (const action of renderedActions) {
  if (!handledActions.has(action)) {
    console.error("Rendered action has no click-handler branch: data-a=" + action);
    process.exit(1);
  }
}

// Android bundles the decoder locally; remote CDN script loading is blocked by the WebView.
if (!/<script\b[^>]*\bsrc\s*=\s*["']jsQR\.js["']/i.test(html)) {
  console.error("Bundled local jsQR.js script reference is missing.");
  process.exit(1);
}
if (html.toLowerCase().includes("cdn.jsdelivr.net/npm/jsqr")) {
  console.error("An external jsQR CDN reference remains in the Android asset.");
  process.exit(1);
}

for (const hook of [
  "window.onNativeQrScanResult",
  "window.onNativeQrImageResult",
  "window.onNativeQrState",
  "ScamShieldNative.launchQrScanner",
  "ScamShieldNative.pickQrImage"
]) {
  if (!html.includes(hook)) {
    console.error("Missing native QR integration hook: " + hook);
    process.exit(1);
  }
}

// Every ScamShieldNative.<method> the page calls or feature-tests must exist as a
// @JavascriptInterface method in MainActivity.kt, otherwise the button silently does nothing.
const mainKt = require("node:path").join(__dirname, "..", "app/src/main/java/rs/scamshield/app/MainActivity.kt");
if (fs.existsSync(mainKt)) {
  const kt = fs.readFileSync(mainKt, "utf8");
  const exposed = new Set(
    [...kt.matchAll(/@JavascriptInterface\s+fun\s+([A-Za-z0-9_]+)/g)].map((m) => m[1])
  );
  const used = new Set(
    [...html.matchAll(/ScamShieldNative\.([A-Za-z0-9_]+)/g)].map((m) => m[1])
  );
  // Calls made through a local alias (const n=window.ScamShieldNative; n.openExternalUrl(...)).
  const aliases = [...html.matchAll(/(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*window\.ScamShieldNative\s*;/g)].map((m) => m[1]);
  for (const alias of aliases) {
    for (const m of html.matchAll(new RegExp("\\b" + alias.replace(/\$/g, "\\$") + "\\.([A-Za-z0-9_]+)\\s*\\(", "g"))) used.add(m[1]);
  }
  for (const name of used) {
    if (!exposed.has(name)) {
      console.error("Page uses ScamShieldNative." + name + " but MainActivity.kt does not expose it.");
      process.exit(1);
    }
  }
  if (used.size === 0) {
    console.error("No ScamShieldNative bridge calls were found in the page.");
    process.exit(1);
  }
}

// "Not enough data" must never be styled green (it looked like a safety confirmation).
if (/\.none\s*[,{][^}]*var\(--ok\)/.test(html)) {
  console.error("The .none (not enough data) style uses the green --ok colour.");
  process.exit(1);
}
if (!/\.unknown/.test(html) || !/\.strong\{background:var\(--ok\)\}/.test(html)) {
  console.error("Missing neutral .unknown style or the password-only green .strong style.");
  process.exit(1);
}
// Phone numbers must use the selected country's calling code, not a hard-coded 381.
if (/'381'\s*\+\s*d\.slice\(1\)/.test(html)) {
  console.error("Hard-coded Serbian prefix found in phone normalisation.");
  process.exit(1);
}

// QR text must never be evaluated or navigated to by the page itself.
for (const bad of [/\beval\s*\(/, /\bnew\s+Function\s*\(/, /location\s*(\.href)?\s*=\s*(?:v|raw|payload|value)\b/, /window\.open\s*\(/]) {
  if (bad.test(html)) {
    console.error("Unsafe pattern found in the page: " + bad);
    process.exit(1);
  }
}

// DEMO examples must never be treated as independent user reports or drive a scam verdict.
if (/ind\s*>=\s*5|hits\.reduce\([^)]*d\[2\]/s.test(html) || /Independent reports \(DEMO\)/i.test(html)) {
  console.error("DEMO examples are being counted as real independent reports.");
  process.exit(1);
}
if (!/Matches an internal reference example, not a user report|Poklapa se sa internim referentnim primerom, ne sa prijavom korisnika/i.test(html)) {
  console.error("A matching internal sample must be distinguished from a real user report.");
  process.exit(1);
}
if (/DEMO MODE|DEMO CONSOLE|<span[^>]*>DEMO<\\/span>/i.test(html)) {
  console.error("Visible DEMO labels remain in the user interface.");
  process.exit(1);
}

if (/target\s*=\s*["_']?_blank/i.test(html)) {
  console.error('Found target="_blank" despite multiple WebView windows being disabled.');
  process.exit(1);
}

console.log("OK: " + inlineCount + " inline script(s), required action handlers, native bridge references, QR hooks, and external-link policy.");
