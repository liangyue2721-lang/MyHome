#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Playwright 浏览器模拟访问 trends2 接口
"""

import os
import json
import re
import time
import logging
from playwright.sync_api import sync_playwright

# ====== 日志 =======
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("trends-fetcher")

# ====== JSONP 解析 ======
def parse_jsonp(text: str):
    m = re.match(r"^\s*\w+\s*\((.*)\)\s*;?\s*$", text, flags=re.S)
    if m:
        return json.loads(m.group(1))
    return None

# ====== 主函数 ======
def fetch_trends(secid="1.601138", ndays=5):
    url = (
        "https://push2his.eastmoney.com/api/qt/stock/trends2/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f17&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58&"
        f"ut=fa5fd1943c7b386f172d6893dbfba10b&"
        f"secid={secid}&ndays={ndays}&iscr=0&iscca=0&"
        f"cb=jsonp{int(time.time()*1000)}"
    )

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context(
            java_script_enabled=True,
            locale="zh-CN",
            viewport={"width": 1366, "height": 768},
        )

        page = context.new_page()

        # 1) 访问东财主页建立 session + cookie
        logger.info("访问东财页面建立 session")
        page.goto("https://quote.eastmoney.com", timeout=15000)
        time.sleep(1.5)

        # 2) 在浏览器上下文执行 fetch
        logger.info(f"开始浏览器内部 fetch(): {url}")
        resp_text = page.evaluate(
            """async (url) => {
                const r = await fetch(url, {
                    credentials: "include",
                    cache: "no-cache",
                });
                return await r.text();
            }""",
            url,
        )

        browser.close()

    # 3) JSONP 解析
    parsed = parse_jsonp(resp_text)
    if not parsed:
        logger.error("解析失败，原始响应：\n%s", resp_text[:300])
        return None

    # 4) 输出结果
    logger.info("解析成功")
    return parsed


# ===============================
# 入口
# ===============================
if __name__ == "__main__":
    result = fetch_trends("1.601138", 5)

    if result:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print("None")
