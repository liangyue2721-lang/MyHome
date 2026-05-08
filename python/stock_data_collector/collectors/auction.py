"""
模块1: 集合竞价全息数据 (9:15-9:25) —— 感知主力早盘态度
"""

import time
import pymysql
from datetime import datetime

from config import (
    DB_CONFIG, REDIS_AVAILABLE, rds,
    AUCTION_LIST_URL, AUCTION_FIELDS, AUCTION_FS,
)
from utils import (
    _log, redis_key, _fetch_json, clean_value, _safe_float, _safe_int,
)

MODULE_PREFIX = "market_smart"


def fetch_auction_snapshot():
    """采集全市场集合竞价快照，返回items列表"""
    all_items = []
    page = 1

    while True:
        params = {
            'pn': page,
            'pz': 5000,
            'po': 1,
            'np': 1,
            'fltt': 2,
            'invt': 2,
            'fid': 'f3',
            'fs': AUCTION_FS,
            'fields': AUCTION_FIELDS,
        }
        data = _fetch_json(AUCTION_LIST_URL, params=params)
        if not data or not data.get('data') or not data['data'].get('diff'):
            break

        items = data['data']['diff']
        all_items.extend(items)

        total = data['data'].get('total', 0)
        if len(all_items) >= total:
            break
        page += 1
        time.sleep(1)

    return all_items


def save_auction_snapshot(items):
    """保存集合竞价快照到数据库"""
    if not items:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_auction_snapshot (
                    trade_date, stock_code, stock_name, market_type,
                    auction_price, auction_volume, auction_amount, auction_change_pct,
                    prev_close, auction_open_ratio,
                    unmatched_buy, unmatched_sell,
                    auction_time, data_timestamp
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                ) ON DUPLICATE KEY UPDATE
                    auction_price = VALUES(auction_price),
                    auction_volume = VALUES(auction_volume),
                    auction_amount = VALUES(auction_amount),
                    auction_change_pct = VALUES(auction_change_pct),
                    prev_close = VALUES(prev_close),
                    auction_open_ratio = VALUES(auction_open_ratio),
                    unmatched_buy = VALUES(unmatched_buy),
                    unmatched_sell = VALUES(unmatched_sell),
                    data_timestamp = VALUES(data_timestamp),
                    updated_at = CURRENT_TIMESTAMP
            """
            now = datetime.now()
            trade_date = now.strftime('%Y-%m-%d')
            auction_time = now.strftime('%H:%M:%S')
            ts = int(now.timestamp())

            values_list = []
            for item in items:
                auction_price = _safe_float(item.get('f86'))
                prev_close = _safe_float(item.get('f18'))
                auction_change = _safe_float(item.get('f88'))

                open_ratio = None
                if auction_price and prev_close and prev_close > 0:
                    open_ratio = round((auction_price - prev_close) / prev_close * 100, 2)

                if auction_change is None:
                    auction_change = open_ratio

                values = (
                    trade_date,
                    clean_value(item.get('f12')),
                    clean_value(item.get('f14')),
                    _safe_int(item.get('f13'), 0),
                    auction_price,
                    _safe_int(item.get('f87')),
                    _safe_float(item.get('f6')),
                    auction_change,
                    prev_close,
                    open_ratio,
                    None,
                    None,
                    auction_time,
                    ts,
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)
            saved = len(values_list)
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"竞价快照入库失败: {e}")
    finally:
        conn.close()
    return saved


def detect_auction_anomaly(items):
    """检测竞价异动: 高开>5% / 竞价涨停"""
    anomalies = []
    for item in items:
        auction_price = _safe_float(item.get('f86'))
        prev_close = _safe_float(item.get('f18'))
        auction_vol = _safe_int(item.get('f87'))
        auction_change = _safe_float(item.get('f88'))

        if not auction_price or not prev_close or prev_close <= 0:
            continue

        open_ratio = (auction_price - prev_close) / prev_close * 100

        if open_ratio >= 5:
            anomalies.append({
                'code': item.get('f12'),
                'name': item.get('f14'),
                'type': 'HIGH_OPEN',
                'ratio': round(open_ratio, 2),
                'volume': auction_vol,
            })

        if auction_change and auction_change >= 9.8:
            anomalies.append({
                'code': item.get('f12'),
                'name': item.get('f14'),
                'type': 'AUCTION_LIMIT_UP',
                'ratio': round(open_ratio, 2),
                'volume': auction_vol,
            })

    return anomalies


def save_auction_anomaly_to_redis(anomalies):
    """将竞价异动股写入Redis供盘口监控"""
    if not REDIS_AVAILABLE or not anomalies:
        return

    key = redis_key(MODULE_PREFIX, "auction_hot")
    rds.delete(key)
    for a in anomalies:
        code = a.get('code', '')
        mkt = 1 if code.startswith('6') else 0
        rds.rpush(key, f"{mkt}.{code}")
    rds.expire(key, 900)

    _log(f"竞价异动: {len(anomalies)} 只, 已写入Redis")


def job_auction():
    """集合竞价采集任务"""
    _log("开始采集集合竞价快照...")
    items = fetch_auction_snapshot()
    if items:
        saved = save_auction_snapshot(items)
        _log(f"竞价快照: 获取 {len(items)} 只, 入库 {saved} 条")

        # 异动检测仅在竞价时段(9:15-9:25)有效，盘中价格数据无竞价含义
        current_time = datetime.now().strftime("%H:%M")
        if "09:15" <= current_time <= "09:26":
            anomalies = detect_auction_anomaly(items)
            if anomalies:
                save_auction_anomaly_to_redis(anomalies)
                for a in anomalies[:10]:
                    _log(f"  竞价异动: {a['code']} {a['name']} {a['type']} 高开{a['ratio']}%")
        else:
            _log("非竞价时段，跳过异动检测(盘中价格非竞价数据)")
    else:
        _log("竞价快照: 未获取到数据")
