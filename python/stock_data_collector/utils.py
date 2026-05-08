"""
股票数据采集系统 - 通用工具函数
"""

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import random
import pymysql
from datetime import datetime

from config import DB_CONFIG, USER_AGENTS, WORKER_ID, CREATE_TABLE_SQL


def _build_headers():
    """构建请求头"""
    return {
        "User-Agent": random.choice(USER_AGENTS),
        "Accept": "*/*",
        "Referer": "https://data.eastmoney.com/",
    }


def _build_session():
    """构建 requests.Session (自动重试)"""
    session = requests.Session()
    retry_strategy = Retry(
        total=3,
        backoff_factor=0.5,
        status_forcelist=[502, 503, 504],
        allowed_methods=["GET"],
    )
    adapter = HTTPAdapter(max_retries=retry_strategy, pool_connections=2, pool_maxsize=2)
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    return session


def redis_key(prefix, suffix):
    """生成Redis key: {prefix}:{today}:{suffix}"""
    today = datetime.now().strftime('%Y-%m-%d')
    return f"{prefix}:{today}:{suffix}"


def _log(msg):
    """带时间戳和机器标识的日志"""
    ts = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    print(f"[{ts}][{WORKER_ID}] {msg}")


def clean_value(val):
    """处理接口返回的空值 '-' 为 None"""
    if val == "-" or val == "":
        return None
    return val


def _safe_float(val, default=None):
    try:
        if val is None or val == "-" or val == "":
            return default
        return float(val)
    except (ValueError, TypeError):
        return default


def _safe_int(val, default=None):
    try:
        if val is None or val == "-" or val == "":
            return default
        return int(val)
    except (ValueError, TypeError):
        return default


def _secid(stock_code, market_type):
    """生成东方财富secid: 0.000001(深) 1.600519(沪)"""
    prefix = 1 if market_type in (1, 11, 17) else 0
    return f"{prefix}.{stock_code}"


def _fetch_json(url, params=None, timeout=15):
    """通用JSON请求，返回dict或None"""
    session = _build_session()
    try:
        resp = session.get(url, headers=_build_headers(), params=params, timeout=timeout)
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        _log(f"请求失败 {url[:80]}: {type(e).__name__}: {e}")
        return None
    finally:
        session.close()


def init_db():
    """初始化所有数据库表"""
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            for name, sql in CREATE_TABLE_SQL.items():
                cursor.execute(sql)
                _log(f"表 {name} 初始化完成")
        conn.commit()
    finally:
        conn.close()
