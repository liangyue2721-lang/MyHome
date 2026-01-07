from playwright.sync_api import sync_playwright
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        # 1. Mock Captcha (Disable it)
        page.route("**/dev-api/captchaImage", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"captchaEnabled": false, "code": 200, "msg": "success"}'
        ))

        # 2. Mock Login
        page.route("**/dev-api/login", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"msg": "操作成功", "code": 200, "token": "dummy_token"}'
        ))

        # 3. Mock User Info (Permissions)
        page.route("**/dev-api/getInfo", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"msg": "操作成功", "code": 200, "permissions": ["*:*:*"], "roles": ["admin"], "user": {"userName": "admin", "nickName": "Admin"}}'
        ))

        # 4. Mock Routers (Navigation)
        # We need to make sure the Watch Stock page is in the router
        page.route("**/dev-api/getRouters", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='''{
                "msg": "操作成功",
                "code": 200,
                "data": [
                    {
                        "name": "Stock",
                        "path": "/stock",
                        "hidden": false,
                        "redirect": "noRedirect",
                        "component": "Layout",
                        "alwaysShow": true,
                        "meta": {"title": "股票管理", "icon": "stock", "noCache": false},
                        "children": [
                            {
                                "name": "WatchStock",
                                "path": "watch_stock",
                                "hidden": false,
                                "component": "stock/watch_stock/index",
                                "meta": {"title": "股票监控", "icon": "eye", "noCache": false}
                            }
                        ]
                    }
                ]
            }'''
        ))

        # 5. Mock Watch Stock List (Main Table)
        page.route("**/dev-api/stock/watch_stock/list*", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"total": 0, "rows": [], "code": 200, "msg": "查询成功"}'
        ))

        # 6. Mock Rankings (The New Feature)
        ranking_data = '''{
            "code": 200,
            "msg": "操作成功",
            "data": [
                {"stockCode": "000001", "stockName": "平安银行", "currentValue": 10.5, "prevValue": 10.0},
                {"stockCode": "600519", "stockName": "贵州茅台", "currentValue": 1800.0, "prevValue": 1750.0}
            ]
        }'''
        page.route("**/dev-api/stock/kline/ranking/WEEKLY_GAIN", lambda route: route.fulfill(
            status=200, content_type="application/json", body=ranking_data
        ))

        loss_data = '''{
             "code": 200,
            "msg": "操作成功",
            "data": [
                {"stockCode": "000002", "stockName": "万科A", "currentValue": 8.5, "prevValue": 9.0}
            ]
        }'''
        page.route("**/dev-api/stock/kline/ranking/WEEKLY_LOSS", lambda route: route.fulfill(
            status=200, content_type="application/json", body=loss_data
        ))

        try:
            # Go to Login
            print("Navigating to login...")
            page.goto("http://localhost:1024/login")

            # Wait for captcha mock to take effect and code input to disappear
            # We might need to wait a split second for the created() hook to fire getCodeImg
            page.wait_for_timeout(1000)

            print("Filling credentials...")
            page.fill('input[type="text"]', "admin")
            page.fill('input[type="password"]', "admin123")

            # Check if captcha input is still there
            if page.locator('input[placeholder="驗證碼"]').is_visible():
                print("Captcha field still visible, forcing fill...")
                page.fill('input[placeholder="驗證碼"]', "8888")

            print("Clicking login...")
            page.click(".login-btn")

            # Wait for navigation
            print("Waiting for dashboard/redirect...")
            page.wait_for_url("**/index", timeout=5000) # Usually goes to index first

            print("Navigating to Watch Stock page...")
            page.goto("http://localhost:1024/stock/watch_stock")

            # Wait for the new elements
            print("Verifying new elements...")
            page.wait_for_selector("text=本周涨幅榜", timeout=10000)
            page.wait_for_selector("text=本周跌幅榜", timeout=10000)

            # Verify data presence
            content = page.content()
            if "平安银行" in content and "万科A" in content:
                print("SUCCESS: Found ranking data in the page.")
            else:
                print("FAILURE: Ranking data not found.")
                print(content)

            page.screenshot(path="verify_success.png")
            print("Verification Complete. Screenshot saved to verify_success.png")

        except Exception as e:
            print(f"Error: {e}")
            page.screenshot(path="verify_error.png")
            raise e
        finally:
            browser.close()

if __name__ == "__main__":
    run()
