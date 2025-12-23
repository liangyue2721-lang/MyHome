from playwright.sync_api import sync_playwright
import time

def verify_frontend():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        # Mock the captcha request to bypass login
        page.route("**/captchaImage", lambda route: route.fulfill(
            status=200,
            body='{"captchaEnabled": false, "code": 200, "uuid": "test-uuid"}'
        ))

        # Mock login request
        page.route("**/login", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "token": "test-token"}'
        ))

        # Mock getInfo request (user info)
        page.route("**/getInfo", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "user": {"userName": "admin"}, "roles": ["admin"], "permissions": ["*:*:*"]}'
        ))

        # Mock getRouters (dynamic routing)
        page.route("**/getRouters", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "data": []}'
        ))

         # Mock status-summary replacement APIs for Dashboard
        page.route("**/quartz/runtime/list?status=WAITING&pageSize=1", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "total": 15, "rows": []}'
        ))
        page.route("**/quartz/runtime/list?status=RUNNING&pageSize=1", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "total": 5, "rows": []}'
        ))
        page.route("**/monitor/jobLog/list?pageSize=1", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "total": 100, "rows": []}'
        ))

        # Mock other dashboard charts to prevent errors
        page.route("**/pieChart/**", lambda route: route.fulfill(
             status=200,
             body='[]'
        ))

        # Mock other APIs to prevent errors
        page.route("**/system/user/list", lambda route: route.fulfill(status=200, body='{"code":200, "rows":[]}'))


         # Mock server monitor list
        page.route("**/monitor/server", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "data": {}}'
        ))
        page.route("**/monitor/server/cluster/thread-pool/redis", lambda route: route.fulfill(
             status=200,
             body='{"code": 200, "data": {}}'
        ))
        page.route("**/monitor/server/cluster/server-info/redis", lambda route: route.fulfill(
             status=200,
             body='{"code": 200, "data": {}}'
        ))

        # Mock queue details for Server Monitor
        page.route("**/quartz/runtime/list", lambda route: route.fulfill(
            status=200,
            body='{"code": 200, "rows": [{"jobGroup": "DEFAULT", "jobId": 1, "jobName": "TestJob", "nodeId": "127.0.0.1", "status": "WAITING", "executionId": "exec-123", "enqueueTime": "2023-10-27 10:00:00", "retryCount": 0}]}'
        ))


        # 1. Login and go to Dashboard
        # Note: Using port 1024 as per logs and navigating to the hash route
        page.goto("http://localhost:1024/#/login")

        # Wait for the input to be available
        page.wait_for_selector('input[type="text"]')

        page.fill('input[type="text"]', "admin")
        page.fill('input[type="password"]', "admin123")
        page.click('.login-btn')

        # Wait for navigation to dashboard - just wait for it to be visible
        time.sleep(5)

        # Verify Dashboard
        page.goto("http://localhost:1024/#/index")
        time.sleep(5) # Allow animations/charts to settle
        page.screenshot(path="/home/jules/verification/dashboard.png")
        print("Dashboard screenshot taken")

        # Verify Server Monitor
        page.goto("http://localhost:1024/#/monitor/server")
        time.sleep(5)
        page.screenshot(path="/home/jules/verification/server_monitor.png")
        print("Server Monitor screenshot taken")

        browser.close()

if __name__ == "__main__":
    verify_frontend()
