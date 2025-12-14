#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
最终稳定版：东财 → 百度 → 新浪（日级聚合）
------------------------------------------------
1. Playwright 真实浏览器 + cookie 预热
2. 东财 trends2 优先
3. 百度分钟 K 兜底
4. 新浪快照最终兜底
5. 分钟级 → 日级聚合
6. stdout 仅输出 JSON
"""

import os
import sys
import re
import json
import time
import random
import logging
from datetime import datetime
from logging.handlers import TimedRotatingFileHandler
from playwright.sync_api import sync_playwright

# =========================================================
# 日志（仅文件）
# =========================================================

LOG_DIR = "logs"
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("hybrid-daily-fetch")
logger.setLevel(logging.INFO)

fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "hybrid_daily_fetch.log"),
    when="midnight", interval=1, backupCount=30, encoding="utf-8"
)
fh.setFormatter(logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
))
logger.addHandler(fh)
logger.propagate = False

# =========================================================
# 工具函数
# =========================================================

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_6) AppleWebKit/605.1.15 Version/15.5 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
]


def random_ua():
    return random.choice(UA_POOL)


def normalize_secid(code: str):
    if "." in code:
        return code
    if code.startswith(("83", "43")):
        return f"113.{code}"
    if code.startswith(("6", "5")):
        return f"1.{code}"
    if code.startswith(("0", "3")):
        return f"0.{code}"
    return f"0.{code}"


def parse_json_or_jsonp(text: str):
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except:
        pass
    m = re.search(r"\w+\((.*)\)", text, flags=re.S)
    if m:
        try:
            return json.loads(m.group(1))
        except:
            return None
    return None


# =========================================================
# 浏览器 Context
# =========================================================

def build_context(p):
    browser = p.chromium.launch(headless=True)
    ctx = browser.new_context(
        locale="zh-CN",
        user_agent=random_ua()
    )
    ctx.add_init_script(
        "() => Object.defineProperty(navigator,'webdriver',{get:()=>false})"
    )
    page = ctx.new_page()
    try:
        page.goto("https://quote.eastmoney.com/", timeout=15000)
        time.sleep(0.5 + random.random())
    except:
        pass
    page.close()
    return browser, ctx


# =========================================================
# 东财 trends2
# =========================================================

def fetch_trends_eastmoney(page, secid, ndays):
    cb = f"jsonp{int(time.time() * 1000)}"
    url = (
        "https://push2his.eastmoney.com/api/qt/stock/trends2/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f17&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58&"
        f"secid={secid}&ndays={ndays}&ut=fa5fd1943c7b386f172d6893db (?)"
        f"fa10b&iscr=0&iscca=0&cb={cb}"
    )
    page.goto(url, timeout=20000)
    text = page.evaluate("() => document.body.innerText || ''")
    if text.startswith("<"):
        return None
    parsed = parse_json_or_jsonp(text)
    if parsed and parsed.get("data", {}).get("trends"):
        return parsed["data"]["trends"]
    return None


# =========================================================
# 百度分钟 K
# =========================================================

def fetch_trends_baidu(page, secid):
    code = secid.split(".")[1]
    url = (
        "https://finance.pae.baidu.com/vapi/v1/getquotation?"
        f"group=quotation_fiveday_ab&code={code}&market_type=ab&newFormat=1"
    )
    page.goto("https://finance.baidu.com", timeout=15000)
    time.sleep(0.3)
    page.goto(url, timeout=20000)
    text = page.evaluate("() => document.body.innerText || ''")
    data = json.loads(text)
    rows = []
    md = data.get("Result", {}).get("newMarketData", {})
    for day in md.get("marketData", []):
        p = day.get("p")
        if not p:
            continue
        for seg in p.split(";"):
            arr = seg.split(",")
            if len(arr) < 8:
                continue
            ts = int(arr[0])
            dt = datetime.fromtimestamp(ts)
            rows.append(
                f"{dt.strftime('%Y-%m-%d %H:%M')},{arr[2]},{arr[2]},{arr[2]},{arr[2]},{arr[6]},{arr[7]}"
            )
    return rows or None


# =========================================================
# 新浪兜底
# =========================================================

def fetch_trends_sina(page, secid):
    code = secid.split(".")[1]
    market = "sh" if secid.startswith("1.") else "sz"
    url = f"https://hq.sinajs.cn/list={market}{code}"
    page.goto("https://finance.sina.com.cn", timeout=15000)
    time.sleep(0.3)
    page.goto(url, timeout=15000)
    text = page.evaluate("() => document.body.innerText || ''")
    m = re.search(r'="([^"]+)"', text)
    if not m:
        return None
    arr = m.group(1).split(",")
    if len(arr) < 32:
        return None
    date = arr[30]
    time_ = arr[31][:5]
    return [
        f"{date} {time_},{arr[1]},{arr[3]},{arr[4]},{arr[5]},{arr[8]},{arr[9]}"
    ]


# =========================================================
# 分钟 → 日级
# =========================================================

def aggregate_daily(rows, secid):
    groups = {}
    for r in rows:
        p = r.split(",")
        day = p[0].split(" ")[0]
        groups.setdefault(day, []).append(p)

    result = []
    prev_close = None
    code = secid.split(".")[1]

    for day in sorted(groups):
        rows = groups[day]
        o = float(rows[0][1])
        c = float(rows[-1][2])
        h = max(float(x[3]) for x in rows)
        l = min(float(x[4]) for x in rows)
        v = sum(int(float(x[5])) for x in rows)
        amt = sum(float(x[6]) for x in rows)

        chg = None if prev_close is None else round(c - prev_close, 4)
        pct = None if prev_close in (None, 0) else round(chg / prev_close * 100, 4)

        result.append({
            "trade_date": day,
            "trade_time": f"{day}T00:00:00",
            "stock_code": code,
            "open": o,
            "close": c,
            "high": h,
            "low": l,
            "volume": v,
            "amount": amt,
            "pre_close": prev_close,
            "change": chg,
            "change_pct": pct,
            "turnover_ratio": None
        })
        prev_close = c

    return result


# =========================================================
# 主入口
# =========================================================

if __name__ == "__main__":
    raw = sys.argv[1] if len(sys.argv) > 1 else "600000"
    ndays = int(sys.argv[2]) if len(sys.argv) > 2 else 5

    secid = normalize_secid(raw)
    logger.info(f"START secid={secid}, ndays={ndays}")

    daily = []

    with sync_playwright() as p:
        browser, ctx = build_context(p)
        page = ctx.new_page()

        rows = (
                fetch_trends_eastmoney(page, secid, ndays)
                or fetch_trends_baidu(page, secid)
                or fetch_trends_sina(page, secid)
        )

        if rows:
            daily = aggregate_daily(rows, secid)

        page.close()
        browser.close()

    # stdout 只输出 JSON
    print(json.dumps(daily, ensure_ascii=False, separators=(",", ":")))
