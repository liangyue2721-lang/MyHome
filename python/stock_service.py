#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v7.3 (Tab Pooling & Auto-Recovery)
【核心修复】
1. 标签页池化 (Tab Pooling):
   - 启动时预创建 4 个 Tab，长期驻留，不再每次请求都开关 Tab。
   - 彻底解决高并发导致 Chrome 崩溃的问题。
2. 自动熔断重启 (Crash Recovery):
   - 检测到浏览器崩溃或卡死，自动销毁进程并重建。
3. 性能飞跃:
   - 移除 page.goto 开销，单次请求耗时将降至 200ms - 500ms。
"""

import os
import json
import logging
import asyncio
import time
import uuid
import random
import urllib.parse
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any, List
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Page, BrowserContext, Browser

# =========================================================
# 1. 核心配置
# =========================================================

HEADLESS = True  # 生产模式 True
POOL_SIZE = 4  # 标签页数量 (建议 4-8，视 CPU 核心数而定)
PAGE_TTL = 200  # 每个标签页处理多少次请求后刷新一次 (防内存泄漏)
BROWSER_TTL = 3600  # 浏览器每 1 小时重启一次

# 拦截资源类型
BLOCK_TYPES = ["image", "media", "font"]

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
# 3. 标签页工作单元 (Worker Page)
# =========================================================

class TabWorker:
    def __init__(self, index: int, context: BrowserContext):
        self.index = index
        self.context = context
        self.page: Optional[Page] = None
        self.req_count = 0
        self.lock = asyncio.Lock()

    async def init_page(self):
        """初始化或重置页面"""
        try:
            if self.page:
                try:
                    await self.page.close()
                except:
                    pass

            logger.info(f"[Worker-{self.index}] Opening Tab...")
            self.page = await self.context.new_page()

            # 资源拦截
            await self.page.route("**/*", lambda route: route.abort()
            if route.request.resource_type in BLOCK_TYPES
            else route.continue_())

            # 导航到宿主页面 (常驻)
            host = random.choice([
                "https://quote.eastmoney.com/center/gridlist.html",
                "https://quote.eastmoney.com/sz000001.html"
            ])
            await self.page.goto(host, wait_until="domcontentloaded", timeout=20000)
            self.req_count = 0
            logger.info(f"[Worker-{self.index}] Ready.")
        except Exception as e:
            logger.error(f"[Worker-{self.index}] Init Failed: {e}")
            self.page = None

    async def fetch(self, url: str) -> str:
        """在该 Tab 上执行 fetch"""
        if not self.page: raise Exception("Page not ready")

        # 注入 Fetch 代码
        fetch_js = """
        async (url) => {
            try {
                const controller = new AbortController();
                const id = setTimeout(() => controller.abort(), 8000);
                const res = await fetch(url, { 
                    signal: controller.signal,
                    method: 'GET',
                    headers: {'Accept': '*/*'}
                });
                clearTimeout(id);
                if (!res.ok) return { err: res.status };
                return { txt: await res.text() };
            } catch (e) {
                return { err: e.toString() };
            }
        }
        """

        # 页面刷新机制
        self.req_count += 1
        if self.req_count > PAGE_TTL:
            logger.info(f"[Worker-{self.index}] TTL reached, refreshing...")
            await self.init_page()

        result = await self.page.evaluate(fetch_js, url)

        if result.get("err"):
            raise Exception(f"JS Error: {result.get('err')}")
        return result.get("txt", "")


# =========================================================
# 4. 浏览器管理器 (Pool Manager)
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
        self.browser = await self.pw.chromium.launch(
            headless=HEADLESS,
            channel="chrome",
            args=[
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-infobars",
                "--disable-background-timer-throttling",
                "--disable-web-security",
                "--disable-features=IsolateOrigins,site-per-process",
            ]
        )

        self.context = await self.browser.new_context(
            viewport={"width": 1280, "height": 800},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            bypass_csp=True
        )
        await self.context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        # 创建 Worker 池
        for i in range(POOL_SIZE):
            w = TabWorker(i, self.context)
            await w.init_page()
            self.workers.append(w)
            await self.queue.put(w)

        logger.info(f">>> Pool Started with {POOL_SIZE} workers.")

    async def restart(self):
        """崩溃恢复：重启整个浏览器"""
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
        # 清空队列
        while not self.queue.empty(): self.queue.get_nowait()

    async def dispatch(self, url: str) -> str:
        """分发请求"""
        # 检查浏览器是否存活
        if not self.browser or not self.browser.is_connected():
            await self.restart()

        # 检查是否需要定期重启
        if time.time() - self.start_time > BROWSER_TTL:
            await self.restart()

        worker = await self.queue.get()  # 获取空闲 Worker
        try:
            return await worker.fetch(url)
        except Exception as e:
            # 如果是页面崩溃错误，尝试修复该 Worker
            if "Target closed" in str(e) or "closed" in str(e):
                logger.error(f"Worker-{worker.index} Crashed! Re-init...")
                await worker.init_page()
                raise e  # 本次请求失败，抛出让客户端重试
            else:
                raise e
        finally:
            # 归还 Worker
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


# 工具与模型
def clean_jsonp(text: str):
    if not text: return None
    text = text.strip().lstrip('\ufeff')
    try:
        return json.loads(text)
    except:
        pass
    try:
        s, e = text.find('('), text.rfind(')')
        if s != -1 and e > s: return json.loads(text[s + 1:e])
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
        if "fields" not in qs or len(qs["fields"]) < 50: qs["fields"] = FULL_FIELDS
    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    cb, ts = generate_jquery_cb()
    qs["cb"] = cb
    qs["_"] = ts
    return urllib.parse.urlunparse(parsed._replace(query=urllib.parse.urlencode(qs)))


class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: int


class KlineRangeReq(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineReq(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class TickReq(BaseModel): secid: str


# 数据标准化函数
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
    try:
        response = await call_next(request)
        cost = (time.time() - start) * 1000
        logger.info(f"[{rid}] {request.method} {request.url.path} -> {response.status_code} ({cost:.0f}ms)")
        return response
    except Exception as e:
        logger.error(f"[{rid}] Err: {e}")
        raise


@app.get("/health")
def health(): return {"status": "ok", "workers": POOL.queue.qsize()}


# 接口实现
@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    url = f"https://push2.eastmoney.com/api/qt/stock/get?secid={req.secid}"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    if not data or not data.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    if not data or not data.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    if not data or not data.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    return standardize_tick(data.get("data", {}), secid.split(".")[-1]) if data else []


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    result = standardize_kline(data.get("data", {}), secid) if data else []
    if req.ndays and len(result) > req.ndays: result = result[-req.ndays:]
    return result


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeReq, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20991231"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    return standardize_kline(data.get("data", {}), secid) if data else []


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56"
    url = fix_url_params(url)
    raw = await POOL.dispatch(url)
    data = clean_jsonp(raw)
    return standardize_kline(data.get("data", {}), secid) if data else []


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await POOL.dispatch(url)
    return clean_jsonp(raw)


if __name__ == "__main__":
    import uvicorn

    # 增加并发数，因为现在是队列机制，不会爆浏览器了
    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, limit_concurrency=200, access_log=False)
