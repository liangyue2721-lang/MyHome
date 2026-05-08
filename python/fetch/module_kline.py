"""
模块5: K线数据 + 逐笔成交明细 —— 量化回测 + 集合竞价增强

接口1: push2his.eastmoney.com/api/qt/stock/kline/get
    - 历史K线(含当天), 支持1/5/15/30/60分钟及日/周/月
    - 返回data.klines数组, 每项逗号分隔: 时间,开,收,高,低,量(手),额(元)[,振幅,涨跌幅,涨跌额,换手率]

接口2: push2.eastmoney.com/api/qt/stock/details/get
    - 逐笔成交明细, 全天(含9:15集合竞价)
    - fields2=f51(时间),f52(价),f53(量/手),f54(方向1买2卖4中),f55(额)
    - pos分页, iscca=1包含竞价
"""

import time
import pymysql
from datetime import datetime

from market_smart_config import (
    DB_CONFIG, REDIS_AVAILABLE, rds,
    KLINE_HISTORY_URL, TICK_DETAIL_URL,
)
from market_smart_utils import (
    _log, _redis_key, _fetch_json, _secid,
    clean_value, _safe_float, _safe_int,
)


# =====================================================================
# K线数据
# =====================================================================

def fetch_kline(stock_code, market_type, klt=5, lmt=48, fqt=1):
    """
    获取K线数据
    klt: 1/5/15/30/60 分钟线, 101日线, 102周线, 103月线
    lmt: 获取条数 (5分钟线一天48根)
    fqt: 0不复权 1前复权 2后复权
    """
    secid = _secid(stock_code, market_type)
    params = {
        'secid': secid,
        'klt': klt,
        'fqt': fqt,
        'lmt': lmt,
        'end': '20500101',
        'iscca': 1,
        'fields1': 'f1,f2,f3,f4,f5',
        'fields2': 'f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61',
        'ut': 'fa5fd1943c7b386f172d6893dbfba10b',
    }
    data = _fetch_json(KLINE_HISTORY_URL, params=params)
    if not data or not data.get('data') or not data['data'].get('klines'):
        return []
    return data['data']['klines']


def parse_kline_row(kline_str, stock_code):
    """
    解析单根K线字符串
    格式: 时间,开盘,收盘,最高,最低,成交量(手),成交额(元),振幅%,涨跌幅%,涨跌额,换手率%
    """
    parts = kline_str.split(',')
    if len(parts) < 7:
        return None

    kline_time = parts[0]  # "2026-05-08 14:55" or "2026-05-08"
    trade_date = kline_time[:10]

    return {
        'trade_date': trade_date,
        'stock_code': stock_code,
        'kline_time': kline_time,
        'open_price': _safe_float(parts[1]),
        'close_price': _safe_float(parts[2]),
        'high_price': _safe_float(parts[3]),
        'low_price': _safe_float(parts[4]),
        'volume': _safe_int(parts[5]),
        'amount': _safe_float(parts[6]),
        'change_pct': _safe_float(parts[8]) if len(parts) > 8 else None,
        'amplitude': _safe_float(parts[7]) if len(parts) > 7 else None,
        'turnover_rate': _safe_float(parts[10]) if len(parts) > 10 else None,
    }


def save_klines(rows):
    """保存K线数据到stock_kline_5min"""
    if not rows:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT IGNORE INTO stock_kline_5min (
                    trade_date, stock_code, kline_time,
                    open_price, close_price, high_price, low_price,
                    volume, amount, change_pct, amplitude, turnover_rate,
                    data_timestamp
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """
            ts = int(datetime.now().timestamp())
            values_list = []
            for row in rows:
                values_list.append((
                    row['trade_date'],
                    row['stock_code'],
                    row['kline_time'],
                    row['open_price'],
                    row['close_price'],
                    row['high_price'],
                    row['low_price'],
                    row['volume'],
                    row['amount'],
                    row['change_pct'],
                    row['amplitude'],
                    row['turnover_rate'],
                    ts,
                ))

            cursor.executemany(sql, values_list)
            saved = cursor.rowcount
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"K线入库失败: {e}")
    finally:
        conn.close()
    return saved


# =====================================================================
# 逐笔成交明细
# =====================================================================

def fetch_tick_details(stock_code, market_type, max_pages=20):
    """
    获取逐笔成交明细(含集合竞价9:15起)
    分页获取, 每页约50条, pos=-1结束
    """
    secid = _secid(stock_code, market_type)
    all_ticks = []
    pos = 0

    for _ in range(max_pages):
        params = {
            'secid': secid,
            'fields1': 'f1,f2,f3,f4',
            'fields2': 'f51,f52,f53,f54,f55',
            'pos': pos,
            'iscca': 1,
            'invt': 2,
            'ut': 'fa5fd1943c7b386f172d6893dbfba10b',
        }
        data = _fetch_json(TICK_DETAIL_URL, params=params)
        if not data or not data.get('data'):
            break

        details = data['data'].get('details', [])
        if not details:
            break

        for d in details:
            parts = d.split(',')
            if len(parts) >= 5:
                all_ticks.append({
                    'tick_time': parts[0],       # HH:MM:SS
                    'price': _safe_float(parts[1]),
                    'volume': _safe_int(parts[2]),
                    'direction': _safe_int(parts[3], 0),  # 1买 2卖 4中性
                    'amount': _safe_float(parts[4]),
                })

        # 检查是否有更多数据
        new_pos = data['data'].get('pos', -1)
        if new_pos == -1 or new_pos == pos:
            break
        pos = new_pos
        time.sleep(0.1)

    return all_ticks


def save_tick_details(stock_code, ticks):
    """保存逐笔明细"""
    if not ticks:
        return 0

    conn = pymysql.connect(**DB_CONFIG)
    saved = 0
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO stock_tick_detail (
                    trade_date, stock_code, tick_time,
                    price, volume, direction, amount, data_timestamp
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
            """
            trade_date = datetime.now().strftime('%Y-%m-%d')
            ts = int(datetime.now().timestamp())
            values_list = []
            for t in ticks:
                values_list.append((
                    trade_date,
                    stock_code,
                    t['tick_time'],
                    t['price'],
                    t['volume'],
                    t['direction'],
                    t['amount'],
                    ts,
                ))

            cursor.executemany(sql, values_list)
            saved = cursor.rowcount
        conn.commit()
    except Exception as e:
        conn.rollback()
        _log(f"逐笔明细入库失败({stock_code}): {e}")
    finally:
        conn.close()
    return saved


# =====================================================================
# 获取监控股票列表
# =====================================================================

def get_kline_stocks():
    """获取需要拉K线的股票: 涨停池 + 竞价高开股 + 热门股"""
    stocks = []

    # 1. 从Redis获取涨停池股票
    if REDIS_AVAILABLE:
        zt_keys = rds.lrange(_redis_key("limit_up_stocks"), 0, -1)
        for k in zt_keys:
            if isinstance(k, bytes):
                k = k.decode()
            parts = k.split('.')
            if len(parts) == 2:
                stocks.append((parts[1], _safe_int(parts[0], 0)))

    # 2. 从Redis获取竞价高开股
    if REDIS_AVAILABLE:
        hot_keys = rds.lrange(_redis_key("auction_hot"), 0, 49)
        for k in hot_keys:
            if isinstance(k, bytes):
                k = k.decode()
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
    return unique[:100]


# =====================================================================
# 定时任务入口
# =====================================================================

def job_kline():
    """K线数据采集: 盘后拉取当天5分钟K线"""
    stocks = get_kline_stocks()
    if not stocks:
        _log("K线采集: 无监控标的，跳过")
        return

    _log(f"K线采集: 开始拉取 {len(stocks)} 只5分钟线...")
    total_saved = 0
    for code, mkt in stocks:
        klines = fetch_kline(code, mkt, klt=5, lmt=48)
        if klines:
            rows = []
            for k in klines:
                row = parse_kline_row(k, code)
                if row:
                    rows.append(row)
            saved = save_klines(rows)
            total_saved += saved
        time.sleep(0.3)

    _log(f"K线采集: 入库 {total_saved} 条")


def job_tick_detail():
    """逐笔明细采集: 对关键股票拉取全天逐笔(含竞价)"""
    stocks = get_kline_stocks()
    if not stocks:
        _log("逐笔明细: 无监控标的，跳过")
        return

    # 逐笔量大，只取前30只
    stocks = stocks[:30]
    _log(f"逐笔明细: 开始采集 {len(stocks)} 只...")
    total_saved = 0
    for code, mkt in stocks:
        ticks = fetch_tick_details(code, mkt, max_pages=200)
        if ticks:
            saved = save_tick_details(code, ticks)
            total_saved += saved
        time.sleep(0.5)

    _log(f"逐笔明细: 入库 {total_saved} 条")
