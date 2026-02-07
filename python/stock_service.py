#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v9.0 (Production Hybrid Mode)
【严格保证：接口路径不变、出参结构不缺、解析逻辑对齐】
修复核心：通过浏览器“养号”获取 Cookie，HTTPX 模拟 CORS 异步请求规避 ERR_EMPTY_RESPONSE。
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
import httpx
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any, List
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Page, BrowserContext

# =========================================================
# 1. 核心配置
# =========================================================

HEADLESS = True
POOL_SIZE = 4
PAGE_TTL = 200
BROWSER_TTL = 3600
FULL_FIELDS = "f58,f734,f107,f57,f43,f59,f169,f301,f60,f170,f152,f177,f111,f46,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f748,f747"

# =========================================================
# 2. 日志系统
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger("stock")
logger.setLevel(logging.INFO)
formatter = logging.Formatter("[%(asctime)s] %(message)s")
ch = logging.StreamHandler()
ch.setFormatter(formatter)
logger.addHandler(ch)
fh = TimedRotatingFileHandler(os.path.join(LOG_DIR, "stock.log"), when="midnight", interval=1, backupCount=3)
fh.setFormatter(formatter)
logger.addHandler(fh)
logging.getLogger("uvicorn.access").disabled = True


# =========================================================
# 3. 混合动力工作单元 (TabWorker)
# =========================================================

class TabWorker:
    def __init__(self, index: int, context: BrowserContext):
        self.index = index
        self.context = context
        self.page: Optional[Page] = None
        self.cookies_str = ""
        self.req_count = 0
        self.client = httpx.AsyncClient(http2=False, verify=False, timeout=12.0)

    async def init_session(self):
        """Playwright 模拟真实用户访问获取 Cookie (加速优化版)"""
        try:
            if not self.page:
                self.page = await self.context.new_page()

                # --- 新增：资源拦截优化 ---
                # 禁用图片、样式表、媒体文件以加速加载并减少超时风险
                await self.page.route("**/*.{png,jpg,jpeg,gif,svg,css,woff,woff2,mp4,webm}",
                                      lambda route: route.abort())

            logger.info(f"[Worker-{self.index}] Browser refreshing session (Optimized)...")

            # 将 wait_until 改为 "domcontentloaded"，并设置较短的超时
            try:
                await self.page.goto(
                    "https://quote.eastmoney.com/center/gridlist.html",
                    wait_until="domcontentloaded",
                    timeout=15000  # 缩短到15秒，没必要等太久
                )
            except Exception as goto_err:
                logger.warning(f"[Worker-{self.index}] Goto Warning: {goto_err}")
                # 即使超时，只要 DOM 出来了，Cookie 通常也就拿到了，继续往下走

            await asyncio.sleep(1)  # 给页面一点点执行脚本的时间

            cookies = await self.context.cookies()
            self.cookies_str = "; ".join([f"{c['name']}={c['value']}" for c in cookies])
            self.req_count = 0
            logger.info(f"[Worker-{self.index}] Session Renewed. Cookies length: {len(self.cookies_str)}")

        except Exception as e:
            logger.error(f"[Worker-{self.index}] Init Session Failed: {e}")

    async def fetch(self, url: str, rid: str) -> str:
        if not self.cookies_str:
            await self.init_session()

        self.req_count += 1
        if self.req_count > PAGE_TTL:
            await self.init_session()

        headers = POOL.extra_headers.copy()
        headers.update({
            "Cookie": self.cookies_str,
            "Referer": "https://quote.eastmoney.com/",
            "Sec-Fetch-Mode": "cors",  # 模拟网页异步加载
            "Sec-Fetch-Dest": "empty",
            "Sec-Fetch-Site": "same-site"
        })

        for attempt in range(2):
            try:
                resp = await self.client.get(url, headers=headers)
                if resp.status_code == 200:
                    return resp.text
                if resp.status_code in (403, 412):
                    logger.warning(f"[{rid}] Blocked (Status {resp.status_code}), refreshing session...")
                    await self.init_session()
                    continue
                return ""
            except Exception as e:
                if attempt == 0:
                    await self.init_session()
                    continue
                raise e


# =========================================================
# 4. 浏览器池
# =========================================================

class BrowserPool:
    def __init__(self):
        self.pw = None
        self.browser = None
        self.context = None
        self.workers: List[TabWorker] = []
        self.queue = asyncio.Queue()
        self.start_time = 0
        self.extra_headers = {}
        self.headers_file = os.path.join(BASE_DIR, "headers.json")

    async def start(self):
        self.start_time = time.time()
        # 默认使用移动端指纹
        self.extra_headers = {
            "User-Agent": "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"}
        if os.path.exists(self.headers_file):
            with open(self.headers_file, "r") as f:
                self.extra_headers.update(json.load(f))

        self.pw = await async_playwright().start()
        self.browser = await self.pw.chromium.launch(headless=HEADLESS,
                                                     args=["--disable-blink-features=AutomationControlled",
                                                           "--no-sandbox"])

        # 匹配移动端环境
        device = self.pw.devices["Pixel 7"]
        self.context = await self.browser.new_context(**device)

        for i in range(POOL_SIZE):
            w = TabWorker(i, self.context)
            await w.init_session()
            self.workers.append(w)
            await self.queue.put(w)
        logger.info(f"Hybrid Pool Started ({POOL_SIZE} workers).")

    async def stop(self):
        for w in self.workers: await w.client.aclose()
        if self.context: await self.context.close()
        if self.browser: await self.browser.close()
        if self.pw: await self.pw.stop()

    async def dispatch(self, url: str, rid: Optional[str] = None) -> str:
        worker = await self.queue.get()
        try:
            return await worker.fetch(url, rid or "SYS")
        finally:
            await self.queue.put(worker)


# =========================================================
# 5. 数据解析与标准输出 (与旧版本完全对齐)
# =========================================================

def clean_jsonp(text: str):
    if not text: return None
    text = text.strip().lstrip('\ufeff')
    try:
        match = re.search(r'^[^(]*?\((.*)\)[^)]*?$', text, re.DOTALL)
        if match: return json.loads(match.group(1))
        return json.loads(text)
    except:
        return None


def standardize_realtime(data: Dict) -> Dict:
    d = data.get("data", {}) if data.get("data") else data

    def _div100(v): return float(v) / 100 if v not in (None, "-", "") else None

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
        "changePercent": _div100(d.get("f170")),
    }


def standardize_kline(data: Dict) -> List:
    klines = data.get("data", {}).get("klines", [])
    res = []
    for line in klines:
        p = line.split(",")
        if len(p) >= 6:
            res.append(
                {"date": p[0], "open": float(p[1]), "close": float(p[2]), "high": float(p[3]), "low": float(p[4]),
                 "vol": int(p[5])})
    return res


def fix_url_params(url: str, fields: str = "") -> str:
    parsed = urllib.parse.urlparse(url)
    qs = dict(urllib.parse.parse_qsl(parsed.query))
    if fields and "fields" not in qs: qs["fields"] = fields
    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    if "cb" not in qs:
        ts = int(time.time() * 1000)
        qs["cb"] = f"jQuery3510{random.randint(10 ** 15, 10 ** 16)}_{ts}"
        qs["_"] = str(ts)
    return urllib.parse.urlunparse(parsed._replace(query=urllib.parse.urlencode(qs)))


def normalize_secid(code: str) -> str:
    if "." in code: return code
    return f"1.{code}" if code.startswith(("6", "5")) else f"0.{code}"


# =========================================================
# 6. FastAPI 路由实现
# =========================================================

POOL = BrowserPool()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await POOL.start()
    yield
    await POOL.stop()


app = FastAPI(lifespan=lifespan)


class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: Optional[int] = 0


class RangeReq(BaseModel): secid: str; beg: str; end: str


class USReq(BaseModel): secid: str; market: str


class TickReq(BaseModel): secid: str


@app.middleware("http")
async def add_rid(request: Request, call_next):
    request.state.rid = uuid.uuid4().hex[:6]
    return await call_next(request)


# --- 接口全量对齐 ---

@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    url = fix_url_params(f"https://push2.eastmoney.com/api/qt/stock/get?secid={req.secid}", FULL_FIELDS)
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

    # 【核心修复】必须显式添加 fields1 和 fields2 参数
    # fields1: 基础元数据
    # fields2: f51(时间), f52(价格), f53(成交量), f54(变动), f55(性质)
    base_url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&pos=-100&num=100&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55"

    url = fix_url_params(base_url)

    # 发起请求
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw) or {}

    # 解析数据
    details = data.get("data", {}).get("details", [])
    result = []

    # 映射表：1=买入, 2=卖出, 4=中性
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}

    for p in details:
        parts = p.split(",")
        # 确保数据长度足够，防止索引越界报错
        if len(parts) >= 5:
            try:
                result.append({
                    "time": parts[0],  # f51
                    "price": float(parts[1]),  # f52
                    "volume": int(parts[2]),  # f53
                    "side": side_map.get(parts[4], "其他")  # f55 (注意索引是4)
                })
            except (ValueError, IndexError):
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
        f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56")
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
