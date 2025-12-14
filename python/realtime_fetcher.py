#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
最终稳定版：东财实时接口（任意 URL） → 标准 JSON 输出
-----------------------------------------------------
根据 Java DTO 映射输出字段结构，并做基础类型解析与默认值补齐
"""

import os
import sys
import re
import json
import time
import random
import logging
from logging.handlers import TimedRotatingFileHandler
from playwright.sync_api import sync_playwright

# -------------------- 日志 --------------------

LOG_DIR = "logs"
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("realtime-fetcher-standard")
logger.setLevel(logging.INFO)

file_handler = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "realtime_fetch_standard.log"),
    when="midnight", interval=1, backupCount=30, encoding="utf-8"
)
file_handler.setFormatter(logging.Formatter(
    fmt="[%(asctime)s] [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
))
logger.addHandler(file_handler)
logger.propagate = False

# -------------------- UA池 --------------------

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
]


def random_ua():
    return random.choice(UA_POOL)


# -------------------- JSON/JSONP 解析 --------------------

def parse_json_or_jsonp(text: str):
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except:
        pass
    m = re.search(r"\w+\s*\((.*)\)\s*;?\s*$", text, flags=re.S)
    if not m:
        return None
    try:
        return json.loads(m.group(1))
    except:
        return None


# -------------------- 浏览器请求 --------------------

def fetch_json_by_browser(api_url: str, max_retry: int = 4):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(locale="zh-CN", user_agent=random_ua())
        context.add_init_script("() => Object.defineProperty(navigator, 'webdriver', {get: () => false})")
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
                if not text:
                    raise Exception("empty body")
                if text.startswith("<"):
                    raise Exception("HTML风控页")
                parsed = parse_json_or_jsonp(text)
                if parsed is None:
                    raise Exception("parse fail")
                return parsed
            except Exception as e:
                logger.warning(f"[尝试失败] {attempt}: {e}")
                time.sleep(1 + random.random())
        return None


# -------------------- 字段安全解析 --------------------

def safe_str(dct, key):
    v = dct.get(key)
    return str(v) if v is not None else ""


def safe_float(dct, key):
    try:
        return float(dct.get(key)) if (key in dct and dct.get(key) is not None) else None
    except:
        return None


def safe_int(dct, key):
    try:
        return int(dct.get(key)) if (key in dct and dct.get(key) is not None) else None
    except:
        return None


# -------------------- 标准对象构建 --------------------

def build_standard_object(data: dict):
    out = {
        "stockCode": safe_str(data, "f57"),
        "companyName": safe_str(data, "f58"),
        "price": safe_float(data, "f43") / 100 if data.get("f43") is not None else None,
        "prevClose": safe_float(data, "f60") / 100 if data.get("f60") is not None else None,
        "openPrice": safe_float(data, "f46") / 100 if data.get("f46") is not None else None,
        "highPrice": safe_float(data, "f44") / 100 if data.get("f44") is not None else None,
        "lowPrice": safe_float(data, "f45") / 100 if data.get("f45") is not None else None,
        "volume": safe_int(data, "f47"),
        "turnover": safe_float(data, "f48"),
        "volumeRatio": safe_float(data, "f52"),
        "commissionRatio": safe_float(data, "f20"),
        "mainFundsInflow": safe_float(data, "f152"),
        # 基础字段没有的数据仍然保持 None
        "peRatio": None,
        "pbRatio": None,
        "turnoverRate": None,
        "amplitude": None,
        "eps": None,
        "mainNetInflow": None,
        "circulatingShares": None,
        "totalShares": None,
        "volumePriceTrend": None,
        "dividendYield": None,
        "roe": None,
        "grossMargin": None,
        "institutionalFlow": None,
        "retailFlow": None
    }
    return out


# -------------------- 主入口 --------------------

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("{}", end="")
        sys.exit(0)

    api_url = sys.argv[1]
    logger.info(f"[MAIN] {api_url}")

    parsed = {}
    try:
        parsed = fetch_json_by_browser(api_url) or {}
    except Exception as e:
        logger.error(f"[MAIN ERROR] {e}")

    # 如果没有 data 直接空对象
    data = parsed.get("data", {})
    standard_obj = build_standard_object(data)

    # 输出标准 JSON
    print(json.dumps(standard_obj, ensure_ascii=False, separators=(",", ":")))
    sys.exit(0)
