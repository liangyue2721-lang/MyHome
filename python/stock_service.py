#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v1.9 (New Snapshot Interface)
【更新说明】
1. 新增 /stock/snapshot 接口，通过 secid 获取个股详情
2. 保持 v1.8 的稳定性修复和动态 Headers 逻辑
"""

import os
import json
import logging
import asyncio
import re
import time
import uuid
import random
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
# 1. 日志系统初始化
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name: str, filename: str, fmt: logging.Formatter):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
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


logger = _build_logger("stock-service", "stock_service.log",
                       logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s"))
access_logger = _build_logger("access", "access.log", logging.Formatter("%(message)s"))
trace_logger = _build_logger("trace", "trace.log",
                             logging.Formatter("[%(asctime)s] [%(levelname)s] [%(request_id)s] %(message)s"))
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# 2. FastAPI 应用 & 全局状态
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.9.0")

PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE: Optional[asyncio.Semaphore] = None
PAGE_POOL: asyncio.Queue = asyncio.Queue()

AUTH_LOCK = asyncio.Lock()
CACHED_HEADERS = {}
LAST_AUTH_TIME = 0


# =========================================================
# 3. 中间件与工具
# =========================================================

class TraceAdapter(logging.LoggerAdapter):
    def process(self, msg, kwargs):
        kwargs.setdefault("extra", {})
        kwargs["extra"]["request_id"] = self.extra["request_id"]
        return msg, kwargs


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
        access_logger.info(json.dumps({
            "ts": datetime.utcnow().isoformat() + "Z",
            "rid": request_id,
            "path": request.url.path,
            "method": request.method,
            "status": status_code,
            "cost_ms": int((time.time() - start_time) * 1000),
            "error": error,
        }, ensure_ascii=False))


def parse_json_or_jsonp(text: str):
    if not text: return None
    try:
        return json.loads(text)
    except:
        pass
    match = re.search(r"^\s*(?:jQuery\w+|callback|cb\w*)\s*\((.*)\)\s*;?\s*$", text, flags=re.S)
    if match:
        try:
            return json.loads(match.group(1))
        except:
            pass
    return None


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


# =========================================================
# 4. 生命周期管理
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER, SEMAPHORE
    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()

    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=[
            "--disable-blink-features=AutomationControlled",
            "--disable-web-security",
            "--ignore-certificate-errors",
            "--no-sandbox",
        ],
    )

    SEMAPHORE = asyncio.Semaphore(15)

    # 启动前预热一次 Headers
    await _refresh_auth_headers_if_needed()

    for i in range(5):
        context = await BROWSER.new_context(
            locale="zh-CN",
            ignore_https_errors=True,
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        )
        await context.add_init_script("() => Object.defineProperty(navigator,'webdriver',{get:()=>undefined})")
        page = await context.new_page()
        # 初始停靠在同域名下，后续直接 fetch 不报错
        await page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded")
        await PAGE_POOL.put(page)
    logger.info("Browser started. Page pool ready.")


@app.on_event("shutdown")
async def shutdown():
    while not PAGE_POOL.empty():
        page = await PAGE_POOL.get()
        await page.close()
    if BROWSER: await BROWSER.close()
    if PLAYWRIGHT: await PLAYWRIGHT.stop()


async def _recreate_page():
    try:
        if not BROWSER: return
        context = await BROWSER.new_context(locale="zh-CN", ignore_https_errors=True)
        new_page = await context.new_page()
        await new_page.goto("https://quote.eastmoney.com/", wait_until="domcontentloaded")
        await PAGE_POOL.put(new_page)
    except Exception as e:
        logger.error(f"Failed to recreate page: {e}")


# =========================================================
# 5. 自动认证逻辑 (动态抓取修复)
# =========================================================

async def _refresh_auth_headers_if_needed():
    global CACHED_HEADERS, LAST_AUTH_TIME
    if time.time() - LAST_AUTH_TIME < 60 and CACHED_HEADERS:
        return

    logger.info(">>> [Auth] 正在动态抓取最新 Headers 和 Cookies...")
    async with async_playwright() as p:
        # 使用本地 channel 往往指纹更真实，报错几率更小
        auth_browser = await p.chromium.launch(headless=True)
        context = await auth_browser.new_context(ignore_https_errors=True)
        page = await context.new_page()

        captured = {"headers": {}}
        # 增加一个内部标志位，防止浏览器关闭后回调继续执行 API 调用
        is_closing = False

        async def handle_request(request):
            # 如果浏览器正在关闭，或者已经抓取到了，直接跳过
            if is_closing or captured["headers"]:
                return

            try:
                if ("api/qt/stock" in request.url or "push2" in request.url):
                    # 在调用 all_headers 这一步最容易报 TargetClosedError
                    h = await request.all_headers()
                    valid = {k: v for k, v in h.items() if not k.startswith(':') and k.lower() != 'content-length'}

                    cookies = await context.cookies()
                    valid["cookie"] = "; ".join([f"{c['name']}={c['value']}" for c in cookies])
                    captured["headers"] = valid
            except Exception:
                # 捕获并静默忽略所有 TargetClosedError
                pass

        page.on("request", handle_request)
        try:
            # wait_until="networkidle" 确保请求已经发完
            await page.goto("https://quote.eastmoney.com/sz000001.html", wait_until="networkidle", timeout=20000)

            if captured["headers"]:
                CACHED_HEADERS = captured["headers"]
                LAST_AUTH_TIME = time.time()
                logger.info(">>> [Auth] 动态认证信息更新成功")
        except Exception as e:
            logger.error(f">>> [Auth] 抓取流程异常: {e}")
        finally:
            is_closing = True  # 标记即将关闭，不再处理新回调
            # 移除所有监听器，彻底切断回调链
            page.remove_listener("request", handle_request)
            await auth_browser.close()


# =========================================================
# 6. 核心 Fetch 逻辑 (修复 Failed to fetch)
# =========================================================

async def fetch_json_with_browser(url: str, request_id: str, max_retry: int = 3):
    trace = TraceAdapter(trace_logger, {"request_id": request_id})
    if PAGE_POOL.empty(): raise HTTPException(status_code=503, detail="Browser busy")

    async with SEMAPHORE:
        page = await PAGE_POOL.get()
        page_ok = True
        try:
            for attempt in range(1, max_retry + 1):
                try:
                    # 【核心修复】必须保证当前页面在目标域名下，否则 JS Fetch 会报 TypeError
                    if "eastmoney.com" not in page.url:
                        await page.goto("https://quote.eastmoney.com/", wait_until="commit")

                    text = await page.evaluate(
                        """async ([url, headers]) => {
                            const resp = await fetch(url, {
                                method: 'GET',
                                credentials: 'include',
                                headers: headers
                            });
                            return await resp.text();
                        }""",
                        [url, CACHED_HEADERS],
                    )

                    parsed = parse_json_or_jsonp(text)
                    if parsed: return parsed

                except Exception as e:
                    error_msg = str(e)
                    trace.warning(f"[Attempt {attempt}] Error: {error_msg}")

                    if "Target closed" in error_msg:
                        page_ok = False
                        break

                    if attempt < max_retry:
                        if not AUTH_LOCK.locked():
                            async with AUTH_LOCK: await _refresh_auth_headers_if_needed()
                        await asyncio.sleep(1)

            raise HTTPException(status_code=502, detail="Fetch failed")
        finally:
            if page_ok:
                await PAGE_POOL.put(page)
            else:
                asyncio.create_task(_recreate_page())


# =========================================================
# 7. 数据模型与接口 (保持原始逻辑)
# =========================================================

class RealtimeRequest(BaseModel): url: str


class KlineRequest(BaseModel): secid: str; ndays: int


class KlineRangeRequest(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineRequest(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class GenericJsonRequest(BaseModel): url: str


class TickRequest(BaseModel): secid: str


# ============ 新增：Snapshot 请求模型 ============
class StockSnapshotRequest(BaseModel):
    secid: str


# ===============================================


def safe_get(dct, key): return dct.get(key)


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    def _div100(val):
        if val in (None, "", "-"): return None
        try:
            return float(val) / 100
        except:
            return None

    def _num(val):
        return None if val in (None, "", "-") else val

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
    clean = {}
    for k, v in obj.items():
        nk = re.sub(r"[\r\n\t ]+", "", k)
        if nk: clean[nk] = v
    for f in ["stockCode", "time", "price", "volume", "side", "sideCode", "tickCount", "avgVol"]:
        if f not in clean: clean[f] = None
    return clean


def standardize_tick(data: Dict[str, Any], stock_code: str) -> list:
    details = data.get("details", [])
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}
    for item in details:
        parts = item.split(",")
        if len(parts) < 5: continue
        try:
            vol = int(parts[2]);
            ticks = int(parts[3]);
            side_code = parts[4]
            avg_vol = vol / ticks if ticks > 0 else 0
            raw_obj = {
                "stockCode": stock_code, "time": parts[0], "price": float(parts[1]),
                "volume": vol, "side": side_map.get(side_code, "其他"),
                "sideCode": side_code, "tickCount": ticks, "avgVol": round(avg_vol, 2)
            }
            result.append(normalize_record(raw_obj))
        except:
            continue
    return result


@app.get("/health")
def health():
    return {
        "status": "ok",
        "browser": BROWSER is not None,
        "page_pool": PAGE_POOL.qsize(),
        "last_auth": datetime.fromtimestamp(LAST_AUTH_TIME).isoformat() if LAST_AUTH_TIME else None
    }


@app.post("/stock/realtime")
async def stock_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"): raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


# ================== 新增：Snapshot 接口 ==================
@app.post("/stock/snapshot")
async def stock_snapshot(req: StockSnapshotRequest, request: Request):
    """
    根据 secid 获取实时快照数据
    自动构建 URL，并在失败时(默认)使用浏览器获取
    """
    # 1. 构造时间戳
    ts = int(time.time() * 1000)

    # 2. 构造 Fields (保留原始链接中的完整字段)
    fields = "f58%2Cf734%2Cf107%2Cf57%2Cf43%2Cf59%2Cf169%2Cf301%2Cf60%2Cf170%2Cf152%2Cf177%2Cf111%2Cf46%2Cf44%2Cf45%2Cf47%2Cf260%2Cf48%2Cf261%2Cf279%2Cf277%2Cf278%2Cf288%2Cf19%2Cf17%2Cf531%2Cf15%2Cf13%2Cf11%2Cf20%2Cf18%2Cf16%2Cf14%2Cf12%2Cf39%2Cf37%2Cf35%2Cf33%2Cf31%2Cf40%2Cf38%2Cf36%2Cf34%2Cf32%2Cf211%2Cf212%2Cf213%2Cf214%2Cf215%2Cf210%2Cf209%2Cf208%2Cf207%2Cf206%2Cf161%2Cf49%2Cf171%2Cf50%2Cf86%2Cf84%2Cf85%2Cf168%2Cf108%2Cf116%2Cf167%2Cf164%2Cf162%2Cf163%2Cf92%2Cf71%2Cf117%2Cf292%2Cf51%2Cf52%2Cf191%2Cf192%2Cf262%2Cf294%2Cf295%2Cf269%2Cf270%2Cf256%2Cf257%2Cf285%2Cf286%2Cf748%2Cf747"

    # 3. 构造完整 URL
    # 注意：wbp2u 参数中包含了管道符 |，在 URL 中通常转义为 %7C
    target_url = (
        f"https://push2.eastmoney.com/api/qt/stock/get"
        f"?invt=2&fltt=1"
        f"&cb=jQuery35105931811675311084_{ts}"  # 动态 callback
        f"&fields={fields}"
        f"&secid={req.secid}"  # 动态 secid
        f"&ut=fa5fd1943c7b386f172d6893dbfba10b"
        f"&wbp2u=%7C0%7C1%7C0%7Cweb"
        f"&dect=1"
        f"&_={ts}"  # 动态时间戳
    )

    # 4. 调用浏览器获取 (满足"访问不通时尝试调用浏览器"的需求，这里直接使用浏览器以保证稳定性)
    raw = await fetch_json_with_browser(target_url, request.state.request_id)

    if not raw or not raw.get("data"):
        raise HTTPException(status_code=404, detail="Stock Data Not Found")

    # 5. 返回标准化数据
    return standardize_realtime_data(raw["data"])


# ========================================================


@app.post("/etf/realtime")
async def etf_realtime(req: RealtimeRequest, request: Request):
    raw = await fetch_json_with_browser(req.url, request.state.request_id)
    if not raw or not raw.get("data"): raise HTTPException(status_code=404, detail="Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickRequest, request: Request):
    secid = normalize_secid(req.secid)
    stock_code = secid.split('.')[1] if '.' in secid else secid
    ts = int(time.time() * 1000)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&ut=bd1d9ddb04089700cf9c27f6f7426281&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1000000&num=1000000&cb=jQuery_{ts}&_={ts}"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    if not raw or not raw.get("data"): return []
    return standardize_tick(raw["data"], stock_code)


@app.post("/stock/kline")
async def stock_kline(req: KlineRequest, request: Request):
    secid = normalize_secid(req.secid)
    lookback = req.ndays * 2 + 20
    beg = (datetime.now() - timedelta(days=lookback)).strftime("%Y%m%d")
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg={beg}&end=20500101&secid={secid}&klt=101&fqt=1"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])
    result = []
    for line in klines:
        try:
            arr = line.split(",")
            if len(arr) < 11: continue
            high = float(arr[3]);
            low = float(arr[4])
            result.append({
                "trade_date": arr[0], "stock_code": req.secid, "open": float(arr[1]),
                "close": float(arr[2]), "high": high, "low": low, "volume": int(arr[5]),
                "amount": round(float(arr[6]), 2), "change": float(arr[9]),
                "change_percent": float(arr[9]), "amplitude": round(((high - low) / low) * 100, 2) if low != 0 else 0,
                "turnover_ratio": float(arr[10]) if len(arr) > 10 else 0
            })
        except:
            continue
    return result


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeRequest, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101";
    end = req.end or "20500101"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg={beg}&end={end}&secid={secid}&klt=101&fqt=1"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])
    result = []
    for line in klines:
        try:
            arr = line.split(",");
            high = float(arr[3]);
            low = float(arr[4])
            result.append({
                "trade_date": arr[0], "stock_code": req.secid, "open": float(arr[1]),
                "close": float(arr[2]), "high": high, "low": low, "volume": int(arr[5]),
                "amount": round(float(arr[6]), 2), "change_percent": float(arr[9]),
                "amplitude": round(((high - low) / low) * 100, 2) if low != 0 else 0
            })
        except:
            continue
    return result


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineRequest, request: Request):
    secid = f"{req.market}.{req.secid}"
    beg = req.beg or "19900101";
    end = req.end or "20500101"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg={beg}&end={end}&secid={secid}&klt=101&fqt=1"
    raw = await fetch_json_with_browser(url, request.state.request_id)
    klines = raw.get("data", {}).get("klines", [])
    result = []
    for line in klines:
        try:
            arr = line.split(",");
            high = float(arr[3]);
            low = float(arr[4])
            result.append({
                "trade_date": arr[0], "stock_code": req.secid, "open": float(arr[1]),
                "close": float(arr[2]), "high": high, "low": low, "volume": int(arr[5]),
                "amount": round(float(arr[6]), 2), "change_percent": float(arr[9]),
                "amplitude": round(((high - low) / low) * 100, 2) if low != 0 else 0
            })
        except:
            continue
    return result


@app.post("/proxy/json")
async def proxy_json(req: GenericJsonRequest, request: Request):
    data = await fetch_json_with_browser(req.url, request.state.request_id)
    if data is None: raise HTTPException(status_code=502, detail="Invalid upstream JSON")
    return data
