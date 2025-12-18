#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Service with Persistent Playwright Instance
Powered by FastAPI
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

from fastapi import FastAPI, HTTPException, Query, Body
from pydantic import BaseModel
from playwright.async_api import async_playwright, Browser, BrowserContext

# Logging Setup
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger("stock-service")

app = FastAPI(title="Stock Data Service", version="1.0.0")

# Global State
PLAYWRIGHT = None
BROWSER: Optional[Browser] = None
SEMAPHORE = asyncio.Semaphore(10) # Max 10 concurrent tabs

# =========================================================
# Utilities
# =========================================================

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36"
]

def random_ua():
    return random.choice(UA_POOL)

def parse_json_or_jsonp(text: str):
    text = text.lstrip("\ufeff").strip()
    try:
        return json.loads(text)
    except:
        pass
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if m:
        try:
            return json.loads(m.group(1))
        except:
            pass
    return None

def normalize_secid(code: str):
    if "." in code:
        return code
    if code.startswith(("83", "43")):
        return f"113.{code}"
    if code.startswith(("6", "5")):
        return f"1.{code}"
    if code.startswith(("0", "3")):
        return f"0.{code}"
    return f"0.{code}"

# =========================================================
# Lifecycle
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER
    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()
    BROWSER = await PLAYWRIGHT.chromium.launch(headless=True)
    logger.info("Browser launched successfully.")

@app.on_event("shutdown")
async def shutdown():
    global PLAYWRIGHT, BROWSER
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()
    logger.info("Playwright stopped.")

# =========================================================
# Logic: ETF Realtime (Ported from etf_realtime_fetcher.py)
# =========================================================

def standardize_etf_data(data):
    def safe(dct, key): return dct.get(key)
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
        "mainFundsInflow": safe(data, "f152")
    }

class EtfRequest(BaseModel):
    url: str

@app.post("/etf/realtime")
async def fetch_etf_realtime(req: EtfRequest):
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
            # Short timeout because Browser is already hot
            logger.info(f"Fetching ETF: {req.url}")
            await page.goto(req.url, timeout=20000, wait_until="domcontentloaded")
            text = await page.evaluate("() => document.body.innerText || ''")

            if not text: raise HTTPException(status_code=500, detail="Empty response")

            parsed = parse_json_or_jsonp(text)
            if not parsed: raise HTTPException(status_code=500, detail="JSON parse failed")

            data = parsed.get("data", {})
            return standardize_etf_data(data)

        except Exception as e:
            logger.error(f"ETF Fetch Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()

# =========================================================
# Logic: Trends2 Kline (Ported from trends2_playwright_logger.py)
# =========================================================

class KlineRequest(BaseModel):
    secid: str
    ndays: int = 5

async def fetch_trends_eastmoney(page, secid, ndays):
    cb = f"jsonp{int(time.time() * 1000)}"
    url = (
        "https://push2his.eastmoney.com/api/qt/stock/trends2/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f17&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58&"
        f"secid={secid}&ndays={ndays}&ut=fa5fd1943c7b386f172d6893db (?)"
        f"fa10b&iscr=0&iscca=0&cb={cb}"
    )
    try:
        await page.goto(url, timeout=10000)
        text = await page.evaluate("() => document.body.innerText || ''")
        parsed = parse_json_or_jsonp(text)
        if parsed and parsed.get("data") and parsed.get("data").get("trends"):
            return parsed["data"]["trends"]
    except Exception as e:
        logger.warning(f"EastMoney trends failed: {e}")
    return None

def aggregate_daily(rows, secid):
    groups = {}
    for r in rows:
        p = r.split(",")
        day = p[0].split(" ")[0]
        groups.setdefault(day, []).append(p)

    result = []
    prev_close = None
    code = secid.split(".")[1] if "." in secid else secid

    for day in sorted(groups):
        rows = groups[day]
        o = float(rows[0][1])
        c = float(rows[-1][2])
        h = max(float(x[3]) for x in rows)
        l = min(float(x[4]) for x in rows)
        v = sum(int(float(x[5])) for x in rows)
        amt = sum(float(x[6]) for x in rows)

        chg = None if prev_close is None else round(c - prev_close, 4)
        pct = None if prev_close in (None, 0) else round(chg / prev_close * 100, 4)

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
            "turnover_ratio": None
        })
        prev_close = c
    return result

@app.post("/stock/kline")
async def fetch_stock_kline(req: KlineRequest):
    secid = normalize_secid(req.secid)
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
            logger.info(f"Fetching Kline: {secid}")
            # Priority 1: EastMoney
            rows = await fetch_trends_eastmoney(page, secid, req.ndays)

            # TODO: Add Baidu/Sina fallback if needed, keeping it simple for now

            if rows:
                return aggregate_daily(rows, secid)

            # Return empty list if no data, don't error out
            return []

        except Exception as e:
            logger.error(f"Kline Fetch Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()

@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}

if __name__ == "__main__":
    import uvicorn
    # In production, use start_service.sh
    uvicorn.run(app, host="0.0.0.0", port=8000)
