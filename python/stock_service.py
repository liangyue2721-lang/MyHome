#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v3.4 (CORS Bypass & Detailed Debug)
【核心修复 - 解决 JS Fetch returned null】
1. 暴力解锁：添加 --disable-web-security 参数，关闭浏览器的跨域(CORS)检查。
2. 错误透传：JS 执行失败时返回具体错误信息，不再静默返回 null。
3. 容错增强：保留了 v3.3 的隐形模式和持久化会话。
"""

import os
import json
import logging
import asyncio
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

# 调试建议开启窗口，观察是否出现"您使用的是不受支持的命令行标记"提示（正常现象）
HEADLESS = False

POOL_SIZE = 4
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

        context = await PW_OBJ.chromium.launch_persistent_context(
            user_data_dir=worker_user_dir,
            channel="chrome",
            headless=HEADLESS,
            ignore_default_args=["--enable-automation"],
            args=[
                # ================= 关键修复 =================
                "--disable-web-security",  # 关闭跨域检查
                "--disable-features=IsolateOrigins,site-per-process",  # 关闭站点隔离
                "--disable-site-isolation-trials",
                # ===========================================
                "--no-sandbox",
                "--disable-infobars",
                "--disable-blink-features=AutomationControlled",
                "--disable-gpu",
            ],
            viewport={"width": 800, "height": 600}
        )

        await context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        pages = context.pages
        page = pages[0] if pages else await context.new_page()

        logger.info(f"Worker {index}: Parking...")
        try:
            # 依然访问主页以获取 Cookie，但现在可以跨域请求了
            await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=15000)
        except:
            pass

        logger.info(f">>> Worker {index} Ready (CORS Unlocked).")
        CONTEXTS.append(context)
        return page
    except Exception as e:
        logger.error(f">>> Worker {index} Start Failed: {e}")
        return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global PW_OBJ
    logger.info(">>> System Starting... (CORS Bypass Mode)")
    PW_OBJ = await async_playwright().start()

    for i in range(POOL_SIZE):
        page = await _init_worker(i + 1)
        if page: await PAGE_QUEUE.put(page)

    logger.info(f">>> System Ready. {PAGE_QUEUE.qsize()} Workers.")
    yield

    for ctx in CONTEXTS:
        try:
            await ctx.close()
        except:
            pass
    if PW_OBJ: await PW_OBJ.stop()


app = FastAPI(lifespan=lifespan)


# =========================================================
# 3. 核心 Fetch 逻辑 (增强调试版)
# =========================================================

async def fetch_data_via_js(url: str, rid: str, timeout: float = 15.0):
    page = None
    try:
        page = await asyncio.wait_for(PAGE_QUEUE.get(), timeout=5.0)

        if "eastmoney.com" not in page.url:
            try:
                await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=5000)
            except:
                pass

        # 注入更健壮的 JS 代码，返回具体的错误信息
        result = await page.evaluate(f"""async () => {{
            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), {int(timeout * 1000)});
            
            try {{
                const response = await fetch("{url}", {{
                    method: 'GET',
                    signal: controller.signal,
                    mode: 'cors', // 配合 disable-web-security 生效
                }});
                clearTimeout(id);
                
                if (!response.ok) {{
                    return {{ success: false, error: 'HTTP ' + response.status }};
                }}
                const text = await response.text();
                return {{ success: true, data: text }};
            }} catch (e) {{
                return {{ success: false, error: e.toString() }};
            }}
        }}""")

        if not result or not result.get("success"):
            err = result.get("error", "Unknown JS Error") if result else "Null Result"
            logger.warning(f"[{rid}] JS Fetch Error: {err}")
            # 如果是网络错误，可能是页面死掉了，抛出异常以触发重试或销毁
            raise Exception(f"JS Error: {err}")

        # 解析 JSONP 或 JSON
        text = result["data"]
        # 简单清洗 JSONP
        try:
            if text.strip().startswith("jQuery") or text.strip().startswith("callback"):
                match = re.search(r"^\s*(?:jQuery\w+|callback|cb\w*)\s*\((.*)\)\s*;?\s*$", text, flags=re.S)
                if match:
                    return json.loads(match.group(1))
            return json.loads(text)
        except:
            # 可能是纯文本或其他格式，视情况而定，这里假设必须是 JSON
            logger.warning(f"[{rid}] Parse JSON Failed: {text[:50]}...")
            raise Exception("Parse Error")

    except Exception as e:
        if page and page.is_closed(): page = None
        raise HTTPException(status_code=502, detail=f"Fetch Failed: {str(e)}")

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


@app.get("/health")
def health():
    return {"status": "ok", "workers": PAGE_QUEUE.qsize(), "mode": "CORS Unlocked"}


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    ts = int(time.time() * 1000)
    url = f"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f58,f57,f43,f59,f60,f46,f44,f45,f47,f48,f52,f152&secid={req.secid}&ut=fa5fd1943c7b386f172d6893dbfba10b&cb=jQuery{ts}&_={ts}"
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

    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, reload=False, limit_concurrency=100, access_log=False)
