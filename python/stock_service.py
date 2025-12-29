#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import json
import logging
import asyncio
import re
import time
import random
from datetime import datetime, timedelta
from typing import Optional, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Browser, BrowserContext

# =========================================================
# Logging Setup（原样保留）
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("stock-service")
logger.setLevel(logging.INFO)

formatter = logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
)

ch = logging.StreamHandler()
ch.setFormatter(formatter)
logger.addHandler(ch)

fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "stock_service.log"),
    when="midnight",
    interval=1,
    backupCount=30,
    encoding="utf-8"
)
fh.setFormatter(formatter)
logger.addHandler(fh)

access_logger = logging.getLogger("access")
access_logger.setLevel(logging.INFO)

access_console = logging.StreamHandler()
access_console.setFormatter(logging.Formatter("%(message)s"))

access_file = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "access.log"),
    when="midnight",
    interval=1,
    backupCount=14,
    encoding="utf-8"
)
access_file.setFormatter(logging.Formatter("%(message)s"))

access_logger.addHandler(access_console)
access_logger.addHandler(access_file)

logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI App（原样）
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.0")

# =========================================================
# Global Runtime State（⚠️ 不再创建 asyncio 对象）
# =========================================================

PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE: Optional[asyncio.Semaphore] = None  # ← 修复点


# =========================================================
# Middleware: JSON Access Log（原样）
# =========================================================

@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    start_time = time.time()

    body_bytes = await request.body()
    request._body = body_bytes

    body = None
    if body_bytes:
        try:
            body = body_bytes.decode("utf-8")
            if len(body) > 2000:
                body = body[:2000] + "...[truncated]"
        except Exception:
            body = "<binary>"

    response = None
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
        duration_ms = int((time.time() - start_time) * 1000)

        log_record = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "client_ip": request.client.host if request.client else None,
            "method": request.method,
            "path": request.url.path,
            "query_params": dict(request.query_params) or None,
            "body": body,
            "headers": dict(request.headers),
            "status_code": status_code,
            "duration_ms": duration_ms,
            "error": error,
        }

        access_logger.info(json.dumps(log_record, ensure_ascii=False))


# =========================================================
# Utilities（原样）
# =========================================================

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) Chrome/118",
]


def random_ua() -> str:
    return random.choice(UA_POOL)


def parse_json_or_jsonp(text: str):
    if not text:
        return None
    text = text.lstrip("\ufeff").strip()
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
    if code.startswith(("0", "3")):
        return f"0.{code}"
    return f"0.{code}"


async def hide_webdriver_property(context: BrowserContext) -> None:
    await context.add_init_script(
        "() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})"
    )


# =========================================================
# Lifecycle（✅ 唯一修改点）
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER, SEMAPHORE

    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()
    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )

    SEMAPHORE = asyncio.Semaphore(10)  # ✅ 正确位置创建

    logger.info("Browser started.")


@app.on_event("shutdown")
async def shutdown():
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()
    logger.info("Playwright stopped.")


# =========================================================
# Health（原样）
# =========================================================

@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}


# =========================================================
# Data Models（原样）
# =========================================================

class RealtimeRequest(BaseModel):
    url: str


class KlineRequest(BaseModel):
    secid: str
    ndays: int


# =========================================================
# Business Helpers（原样）
# =========================================================

def safe_get(dct, key):
    return dct.get(key)


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    def _div100(val):
        if val is None:
            return None
        if val == "" or val == "-":
            return None
        try:
            return float(val) / 100
        except (ValueError, TypeError):
            logger.warning(f"Invalid price value: {val}")
            return None


    return {
        "stockCode": safe_get(data, "f57"),
        "companyName": safe_get(data, "f58"),
        "price": _div100(safe_get(data, "f43")),
        "prevClose": _div100(safe_get(data, "f60")),
        "openPrice": _div100(safe_get(data, "f46")),
        "highPrice": _div100(safe_get(data, "f44")),
        "lowPrice": _div100(safe_get(data, "f45")),
        "volume": safe_get(data, "f47"),
        "turnover": safe_get(data, "f48"),
        "volumeRatio": safe_get(data, "f52"),
        "commissionRatio": safe_get(data, "f20"),
        "mainFundsInflow": safe_get(data, "f152"),
    }


# =========================================================
# Browser Fetch（逻辑不变，仅加防御）
# =========================================================

async def fetch_json_with_browser(url: str, max_retry=3) -> Optional[Dict[str, Any]]:
    global BROWSER, SEMAPHORE

    if not BROWSER or not SEMAPHORE:
        raise HTTPException(status_code=500, detail="Browser service not ready")

    async with SEMAPHORE:
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent=random_ua()
        )
        await hide_webdriver_property(context)
        page = await context.new_page()

        try:
            for attempt in range(1, max_retry + 1):
                try:
                    await page.goto(url, timeout=20000, wait_until="domcontentloaded")
                    text = (await page.evaluate(
                        "() => document.body.innerText || ''"
                    )).strip()
                    parsed = parse_json_or_jsonp(text)
                    if parsed is not None:
                        return parsed
                    raise Exception("JSON parse failed")
                except Exception:
                    if attempt == max_retry:
                        raise
                    await asyncio.sleep(0.5 + random.random())
        finally:
            await page.close()
            await context.close()


# =========================================================
# Endpoints（100% 原接口）
# =========================================================

@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest):
    data_json = await fetch_json_with_browser(req.url)
    if not data_json or not data_json.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(data_json["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest):
    data_json = await fetch_json_with_browser(req.url)
    if not data_json or not data_json.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(data_json["data"])


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest):
    secid = normalize_secid(req.secid)
    lookback = req.ndays * 2 + 20
    beg_date = (datetime.now() - timedelta(days=lookback)).strftime("%Y%m%d")

    url = (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        f"beg={beg_date}&end=20500101&secid={secid}&klt=101&fqt=1"
    )

    raw_json = await fetch_json_with_browser(url)
    if not raw_json or not raw_json.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")

    return raw_json["data"].get("klines", [])
