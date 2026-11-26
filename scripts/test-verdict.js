#!/usr/bin/env node
"use strict";
// Execute the shipped verdict() function with the real demo/signature tables.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const html = fs.readFileSync("app/src/main/assets/index.html", "utf8");
const dirMatch = html.match(/const DIR=(\[[^\n]+\]);/);
const sigMatch = html.match(/const SIG=(\[[^\n]+\]);/);
const verdictMatch = html.match(/function verdict\(q\)\{[\s\S]*?return\{v,fl,demoMatches,hits:demoMatches\}\}/);
assert.ok(dirMatch && sigMatch && verdictMatch, "Could not extract shipped verdict data/function");
const DIR = vm.runInNewContext(dirMatch[1]);
const SIG = vm.runInNewContext(sigMatch[1]);
const verdict = new Function("target", "nz", "DIR", "SIG", `${verdictMatch[0]}; return verdict;`)(
  (s) => String(s).trim().toLowerCase(),
  (s) => String(s).trim().toLowerCase(),
  DIR,
  SIG
);
const demo = verdict("+381605550101");
assert.equal(demo.demoMatches.length, 1, "Expected the sample entry to match");
assert.equal(demo.v, "none", "A DEMO match must not create a scam/suspicious verdict");
assert.equal(Object.hasOwn(demo, "ind"), false, "The result must not expose invented report counts");
const heuristic = verdict("Hitno odmah uplatite novac, pošaljite PIN i otvorite bit.ly/example");
assert.equal(heuristic.v, "susp", "Multiple local warning signals should be labelled suspicious");
console.log("PASS DEMO matches do not affect verdicts or fabricate report counts");
console.log("PASS multiple local warning signals still produce a cautious suspicious result");
