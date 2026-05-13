"""
股票数据采集系统 - 通用工具函数
"""

import tls_client
import random
import pymysql
from datetime import datetime

from config import DB_CONFIG, USER_AGENTS, WORKER_ID, CREATE_TABLE_SQL


def _build_headers():
    """构建请求头 (模拟浏览器完整头)"""
    return {
        "User-Agent": random.choice(USER_AGENTS),
        "Accept": "*/*",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Referer": "https://data.eastmoney.com/",
    }


def _build_session():
    """构建 tls_client.Session (Chrome TLS指纹 + 随机扩展顺序)"""
    session = tls_client.Session(
        client_identifier="chrome_120",
        random_tls_extension_order=True,
    )
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


def _fetch_json(url, params=None, timeout=15, max_retries=3):
    """通用JSON请求，返回dict或None (tls_client + 自动重试)"""
    headers = _build_headers()
    for attempt in range(1, max_retries + 1):
        session = _build_session()
        try:
            resp = session.get(url, params=params, headers=headers)
            if resp.status_code >= 400:
                raise Exception(f"HTTP {resp.status_code}")
            return resp.json()
        except Exception as e:
            _log(f"请求失败(第{attempt}次) {url[:80]}: {type(e).__name__}: {e}")
            if attempt == max_retries:
                return None
        finally:
            pass
    return None


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
