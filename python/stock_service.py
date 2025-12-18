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

app = FastAPI(title="Stock Data Service", version="1.1.0")

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
    if not text: return None
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
    BROWSER = await PLAYWRIGHT.chromium.launch(headless=True, args=["--disable-blink-features=AutomationControlled"])
    # Warmup
    try:
        page = await BROWSER.new_page()
        await page.goto("https://quote.eastmoney.com/", timeout=15000)
        await page.close()
    except:
        pass
    logger.info("Browser launched and warmed up.")

@app.on_event("shutdown")
async def shutdown():
    global PLAYWRIGHT, BROWSER
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()
    logger.info("Playwright stopped.")

# =========================================================
# Endpoint: ETF Realtime
# =========================================================

class EtfRequest(BaseModel):
    url: str

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

@app.post("/etf/realtime")
async def fetch_etf_realtime(req: EtfRequest):
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
            logger.info(f"Fetching ETF: {req.url}")
            await page.goto(req.url, timeout=20000, wait_until="domcontentloaded")
            text = await page.evaluate("() => document.body.innerText || ''")
            if not text: raise Exception("Empty response")
            parsed = parse_json_or_jsonp(text)
            if not parsed: raise Exception("JSON parse failed")
            return standardize_etf_data(parsed.get("data", {}))
        except Exception as e:
            logger.error(f"ETF Fetch Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()

# =========================================================
# Endpoint: Stock Realtime (realtime_fetcher.py)
# =========================================================

class StockRealtimeRequest(BaseModel):
    url: str

def build_standard_stock_object(data: dict):
    def safe_float(d, k):
        try: return float(d.get(k)) if d.get(k) is not None else None
        except: return None
    def safe_int(d, k):
        try: return int(d.get(k)) if d.get(k) is not None else None
        except: return None

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
        # Null fields
        "peRatio": None, "pbRatio": None, "turnoverRate": None, "amplitude": None,
        "eps": None, "mainNetInflow": None, "circulatingShares": None,
        "totalShares": None, "volumePriceTrend": None, "dividendYield": None,
        "roe": None, "grossMargin": None, "institutionalFlow": None, "retailFlow": None
    }

@app.post("/stock/realtime")
async def fetch_stock_realtime(req: StockRealtimeRequest):
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
            logger.info(f"Fetching Stock Realtime: {req.url}")
            await page.goto(req.url, timeout=20000, wait_until="domcontentloaded")
            text = await page.evaluate("() => document.body.innerText || ''")
            parsed = parse_json_or_jsonp(text) or {}
            return build_standard_stock_object(parsed.get("data", {}))
        except Exception as e:
            logger.error(f"Stock Realtime Fetch Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()

# =========================================================
# Endpoint: Kline (Trends2 / 5-Day)
# =========================================================

class KlineTrendsRequest(BaseModel):
    secid: str
    ndays: int = 5

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

        # Calculate change if prev_close exists
        chg = round(c - prev_close, 4) if prev_close is not None else None
        pct = round(chg / prev_close * 100, 4) if (prev_close and prev_close != 0) else None

        result.append({
            "trade_date": day,
            "trade_time": f"{day}T00:00:00",
            "stock_code": code,
            "open": o, "close": c, "high": h, "low": l,
            "volume": v, "amount": amt,
            "pre_close": prev_close,
            "change": chg, "change_pct": pct,
            "turnover_ratio": None
        })
        prev_close = c
    return result

@app.post("/stock/kline")
async def fetch_stock_kline(req: KlineTrendsRequest):
    # Used for fetchKlineDataFiveDay & Hybrid
    secid = normalize_secid(req.secid)
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
            # 1. Access page (anti-bot)
            code = secid.split(".")[1]
            try:
                await page.goto(f"https://quote.eastmoney.com/sh{code}.html", timeout=10000)
            except:
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
            await page.goto(url, timeout=20000)
            text = await page.evaluate("() => document.body.innerText || ''")
            parsed = parse_json_or_jsonp(text)

            rows = []
            if parsed and parsed.get("data", {}).get("trends"):
                rows = parsed["data"]["trends"]

            if rows:
                return aggregate_daily(rows, secid)
            return []

        except Exception as e:
            logger.error(f"Kline Trends Fetch Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


# =========================================================
# Endpoint: Kline Range (Historical)
# =========================================================

class KlineRangeRequest(BaseModel):
    secid: str
    market: str = "1"
    beg: str = "0"
    end: str = "20500101"

def parse_kline_items(data: dict, pre_close_from_api: Optional[float]) -> List[Dict[str, Any]]:
    if not data or "klines" not in data: return []
    code = data.get("code")
    result = []
    prev_close = pre_close_from_api

    for item in data["klines"]:
        parts = item.split(",")
        if len(parts) < 10: continue

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
                "pre_close": prev_close
            })
            prev_close = close_p
        except:
            continue
    return result

@app.post("/stock/kline/range")
async def fetch_kline_range(req: KlineRangeRequest):
    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
            # Warmup
            try: await page.goto("https://quote.eastmoney.com/", timeout=5000)
            except: pass

            url_template = (
                "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
                "secid={market}.{secid}&ut=7eea3edcaed734bea9cbfc24409ed989&"
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f18&"
                "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
                "klt=101&fqt=1&beg={beg}&end={end}&smplmt=100000&lmt=1000000"
            )
            url = url_template.format(market=req.market, secid=req.secid, beg=req.beg, end=req.end)

            await page.goto(url, timeout=20000)
            text = await page.evaluate("() => document.body.innerText || ''")
            parsed = parse_json_or_jsonp(text)

            if parsed and parsed.get("data", {}).get("klines"):
                data = parsed["data"]
                pre_close = None
                if "preKPrice" in data: pre_close = float(data["preKPrice"])
                elif "f18" in data: pre_close = float(data["f18"])
                return parse_kline_items(data, pre_close)
            return []

        except Exception as e:
            logger.error(f"Kline Range Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


# =========================================================
# Endpoint: US Stock Kline
# =========================================================

@app.post("/stock/kline/us")
async def fetch_us_kline(req: KlineRangeRequest):
    # Reuse range logic but URL/params might be slightly different for US
    # fetch_stock_realtime.py uses "secid={secid}" directly (e.g. 105.NVDA)
    # So we construct secid from market.code
    full_secid = f"{req.market}.{req.secid}"

    async with SEMAPHORE:
        context = await BROWSER.new_context(locale="zh-CN", user_agent=random_ua())
        page = await context.new_page()
        try:
             # Warmup
            try: await page.goto("https://quote.eastmoney.com/", timeout=5000)
            except: pass

            url = (
                "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
                "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
                f"beg={req.beg}&end={req.end}&ut=fa5fd1943c7b386f172d6893dbfba10b&"
                f"secid={full_secid}&klt=101&fqt=1"
            )

            await page.goto(url, timeout=20000)
            text = await page.evaluate("() => document.body.innerText || ''")
            parsed = parse_json_or_jsonp(text)

            # Logic from fetch_stock_realtime.py (normalize_record)
            result = []
            if parsed and parsed.get("data", {}).get("klines"):
                data = parsed["data"]
                code = data.get("code")
                prev_close = None

                for row in data["klines"]:
                    parts = row.split(",")
                    if len(parts) < 10: continue
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
                            "pre_close": prev_close
                        })
                        prev_close = close_p
                    except:
                        continue
            return result

        except Exception as e:
            logger.error(f"US Kline Error: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        finally:
            await page.close()
            await context.close()


@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
