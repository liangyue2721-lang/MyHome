#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
eastmoney_kline_playwright.py
---------------------------------
Playwright 版 — 模拟浏览器身份访问东方财富 push2his 接口，
获取 K 线数据并解析为结构化 JSON。

用法:
    python eastmoney_kline_playwright.py

说明:
- 支持重试与自动切换 secid 参数；
- 可通过环境变量 PLAYWRIGHT_PROXY 指定代理 (例: http://user:pass@host:port)
- 可通过 HEADFUL=1 打开有头浏览器调试。
"""

import os
import time
import json
import re
import random
from datetime import datetime
from typing import Optional, List, Dict, Any
from playwright.sync_api import sync_playwright, TimeoutError as PWTimeoutError

# ---------------- 配置区 ----------------
DEFAULT_RETRIES = 3         # 每种 URL 的最大重试次数
DEFAULT_DELAY = 2           # 重试间隔秒数
HEADFUL = os.getenv("HEADFUL", "0") == "1"
PLAYWRIGHT_PROXY = os.getenv("PLAYWRIGHT_PROXY", "")
# --------------------------------------

USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/127.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/126.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_4) AppleWebKit/605.1.15 (KHTML, like Gecko) "
    "Version/17.4 Safari/605.1.15",
]

# 两种常见的 URL 模板（有些股票必须切换 ut / 参数组合）
URL_TEMPLATES = [
    # 模式 A（较旧）
    (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        "beg=0&end=20500101&ut=fa5fd1943c7b386f172d6893dbfba10b&"
        "secid={market}.{secid}&klt=101&fqt=1"
    ),
    # 模式 B（较新）
    (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "secid={market}.{secid}&ut=7eea3edcaed734bea9cbfc24409ed989&"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        "klt=101&fqt=1&beg=0&end=20500101&smplmt=100000&lmt=1000000"
    ),
]


def parse_json_or_jsonp(text: str) -> Optional[dict]:
    """解析 JSON 或 JSONP 文本"""
    if not text:
        return None
    text = text.lstrip('\ufeff')
    m = re.match(r'^\s*\w+\s*\((.*)\)\s*;?\s*$', text, flags=re.S)
    payload = m.group(1) if m else text
    try:
        return json.loads(payload)
    except json.JSONDecodeError:
        return None


def to_decimal(val: str) -> Optional[float]:
    try:
        return round(float(val), 4)
    except Exception:
        return None


def to_int(val: str) -> Optional[int]:
    try:
        return int(val)
    except Exception:
        return None


def parse_kline_items(data: dict) -> List[Dict[str, Any]]:
    """解析东方财富 K 线数据"""
    if not data or "klines" not in data or not data["klines"]:
        return []

    code = data.get("code")
    result = []
    for item in data["klines"]:
        parts = item.split(",")
        if len(parts) < 10:
            continue

        trade_date_str = parts[0]
        open_price = to_decimal(parts[1])
        close_price = to_decimal(parts[2])
        high_price = to_decimal(parts[3])
        low_price = to_decimal(parts[4])
        volume = to_int(parts[5])
        amount = to_decimal(parts[6])
        change = to_decimal(parts[7])
        change_pct = to_decimal(parts[8])
        turnover_ratio = to_decimal(parts[9])

        try:
            trade_date = datetime.strptime(trade_date_str, "%Y-%m-%d").date()
            trade_time = datetime.combine(trade_date, datetime.min.time())
        except Exception:
            trade_time = None

        result.append({
            "trade_date": trade_date_str,
            "trade_time": trade_time.isoformat() if trade_time else None,
            "stock_code": code,
            "open": open_price,
            "close": close_price,
            "high": high_price,
            "low": low_price,
            "volume": volume,
            "amount": amount,
            "change": change,
            "change_pct": change_pct,
            "turnover_ratio": turnover_ratio,
            "pre_close": None
        })
    return result


def fetch_kline_playwright(secid: str, market: str = "1",
                           retries: int = DEFAULT_RETRIES, delay: int = DEFAULT_DELAY) -> Optional[List[Dict[str, Any]]]:
    """使用 Playwright 请求东方财富接口，失败自动切换 URL 组合和 market"""
    proxy_config = {"server": PLAYWRIGHT_PROXY} if PLAYWRIGHT_PROXY else None

    with sync_playwright() as p:
        browser = p.chromium.launch(proxy=proxy_config, headless=not HEADFUL)
        try:
            context = browser.new_context(
                user_agent=random.choice(USER_AGENTS),
                locale="zh-CN",
                viewport={"width": 1366, "height": 768},
                java_script_enabled=True,
            )

            page = context.new_page()
            try:
                page.goto("https://quote.eastmoney.com/", timeout=8000)
                time.sleep(random.uniform(0.2, 0.6))
            except Exception:
                pass
            page.close()

            request_ctx = context.request
            headers = {
                "accept": "application/json, text/javascript, */*; q=0.01",
                "accept-language": "zh-CN,zh;q=0.9,en;q=0.8",
                "referer": "https://quote.eastmoney.com/",
                "origin": "https://quote.eastmoney.com",
                "cache-control": "no-cache",
            }

            # 遍历多种 URL 模式 + 市场组合
            market_candidates = [market, "0", "1", "2"]
            for url_tpl in URL_TEMPLATES:
                for mkt in market_candidates:
                    url = url_tpl.format(secid=secid, market=mkt)
                    #print(f"[INFO] 尝试 URL: {url}")
                    for attempt in range(1, retries + 1):
                        headers["user-agent"] = random.choice(USER_AGENTS)
                        time.sleep(random.uniform(0.3, 0.8))
                        try:
                            resp = request_ctx.get(url, headers=headers, timeout=30000)
                            text = resp.text()
                            status = resp.status
                            #print(f"[DEBUG] 第{attempt}次尝试 HTTP {status}, len={len(text)}")

                            if status in (403, 429):
                                #print(f"[WARN] 限流/拦截 HTTP {status}")
                                if attempt < retries:
                                    time.sleep(delay)
                                    continue

                            parsed = parse_json_or_jsonp(text)
                            if parsed and "data" in parsed and parsed["data"].get("klines"):
                                print(f"[OK] 成功解析 {len(parsed['data']['klines'])} 条记录 (market={mkt})")
                                return parse_kline_items(parsed["data"])
                            else:
                                #print(f"[WARN] 无效响应，重试中...")
                                time.sleep(delay)
                        except Exception as e:
                            #print(f"[ERROR] 请求异常: {e}")
                            time.sleep(delay)
            #print("[FAIL] 所有 URL 模式与市场组合均失败。")
            return None

        finally:
            browser.close()


if __name__ == "__main__":
    stock_code = "688596"   # 示例股票
    market_code = "2"       # 默认上交所

    #print(f"开始获取 {stock_code} 的历史 K 线数据...")
    result = fetch_kline_playwright(stock_code, market_code, retries=3, delay=2)

    if not result:
        print("None")
    else:
        #print(f"✅ 成功获取 {len(result)} 条记录")
        print(json.dumps(result[:5], ensure_ascii=False, indent=2))  # 仅展示前5条
