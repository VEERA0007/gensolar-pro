import { chromium } from 'playwright';
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, resolve, normalize, sep } from 'node:path';

const root = resolve('gensolar-android-project/gensolar-android/app/src/main/assets');
const mime = { '.html':'text/html; charset=utf-8', '.jpg':'image/jpeg', '.png':'image/png', '.css':'text/css' };
const server = createServer(async (req, res) => {
  try {
    const pathname = decodeURIComponent(new URL(req.url, 'http://127.0.0.1').pathname);
    const relative = normalize(pathname === '/' ? '/index.html' : pathname);
    const file = resolve(root, '.' + relative);
    if (!file.startsWith(root + sep) && file !== resolve(root, 'index.html')) {
      res.writeHead(403); res.end('Forbidden'); return;
    }
    const body = await readFile(file);
    res.writeHead(200, { 'content-type': mime[extname(file)] || 'application/octet-stream' });
    res.end(body);
  } catch {
    res.writeHead(404); res.end('Not found');
  }
});
await new Promise(resolveListen => server.listen(4179, '127.0.0.1', resolveListen));
const browser = await chromium.launch({ headless: true });
const failures = [];
const check = (condition, message) => { if (!condition) failures.push(message); else console.log('PASS:', message); };
try {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
  const errors = [];
  const consoleErrors = [];
  page.on('pageerror', e => errors.push(e.stack || e.message));
  page.on('console', msg => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
  await page.goto('http://127.0.0.1:4179/', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2200);
  check(await page.locator('#authGate').isVisible(), 'Welcome screen is visible on a mobile-sized viewport');
  console.log('Startup JS errors:', JSON.stringify(errors));
  console.log('Console errors:', JSON.stringify(consoleErrors));

  const photoStates = await page.locator('.hero .hero-bg').evaluateAll(els =>
    els.map(el => ({ src: el.getAttribute('src'), loaded: el.complete && el.naturalWidth > 0, width: el.naturalWidth }))
  );
  check(photoStates.length === 1 && photoStates.every(x => x.loaded)
    && true,
    'Bundled welcome-screen solar images load: ' + JSON.stringify(photoStates));

  await page.getByRole('button', { name: /login/i }).click();
  check(await page.locator('#loginForm').isVisible(), 'Login button opens the real OTP login sheet');
  await page.locator('#sendOtpButton').click();
  check((await page.locator('#authMessage').innerText()).includes('valid 10-digit'),
    'OTP form validates an empty/invalid mobile number instead of silently doing nothing');

  await page.locator('.lback').first().click();
  await page.waitForTimeout(150);
  await page.locator('.feats .feat').first().click();
  await page.waitForTimeout(100);
  check(await page.locator('#featureLoginModal').isVisible(), 'Solar Solutions shows a branded login-required popup');
  check((await page.locator('#featureLoginMessage').innerText()).includes('Solar Solutions'), 'Feature popup names the selected service');
  await page.locator('#featureCancel').click();
  check(await page.locator('#featureLoginModal').isHidden(), 'Cancel closes the feature popup without leaving the welcome screen');
  // Exercise the guest estimator separately; feature cards are intentionally login-gated.
  await page.evaluate(() => { window.exploreGuest(); });
  await page.waitForTimeout(200);
  check(await page.locator('#app').isVisible(), 'Guest estimator workspace can still be opened');
  check(await page.locator('#view-est').isVisible(), 'Estimator view is shown for guest exploration');
  check(await page.locator('#amount').count() === 1 && await page.locator('#state').count() === 1,
    'Estimator has usage and state inputs');
  check(await page.locator('#out').count() === 1, 'Estimator results container is present');
  await page.locator('#amount').fill('45000');
  await page.waitForTimeout(100);
  check(await page.locator('#out').isVisible(), 'Estimator produces a visible result after changing the monthly usage input');
  check((await page.locator('#tSize').innerText()).trim().length > 0, 'Estimator calculates and displays a system-size result');

  check(errors.length === 0, 'No uncaught JavaScript errors: ' + (errors.join(' | ') || 'none'));
} finally {
  await browser.close();
  await new Promise(resolveClose => server.close(resolveClose));
}
if (failures.length) {
  console.error('\nFAILURES:\n- ' + failures.join('\n- '));
  process.exit(1);
}
console.log('\nAll GenSolar WebView smoke checks passed.');
