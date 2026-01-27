#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import json
import re
import time
import random
from datetime import datetime
from playwright.sync_api import sync_playwright

# -------------------- 日志配置 --------------------
import logging
from logging.handlers import TimedRotatingFileHandler
import os

def setup_logger():
    LOG_DIR = "logs"
    os.makedirs(LOG_DIR, exist_ok=True)
    logger = logging.getLogger("tick-realtime")
    logger.setLevel(logging.INFO)
    if not logger.handlers:
        console_fmt = logging.Formatter('%(asctime)s | %(levelname)s | %(message)s', '%H:%M:%S')
        ch = logging.StreamHandler()
        ch.setFormatter(console_fmt)
        logger.addHandler(ch)
    return logger

logger = setup_logger()

# -------------------- 工具方法 --------------------

def random_ua():
    UA_POOL = [
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
    ]
    return random.choice(UA_POOL)

def normalize_record(obj: dict) -> dict:
    """清洗 Key 隐藏字符并补齐字段"""
    clean = {}
    for k, v in obj.items():
        nk = re.sub(r"[\r\n\t ]+", "", k)
        if nk: clean[nk] = v

    # 确保字段完整性
    required = ["stockCode", "time", "price", "volume", "side", "sideCode", "tickCount", "avgVol"]
    for f in required:
        if f not in clean: clean[f] = None
    return clean

def parse_json_or_jsonp(text):
    """解析 JSONP 响应"""
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except:
        pass
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if m:
        try: return json.loads(m.group(1))
        except: return None
    return None

# -------------------- 数据解析逻辑 --------------------

def standardize_tick(data, stock_code):
    """解析东方财富实时明细数据"""
    details = data.get("details", [])
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}

    for item in details:
        parts = item.split(",")
        if len(parts) < 5: continue
        try:
            vol = int(parts[2])
            ticks = int(parts[3])
            side_code = parts[4]
            avg_vol = vol / ticks if ticks > 0 else 0

            raw_obj = {
                "stockCode": stock_code,
                "time": parts[0],
                "price": float(parts[1]),
                "volume": vol,
                "side": side_map.get(side_code, "其他"),
                "sideCode": side_code,
                "tickCount": ticks,
                "avgVol": round(avg_vol, 2)
            }
            result.append(normalize_record(raw_obj))
        except: continue
    return result

# -------------------- 监控主程序 --------------------

def start_multi_monitor(secid_list):
    # 自动修正 secid 前缀
    tasks = []
    for sid in secid_list:
        raw_code = sid.split('.')[-1]
        real_sid = f"1.{raw_code}" if raw_code.startswith(('6', '688', '900')) else f"0.{raw_code}"
        tasks.append({"sid": real_sid, "code": raw_code, "last_seen": set(), "fails": 0})

    logger.info(f"==== 稳定监控启动 | 任务数: {len(tasks)} ====")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            user_agent=random_ua(),
            extra_http_headers={"Referer": "https://quote.eastmoney.com/"}
        )
        context.add_init_script("() => Object.defineProperty(navigator, 'webdriver', { get: () => false })")

        page = context.new_page()
        # 预热
        try:
            page.goto("https://quote.eastmoney.com/", timeout=15000)
            time.sleep(1)
        except: pass

        try:
            while True:
                for task in tasks:
                    if task["fails"] > 3:
                        time.sleep(5)
                        task["fails"] = 0

                    ts = int(time.time() * 1000)
                    url = (
                        "https://push2.eastmoney.com/api/qt/stock/details/get?"
                        f"secid={task['sid']}&ut=bd1d9ddb04089700cf9c27f6f7426281&"
                        "fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&"
                        f"pos=-200&num=200&cb=jQuery_{ts}&_={ts}"
                    )

                    try:
                        # 核心：复用 Page 跳转而非重启浏览器
                        page.goto(url, wait_until="commit", timeout=10000)
                        content = page.evaluate("() => document.body.innerText").strip()

                        parsed = parse_json_or_jsonp(content)
                        if not parsed or not parsed.get("data"): continue

                        ticks = standardize_tick(parsed["data"], task["code"])

                        for tick in ticks:
                            # 唯一键去重
                            key = (tick["time"], tick["price"], tick["volume"], tick["sideCode"])
                            if key in task["last_seen"]: continue

                            task["last_seen"].add(key)

                            # --- 您的示例逻辑：大资金入场判定 ---
                            avg_vol = tick["volume"] / tick["tickCount"] if tick["tickCount"] > 0 else 0
                            if tick["volume"] > 10000 and avg_vol > 1000 and tick["sideCode"] == "1":
                                # 输出大资金提示（带前缀便于 Java 或日志识别）
                                print(f"!!! 疑似大资金入场: {tick['stockCode']} 在 {tick['time']} 成交了 {tick['volume']} 股，单笔均量 {avg_vol:.0f}")

                            # 正常输出 JSON 数据
                            print(json.dumps(tick, ensure_ascii=False, separators=(",", ":")))
                            sys.stdout.flush()

                        task["fails"] = 0
                        time.sleep(random.uniform(0.3, 0.6))

                    except Exception as e:
                        task["fails"] += 1
                        if "ERR_EMPTY_RESPONSE" in str(e) or "connection reset" in str(e).lower():
                            logger.error(f"代码 {task['code']} 被限制，等待恢复...")
                            time.sleep(5)
                        continue

                time.sleep(random.uniform(1.0, 2.5))

        except KeyboardInterrupt:
            logger.info("监控停止")
        finally:
            browser.close()

if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit(0)
    start_multi_monitor(args)