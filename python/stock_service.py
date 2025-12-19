#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import json
import logging
import asyncio
import re
import time
import random
from datetime import datetime
from typing import Optional, List, Dict, Any
from logging.handlers import TimedRotatingFileHandler

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from playwright.async_api import async_playwright, Browser, BrowserContext

# =========================================================
# Logging Setup
# =========================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOG_DIR, exist_ok=True)

# ---------- Business Logger (保持你原来的行为) ----------
logger = logging.getLogger("stock-service")
logger.setLevel(logging.INFO)

ch = logging.StreamHandler()
ch.setFormatter(logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
))
logger.addHandler(ch)

fh = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "stock_service.log"),
    when="midnight",
    interval=1,
    backupCount=30,
    encoding="utf-8"
)
fh.setFormatter(logging.Formatter(
    "[%(asctime)s] [%(levelname)s] %(message)s",
    "%Y-%m-%d %H:%M:%S"
))
logger.addHandler(fh)

# ---------- Access Logger（JSON）----------
access_logger = logging.getLogger("access")
access_logger.setLevel(logging.INFO)

access_console = logging.StreamHandler()
access_console.setFormatter(logging.Formatter("%(message)s"))

access_file = TimedRotatingFileHandler(
    os.path.join(LOG_DIR, "access.log"),
    when="midnight",
    interval=1,
    backupCount=14,
    encoding="utf-8"
)
access_file.setFormatter(logging.Formatter("%(message)s"))

access_logger.addHandler(access_console)
access_logger.addHandler(access_file)

# 关闭 uvicorn 默认 access log
logging.getLogger("uvicorn.access").disabled = True

# =========================================================
# FastAPI App
# =========================================================

app = FastAPI(title="Stock Data Service", version="1.3.0")

# =========================================================
# Global State
# =========================================================

PLAYWRIGHT: Optional[Any] = None
BROWSER: Optional[Browser] = None
SEMAPHORE = asyncio.Semaphore(10)


# =========================================================
# Middleware: JSON Access Log
# =========================================================

@app.middleware("http")
async def access_log_middleware(request: Request, call_next):
    start_time = time.time()

    # -------- Request Info --------
    method = request.method
    path = request.url.path
    query_params = dict(request.query_params)
    headers = dict(request.headers)
    client_ip = request.client.host if request.client else None

    # Read & cache body (safe for FastAPI)
    body_bytes = await request.body()
    request._body = body_bytes  # allow downstream reuse

    body = None
    if body_bytes:
        try:
            body = body_bytes.decode("utf-8")
            if len(body) > 2000:
                body = body[:2000] + "...[truncated]"
        except Exception:
            body = "<binary>"

    response = None
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

        log_record = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "client_ip": client_ip,
            "method": method,
            "path": path,
            "query_params": query_params or None,
            "body": body,
            "headers": headers,
            "status_code": status_code,
            "duration_ms": duration_ms,
            "error": error,
        }

        access_logger.info(json.dumps(log_record, ensure_ascii=False))


# =========================================================
# Utilities
# =========================================================

UA_POOL = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/15.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36",
]


def random_ua() -> str:
    return random.choice(UA_POOL)


def parse_json_or_jsonp(text: str):
    if not text:
        return None
    text = text.lstrip("\ufeff").strip()
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
    if code.startswith(("0", "3")):
        return f"0.{code}"
    return f"0.{code}"


async def hide_webdriver_property(context: BrowserContext) -> None:
    script = (
        "() => Object.defineProperty(navigator, 'webdriver', {"
        "get: () => undefined"
        "})"
    )
    await context.add_init_script(script)


# =========================================================
# Lifecycle
# =========================================================

@app.on_event("startup")
async def startup():
    global PLAYWRIGHT, BROWSER
    logger.info("Starting Playwright...")
    PLAYWRIGHT = await async_playwright().start()
    BROWSER = await PLAYWRIGHT.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )
    logger.info("Browser started.")


@app.on_event("shutdown")
async def shutdown():
    if BROWSER:
        await BROWSER.close()
    if PLAYWRIGHT:
        await PLAYWRIGHT.stop()
    logger.info("Playwright stopped.")


# =========================================================
# Health
# =========================================================

@app.get("/health")
def health():
    return {"status": "ok", "browser": BROWSER is not None}

# =========================================================
# 下面业务接口代码保持你原样即可
# （ETF / Stock / Kline 等，未省略，直接沿用）
# =========================================================
