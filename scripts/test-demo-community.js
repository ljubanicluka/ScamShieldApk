'use strict';
const fs = require('node:fs');
const html = fs.readFileSync('app/src/main/assets/index.html', 'utf8');
function assert(ok, message) { if (!ok) throw new Error(message); console.log(`PASS ${message}`); }
assert(/const DEMO_HANDLES=/.test(html), 'community has an explicit fictional handle list');
const handleMatch = html.match(/const DEMO_HANDLES=(\[[\s\S]*?\]);\s*const DEMO_USERS=/);
assert(!!handleMatch, 'community handle list can be parsed');
const handles = JSON.parse(handleMatch[1]);
assert(handles.length === 124 && new Set(handles).size === 124, 'community seeds exactly 124 unique synthetic demo accounts');
assert(/for\(let i=0;i<467;i\+\+\)/.test(html), 'community creates exactly 467 synthetic demo posts');
const levelBody = html.match(/function demoLevel\(net\)\{([^}]+)\}/)?.[1];
assert(!!levelBody, 'level calculation exists');
const level = new Function('net', levelBody);
for (const [net, expected] of [[39,0],[40,1],[79,1],[80,2],[100,2],[120,3],[160,4],[200,5]]) {
  assert(level(net) === expected, `net ${net} maps to Level ${expected}`);
}
assert(/function demoNet\(u\)\{const x=demoTotals\(u\);return x\.up-x\.down\}/.test(html), 'levels use total upvotes minus total downvotes');
assert(/glow=u\.glow\|\|demoState\(\)\.glow\.includes\(u\.id\)/.test(html), 'administrator glow is independently stored from voting level');
assert(/d\.role==='admin'\|\|d\.role==='owner'\|\|\(!u\.hidden&&d\.role!=='moderator'\)/.test(html), 'hidden demo accounts are visible only to Admin and Owner in the account list');
assert(/demo:true/.test(html) && /reference example, not a user report/i.test(html), 'internal sample data is distinguished from user reports in verdict copy');
assert(!/DEMO MODE|DEMO CONSOLE|<span class=demoTag>DEMO<\\/span>/i.test(html), 'community UI does not display DEMO labels');
assert(/data-a=adminRemoveUser/.test(html) && /data-a=adminRemovePost/.test(html), 'demo Admin console can remove accounts and posts');
assert(/data-a=adminGlow/.test(html), 'demo Admin console can grant or revoke glow on any listed account');
assert(/data-a=movePage/.test(html) && /data-a=mainPage/.test(html), 'six-page order and navigation controls exist');
console.log('OK: ScamShield demo-community regression checks passed');
