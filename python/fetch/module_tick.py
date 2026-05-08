"""
模块2: 五档盘口快照数据 (盘中) —— 感知盘口微观结构
"""

import time
import pymysql
from datetime import datetime

from market_smart_config import (
    DB_CONFIG, REDIS_AVAILABLE, rds,
    STOCK_SNAPSHOT_URL, SNAPSHOT_FIELDS,
)
from market_smart_utils import (
    _log, _redis_key, _fetch_json, _secid,
    clean_value, _safe_float, _safe_int,
)
from module_sentiment import fetch_limit_up_pool


def fetch_stock_snapshot(stock_code, market_type):
    """获取单只股票五档盘口快照"""
    secid = _secid(stock_code, market_type)
    params = {
        'secid': secid,
        'fields': SNAPSHOT_FIELDS,
        'ut': 'fa5fd1943c7b386f172d6893dbfba10b',
        'fltt': 2,
        'invt': 2,
    }
    data = _fetch_json(STOCK_SNAPSHOT_URL, params=params)
    if not data or not data.get('data'):
        return None
    return data['data']


def get_monitor_stocks():
    """获取需要监控盘口的股票列表: 涨停池 + 竞价高开股"""
    stocks = []
    # 1. 从涨停池获取
    limit_stocks = fetch_limit_up_pool()
    for s in limit_stocks:
        stocks.append((s.get('c', ''), _safe_int(s.get('m', 0), 0)))

    # 2. 从Redis获取竞价高开股
    if REDIS_AVAILABLE:
        hot_keys = rds.lrange(_redis_key("auction_hot"), 0, 49)
        for k in hot_keys:
            parts = k.split('.')
            if len(parts) == 2:
                stocks.append((parts[1], _safe_int(parts[0], 0)))

    # 去重
    seen = set()
    unique = []
    for code, mkt in stocks:
        if code and code not in seen:
            seen.add(code)
            unique.append((code, mkt))
    return unique[:200]


def save_bid_ask_snapshot(snapshots):
    """保存五档盘口快照"""
    if not snapshots:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_bid_ask_snapshot (
                    trade_date, stock_code, stock_name, market_type,
                    latest_price, change_pct,
                    buy1_price, buy1_vol, buy2_price, buy2_vol,
                    buy3_price, buy3_vol, buy4_price, buy4_vol,
                    buy5_price, buy5_vol,
                    sell1_price, sell1_vol, sell2_price, sell2_vol,
                    sell3_price, sell3_vol, sell4_price, sell4_vol,
                    sell5_price, sell5_vol,
                    total_volume, total_amount, bid_ask_imbalance,
                    snapshot_time, data_timestamp
                ) VALUES (
                    %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                    %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
                )
            """
            now = datetime.now()
            trade_date = now.strftime('%Y-%m-%d')
            snapshot_time = now.strftime('%H:%M:%S')
            ts = int(now.timestamp())

            values_list = []
            for snap in snapshots:
                values = (
                    trade_date,
                    clean_value(snap.get('f57')),      # stock_code
                    clean_value(snap.get('f58')),      # stock_name
                    _safe_int(snap.get('f107'), 0),    # market_type (stock/get上f107=市场)
                    _safe_float(snap.get('f43')),      # latest_price
                    _safe_float(snap.get('f71')),      # change_pct
                    # 买一~买五 (价,量)
                    _safe_float(snap.get('f19')), _safe_int(snap.get('f20')),  # buy1
                    _safe_float(snap.get('f17')), _safe_int(snap.get('f18')),  # buy2
                    _safe_float(snap.get('f15')), _safe_int(snap.get('f16')),  # buy3
                    _safe_float(snap.get('f13')), _safe_int(snap.get('f14')),  # buy4
                    _safe_float(snap.get('f11')), _safe_int(snap.get('f12')),  # buy5
                    # 卖一~卖五 (价,量)
                    _safe_float(snap.get('f39')), _safe_int(snap.get('f40')),  # sell1
                    _safe_float(snap.get('f37')), _safe_int(snap.get('f38')),  # sell2
                    _safe_float(snap.get('f35')), _safe_int(snap.get('f36')),  # sell3
                    _safe_float(snap.get('f33')), _safe_int(snap.get('f34')),  # sell4
                    _safe_float(snap.get('f31')), _safe_int(snap.get('f32')),  # sell5
                    _safe_int(snap.get('f47')),        # total_volume
                    _safe_float(snap.get('f48')),      # total_amount
                    _safe_float(snap.get('f161')),     # bid_ask_imbalance (委比)
                    snapshot_time,
                    ts,
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)
            saved = len(values_list)
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"盘口快照入库失败: {e}")
    finally:
        conn.close()
    return saved


def job_tick():
    """盘中盘口快照采集: 对关键股票采集五档盘口"""
    stocks = get_monitor_stocks()
    if not stocks:
        _log("盘口快照: 无监控标的，跳过")
        return

    _log(f"盘口快照: 开始采集 {len(stocks)} 只...")
    snapshots = []
    for code, mkt in stocks:
        snap = fetch_stock_snapshot(code, mkt)
        if snap:
            snapshots.append(snap)
        time.sleep(0.3)

    if snapshots:
        saved = save_bid_ask_snapshot(snapshots)
        _log(f"盘口快照: 入库 {saved} 条")
    else:
        _log("盘口快照: 未获取到数据")
