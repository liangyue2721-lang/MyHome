#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 索引位置,对应字段,参数名称,示例值,说明
# parts[0],f51,time,09:48:14,成交时刻
# parts[1],f52,price,58.78,成交价格
# parts[2],f53,volume,11857,成交总量（股）
# parts[3],f54,tickCount,2845,该时刻包含的原始订单笔数
# parts[4],f55,sideCode,1,"1:买, 2:卖, 4:中性"
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

        fh = TimedRotatingFileHandler(os.path.join(LOG_DIR, "tick_worker.log"), when="midnight", encoding="utf-8", backupCount=30)
        fh.setFormatter(file_formatter)

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

def standardize_tick(data, stock_code):
    """标准化成交数据格式"""
    details = data.get("details", [])
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}

    for item in details:
        parts = item.split(",")
        if len(parts) < 5: continue
        try:
            raw_side = parts[4]
            result.append({
                "stockCode": stock_code,
                "companyName": "",
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

# -------------------- 多股运行逻辑 --------------------

def start_multi_monitor(secid_list):
    """
    初始化每只股票的状态并启动主循环
    """
    # 预处理每只股票的配置
    stock_tasks = []
    for sid in secid_list:
        clean_code = sid.split('.')[-1] if '.' in sid else sid
        stock_tasks.append({
            "secid": sid,
            "code": clean_code,
            "last_seen": set()
        })

    logger.info(f"==== 监控启动 | 股票总数: {len(stock_tasks)} ====")

    with sync_playwright() as p:
        # 性能优化：在循环外启动浏览器并常驻
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(locale="zh-CN", user_agent=random_ua())
        page = context.new_page()

        # 预热首页
        try:
            page.goto("https://quote.eastmoney.com/", timeout=10000)
            time.sleep(1)
        except:
            pass

        try:
            while True:
                for task in stock_tasks:
                    sid = task["secid"]
                    code = task["code"]
                    api_url = build_f1_url(sid)

                    try:
                        # 核心抓取：在同一个 Page 中跳转，大幅提高速度
                        page.goto(api_url, timeout=10000, wait_until="domcontentloaded")
                        text = page.evaluate("() => document.body.innerText || ''").strip()

                        if not text or text.startswith("<"):
                            continue

                        parsed = parse_json_or_jsonp(text)
                        if not parsed:
                            continue

                        data = parsed.get("data") or {}
                        ticks = standardize_tick(data, code)

                        new_count = 0
                        for tick in ticks:
                            # 独立股票的唯一键去重
                            unique_key = (tick["time"], tick["price"], tick["volume"], tick["sideCode"])
                            if unique_key in task["last_seen"]:
                                continue

                            task["last_seen"].add(unique_key)
                            new_count += 1

                            # 输出 JSON 数据
                            print(json.dumps(tick, ensure_ascii=False, separators=(",", ":")))
                            sys.stdout.flush()

                        if new_count > 0:
                            logger.info(f"代码: {code} | 更新: {new_count} 条 | 内存池: {len(task['last_seen'])}")

                        # 内存回收
                        if len(task["last_seen"]) > 5000:
                            task["last_seen"] = set(list(task["last_seen"])[-1000:])

                    except Exception as e:
                        logger.warning(f"代码 {code} 处理异常: {e}")
                        continue

                # 轮询间隔：根据股票数量微调，防止请求过快被封
                time.sleep(1.0)

        except KeyboardInterrupt:
            logger.info("用户终止任务")
        finally:
            browser.close()

# -------------------- 主入口 --------------------

if __name__ == "__main__":
    # 获取命令行传入的所有参数
    args = sys.argv[1:]

    if not args:
        print("Usage: python realtime_tick_v2.py <secid1> <secid2> ...")
        print("Example: python realtime_tick_v2.py 1.002261 1.601138")
        sys.exit(0)

    start_multi_monitor(args)