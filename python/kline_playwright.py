#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
eastmoney_kline_playwright_single_market.py
-------------------------------------------
Playwright 版本 — 模拟浏览器请求东方财富 K 线接口。

本脚本与原版主要区别：
✅ 去除了 “多 market 尝试逻辑”
✅ 仅使用传入的 market，不自动切换 0/1/2
✅ 保留所有其它功能：多 URL 模板、JSONP 解析、昨日收盘价提取等

用法:
    python eastmoney_kline_playwright.py 股票代码 市场 起始日期 结束日期

参数:
    market:
        0 - 深市
        1 - 沪市
        2 - 北交所

环境变量:
    PLAYWRIGHT_PROXY  设置代理，如: http://user:pass@host:port
    HEADFUL=1         允许打开有头浏览器（调试用）
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
DEFAULT_RETRIES = 3  # 每种 URL 模式最大重试次数
DEFAULT_DELAY = 2  # 每次失败后的等待秒数
HEADFUL = os.getenv("HEADFUL", "0") == "1"
PLAYWRIGHT_PROXY = os.getenv("PLAYWRIGHT_PROXY", "")
# ---------------------------------------

# 随机 UA 列表
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
]

# URL 模板（保留 2 种，兼容不同股票）
URL_TEMPLATES = [
    # 模式 A（旧版接口）
    (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f18&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        "beg={beg_date}&end={end_date}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
        "secid={market}.{secid}&klt=101&fqt=1"
    ),

    # 模式 B（新接口）
    (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "secid={market}.{secid}&ut=7eea3edcaed734bea9cbfc24409ed989&"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f18&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        "klt=101&fqt=1&beg={beg_date}&end={end_date}&smplmt=100000&lmt=1000000"
    ),
]


# ---------------- 工具函数 ----------------

def parse_json_or_jsonp(text: str) -> Optional[dict]:
    """解析 JSON 或 JSONP"""
    if not text:
        return None
    text = text.lstrip("\ufeff")

    # JSONP 格式：foo({...});
    m = re.match(r"^\s*\w+\s*\((.*)\)\s*;?\s*$", text, flags=re.S)
    payload = m.group(1) if m else text

    try:
        return json.loads(payload)
    except Exception:
        return None


def to_decimal(val: str) -> Optional[float]:
    """字符串 → float"""
    try:
        return round(float(val), 4)
    except Exception:
        return None


def to_int(val: str) -> Optional[int]:
    """字符串 → int"""
    try:
        return int(val)
    except Exception:
        return None


def parse_kline_items(data: dict, pre_close_from_api: Optional[float]) -> List[Dict[str, Any]]:
    """解析 kline 列表，并补充昨日收盘价"""
    if not data or "klines" not in data:
        return []

    code = data.get("code")
    result = []
    prev_close = pre_close_from_api  # 初始昨日收盘价

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

        # 日期格式处理
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
            "pre_close": prev_close,
        })

        prev_close = close_price  # 更新昨日收盘价

    return result


# ---------------- 核心函数：去除多 market ----------------

def fetch_kline_playwright(secid: str,
                           market: str = "1",
                           beg_date: str = "0",
                           end_date: str = "20500101",
                           retries: int = DEFAULT_RETRIES,
                           delay: int = DEFAULT_DELAY) -> Optional[List[Dict[str, Any]]]:
    """
    Playwright 访问东方财富 K 线接口。
    ✅ 已移除多 market 自动切换，仅使用传入的 market。

    参数：
        secid:   股票代码（不含市场前缀）
        market:  市场代码（0/1/2）
        beg/end: 日期
    """
    proxy_config = {"server": PLAYWRIGHT_PROXY} if PLAYWRIGHT_PROXY else None

    with sync_playwright() as p:
        browser = p.chromium.launch(proxy=proxy_config, headless=not HEADFUL)
        try:
            context = browser.new_context(
                user_agent=random.choice(USER_AGENTS),
                viewport={"width": 1366, "height": 768},
                java_script_enabled=True,
                locale="zh-CN",
            )

            # 访问首页，避免被反爬识别
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

            # ✅ 不再循环 market，仅使用传入的 market
            for url_tpl in URL_TEMPLATES:

                url = url_tpl.format(
                    secid=secid,
                    market=market,
                    beg_date=beg_date,
                    end_date=end_date
                )

                for attempt in range(1, retries + 1):
                    headers["user-agent"] = random.choice(USER_AGENTS)
                    time.sleep(random.uniform(0.3, 0.8))

                    try:
                        resp = request_ctx.get(url, headers=headers, timeout=30000)
                        text = resp.text()
                        parsed = parse_json_or_jsonp(text)

                        # 成功解析到 kline
                        if parsed and parsed.get("data", {}).get("klines"):
                            data = parsed["data"]

                            # ✅ 提取昨日收盘价
                            pre_close = None
                            if "preKPrice" in data:
                                pre_close = to_decimal(data["preKPrice"])
                            elif "f18" in data:
                                pre_close = to_decimal(data["f18"])

                            return parse_kline_items(data, pre_close)

                    except Exception:
                        time.sleep(delay)

            return None

        finally:
            browser.close()


if __name__ == "__main__":
    # 示例：上证 601138 (工业富联)
    # 您可以修改这里的股票代码和市场代码来获取不同股票的数据
    # 市场代码: 0 - 深圳, 1 - 上海, 2 - 北京证券交易所（北交所）

    import sys

    # 默认值
    stock_code = "601138"
    market_code = "1"
    beg_date = "0"
    end_date = "20500101"

    # 检查是否有命令行参数传入
    if len(sys.argv) >= 5:
        stock_code = sys.argv[1]
        market_code = sys.argv[2]
        beg_date = sys.argv[3]
        end_date = sys.argv[4]
    elif len(sys.argv) >= 3:
        stock_code = sys.argv[1]
        market_code = sys.argv[2]
    elif len(sys.argv) >= 2:
        stock_code = sys.argv[1]

    # print(f"正在获取 {stock_code} 的全部历史数据...")
    result = fetch_kline_playwright(stock_code, market_code, beg_date, end_date, retries=4, delay=2)

    if not result:
        print("None")
    else:
        # 输出数据条数
        # print(f"成功获取到 {len(result)} 条K线数据")

        # 按用户要求的格式输出数据
        print(json.dumps(result, ensure_ascii=False, indent=2))

        # 如果您希望将数据保存到文件中，可以取消下面几行的注释
        # with open(f'{stock_code}_kline_data.json', 'w', encoding='utf-8') as f:
        #     json.dump(result, f, ensure_ascii=False, indent=2)
        # print(f"数据已保存到 {stock_code}_kline_data.json 文件中")
