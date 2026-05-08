"""
模块3: 市场全局情绪指标 —— 感知系统性风险与胜率
"""

import json
import pymysql
from datetime import datetime, timedelta

from market_smart_config import (
    DB_CONFIG, REDIS_AVAILABLE, rds,
    LIMIT_UP_URL, LIMIT_DOWN_URL, CONSECUTIVE_BOARD_URL,
    LIMIT_POOL_PARAMS,
)
from market_smart_utils import (
    _log, _redis_key, _fetch_json, clean_value, _safe_float, _safe_int,
)


def fetch_limit_up_pool():
    """获取涨停股池"""
    today = datetime.now().strftime('%Y%m%d')
    params = {**LIMIT_POOL_PARAMS, 'date': today}
    data = _fetch_json(LIMIT_UP_URL, params=params)
    if data and data.get('data') and data['data'].get('pool'):
        return data['data']['pool']
    return []


def fetch_limit_down_pool():
    """获取跌停股池"""
    today = datetime.now().strftime('%Y%m%d')
    params = {**LIMIT_POOL_PARAMS, 'date': today}
    data = _fetch_json(LIMIT_DOWN_URL, params=params)
    if data and data.get('data') and data['data'].get('pool'):
        return data['data']['pool']
    return []


def fetch_consecutive_board_pool():
    """获取连板股池 (getTopicQSPool)"""
    today = datetime.now().strftime('%Y%m%d')
    params = {**LIMIT_POOL_PARAMS, 'date': today, 'sort': 'zdp:desc'}
    data = _fetch_json(CONSECUTIVE_BOARD_URL, params=params)
    if data and data.get('data') and data['data'].get('pool'):
        return data['data']['pool']
    return []


def save_limit_pool(items, limit_type):
    """保存涨/跌停股池"""
    if not items:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_limit_pool (
                    trade_date, stock_code, stock_name, market_type,
                    limit_type, latest_price, change_pct, consecutive_boards,
                    seal_amount, first_limit_time, last_limit_time,
                    open_times, turnover_rate, industry, data_timestamp
                ) VALUES (
                    %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
                ) ON DUPLICATE KEY UPDATE
                    latest_price = VALUES(latest_price),
                    change_pct = VALUES(change_pct),
                    consecutive_boards = VALUES(consecutive_boards),
                    seal_amount = VALUES(seal_amount),
                    last_limit_time = VALUES(last_limit_time),
                    open_times = VALUES(open_times),
                    turnover_rate = VALUES(turnover_rate),
                    updated_at = CURRENT_TIMESTAMP
            """
            trade_date = datetime.now().strftime('%Y-%m-%d')
            ts = int(datetime.now().timestamp())

            values_list = []
            for item in items:
                zttj = item.get('zttj', {})
                if isinstance(zttj, str):
                    try:
                        zttj = json.loads(zttj)
                    except Exception:
                        zttj = {}

                first_time = zttj.get('ztsj', None)
                last_time = zttj.get('ztsj', None)
                open_times = _safe_int(zttj.get('kbc', 0), 0)

                values = (
                    trade_date,
                    clean_value(item.get('c')),
                    clean_value(item.get('n')),
                    _safe_int(item.get('m', 0), 0),
                    limit_type,
                    _safe_float(item.get('p')),
                    _safe_float(item.get('zdp')),
                    _safe_int(item.get('lbc', 0), 0),
                    _safe_float(item.get('fund')),
                    first_time,
                    last_time,
                    open_times,
                    _safe_float(item.get('hs')),
                    clean_value(item.get('hybk')),
                    ts,
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)
            saved = len(values_list)
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"涨跌停池入库失败: {e}")
    finally:
        conn.close()
    return saved


def calc_yesterday_limit_up_premium():
    """计算昨日涨停池今日溢价率"""
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
            today = datetime.now().strftime('%Y-%m-%d')
            sql = """
                SELECT s.stock_code, s.stock_name, s.latest_price AS limit_price,
                       t.auction_change_pct, t.auction_open_ratio
                FROM stock_limit_pool s
                LEFT JOIN stock_auction_snapshot t
                    ON s.stock_code = t.stock_code AND t.trade_date = %s AND t.auction_time = (
                        SELECT MAX(auction_time) FROM stock_auction_snapshot
                        WHERE stock_code = s.stock_code AND trade_date = %s
                    )
                WHERE s.trade_date = %s AND s.limit_type = 'UP'
            """
            cursor.execute(sql, (today, today, yesterday))
            rows = cursor.fetchall()

            if not rows:
                return None, None

            open_ratios = [r['auction_open_ratio'] for r in rows if r.get('auction_open_ratio') is not None]
            change_pcts = [r['auction_change_pct'] for r in rows if r.get('auction_change_pct') is not None]

            avg_open = round(sum(open_ratios) / len(open_ratios), 2) if open_ratios else None
            avg_change = round(sum(change_pcts) / len(change_pcts), 2) if change_pcts else None
            return avg_open, avg_change
    except Exception as e:
        _log(f"计算昨日溢价失败: {e}")
        return None, None
    finally:
        conn.close()


def calc_board_progress_rate():
    """计算连板晋级率(基于连板股池数据)"""
    items = fetch_consecutive_board_pool()
    if not items:
        return None, None

    board_counts = {}
    for item in items:
        lbc = _safe_int(item.get('lbc', 0), 0)
        board_counts[lbc] = board_counts.get(lbc, 0) + 1

    b2 = board_counts.get(2, 0)
    b3 = board_counts.get(3, 0)
    rate_2to3 = round(b3 / (b2 + b3) * 100, 2) if (b2 + b3) > 0 else None

    b4 = board_counts.get(4, 0)
    rate_3to4 = round(b4 / (b3 + b4) * 100, 2) if (b3 + b4) > 0 else None

    return rate_2to3, rate_3to4


def calc_sentiment_score(limit_up, limit_down, max_boards, avg_open):
    """计算市场情绪评分(0-100)"""
    score = 50

    if limit_up >= 80:
        score += 25
    elif limit_up >= 50:
        score += 20
    elif limit_up >= 30:
        score += 10
    elif limit_up < 10:
        score -= 20

    if limit_down >= 50:
        score -= 30
    elif limit_down >= 20:
        score -= 15
    elif limit_down >= 10:
        score -= 5

    if max_boards and max_boards >= 8:
        score += 15
    elif max_boards and max_boards >= 5:
        score += 10
    elif max_boards and max_boards >= 3:
        score += 5

    if avg_open is not None:
        if avg_open >= 3:
            score += 10
        elif avg_open >= 1:
            score += 5
        elif avg_open < -2:
            score -= 15
        elif avg_open < 0:
            score -= 8

    return max(0, min(100, score))


def save_sentiment(sentiment_data):
    """保存市场情绪指标"""
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_market_sentiment (
                    trade_date, snapshot_time,
                    limit_up_count, limit_down_count, limit_ratio,
                    max_consecutive_boards,
                    board2_progress_rate, board3_progress_rate,
                    yesterday_limit_up_open_pct, yesterday_limit_up_avg_change,
                    sentiment_score, data_timestamp
                ) VALUES (
                    %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s
                ) ON DUPLICATE KEY UPDATE
                    limit_up_count = VALUES(limit_up_count),
                    limit_down_count = VALUES(limit_down_count),
                    limit_ratio = VALUES(limit_ratio),
                    max_consecutive_boards = VALUES(max_consecutive_boards),
                    board2_progress_rate = VALUES(board2_progress_rate),
                    board3_progress_rate = VALUES(board3_progress_rate),
                    yesterday_limit_up_open_pct = VALUES(yesterday_limit_up_open_pct),
                    yesterday_limit_up_avg_change = VALUES(yesterday_limit_up_avg_change),
                    sentiment_score = VALUES(sentiment_score),
                    updated_at = CURRENT_TIMESTAMP
            """
            cursor.execute(sql, sentiment_data)
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"情绪指标入库失败: {e}")
    finally:
        conn.close()


def job_sentiment():
    """市场情绪指标采集任务"""
    _log("开始采集市场情绪指标...")

    up_items = fetch_limit_up_pool()
    up_count = len(up_items)
    if up_items:
        save_limit_pool(up_items, 'UP')
        _log(f"涨停池: {up_count} 只")

    down_items = fetch_limit_down_pool()
    down_count = len(down_items)
    if down_items:
        save_limit_pool(down_items, 'DOWN')
        _log(f"跌停池: {down_count} 只")

    # 连板数据从独立接口(getTopicQSPool)获取
    board_items = fetch_consecutive_board_pool()
    max_boards = 0
    if board_items:
        max_boards = max(_safe_int(item.get('lbc', 0), 0) for item in board_items)
        _log(f"连板池: {len(board_items)} 只, 最高 {max_boards} 连板")
    elif up_items:
        # fallback: 从涨停池lbc字段提取
        board_items = [item for item in up_items if _safe_int(item.get('lbc', 0), 0) >= 2]
        if board_items:
            max_boards = max(_safe_int(item.get('lbc', 0), 0) for item in board_items)
            _log(f"连板(涨停池fallback): {len(board_items)} 只, 最高 {max_boards} 连板")

    rate_2to3, rate_3to4 = calc_board_progress_rate()
    avg_open, avg_change = calc_yesterday_limit_up_premium()

    limit_ratio = round(up_count / down_count, 2) if down_count > 0 else (99.0 if up_count > 0 else 0)
    score = calc_sentiment_score(up_count, down_count, max_boards, avg_open)

    now = datetime.now()
    sentiment_data = (
        now.strftime('%Y-%m-%d'),
        now.strftime('%H:%M:%S'),
        up_count,
        down_count,
        limit_ratio,
        max_boards,
        rate_2to3,
        rate_3to4,
        avg_open,
        avg_change,
        score,
        int(now.timestamp()),
    )
    save_sentiment(sentiment_data)

    if REDIS_AVAILABLE and up_items:
        key = _redis_key("limit_up_stocks")
        rds.delete(key)
        for item in up_items:
            code = item.get('c', '')
            mkt = item.get('m', 0)
            rds.rpush(key, f"{mkt}.{code}")
        rds.expire(key, 900)

    _log(f"情绪指标: 涨停{up_count} 跌停{down_count} 比{limit_ratio} "
         f"最高{max_boards}板 情绪{score}分 昨日溢价{avg_open}%")
