#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
实时 ETF 行情标准输出版本（任意 push2/qt/stock/get API URL）
输出字段与 Java DTO 完全对应
"""

import sys, json, re, time, random, logging, os
from logging.handlers import TimedRotatingFileHandler
from playwright.sync_api import sync_playwright

# 日志设置
LOG_DIR = "logs"
os.makedirs(LOG_DIR, exist_ok=True)
logger = logging.getLogger("etf-realtime")
logger.setLevel(logging.INFO)
fh = TimedRotatingFileHandler(os.path.join(LOG_DIR, "etf_realtime.log"),
                              when="midnight", encoding="utf-8", backupCount=30)
fh.setFormatter(logging.Formatter(fmt="[%(asctime)s] %(message)s", datefmt="%Y-%m-%d %H:%M:%S"))
logger.addHandler(fh)
logger.propagate = False

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
]


def random_ua(): return random.choice(UA_POOL)


def parse_json_or_jsonp(text):
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except:
        pass
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if not m: return None
    try:
        return json.loads(m.group(1))
    except:
        return None


def fetch_json_by_browser(api_url, max_retry=4):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(locale="zh-CN", user_agent=random_ua())
        context.add_init_script(
            "() => Object.defineProperty(navigator,'webdriver',{get:()=>false})"
        )
        page = context.new_page()
        try:
            page.goto("https://quote.eastmoney.com/", timeout=15000)
            time.sleep(0.5 + random.random())
        except Exception as e:
            logger.warning(f"[预热失败] {e}")

        for attempt in range(1, max_retry + 1):
            try:
                logger.info(f"[FETCH] {attempt}/{max_retry} {api_url}")
                page.goto(api_url, timeout=20000, wait_until="domcontentloaded")
                text = page.evaluate("() => document.body.innerText || ''").strip()
                if not text: raise Exception("empty body")
                if text.startswith("<"): raise Exception("HTML risk")
                parsed = parse_json_or_jsonp(text)
                if parsed is None: raise Exception("parse fail")
                return parsed
            except Exception as e:
                logger.warning(f"[ERR] attempt {attempt}: {e}")
                time.sleep(1 + random.random())
        return None


def safe(dct, key):
    return dct.get(key)


def standardize(data):
    return {
        "stockCode": safe(data, "f57"),
        "companyName": safe(data, "f58"),
        "price": safe(data, "f43") / 100 if safe(data, "f43") is not None else None,
        "prevClose": safe(data, "f60") / 100 if safe(data, "f60") is not None else None,
        "openPrice": safe(data, "f46") / 100 if safe(data, "f46") is not None else None,
        "highPrice": safe(data, "f44") / 100 if safe(data, "f44") is not None else None,
        "lowPrice": safe(data, "f45") / 100 if safe(data, "f45") is not None else None,
        "volume": safe(data, "f47"),
        "turnover": safe(data, "f48"),
        "volumeRatio": safe(data, "f52"),
        "commissionRatio": safe(data, "f20"),
        "mainFundsInflow": safe(data, "f152")
    }


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("{}", end="")
        sys.exit(0)
    url = sys.argv[1]
    parsed = fetch_json_by_browser(url) or {}
    data = parsed.get("data", {})
    obj = standardize(data)
    print(json.dumps(obj, ensure_ascii=False, separators=(",", ":")))
    sys.exit(0)
