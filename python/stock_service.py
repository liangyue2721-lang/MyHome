#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import json
import logging
import asyncio
import re
import time
import random
from datetime import datetime, timedelta, date
from typing import Optional, List, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Browser, BrowserContext

# =========================================================
# Logging Setup
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)

# ---------- Business Logger (保持你原来的行为) ----------
logger = logging.getLogger("stock-service")
logger.setLevel(logging.INFO)

ch = logging.StreamHandler()
ch.setFormatter(logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
))
logger.addHandler(ch)

fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "stock_service.log"),
    when="midnight",
    interval=1,
    backupCount=30,
    encoding="utf-8"
)
fh.setFormatter(logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
))
logger.addHandler(fh)

# ---------- Access Logger（JSON）----------
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

# 关闭 uvicorn 默认 access log
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI App
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.0")

# =========================================================
# Global State
# =========================================================

PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE = asyncio.Semaphore(10)


# =========================================================
# Middleware: JSON Access Log
# =========================================================

@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    start_time = time.time()

    # -------- Request Info --------
    method = request.method
    path = request.url.path
    query_params = dict(request.query_params)
    headers = dict(request.headers)
    client_ip = request.client.host if request.client else None

    # Read & cache body (safe for FastAPI)
    body_bytes = await request.body()
    request._body = body_bytes  # allow downstream reuse

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
            "client_ip": client_ip,
            "method": method,
            "path": path,
            "query_params": query_params or None,
            "body": body,
            "headers": headers,
            "status_code": status_code,
            "duration_ms": duration_ms,
            "error": error,
        }

        access_logger.info(json.dumps(log_record, ensure_ascii=False))


# =========================================================
# Utilities
# =========================================================

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36",
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
    script = (
        "() => Object.defineProperty(navigator, 'webdriver', {"
        "get: () => undefined"
        "})"
    )
    await context.add_init_script(script)


# =========================================================
# Lifecycle
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER
    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()
    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )
    logger.info("Browser started.")


@app.on_event("shutdown")
async def shutdown():
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()
    logger.info("Playwright stopped.")


# =========================================================
# Health
# =========================================================

@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}

# =========================================================
# Data Models
# =========================================================

class RealtimeRequest(BaseModel):
    url: str

class KlineRequest(BaseModel):
    secid: str
    ndays: int

# =========================================================
# Business Logic Helpers
# =========================================================

def safe_get(dct, key):
    return dct.get(key)

def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    """
    Standardize the realtime data fields to match the Java DTO.
    Based on etf_realtime_fetcher.py mapping.
    """
    def _div100(val):
        return val / 100 if val is not None else None

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
        "mainFundsInflow": safe_get(data, "f152")
    }

async def fetch_json_with_browser(url: str, max_retry=3) -> Optional[Dict[str, Any]]:
    """
    Fetch JSON data using the global Playwright browser.
    """
    global BROWSER, SEMAPHORE

    if not BROWSER:
        logger.error("Global BROWSER is not initialized.")
        raise HTTPException(status_code=500, detail="Browser service not ready")

    async with SEMAPHORE:
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent=random_ua()
        )
        await hide_webdriver_property(context)

        page = None
        try:
            page = await context.new_page()

            for attempt in range(1, max_retry + 1):
                t_start = time.time()
                try:
                    logger.info(f"[FETCH_START] attempt={attempt}/{max_retry} url={url}")
                    await page.goto(url, timeout=20000, wait_until="domcontentloaded")

                    # innerText is robust for JSON responses rendered in browser
                    text = await page.evaluate("() => document.body.innerText || ''")
                    text = text.strip()

                    if not text:
                        raise Exception("Empty response body")
                    if text.startswith("<"):
                        # Log snippet for debugging
                        snippet = text[:100].replace("\n", " ")
                        raise Exception(f"HTML content detected: {snippet}")

                    parsed = parse_json_or_jsonp(text)
                    if parsed is None:
                        raise Exception("JSON parse failed")

                    duration = int((time.time() - t_start) * 1000)
                    data_size = len(str(parsed))
                    logger.info(f"[FETCH_SUCCESS] attempt={attempt} costMs={duration} size={data_size} url={url}")

                    return parsed

                except Exception as e:
                    duration = int((time.time() - t_start) * 1000)
                    logger.warning(f"[FETCH_FAIL] attempt={attempt} costMs={duration} error={str(e)} url={url}")
                    if attempt < max_retry:
                        await asyncio.sleep(0.5 + random.random())
                    else:
                        logger.error(f"[FETCH_GIVEUP] All attempts failed for {url}: {e}")

            return None

        except Exception as e:
            logger.error(f"Unexpected browser error: {e}")
            raise HTTPException(status_code=500, detail=f"Browser fetch error: {str(e)}")
        finally:
            if page:
                try:
                    await page.close()
                except:
                    pass
            if context:
                try:
                    await context.close()
                except:
                    pass

def normalize_kline_record(obj: dict) -> dict:
    """Clean keys and ensure required fields exist."""
    clean = {}
    for k, v in obj.items():
        nk = re.sub(r"[\r\n\t ]+", "", k)
        if nk:
            clean[nk] = v

    required = [
        "trade_date", "trade_time", "stock_code",
        "open", "close", "high", "low",
        "volume", "amount",
        "pre_close", "change", "change_pct", "turnover_ratio"
    ]
    for f in required:
        if f not in clean:
            clean[f] = None
    return clean

# =========================================================
# Endpoints
# =========================================================

@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest):
    data_json = await fetch_json_with_browser(req.url)
    if not data_json:
        raise HTTPException(status_code=404, detail="Not Found")

    data = data_json.get("data", {})
    if not data:
         # Sometimes API returns code=0 but data is null
         raise HTTPException(status_code=404, detail="No data in response")

    return standardize_realtime_data(data)


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest):
    # Logic is identical to stock/realtime, just separated for compatibility
    data_json = await fetch_json_with_browser(req.url)
    if not data_json:
        raise HTTPException(status_code=404, detail="Not Found")

    data = data_json.get("data", {})
    if not data:
         raise HTTPException(status_code=404, detail="No data in response")

    return standardize_realtime_data(data)


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest):
    """
    Fetch K-line data for the last `ndays`.
    """
    # 0. Validate input (Defensive check)
    if not req.secid or req.secid.startswith("null."):
        raise HTTPException(status_code=422, detail="Invalid secid parameter")

    # 1. Normalize secid (add market prefix if missing)
    secid = normalize_secid(req.secid)

    # 2. Calculate beg_date
    # To be safe, look back (ndays * 2 + 20) days to account for weekends/holidays
    lookback = req.ndays * 2 + 20
    beg_date_dt = datetime.now() - timedelta(days=lookback)
    beg_date = beg_date_dt.strftime("%Y%m%d")
    end_date = "20500101"

    # 3. Construct URL
    # Using the template from fetch_stock_realtime.py
    url = (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        f"beg={beg_date}&end={end_date}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
        f"secid={secid}&klt=101&fqt=1"
    )

    # 4. Fetch
    raw_json = await fetch_json_with_browser(url)
    if not raw_json:
        raise HTTPException(status_code=404, detail="Not Found")

    data = raw_json.get("data")
    if data is None:
        # API returned success (HTTP 200/JSON) but data is null (e.g. invalid secid on provider side)
        raise HTTPException(status_code=404, detail="Remote data is null")

    klines = data.get("klines", [])
    code = data.get("code")

    # 5. Parse
    result = []
    prev_close = None
    # API doesn't always give explicit pre-close for each day in this format,
    # but we can track it from the previous day's close.
    # Alternatively, data.get("preKPrice") might be available for the *first* item,
    # but calculating sequentially is safer if we have enough history.

    for row in klines:
        parts = row.split(",")
        if len(parts) < 10:
            continue

        trade_date_str = parts[0]
        try:
            open_p = float(parts[1])
            close_p = float(parts[2])
            high_p = float(parts[3])
            low_p = float(parts[4])
            volume = int(parts[5])
            amount = float(parts[6])
            change = float(parts[7])
            change_pct = float(parts[8])
            turnover_ratio = float(parts[9])
        except (ValueError, IndexError):
            continue

        ttime_str = None
        try:
            tdate = datetime.strptime(trade_date_str, "%Y-%m-%d").date()
            ttime = datetime.combine(tdate, datetime.min.time())
            ttime_str = ttime.isoformat()
        except:
            pass

        item = {
            "trade_date": trade_date_str,
            "trade_time": ttime_str,
            "stock_code": code,
            "open": open_p,
            "close": close_p,
            "high": high_p,
            "low": low_p,
            "volume": volume,
            "amount": amount,
            "change": change,
            "change_pct": change_pct,
            "turnover_ratio": turnover_ratio,
            "pre_close": prev_close
        }

        result.append(normalize_kline_record(item))
        prev_close = close_p

    # 6. Slice the last ndays
    if not result:
        return []

    if len(result) > req.ndays:
        result = result[-req.ndays:]

    return result
