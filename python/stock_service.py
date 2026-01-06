#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service

核心说明：
- 本服务使用 Playwright 作为浏览器执行环境
- 通过 Page Pool（页面池）模型保证并发安全
- 每个请求独占一个 Page，用完即归还
- 避免 Page 并发共享导致的 execution context destroyed 问题
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
# 日志系统（完全保留你的原设计）
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name, filename, fmt):
    """
    构建按天滚动的文件日志
    """
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.handlers.clear()

    fh = TimedRotatingFileHandler(
        os.path.join(LOG_DIR, filename),
        when="midnight",
        interval=1,
        backupCount=14,
        encoding="utf-8",
    )
    fh.setFormatter(fmt)
    logger.addHandler(fh)
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

# 关闭 uvicorn 默认 access log，避免重复
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI 应用
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.1")

# =========================================================
# 运行期全局对象
# =========================================================

# Playwright 主实例
PLAYWRIGHT: Optional[Any] = None

# Chromium 浏览器实例
BROWSER: Optional[Browser] = None

# 并发限流（防止瞬时请求把浏览器拖死）
SEMAPHORE: Optional[asyncio.Semaphore] = None

# Page Pool：核心并发安全设计
# 每一个 page 在同一时间只会被一个请求使用
PAGE_POOL: asyncio.Queue = asyncio.Queue()


# =========================================================
# Trace 日志适配器（保留原逻辑）
# =========================================================

class TraceAdapter(logging.LoggerAdapter):
    """
    为 trace 日志自动注入 request_id
    """

    def process(self, msg, kwargs):
        kwargs.setdefault("extra", {})
        kwargs["extra"]["request_id"] = self.extra["request_id"]
        return msg, kwargs


# =========================================================
# 中间件：访问日志（完全保留）
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
# 工具函数
# =========================================================

def parse_json_or_jsonp(text: str):
    """
    支持 JSON / JSONP 自动解析
    """
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        pass

    # JSONP 兜底
    m = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if m:
        try:
            return json.loads(m.group(1))
        except Exception:
            pass
    return None


def normalize_secid(code: str) -> str:
    """
    东方财富 secid 规范化
    """
    if "." in code:
        return code
    if code.startswith(("83", "43")):
        return f"113.{code}"
    if code.startswith(("6", "5")):
        return f"1.{code}"
    return f"0.{code}"


# =========================================================
# 生命周期：启动
# =========================================================

@app.on_event("startup")
async def startup():
    """
    服务启动时：
    1. 启动 Playwright
    2. 启动 Chromium
    3. 初始化 Page Pool
    """
    global PLAYWRIGHT, BROWSER, SEMAPHORE

    logger.info("Starting Playwright...")

    PLAYWRIGHT = await async_playwright().start()

    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )

    # 控制并发请求总数（不是 page 数）
    SEMAPHORE = asyncio.Semaphore(10)

    page_count = 4  # Page Pool 大小，可根据机器性能调整

    for i in range(page_count):
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )

        # 隐藏 webdriver 特征
        await context.add_init_script(
            "() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})"
        )

        page = await context.new_page()

        # 预热页面，建立 session / cookie
        try:
            await page.goto(
                "https://quote.eastmoney.com/",
                wait_until="domcontentloaded",
                timeout=30000,
            )
            logger.info(f"Warmup page {i} OK")
        except Exception as e:
            logger.warning(f"Warmup page {i} ignored: {e}")

        # 放入 Page Pool
        await PAGE_POOL.put(page)

    logger.info("Browser started. Page pool ready.")


# =========================================================
# 生命周期：关闭
# =========================================================

@app.on_event("shutdown")
async def shutdown():
    """
    优雅关闭：
    - 逐个关闭 page
    - 关闭 browser
    - 停止 playwright
    """
    while not PAGE_POOL.empty():
        page = await PAGE_POOL.get()
        await page.close()

    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()


# =========================================================
# 核心函数：安全的浏览器 fetch（Page Pool 版）
# =========================================================

async def fetch_json_with_browser(url: str, request_id: str, max_retry=3):
    """
    核心设计原则：
    - 一个请求独占一个 page
    - 不 reload page
    - 不并发共享 page
    """

    trace = TraceAdapter(trace_logger, {"request_id": request_id})
    trace.info(f"Fetch start url={url}")

    # 如果 page 全被占用，直接拒绝
    if PAGE_POOL.empty():
        raise HTTPException(status_code=503, detail="Browser busy")

    async with SEMAPHORE:
        page = await PAGE_POOL.get()
        try:
            for attempt in range(1, max_retry + 1):
                try:
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

                    parsed = parse_json_or_jsonp(text)
                    if parsed:
                        return parsed

                except PlaywrightError as e:
                    trace.warning(f"[Attempt {attempt}] PlaywrightError: {e}")
                    await asyncio.sleep(0.2)

            raise HTTPException(status_code=500, detail="Upstream fetch failed")

        finally:
            # ❗无论成功失败，必须归还 page
            await PAGE_POOL.put(page)


# =========================================================
# 请求模型（全部保留）
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
# 业务辅助函数（保留）
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
# 接口定义（全部原样保留）
# =========================================================

@app.get("/health")
def health():
    return {
        "status": "ok",
        "browser": BROWSER is not None,
        "page_pool": PAGE_POOL.qsize(),
    }


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

            # 提前处理可能的转换错误
            try:
                amount_value = round(float(arr[6]), 2) if arr[6] and arr[6].strip() else 0.0
            except (ValueError, TypeError):
                amount_value = 0.0

            # 计算涨跌幅（假设东方财富数据中 arr[9] 已经是涨跌幅）
            # 如果 arr[9] 就是涨跌幅百分比，可以直接用
            try:
                change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0
            except (ValueError, TypeError):
                change_percent_value = 0.0

            # 计算振幅（日内波动）
            try:
                high = float(arr[3])
                low = float(arr[4])
                amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0
            except (ValueError, TypeError, ZeroDivisionError):
                amplitude = 0.0

            result.append({
                "trade_date": arr[0],
                "trade_time": None,
                "stock_code": req.secid,
                "open": float(arr[1]),
                "close": float(arr[2]),
                "high": float(arr[3]),
                "low": float(arr[4]),
                "volume": int(arr[5]),
                "amount": amount_value,
                "change": float(arr[9]) if len(arr) > 9 else 0.0,  # 涨跌额
                "change_percent": change_percent_value,  # 涨跌幅
                "amplitude": amplitude,  # 振幅
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0,
                "pre_close": None
            })
        except Exception as e:
            logger.error("Parsing line failed. [line=%s, error=%s]", line, str(e))
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
