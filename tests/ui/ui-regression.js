// Headless-Chrome UI regression tests (touch emulation). Run: node tests/ui/ui-regression.js
// These exercise the real HTML in a mobile viewport. They do NOT replace testing on a physical Android device.
const path = require('path'), fs = require('fs');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/opt/npm-tools/node_modules/playwright')); }
const root = path.resolve(__dirname, '../..');
const html = path.join(root, 'app/src/main/assets/index.html');
let fails = 0; const ok = (c, m) => { console.log((c ? 'PASS ' : 'FAIL ') + m); if (!c) fails++; };
const todo = (c, m) => console.log(c ? 'PASS ' + m : 'TODO (not implemented yet) ' + m);
const PAGES = ['tools', 'home', 'feed', 'link', 'help', 'family'];

(async () => {
  const browser = await chromium.launch({ args: ['--no-sandbox'] });
  const ctx = await browser.newContext({ viewport: { width: 390, height: 780 }, hasTouch: true, isMobile: true });
  const page = await ctx.newPage();
  const errors = []; page.on('pageerror', e => errors.push(String(e)));
  await page.addInitScript(() => { try { if (!localStorage.getItem('ss')) localStorage.setItem('ss', JSON.stringify({ done: 1 })); } catch (e) {} });
  await page.goto('file://' + html);
  const cdp = await ctx.newCDPSession(page);
  const swipe = async (x1, y1, x2, y2) => {
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: x1, y: y1 }] });
    for (let i = 1; i <= 8; i++) await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x1 + (x2 - x1) * i / 8, y: y1 + (y2 - y1) * i / 8 }] });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
    await page.waitForTimeout(450);
  };
  const cur = () => page.evaluate(() => pg);
  const goto = async id => { await page.evaluate(id => go(id), id); await page.waitForTimeout(100); };

  // Capture the six main screens in two themes and three language modes for visual review.
  const screenshotDir = path.join(root, 'ui-screenshots');
  fs.mkdirSync(screenshotDir, { recursive: true });
  const languageModes = [
    { id: 'sr-lat', lang: 'sr', cyr: 0 },
    { id: 'sr-cyr', lang: 'sr', cyr: 1 },
    { id: 'en', lang: 'en', cyr: 0 }
  ];
  for (const theme of ['light', 'dark']) {
    for (const language of languageModes) {
      for (const id of PAGES) {
        await page.evaluate(({ id, language, theme }) => {
          S.done = 1; S.lang = language.lang; S.cyr = language.cyr; S.th = theme; pg = id; R(); window.scrollTo(0, 0);
        }, { id, language, theme });
        await page.waitForTimeout(80);
        await page.screenshot({
          path: path.join(screenshotDir, id + '-' + theme + '-' + language.id + '-390x780.png'),
          fullPage: true,
          animations: 'disabled'
        });
      }
    }
  }
  await page.evaluate(() => { S.lang = 'sr'; S.cyr = 0; S.th = ''; R(); });
  const shell = await page.evaluate(() => ({
    nav: document.querySelectorAll('#bottomNav button').length,
    header: !!document.querySelector('header.appHeader'),
    icons: document.querySelectorAll('#bottomNav svg').length
  }));
  ok(shell.nav === 6, 'redesigned shell retains all six navigation items');
  ok(shell.header, 'branded ScamShield app header is present');
  ok(shell.icons === 6, 'all six navigation items have vector icons');
  const sourceCopy = fs.readFileSync(path.join(root, 'web/scamshield-app.html'));
  const assetCopy = fs.readFileSync(html);
  ok(sourceCopy.equals(assetCopy), 'editable UI and packaged Android asset are byte-identical');

  // All six principal screens should have the redesigned content hierarchy.
  for (const id of PAGES) {
    await goto(id);
    const check = await page.evaluate(() => ({
      pageIntro: !!document.querySelector('#app .pageIntro'),
      homeHero: !!document.querySelector('#app .homeHero')
    }));
    ok(id === 'home' ? check.homeHero : check.pageIntro, id + ' page has the redesigned section header');
  }

  // 1. swipe from every page, from the empty area below content and from the middle of content
  for (const y of [650, 300]) {
    for (let i = 0; i < 6; i++) {
      await goto(PAGES[i]);
      if (i < 5) { await swipe(330, y, 60, y); ok(await cur() === PAGES[i + 1], `swipe left at y=${y} from ${PAGES[i]} -> ${PAGES[i + 1]} (got ${await cur()})`); }
      await goto(PAGES[i]);
      // On Family the new layout places an input at y=650; swipe guards should prevent
      // gestures from hijacking the input, so test the adjacent safe area instead.
      const swipeY = PAGES[i] === 'family' && y === 650 ? 600 : (PAGES[i] === 'feed' && y === 300 ? 650 : y);
      if (i > 0) { await swipe(60, swipeY, 330, swipeY); ok(await cur() === PAGES[i - 1], `swipe right at y=${swipeY} from ${PAGES[i]} -> ${PAGES[i - 1]} (got ${await cur()})`); }
    }
  }
  // 2. language switch updates immediately incl. QR page
  await goto('link');
  const before = await page.locator('#app h2').first().innerText();
  await page.click('[data-a=lang]'); await page.waitForTimeout(100);
  const after = await page.locator('#app h2').first().innerText();
  ok(before !== after, `language toggle changes QR heading immediately ("${before}" -> "${after}")`);
  await page.click('[data-a=lang]');
  // 3. back stack
  await goto('feed'); await goto('link');
  await page.goBack(); await page.waitForTimeout(150);
  ok(await cur() === 'feed', `Back from QR returns to previous page (got ${await cur()})`);

  // 5. finger-following, vertical scroll, guarded controls, settings panel, swipe toggle
  await goto('home');
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: 300, y: 600 }] });
  for (let i = 1; i <= 5; i++) await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: 300 - i * 20, y: 600 }] });
  const tf = await page.evaluate(() => document.querySelector('#app').style.transform);
  ok(/translateX\(-?\d+/.test(tf) && tf !== 'translateX(0px)', `page follows the finger mid-drag (transform=${tf})`);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] }); await page.waitForTimeout(400);
  await goto('home'); await swipe(200, 650, 200, 250);
  ok(await cur() === 'home', 'vertical drag does not change page');
  await goto('link');
  const li = await page.locator('#li').boundingBox();
  await swipe(li.x + li.width - 10, li.y + 10, li.x + 10, li.y + 10);
  ok(await cur() === 'link', 'swiping inside a text input does not change page');
  await page.click('[data-a=profile]'); await page.waitForTimeout(150);
  ok(await page.locator('.modalShade').count() === 1, 'settings panel opens');
  await page.locator('.modalPanel h2').click(); await page.waitForTimeout(100);
  ok(await page.locator('.modalShade').count() === 1, 'tapping inside the settings panel does not close it');
  await swipe(300, 400, 40, 400);
  ok(await cur() === 'link', 'no page swipe while settings panel is open');
  await page.goBack(); await page.waitForTimeout(200);
  ok(await page.locator('.modalShade').count() === 0 && await cur() === 'link', 'Back closes the settings panel first');
  await page.evaluate(() => { S.swipeOff = 1; sv(); });
  await swipe(330, 650, 60, 650);
  ok(await cur() === 'link', 'swipe can be disabled in settings');
  await page.evaluate(() => { S.swipeOff = 0; sv(); });
  // 6. QR page in Serbian Cyrillic: no Latin words except protected names
  await page.evaluate(() => { S.lang = 'sr'; S.cyr = 1; R(); });
  const latin = await page.evaluate(() => [...document.querySelector('main').innerText.matchAll(/[A-Za-zČŠŽĆĐčšžćđ]{3,}/g)].map(m => m[0]).filter(w => !/^(Android|DEMO|MTS|QR)$/.test(w)));
  ok(latin.length === 0, `QR page fully Cyrillic (leftover Latin: ${latin.join(',') || 'none'})`);
  ok(!/не отвара ништа/.test(await page.locator('main').innerText()), 'awkward "(ne otvara ništa)" filler removed from QR title');

  // Stage 3: tutorial, profile setup, replay and appearance preferences.
  await page.evaluate(() => { S.done = 0; S.ob = 0; R(); });
  ok(await page.locator('.onboardWrap').count() === 1, 'first-run tutorial is shown');
  for (let i = 0; i < 3; i++) { await page.click('[data-a=obn]'); await page.waitForTimeout(40); }
  ok(await page.locator('#onAge').count() === 1, 'onboarding reaches profile setup after tutorial');
  await page.fill('#onAge', '17');
  await page.fill('#onName', 'Test User');
  await page.fill('#onUsername', 'test_user_17');
  await page.selectOption('#onSex', 'prefer-not');
  await page.selectOption('#onAccent', 'teal');
  await page.click('[data-a=finishOnboarding]');
  await page.waitForTimeout(100);
  const profileSaved = await page.evaluate(() => S.done === 1 && S.profile.username === 'test_user_17' && S.accent === 'teal');
  ok(profileSaved, 'profile setup saves required fields locally');
  await page.click('[data-a=profile]'); await page.waitForTimeout(100);
  await page.selectOption('#accent', 'violet');
  await page.selectOption('#roundness', 'extra');
  await page.click('[data-a=saveProfile]'); await page.waitForTimeout(100);
  const appearance = await page.evaluate(() => ({ accent: document.documentElement.dataset.accent, roundness: document.documentElement.dataset.roundness }));
  ok(appearance.accent === 'violet' && appearance.roundness === 'extra', 'custom accent and corner roundness apply immediately');
  await page.click('[data-a=profile]'); await page.waitForTimeout(80);
  await page.click('[data-a=replayTutorial]'); await page.waitForTimeout(80);
  ok(await page.locator('.onboardWrap').count() === 1, 'tutorial can be replayed from settings');
  await page.click('[data-a=skipTutorial]'); await page.waitForTimeout(80);
  await page.fill('#onAge', '17'); await page.fill('#onName', 'Test User'); await page.fill('#onUsername', 'test_user_17'); await page.selectOption('#onSex', 'prefer-not');
  await page.click('[data-a=finishOnboarding]'); await page.waitForTimeout(80);

  // Stage 3: rules, profile tabs, guest browsing and account boundary.
  await page.click('[data-a=profile]'); await page.waitForTimeout(80);
  await page.click('[data-a=openRules]'); await page.waitForTimeout(80);
  ok(await page.locator('.ruleItem').count() === 200, 'rulebook contains 200 numbered entries');
  await page.fill('#ruleSearch', 'PIN'); await page.waitForTimeout(80);
  ok(await page.locator('.ruleItem').count() > 0 && await page.locator('.ruleItem').count() < 200, 'rulebook search filters the 200 rules');
  await page.click('[data-a=ruleFilter][data-v=all]'); await page.waitForTimeout(80);
  await page.click('[data-a=acceptRules]'); await page.waitForTimeout(80);
  ok(await page.evaluate(() => !!S.rulesAccepted), 'community rules acceptance is saved');
  await page.click('[data-a=profile]'); await page.waitForTimeout(80);
  await page.click('.modalPanel [data-a=openProfile]'); await page.waitForTimeout(80);
  ok(await page.locator('.profileTabs button').count() === 4, 'profile has Posts, Comments, Reposts and About tabs');
  await page.click('[data-a=profileTab][data-v=about]'); await page.waitForTimeout(80);
  await page.click('[data-a=editLocalProfile]'); await page.waitForTimeout(80);
  ok(await page.locator('#editBio').count() === 1, 'profile editor includes optional bio');
  await page.click('[data-a=saveLocalProfile]'); await page.waitForTimeout(80);
  await page.click('[data-a=openAccount]').catch(() => {}); await page.waitForTimeout(80);
  ok(await page.locator('#authEmail').count() === 1, 'account access page is available');
  ok(await page.locator('#authPassword').isDisabled(), 'password field stays disabled until a secure auth provider is connected');
  ok(await page.locator('.ruleItem').count() === 0, 'account access does not render stale rulebook entries');

  // 4. features that must exist
  const src = fs.readFileSync(html, 'utf8');
  ok(/RULEBOOK=RULE_CATEGORIES\.flatMap/.test(src), 'rulebook data source present');
  ok(/APP WALKTHROUGH|VODIČ KROZ APLIKACIJU/.test(src), 'interactive tutorial present in source');
  ok(/authSignIn/.test(src) && /authRegister/.test(src), 'account sign-in and registration adapter hooks present');
  console.log('page errors:', errors.length ? errors : 'none');
  await browser.close();
  console.log(fails ? `\n${fails} FAILED` : '\nALL PASSED');
  process.exit(fails ? 1 : 0);
})();
