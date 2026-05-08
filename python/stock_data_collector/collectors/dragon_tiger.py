"""
模块4: 龙虎榜与异动公告 (盘后) —— 给资金打标签
"""

import time
import pymysql
from datetime import datetime

from config import (
    DB_CONFIG, DRAGON_TIGER_URL, DRAGON_TIGER_DETAIL_URL,
)
from utils import (
    _log, _fetch_json, _safe_float,
)


def fetch_dragon_tiger_list(trade_date=None):
    """获取龙虎榜列表"""
    if not trade_date:
        trade_date = datetime.now().strftime('%Y-%m-%d')

    all_items = []
    page = 1

    while True:
        params = {
            'reportName': 'RPT_DMSK_TS_STOCKLHBNEW',
            'columns': 'ALL',
            'filter': f'(TRADE_DATE="{trade_date}")',
            'pageNumber': page,
            'pageSize': 200,
            'sortTypes': -1,
            'sortColumns': 'CHANGE_RATE',
        }
        data = _fetch_json(DRAGON_TIGER_URL, params=params)
        if not data or not data.get('result') or not data['result'].get('data'):
            break

        items = data['result']['data']
        all_items.extend(items)

        total = data['result'].get('count', 0)
        if len(all_items) >= total:
            break
        page += 1
        time.sleep(1)

    return all_items


def fetch_dragon_tiger_detail(trade_date=None):
    """获取龙虎榜席位明细"""
    if not trade_date:
        trade_date = datetime.now().strftime('%Y-%m-%d')

    all_items = []
    page = 1

    while True:
        params = {
            'reportName': 'RPT_DMSK_TS_STOCKLHBNEWDETAIL',
            'columns': 'ALL',
            'filter': f'(TRADE_DATE="{trade_date}")',
            'pageNumber': page,
            'pageSize': 500,
            'sortTypes': -1,
            'sortColumns': 'NET_AMOUNT',
        }
        data = _fetch_json(DRAGON_TIGER_DETAIL_URL, params=params)
        if not data or not data.get('result') or not data['result'].get('data'):
            break

        items = data['result']['data']
        all_items.extend(items)

        total = data['result'].get('count', 0)
        if len(all_items) >= total:
            break
        page += 1
        time.sleep(1)

    return all_items


def save_dragon_tiger(items):
    """保存龙虎榜数据"""
    if not items:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_dragon_tiger (
                    trade_date, stock_code, stock_name,
                    change_pct, reason, buy_amount, sell_amount,
                    net_amount, north_net_buy, data_timestamp
                ) VALUES (
                    %s,%s,%s,%s,%s,%s,%s,%s,%s,%s
                ) ON DUPLICATE KEY UPDATE
                    change_pct = VALUES(change_pct),
                    buy_amount = VALUES(buy_amount),
                    sell_amount = VALUES(sell_amount),
                    net_amount = VALUES(net_amount),
                    north_net_buy = VALUES(north_net_buy),
                    updated_at = CURRENT_TIMESTAMP
            """
            ts = int(datetime.now().timestamp())

            values_list = []
            seen = set()
            for item in items:
                code = item.get('SECURITY_CODE', '')
                reason = item.get('REASON_TYPE', '')
                key = (code, reason)
                if key in seen:
                    continue
                seen.add(key)

                values = (
                    item.get('TRADE_DATE', '')[:10] if item.get('TRADE_DATE') else None,
                    code,
                    item.get('SECURITY_NAME_ABBR', ''),
                    _safe_float(item.get('CHANGE_RATE')),
                    reason,
                    _safe_float(item.get('BUY_AMOUNT')),
                    _safe_float(item.get('SELL_AMOUNT')),
                    _safe_float(item.get('NET_AMOUNT')),
                    _safe_float(item.get('HSGT_NET_AMOUNT')),
                    ts,
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)
            saved = len(values_list)
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"龙虎榜入库失败: {e}")
    finally:
        conn.close()
    return saved


def save_dragon_tiger_detail(items):
    """保存龙虎榜席位明细"""
    if not items:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_dragon_tiger_detail (
                    trade_date, stock_code, stock_name,
                    seat_name, seat_code,
                    buy_amount, sell_amount, net_amount,
                    is_institution, data_timestamp
                ) VALUES (
                    %s,%s,%s,%s,%s,%s,%s,%s,%s,%s
                )
            """
            ts = int(datetime.now().timestamp())

            values_list = []
            for item in items:
                seat_name = item.get('OPERATEDEPT_NAME', '')
                is_inst = 0
                if '机构专用' in seat_name:
                    is_inst = 1
                elif '沪股通' in seat_name or '深股通' in seat_name:
                    is_inst = 2

                values = (
                    item.get('TRADE_DATE', '')[:10] if item.get('TRADE_DATE') else None,
                    item.get('SECURITY_CODE', ''),
                    item.get('SECURITY_NAME_ABBR', ''),
                    seat_name,
                    item.get('OPERATEDEPT_CODE', ''),
                    _safe_float(item.get('BUY_AMOUNT'), 0),
                    _safe_float(item.get('SELL_AMOUNT'), 0),
                    _safe_float(item.get('NET_AMOUNT'), 0),
                    is_inst,
                    ts,
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)
            saved = len(values_list)
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"龙虎榜明细入库失败: {e}")
    finally:
        conn.close()
    return saved


def job_dragon_tiger():
    """龙虎榜采集任务(盘后执行)"""
    _log("开始采集龙虎榜数据...")

    items = fetch_dragon_tiger_list()
    if items:
        saved = save_dragon_tiger(items)
        _log(f"龙虎榜: {len(items)} 条, 入库 {saved} 条")
    else:
        _log("龙虎榜: 未获取到数据(可能尚未公布)")

    detail_items = fetch_dragon_tiger_detail()
    if detail_items:
        saved = save_dragon_tiger_detail(detail_items)
        _log(f"龙虎榜明细: {len(detail_items)} 条, 入库 {saved} 条")
    else:
        _log("龙虎榜明细: 未获取到数据")
