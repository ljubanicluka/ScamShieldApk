const fs=require('fs'),assert=require('assert'),html=fs.readFileSync('web/scamshield-app.html','utf8');
const m=html.match(/const DEMO_HANDLES=(\[[\s\S]*?\]);\s*const DEMO_USERS=/);assert(m,'handle list missing');
const h=JSON.parse(m[1]);assert.strictEqual(h.length,124);assert.strictEqual(new Set(h).size,124);
assert(html.includes('for(let i=0;i<467;i++)'));assert(html.includes('Math.floor(rnd()*90)'));
assert(html.includes('net>=250')&&html.includes('Auto Verified'));assert(html.includes('ADMIN VERIFIED'));
assert(html.includes('data-a=demoReport'));assert(html.includes('comments.map'));
console.log('Community seed checks passed: 124 unique accounts, 467 posts and separate verification states.');