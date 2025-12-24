
import { test, expect } from '@playwright/test';

test('Verify Server Monitor Page', async ({ page }) => {
  // Mock API responses
  await page.route('**/dev-api/captchaImage', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ captchaEnabled: false, code: 200 })
    });
  });

  await page.route('**/dev-api/login', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'mock-token', code: 200 })
    });
  });

  await page.route('**/dev-api/getInfo', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        user: {
          userName: 'admin',
          nickName: 'Admin',
          avatar: ''
        },
        roles: ['admin'],
        permissions: ['*:*:*'],
        code: 200
      })
    });
  });

  await page.route('**/dev-api/getRouters', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        data: [{
            path: '/monitor',
            component: 'Layout',
            children: [{
                path: 'server',
                component: 'monitor/server/index',
                meta: { title: 'Service Monitor' }
            }]
        }],
        code: 200
      })
    });
  });

  // Mock /monitor/server endpoint (Server Info)
  await page.route('**/dev-api/monitor/server', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        data: {
          cpu: { cpuNum: 4, used: 25.5, sys: 10, free: 64.5 },
          mem: { total: 16384, used: 8192, free: 8192, usage: 50 },
          jvm: { total: 4096, used: 2048, free: 2048, usage: 50 },
          sys: { computerName: 'TestServer', osName: 'Linux', computerIp: '127.0.0.1', osArch: 'amd64' },
          sysFiles: []
        }
      })
    });
  });

  // Mock /quartz/runtime/detail endpoint (Queue)
  await page.route('**/dev-api/quartz/runtime/detail', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        data: [
          { jobId: 1, jobName: 'TestJob', jobGroup: 'DEFAULT', executionId: 'exec-1', status: 'RUNNING', nodeId: '127.0.0.1', scheduledTime: Date.now(), enqueueTime: Date.now(), startTime: Date.now(), retryCount: 0, maxRetry: 3 },
          { jobId: 2, jobName: 'FailedJob', jobGroup: 'DEFAULT', executionId: 'exec-2', status: 'FAILED', nodeId: '127.0.0.1', scheduledTime: Date.now(), enqueueTime: Date.now(), startTime: Date.now(), retryCount: 3, maxRetry: 3 }
        ]
      })
    });
  });

  // Mock Cluster Info (Redis Mode)
  await page.route('**/dev-api/monitor/server/clusterThreadPoolRedis', async route => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: {} }) });
  });
  await page.route('**/dev-api/monitor/server/clusterServerRedis', async route => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, data: {} }) });
  });


  // Login
  await page.goto('http://localhost:1024/login');
  await page.fill('input[placeholder="帳號"]', 'admin');
  await page.fill('input[placeholder="密碼"]', 'admin123');
  await page.click('button:has-text("登 錄")');
  await page.waitForURL('**/index');

  // Navigate to Server Monitor
  await page.goto('http://localhost:1024/monitor/server');

  // Verify Elements
  await expect(page.locator('text=CPU').first()).toBeVisible();
  await expect(page.locator('text=任务队列实时监控')).toBeVisible();
  await expect(page.locator('text=TestJob')).toBeVisible();
  await expect(page.locator('text=FailedJob')).toBeVisible();

  // Verify No Data Handling (Mocking a 404/Empty response for server info)
  // Re-route to simulate empty data
    await page.route('**/dev-api/monitor/server', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        data: {} // Empty data
      })
    });
  });

  // Trigger refresh (assuming there's a refresh button or we reload)
  await page.reload();
  await expect(page.locator('text=--').first()).toBeVisible();

  // Take Screenshot
  await page.screenshot({ path: 'server_monitor_verified.png', fullPage: true });
});
