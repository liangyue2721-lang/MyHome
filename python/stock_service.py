#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v3.2 (Native Chrome Hook)
【核心修复】
1. 真机伪装：抛弃 Chromium，强制调用本机安装的 Google Chrome (channel="chrome")。
2. 解决白屏：利用原生 Chrome 的 TLS 指纹绕过服务端对自动化工具的识别 (ERR_EMPTY_RESPONSE)。
3. 用户配置持久化：保存 Cookie 和 Session，越用越稳定。
"""

import os
import json
import logging
import asyncio
import re
import time
import uuid
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, BrowserContext, Page

# =========================================================
# 0. 核心配置
# =========================================================

# 必须设为 False 才能调用本地 Chrome 界面
HEADLESS = False

# 并发窗口数 (建议 2-4，取决于电脑性能)
POOL_SIZE = 2

# 数据目录 (用于保存 Chrome 的登录状态和 Cookie)
USER_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "chrome_user_data")

# =========================================================
# 1. 日志系统
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name: str, filename: str):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.handlers.clear()
    handler = TimedRotatingFileHandler(os.path.join(LOG_DIR, filename), when="midnight", interval=1, backupCount=7,
                                       encoding="utf-8")
    handler.setFormatter(logging.Formatter("[%(asctime)s] %(message)s"))
    logger.addHandler(handler)
    return logger


logger = _build_logger("stock", "stock.log")
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# 2. 浏览器池管理 (持久化上下文)
# =========================================================

PW_OBJ = None
CONTEXTS = []  # 存储浏览器上下文
PAGE_QUEUE = asyncio.Queue()


async def _init_worker(index: int) -> Page:
    """启动一个基于本地 Chrome 的 Worker"""
    try:
        # 为每个 Worker 创建独立的用户目录，防止锁冲突
        worker_user_dir = os.path.join(USER_DATA_DIR, f"worker_{index}")
        os.makedirs(worker_user_dir, exist_ok=True)

        # 核心修改：launch_persistent_context + channel="chrome"
        context = await PW_OBJ.chromium.launch_persistent_context(
            user_data_dir=worker_user_dir,
            channel="chrome",  # <--- 关键：强制使用本机 Google Chrome
            headless=HEADLESS,
            args=[
                "--disable-blink-features=AutomationControlled",  # 去除自动化特征
                "--no-sandbox",
                "--disable-infobars",
                "--disable-gpu",  # 如果白屏严重，尝试注释掉这就话
            ],
            viewport={"width": 1000, "height": 800},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        )

        # 再次注入防检测脚本
        await context.add_init_script("() => { Object.defineProperty(navigator, 'webdriver', {get: () => undefined}) }")

        # 获取默认页面或新建
        pages = context.pages
        page = pages[0] if pages else await context.new_page()

        # 预热：访问一次主页，获取真实 Cookie
        logger.info(f"Worker {index}: Warming up (Loading Eastmoney)...")
        try:
            await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=15000)
            await asyncio.sleep(2)  # 等待一下，让指纹生效
        except:
            pass

        logger.info(f">>> Worker {index} Ready (Native Chrome).")
        CONTEXTS.append(context)  # 保存引用防止被回收
        return page
    except Exception as e:
        logger.error(f">>> Worker {index} Start Failed: {e}")
        return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global PW_OBJ
    logger.info(">>> System Starting... (Launching Native Chrome)")
    PW_OBJ = await async_playwright().start()

    # 初始化 Workers
    for i in range(POOL_SIZE):
        page = await _init_worker(i + 1)
        if page: await PAGE_QUEUE.put(page)

    logger.info(f">>> System Ready. {PAGE_QUEUE.qsize()} Workers available.")
    yield

    logger.info(">>> Shutdown...")
    for ctx in CONTEXTS:
        try:
            await ctx.close()
        except:
            pass
    if PW_OBJ: await PW_OBJ.stop()


app = FastAPI(lifespan=lifespan)


# =========================================================
# 3. 核心 Fetch 逻辑
# =========================================================

def parse_json(text: str):
    if not text: return None
    try:
        return json.loads(text)
    except:
        pass
    match = re.search(r"^\s*(?:jQuery\w+|callback|cb\w*)\s*\((.*)\)\s*;?\s*$", text, flags=re.S)
    if match:
        try:
            return json.loads(match.group(1))
        except:
            pass
    return None


async def fetch_data(url: str, rid: str, timeout: float = 15.0):
    page = None
    try:
        page = await asyncio.wait_for(PAGE_QUEUE.get(), timeout=5.0)

        # 导航
        response = await page.goto(url, wait_until="domcontentloaded", timeout=int(timeout * 1000))

        # 检查 HTTP 状态码
        if response and response.status != 200:
            logger.warning(f"[{rid}] HTTP Status: {response.status}")

        # 获取内容
        content = await page.evaluate("document.body.innerText")

        # 针对 Chrome JSON 视图的特殊处理（有时候 Chrome 会把 JSON 美化展示）
        if "{" not in content and "[" not in content:
            # 尝试从 pre 标签获取（Chrome 默认行为）
            content = await page.evaluate("""() => {
                 let pre = document.querySelector('pre');
                 return pre ? pre.innerText : document.body.innerText;
             }""")

        data = parse_json(content)

        if not data:
            # 最后的挣扎：如果是 ERR_EMPTY_RESPONSE，这里通常会抛错
            logger.warning(f"[{rid}] Data Empty. Content: {content[:100]}")
            raise Exception("Empty Data")

        return data

    except Exception as e:
        err_msg = str(e)
        logger.error(f"[{rid}] Error: {err_msg}")
        # 如果是连接被重置或关闭，销毁该 worker 上下文（此处简化为放回，依靠重试机制）
        # 实际生产中应该重启该 Context
        raise HTTPException(status_code=502, detail=f"Fetch Failed: {err_msg}")

    finally:
        if page: await PAGE_QUEUE.put(page)


# =========================================================
# 4. 接口定义
# =========================================================

class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: int


class KlineRangeReq(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineReq(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class TickReq(BaseModel): secid: str


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


# --- Endpoints ---

@app.get("/health")
def health():
    return {"status": "ok", "workers": PAGE_QUEUE.qsize(), "mode": "Native Chrome"}


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    ts = int(time.time() * 1000)
    url = f"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f58,f57,f43,f59,f60,f46,f44,f45,f47,f48,f52,f152&secid={req.secid}&ut=fa5fd1943c7b386f172d6893dbfba10b&cb=jQuery{ts}&_={ts}"
    raw = await fetch_data(url, uuid.uuid4().hex[:6])
    return raw.get("data", {})


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    raw = await fetch_data(req.url, uuid.uuid4().hex[:6])
    return raw.get("data", {})


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    raw = await fetch_data(req.url, uuid.uuid4().hex[:6])
    return raw.get("data", {})


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100&cb=jQuery&_={int(time.time())}"
    raw = await fetch_data(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("details", [])


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    # K线数据
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await fetch_data(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("klines", [])


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeReq, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20991231"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await fetch_data(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("klines", [])


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await fetch_data(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("klines", [])


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    return await fetch_data(req.url, uuid.uuid4().hex[:6])


if __name__ == "__main__":
    import uvicorn

    # 为了防止控制台乱码和报错，禁用 access log
    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, reload=False, limit_concurrency=100, access_log=False)
