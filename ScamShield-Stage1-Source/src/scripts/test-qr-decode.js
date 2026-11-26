#!/usr/bin/env node
"use strict";
// Regression test for the bundled jsQR decoder (the non-Android fallback) against the PNG fixtures.
// It mirrors the retry loop in the page's decodeSource(): several scale factors, both polarities.
// It does NOT test ML Kit (Android) - that needs the instrumented test or a physical device.
const fs = require("node:fs");
const path = require("node:path");
const { decodePng } = require("./lib/png");
const jsQR = require(path.join(__dirname, "..", "app/src/main/assets/jsQR.js"));
const { loadQrAnalyze } = require("./lib/load-qr-analyze");
const { qrTiles } = loadQrAnalyze();

const dir = path.join(__dirname, "..", "tests/fixtures/qr");
const expected = JSON.parse(fs.readFileSync(path.join(dir, "expected.json"), "utf8"));
// jsQR returns one code per call; these fixtures are known to contain more than one or are not supported.
const SINGLE_ONLY = new Set(["multi.png"]);

function resize(img, w, h) {
  const out = new Uint8ClampedArray(w * h * 4);
  for (let y = 0; y < h; y++) {
    const sy = Math.min(img.height - 1, Math.floor((y + 0.5) * img.height / h));
    for (let x = 0; x < w; x++) {
      const sx = Math.min(img.width - 1, Math.floor((x + 0.5) * img.width / w));
      const s = (sy * img.width + sx) * 4, d = (y * w + x) * 4;
      out[d] = img.data[s]; out[d + 1] = img.data[s + 1]; out[d + 2] = img.data[s + 2]; out[d + 3] = 255;
    }
  }
  return out;
}

function crop(img, x, y, w, h) {
  const data = new Uint8ClampedArray(w * h * 4);
  for (let j = 0; j < h; j++)
    data.set(img.data.subarray(((y + j) * img.width + x) * 4, ((y + j) * img.width + x + w) * 4), j * w * 4);
  return { width: w, height: h, data };
}

function decodeLikeThePage(img) {
  for (const t of qrTiles(img.width, img.height, true)) {
    const tile = crop(img, t[0], t[1], t[2], t[3]);
    const scale = Math.min(1, 1800 / Math.max(tile.width, tile.height));
    for (const factor of [1, 0.75, 0.5, 1.25]) {
      const W = Math.max(80, Math.round(tile.width * scale * factor));
      const H = Math.max(80, Math.round(tile.height * scale * factor));
      if (W > 2400 || H > 2400) continue;
      const r = jsQR(resize(tile, W, H), W, H, { inversionAttempts: "attemptBoth" });
      if (r && r.data) return r.data;
    }
  }
  return null;
}

let failed = 0;
for (const [name, want] of Object.entries(expected)) {
  const img = decodePng(fs.readFileSync(path.join(dir, name)));
  const got = decodeLikeThePage(img);
  let ok;
  if (want.length === 0) ok = got === null;
  else if (SINGLE_ONLY.has(name)) ok = want.includes(got);
  else ok = got === want[0];
  console.log((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  got=" + JSON.stringify(got) + " want=" + JSON.stringify(want)));
  if (!ok) failed++;
}
if (failed) { console.error(failed + " fixture(s) failed"); process.exit(1); }
console.log("OK: jsQR fallback decoded all fixtures as expected (no-QR image correctly returned nothing).");
