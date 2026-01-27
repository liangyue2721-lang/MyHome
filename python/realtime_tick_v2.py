#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import json
import re
import time
import random
import logging
import os
from logging.handlers import TimedRotatingFileHandler
from playwright.sync_api import sync_playwright

# -------------------- 日志高级配置 --------------------

def setup_logger():
    """
    配置双路日志系统：
    - FileHandler: 每天滚动记录，保留30天，存放在 logs/tick_worker.log
    - StreamHandler: 实时输出到控制台，便于调试
    """
    LOG_DIR = "logs"
    os.makedirs(LOG_DIR, exist_ok=True)

    logger = logging.getLogger("tick-realtime")
    logger.setLevel(logging.INFO)

    # 避免重复添加 Handler
    if not logger.handlers:
        # 文件日志格式：包含详细时间、级别和具体信息
        file_formatter = logging.Formatter(
            fmt="[%(asctime)s] [%(levelname)s] %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )
        # 控制台日志格式：更加简洁
        console_formatter = logging.Formatter(
            fmt="%(asctime)s | %(levelname)s | %(message)s",
            datefmt="%H:%M:%S"
        )

        # 文件滚动处理器
        fh = TimedRotatingFileHandler(
            os.path.join(LOG_DIR, "tick_worker.log"),
            when="midnight",
            encoding="utf-8",
            backupCount=30
        )
        fh.setFormatter(file_formatter)

        # 控制台处理器
        ch = logging.StreamHandler()
        ch.setFormatter(console_formatter)

        logger.addHandler(fh)
        logger.addHandler(ch)

    logger.propagate = False
    return logger

logger = setup_logger()

# 浏览器 User-Agent 池，用于模拟真实用户
UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
]

def random_ua():
    """随机获取一个 User-Agent"""
    return random.choice(UA_POOL)

def parse_json_or_jsonp(text):
    """
    解析原始响应字符串，支持标准 JSON 和带回调函数的 JSONP 格式。

    Args:
        text (str): 接口返回的原始文本
    Returns:
        dict: 解析后的字典对象，失败则返回 None
    """
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except:
        pass
    # 匹配类似 jQueryxxxx(...) 的格式
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if not m:
        return None
    try:
        return json.loads(m.group(1))
    except:
        return None

# -------------------- 核心抓取逻辑 --------------------

def fetch_json_by_browser(api_url, max_retry=3):
    """
    使用 Playwright 模拟浏览器访问接口，以绕过简单的反爬指纹校验。

    Args:
        api_url (str): 目标 API 的完整 URL
        max_retry (int): 最大重试次数
    Returns:
        dict: 返回的 JSON 数据包，失败返回 None
    """
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # 模拟真实的浏览器环境
        context = browser.new_context(locale="zh-CN", user_agent=random_ua())
        # 屏蔽 webdriver 特征
        context.add_init_script(
            "() => Object.defineProperty(navigator, 'webdriver', {get: () => false})"
        )
        page = context.new_page()

        # 预热首页，建立会话上下文
        try:
            page.goto("https://quote.eastmoney.com/", timeout=15000)
            time.sleep(0.5)
        except Exception as e:
            logger.warning(f"预热失败(通常可忽略): {e}")

        for attempt in range(1, max_retry + 1):
            start_time = time.time()
            try:
                logger.debug(f"正在尝试抓取第 {attempt} 次: {api_url}")
                page.goto(api_url, timeout=20000, wait_until="domcontentloaded")

                # 获取网页正文内容
                text = page.evaluate("() => document.body.innerText || ''").strip()
                duration = (time.time() - start_time) * 1000

                if not text:
                    raise Exception("响应体为空")
                if text.startswith("<"):
                    raise Exception("返回了 HTML 而非 JSONP，可能触发封禁")

                parsed = parse_json_or_jsonp(text)
                if parsed is None:
                    raise Exception("数据解析失败")

                logger.info(f"抓取成功 | 耗时: {duration:.0f}ms | 数据长度: {len(text)}")
                return parsed

            except Exception as e:
                logger.error(f"抓取尝试 {attempt} 失败: {str(e)}")
                time.sleep(1 + random.random())

        browser.close()
        return None

def standardize_tick(data):
    """
    将东方财富 F1 接口的原始数据标准化为统一的字典格式。

    Args:
        data (dict): API 返回的 'data' 节点内容
    Returns:
        list: 包含 stockCode, companyName, time, price, volume 的字典列表
    """
    stock_code = data.get("f57")
    company_name = data.get("f58")
    details = data.get("details", [])

    result = []
    for item in details:
        parts = item.split(",")
        if len(parts) < 3:
            continue

        try:
            result.append({
                "stockCode": stock_code,
                "companyName": company_name,
                "time": parts[0],
                "price": float(parts[1]),
                "volume": int(parts[2])
            })
        except (ValueError, IndexError):
            continue

    return result

def build_f1_url(secid, pos=-2000, num=2000):
    """
    构造东方财富逐笔成交 (F1) 的 API 请求地址。

    Args:
        secid (str): 市场代码.股票代码 (如 1.601138)
        pos (int): 起始位置，负数表示从最新记录往前取
        num (int): 获取条数
    Returns:
        str: 拼接完成的 URL
    """
    base = "https://push2.eastmoney.com/api/qt/stock/details/get"
    # ut 和 cb 是目前接口正常访问的关键
    timestamp = int(time.time() * 1000)
    params = {
        "secid": secid,
        "ut": "bd1d9ddb04089700cf9c27f6f7426281",
        "fields1": "f1,f2,f3,f4,f5",
        "fields2": "f51,f52,f53",
        "pos": pos,
        "num": num,
        "cb": f"jQuery112401614769018445103_{timestamp}",
        "_": timestamp
    }
    param_str = "&".join([f"{k}={v}" for k, v in params.items()])
    return f"{base}?{param_str}"

# -------------------- 主执行逻辑 --------------------

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python realtime_tick_v2.py <secid>")
        print("Example: python realtime_tick_v2.py 1.601138")
        sys.exit(0)

    secid = sys.argv[1].strip()
    api_url = build_f1_url(secid)

    logger.info(f"==== 实时抓取启动: {secid} ====")

    # 使用 set 记录已处理的 Tick 唯一标识，防止重复输出
    last_seen = set()

    try:
        while True:
            parsed = fetch_json_by_browser(api_url)
            if not parsed:
                logger.warning("本次未能获取有效数据，等待 5s 后重试...")
                time.sleep(5)
                continue

            data = parsed.get("data", {}) or {}
            ticks = standardize_tick(data)

            new_ticks_count = 0
            for tick in ticks:
                # 唯一键：时间+价格+成交量 (在极短时间内完全相同的成交通常视为同一笔)
                key = (tick["time"], tick["price"], tick["volume"])
                if key in last_seen:
                    continue

                last_seen.add(key)
                new_ticks_count += 1

                # 数据结果输出到 stdout (不带日志前缀，方便下游解析)
                print(json.dumps(tick, ensure_ascii=False, separators=(",", ":")))
                sys.stdout.flush()

            if new_ticks_count > 0:
                logger.info(f"数据更新 | 新增: {new_ticks_count} 条 | 内存池大小: {len(last_seen)}")

            # 内存回收逻辑：当记录超过 5000 条时，保留最新的 1000 条，防止长时间运行 OOM
            if len(last_seen) > 5000:
                old_size = len(last_seen)
                last_seen = set(list(last_seen)[-1000:])
                logger.info(f"内存清理 | 记录从 {old_size} 清减至 {len(last_seen)}")

            # 轮询间隔：1.5秒是比较均衡的选择
            time.sleep(1.5)

    except KeyboardInterrupt:
        logger.info("用户终止任务，安全退出中...")
    except Exception as e:
        logger.critical(f"系统因不可控错误崩溃: {str(e)}", exc_info=True)
    finally:
        logger.info("==== 任务已停止 ====")