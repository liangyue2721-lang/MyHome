#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v7.2 (Full API Restoration)
【修复日志】
1. 核心修复：找回丢失的业务接口 (/stock/kline, /stock/ticks, /stock/realtime 等)。
2. 架构保持：继续使用 v7.1 的 HumanBrowser (跨域解锁 + 拟人操作)。
3. 数据标准化：恢复了数据清洗和格式化逻辑。
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

HEADLESS = True  # 生产环境建议 True
MAX_TABS = 4  # 并发上限
BLOCK_TYPES = ["image", "media", "font"]  # 拦截类型 (不拦截 other)

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
# 3. 拟人浏览器核心 (HumanBrowser)
# =========================================================

class HumanBrowser:
    def __init__(self):
        self.pw = None
        self.browser = None
        self.context = None
        self.sem = asyncio.Semaphore(MAX_TABS)

    async def start(self):
        logger.info(">>> Launching Chrome (Human Mode)...")
        self.pw = await async_playwright().start()

        self.browser = await self.pw.chromium.launch(
            headless=HEADLESS,
            channel="chrome",
            args=[
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-infobars",
                "--window-size=1280,800",
                "--disable-background-timer-throttling",
                # 允许跨域
                "--disable-web-security",
                "--disable-features=IsolateOrigins,site-per-process",
            ]
        )

        self.context = await self.browser.new_context(
            viewport={"width": 1280, "height": 800},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            locale="zh-CN",
            timezone_id="Asia/Shanghai",
            bypass_csp=True
        )

        await self.context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        # 预热
        try:
            page = await self.context.new_page()
            await page.goto("https://quote.eastmoney.com/center/gridlist.html", wait_until="domcontentloaded")
            await asyncio.sleep(1)
            await page.close()
        except:
            pass
        logger.info(">>> Browser Ready.")

    async def stop(self):
        if self.context: await self.context.close()
        if self.browser: await self.browser.close()
        if self.pw: await self.pw.stop()

    async def _simulate_human_behavior(self, page: Page):
        try:
            await page.mouse.move(random.randint(100, 700), random.randint(100, 500), steps=5)
            await page.evaluate(f"window.scrollTo({{top: {random.randint(100, 400)}, behavior: 'smooth'}})")
            await asyncio.sleep(random.uniform(0.1, 0.3))
        except:
            pass

    async def fetch(self, target_api_url: str, rid: str):
        async with self.sem:
            page = await self.context.new_page()
            try:
                # 路由拦截
                await page.route("**/*", lambda route: route.abort()
                if route.request.resource_type in BLOCK_TYPES
                else route.continue_())

                # 随机宿主
                host_url = random.choice([
                    "https://quote.eastmoney.com/sz000001.html",
                    "https://quote.eastmoney.com/center/gridlist.html"
                ])

                await page.goto(host_url, wait_until="domcontentloaded", timeout=12000)
                await self._simulate_human_behavior(page)

                # 注入 Fetch (带 Header 伪装)
                fetch_js = """
                async (url) => {
                    const controller = new AbortController();
                    const id = setTimeout(() => controller.abort(), 10000);
                    try {
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

                result = await page.evaluate(fetch_js, target_api_url)

                if result.get("err"):
                    raise Exception(f"JS Fetch Fail: {result.get('err')}")

                return result.get("txt", "")

            except Exception as e:
                raise e
            finally:
                if not page.is_closed(): await page.close()


# =========================================================
# 4. 数据标准化 (找回的逻辑)
# =========================================================

def safe_get(dct, key): return dct.get(key)


def standardize_realtime_data(data: Dict[str, Any]) -> Dict[str, Any]:
    def _div100(val):
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
        "changePercent": _div100(safe_get(data, "f170")),
    }


def standardize_tick(data: Dict[str, Any], stock_code: str) -> list:
    result = []
    side_map = {"1": "买入", "2": "卖出", "4": "中性"}
    for item in data.get("details", []):
        p = item.split(",")
        if len(p) < 5: continue
        try:
            result.append({
                "time": p[0], "price": float(p[1]), "volume": int(p[2]), "side": side_map.get(p[4], "其他")
            })
        except:
            continue
    return result


def standardize_kline(data: Dict[str, Any], secid: str) -> list:
    result = []
    for line in data.get("klines", []):
        p = line.split(",")
        if len(p) < 5: continue
        try:
            result.append({
                "date": p[0], "open": float(p[1]), "close": float(p[2]), "high": float(p[3]), "low": float(p[4]),
                "vol": int(p[5])
            })
        except:
            continue
    return result


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


# =========================================================
# 5. FastAPI 服务层
# =========================================================

HUMAN_BROWSER = HumanBrowser()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await HUMAN_BROWSER.start()
    yield
    await HUMAN_BROWSER.stop()


app = FastAPI(lifespan=lifespan)


# 工具函数
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

    # 自动补全 fields
    if "/api/qt/stock/get" in parsed.path:
        if "fields" not in qs or len(qs["fields"]) < 50:
            qs["fields"] = FULL_FIELDS

    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    cb, ts = generate_jquery_cb()
    qs["cb"] = cb
    qs["_"] = ts
    return urllib.parse.urlunparse(parsed._replace(query=urllib.parse.urlencode(qs)))


# Models
class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: int


class KlineRangeReq(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineReq(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class TickReq(BaseModel): secid: str


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
def health(): return {"status": "ok"}


# --- 核心接口 (全部已找回) ---

@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    url = f"https://push2.eastmoney.com/api/qt/stock/get?secid={req.secid}"
    url = fix_url_params(url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    data = clean_jsonp(raw)
    if not data or not data.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    data = clean_jsonp(raw)
    if not data or not data.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    data = clean_jsonp(raw)
    if not data or not data.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(data["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100"
    url = fix_url_params(url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    data = clean_jsonp(raw)
    return standardize_tick(data.get("data", {}), secid.split(".")[-1]) if data else []


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    url = fix_url_params(url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
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
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    data = clean_jsonp(raw)
    return standardize_kline(data.get("data", {}), secid) if data else []


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56"
    url = fix_url_params(url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    data = clean_jsonp(raw)
    return standardize_kline(data.get("data", {}), secid) if data else []


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    url = fix_url_params(req.url)
    raw = await HUMAN_BROWSER.fetch(url, request.state.rid)
    return clean_jsonp(raw)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, limit_concurrency=50, access_log=False)
