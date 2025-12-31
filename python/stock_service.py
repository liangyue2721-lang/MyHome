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
# 1. 引入 Playwright 的 Error 类型以便捕获
from playwright.async_api import async_playwright, Browser, Error as PlaywrightError

# =========================================================
# Logging Setup
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)

# ---------- main logger ----------
logger = logging.getLogger("stock-service")
logger.setLevel(logging.INFO)

formatter = logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
)

for h in list(logger.handlers):
    logger.removeHandler(h)

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

# ---------- access logger ----------
access_logger = logging.getLogger("access")
access_logger.setLevel(logging.INFO)

for h in list(access_logger.handlers):
    access_logger.removeHandler(h)

access_fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "access.log"),
    when="midnight",
    interval=1,
    backupCount=14,
    encoding="utf-8"
)
access_fh.setFormatter(logging.Formatter("%(message)s"))
access_logger.addHandler(access_fh)

# ---------- trace logger ----------
trace_logger = logging.getLogger("trace")
trace_logger.setLevel(logging.INFO)

for h in list(trace_logger.handlers):
    trace_logger.removeHandler(h)

trace_fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "trace.log"),
    when="midnight",
    interval=1,
    backupCount=14,
    encoding="utf-8"
)
trace_fh.setFormatter(
    logging.Formatter(
        "[%(asctime)s] [%(levelname)s] [%(request_id)s] %(message)s",
        "%Y-%m-%d %H:%M:%S"
    )
)
trace_logger.addHandler(trace_fh)

logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.1")

# =========================================================
# Global Runtime
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
        duration_ms = int((time.time() - start_time) * 1000)

        access_logger.info(json.dumps({
            "ts": datetime.utcnow().isoformat() + "Z",
            "rid": request_id,
            "ip": request.client.host if request.client else None,
            "method": request.method,
            "path": request.url.path,
            "status": status_code,
            "cost_ms": duration_ms,
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

    # 创建长期页面
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
            # 2. 增加 wait_until='networkidle' 让初始化更稳定
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
# Browser Fetch (核心修复部分)
# =========================================================

async def fetch_json_with_browser(url: str, request_id: str, max_retry=3):
    global PAGE_INDEX

    trace = TraceAdapter(trace_logger, {"request_id": request_id})

    if not PAGES:
        raise HTTPException(status_code=500, detail="Browser service not ready")

    async with SEMAPHORE:
        async with PAGE_LOCK:
            page = PAGES[PAGE_INDEX]
            page_idx = PAGE_INDEX
            PAGE_INDEX = (PAGE_INDEX + 1) % len(PAGES)

        trace.info(f"Use page index={page_idx}")
        trace.info(f"Fetch start: {url}")

        for attempt in range(1, max_retry + 1):
            start = time.time()
            text = None

            try:
                # 3. 执行 JS Fetch
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

            # 4. 捕获 Playwright 特定错误
            except PlaywrightError as e:
                err_msg = str(e)
                # 核心修复：如果上下文被销毁，或者页面崩溃，则重新加载页面
                if "Execution context was destroyed" in err_msg or "Target closed" in err_msg:
                    trace.warning(f"Page context lost (Attempt {attempt}): {err_msg}. Reloading page...")
                    try:
                        # 尝试刷新页面来恢复环境
                        await page.reload(wait_until="domcontentloaded", timeout=10000)
                        trace.info("Page reloaded successfully. Retrying fetch...")
                        continue  # 跳过本次循环的剩余部分，直接进入下一次 attempt
                    except Exception as reload_e:
                        trace.error(f"Failed to reload page: {reload_e}")
                        # 如果刷新都失败了，可能需要抛出异常或尝试下一个页面(这里选择继续抛出让上层处理)

                # 如果是其他错误，记录并继续尝试
                trace.warning(f"Playwright error on attempt {attempt}: {e}")

            except Exception as e:
                trace.warning(f"General error on attempt {attempt}: {e}")

            # 5. 如果没有拿到 text (报错了)，则进入下一次重试
            if text is None:
                continue

            cost = int((time.time() - start) * 1000)
            trace.info(f"Attempt {attempt} done cost={cost}ms size={len(text)}")

            parsed = parse_json_or_jsonp(text)
            if parsed:
                trace.info("JSON parse success")
                return parsed

            trace.warning(f"Parse failed attempt={attempt} snippet={text[:200]!r}")

        trace.error("All retries failed")
        raise HTTPException(status_code=500, detail="Upstream fetch failed")


# =========================================================
# Models
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
        if val in (None, "", "-"):
            return None
        try:
            return float(val) / 100
        except Exception:
            return None

    def _numeric_or_none(val):
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
        "volume": _numeric_or_none(safe_get(data, "f47")),
        "turnover": _numeric_or_none(safe_get(data, "f48")),
        "volumeRatio": _numeric_or_none(safe_get(data, "f52")),
        "commissionRatio": _numeric_or_none(safe_get(data, "f20")),
        "mainFundsInflow": _numeric_or_none(safe_get(data, "f152")),
    }


# =========================================================
# Endpoints（接口不变）
# =========================================================

@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}


@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest, request: Request):
    data_json = await fetch_json_with_browser(req.url, request.state.request_id)
    if not data_json or not data_json.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(data_json["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest, request: Request):
    data_json = await fetch_json_with_browser(req.url, request.state.request_id)
    if not data_json or not data_json.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(data_json["data"])


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest, request: Request):
    secid = normalize_secid(req.secid)
    lookback = req.ndays * 2 + 20
    beg_date = (datetime.now() - timedelta(days=lookback)).strftime("%Y%m%d")

    url = (
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
        "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&"
        "fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&"
        f"beg={beg_date}&end=20500101&secid={secid}&klt=101&fqt=1"
    )

    raw_json = await fetch_json_with_browser(url, request.state.request_id)
    if not raw_json or not raw_json.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")

    return raw_json["data"].get("klines", [])
