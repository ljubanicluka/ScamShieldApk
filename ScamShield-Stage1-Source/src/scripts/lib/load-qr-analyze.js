"use strict";
// Loads pure helper blocks that live inside web/scamshield-app.html between
// "// <name>" and "// </name>" markers, so Node tests exercise the code that ships.
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

function loadBlock(name, exportsList, file) {
  const html = fs.readFileSync(file || path.join(__dirname, "..", "..", "web", "scamshield-app.html"), "utf8");
  const m = html.match(new RegExp("// <" + name + ">([\\s\\S]*?)// </" + name + ">"));
  if (!m) throw new Error(name + " block not found in the web page");
  const ctx = vm.createContext({ URLSearchParams });
  vm.runInContext(m[1] + "\nthis.__api={" + exportsList.join(",") + "};", ctx, { filename: name });
  return ctx.__api;
}
const loadQrAnalyze = (file) => loadBlock("qr-analyze", ["qrParse", "qrTiles", "QKN", "qrFields"], file);
const loadPhoneNorm = (file) => loadBlock("phone-norm", ["CALLING", "normPhone", "maskPii"], file);
module.exports = { loadBlock, loadQrAnalyze, loadPhoneNorm };
