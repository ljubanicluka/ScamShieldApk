#!/usr/bin/env node
"use strict";
// Tests the real qrParse() / qrTiles() shipped in web/scamshield-app.html.
const assert = require("node:assert/strict");
const { loadQrAnalyze } = require("./lib/load-qr-analyze");
const { qrParse, qrTiles, QKN } = loadQrAnalyze();

let n = 0;
const t = (name, fn) => { fn(); n++; console.log("PASS " + name); };

t("https URL", () => { const p = qrParse("https://example.com/a?b=1"); assert.equal(p.k, "url"); assert.equal(p.f.url, "https://example.com/a?b=1"); });
t("bare domain becomes an https URL candidate, never opened", () => { const p = qrParse("example.rs/path"); assert.equal(p.k, "url"); assert.equal(p.f.url, "https://example.rs/path"); });
t("plain text stays text", () => assert.equal(qrParse("hello world").k, "text"));
t("empty/whitespace is text with no fields", () => { assert.equal(qrParse("   ").k, "text"); assert.equal(qrParse(null).k, "text"); });
t("Wi-Fi with escapes", () => {
  const p = qrParse("WIFI:T:WPA;S:My\\;Net;P:pa\\:ss;H:true;;");
  assert.equal(p.k, "wifi"); assert.equal(p.f.ssid, "My;Net"); assert.equal(p.f.auth, "WPA");
  assert.equal(p.f.hidden, true); assert.equal(p.f.hasPassword, true);
  assert.ok(!JSON.stringify(p.f).includes("pa:ss"), "the password must not be copied into display fields");
});
t("Wi-Fi open network", () => { const p = qrParse("WIFI:T:nopass;S:Guest;;"); assert.equal(p.f.hasPassword, false); assert.equal(p.f.auth, "NOPASS"); });
t("tel", () => { const p = qrParse("tel:+381601234567"); assert.equal(p.k, "tel"); assert.equal(p.f.num, "+381601234567"); });
t("sms with query body", () => { const p = qrParse("sms:+38160111?body=Hello%20there"); assert.equal(p.k, "sms"); assert.equal(p.f.num, "+38160111"); assert.equal(p.f.body, "Hello there"); });
t("SMSTO with colon body", () => { const p = qrParse("SMSTO:+38160111222:Pozovi me"); assert.equal(p.k, "sms"); assert.equal(p.f.num, "+38160111222"); assert.equal(p.f.body, "Pozovi me"); });
t("mailto", () => { const p = qrParse("mailto:a@b.rs?subject=Hi&body=x"); assert.equal(p.k, "mail"); assert.equal(p.f.to, "a@b.rs"); assert.equal(p.f.subject, "Hi"); });
t("MATMSG email", () => { const p = qrParse("MATMSG:TO:a@b.rs;SUB:Hello;BODY:x;;"); assert.equal(p.k, "mail"); assert.equal(p.f.to, "a@b.rs"); assert.equal(p.f.subject, "Hello"); });
t("geo", () => { const p = qrParse("geo:44.8176,20.4633?q=Beograd"); assert.equal(p.k, "geo"); assert.equal(p.f.lat, "44.8176"); assert.equal(p.f.lon, "20.4633"); });
t("Serbian IPS payment", () => {
  const p = qrParse("K:PR|V:01|C:1|R:160000000000000000|N:Test Primalac|I:RSD1500,00|SF:289|S:Racun 12");
  assert.equal(p.k, "pay"); assert.equal(p.f.kind, "IPS"); assert.equal(p.f.name, "Test Primalac");
  assert.equal(p.f.account, "160000000000000000"); assert.equal(p.f.amount, "RSD1500,00"); assert.equal(p.f.purpose, "Racun 12");
});
t("EPC payment", () => { const p = qrParse("BCD\n001\n1\nSCT\nBIC\nAcme Ltd\nDE00123456781234567890\nEUR12.34\n\n\nInv 7"); assert.equal(p.k, "pay"); assert.equal(p.f.kind, "EPC"); assert.equal(p.f.name, "Acme Ltd"); assert.equal(p.f.amount, "EUR12.34"); });
t("crypto URI", () => { const p = qrParse("bitcoin:bc1qxyz?amount=0.1"); assert.equal(p.k, "pay"); assert.equal(p.f.kind, "bitcoin"); assert.equal(p.f.account, "bc1qxyz"); });
t("otpauth", () => assert.equal(qrParse("otpauth://totp/x?secret=ABC").k, "otp"));
t("javascript:/data: payloads are plain text, never a URL", () => { assert.equal(qrParse("javascript:alert(1)").k, "text"); assert.equal(qrParse("data:text/html,<b>x</b>").k, "text"); });
t("every kind has a label in both languages", () => { for (const k of ["url","wifi","tel","sms","mail","geo","pay","otp","text"]) assert.ok(QKN[k] && QKN[k][0] && QKN[k][1], k); });
t("tiles: single tile for live video, five for gallery", () => { assert.equal(qrTiles(1000, 800, false).length, 1); assert.equal(qrTiles(1000, 800, true).length, 5); });
t("tiles stay inside the image", () => { for (const [x,y,w,h] of qrTiles(1001, 777, true)) { assert.ok(x >= 0 && y >= 0 && x + w <= 1001 && y + h <= 777); } });
console.log("OK: " + n + " qr-analyze tests passed");
