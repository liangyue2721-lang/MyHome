#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v3.3 (Stealth JS Fetch)
【核心修复 - 解决 ERR_EMPTY_RESPONSE】
1. 隐形模式：通过 ignore_default_args=["--enable-automation"] 彻底移除自动化特征。
2. 注入式请求：不再使用 page.goto 跳转 API，而是停留在主页，通过页内 JS (fetch) 发起请求。
   这是模拟真实用户的终极方案，拥有完美的 Referer 和 Origin。
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
from playwright.async_api import async_playwright, Page

# =========================================================
# 0. 核心配置
# =========================================================

# 设为 False 以便您能看到窗口，确认它打开了东财主页
HEADLESS = False

# 窗口数量 (建议 2-4)
POOL_SIZE = 2

# 数据目录
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
# 2. 浏览器池管理
# =========================================================

PW_OBJ = None
CONTEXTS = []
PAGE_QUEUE = asyncio.Queue()


async def _init_worker(index: int) -> Page:
    try:
        worker_user_dir = os.path.join(USER_DATA_DIR, f"worker_{index}")
        os.makedirs(worker_user_dir, exist_ok=True)

        # 启动持久化上下文
        context = await PW_OBJ.chromium.launch_persistent_context(
            user_data_dir=worker_user_dir,
            channel="chrome",  # 强制使用本机 Chrome
            headless=HEADLESS,
            # 【关键】移除自动化标志，防止被识别
            ignore_default_args=["--enable-automation"],
            args=[
                "--no-sandbox",
                "--disable-infobars",
                "--disable-blink-features=AutomationControlled",
                "--disable-gpu",
            ],
            viewport={"width": 800, "height": 600}
        )

        # 再次确保 navigator.webdriver 为 undefined
        await context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        pages = context.pages
        page = pages[0] if pages else await context.new_page()

        # 【关键步骤】Worker 必须停留在东财域名下，才能发送合法的 JS 请求
        logger.info(f"Worker {index}: Park on Eastmoney Homepage...")
        try:
            # 访问主页，并驻留在此
            await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=15000)
        except:
            pass

        logger.info(f">>> Worker {index} Ready (Stealth Mode).")
        CONTEXTS.append(context)
        return page
    except Exception as e:
        logger.error(f">>> Worker {index} Start Failed: {e}")
        return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global PW_OBJ
    logger.info(">>> System Starting... (Stealth JS Mode)")
    PW_OBJ = await async_playwright().start()

    for i in range(POOL_SIZE):
        page = await _init_worker(i + 1)
        if page: await PAGE_QUEUE.put(page)

    logger.info(f">>> System Ready. {PAGE_QUEUE.qsize()} Workers.")
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
# 3. 核心 Fetch 逻辑 (JS 注入版)
# =========================================================

async def fetch_data_via_js(url: str, rid: str, timeout: float = 15.0):
    page = None
    try:
        page = await asyncio.wait_for(PAGE_QUEUE.get(), timeout=5.0)

        # 确保页面还在活着（且在东财域名下）
        if "eastmoney.com" not in page.url:
            try:
                await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=5000)
            except:
                pass

        # 【核心修改】不在地址栏跳转 URL，而是直接在当前页面执行 JS fetch
        # 这样浏览器会自动带上所有正确的指纹、Cookies 和 Referer
        # 这比 page.goto(api_url) 隐蔽得多
        json_data = await page.evaluate(f"""async () => {{
            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), {int(timeout * 1000)});
            
            try {{
                const response = await fetch("{url}", {{
                    method: 'GET',
                    signal: controller.signal,
                    headers: {{
                        'Accept': '*/*',
                        // 甚至不需要手动加 User-Agent，浏览器会自动加
                    }}
                }});
                clearTimeout(id);
                if (!response.ok) return null;
                return await response.json();
            }} catch (e) {{
                return null; 
            }}
        }}""")

        if not json_data:
            # 如果 JS fetch 失败，可能是跨域或超时，记录一下
            logger.warning(f"[{rid}] JS Fetch returned null.")
            raise Exception("Empty Data")

        return json_data

    except Exception as e:
        # logger.error(f"[{rid}] Error: {e}")
        # 如果页面崩溃，尝试重启
        if page and page.is_closed():
            # 重新初始化逻辑略复杂，这里简单处理：标记废弃，后续 worker 数减少
            page = None
        raise HTTPException(status_code=502, detail="Fetch Failed")

    finally:
        if page and not page.is_closed():
            await PAGE_QUEUE.put(page)


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
    return {"status": "ok", "workers": PAGE_QUEUE.qsize(), "mode": "Stealth JS Injection"}


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    ts = int(time.time() * 1000)
    url = f"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f58,f57,f43,f59,f60,f46,f44,f45,f47,f48,f52,f152&secid={req.secid}&ut=fa5fd1943c7b386f172d6893dbfba10b&cb=jQuery{ts}&_={ts}"
    # 使用新方法
    raw = await fetch_data_via_js(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}) if raw else {}


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    raw = await fetch_data_via_js(req.url, uuid.uuid4().hex[:6])
    return raw.get("data", {}) if raw else {}


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    raw = await fetch_data_via_js(req.url, uuid.uuid4().hex[:6])
    return raw.get("data", {}) if raw else {}


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100&cb=jQuery&_={int(time.time())}"
    raw = await fetch_data_via_js(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("details", []) if raw else []


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await fetch_data_via_js(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("klines", []) if raw else []


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeReq, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20991231"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await fetch_data_via_js(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("klines", []) if raw else []


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await fetch_data_via_js(url, uuid.uuid4().hex[:6])
    return raw.get("data", {}).get("klines", []) if raw else []


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    return await fetch_data_via_js(req.url, uuid.uuid4().hex[:6])


if __name__ == "__main__":
    import uvicorn

    # access_log=False 减少干扰
    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, reload=False, limit_concurrency=100, access_log=False)
