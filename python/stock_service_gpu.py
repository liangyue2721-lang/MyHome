#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v10.2 (GPU Optimized)
"""

import os
import json
import logging
import asyncio
import time
import uuid
import random
import urllib.parse
import re
import traceback
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any, List
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request, Response
from pydantic import BaseModel
from playwright.async_api import async_playwright, Page, BrowserContext

# =========================================================
# 1. Core Config
# =========================================================

HEADLESS = True
POOL_SIZE = 8  # Increased from 4 for GPU node
PAGE_TTL = 200
BROWSER_TTL = 3600
FULL_FIELDS = "f58,f734,f107,f57,f43,f59,f169,f301,f60,f170,f152,f177,f111,f46,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f748,f747"

# =========================================================
# 2. Logging
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("stock_gpu")
logger.setLevel(logging.INFO)
formatter = logging.Formatter("[%(asctime)s] %(message)s")

ch = logging.StreamHandler()
ch.setFormatter(formatter)
logger.addHandler(ch)

fh = TimedRotatingFileHandler(os.path.join(LOG_DIR, "stock_gpu.log"), when="midnight", interval=1, backupCount=7)
fh.setFormatter(formatter)
logger.addHandler(fh)

logging.getLogger("uvicorn.access").disabled = True


# =========================================================
# 3. Browser Worker (Hybrid Fetcher)
# =========================================================

class TabWorker:
    def __init__(self, index: int, context: BrowserContext):
        self.index = index
        self.context = context
        self.page: Optional[Page] = None
        self.req_count = 0

    async def init_session(self):
        try:
            if not self.page:
                self.page = await self.context.new_page()
                # Block heavy resources
                await self.page.route("**/*.{png,jpg,jpeg,gif,svg,css,woff,woff2,mp4,webm}",
                                      lambda route: route.abort())

            logger.info(f"[Worker-{self.index}] Refreshing session...")

            try:
                # Navigate to a real Eastmoney page to establish context/cookies
                await self.page.goto(
                    "https://quote.eastmoney.com/center/gridlist.html",
                    wait_until="domcontentloaded",
                    timeout=15000
                )
            except Exception as goto_err:
                logger.warning(f"[Worker-{self.index}] Goto Warning (continuing): {goto_err}")

            await asyncio.sleep(1)
            self.req_count = 0
            logger.info(f"[Worker-{self.index}] Session Ready.")

        except Exception as e:
            logger.error(f"[Worker-{self.index}] Init Session Failed: {e}", exc_info=True)

    async def fetch_browser(self, url: str, rid: str) -> str:
        """Fetch using page.evaluate (Browser Context) - Bypasses socket hang up on push2"""
        self.req_count += 1
        if self.req_count > PAGE_TTL or not self.page:
            await self.init_session()

        js_code = """
        async (url) => {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 10000);

            try {
                const response = await fetch(url, {
                    signal: controller.signal,
                    headers: { 'Accept': '*/*' }
                });
                if (!response.ok) throw new Error("HTTP " + response.status);
                return await response.text();
            } catch (e) {
                throw e.message;
            } finally {
                clearTimeout(timeoutId);
            }
        }
        """

        for attempt in range(2):
            try:
                if not self.page: await self.init_session()
                content = await self.page.evaluate(js_code, url)
                return content
            except Exception as e:
                err_msg = str(e)
                if "Target closed" in err_msg or "context" in err_msg:
                    logger.warning(f"[{rid}] Context lost, recreating session...")
                    self.page = None
                    await self.init_session()
                    continue

                logger.warning(f"[{rid}] Browser Fetch Error (Attempt {attempt+1}): {err_msg}")
                if attempt == 1:
                    logger.error(f"[{rid}] Final Failure: {err_msg}")
                    return ""
                await asyncio.sleep(0.5)
        return ""

    async def fetch_direct(self, url: str, rid: str) -> str:
        """Fetch using Playwright APIRequestContext - Faster/Standard for push2his"""
        try:
            resp = await self.context.request.get(url, timeout=10000)
            if resp.status == 200:
                return await resp.text()
            logger.warning(f"[{rid}] Direct Fetch Status {resp.status} for {url}")
            return ""
        except Exception as e:
            logger.error(f"[{rid}] Direct Fetch Error: {e}")
            return ""


# =========================================================
# 4. Browser Pool
# =========================================================

class BrowserPool:
    def __init__(self):
        self.pw = None
        self.browser = None
        self.context = None
        self.workers: List[TabWorker] = []
        self.queue = asyncio.Queue()
        self.start_time = 0

    async def start(self):
        self.start_time = time.time()
        self.pw = await async_playwright().start()

        # Launch options for stability and stealth
        # Optimized for GPU usage
        self.browser = await self.pw.chromium.launch(
            headless=HEADLESS,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-http2", # Force HTTP/1.1 for stability
                "--disable-features=IsolateOrigins,site-per-process",
                # GPU Acceleration Flags
                "--ignore-gpu-blocklist",
                "--enable-gpu-rasterization",
                "--enable-zero-copy",
                "--use-gl=desktop"
            ]
        )

        # Emulate a real mobile device
        device = self.pw.devices["Pixel 7"]
        self.context = await self.browser.new_context(
            **device,
            accept_downloads=False,
            ignore_https_errors=True
        )

        for i in range(POOL_SIZE):
            w = TabWorker(i, self.context)
            await w.init_session()
            self.workers.append(w)
            await self.queue.put(w)
        logger.info(f"Hybrid Pool Started ({POOL_SIZE} workers).")

    async def stop(self):
        if self.context: await self.context.close()
        if self.browser: await self.browser.close()
        if self.pw: await self.pw.stop()

    async def dispatch(self, url: str, rid: Optional[str] = None) -> str:
        worker = await self.queue.get()
        rid = rid or "SYS"
        try:
            # Hybrid Strategy:
            # push2his (Kline) -> Direct Fetch (Faster, proven stable)
            # push2 (Realtime) -> Browser Fetch (Bypasses socket hang up)
            if "push2his.eastmoney.com" in url:
                return await worker.fetch_direct(url, rid)
            else:
                return await worker.fetch_browser(url, rid)
        finally:
            await self.queue.put(worker)


# =========================================================
# 5. Data Standardization
# =========================================================

def clean_jsonp(text: str):
    if not text: return None
    text = text.strip().lstrip('\ufeff')
    try:
        # Match JSONP wrapper
        match = re.search(r'^[^(]*?\((.*)\)[^)]*?$', text, re.DOTALL)
        if match: return json.loads(match.group(1))
        # Try raw JSON
        return json.loads(text)
    except:
        return None

def _div100(v):
    """Safely divide by 100 if value exists"""
    try:
        if v in (None, "-", ""): return None
        return float(v) / 100.0
    except:
        return None

def standardize_realtime(data: Dict) -> Dict:
    d = data.get("data", {}) if data.get("data") else data
    if not d: return {}

    # Map Eastmoney fields to StockRealtimeInfo
    return {
        "stockCode": d.get("f57"),
        "companyName": d.get("f58"),
        "price": _div100(d.get("f43")),
        "prevClose": _div100(d.get("f60")),
        "openPrice": _div100(d.get("f46")),
        "highPrice": _div100(d.get("f44")),
        "lowPrice": _div100(d.get("f45")),
        "volume": d.get("f47"),
        "turnover": d.get("f48"),
        # f170: Change Percent (e.g. 125 -> 1.25)
        "changePercent": _div100(d.get("f170")),
        # f50/f52: Volume Ratio (e.g. 85 -> 0.85)
        "volumeRatio": _div100(d.get("f50") or d.get("f52")),
        # f191/f20: Commission Ratio (e.g. 5944 -> 59.44)
        "commissionRatio": _div100(d.get("f191") or d.get("f20")),
        "mainFundsInflow": d.get("f152"),
        "turnoverRate": _div100(d.get("f168")),
        "peRatio": _div100(d.get("f162")),
        "pbRatio": _div100(d.get("f167")),
    }


def standardize_kline(data: Dict) -> List:
    # Use .get with default dict to prevent crash if 'data' key exists but value is None
    klines = (data.get("data") or {}).get("klines", [])
    stock_code = (data.get("data") or {}).get("code", "")
    res = []

    for line in klines:
        p = line.split(",")
        if len(p) >= 6:
            try:
                # Basic fields
                item = {
                    "trade_date": p[0],
                    "stock_code": stock_code,
                    "open": float(p[1]),
                    "close": float(p[2]),
                    "high": float(p[3]),
                    "low": float(p[4]),
                    "volume": int(p[5]),
                }
                # Extended fields
                if len(p) >= 11:
                    close_val = float(p[2])
                    change_val = float(p[9]) # Change amount
                    item.update({
                        "amount": float(p[6]),
                        "change": change_val,
                        "change_percent": float(p[8]), # Usually pre-formatted percentage
                        "turnover_ratio": float(p[10]),
                        "pre_close": close_val - change_val
                    })
                res.append(item)
            except (ValueError, IndexError):
                continue
    return res


def fix_url_params(url: str, fields: str = "") -> str:
    parsed = urllib.parse.urlparse(url)
    qs = dict(urllib.parse.parse_qsl(parsed.query))
    if fields and "fields" not in qs: qs["fields"] = fields
    # Default token
    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    # Cache buster / JSONP callback
    if "cb" not in qs:
        ts = int(time.time() * 1000)
        qs["cb"] = f"jQuery3510{random.randint(10 ** 15, 10 ** 16)}_{ts}"
        qs["_"] = str(ts)
    return urllib.parse.urlunparse(parsed._replace(query=urllib.parse.urlencode(qs)))


def normalize_secid(code: str) -> str:
    if "." in code: return code
    # Heuristic: 6/5 -> SH (1), others -> SZ (0)
    return f"1.{code}" if code.startswith(("6", "5")) else f"0.{code}"


# =========================================================
# 6. FastAPI App
# =========================================================

POOL = BrowserPool()

@asynccontextmanager
async def lifespan(app: FastAPI):
    await POOL.start()
    yield
    await POOL.stop()

app = FastAPI(lifespan=lifespan)

# Request Models
class BaseReq(BaseModel): secid: str
class UrlReq(BaseModel): url: str
class KlineReq(BaseModel): secid: str; ndays: Optional[int] = 0
class RangeReq(BaseModel): secid: str; beg: str; end: str
class USReq(BaseModel): secid: str; market: str
class TickReq(BaseModel): secid: str

@app.middleware("http")
async def log_requests(request: Request, call_next):
    rid = uuid.uuid4().hex[:6]
    request.state.rid = rid
    start_time = time.time()

    try:
        response = await call_next(request)
        cost = (time.time() - start_time) * 1000
        logger.info(f"[{rid}] {request.method} {request.url.path} -> {response.status_code} ({cost:.0f}ms)")
        return response
    except Exception as e:
        cost = (time.time() - start_time) * 1000
        logger.error(f"[{rid}] ERROR {request.url.path}: {e}")
        return Response(content=json.dumps({"error": str(e)}), status_code=500, media_type="application/json")


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    secid = normalize_secid(req.secid)
    url = fix_url_params(f"https://push2.eastmoney.com/api/qt/stock/get?secid={secid}", FULL_FIELDS)
    raw = await POOL.dispatch(url, request.state.rid)
    return standardize_realtime(clean_jsonp(raw) or {})


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url, request.state.rid)
    return standardize_realtime(clean_jsonp(raw) or {})


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url, request.state.rid)
    return standardize_realtime(clean_jsonp(raw) or {})


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    base_url = (
        f"https://push2.eastmoney.com/api/qt/stock/details/get?"
        f"secid={secid}"
        f"&fields1=f1,f2,f3,f4"
        f"&fields2=f51,f52,f53,f54,f55"
        f"&pos=-1000&num=1000"
        f"&forcect=1"
    )
    url = fix_url_params(base_url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw) or {}
    details = data.get("data", {}).get("details", [])
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}
    for p in details:
        parts = p.split(",")
        if len(parts) >= 5:
            try:
                result.append({
                    "time": parts[0],
                    "price": float(parts[1]),
                    "volume": int(parts[2]),
                    "side": side_map.get(parts[4], "其他")
                })
            except:
                continue
    return result


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = fix_url_params(
        f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61")
    raw = await POOL.dispatch(url, request.state.rid)
    res = standardize_kline(clean_jsonp(raw) or {})
    return res[-req.ndays:] if req.ndays else res


@app.post("/stock/kline/range")
async def stock_kline_range(req: RangeReq, request: Request):
    secid = normalize_secid(req.secid)
    url = fix_url_params(
        f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={req.beg}&end={req.end}&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61")
    raw = await POOL.dispatch(url, request.state.rid)
    return standardize_kline(clean_jsonp(raw) or {})


@app.post("/stock/kline/us")
async def stock_kline_us(req: USReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = fix_url_params(
        f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61")
    raw = await POOL.dispatch(url, request.state.rid)
    return standardize_kline(clean_jsonp(raw) or {})


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url, request.state.rid)
    return clean_jsonp(raw)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, access_log=False)
