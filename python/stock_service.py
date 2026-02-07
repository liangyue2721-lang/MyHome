#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v7.8 (Navigation Mode & Mobile Emulation)
【核心修复】
1. Socket Hang Up 修复: 启动参数增加 --disable-http2，强制使用 HTTP/1.1。
2. 404/Empty Data 修复: 弃用 fetch/request，改用 page.goto()。
   - 这会自然生成 Sec-Fetch-Mode: navigate。
   - 这会完美处理 Cookies 和 Session。
3. 环境模拟: 使用 Pixel 7 模拟器，匹配抓包中的 Android UA。
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

# 完整字段列表
FULL_FIELDS = "f58,f734,f107,f57,f43,f59,f169,f301,f60,f170,f152,f177,f111,f46,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f748,f747"

# =========================================================
# 2. 日志与工具
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
# 3. 标签页工作单元 (Worker)
# =========================================================


class TabWorker:
    def __init__(self, index: int, context: BrowserContext):
        self.index = index
        self.context = context
        self.page: Optional[Page] = None
        self.req_count = 0

    async def init_page(self):
        try:
            if self.page:
                try:
                    await self.page.close()
                except:
                    pass

            logger.info(f"[Worker-{self.index}] Opening Tab...")
            self.page = await self.context.new_page()
            self.req_count = 0
            logger.info(f"[Worker-{self.index}] Ready.")
        except Exception as e:
            logger.error(f"[Worker-{self.index}] Init Failed: {e}")
            self.page = None

    async def fetch(self, url: str, rid: str) -> str:
        """
        统一使用 page.goto 导航模式
        绝对不要再使用 page.evaluate(fetch)
        """
        if not self.page:
            raise Exception("Page not ready")

        self.req_count += 1
        if self.req_count > PAGE_TTL:
            logger.info(f"[Worker-{self.index}] TTL reached, refreshing...")
            await self.init_page()

        for attempt in range(2):
            try:
                # 显式重置 headers，确保每次导航都带上完整指纹
                if hasattr(POOL, 'extra_headers'):
                    await self.page.set_extra_http_headers(POOL.extra_headers)

                response = await self.page.goto(
                    url,
                    wait_until="domcontentloaded",
                    timeout=10000
                )

                if response and response.status != 200:
                    logger.error(f"[{rid}] Status {response.status} | URL: {url}")

                # 直接从 body 取 JSON / JSONP
                content = await self.page.inner_text("body")

                preview = content[:120].replace("\n", "").replace("\r", "")
                logger.info(f"[{rid}] EM_RAW: {preview}")

                if not content:
                    logger.warning(f"[{rid}] Empty Data URL: {url}")

                return content

            except Exception as e:
                if attempt == 0:
                    logger.warning(f"[{rid}] Goto Err: {e}. Retrying...")
                    await asyncio.sleep(0.5)
                    continue
                logger.error(f"[{rid}] Goto Err: {e}")
                raise e


# 4. 浏览器管理器
# =========================================================

class BrowserPool:
    def __init__(self):
        self.pw = None
        self.browser = None
        self.context = None
        self.workers: List[TabWorker] = []
        self.queue = asyncio.Queue()
        self.running = False
        self.start_time = 0

    async def start(self):
        self.running = True
        self.start_time = time.time()
        logger.info(">>> Pool Starting...")

        self.pw = await async_playwright().start()

        # 使用 Mobile Emulation 预设
        device = self.pw.devices["Pixel 7"]

        self.browser = await self.pw.chromium.launch(
            headless=HEADLESS,
            channel="chrome",
            args=[
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-infobars",
                "--ignore-certificate-errors",
                "--disable-http2",
            ]
        )

        # Override user_agent and is_mobile to match the provided successful request
        # User-Agent: Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36 CrKey/1.54.248666
        # sec-ch-ua-mobile: ?0
        context_options = device.copy()
        context_options["user_agent"] = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36 CrKey/1.54.248666"
        context_options["is_mobile"] = False  # Matches sec-ch-ua-mobile: ?0

        # Inject real fingerprint headers
        self.extra_headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "Accept-Language": "zh-CN,zh;q=0.9",
            "Cache-Control": "no-cache",
            "Pragma": "no-cache",
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "none",
            "Sec-Fetch-User": "?1",
            "Upgrade-Insecure-Requests": "1",
            "sec-ch-ua": '"Not(A:Brand";v="8", "Chromium";v="144", "Google Chrome";v="144"',
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": '"Android"',
        }

        self.context = await self.browser.new_context(
            **context_options,
            extra_http_headers=self.extra_headers,
            accept_downloads=False,
            bypass_csp=True,
            ignore_https_errors=True
        )

        # 注入防检测脚本
        await self.context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        for i in range(POOL_SIZE):
            w = TabWorker(i, self.context)
            await w.init_page()
            self.workers.append(w)
            await self.queue.put(w)

        logger.info(f">>> Pool Started with {POOL_SIZE} workers.")

    async def restart(self):
        logger.warning(">>> !!! RESTARTING BROWSER POOL !!!")
        try:
            await self.stop()
        except:
            pass
        await asyncio.sleep(2)
        await self.start()

    async def stop(self):
        self.running = False
        if self.context: await self.context.close()
        if self.browser: await self.browser.close()
        if self.pw: await self.pw.stop()
        self.workers = []
        while not self.queue.empty(): self.queue.get_nowait()

    async def dispatch(self, url: str, rid: Optional[str] = None) -> str:
        if not self.browser or not self.browser.is_connected():
            await self.restart()

        if time.time() - self.start_time > BROWSER_TTL:
            await self.restart()

        worker = await self.queue.get()
        try:
            res = await worker.fetch(url, rid if rid else "SYS")
            return res
        except Exception as e:
            # 导航错误通常不需要重置整个 Worker，除非是 crash
            if "Target closed" in str(e) or "closed" in str(e):
                logger.error(f"Worker-{worker.index} Crashed! Re-init...")
                await worker.init_page()
                raise e
            else:
                raise e
        finally:
            await self.queue.put(worker)

        # =========================================================


# 5. FastAPI 服务层
# =========================================================

POOL = BrowserPool()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await POOL.start()
    yield
    await POOL.stop()


app = FastAPI(lifespan=lifespan)


# 工具函数
def clean_jsonp(text: str):
    if not text: return None
    text = text.strip().lstrip('\ufeff')

    # 有些情况下 body 会包含 HTML 标签，先简单清洗
    if "<pre" in text:
        # 如果 content() 返回了 html，尝试提取 pre 内容
        try:
            match = re.search(r'<pre.*?>(.*?)</pre>', text, re.DOTALL)
            if match:
                text = match.group(1)
        except:
            pass

    try:
        return json.loads(text)
    except:
        pass
    try:
        match = re.search(r'^[^(]*?\((.*)\)[^)]*?$', text, re.DOTALL)
        if match:
            return json.loads(match.group(1))
    except:
        pass
    return None


def generate_jquery_cb():
    ts = int(time.time() * 1000)
    random_part = "".join([str(random.randint(0, 9)) for _ in range(20)])
    return f"jQuery3510{random_part}_{ts}", str(ts)


def fix_url_params(url: str) -> str:
    if "eastmoney.com" not in url: return url
    parsed = urllib.parse.urlparse(url)
    qs = dict(urllib.parse.parse_qsl(parsed.query))

    if "/api/qt/stock/get" in parsed.path:
        if "fields" not in qs or len(qs["fields"]) < 20: qs["fields"] = FULL_FIELDS

        # 补全关键参数
    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    if "dect" not in qs: qs["dect"] = "1"
    if "wbp2u" not in qs: qs["wbp2u"] = "|0|0|0|web"

    if "cb" not in qs:
        cb, ts = generate_jquery_cb()
        qs["cb"] = cb
        qs["_"] = ts
    elif "_" not in qs:
        qs["_"] = str(int(time.time() * 1000))

    return urllib.parse.urlunparse(parsed._replace(query=urllib.parse.urlencode(qs)))


class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: int


class KlineRangeReq(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineReq(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class TickReq(BaseModel): secid: str


def safe_get(dct, key): return dct.get(key)


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    def _div100(val):
        try:
            return float(val) / 100
        except:
            return None

    def _num(val):
        return None if val in (None, "", "-") else val

    return {
        "stockCode": safe_get(data, "f57"), "companyName": safe_get(data, "f58"),
        "price": _div100(safe_get(data, "f43")), "prevClose": _div100(safe_get(data, "f60")),
        "openPrice": _div100(safe_get(data, "f46")), "highPrice": _div100(safe_get(data, "f44")),
        "lowPrice": _div100(safe_get(data, "f45")), "volume": _num(safe_get(data, "f47")),
        "turnover": _num(safe_get(data, "f48")), "changePercent": _div100(safe_get(data, "f170")),
    }


def standardize_tick(data: Dict[str, Any], stock_code: str) -> list:
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}
    for item in data.get("details", []):
        p = item.split(",")
        if len(p) < 5: continue
        try:
            result.append({"time": p[0], "price": float(p[1]), "volume": int(p[2]), "side": side_map.get(p[4], "其他")})
        except:
            continue
    return result


def standardize_kline(data: Dict[str, Any], secid: str) -> list:
    result = []
    for line in data.get("klines", []):
        p = line.split(",")
        if len(p) < 5: continue
        try:
            result.append(
                {"date": p[0], "open": float(p[1]), "close": float(p[2]), "high": float(p[3]), "low": float(p[4]),
                 "vol": int(p[5])})
        except:
            continue
    return result


@app.middleware("http")
async def log_requests(request: Request, call_next):
    rid = uuid.uuid4().hex[:6]
    request.state.rid = rid
    start = time.time()

    body = b""
    if request.method in ("POST", "PUT"):
        try:
            body = await request.body()
            body_text = body.decode("utf-8")
        except:
            body_text = str(body)

        logger.info(f"[{rid}] IN  {request.method} {request.url.path} BODY={body_text}")

        async def receive():
            return {"type": "http.request", "body": body}

        request._receive = receive

    try:
        response = await call_next(request)
        cost = (time.time() - start) * 1000
        logger.info(f"[{rid}] OUT {response.status_code} ({cost:.0f}ms)")
        return response
    except Exception as e:
        logger.error(f"[{rid}] ERR {e}")
        raise


@app.get("/health")
def health(): return {"status": "ok", "workers": POOL.queue.qsize()}


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    # 基础 URL
    url = f"https://push2.eastmoney.com/api/qt/stock/get?secid={req.secid}"
    # 补全参数 (wbp2u, dect 等)
    url = fix_url_params(url)

    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)

    if not data or not data.get("data"):
        # 404 时打印更多信息
        raise HTTPException(404, f"Data Empty. RAW: {raw[:200] if raw else 'None'}")

    return standardize_realtime_data(data["data"])


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)
    if not data or not data.get("data"):
        raise HTTPException(404, f"Data Empty.")
    return standardize_realtime_data(data["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)
    if not data or not data.get("data"):
        raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)
    return standardize_tick(data.get("data", {}), secid.split(".")[-1]) if data else []


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)
    result = standardize_kline(data.get("data", {}), secid) if data else []
    if req.ndays and len(result) > req.ndays:
        result = result[-req.ndays:]
    return result


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeReq, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20991231"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)
    return standardize_kline(data.get("data", {}), secid) if data else []


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url, request.state.rid)
    data = clean_jsonp(raw)
    return standardize_kline(data.get("data", {}), secid) if data else []


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url, request.state.rid)
    return clean_jsonp(raw)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, limit_concurrency=200, access_log=False)
