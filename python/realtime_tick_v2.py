#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#  参数名,数据类型,对应接口字段,说明
#  stockCode,String,外部传入,6 位股票代码
#  time,String,f51,成交时刻 (HH:mm:ss)
#  price,Float,f52,每股成交价
#  volume,Integer,f53,成交数量（股）
#  sideCode,String,f55,"1:买, 2:卖, 4:中性"
#  tickCount,Integer,f54,该聚合时刻包含的原始订单笔数
import sys
import json
import re
import time
import random
import logging
import os
from logging.handlers import TimedRotatingFileHandler
from playwright.sync_api import sync_playwright

# -------------------- 日志配置 --------------------

def setup_logger():
    """
    配置双路日志系统：
    - 文件日志：存放在 logs/tick_worker.log，按天滚动，保留30天。
    - 控制台日志：简洁实时输出运行状态。
    """
    LOG_DIR = "logs"
    os.makedirs(LOG_DIR, exist_ok=True)

    logger = logging.getLogger("tick-realtime")
    logger.setLevel(logging.INFO)

    if not logger.handlers:
        file_formatter = logging.Formatter('[%(asctime)s] [%(levelname)s] %(message)s', '%Y-%m-%d %H:%M:%S')
        console_formatter = logging.Formatter('%(asctime)s | %(levelname)s | %(message)s', '%H:%M:%S')

        # 文件处理器
        fh = TimedRotatingFileHandler(os.path.join(LOG_DIR, "tick_worker.log"), when="midnight", encoding="utf-8", backupCount=30)
        fh.setFormatter(file_formatter)

        # 控制台处理器
        ch = logging.StreamHandler()
        ch.setFormatter(console_formatter)

        logger.addHandler(fh)
        logger.addHandler(ch)

    logger.propagate = False
    return logger

logger = setup_logger()

# -------------------- 工具方法 --------------------

def random_ua():
    """随机获取 User-Agent 模拟真实浏览器"""
    UA_POOL = [
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
    ]
    return random.choice(UA_POOL)

def parse_json_or_jsonp(text):
    """解析 JSONP 或 JSON 格式的接口响应"""
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

# -------------------- 抓取与解析逻辑 --------------------

def fetch_json_by_browser(api_url, max_retry=3):
    """使用 Playwright 模拟浏览器环境抓取数据"""
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(locale="zh-CN", user_agent=random_ua())
        page = context.new_page()

        for attempt in range(1, max_retry + 1):
            start_time = time.time()
            try:
                page.goto(api_url, timeout=20000, wait_until="domcontentloaded")
                text = page.evaluate("() => document.body.innerText || ''").strip()
                duration = (time.time() - start_time) * 1000

                if not text or text.startswith("<"):
                    raise Exception("返回内容非合法JSON")

                parsed = parse_json_or_jsonp(text)
                if parsed:
                    logger.info(f"API请求成功 | 耗时: {duration:.0f}ms | 长度: {len(text)}")
                    return parsed
            except Exception as e:
                logger.warning(f"抓取失败 (重试 {attempt}/{max_retry}): {e}")
                time.sleep(1)

        browser.close()
        return None

def standardize_tick(data, stock_code):
    """
    将原始明细解析为标准格式。
    - parts[3]: 成交笔数 (tickCount)
    - parts[4]: 买卖方向 (side)
    """
    details = data.get("details", [])
    result = []
    # 1: 买入, 2: 卖出, 4: 中性
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}

    for item in details:
        parts = item.split(",")
        if len(parts) < 5: continue

        try:
            raw_side = parts[4]
            result.append({
                "stockCode": stock_code,
                "companyName": "", # 允许为空
                "time": parts[0],
                "price": float(parts[1]),
                "volume": int(parts[2]),
                "side": side_map.get(raw_side, "其他"),
                "sideCode": raw_side,
                "tickCount": int(parts[3])
            })
        except:
            continue
    return result

def build_f1_url(secid):
    """构造带有鉴权与格式参数的 URL"""
    base = "https://push2.eastmoney.com/api/qt/stock/details/get"
    ts = int(time.time() * 1000)
    params = {
        "secid": secid,
        "ut": "bd1d9ddb04089700cf9c27f6f7426281",
        "fields1": "f1,f2,f3,f4",
        "fields2": "f51,f52,f53,f54,f55", # f54=笔数, f55=方向
        "pos": -2000,
        "num": 2000,
        "cb": f"jQuery_{ts}",
        "_": ts
    }
    return f"{base}?{'&'.join([f'{k}={v}' for k, v in params.items()])}"

# -------------------- 主程序 --------------------

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python realtime_tick_v2.py <secid> (e.g., 1.601138)")
        sys.exit(0)

    # 从参数提取股票代码 (如 1.601138 -> 601138)
    secid_input = sys.argv[1].strip()
    stock_code_final = secid_input.split('.')[-1] if '.' in secid_input else secid_input

    api_url = build_f1_url(secid_input)
    logger.info(f"监控启动 | 代码: {stock_code_final} | 完整标识: {secid_input}")

    last_seen = set()

    try:
        while True:
            parsed = fetch_json_by_browser(api_url)
            if not parsed:
                time.sleep(5)
                continue

            data = parsed.get("data") or {}
            ticks = standardize_tick(data, stock_code_final)

            new_count = 0
            for tick in ticks:
                # 唯一键：时间+价格+量+方向代码，确保精确去重
                unique_key = (tick["time"], tick["price"], tick["volume"], tick["sideCode"])
                if unique_key in last_seen:
                    continue

                last_seen.add(unique_key)
                new_count += 1

                # 输出 JSON 数据至 stdout
                print(json.dumps(tick, ensure_ascii=False, separators=(",", ":")))
                sys.stdout.flush()

            if new_count > 0:
                logger.info(f"数据更新 | 新增: {new_count} 条 | 内存池: {len(last_seen)}")

            # 内存回收，防止长期运行占用过大
            if len(last_seen) > 5000:
                last_seen = set(list(last_seen)[-1000:])
                logger.info("执行内存池清理")

            time.sleep(1.5)

    except KeyboardInterrupt:
        logger.info("用户终止任务")
    except Exception as e:
        logger.critical(f"系统异常崩溃: {e}", exc_info=True)