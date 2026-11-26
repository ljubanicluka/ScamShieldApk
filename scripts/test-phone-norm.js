#!/usr/bin/env node
"use strict";
// Tests the real normPhone()/maskPii() shipped in web/scamshield-app.html.
const assert = require("node:assert/strict");
const { loadPhoneNorm } = require("./lib/load-qr-analyze");
const { CALLING, normPhone, maskPii } = loadPhoneNorm();
let n = 0;
const t = (name, fn) => { fn(); n++; console.log("PASS " + name); };

t("calling codes", () => assert.deepEqual({ ...CALLING }, { rs: "381", ba: "387", me: "382", mk: "389", hr: "385" }));
t("local 0-number uses the SELECTED country", () => {
  assert.equal(normPhone("060 555 0101", "rs"), "+381605550101");
  assert.equal(normPhone("061 555 0101", "ba"), "+387615550101");
  assert.equal(normPhone("067 555 010", "me"), "+38267555010");
  assert.equal(normPhone("070 555 010", "mk"), "+38970555010");
  assert.equal(normPhone("091 555 0101", "hr"), "+385915550101");
});
t("international numbers are preserved whatever the country", () => {
  assert.equal(normPhone("+387 61 555 0101", "rs"), "+387615550101");
  assert.equal(normPhone("00385 91 555 0101", "rs"), "+385915550101");
  assert.equal(normPhone("+381 (0) 60 555 0101", "hr"), "+381605550101");
});
t("same number typed three ways gives one key (duplicate detection)", () => {
  const k = ["060/555-0101", "+381 60 5550101", "00381605550101"].map((x) => normPhone(x, "rs"));
  assert.equal(new Set(k).size, 1);
});
t("unknown country falls back to Serbia, never throws", () => assert.equal(normPhone("060 555 0101", "xx"), "+381605550101"));
t("non-phone text and short numeric tokens are not treated as phone numbers", () => { assert.equal(normPhone("paket-info-dostava.xyz", "rs"), null); assert.equal(normPhone("12345", "rs"), null); assert.equal(normPhone("123456", "rs"), null); assert.equal(normPhone(null, "rs"), null); });
t("mask hides international and local numbers and emails", () => {
  const out = maskPii("Zvao me +387 61 555 010 i 060 555 0101, mail a.b@x.rs, 00385915550101");
  assert.ok(!/\d{3}[\s]?\d{3}/.test(out.replace(/\[tel\]/g, "")), out);
  assert.equal((out.match(/\[tel\]/g) || []).length, 3, out);
  assert.ok(out.includes("[email]"));
});
t("mask hides PIN/OTP codes", () => assert.ok(maskPii("moj pin: 4821").includes("[***]")));
t("mask leaves ordinary text and short numbers alone", () => assert.equal(maskPii("Platio sam 1500 dinara 3 puta"), "Platio sam 1500 dinara 3 puta"));
console.log("OK: " + n + " phone-norm tests passed");
