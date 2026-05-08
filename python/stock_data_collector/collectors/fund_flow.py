"""
模块6: 个股资金流向 —— 感知主力资金动向

支持Redis分布式多机协调采集, 无Redis时降级为单机模式
接口: push2.eastmoney.com/api/qt/clist/get (与竞价共用基础URL)
"""

import json
import time
import math
import pymysql
from datetime import datetime, timedelta

from config import (
    DB_CONFIG, REDIS_AVAILABLE, rds,
    FUND_FLOW_URL, FUND_FLOW_FIELDS, FUND_FLOW_FS,
    FUND_FLOW_PAGE_SIZE, FUND_FLOW_MAX_RETRIES,
    FUND_FLOW_RETRY_BACKOFF, FUND_FLOW_PAGE_SLEEP,
    FUND_FLOW_RATE_LIMIT_COOLDOWN,
)
from utils import (
    _log, redis_key, _build_headers, _build_session, clean_value,
)

MODULE_PREFIX = "fund_flow"


# =====================================================================
# Redis 分布式协调
# =====================================================================

def _rkey(suffix):
    """资金流向专用Redis key"""
    return redis_key(MODULE_PREFIX, suffix)


def claim_page():
    """从Redis领取一个页码：优先失败重试队列，否则原子递增新页码
    返回 (page_number, is_retry) 或 (None, False) 表示无任务"""
    key_failed = _rkey("failed")
    key_next = _rkey("next_page")
    key_done = _rkey("done")
    key_finished = _rkey("finished")
    key_max = _rkey("max_pages")

    # 0. 获取已知总页数上限（由第一个成功响应计算得出）
    max_pages = rds.get(key_max)
    max_pages = int(max_pages) if max_pages else None

    # 1. 优先从失败队列领取重试页（原子弹出）
    retry_page = rds.lpop(key_failed)
    if retry_page:
        page = int(retry_page)
        # 超出总页数的失败页直接丢弃
        if max_pages and page > max_pages:
            pass
        elif not rds.sismember(key_done, str(page)):
            _log(f"领取重试页: {page}")
            return page, True

    # 2. 检查是否所有页已抓完
    if rds.exists(key_finished):
        return None, False

    # 3. 原子递增领取新页码（从1开始）
    page = rds.incr(key_next)

    # 跳过已完成的页
    while rds.sismember(key_done, str(page)):
        page = rds.incr(key_next)

    rds.expire(key_next, 900)

    # 4. 超出总页数 → 标记完成
    if max_pages and page > max_pages:
        mark_all_finished()
        return None, False

    _log(f"领取新页: {page}")
    return page, False


def mark_page_done(page, item_count):
    """标记页码为已完成，累加入库数"""
    rds.sadd(_rkey("done"), str(page))
    rds.expire(_rkey("done"), 900)
    rds.incrby(_rkey("total_saved"), item_count)
    rds.expire(_rkey("total_saved"), 900)


def mark_page_failed(page):
    """将失败页放回重试队列"""
    rds.rpush(_rkey("failed"), str(page))
    rds.expire(_rkey("failed"), 900)
    _log(f"第 {page} 页放入重试队列")


def mark_all_finished():
    """标记所有页面已抓完，2分钟后自动清理Redis"""
    key = _rkey("finished")
    if not rds.exists(key):
        rds.set(key, "1", ex=120)
        for suffix in ["next_page", "done", "failed", "total_saved", "max_pages"]:
            k = _rkey(suffix)
            if rds.exists(k):
                rds.expire(k, 120)
        _log("标记所有页面已抓完，Redis将在2分钟后自动清理")


def get_global_stats():
    """获取全局统计：已完成页数、总入库数"""
    done_count = rds.scard(_rkey("done"))
    total_saved = int(rds.get(_rkey("total_saved")) or 0)
    failed_count = rds.llen(_rkey("failed"))
    return done_count, total_saved, failed_count


def _cleanup_redis():
    """清理当日Redis状态"""
    for suffix in ["next_page", "done", "failed", "total_saved", "finished", "max_pages"]:
        rds.delete(_rkey(suffix))
    _log("已清理Redis状态，下一轮可重新采集")


def _cleanup_yesterday_redis():
    """清理昨日的Redis残留key"""
    yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
    for suffix in ["next_page", "done", "failed", "total_saved", "finished", "max_pages"]:
        key = f"{MODULE_PREFIX}:{yesterday}:{suffix}"
        rds.delete(key)
    _log(f"已清理昨日({yesterday})Redis残留key")


# =====================================================================
# 数据抓取
# =====================================================================

def fetch_page(page):
    """抓取单页数据，返回 (items, is_last_page) 或 None 表示失败"""
    url = (
        f"{FUND_FLOW_URL}?"
        f"po=1&pz={FUND_FLOW_PAGE_SIZE}&pn={page}&np=1&fltt=2&invt=2"
        f"&fs={FUND_FLOW_FS}"
        f"&fields={FUND_FLOW_FIELDS}"
    )

    for attempt in range(1, FUND_FLOW_MAX_RETRIES + 1):
        session = _build_session()
        headers = _build_headers()
        try:
            response = session.get(url, headers=headers, timeout=15)

            if response.status_code in (502, 503, 504):
                wait = FUND_FLOW_RETRY_BACKOFF * (2 ** attempt)
                _log(f"第 {page} 页 HTTP {response.status_code}，{wait}s 后重试...")
                time.sleep(wait)
                continue

            response.raise_for_status()

            text = response.text
            if not text or len(text.strip()) < 10:
                wait = FUND_FLOW_RETRY_BACKOFF * (2 ** attempt)
                _log(f"第 {page} 页空响应，{wait}s 后重试...")
                time.sleep(wait)
                continue

            data = json.loads(text)
            data_node = data.get('data')

            if data_node is None or not data_node.get('diff'):
                total_records = data_node.get('total', 0) if data_node else 0
                if total_records > 0:
                    known_max = rds.get(_rkey("max_pages")) if REDIS_AVAILABLE else None
                    if not known_max and REDIS_AVAILABLE:
                        page_size = 100
                        max_pages = math.ceil(total_records / page_size)
                        rds.set(_rkey("max_pages"), str(max_pages), ex=900)
                    _log(f"第 {page} 页无数据，API报告总量 {total_records}，已超出末页")
                    return [], True
                else:
                    if REDIS_AVAILABLE:
                        done_count = rds.scard(_rkey("done"))
                        if done_count > 0:
                            return [], True
                    wait = FUND_FLOW_RETRY_BACKOFF * (2 ** attempt)
                    _log(f"第 {page} 页空数据，疑似限速，{wait}s 后重试...")
                    time.sleep(wait)
                    continue

            items = data_node.get('diff', [])
            total_records = data_node.get('total', 0)
            if total_records > 0 and len(items) > 0 and REDIS_AVAILABLE:
                actual_page_size = len(items)
                max_pages = math.ceil(total_records / actual_page_size)
                if rds.setnx(_rkey("max_pages"), str(max_pages)):
                    rds.expire(_rkey("max_pages"), 900)
                    _log(f"API报告总量: {total_records} 条，实际每页: {actual_page_size} 条，总页数: {max_pages}")
            _log(f"第 {page} 页获取 {len(items)} 条 ")
            return items, False

        except (json.JSONDecodeError, ValueError) as e:
            wait = FUND_FLOW_RETRY_BACKOFF * (2 ** attempt)
            _log(f"第 {page} 页第 {attempt}/{FUND_FLOW_MAX_RETRIES} 次解析失败: {type(e).__name__}，{wait}s 后重试...")
            time.sleep(wait)
        except Exception as e:
            wait = FUND_FLOW_RETRY_BACKOFF * (2 ** attempt)
            _log(f"第 {page} 页第 {attempt}/{FUND_FLOW_MAX_RETRIES} 次失败: {type(e).__name__}，{wait}s 后重试...")
            time.sleep(wait)
        finally:
            session.close()

    return None  # 全部重试失败


# =====================================================================
# 入库
# =====================================================================

def save_to_db(items):
    """将数据保存到 MySQL，存在则全量更新"""
    if not items:
        return

    connection = pymysql.connect(**DB_CONFIG)
    try:
        with connection.cursor() as cursor:
            sql = """
                INSERT INTO stock_fund_flow (
                    trade_date, stock_code, stock_name, market_type,
                    latest_price, change_percent, total_amount,
                    main_net_inflow, main_inflow_ratio,
                    super_large_inflow, super_large_abs_inflow, super_large_outflow, super_large_ratio,
                    large_abs_inflow, large_abs_outflow, large_inflow, large_ratio,
                    medium_abs_inflow, medium_abs_outflow, medium_inflow, medium_ratio,
                    small_abs_inflow, small_abs_outflow, small_inflow, small_ratio,
                    data_timestamp
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                ) ON DUPLICATE KEY UPDATE
                    stock_name = VALUES(stock_name),
                    latest_price = VALUES(latest_price),
                    change_percent = VALUES(change_percent),
                    total_amount = VALUES(total_amount),
                    main_net_inflow = VALUES(main_net_inflow),
                    main_inflow_ratio = VALUES(main_inflow_ratio),
                    super_large_inflow = VALUES(super_large_inflow),
                    super_large_abs_inflow = VALUES(super_large_abs_inflow),
                    super_large_outflow = VALUES(super_large_outflow),
                    super_large_ratio = VALUES(super_large_ratio),
                    large_abs_inflow = VALUES(large_abs_inflow),
                    large_abs_outflow = VALUES(large_abs_outflow),
                    large_inflow = VALUES(large_inflow),
                    large_ratio = VALUES(large_ratio),
                    medium_abs_inflow = VALUES(medium_abs_inflow),
                    medium_abs_outflow = VALUES(medium_abs_outflow),
                    medium_inflow = VALUES(medium_inflow),
                    medium_ratio = VALUES(medium_ratio),
                    small_abs_inflow = VALUES(small_abs_inflow),
                    small_abs_outflow = VALUES(small_abs_outflow),
                    small_inflow = VALUES(small_inflow),
                    small_ratio = VALUES(small_ratio),
                    data_timestamp = VALUES(data_timestamp),
                    updated_at = CURRENT_TIMESTAMP
            """

            values_list = []
            for item in items:
                ts = clean_value(item.get('f124'))
                trade_date = datetime.fromtimestamp(ts).strftime('%Y-%m-%d') if ts else datetime.now().strftime('%Y-%m-%d')

                values = (
                    trade_date,
                    clean_value(item.get('f12')),   # stock_code
                    clean_value(item.get('f14')),   # stock_name
                    clean_value(item.get('f13')),   # market_type
                    clean_value(item.get('f2')),    # latest_price
                    clean_value(item.get('f3')),    # change_percent
                    clean_value(item.get('f6')),    # total_amount
                    clean_value(item.get('f62')),   # main_net_inflow
                    clean_value(item.get('f184')),  # main_inflow_ratio
                    clean_value(item.get('f66')),   # super_large_inflow
                    clean_value(item.get('f64')),   # super_large_abs_inflow
                    clean_value(item.get('f65')),   # super_large_outflow
                    clean_value(item.get('f69')),   # super_large_ratio
                    clean_value(item.get('f70')),   # large_abs_inflow
                    clean_value(item.get('f71')),   # large_abs_outflow
                    clean_value(item.get('f72')),   # large_inflow
                    clean_value(item.get('f75')),   # large_ratio
                    clean_value(item.get('f76')),   # medium_abs_inflow
                    clean_value(item.get('f77')),   # medium_abs_outflow
                    clean_value(item.get('f78')),   # medium_inflow
                    clean_value(item.get('f81')),   # medium_ratio
                    clean_value(item.get('f82')),   # small_abs_inflow
                    clean_value(item.get('f83')),   # small_abs_outflow
                    clean_value(item.get('f84')),   # small_inflow
                    clean_value(item.get('f87')),   # small_ratio
                    ts                              # data_timestamp
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)

        connection.commit()
    except Exception as e:
        connection.rollback()
        _log(f"资金流向入库失败: {e}")
    finally:
        connection.close()


# =====================================================================
# 采集流程
# =====================================================================

def fetch_and_save_standalone():
    """单机顺序采集（无Redis时的降级模式）"""
    local_saved = 0
    page = 1
    consecutive_fails = 0

    _log("单机模式采集（Redis不可用）...")

    while True:
        result = fetch_page(page)

        if result is None:
            consecutive_fails += 1
            if consecutive_fails >= 3:
                _log(f"连续 {consecutive_fails} 页失败，冷却 {FUND_FLOW_RATE_LIMIT_COOLDOWN}s...")
                time.sleep(FUND_FLOW_RATE_LIMIT_COOLDOWN)
                consecutive_fails = 0
            page += 1
            continue

        items, is_last_page = result

        if is_last_page:
            if items:
                save_to_db(items)
                local_saved += len(items)
            _log(f"全部完成！共 {page} 页，入库 {local_saved} 条。")
            return

        save_to_db(items)
        local_saved += len(items)
        consecutive_fails = 0
        _log(f"第 {page} 页 {len(items)} 条已入库，累计 {local_saved} 条。")
        page += 1
        time.sleep(FUND_FLOW_PAGE_SLEEP)


def fetch_and_save():
    """分布式逐页采集（Redis协调多机）"""
    if not REDIS_AVAILABLE:
        fetch_and_save_standalone()
        return

    local_saved = 0
    consecutive_fails = 0
    start_time = time.time()

    _log("开始分布式采集任务...")

    while True:
        page, is_retry = claim_page()

        if page is None:
            failed_count = rds.llen(_rkey("failed"))
            if failed_count > 0:
                _log(f"新页已全部抓完，还有 {failed_count} 个失败页待重试...")
                time.sleep(3)
                continue

            done_count, total_saved, _ = get_global_stats()
            _log(f"所有页面已抓完！全局统计: {done_count} 页, {total_saved} 条。本机入库: {local_saved} 条。")
            return

        tag = "重试" if is_retry else "新页"
        result = fetch_page(page)

        if result is None:
            mark_page_failed(page)
            consecutive_fails += 1
            if consecutive_fails >= 3:
                _log(f"连续 {consecutive_fails} 页失败，冷却 {FUND_FLOW_RATE_LIMIT_COOLDOWN}s...")
                time.sleep(FUND_FLOW_RATE_LIMIT_COOLDOWN)
                consecutive_fails = 0
            continue

        items, is_last_page = result

        if is_last_page:
            mark_all_finished()
            if items:
                save_to_db(items)
                mark_page_done(page, len(items))
                local_saved += len(items)
            done_count, total_saved, failed_count = get_global_stats()
            if failed_count > 0:
                _log(f"到达末页，还有 {failed_count} 个失败页待重试...")
                continue
            _log(f"全部完成！全局: {done_count} 页, {total_saved} 条。本机: {local_saved} 条。")
            return

        save_to_db(items)
        mark_page_done(page, len(items))
        local_saved += len(items)
        consecutive_fails = 0

        done_count, total_saved, failed_count = get_global_stats()
        max_pages = rds.get(_rkey("max_pages"))
        max_pages = int(max_pages) if max_pages else 0
        progress = f"{done_count}/{max_pages}" if max_pages else f"{done_count}/?"
        pct = f"{done_count*100//max_pages}%" if max_pages else "?%"
        elapsed = int(time.time() - start_time)
        speed = f"{local_saved/(elapsed/60):.0f}" if elapsed > 60 else "-"
        eta_min = f"{(max_pages - done_count) * (elapsed / done_count) / 60:.0f}" if done_count > 0 and max_pages else "?"
        sample = f"{items[0].get('f12','')} {items[0].get('f14','')}" if items else ""
        _log(f"第{page}页({tag}) {len(items)}条入库 [{sample}...] 进度{progress}({pct}) 本机{local_saved}条 全局{total_saved}条 速度{speed}条/分 ETA~{eta_min}min 失败{failed_count}")

        time.sleep(FUND_FLOW_PAGE_SLEEP)


# =====================================================================
# 定时任务入口
# =====================================================================

def job_fund_flow():
    """资金流向采集任务"""
    _log("开始采集个股资金流向...")

    # 清理Redis残留
    if REDIS_AVAILABLE:
        _cleanup_yesterday_redis()
        if rds.exists(_rkey("finished")):
            _cleanup_redis()
            _log("检测到上一轮已完成，已清理Redis状态，开始新一轮采集")

    fetch_and_save()
