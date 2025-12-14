#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Playwright KLINE 聚合版（含 normalize 清洗）
-------------------------------------------
特点：
1) 真实浏览器预热 → 获取 Cookie（规避反爬）
2) Playwright request.get 访问 KLINE
3) Normalize 字段清洗（去除 \r\n\t 空格）
4) 自动补齐字段，永不缺字段
5) 每天一个对象（日级 K 线）
6) stdout 只输出 JSON（Java 直接反序列化）
"""

import sys
import json
import time
import random
import re
from datetime import datetime
from playwright.sync_api import sync_playwright


# =============================================
# normalize：清洗 key + 补齐字段
# =============================================

def normalize_record(obj: dict) -> dict:
    clean = {}

    for k, v in obj.items():
        nk = re.sub(r"[\r\n\t ]+", "", k)  # 清理隐藏字符
        if nk:
            clean[nk] = v

    required = [
        "trade_date", "trade_time", "stock_code",
        "open", "close", "high", "low",
        "volume", "amount",
        "pre_close", "change", "change_pct", "turnover_ratio"
    ]

    for f in required:
        if f not in clean:
            clean[f] = None

    return clean


def normalize_list(lst: list):
    if not lst:
        return []
    return [normalize_record(x) for x in lst]


# =============================================
# KLINE API 请求（Playwright request）
# =============================================

def fetch_kline_playwright(secid: str, beg_date="0", end_date="20500101"):
    """
    secid 示例：1.601138
    """

    with sync_playwright() as p:
        # ----------------------------
        # 1) 启动真实浏览器
        # ----------------------------
        browser = p.chromium.launch(headless=True)

        context = browser.new_context(
            locale="zh-CN",
            user_agent=random.choice([
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_6) AppleWebKit/605.1.15 Version/15.5 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
            ])
        )

        # 反爬关键：隐藏 webdriver
        context.add_init_script(
            "() => Object.defineProperty(navigator, 'webdriver', { get: () => false })"
        )

        # ----------------------------
        # 2) 预热 cookies（最关键）
        # ----------------------------
        page = context.new_page()
        try:
            page.goto("https://quote.eastmoney.com/", timeout=15000)
            time.sleep(0.5 + random.random())
        except Exception:
            pass
        page.close()

        # ----------------------------
        # 3) request 访问 KLINE API
        # ----------------------------
        api = context.request

        url = (
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
            "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
            "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
            f"beg={beg_date}&end={end_date}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
            f"secid={secid}&klt=101&fqt=1"
        )

        headers = {
            "Referer": "https://quote.eastmoney.com/",
            "Origin": "https://quote.eastmoney.com",
            "Accept": "*/*",
            "Accept-Encoding": "gzip, deflate, br"
        }

        try:
            resp = api.get(url, headers=headers, timeout=20000)
            text = resp.text()
        except Exception:
            browser.close()
            return []

        browser.close()

    # ----------------------------
    # 4) 解析 JSON
    # ----------------------------
    try:
        raw = json.loads(text)
        data = raw.get("data", {})
        klines = data.get("klines", [])
    except:
        return []

    result = []
    code = data.get("code", None)

    # ----------------------------
    # 5) 解析每条 KLINE
    # ----------------------------
    prev_close = None

    for row in klines:
        parts = row.split(",")
        if len(parts) < 10:
            continue

        trade_date_str = parts[0]
        open_p = float(parts[1])
        close_p = float(parts[2])
        high_p = float(parts[3])
        low_p = float(parts[4])
        volume = int(parts[5])
        amount = float(parts[6])
        change = float(parts[7])
        change_pct = float(parts[8])
        turnover_ratio = float(parts[9])

        try:
            tdate = datetime.strptime(trade_date_str, "%Y-%m-%d").date()
            ttime = datetime.combine(tdate, datetime.min.time())
            ttime_str = ttime.isoformat()
        except:
            ttime_str = None

        result.append({
            "trade_date": trade_date_str,
            "trade_time": ttime_str,
            "stock_code": code,
            "open": open_p,
            "close": close_p,
            "high": high_p,
            "low": low_p,
            "volume": volume,
            "amount": amount,
            "change": change,
            "change_pct": change_pct,
            "turnover_ratio": turnover_ratio,
            "pre_close": prev_close
        })

        prev_close = close_p

    # ----------------------------
    # 6) normalize：修复转义符 & 补齐字段
    # ----------------------------
    return normalize_list(result)


# =============================================
# CLI：stdout 只输出 JSON 一行
# =============================================

if __name__ == "__main__":
    stock_code = "601138"
    market = "1"

    if len(sys.argv) >= 3:
        stock_code = sys.argv[1]
        market = sys.argv[2]
    elif len(sys.argv) >= 2:
        stock_code = sys.argv[1]

    secid = f"{market}.{stock_code}"

    items = fetch_kline_playwright(secid)

    print(json.dumps(items, ensure_ascii=False, separators=(",", ":")))
