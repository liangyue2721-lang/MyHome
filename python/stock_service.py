#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v1.4 (Fixed)
修复：CACHED_HEADERS 未定义报错
新增：自动抓取 Headers 逻辑
"""

import os
import json
import logging
import asyncio
import re
import time
import uuid
from datetime import datetime, timedelta
from typing import Optional, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import (
    async_playwright,
    Browser,
    Error as PlaywrightError,
)

# =========================================================
# 日志系统初始化
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


logger = _build_logger(
    "stock-service",
    "stock_service.log",
    logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s"),
)

access_logger = _build_logger(
    "access",
    "access.log",
    logging.Formatter("%(message)s"),
)

trace_logger = _build_logger(
    "trace",
    "trace.log",
    logging.Formatter(
        "[%(asctime)s] [%(levelname)s] [%(request_id)s] %(message)s"
    ),
)

logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI 应用
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.4.1")

# =========================================================
# 运行期全局对象
# =========================================================

PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE: Optional[asyncio.Semaphore] = None
PAGE_POOL: asyncio.Queue = asyncio.Queue()

# ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
# 【修复点 1】必须在这里定义全局状态变量，否则报错
# ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
AUTH_LOCK = asyncio.Lock()  # 互斥锁，防止同时弹出浏览器
CACHED_HEADERS = {}  # 存储“偷”来的 Headers
LAST_AUTH_TIME = 0  # 上次获取时间


# =========================================================
# Trace 日志适配器 & 中间件
# =========================================================

class TraceAdapter(logging.LoggerAdapter):
    def process(self, msg, kwargs):
        kwargs.setdefault("extra", {})
        kwargs["extra"]["request_id"] = self.extra["request_id"]
        return msg, kwargs


@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    request_id = uuid.uuid4().hex[:12]
    request.state.request_id = request_id

    start_time = time.time()
    status_code = 500
    error = None

    try:
        response = await call_next(request)
        status_code = response.status_code
        return response
    except Exception as e:
        error = str(e)
        raise
    finally:
        access_logger.info(json.dumps({
            "ts": datetime.utcnow().isoformat() + "Z",
            "rid": request_id,
            "path": request.url.path,
            "method": request.method,
            "status": status_code,
            "cost_ms": int((time.time() - start_time) * 1000),
            "error": error,
        }, ensure_ascii=False))


# =========================================================
# 工具函数
# =========================================================

def parse_json_or_jsonp(text: str):
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        pass
    match = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if match:
        try:
            return json.loads(match.group(1))
        except Exception:
            pass
    return None


def normalize_secid(code: str) -> str:
    if "." in code:
        return code
    if code.startswith(("83", "43")):
        return f"113.{code}"
    if code.startswith(("6", "5")):
        return f"1.{code}"
    return f"0.{code}"


# =========================================================
# 生命周期：启动与关闭
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER, SEMAPHORE

    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()

    # 主服务使用 Headless 模式，且必须禁用 Web 安全策略
    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=[
            "--disable-blink-features=AutomationControlled",
            "--disable-web-security",  # 【重要】禁用跨域
            "--disable-features=IsolateOrigins,site-per-process",
        ],
    )

    SEMAPHORE = asyncio.Semaphore(10)
    page_count = 4

    for i in range(page_count):
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        )
        await context.add_init_script("() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})")
        page = await context.new_page()

        try:
            # 预热
            await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded", timeout=30000)
            logger.info(f"Warmup page {i} OK")
        except Exception as e:
            logger.warning(f"Warmup page {i} ignored: {e}")

        await PAGE_POOL.put(page)

    logger.info("Browser started. Page pool ready.")


@app.on_event("shutdown")
async def shutdown():
    while not PAGE_POOL.empty():
        page = await PAGE_POOL.get()
        await page.close()
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()


async def _recreate_page():
    """辅助函数：创建一个新页面放回池中"""
    try:
        if not BROWSER: return
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        await context.add_init_script("() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})")
        new_page = await context.new_page()
        await PAGE_POOL.put(new_page)
        logger.info("Recreated page added to pool.")
    except Exception as e:
        logger.error(f"Failed to recreate page: {e}")


# ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
# 【修复点 2】补充缺失的自动认证抓包函数
# ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
async def _refresh_auth_headers_if_needed():
    """
    启动一个可见的浏览器，访问目标页面，捕获真实的 Request Headers
    """
    global CACHED_HEADERS, LAST_AUTH_TIME

    # 冷却时间 60秒，避免频繁弹窗
    if time.time() - LAST_AUTH_TIME < 60 and CACHED_HEADERS:
        return

    logger.info(">>> 启动可见浏览器进行身份认证抓包 (Headless=False)...")

    # 使用独立的 playwright 实例
    async with await async_playwright().start() as p:
        try:
            # 必须用有头模式
            auth_browser = await p.chromium.launch(
                headless=False,
                args=["--disable-blink-features=AutomationControlled"]
            )
            context = await auth_browser.new_context()
            page = await context.new_page()

            # Future 对象用于接收结果
            headers_future = asyncio.get_running_loop().create_future()

            async def handle_request(request):
                # 监听行情接口，截获 Headers
                if (
                        "api/qt/stock" in request.url or "push2.eastmoney.com" in request.url) and not headers_future.done():
                    headers = await request.all_headers()
                    # 过滤掉不必要的头
                    valid_headers = {
                        k: v for k, v in headers.items()
                        if k.lower() not in [':method', ':scheme', ':authority', ':path', 'content-length']
                    }
                    if not headers_future.done():
                        headers_future.set_result(valid_headers)

            page.on("request", handle_request)

            logger.info("Navigating to capture headers...")
            # 访问平安银行行情页，触发 API
            await page.goto("https://quote.eastmoney.com/sz000001.html", wait_until="domcontentloaded")

            # 最多等待 15 秒
            new_headers = await asyncio.wait_for(headers_future, timeout=15.0)

            if new_headers:
                CACHED_HEADERS = new_headers
                LAST_AUTH_TIME = time.time()
                logger.info(f"Headers captured successfully! Keys: {list(CACHED_HEADERS.keys())}")

        except asyncio.TimeoutError:
            logger.error("Auth browser timed out.")
        except Exception as e:
            logger.error(f"Auth browser failed: {e}")
        finally:
            try:
                await auth_browser.close()
            except:
                pass


# =========================================================
# 核心：基于 Page Pool 的安全 fetch
# =========================================================

async def fetch_json_with_browser(url: str, request_id: str, max_retry: int = 3):
    trace = TraceAdapter(trace_logger, {"request_id": request_id})
    trace.info(f"Fetch start url={url}")

    if PAGE_POOL.empty():
        raise HTTPException(status_code=503, detail="Browser busy")

    async with SEMAPHORE:
        page = await PAGE_POOL.get()
        page_ok = True

        try:
            for attempt in range(1, max_retry + 1):
                try:
                    # 准备 Headers：如果有缓存的“真”Headers，就用它；否则用默认伪造的
                    current_headers = CACHED_HEADERS.copy() if CACHED_HEADERS else {
                        'Accept': '*/*',
                        'Accept-Language': 'zh-CN,zh;q=0.9',
                        'Referer': 'https://quote.eastmoney.com/',
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
                    }

                    # =================================================
                    # 策略 A: JS fetch (动态注入 Headers)
                    # =================================================
                    text = await page.evaluate(
                        """async ([url, headers]) => {
                            const controller = new AbortController();
                            const timer = setTimeout(() => controller.abort(), 8000);

                            try {
                                const r = await fetch(url, {
                                    method: 'GET',
                                    credentials: 'include',
                                    signal: controller.signal,
                                    headers: headers  // 【修复】使用 Python 传入的全局 Headers
                                });
                                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                                return await r.text();
                            } finally {
                                clearTimeout(timer);
                            }
                        }""",
                        [url, current_headers],
                    )

                    parsed = parse_json_or_jsonp(text)
                    if parsed:
                        return parsed

                    if attempt == max_retry:
                        trace.warning(f"Response parsed empty.")

                except Exception as e:
                    error_msg = str(e)
                    trace.warning(f"[Attempt {attempt}] Error: {error_msg}")

                    if "Target closed" in error_msg or "Session closed" in error_msg:
                        page_ok = False
                        break

                    # =================================================
                    # 触发认证刷新逻辑
                    # =================================================
                    if ("Failed to fetch" in error_msg or "HTTP 403" in error_msg) and attempt < max_retry:
                        trace.warning("Detected auth failure. Launching rescue browser...")
                        if not AUTH_LOCK.locked():
                            async with AUTH_LOCK:
                                await _refresh_auth_headers_if_needed()
                        else:
                            await asyncio.sleep(2)

                    # =================================================
                    # 策略 B: page.goto 兜底
                    # =================================================
                    if ("Failed to fetch" in error_msg or attempt == max_retry) and page_ok:
                        try:
                            trace.info(f"[Attempt {attempt}] Fallback to page.goto")
                            resp = await page.goto(url, wait_until="commit", timeout=10000)
                            if resp and resp.ok:
                                text = await page.evaluate("document.body.innerText")
                                parsed = parse_json_or_jsonp(text)
                                if parsed: return parsed
                        except Exception as goto_e:
                            trace.error(f"Goto fallback failed: {goto_e}")

                    await asyncio.sleep(0.3 * attempt)

            raise HTTPException(status_code=502, detail="Upstream fetch failed")

        finally:
            if page_ok:
                await PAGE_POOL.put(page)
            else:
                trace.warning("Discard broken page, recreating...")
                try:
                    await page.close()
                except Exception:
                    pass
                asyncio.create_task(_recreate_page())


# =========================================================
# 请求模型与接口实现 (保持不变)
# =========================================================

class RealtimeRequest(BaseModel):
    url: str


class KlineRequest(BaseModel):
    secid: str
    ndays: int


class KlineRangeRequest(BaseModel):
    secid: str
    beg: Optional[str] = None
    end: Optional[str] = None


class USKlineRequest(BaseModel):
    secid: str
    market: str
    beg: Optional[str] = None
    end: Optional[str] = None


class GenericJsonRequest(BaseModel):
    url: str


class TickRequest(BaseModel):
    secid: str


def safe_get(dct, key):
    return dct.get(key)


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    def _div100(val):
        if val in (None, "", "-"): return None
        try:
            return float(val) / 100
        except Exception:
            return None

    def _num(val):
        return None if val in (None, "", "-") else val

    return {
        "stockCode": safe_get(data, "f57"),
        "companyName": safe_get(data, "f58"),
        "price": _div100(safe_get(data, "f43")),
        "prevClose": _div100(safe_get(data, "f60")),
        "openPrice": _div100(safe_get(data, "f46")),
        "highPrice": _div100(safe_get(data, "f44")),
        "lowPrice": _div100(safe_get(data, "f45")),
        "volume": _num(safe_get(data, "f47")),
        "turnover": _num(safe_get(data, "f48")),
        "volumeRatio": _num(safe_get(data, "f52")),
        "commissionRatio": _num(safe_get(data, "f20")),
        "mainFundsInflow": _num(safe_get(data, "f152")),
    }


def normalize_record(obj: Dict[str, Any]) -> Dict[str, Any]:
    clean = {}
    for k, v in obj.items():
        nk = re.sub(r"[\r\n\t ]+", "", k)
        if nk: clean[nk] = v
    required = ["stockCode", "time", "price", "volume", "side", "sideCode", "tickCount", "avgVol"]
    for f in required:
        if f not in clean: clean[f] = None
    return clean


def standardize_tick(data: Dict[str, Any], stock_code: str) -> list:
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
        except Exception:
            continue
    return result


@app.get("/health")
def health():
    return {
        "status": "ok",
        "browser": BROWSER is not None,
        "page_pool": PAGE_POOL.qsize(),
        "cached_headers_keys": list(CACHED_HEADERS.keys()) if CACHED_HEADERS else [],
    }


@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"): raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"): raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickRequest, request: Request):
    secid = normalize_secid(req.secid)
    parts = secid.split('.')
    stock_code = parts[1] if len(parts) > 1 else secid
    ts = int(time.time() * 1000)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&ut=bd1d9ddb04089700cf9c27f6f7426281&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1000000&num=1000000&cb=jQuery_{ts}&_={ts}"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    if not raw or not raw.get("data"): return []
    return standardize_tick(raw["data"], stock_code)


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest, request: Request):
    secid = normalize_secid(req.secid)
    lookback = req.ndays * 2 + 20
    beg = (datetime.now() - timedelta(days=lookback)).strftime("%Y%m%d")
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg={beg}&end=20500101&secid={secid}&klt=101&fqt=1"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])
    result = []
    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11: continue
            amount_value = round(float(arr[6]), 2) if arr[6] else 0.0
            change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0
            high = float(arr[3])
            low = float(arr[4])
            amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0
            result.append({
                "trade_date": arr[0],
                "stock_code": req.secid,
                "open": float(arr[1]),
                "close": float(arr[2]),
                "high": float(arr[3]),
                "low": float(arr[4]),
                "volume": int(arr[5]),
                "amount": amount_value,
                "change": float(arr[9]) if len(arr) > 9 else 0.0,
                "change_percent": change_percent_value,
                "amplitude": amplitude,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0
            })
        except Exception:
            continue
    return result


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeRequest, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20500101"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg={beg}&end={end}&secid={secid}&klt=101&fqt=1"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])
    result = []
    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11: continue
            amount_value = round(float(arr[6]), 2) if arr[6] else 0.0
            change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0
            high = float(arr[3])
            low = float(arr[4])
            amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0
            result.append({
                "trade_date": arr[0],
                "stock_code": req.secid,
                "open": float(arr[1]),
                "close": float(arr[2]),
                "high": float(arr[3]),
                "low": float(arr[4]),
                "volume": int(arr[5]),
                "amount": amount_value,
                "change": float(arr[9]) if len(arr) > 9 else 0.0,
                "change_percent": change_percent_value,
                "amplitude": amplitude,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0
            })
        except Exception:
            continue
    return result


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineRequest, request: Request):
    secid = f"{req.market}.{req.secid}"
    beg = req.beg or "19900101"
    end = req.end or "20500101"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg={beg}&end={end}&secid={secid}&klt=101&fqt=1"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])
    result = []
    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11: continue
            amount_value = round(float(arr[6]), 2) if arr[6] else 0.0
            change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0
            high = float(arr[3])
            low = float(arr[4])
            amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0
            result.append({
                "trade_date": arr[0],
                "stock_code": req.secid,
                "open": float(arr[1]),
                "close": float(arr[2]),
                "high": float(arr[3]),
                "low": float(arr[4]),
                "volume": int(arr[5]),
                "amount": amount_value,
                "change": float(arr[9]) if len(arr) > 9 else 0.0,
                "change_percent": change_percent_value,
                "amplitude": amplitude,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0
            })
        except Exception:
            continue
    return result


@app.post("/proxy/json")
async def proxy_json(req: GenericJsonRequest, request: Request):
    data = await fetch_json_with_browser(req.url, request.state.request_id)
    if data is None: raise HTTPException(status_code=502, detail="Invalid upstream JSON")
    return data
