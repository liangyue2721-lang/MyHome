#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Stock Data Service v4.3 (JS Injection & Hybrid Mode)
【核心修复】
1. 混合请求策略：
   - 敏感接口 (/stock/get): 改用 page.evaluate(fetch) 在页面内部发起请求。这能生成正确的 Sec-Fetch-Dest: empty 头，绕过"禁止直接导航"的风控。
   - 宽容接口 (/kline, /details): 继续使用 page.goto，因为它们数据量大且服务端允许直接访问。
2. 真实指纹：生成 100% 仿真的 jQuery 回调参数 (jQuery3510...+随机数)。
3. 跨域解锁：确保 --disable-web-security 生效，允许页面内 fetch 任意接口。
"""

import os
import json
import logging
import asyncio
import re
import time
import uuid
import random
import subprocess
import urllib.parse
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Page

# =========================================================
# 0. 核心配置
# =========================================================

HEADLESS = False  # 保持可见，方便调试
POOL_SIZE = 2
USER_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "chrome_user_data")

# 完整字段列表
FULL_FIELDS = "f58,f734,f107,f57,f43,f59,f169,f301,f60,f170,f152,f177,f111,f46,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f748,f747"

# =========================================================
# 1. 日志系统
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name: str, filename: str):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.handlers.clear()
    file_handler = TimedRotatingFileHandler(os.path.join(LOG_DIR, filename), when="midnight", interval=1, backupCount=7,
                                            encoding="utf-8")
    file_handler.setFormatter(logging.Formatter("[%(asctime)s] %(message)s"))
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter("[%(asctime)s] %(message)s"))
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    return logger


logger = _build_logger("stock", "stock.log")
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# 2. 浏览器与进程管理
# =========================================================

PW_OBJ = None
CONTEXTS = []
PAGE_QUEUE = asyncio.Queue()


def kill_chrome_processes():
    try:
        cmd = "taskkill /F /IM chrome.exe /T" if os.name == 'nt' else "pkill -9 chrome"
        subprocess.run(cmd, shell=True, stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
    except:
        pass


async def _init_worker(index: int) -> Page:
    try:
        worker_user_dir = os.path.join(USER_DATA_DIR, f"worker_{index}")
        os.makedirs(worker_user_dir, exist_ok=True)

        lock = os.path.join(worker_user_dir, "SingletonLock")
        if os.path.exists(lock):
            try:
                os.remove(lock)
            except:
                pass

        context = await PW_OBJ.chromium.launch_persistent_context(
            user_data_dir=worker_user_dir,
            channel="chrome",
            headless=HEADLESS,
            ignore_default_args=["--enable-automation"],
            args=[
                "--disable-web-security",  # 允许页面内 fetch 跨域
                "--disable-features=IsolateOrigins,site-per-process",
                "--no-sandbox",
                "--disable-infobars",
                "--disable-blink-features=AutomationControlled",
                "--disable-gpu",
                "--no-first-run"
            ],
            viewport={"width": 1024, "height": 768}
        )

        await context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
        # 移除全局 Referer，由 JS 注入时自动处理
        # await context.set_extra_http_headers(...)

        pages = context.pages
        page = pages[0] if pages else await context.new_page()

        logger.info(f"Worker {index}: Warming up...")
        try:
            # 必须停留在东财域名下，才能发合法的 fetch
            await page.goto("https://quote.eastmoney.com/center/", wait_until="domcontentloaded", timeout=20000)
            await asyncio.sleep(2)
        except:
            pass

        logger.info(f">>> Worker {index} Ready.")
        CONTEXTS.append(context)
        return page
    except Exception as e:
        logger.error(f"Worker {index} Init Error: {e}")
        return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global PW_OBJ
    kill_chrome_processes()

    logger.info(">>> System Starting...")
    PW_OBJ = await async_playwright().start()

    for i in range(POOL_SIZE):
        page = await _init_worker(i + 1)
        if page:
            await PAGE_QUEUE.put(page)
        else:
            await asyncio.sleep(2)
            retry = await _init_worker(i + 1)
            if retry: await PAGE_QUEUE.put(retry)

    logger.info(f">>> System Ready. {PAGE_QUEUE.qsize()} Workers.")
    yield

    for ctx in CONTEXTS:
        try:
            await ctx.close()
        except:
            pass
    if PW_OBJ: await PW_OBJ.stop()
    kill_chrome_processes()


app = FastAPI(lifespan=lifespan)


# =========================================================
# 3. 核心 Fetch 逻辑 (混合模式)
# =========================================================

def generate_jquery_cb():
    """生成仿真的 jQuery 回调函数名"""
    ts = int(time.time() * 1000)
    # 模拟 jQuery3.5.1 的随机数部分 (通常是 20位数字)
    random_part = "".join([str(random.randint(0, 9)) for _ in range(20)])
    return f"jQuery3510{random_part}_{ts}", str(ts)


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


def fix_url_params(url: str) -> str:
    """补全参数并生成真实回调"""
    if "eastmoney.com" not in url: return url

    parsed = urllib.parse.urlparse(url)
    qs = dict(urllib.parse.parse_qsl(parsed.query))

    # 强制全量字段
    if "/api/qt/stock/get" in parsed.path:
        if "fields" not in qs or len(qs["fields"]) < 50:
            qs["fields"] = FULL_FIELDS

    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    if "invt" not in qs: qs["invt"] = "2"
    if "fltt" not in qs: qs["fltt"] = "1"

    # 重新生成 callback，覆盖旧的
    cb, ts = generate_jquery_cb()
    qs["cb"] = cb
    qs["_"] = ts

    new_query = urllib.parse.urlencode(qs)
    return urllib.parse.urlunparse(parsed._replace(query=new_query))


async def fetch_data(url: str, rid: str, method: str = "auto", timeout: float = 15.0):
    url = fix_url_params(url)
    method = "goto"
    # 自动判断请求方式
    if method == "auto":
        # 如果是 get 接口 (Snapshot/Realtime)，必须用 JS Fetch 绕过导航检测
        #if "/api/qt/stock/get" in url:
            #method = "js_fetch"
        #else:
            # 其他接口 (Kline/Ticks) 用 Goto 更快更稳
            method = "goto"

    page = None
    try:
        page = await asyncio.wait_for(PAGE_QUEUE.get(), timeout=5.0)
        if page.is_closed(): raise Exception("Page Closed")

        # 确保页面在东财域名下 (为了 fetch 不跨域/少跨域)
        if "eastmoney.com" not in page.url:
            try:
                await page.goto("https://quote.eastmoney.com/center/", wait_until="domcontentloaded", timeout=5000)
            except:
                pass

        text_content = ""

        if method == "js_fetch":
            logger.info(f"[{rid}] Mode: JS Fetch -> {url[:60]}...")
            # 在页面上下文执行 fetch
            result = await page.evaluate(f"""async () => {{ 
                try {{ 
                    const controller = new AbortController(); 
                    const id = setTimeout(() => controller.abort(), {int(timeout * 1000)}); 
                    const res = await fetch("{url}", {{ signal: controller.signal }}); 
                    clearTimeout(id); 
                    if (!res.ok) return {{ err: res.status }}; 
                    return {{ txt: await res.text() }}; 
                }} catch (e) {{ return {{ err: e.toString() }}; }} 
            }}""")

            if result.get("err"):
                raise Exception(f"JS Error: {result.get('err')}")
            text_content = result.get("txt", "")

        else:  # method == "goto"
            logger.info(f"[{rid}] Mode: Direct Goto -> {url[:60]}...")
            response = await page.goto(url, wait_until="commit", timeout=int(timeout * 1000))
            if not response or response.status != 200:
                raise Exception(f"HTTP {response.status if response else 'Null'}")
            text_content = await response.text()

        data = clean_jsonp(text_content)
        if not data:
            if len(text_content) < 50:
                asyncio.create_task(_refresh_worker(page))
                raise Exception("Empty Data")

        return data

    except Exception as e:
        logger.error(f"[{rid}] Fail: {e}")
        if page and ("Target closed" in str(e) or "Session closed" in str(e)):
            try:
                await page.close();
                page = None
            except:
                pass
        raise HTTPException(status_code=502, detail=f"Fetch Failed: {str(e)}")
    finally:
        if page and not page.is_closed(): await PAGE_QUEUE.put(page)


async def _refresh_worker(page: Page):
    try:
        await page.goto("https://quote.eastmoney.com/center/", wait_until="domcontentloaded", timeout=10000)
    except:
        pass

    # =========================================================


# 4. 数据处理与接口
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


# Models
class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: int


class KlineRangeReq(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineReq(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class TickReq(BaseModel): secid: str


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


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
def health(): return {"status": "ok", "workers": PAGE_QUEUE.qsize()}


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    url = f"https://push2.eastmoney.com/api/qt/stock/get?secid={req.secid}"
    raw = await fetch_data(url, request.state.rid, method="js_fetch")
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    raw = await fetch_data(req.url, request.state.rid, method="js_fetch")
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    raw = await fetch_data(req.url, request.state.rid, method="js_fetch")
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100"
    raw = await fetch_data(url, request.state.rid, method="goto")
    return standardize_tick(raw.get("data", {}), secid.split(".")[-1]) if raw else []


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    raw = await fetch_data(url, request.state.rid, method="goto")
    data = standardize_kline(raw.get("data", {}), secid) if raw else []
    if req.ndays and len(data) > req.ndays:
        data = data[-req.ndays:]
    return data


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeReq, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20991231"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    raw = await fetch_data(url, request.state.rid, method="goto")
    return standardize_kline(raw.get("data", {}), secid) if raw else []


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56"
    raw = await fetch_data(url, request.state.rid, method="goto")
    return standardize_kline(raw.get("data", {}), secid) if raw else []


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    return await fetch_data(req.url, request.state.rid)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, reload=False, limit_concurrency=100, access_log=False)
# !/usr/bin/env python3
# -*- coding: utf-8 -*-

""" 
Stock Data Service v4.3 (JS Injection & Hybrid Mode) 
【核心修复】 
1. 混合请求策略： 
   - 敏感接口 (/stock/get): 改用 page.evaluate(fetch) 在页面内部发起请求。这能生成正确的 Sec-Fetch-Dest: empty 头，绕过"禁止直接导航"的风控。 
   - 宽容接口 (/kline, /details): 继续使用 page.goto，因为它们数据量大且服务端允许直接访问。 
2. 真实指纹：生成 100% 仿真的 jQuery 回调参数 (jQuery3510...+随机数)。 
3. 跨域解锁：确保 --disable-web-security 生效，允许页面内 fetch 任意接口。 
"""

import os
import json
import logging
import asyncio
import re
import time
import uuid
import random
import subprocess
import urllib.parse
from contextlib import asynccontextmanager
from typing import Optional, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Page

# =========================================================
# 0. 核心配置
# =========================================================

HEADLESS = False  # 保持可见，方便调试
POOL_SIZE = 2
USER_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "chrome_user_data")

# 完整字段列表
FULL_FIELDS = "f58,f734,f107,f57,f43,f59,f169,f301,f60,f170,f152,f177,f111,f46,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f748,f747"

# =========================================================
# 1. 日志系统
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)


def _build_logger(name: str, filename: str):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.handlers.clear()
    file_handler = TimedRotatingFileHandler(os.path.join(LOG_DIR, filename), when="midnight", interval=1, backupCount=7,
                                            encoding="utf-8")
    file_handler.setFormatter(logging.Formatter("[%(asctime)s] %(message)s"))
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter("[%(asctime)s] %(message)s"))
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    return logger


logger = _build_logger("stock", "stock.log")
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# 2. 浏览器与进程管理
# =========================================================

PW_OBJ = None
CONTEXTS = []
PAGE_QUEUE = asyncio.Queue()


def kill_chrome_processes():
    try:
        cmd = "taskkill /F /IM chrome.exe /T" if os.name == 'nt' else "pkill -9 chrome"
        subprocess.run(cmd, shell=True, stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
    except:
        pass


async def _init_worker(index: int) -> Page:
    try:
        worker_user_dir = os.path.join(USER_DATA_DIR, f"worker_{index}")
        os.makedirs(worker_user_dir, exist_ok=True)

        lock = os.path.join(worker_user_dir, "SingletonLock")
        if os.path.exists(lock):
            try:
                os.remove(lock)
            except:
                pass

        context = await PW_OBJ.chromium.launch_persistent_context(
            user_data_dir=worker_user_dir,
            channel="chrome",
            headless=HEADLESS,
            ignore_default_args=["--enable-automation"],
            args=[
                "--disable-web-security",  # 允许页面内 fetch 跨域
                "--disable-features=IsolateOrigins,site-per-process",
                "--no-sandbox",
                "--disable-infobars",
                "--disable-blink-features=AutomationControlled",
                "--disable-gpu",
                "--no-first-run"
            ],
            viewport={"width": 1024, "height": 768}
        )

        await context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
        # 移除全局 Referer，由 JS 注入时自动处理
        # await context.set_extra_http_headers(...)

        pages = context.pages
        page = pages[0] if pages else await context.new_page()

        logger.info(f"Worker {index}: Warming up...")
        try:
            # 必须停留在东财域名下，才能发合法的 fetch
            await page.goto("https://quote.eastmoney.com/center/", wait_until="domcontentloaded", timeout=20000)
            await asyncio.sleep(2)
        except:
            pass

        logger.info(f">>> Worker {index} Ready.")
        CONTEXTS.append(context)
        return page
    except Exception as e:
        logger.error(f"Worker {index} Init Error: {e}")
        return None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global PW_OBJ
    kill_chrome_processes()

    logger.info(">>> System Starting...")
    PW_OBJ = await async_playwright().start()

    for i in range(POOL_SIZE):
        page = await _init_worker(i + 1)
        if page:
            await PAGE_QUEUE.put(page)
        else:
            await asyncio.sleep(2)
            retry = await _init_worker(i + 1)
            if retry: await PAGE_QUEUE.put(retry)

    logger.info(f">>> System Ready. {PAGE_QUEUE.qsize()} Workers.")
    yield

    for ctx in CONTEXTS:
        try:
            await ctx.close()
        except:
            pass
    if PW_OBJ: await PW_OBJ.stop()
    kill_chrome_processes()


app = FastAPI(lifespan=lifespan)


# =========================================================
# 3. 核心 Fetch 逻辑 (混合模式)
# =========================================================

def generate_jquery_cb():
    """生成仿真的 jQuery 回调函数名"""
    ts = int(time.time() * 1000)
    # 模拟 jQuery3.5.1 的随机数部分 (通常是 20位数字)
    random_part = "".join([str(random.randint(0, 9)) for _ in range(20)])
    return f"jQuery3510{random_part}_{ts}", str(ts)


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


def fix_url_params(url: str) -> str:
    """补全参数并生成真实回调"""
    if "eastmoney.com" not in url: return url

    parsed = urllib.parse.urlparse(url)
    qs = dict(urllib.parse.parse_qsl(parsed.query))

    # 强制全量字段
    if "/api/qt/stock/get" in parsed.path:
        if "fields" not in qs or len(qs["fields"]) < 50:
            qs["fields"] = FULL_FIELDS

    if "ut" not in qs: qs["ut"] = "fa5fd1943c7b386f172d6893dbfba10b"
    if "invt" not in qs: qs["invt"] = "2"
    if "fltt" not in qs: qs["fltt"] = "1"

    # 重新生成 callback，覆盖旧的
    cb, ts = generate_jquery_cb()
    qs["cb"] = cb
    qs["_"] = ts

    new_query = urllib.parse.urlencode(qs)
    return urllib.parse.urlunparse(parsed._replace(query=new_query))


# ========================= 核心修改 1 =========================
# 删除 js_fetch 逻辑，只保留 goto

async def fetch_data(url: str, rid: str, timeout: float = 15.0):
    url = fix_url_params(url)

    page = None
    try:
        page = await asyncio.wait_for(PAGE_QUEUE.get(), timeout=5.0)
        if page.is_closed():
            raise Exception("Page Closed")

        # 保证在东财域名上下文
        if "eastmoney.com" not in page.url:
            await page.goto(
                "https://quote.eastmoney.com/center/",
                wait_until="domcontentloaded",
                timeout=8000
            )

        logger.info(f"[{rid}] Mode: GOTO -> {url[:80]}")

        # 关键：使用 document navigation
        response = await page.goto(
            url,
            wait_until="commit",
            timeout=int(timeout * 1000)
        )

        if not response or response.status != 200:
            raise Exception(f"HTTP {response.status if response else 'Null'}")

        text_content = await response.text()
        data = clean_jsonp(text_content)

        if not data:
            raise Exception("Empty Data")

        return data

    except Exception as e:
        logger.error(f"[{rid}] Fail: {e}")
        raise HTTPException(status_code=502, detail=str(e))

    finally:
        if page and not page.is_closed():
            await PAGE_QUEUE.put(page)


async def _refresh_worker(page: Page):
    try:
        await page.goto("https://quote.eastmoney.com/center/", wait_until="domcontentloaded", timeout=10000)
    except:
        pass

    # =========================================================


# 4. 数据处理与接口
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


# Models
class BaseReq(BaseModel): secid: str


class UrlReq(BaseModel): url: str


class KlineReq(BaseModel): secid: str; ndays: int


class KlineRangeReq(BaseModel): secid: str; beg: Optional[str] = None; end: Optional[str] = None


class USKlineReq(BaseModel): secid: str; market: str; beg: Optional[str] = None; end: Optional[str] = None


class TickReq(BaseModel): secid: str


def normalize_secid(code: str) -> str:
    if "." in code: return code
    if code.startswith(("83", "43")): return f"113.{code}"
    if code.startswith(("6", "5")): return f"1.{code}"
    return f"0.{code}"


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
def health(): return {"status": "ok", "workers": PAGE_QUEUE.qsize()}


@app.post("/stock/snapshot")
async def stock_snapshot(req: BaseReq, request: Request):
    url = f"https://push2.eastmoney.com/api/qt/stock/get?secid={req.secid}"
    raw = await fetch_data(url, request.state.rid)
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/realtime")
async def stock_realtime(req: UrlReq, request: Request):
    raw = await fetch_data(req.url, request.state.rid)
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/etf/realtime")
async def etf_realtime(req: UrlReq, request: Request):
    raw = await fetch_data(req.url, request.state.rid)
    if not raw or not raw.get("data"): raise HTTPException(404, "Not Found")
    return standardize_realtime_data(raw["data"])


@app.post("/stock/ticks")
async def stock_ticks(req: TickReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2.eastmoney.com/api/qt/stock/details/get?secid={secid}&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-100&num=100"
    raw = await fetch_data(url, request.state.rid)
    return standardize_tick(raw.get("data", {}), secid.split(".")[-1]) if raw else []


@app.post("/stock/kline")
async def stock_kline(req: KlineReq, request: Request):
    secid = normalize_secid(req.secid)
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    raw = await fetch_data(url, request.state.rid)
    data = standardize_kline(raw.get("data", {}), secid) if raw else []
    if req.ndays and len(data) > req.ndays:
        data = data[-req.ndays:]
    return data


@app.post("/stock/kline/range")
async def stock_kline_range(req: KlineRangeReq, request: Request):
    secid = normalize_secid(req.secid)
    beg = req.beg or "19900101"
    end = req.end or "20991231"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg={beg}&end={end}&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
    raw = await fetch_data(url, request.state.rid)
    return standardize_kline(raw.get("data", {}), secid) if raw else []


@app.post("/stock/kline/us")
async def stock_kline_us(req: USKlineReq, request: Request):
    secid = f"{req.market}.{req.secid}"
    url = f"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={secid}&klt=101&fqt=1&beg=19900101&end=20991231&fields1=f1&fields2=f51,f52,f53,f54,f55,f56"
    raw = await fetch_data(url, request.state.rid)
    return standardize_kline(raw.get("data", {}), secid) if raw else []


@app.post("/proxy/json")
async def proxy_json(req: UrlReq, request: Request):
    return await fetch_data(req.url, request.state.rid)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("stock_service:app", host="0.0.0.0", port=8000, reload=False, limit_concurrency=100, access_log=False)
