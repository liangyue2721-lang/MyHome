#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v2.5 (High Load Stability)
【负载优化重点】
1. 弹性资源池：页面崩溃或超时后自动销毁并补充新页面，防止脏页面循环利用。
2. 内存保护：增加浏览器启动参数，减少高并发下的崩溃概率。
3. 队列优化：增加池大小(10)与超时阈值(30s)，适应压测场景。
4. 接口全量：包含所有业务接口 (Snapshot/Kline/Tick/Proxy等)。
"""

import os
import json
import logging
import asyncio
import re
import time
import uuid
import random
from contextlib import asynccontextmanager
from datetime import datetime, timedelta
from typing import Optional, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
import httpx
from playwright.async_api import (
    async_playwright,
    Browser,
    Playwright,
    Page
)

# =========================================================
# 0. 配置参数 (根据负载调整)
# =========================================================

# 页面池大小 (太大会耗尽内存，太小并发上不去，建议 8-15)
POOL_SIZE = 10
# 获取页面等待超时 (秒)
QUEUE_TIMEOUT = 30.0
# 浏览器页面加载超时 (毫秒)
PAGE_TIMEOUT = 25000
# HTTP 请求超时 (秒)
HTTP_TIMEOUT = 8.0

# =========================================================
# 1. 日志系统
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name: str, filename: str, fmt: logging.Formatter):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.handlers.clear()
    handler = TimedRotatingFileHandler(
        os.path.join(LOG_DIR, filename),
        when="midnight",
        interval=1,
        backupCount=14,
        encoding="utf-8",
    )
    handler.setFormatter(fmt)
    logger.addHandler(handler)
    return logger


logger = _build_logger("stock-service", "stock_service.log",
                       logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s"))
access_logger = _build_logger("access", "access.log", logging.Formatter("%(message)s"))
trace_logger = _build_logger("trace", "trace.log",
                             logging.Formatter("[%(asctime)s] [%(levelname)s] [%(request_id)s] %(message)s"))
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# 2. 全局状态
# =========================================================

PW_OBJ: Optional[Playwright] = None
BROWSER: Optional[Browser] = None
# 限制同时进行浏览器操作的并发数，通常略大于或等于 POOL_SIZE
SEMAPHORE: Optional[asyncio.Semaphore] = None
PAGE_POOL: asyncio.Queue = asyncio.Queue()
HTTP_CLIENT: Optional[httpx.AsyncClient] = None

AUTH_LOCK = asyncio.Lock()
CACHED_HEADERS = {}
LAST_AUTH_TIME = 0


# =========================================================
# 3. 核心工具与生命周期
# =========================================================

class TraceAdapter(logging.LoggerAdapter):
    def process(self, msg, kwargs):
        kwargs.setdefault("extra", {})
        kwargs["extra"]["request_id"] = self.extra["request_id"]
        return msg, kwargs


async def _create_new_page(browser: Browser) -> Optional[Page]:
    """创建一个经过初始化的新页面"""
    try:
        context = await browser.new_context(
            locale="zh-CN",
            ignore_https_errors=True,
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            viewport={"width": 1280, "height": 720},  # 固定视窗节省资源
        )
        # 注入防检测脚本
        await context.add_init_script("() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})")
        page = await context.new_page()
        # 预加载，初始化 Cookie
        await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=PAGE_TIMEOUT)
        return page
    except Exception as e:
        logger.error(f"Failed to create new page: {e}")
        return None


async def _refresh_auth_headers_if_needed():
    """后台刷新 Headers"""
    global CACHED_HEADERS, LAST_AUTH_TIME
    if time.time() - LAST_AUTH_TIME < 60 and CACHED_HEADERS:
        return

    logger.info(">>> [Auth] Refreshing Headers...")
    async with async_playwright() as p:
        # 使用轻量级 args 启动临时浏览器
        auth_browser = await p.chromium.launch(
            headless=True,
            args=["--no-sandbox", "--disable-setuid-sandbox", "--disable-gpu"]
        )
        try:
            context = await auth_browser.new_context()
            page = await context.new_page()
            captured = {"headers": {}}

            async def handle_request(request):
                if captured["headers"]: return
                try:
                    if "api/qt/stock" in request.url:
                        h = await request.all_headers()
                        valid = {k: v for k, v in h.items()
                                 if not k.startswith(':') and k.lower() not in ['content-length', 'host']}
                        cookies = await context.cookies()
                        valid["cookie"] = "; ".join([f"{c['name']}={c['value']}" for c in cookies])
                        captured["headers"] = valid
                except:
                    pass

            page.on("request", handle_request)
            try:
                await page.goto("https://quote.eastmoney.com/sz000001.html", wait_until="networkidle", timeout=15000)
            except:
                pass

            if captured["headers"]:
                CACHED_HEADERS = captured["headers"]
                LAST_AUTH_TIME = time.time()
                logger.info(">>> [Auth] Headers Updated")
        except Exception as e:
            logger.warning(f">>> [Auth] Update failed: {e}")
        finally:
            await auth_browser.close()


@asynccontextmanager
async def lifespan(app: FastAPI):
    global PW_OBJ, BROWSER, SEMAPHORE, HTTP_CLIENT

    # 1. HTTP Client
    HTTP_CLIENT = httpx.AsyncClient(
        headers={"User-Agent": "Mozilla/5.0"},
        verify=False,
        timeout=HTTP_TIMEOUT,
        follow_redirects=True,
        limits=httpx.Limits(max_keepalive_connections=20, max_connections=50)  # 连接池优化
    )

    # 2. Playwright Launch (内存优化参数)
    logger.info("Starting Browser Engine...")
    PW_OBJ = await async_playwright().start()
    BROWSER = await PW_OBJ.chromium.launch(
        headless=True,
        args=[
            "--disable-blink-features=AutomationControlled",
            "--disable-web-security",
            "--ignore-certificate-errors",
            "--no-sandbox",
            "--disable-setuid-sandbox",
            "--disable-dev-shm-usage",  # 防止容器内崩溃
            "--disable-gpu",  # 节省资源
            "--disable-extensions",
        ],
    )

    SEMAPHORE = asyncio.Semaphore(POOL_SIZE + 5)  # 允许稍微多一点的等待

    # 3. 初始化页面池
    logger.info(f"Initializing Page Pool (Target: {POOL_SIZE})...")
    await _refresh_auth_headers_if_needed()

    # 并发创建页面以加快启动
    tasks = [_create_new_page(BROWSER) for _ in range(POOL_SIZE)]
    pages = await asyncio.gather(*tasks)
    for p in pages:
        if p: await PAGE_POOL.put(p)

    logger.info(f"Service Ready. Pool Size: {PAGE_POOL.qsize()}")

    yield

    # Cleanup
    logger.info("Stopping...")
    if HTTP_CLIENT: await HTTP_CLIENT.aclose()

    # 快速关闭所有页面
    while not PAGE_POOL.empty():
        try:
            p = PAGE_POOL.get_nowait()
            await p.close()
        except:
            pass

    if BROWSER: await BROWSER.close()
    if PW_OBJ: await PW_OBJ.stop()


app = FastAPI(title="Stock Service v2.5", lifespan=lifespan)


# =========================================================
# 4. 中间件
# =========================================================

@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    request_id = uuid.uuid4().hex[:12]
    request.state.request_id = request_id
    start_time = time.time()
    try:
        response = await call_next(request)
        status = response.status_code
        return response
    except Exception as e:
        status = 500
        access_logger.error(json.dumps({"rid": request_id, "error": str(e)}))
        raise
    finally:
        cost = int((time.time() - start_time) * 1000)
        access_logger.info(json.dumps({
            "ts": datetime.utcnow().isoformat(),
            "rid": request_id,
            "path": request.url.path,
            "status": status,
            "cost": cost
        }))


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


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


# =========================================================
# 5. 双模 Fetch (增强版)
# =========================================================

async def smart_fetch(url: str, request_id: str) -> Dict[str, Any]:
    trace = TraceAdapter(trace_logger, {"request_id": request_id})

    # Mode 1: HTTPX
    if CACHED_HEADERS:
        try:
            safe_headers = {k: v for k, v in CACHED_HEADERS.items()
                            if k.lower() not in ['content-encoding', 'content-length', 'host']}
            resp = await HTTP_CLIENT.get(url, headers=safe_headers)
            if resp.status_code == 200:
                data = parse_json(resp.text)
                if data and isinstance(data, dict):
                    if "rc" in data and str(data["rc"]) != "0":
                        trace.warning(f"[HTTP] BizError rc={data.get('rc')}, switching...")
                    else:
                        return data
                else:
                    trace.warning("[HTTP] Parse Error")
            else:
                trace.warning(f"[HTTP] Status {resp.status_code}")
        except Exception as e:
            trace.warning(f"[HTTP] Error: {e}")
    else:
        trace.info("[HTTP] No Headers")

    # Mode 2: Browser
    return await fetch_with_browser(url, request_id)


async def _replenish_pool():
    """异步补充页面池"""
    if BROWSER:
        page = await _create_new_page(BROWSER)
        if page:
            await PAGE_POOL.put(page)
            logger.info("Pool Replenished (+1)")


async def fetch_with_browser(url: str, request_id: str, max_retry: int = 2):
    trace = TraceAdapter(trace_logger, {"request_id": request_id})
    page = None

    try:
        # 增加等待时间，应对高并发排队
        page = await asyncio.wait_for(PAGE_POOL.get(), timeout=QUEUE_TIMEOUT)
    except asyncio.TimeoutError:
        trace.error("Pool Exhausted (Timeout)")
        raise HTTPException(status_code=503, detail="System Busy - Try Later")

    is_healthy = True
    try:
        trace.info(f"[Browser] Processing...")
        for attempt in range(1, max_retry + 1):
            try:
                # 显式设置超时，防止挂死
                await page.goto(url, wait_until="domcontentloaded", timeout=PAGE_TIMEOUT)
                content = await page.evaluate("document.body.innerText")
                parsed = parse_json(content)

                if parsed:
                    return parsed

                trace.warning(f"[Browser] Retry {attempt}: Invalid JSON")
                # 随机抖动等待，防止并发撞车
                await asyncio.sleep(random.uniform(0.5, 1.5))

            except Exception as e:
                err = str(e)
                trace.warning(f"[Browser] Retry {attempt} Error: {err}")
                if "Target closed" in err or "Session closed" in err:
                    is_healthy = False
                    break  # 页面崩溃，直接退出

        raise HTTPException(status_code=502, detail="Fetch Failed")

    finally:
        if page:
            if is_healthy:
                # 只有健康的页面才放回池子
                await PAGE_POOL.put(page)
            else:
                # 不健康的页面直接关闭，并触发补充机制
                trace.warning("[Browser] Discarding dirty/crashed page")
                try:
                    await page.close()
                except:
                    pass
                asyncio.create_task(_replenish_pool())


# =========================================================
# 6. 接口定义
# =========================================================

class RealtimeRequest(BaseModel): url: str


class StockSnapshotRequest(BaseModel): secid: str


class TickRequest(BaseModel): secid: str


class KlineRequest(BaseModel): secid: str; ndays: int


class KlineRangeRequest(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineRequest(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class GenericJsonRequest(BaseModel): url: str


# Helper functions
def safe_num(v): return None if v in (None, "", "-") else v


def safe_div(v):
    try:
        return float(v) / 100 if v not in (None, "", "-") else None
    except:
        return None


def fmt_realtime(d: Dict):
    return {
        "stockCode": d.get("f57"), "companyName": d.get("f58"),
        "price": safe_div(d.get("f43")), "prevClose": safe_div(d.get("f60")),
        "openPrice": safe_div(d.get("f46")), "highPrice": safe_div(d.get("f44")),
        "lowPrice": safe_div(d.get("f45")), "volume": safe_num(d.get("f47")),
        "turnover": safe_num(d.get("f48")), "volumeRatio": safe_num(d.get("f52")),
        "mainFundsInflow": safe_num(d.get("f152")),
    }


def fmt_ticks(data: Dict, code: str):
    res = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}
    for item in data.get("details", []):
        try:
            p = item.split(",")
            if len(p) < 5: continue
            res.append({
                "time": p[0], "price": float(p[1]), "volume": int(p[2]),
                "side": side_map.get(p[4], "其他")
            })
        except:
            pass
    return res


# Endpoints
@app.get("/health")
def health():
    return {"status": "ok", "pool_size": PAGE_POOL.qsize(), "mode": "v2.5 HighLoad"}


@app.post("/stock/snapshot")
async def stock_snapshot(req: StockSnapshotRequest, request: Request):
    ts = int(time.time() * 1000)
    # 极速快照接口
    url = f"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f58,f57,f43,f59,f60,f46,f44,f45,f47,f48,f52,f152&secid={req.secid}&ut=fa5fd1943c7b386f172d6893dbfba10b&cb=jQuery{ts}&_={ts}"
    raw = await smart_fetch(url, request.state.request_id)
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return fmt_realtime(raw["data"])


@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest, request: Request):
    raw = await smart_fetch(req.url, request.state.request_id)
    return fmt_realtime(raw.get("data", {}))


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest, request: Request):
    raw = await smart_fetch(req.url, request.state.request_id)
    return fmt_realtime(raw.get("data", {}))


@app.post("/stock/ticks")
async def stock_ticks(req: TickRequest, request: Request):
    secid = normalize_secid(req.secid)
    ts = int(time.time() * 1000)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1000&num=1000&cb=jQuery{ts}&_={ts}"
    raw = await smart_fetch(url, request.state.request_id)
    return fmt_ticks(raw.get("data", {}), secid)


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest, request: Request):
    # 简化版实现，同之前逻辑
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await smart_fetch(url, request.state.request_id)
    return raw.get("data", {}).get("klines", [])


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeRequest, request: Request):
    secid = normalize_secid(req.secid)
    beg, end = req.beg or "0", req.end or "20990101"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await smart_fetch(url, request.state.request_id)
    return raw.get("data", {}).get("klines", [])


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineRequest, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=0&end=20990101&fields1=f1&fields2=f51,f52,f53,f54,f55"
    raw = await smart_fetch(url, request.state.request_id)
    return raw.get("data", {}).get("klines", [])


@app.post("/proxy/json")
async def proxy_json(req: GenericJsonRequest, request: Request):
    data = await smart_fetch(req.url, request.state.request_id)
    if not data: raise HTTPException(502, "Upstream Error")
    return data


if __name__ == "__main__":
    import uvicorn

    # 生产环境建议使用 workers > 1，但配合 Playwright 使用时需谨慎内存
    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, reload=False, limit_concurrency=20)
