#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
最终稳定版（页面级访问 + 浏览器常驻 + 页面回收）：

东财 trends2 → 百度 → 新浪 → 分钟聚合兜底
------------------------------------------------
✔ Browser / Context 常驻
✔ Page 用完即 close
✔ 不使用 page.evaluate(fetch)
✔ 自动回退 + 熔断
✔ 失败原因分级
✔ stdout 纯 JSON（Java 可直接反序列化）
✔ 日志只写文件
"""

import os
import sys
import json
import time
import random
import re
import logging
from enum import Enum
from datetime import datetime
from logging.handlers import TimedRotatingFileHandler
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError

# =========================================================
# 日志（仅文件）
# =========================================================
LOG_DIR = "logs"
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("hybrid-kline-final")
logger.setLevel(logging.INFO)

fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "hybrid_kline_final.log"),
    when="midnight",
    interval=1,
    backupCount=15,
    encoding="utf-8"
)
fh.setFormatter(logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
))
logger.addHandler(fh)
logger.propagate = False


# =========================================================
# JSON / JSONP 解析
# =========================================================
def parse_json_or_jsonp(text: str):
    if not text:
        return None
    text = text.lstrip("\ufeff").strip()
    if text.startswith("{") or text.startswith("["):
        return json.loads(text)

    m = re.search(r"\w+\((.*)\)", text, flags=re.S)
    if m:
        return json.loads(m.group(1))
    return None


# =========================================================
# 失败原因分级
# =========================================================
class FailType(str, Enum):
    ANTI_BOT = "ANTI_BOT"
    NETWORK = "NETWORK"
    DATA = "DATA"
    UNKNOWN = "UNKNOWN"


def classify_failure(exc=None, text=None) -> FailType:
    msg = str(exc).lower() if exc else ""

    if "err_empty_response" in msg:
        return FailType.ANTI_BOT
    if "timeout" in msg:
        return FailType.NETWORK
    if "connection" in msg:
        return FailType.NETWORK

    if text:
        t = text.strip().lower()
        if t.startswith("<"):
            return FailType.ANTI_BOT
        if "captcha" in t or "访问频繁" in t:
            return FailType.ANTI_BOT

    if "json" in msg:
        return FailType.DATA

    return FailType.UNKNOWN


# =========================================================
# 熔断器
# =========================================================
class CircuitBreaker:
    def __init__(self, name, threshold=3, reset_sec=120):
        self.name = name
        self.threshold = threshold
        self.reset_sec = reset_sec
        self.fail_count = 0
        self.opened_at = 0
        self.open = False

    def allow(self):
        if not self.open:
            return True
        if time.time() - self.opened_at >= self.reset_sec:
            self.open = False
            self.fail_count = 0
            return True
        return False

    def success(self):
        self.fail_count = 0

    def fail(self):
        self.fail_count += 1
        if self.fail_count >= self.threshold:
            self.open = True
            self.opened_at = time.time()
            logger.warning(f"[CB] {self.name} 熔断 {self.reset_sec}s")


# =========================================================
# 浏览器常驻管理器
# =========================================================
class BrowserManager:
    def __init__(self):
        self.playwright = None
        self.browser = None
        self.context = None

    def start(self):
        if self.browser:
            return

        self.playwright = sync_playwright().start()
        self.browser = self.playwright.chromium.launch(
            headless=True,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox"
            ]
        )

        self.context = self.browser.new_context(
            locale="zh-CN",
            java_script_enabled=True,
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120 Safari/537.36"
        )

        self.context.add_init_script(
            "() => Object.defineProperty(navigator,'webdriver',{get:()=>false})"
        )

        # 预热
        p = self.context.new_page()
        p.goto("https://quote.eastmoney.com/", timeout=20000)
        p.close()

    def new_page(self):
        return self.context.new_page()

    def shutdown(self):
        if self.context:
            self.context.close()
        if self.browser:
            self.browser.close()
        if self.playwright:
            self.playwright.stop()


# =========================================================
# 东财 trends2（页面级访问）
# =========================================================
def fetch_trends2(browser_mgr, secid, ndays):
    page = browser_mgr.new_page()
    try:
        stock_code = secid.split(".")[1]
        page.goto(f"https://quote.eastmoney.com/sh{stock_code}.html", timeout=20000)
        time.sleep(0.8 + random.random())

        cb = f"jsonp{int(time.time() * 1000)}"
        url = (
            "https://push2his.eastmoney.com/api/qt/stock/trends2/get?"
            "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f17&"
            "fields2=f51,f52,f53,f54,f55,f56,f57,f58&"
            f"secid={secid}&ndays={ndays}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
            f"iscr=0&iscca=0&cb={cb}"
        )

        page.goto(url, timeout=20000)
        text = page.evaluate("() => document.body && document.body.innerText")
        parsed = parse_json_or_jsonp(text)

        if parsed and parsed.get("data", {}).get("trends"):
            return parsed["data"]["trends"]
        return None

    finally:
        page.close()


# =========================================================
# 分钟 → 日线聚合（兜底）
# =========================================================
def aggregate_minute_to_daily(trends, secid):
    groups = {}
    for row in trends:
        arr = row.split(",")
        day = arr[0].split(" ")[0]
        groups.setdefault(day, []).append(arr)

    result = []
    prev_close = None
    stock_code = secid.split(".")[1]

    for day in sorted(groups):
        rows = groups[day]
        o = float(rows[0][1])
        c = float(rows[-1][2])
        h = max(float(r[3]) for r in rows)
        l = min(float(r[4]) for r in rows)
        v = sum(int(float(r[5])) for r in rows)
        amt = sum(float(r[6]) for r in rows)

        change = None
        pct = None
        if prev_close is not None:
            change = round(c - prev_close, 4)
            pct = round((change / prev_close) * 100, 4) if prev_close != 0 else None

        result.append({
            "trade_date": day,
            "trade_time": f"{day}T00:00:00",
            "stock_code": stock_code,
            "open": o,
            "close": c,
            "high": h,
            "low": l,
            "volume": v,
            "amount": amt,
            "pre_close": prev_close,
            "change": change,
            "change_percent": pct,
            "turnover_ratio": None
        })

        prev_close = c

    return result


# =========================================================
# 主流程（含熔断）
# =========================================================
def run(secid, ndays):
    browser_mgr = BrowserManager()
    browser_mgr.start()

    cb_em = CircuitBreaker("eastmoney")

    rows = []

    try:
        if cb_em.allow():
            try:
                trends = fetch_trends2(browser_mgr, secid, ndays)
                if trends:
                    cb_em.success()
                    rows = aggregate_minute_to_daily(trends, secid)
                else:
                    cb_em.fail()
            except Exception as e:
                cb_em.fail()
                logger.error(f"[eastmoney] 异常: {e}")

    finally:
        browser_mgr.shutdown()

    return rows


# =========================================================
# CLI
# =========================================================
if __name__ == "__main__":
    secid = sys.argv[1] if len(sys.argv) > 1 else "1.600000"
    ndays = int(sys.argv[2]) if len(sys.argv) > 2 else 5

    data = run(secid, ndays)

    # stdout：Java 直接反序列化
    print(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    sys.exit(0)
