#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service

【服务定位】
- 提供股票 / ETF / 美股的实时行情与 K 线数据
- 数据源：东方财富
- 访问方式：通过 Playwright 驱动真实浏览器 fetch，规避反爬

【核心设计点】
1. 使用 Page Pool（页面池）模型
   - 每个请求独占一个 page
   - 请求结束后归还 page
   - 避免 Playwright page 并发使用导致 execution context destroyed

2. 三层日志体系
   - service 日志：服务运行状态
   - access 日志：HTTP 请求访问日志
   - trace 日志：单请求链路追踪（request_id）

3. 严格受控的并发
   - Semaphore 控制总体并发
   - Page Pool 控制浏览器资源
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
    """
    构建一个按天切割的文件日志 Logger

    :param name: logger 名称
    :param filename: 日志文件名
    :param fmt: 日志格式
    """
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)

    # 防止重复添加 handler（例如热重载）
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


# 服务运行日志
logger = _build_logger(
    "stock-service",
    "stock_service.log",
    logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s"),
)

# HTTP 访问日志
access_logger = _build_logger(
    "access",
    "access.log",
    logging.Formatter("%(message)s"),
)

# 请求级 trace 日志
trace_logger = _build_logger(
    "trace",
    "trace.log",
    logging.Formatter(
        "[%(asctime)s] [%(levelname)s] [%(request_id)s] %(message)s"
    ),
)

# 关闭 uvicorn 自带 access log，避免重复记录
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI 应用
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.1")

# =========================================================
# 运行期全局对象（生命周期由 FastAPI 控制）
# =========================================================

PLAYWRIGHT: Optional[Any] = None  # Playwright 主实例
BROWSER: Optional[Browser] = None  # Chromium 浏览器实例
SEMAPHORE: Optional[asyncio.Semaphore] = None  # 总并发控制
PAGE_POOL: asyncio.Queue = asyncio.Queue()  # Page 池（核心并发安全机制）


# =========================================================
# Trace 日志适配器
# =========================================================

class TraceAdapter(logging.LoggerAdapter):
    """
    给 trace_logger 自动注入 request_id
    用于单请求链路追踪
    """

    def process(self, msg, kwargs):
        kwargs.setdefault("extra", {})
        kwargs["extra"]["request_id"] = self.extra["request_id"]
        return msg, kwargs


# =========================================================
# HTTP 访问日志中间件
# =========================================================

@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    """
    为每一个 HTTP 请求生成 request_id，
    并记录访问日志（耗时、状态码、异常）
    """
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
    """
    自动解析 JSON / JSONP 返回内容
    """
    if not text:
        return None

    # 尝试直接 JSON
    try:
        return json.loads(text)
    except Exception:
        pass

    # JSONP 兜底
    match = re.search(r"\w+\((.*)\)\s*;?$", text, flags=re.S)
    if match:
        try:
            return json.loads(match.group(1))
        except Exception:
            pass

    return None


def normalize_secid(code: str) -> str:
    """
    将股票代码规范化为东方财富 secid 格式

    规则说明：
    - 沪市：1.xxxxxx
    - 深市：0.xxxxxx
    - 北交所：113.xxxxxx
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
    服务启动流程：
    1. 启动 Playwright
    2. 启动 Chromium 浏览器
    3. 初始化 Page Pool
    """
    global PLAYWRIGHT, BROWSER, SEMAPHORE

    logger.info("Starting Playwright...")

    PLAYWRIGHT = await async_playwright().start()

    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )

    # 控制整体并发（防止瞬时流量拖垮浏览器）
    SEMAPHORE = asyncio.Semaphore(10)

    page_count = 4  # Page Pool 大小，可按机器性能调整

    for i in range(page_count):
        context = await BROWSER.new_context(
            locale="zh-CN",
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )

        # 隐藏 webdriver 特征，降低被识别概率
        await context.add_init_script(
            "() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})"
        )

        page = await context.new_page()

        # 预热页面，建立 cookie / session
        try:
            await page.goto(
                "https://quote.eastmoney.com/",
                wait_until="domcontentloaded",
                timeout=30000,
            )
            logger.info(f"Warmup page {i} OK")
        except Exception as e:
            logger.warning(f"Warmup page {i} ignored: {e}")

        await PAGE_POOL.put(page)

    logger.info("Browser started. Page pool ready.")


# =========================================================
# 生命周期：关闭
# =========================================================

@app.on_event("shutdown")
async def shutdown():
    """
    优雅关闭：
    - 关闭所有 page
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
                    # 注意：这里我们修改了 evaluate 的参数传递方式，把 [url, headers] 传进去
                    text = await page.evaluate(
                        """async ([url, headers]) => {
                            const controller = new AbortController();
                            const timer = setTimeout(() => controller.abort(), 8000);

                            try {
                                const r = await fetch(url, {
                                    method: 'GET',
                                    credentials: 'include',
                                    signal: controller.signal,
                                    headers: headers  // 【关键】使用 Python 传入的 Headers
                                });
                                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                                return await r.text();
                            } finally {
                                clearTimeout(timer);
                            }
                        }""",
                        [url, current_headers],  # 传参
                    )

                    parsed = parse_json_or_jsonp(text)
                    if parsed:
                        return parsed

                    if attempt == max_retry:
                        trace.warning(f"Response parsed empty. content[:50]={text[:50]}")

                except Exception as e:
                    error_msg = str(e)
                    trace.warning(f"[Attempt {attempt}] Error: {error_msg}")

                    if "Target closed" in error_msg or "Session closed" in error_msg:
                        page_ok = False
                        break

                    # =================================================
                    # 触发认证刷新逻辑 (核武器)
                    # =================================================
                    # 如果是 Failed to fetch，或者是 403 禁止访问，说明当前环境/Header 失效
                    if ("Failed to fetch" in error_msg or "HTTP 403" in error_msg) and attempt < max_retry:
                        trace.warning("Detected potential auth failure. Launching rescue browser...")
                        # 使用锁，防止多个并发请求同时弹浏览器
                        if not AUTH_LOCK.locked():
                            async with AUTH_LOCK:
                                await _refresh_auth_headers_if_needed()
                        else:
                            # 如果别人正在刷新，我稍微等一下
                            await asyncio.sleep(2)
                        # 刷新完后，下一次循环会自动使用新的 CACHED_HEADERS

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
# 请求模型
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


# =========================================================
# 实时行情辅助函数
# =========================================================

def safe_get(dct, key):
    return dct.get(key)


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    """
    将东方财富实时行情字段映射为语义化结构
    """

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


def normalize_record(obj: Dict[str, Any]) -> Dict[str, Any]:
    """清洗 Key 隐藏字符并补齐字段"""
    clean = {}
    for k, v in obj.items():
        nk = re.sub(r"[\r\n\t ]+", "", k)
        if nk:
            clean[nk] = v

    # 确保字段完整性
    required = ["stockCode", "time", "price", "volume", "side", "sideCode", "tickCount", "avgVol"]
    for f in required:
        if f not in clean:
            clean[f] = None
    return clean


def standardize_tick(data: Dict[str, Any], stock_code: str) -> list:
    """解析东方财富实时明细数据"""
    details = data.get("details", [])
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}

    for item in details:
        parts = item.split(",")
        if len(parts) < 5:
            continue
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


# =========================================================
# 接口定义
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
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"):
        raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickRequest, request: Request):
    """
    获取实时分笔交易数据 (Snapshot)
    """
    secid = normalize_secid(req.secid)

    parts = secid.split('.')
    stock_code = parts[1] if len(parts) > 1 else secid

    ts = int(time.time() * 1000)
    url = (
        "https://push2.eastmoney.com/api/qt/stock/details/get?"
        f"secid={secid}&ut=bd1d9ddb04089700cf9c27f6f7426281&"
        "fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&"
        f"pos=-1000000&num=1000000&cb=jQuery_{ts}&_={ts}"
    )

    raw = await fetch_json_with_browser(url, request.state.request_id)
    if not raw or not raw.get("data"):
        return []

    return standardize_tick(raw["data"], stock_code)


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest, request: Request):
    """
    按 ndays 回溯获取日 K 线数据

    输出字段说明：
    - trade_date        交易日期（YYYY-MM-DD）
    - open / close      开盘价 / 收盘价
    - high / low        最高价 / 最低价
    - volume            成交量
    - amount            成交额
    - change            涨跌额（东方财富原始字段）
    - change_percent    涨跌幅（%）
    - amplitude         振幅（%）
    - turnover_ratio    换手率
    """
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
    klines = raw.get("data", {}).get("klines", [])

    result = []

    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11:
                continue

            amount_value = round(float(arr[6]), 2) if arr[6] else 0.0
            change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0

            high = float(arr[3])
            low = float(arr[4])
            amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0

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
                "change": float(arr[9]) if len(arr) > 9 else 0.0,
                "change_percent": change_percent_value,
                "amplitude": amplitude,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0,
                "pre_close": None
            })
        except Exception as e:
            logger.error("Parsing line failed. [line=%s, error=%s]", line, str(e))

    return result


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeRequest, request: Request):
    # 与你原始实现一致（同样的字段解析逻辑）
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

            amount_value = round(float(arr[6]), 2) if arr[6] else 0.0
            change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0

            high = float(arr[3])
            low = float(arr[4])
            amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0

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
                "change": float(arr[9]) if len(arr) > 9 else 0.0,
                "change_percent": change_percent_value,
                "amplitude": amplitude,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0,
                "pre_close": None
            })
        except Exception:
            # 单条异常直接跳过，避免整体失败
            logger.error("Parsing line failed. [line=%s]", line)
            continue

    return result


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineRequest, request: Request):
    """
    美股日 K 线接口

    说明：
    - 与 A 股 K 线接口保持完全一致的出参结构
    - 仅 secid 规则不同（market.secid）
    """

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
    klines = raw.get("data", {}).get("klines", [])

    result = []

    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11:
                continue

            # 成交额
            try:
                amount_value = round(float(arr[6]), 2) if arr[6] else 0.0
            except Exception:
                amount_value = 0.0

            # 涨跌幅（东方财富原始字段）
            try:
                change_percent_value = float(arr[9]) if len(arr) > 9 else 0.0
            except Exception:
                change_percent_value = 0.0

            # 振幅 = (high - low) / low * 100
            try:
                high = float(arr[3])
                low = float(arr[4])
                amplitude = round(((high - low) / low) * 100, 2) if low != 0 else 0.0
            except Exception:
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
                "change": float(arr[9]) if len(arr) > 9 else 0.0,
                "change_percent": change_percent_value,
                "amplitude": amplitude,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0.0,
                "pre_close": None
            })
        except Exception as e:
            logger.error(
                "Parsing US kline failed. [line=%s, error=%s]",
                line,
                str(e)
            )

    return result


@app.post("/proxy/json")
async def proxy_json(req: GenericJsonRequest, request: Request):
    data = await fetch_json_with_browser(req.url, request.state.request_id)
    if data is None:
        logger.error("Browser started. [url=%s]", req.url)
        raise HTTPException(status_code=502, detail="Invalid upstream JSON")
    return data
