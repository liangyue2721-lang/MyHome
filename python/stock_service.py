#!/usr/bin/env python3
# -*- coding: utf-8 -*-

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
from playwright.async_api import async_playwright, Browser, Error as PlaywrightError

# =========================================================
# Logging Setup
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name, filename, fmt):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.handlers.clear()

    fh = TimedRotatingFileHandler(
        os.path.join(LOG_DIR, filename),
        when="midnight",
        interval=1,
        backupCount=14,
        encoding="utf-8"
    )
    fh.setFormatter(fmt)
    logger.addHandler(fh)
    return logger


logger = _build_logger(
    "stock-service",
    "stock_service.log",
    logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s")
)

access_logger = _build_logger(
    "access",
    "access.log",
    logging.Formatter("%(message)s")
)

trace_logger = _build_logger(
    "trace",
    "trace.log",
    logging.Formatter(
        "[%(asctime)s] [%(levelname)s] [%(request_id)s] %(message)s"
    )
)

logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.1")

# =========================================================
# Runtime Globals
# =========================================================

PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE: Optional[asyncio.Semaphore] = None

PAGES = []
PAGE_INDEX = 0
PAGE_LOCK = asyncio.Lock()


# =========================================================
# Trace Adapter
# =========================================================

class TraceAdapter(logging.LoggerAdapter):
    def process(self, msg, kwargs):
        kwargs.setdefault("extra", {})
        kwargs["extra"]["request_id"] = self.extra["request_id"]
        return msg, kwargs


# =========================================================
# Middleware
# =========================================================

@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    request_id = uuid.uuid4().hex[:12]
    request.state.request_id = request_id

    start = time.time()
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
            "cost_ms": int((time.time() - start) * 1000),
            "error": error,
        }, ensure_ascii=False))


# =========================================================
# Utils
# =========================================================

def parse_json_or_jsonp(text: str):
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        pass
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if m:
        try:
            return json.loads(m.group(1))
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
# Lifecycle
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER, SEMAPHORE, PAGES

    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()

    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )

    SEMAPHORE = asyncio.Semaphore(10)

    for i in range(2):
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )
        await context.add_init_script(
            "() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})"
        )

        page = await context.new_page()
        try:
            await page.goto(
                "https://quote.eastmoney.com/",
                wait_until="domcontentloaded",
                timeout=30000
            )
            logger.info(f"Warmup page {i} OK")
        except Exception as e:
            logger.warning(f"Warmup page {i} ignored: {e}")

        PAGES.append(page)

    logger.info("Browser started.")


@app.on_event("shutdown")
async def shutdown():
    for page in PAGES:
        await page.close()
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()


# =========================================================
# Browser Fetch (日志增强)
# =========================================================

async def fetch_json_with_browser(url: str, request_id: str, max_retry=3):
    global PAGE_INDEX

    trace = TraceAdapter(trace_logger, {"request_id": request_id})
    trace.info(f"Fetch start url={url}")

    if not PAGES:
        trace.error("Browser not ready")
        raise HTTPException(status_code=500, detail="Browser service not ready")

    async with SEMAPHORE:
        async with PAGE_LOCK:
            page = PAGES[PAGE_INDEX]
            idx = PAGE_INDEX
            PAGE_INDEX = (PAGE_INDEX + 1) % len(PAGES)

        trace.info(f"Use page index={idx}")

        for attempt in range(1, max_retry + 1):
            try:
                start = time.time()
                text = await page.evaluate(
                    """async (url) => {
                        const r = await fetch(url, {
                            credentials: 'include',
                            headers: {
                                'Accept': '*/*',
                                'Accept-Language': 'zh-CN,zh;q=0.9'
                            }
                        });
                        return await r.text();
                    }""",
                    url,
                )

                cost = int((time.time() - start) * 1000)
                trace.info(f"[Attempt {attempt}] fetch ok cost={cost}ms len={len(text)}")

                parsed = parse_json_or_jsonp(text)
                if parsed:
                    trace.info(f"[Attempt {attempt}] JSON parse success")
                    return parsed

                trace.warning(
                    f"[Attempt {attempt}] JSON parse failed "
                    f"head={text[:120]!r} tail={text[-120:]!r}"
                )

            except PlaywrightError as e:
                trace.warning(
                    f"[Attempt {attempt}] PlaywrightError {type(e).__name__}: {e}"
                )
                if "Execution context was destroyed" in str(e) or "Target closed" in str(e):
                    trace.warning("Reloading page...")
                    await page.reload(wait_until="domcontentloaded", timeout=10000)

            except Exception as e:
                trace.error(
                    f"[Attempt {attempt}] GeneralError {type(e).__name__}: {e}"
                )

        trace.error(f"All retries failed url={url}")
        raise HTTPException(status_code=500, detail="Upstream fetch failed")


# =========================================================
# Models
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


# =========================================================
# Business Helpers
# =========================================================

def safe_get(dct, key):
    return dct.get(key)


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    def _div100(val):
        if val in (None, "", "-"):
            return None
        try:
            return float(val) / 100
        except Exception:
            return None

    def _num(val):
        if val in (None, "", "-"):
            return None
        return val

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


# =========================================================
# Endpoints (全部保留)
# =========================================================

@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}


@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"):
        logger.error("Data fetch failed. [url=%s]", req.url)
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest, request: Request):
    secid = normalize_secid(req.secid)
    lookback = req.ndays * 2 + 20
    beg = (datetime.now() - timedelta(days=lookback)).strftime("%Y%m%d")

    url = (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        f"beg={beg}&end=20500101&secid={secid}&klt=101&fqt=1"
    )

    raw = await fetch_json_with_browser(url, request.state.request_id)
    return raw.get("data", {}).get("klines", [])


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeRequest, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20500101"

    url = (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        f"beg={beg}&end={end}&secid={secid}&klt=101&fqt=1"
    )

    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])

    result = []
    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11:
                continue

            result.append({
                "trade_date": arr[0],
                "trade_time": None,  # Java Date，可为空
                "stock_code": req.secid,
                "open": float(arr[1]),
                "close": float(arr[2]),
                "high": float(arr[3]),
                "low": float(arr[4]),
                "volume": int(arr[5]),
                "amount": float(arr[6]),
                "change": float(arr[9]),
                "change_percent": float(arr[8]),
                "turnover_ratio": float(arr[10]),
                "pre_close": None  # 东方财富未提供
            })
        except Exception:
            # 单条异常直接跳过，避免整体失败
            logger.error("Parsing line failed. [line=%s]", line)
            continue

    return result


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineRequest, request: Request):
    secid = f"{req.market}.{req.secid}"
    beg = req.beg or "19900101"
    end = req.end or "20500101"

    url = (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        f"beg={beg}&end={end}&secid={secid}&klt=101&fqt=1"
    )

    raw = await fetch_json_with_browser(url, request.state.request_id)
    return raw.get("data", {}).get("klines", [])


@app.post("/proxy/json")
async def proxy_json(req: GenericJsonRequest, request: Request):
    data = await fetch_json_with_browser(req.url, request.state.request_id)
    if data is None:
        logger.error("Browser started. [url=%s]", req.url)
        raise HTTPException(status_code=502, detail="Invalid upstream JSON")
    return data
