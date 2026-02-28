
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();

  try {
    await page.goto('http://localhost:1024/#/system/user', { timeout: 60000 });
    await page.waitForTimeout(2000); // Wait for 2 seconds to allow rendering
    await page.screenshot({ path: 'debug_screenshot.png' });
    console.log('Debug screenshot taken.');
  } catch (error) {
    console.error('An error occurred during verification:', error);
    await page.screenshot({ path: 'error_screenshot.png' });
  } finally {
    await browser.close();
  }
})();
