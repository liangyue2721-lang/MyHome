#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Optimized Stock Service with Persistent Playwright Instance
---------------------------------------------------------

This module contains an optimized version of the original stock service.
Key improvements include:

* **Preheat phase for anti-bot cookies**: Each request now makes a best-effort
  call to `https://quote.eastmoney.com/` before hitting the target API. This
  aligns with the synchronous scripts in this repository and helps obtain
  cookies that some EastMoney endpoints require.

* **Hiding the `navigator.webdriver` property**: By injecting a small
  JavaScript shim into every new browser context, Playwright no longer exposes
  the `navigator.webdriver` property. This reduces the likelihood of being
  blocked by anti-bot measures.

* **Retry logic**: Network calls sometimes fail due to connectivity issues or
  temporary blocks. Each fetch function now attempts the request up to three
  times with a small delay between attempts. If all attempts fail, an
  appropriate HTTP 500 is returned.

The public API surface remains unchanged. See the individual endpoint
definitions for details.
"""

import os
import json
import logging
import asyncio
import re
import time
import random
from datetime import datetime
from typing import Optional, List, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Browser, BrowserContext

# =========================================================
# Logging Setup
# =========================================================

LOG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "logs")
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("stock-service-optimized")
logger.setLevel(logging.INFO)

# Console Handler
ch = logging.StreamHandler()
ch.setFormatter(logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s", "%Y-%m-%d %H:%M:%S"))
logger.addHandler(ch)

# File Handler
fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "stock_service_optimized.log"),
    when="midnight", interval=1, backupCount=30, encoding="utf-8"
)
fh.setFormatter(logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s", "%Y-%m-%d %H:%M:%S"))
logger.addHandler(fh)

app = FastAPI(title="Stock Data Service (Optimized)", version="1.3.0")

# Global State
PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE = asyncio.Semaphore(10)  # Max 10 concurrent tabs

# =========================================================
# Utilities
# =========================================================

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36",
]


def random_ua() -> str:
    """Return a random user agent from the pool."""
    return random.choice(UA_POOL)


def parse_json_or_jsonp(text: str):
    """Attempt to parse a string as JSON, falling back to JSONP.

    EastMoney returns JSONP for certain endpoints. This helper trims BOMs and
    parentheses to return a dictionary or None.
    """
    if not text:
        return None
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except Exception:
        pass
    # Fallback: try to extract JSON from a JSONP callback
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if m:
        try:
            return json.loads(m.group(1))
        except Exception:
            pass
    return None


def normalize_secid(code: str) -> str:
    """Normalize a stock code into EastMoney's `market.code` format."""
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
    """Inject a script that hides the `navigator.webdriver` property.

    Many anti-scraping solutions check for the presence of `navigator.webdriver`
    and block requests accordingly. Adding this script makes the property
    undefined.
    """
    script = (
        "() => Object.defineProperty(navigator, 'webdriver', {"
        "get: () => undefined"
        "})"
    )
    try:
        await context.add_init_script(script)
    except Exception as e:
        logger.debug(f"Failed to add init script: {e}")


# =========================================================
# Lifecycle
# =========================================================


@app.on_event("startup")
async def startup() -> None:
    """Initialize Playwright and launch a persistent browser."""
    global PLAYWRIGHT, BROWSER
    logger.info("Starting Playwright (optimized)...")
    try:
        PLAYWRIGHT = await async_playwright().start()
        # Launch headless to minimise resources; disable AutomationControlled to reduce detection
        BROWSER = await PLAYWRIGHT.chromium.launch(
            headless=True,
            args=["--disable-blink-features=AutomationControlled"],
        )
        # Warm up the browser by opening a blank page
        try:
            page = await BROWSER.new_page()
            await page.goto("https://www.example.com", timeout=15000)
            await page.close()
            logger.info("Browser launched and warmed up.")
        except Exception as warmup_error:
            logger.warning(f"Warmup failed (non-fatal): {warmup_error}")
    except Exception as e:
        logger.critical(f"Failed to start Playwright: {e}")
        raise e


@app.on_event("shutdown")
async def shutdown() -> None:
    """Shut down Playwright and the browser."""
    global PLAYWRIGHT, BROWSER
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()
    logger.info("Playwright stopped (optimized).")


# Middleware for Request Logging
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = (time.time() - start_time) * 1000
    logger.info(f"{request.method} {request.url.path} - {response.status_code} - {process_time:.2f}ms")
    return response


# =========================================================
# ETF Realtime Endpoint
# =========================================================


class EtfRequest(BaseModel):
    url: str


def standardize_etf_data(data: Dict[str, Any]) -> Dict[str, Any]:
    """Transform raw ETF data into a standard dictionary."""

    def safe(d: Dict[str, Any], key: str):
        return d.get(key)

    return {
        "stockCode": safe(data, "f57"),
        "companyName": safe(data, "f58"),
        "price": safe(data, "f43") / 100 if safe(data, "f43") is not None else None,
        "prevClose": safe(data, "f60") / 100 if safe(data, "f60") is not None else None,
        "openPrice": safe(data, "f46") / 100 if safe(data, "f46") is not None else None,
        "highPrice": safe(data, "f44") / 100 if safe(data, "f44") is not None else None,
        "lowPrice": safe(data, "f45") / 100 if safe(data, "f45") is not None else None,
        "volume": safe(data, "f47"),
        "turnover": safe(data, "f48"),
        "volumeRatio": safe(data, "f52"),
        "commissionRatio": safe(data, "f20"),
        "mainFundsInflow": safe(data, "f152"),
    }


@app.post("/etf/realtime")
async def fetch_etf_realtime(req: EtfRequest):
    """Fetch ETF realtime data with retry and anti-bot handling."""
    async with SEMAPHORE:
        # Create isolated context per request
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        await hide_webdriver_property(context)
        page = await context.new_page()
        try:
            # Preheat to get cookies. Ignore any errors in preheat.
            try:
                await page.goto("https://quote.eastmoney.com/", timeout=10000)
            except Exception:
                pass
            # Retry logic
            last_exception: Optional[Exception] = None
            for attempt in range(3):
                try:
                    await page.goto(req.url, timeout=20000, wait_until="domcontentloaded")
                    text = await page.evaluate("() => document.body.innerText || ''")
                    if not text:
                        raise Exception("Empty response")
                    parsed = parse_json_or_jsonp(text)
                    if not parsed:
                        raise Exception("JSON parse failed")
                    return standardize_etf_data(parsed.get("data", {}))
                except Exception as e:
                    last_exception = e
                    logger.warning(f"ETF attempt {attempt + 1}/3 failed: {e}")
                    await asyncio.sleep(1 + random.random())
            # If all attempts failed, propagate error
            raise last_exception  # type: ignore[arg-type]
        except Exception as e:
            logger.error(f"ETF Fetch Error [{req.url}]: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


# =========================================================
# Stock Realtime Endpoint
# =========================================================


class StockRealtimeRequest(BaseModel):
    url: str


def build_standard_stock_object(data: Dict[str, Any]) -> Dict[str, Any]:
    """Build a standardized stock object with default None values."""

    def safe_float(d: Dict[str, Any], k: str):
        try:
            return float(d.get(k)) if d.get(k) is not None else None
        except Exception:
            return None

    def safe_int(d: Dict[str, Any], k: str):
        try:
            return int(d.get(k)) if d.get(k) is not None else None
        except Exception:
            return None

    return {
        "stockCode": data.get("f57", ""),
        "companyName": data.get("f58", ""),
        "price": safe_float(data, "f43") / 100 if safe_float(data, "f43") is not None else None,
        "prevClose": safe_float(data, "f60") / 100 if safe_float(data, "f60") is not None else None,
        "openPrice": safe_float(data, "f46") / 100 if safe_float(data, "f46") is not None else None,
        "highPrice": safe_float(data, "f44") / 100 if safe_float(data, "f44") is not None else None,
        "lowPrice": safe_float(data, "f45") / 100 if safe_float(data, "f45") is not None else None,
        "volume": safe_int(data, "f47"),
        "turnover": safe_float(data, "f48"),
        "volumeRatio": safe_float(data, "f52"),
        "commissionRatio": safe_float(data, "f20"),
        "mainFundsInflow": safe_float(data, "f152"),
        # Additional fields defaulted to None
        "peRatio": None,
        "pbRatio": None,
        "turnoverRate": None,
        "amplitude": None,
        "eps": None,
        "mainNetInflow": None,
        "circulatingShares": None,
        "totalShares": None,
        "volumePriceTrend": None,
        "dividendYield": None,
        "roe": None,
        "grossMargin": None,
        "institutionalFlow": None,
        "retailFlow": None,
    }


@app.post("/stock/realtime")
async def fetch_stock_realtime(req: StockRealtimeRequest):
    """Fetch stock realtime data with retry and anti-bot handling."""
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        await hide_webdriver_property(context)
        page = await context.new_page()
        try:
            # Preheat to get cookies. Ignore errors as they are non-fatal.
            try:
                await page.goto("https://quote.eastmoney.com/", timeout=10000)
            except Exception:
                pass
            # Retry logic
            last_exception: Optional[Exception] = None
            for attempt in range(3):
                try:
                    await page.goto(req.url, timeout=20000, wait_until="domcontentloaded")
                    text = await page.evaluate("() => document.body.innerText || ''")
                    parsed = parse_json_or_jsonp(text) or {}
                    return build_standard_stock_object(parsed.get("data", {}))
                except Exception as e:
                    last_exception = e
                    logger.warning(f"Stock realtime attempt {attempt + 1}/3 failed: {e}")
                    await asyncio.sleep(1 + random.random())
            # If all attempts failed, propagate error
            raise last_exception  # type: ignore[arg-type]
        except Exception as e:
            logger.error(f"Stock Realtime Fetch Error [{req.url}]: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


# =========================================================
# Kline (Trends2 / 5-Day)
# =========================================================


class KlineTrendsRequest(BaseModel):
    secid: str
    ndays: int = 5


def aggregate_daily(rows: List[str], secid: str) -> List[Dict[str, Any]]:
    """Aggregate minute-level kline rows into daily summaries."""
    groups: Dict[str, List[List[str]]] = {}
    for r in rows:
        p = r.split(",")
        day = p[0].split(" ")[0]
        groups.setdefault(day, []).append(p)

    result: List[Dict[str, Any]] = []
    prev_close: Optional[float] = None
    code = secid.split(".")[1] if "." in secid else secid

    for day in sorted(groups):
        rows_for_day = groups[day]
        o = float(rows_for_day[0][1])
        c = float(rows_for_day[-1][2])
        h = max(float(x[3]) for x in rows_for_day)
        l = min(float(x[4]) for x in rows_for_day)
        v = sum(int(float(x[5])) for x in rows_for_day)
        amt = sum(float(x[6]) for x in rows_for_day)

        chg = round(c - prev_close, 4) if prev_close is not None else None
        pct = round(chg / prev_close * 100, 4) if (prev_close and prev_close != 0) else None

        result.append({
            "trade_date": day,
            "trade_time": f"{day}T00:00:00",
            "stock_code": code,
            "open": o,
            "close": c,
            "high": h,
            "low": l,
            "volume": v,
            "amount": amt,
            "pre_close": prev_close,
            "change": chg,
            "change_pct": pct,
            "turnover_ratio": None,
        })
        prev_close = c
    return result


@app.post("/stock/kline")
async def fetch_stock_kline(req: KlineTrendsRequest):
    """Fetch minute-level kline data and aggregate to daily bars."""
    secid = normalize_secid(req.secid)
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        await hide_webdriver_property(context)
        page = await context.new_page()
        try:
            # Preheat with quote site to get cookies
            try:
                await page.goto("https://quote.eastmoney.com/", timeout=10000)
            except Exception:
                pass
            # 1. Access page (anti-bot)
            code = secid.split(".")[1]
            try:
                await page.goto(f"https://quote.eastmoney.com/sh{code}.html", timeout=10000)
            except Exception:
                pass
            # 2. Fetch API
            cb = f"jsonp{int(time.time() * 1000)}"
            url = (
                "https://push2his.eastmoney.com/api/qt/stock/trends2/get?"
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f17&"
                "fields2=f51,f52,f53,f54,f55,f56,f57,f58&"
                f"secid={secid}&ndays={req.ndays}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
                f"iscr=0&iscca=0&cb={cb}"
            )
            # Retry logic
            last_exception: Optional[Exception] = None
            for attempt in range(3):
                try:
                    await page.goto(url, timeout=20000)
                    text = await page.evaluate("() => document.body.innerText || ''")
                    parsed = parse_json_or_jsonp(text)
                    rows: List[str] = []
                    if parsed and parsed.get("data", {}).get("trends"):
                        rows = parsed["data"]["trends"]
                    if rows:
                        return aggregate_daily(rows, secid)
                    # If no rows, break early
                    return []
                except Exception as e:
                    last_exception = e
                    logger.warning(f"Kline attempt {attempt + 1}/3 failed for {secid}: {e}")
                    await asyncio.sleep(1 + random.random())
            raise last_exception  # type: ignore[arg-type]
        except Exception as e:
            logger.error(f"Kline Trends Fetch Error [{secid}]: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


# =========================================================
# Kline Range (Historical) - Improved with Retry
# =========================================================


class KlineRangeRequest(BaseModel):
    secid: str
    market: str = "1"
    beg: str = "0"
    end: str = "20500101"


def parse_kline_items(data: dict, pre_close_from_api: Optional[float]) -> List[Dict[str, Any]]:
    """Parse historical kline data into structured objects."""
    if not data or "klines" not in data:
        return []
    code = data.get("code")
    result: List[Dict[str, Any]] = []
    prev_close = pre_close_from_api

    for item in data["klines"]:
        parts = item.split(",")
        if len(parts) < 10:
            continue
        try:
            close_p = float(parts[2])
            result.append({
                "trade_date": parts[0],
                "trade_time": f"{parts[0]}T00:00:00",
                "stock_code": code,
                "open": float(parts[1]),
                "close": close_p,
                "high": float(parts[3]),
                "low": float(parts[4]),
                "volume": int(parts[5]),
                "amount": float(parts[6]),
                "change": float(parts[7]),
                "change_pct": float(parts[8]),
                "turnover_ratio": float(parts[9]),
                "pre_close": prev_close,
            })
            prev_close = close_p
        except Exception:
            continue
    return result


@app.post("/stock/kline/range")
async def fetch_kline_range(req: KlineRangeRequest):
    """Fetch historical kline data with retry across multiple templates."""
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        await hide_webdriver_property(context)
        page = await context.new_page()
        try:
            # Preheat to obtain cookies
            try:
                await page.goto("https://quote.eastmoney.com/", timeout=5000)
            except Exception:
                pass
            # Retry logic across URL templates
            last_exception: Optional[Exception] = None
            for url_template in URL_TEMPLATES_KLINE:
                url = url_template.format(market=req.market, secid=req.secid, beg=req.beg, end=req.end)
                for attempt in range(3):
                    try:
                        await page.goto(url, timeout=20000)
                        text = await page.evaluate("() => document.body.innerText || ''")
                        parsed = parse_json_or_jsonp(text)
                        if parsed and parsed.get("data", {}).get("klines"):
                            data = parsed["data"]
                            pre_close: Optional[float] = None
                            if "preKPrice" in data:
                                pre_close = float(data["preKPrice"])
                            elif "f18" in data:
                                pre_close = float(data["f18"])
                            return parse_kline_items(data, pre_close)
                        # If no data, try next template
                        break
                    except Exception as ex:
                        last_exception = ex
                        logger.warning(f"URL attempt failed: {url} | {ex}")
                        await asyncio.sleep(1 + random.random())
                # Try next template if this one failed completely
            # No data found across templates
            return []
        except Exception as e:
            logger.error(f"Kline Range Error [{req.secid}]: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


# =========================================================
# US Stock Kline
# =========================================================


@app.post("/stock/kline/us")
async def fetch_us_kline(req: KlineRangeRequest):
    """Fetch US stock kline data with retry."""
    full_secid = f"{req.market}.{req.secid}"
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        await hide_webdriver_property(context)
        page = await context.new_page()
        try:
            # Preheat to obtain cookies
            try:
                await page.goto("https://quote.eastmoney.com/", timeout=5000)
            except Exception:
                pass
            url = (
                "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
                "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
                f"beg={req.beg}&end={req.end}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
                f"secid={full_secid}&klt=101&fqt=1"
            )
            last_exception: Optional[Exception] = None
            for attempt in range(3):
                try:
                    await page.goto(url, timeout=20000)
                    text = await page.evaluate("() => document.body.innerText || ''")
                    parsed = parse_json_or_jsonp(text)
                    result: List[Dict[str, Any]] = []
                    if parsed and parsed.get("data", {}).get("klines"):
                        data = parsed["data"]
                        code = data.get("code")
                        prev_close = None
                        for row in data["klines"]:
                            parts = row.split(",")
                            if len(parts) < 10:
                                continue
                            try:
                                close_p = float(parts[2])
                                result.append({
                                    "trade_date": parts[0],
                                    "trade_time": f"{parts[0]}T00:00:00",
                                    "stock_code": code,
                                    "open": float(parts[1]),
                                    "close": close_p,
                                    "high": float(parts[3]),
                                    "low": float(parts[4]),
                                    "volume": int(parts[5]),
                                    "amount": float(parts[6]),
                                    "change": float(parts[7]),
                                    "change_pct": float(parts[8]),
                                    "turnover_ratio": float(parts[9]),
                                    "pre_close": prev_close,
                                })
                                prev_close = close_p
                            except Exception:
                                continue
                    return result
                except Exception as e:
                    last_exception = e
                    logger.warning(f"US Kline attempt {attempt + 1}/3 failed: {e}")
                    await asyncio.sleep(1 + random.random())
            raise last_exception  # type: ignore[arg-type]
        except Exception as e:
            logger.error(f"US Kline Error [{full_secid}]: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


@app.get("/health")
def health():
    """Health check endpoint to verify the service and browser status."""
    return {"status": "ok", "browser": BROWSER is not None}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
