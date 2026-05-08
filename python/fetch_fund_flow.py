import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import json
import time
import pymysql
import random
import socket
import redis
from datetime import datetime, timedelta
import schedule
import math

# ================= 请求配置 =================
MAX_RETRIES = 3          # 单页最大重试次数
RETRY_BACKOFF = 2        # 退避基数（秒），实际等待 = RETRY_BACKOFF * (2 ^ attempt)
PAGE_SLEEP = 8           # 翻页间隔（秒），3台分摊后每台约18页，8s足够安全
RATE_LIMIT_COOLDOWN = 60 # 遭遇连续限速后的冷却时间（秒）
IDLE_WAIT = 10           # 无任务时等待（秒），等其他机器产生失败页重试

# ================= 机器标识 =================
WORKER_ID = socket.gethostname()

# ================= 数据库配置 =================
DB_CONFIG = {
    'host': '192.168.1.139',
    'port': 3306,
    'user': 'root',
    'password': 'tomcatV123A1234C222',
    'database': 'make-vue',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

# ================= Redis配置 =================
REDIS_CONFIG = {
    'host': '192.168.0.100',
    'port': 6379,
    'db': 0,
    # 'password': 'aB3x9Rq7Z2pY8sK4nL6',
    'decode_responses': True,
}
rds = redis.Redis(**REDIS_CONFIG)
REDIS_AVAILABLE = False
try:
    rds.ping()
    REDIS_AVAILABLE = True
except Exception:
    pass  # Redis不可用时降级为单机模式

# ================= 伪装配置 =================
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
]

def _random_ip():
    """生成随机虚拟IP（国内常见网段）"""
    segments = [(110,120),(171,183),(202,223),(60,68),(116,123),(218,223)]
    seg = random.choice(segments)
    first = random.randint(seg[0], seg[1])
    return f"{first}.{random.randint(0,255)}.{random.randint(0,255)}.{random.randint(1,254)}"

def _build_headers():
    """构建请求头（伪造IP头已被CDN识别拒绝，改用简洁头）"""
    headers = {
        "User-Agent": random.choice(USER_AGENTS),
        "Accept": "*/*",
        "Referer": "https://data.eastmoney.com/",
    }
    return headers, ""

def _build_session():
    """构建 requests.Session（底层TCP重试3次，不重试HTTP状态码）"""
    session = requests.Session()
    retry_strategy = Retry(
        total=3,
        backoff_factor=0.5,
        status_forcelist=[],
        allowed_methods=["GET"],
    )
    adapter = HTTPAdapter(max_retries=retry_strategy, pool_connections=1, pool_maxsize=1)
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    return session

def clean_value(val):
    """处理接口返回的空值 '-' 为 None"""
    if val == "-" or val == "":
        return None
    return val

def _redis_key(suffix):
    """生成当日Redis key，格式: fund_flow:2026-05-07:suffix"""
    today = datetime.now().strftime('%Y-%m-%d')
    return f"fund_flow:{today}:{suffix}"

def _log(msg):
    """带时间戳和机器标识的日志"""
    ts = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    print(f"[{ts}][{WORKER_ID}] {msg}")

def claim_page():
    """从Redis领取一个页码：优先失败重试队列，否则原子递增新页码
    返回 (page_number, is_retry) 或 (None, False) 表示无任务"""
    key_failed = _redis_key("failed")
    key_next = _redis_key("next_page")
    key_done = _redis_key("done")
    key_finished = _redis_key("finished")
    key_max = _redis_key("max_pages")

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

    rds.expire(key_next, 900)  # 15分钟后过期

    # 4. 超出总页数 → 标记完成
    if max_pages and page > max_pages:
        mark_all_finished()
        return None, False

    _log(f"领取新页: {page}")
    return page, False

def mark_page_done(page, item_count):
    """标记页码为已完成，累加入库数"""
    rds.sadd(_redis_key("done"), str(page))
    rds.expire(_redis_key("done"), 900)  # 15分钟后过期
    rds.incrby(_redis_key("total_saved"), item_count)
    rds.expire(_redis_key("total_saved"), 900)  # 15分钟后过期

def mark_page_failed(page):
    """将失败页放回重试队列"""
    rds.rpush(_redis_key("failed"), str(page))
    rds.expire(_redis_key("failed"), 900)  # 15分钟后过期
    _log(f"第 {page} 页放入重试队列")

def mark_all_finished():
    """标记所有页面已抓完（第一台发现末页的机器设置），2分钟后自动清理Redis"""
    key = _redis_key("finished")
    if not rds.exists(key):
        rds.set(key, "1", ex=120)  # 2分钟后过期
        # 将所有协调key的TTL缩短为2分钟，给其他节点留出重试窗口后自动清理
        for suffix in ["next_page", "done", "failed", "total_saved", "max_pages"]:
            k = _redis_key(suffix)
            if rds.exists(k):
                rds.expire(k, 120)
        _log("标记所有页面已抓完，Redis将在2分钟后自动清理")

def get_global_stats():
    """获取全局统计：已完成页数、总入库数"""
    done_count = rds.scard(_redis_key("done"))
    total_saved = int(rds.get(_redis_key("total_saved")) or 0)
    failed_count = rds.llen(_redis_key("failed"))
    return done_count, total_saved, failed_count

def _cleanup_redis():
    """清理当日Redis状态，为下一轮采集做准备（由完成最后一页的机器调用）"""
    for suffix in ["next_page", "done", "failed", "total_saved", "finished", "max_pages"]:
        rds.delete(_redis_key(suffix))
    _log("已清理Redis状态，下一轮可重新采集")

def _cleanup_yesterday_redis():
    """清理昨日的Redis残留key，防止跨天累积"""
    yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
    for suffix in ["next_page", "done", "failed", "total_saved", "finished", "max_pages"]:
        key = f"fund_flow:{yesterday}:{suffix}"
        rds.delete(key)
    _log(f"已清理昨日({yesterday})Redis残留key")

def fetch_page(page):
    """抓取单页数据，返回 (items, is_last_page) 或 None 表示失败"""
    url = (
        f"https://push2.eastmoney.com/api/qt/clist/get?"
        f"po=1&pz=500&pn={page}&np=1&fltt=2&invt=2"
        f"&fs=m:0+t:6+f:!2,m:0+t:13+f:!2,m:0+t:80+f:!2,m:1+t:2+f:!2,m:1+t:23+f:!2,m:0+t:7+f:!2,m:1+t:3+f:!2"
        f"&fields=f12,f14,f13,f2,f3,f6,f62,f184,f64,f65,f66,f69,f70,f71,f72,f75,f76,f77,f78,f81,f82,f83,f84,f87,f124"
    )

    for attempt in range(1, MAX_RETRIES + 1):
        session = _build_session()
        fake_headers, fake_ip = _build_headers()
        try:
            response = session.get(url, headers=fake_headers, timeout=15)

            if response.status_code in (502, 503, 504):
                wait = RETRY_BACKOFF * (2 ** attempt)
                _log(f"第 {page} 页 HTTP {response.status_code}(IP:{fake_ip})，{wait}s 后换IP重试...")
                time.sleep(wait)
                continue

            response.raise_for_status()

            text = response.text
            if not text or len(text.strip()) < 10:
                wait = RETRY_BACKOFF * (2 ** attempt)
                _log(f"第 {page} 页空响应(IP:{fake_ip})，{wait}s 后换IP重试...")
                time.sleep(wait)
                continue

            data = json.loads(text)
            data_node = data.get('data')

            if data_node is None or not data_node.get('diff'):
                # 用 total 字段判断：有total说明API正常响应，该页确实无数据（末页）
                total_records = data_node.get('total', 0) if data_node else 0
                if total_records > 0:
                    # API正常但该页无数据 → 已超出末页，用已知page_size或默认100
                    known_max = rds.get(_redis_key("max_pages"))
                    if not known_max:
                        page_size = 100  # API实际每页返回100
                        max_pages = math.ceil(total_records / page_size)
                        rds.set(_redis_key("max_pages"), str(max_pages), ex=900)  # 15分钟后过期
                    _log(f"第 {page} 页无数据，API报告总量 {total_records}，已超出末页")
                    return [], True
                else:
                    # total=0 且 data 为空 → 可能限速，也可能真的没数据
                    done_count = rds.scard(_redis_key("done"))
                    if done_count > 0:
                        return [], True  # 已有成功页，说明是末页
                    # 第一页就空 → 限速
                    wait = RETRY_BACKOFF * (2 ** attempt)
                    _log(f"第 {page} 页空数据(IP:{fake_ip})，疑似限速，{wait}s 后重试...")
                    time.sleep(wait)
                    continue

            items = data_node.get('diff', [])
            # 从成功响应计算并存储总页数（用实际返回数量作为页大小）
            total_records = data_node.get('total', 0)
            if total_records > 0 and len(items) > 0:
                actual_page_size = len(items)
                max_pages = math.ceil(total_records / actual_page_size)
                # 仅第一次设置（setnx），后续不覆盖
                if rds.setnx(_redis_key("max_pages"), str(max_pages)):
                    rds.expire(_redis_key("max_pages"), 900)  # 15分钟后过期
                    _log(f"API报告总量: {total_records} 条，实际每页: {actual_page_size} 条，总页数: {max_pages}")
            _log(f"第 {page} 页获取 {len(items)} 条 ")
            return items, False

        except (requests.exceptions.ConnectionError,
                requests.exceptions.Timeout,
                requests.exceptions.ChunkedEncodingError) as e:
            wait = RETRY_BACKOFF * (2 ** attempt)
            _log(f"第 {page} 页第 {attempt}/{MAX_RETRIES} 次失败(IP:{fake_ip}): {type(e).__name__}，{wait}s 后换IP重试...")
            time.sleep(wait)
        except (json.JSONDecodeError, ValueError) as e:
            wait = RETRY_BACKOFF * (2 ** attempt)
            _log(f"第 {page} 页第 {attempt}/{MAX_RETRIES} 次解析失败(IP:{fake_ip}): {type(e).__name__}，{wait}s 后换IP重试...")
            time.sleep(wait)
        except Exception as e:
            _log(f"第 {page} 页异常(IP:{fake_ip}): {type(e).__name__}: {e}")
            time.sleep(5)
        finally:
            session.close()

    return None  # 全部重试失败

def fetch_and_save_standalone():
    """单机顺序采集（无Redis时的降级模式）：逐页抓取 → 即时入库"""
    local_saved = 0
    page = 1
    consecutive_fails = 0

    _log("单机模式采集（Redis不可用）...")

    while True:
        result = fetch_page(page)

        if result is None:
            consecutive_fails += 1
            if consecutive_fails >= 3:
                cooldown = RATE_LIMIT_COOLDOWN
                _log(f"连续 {consecutive_fails} 页失败，冷却 {cooldown}s...")
                time.sleep(cooldown)
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
        time.sleep(PAGE_SLEEP)

def fetch_and_save():
    """分布式逐页采集（Redis协调多机）：领页 → 抓取 → 入库 → 标记"""
    if not REDIS_AVAILABLE:
        fetch_and_save_standalone()
        return

    local_saved = 0
    consecutive_fails = 0
    idle_rounds = 0
    start_time = time.time()

    _log("开始分布式采集任务...")

    while True:
        # 领取页码
        page, is_retry = claim_page()

        if page is None:
            # 检查是否还有失败页待重试
            failed_count = rds.llen(_redis_key("failed"))
            if failed_count > 0:
                _log(f"新页已全部抓完，还有 {failed_count} 个失败页待重试...")
                time.sleep(3)
                continue

            # 全部完成（Redis状态由TTL自然过期，不主动清理避免影响其他节点）
            done_count, total_saved, _ = get_global_stats()
            _log(f"所有页面已抓完！全局统计: {done_count} 页, {total_saved} 条。本机入库: {local_saved} 条。")
            return

        # 抓取该页
        tag = "重试" if is_retry else "新页"
        result = fetch_page(page)

        if result is None:
            # 抓取失败，放回重试队列
            mark_page_failed(page)
            consecutive_fails += 1
            if consecutive_fails >= 3:
                cooldown = RATE_LIMIT_COOLDOWN
                _log(f"连续 {consecutive_fails} 页失败，冷却 {cooldown}s...")
                time.sleep(cooldown)
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

        # 正常入库
        save_to_db(items)
        mark_page_done(page, len(items))
        local_saved += len(items)
        consecutive_fails = 0

        done_count, total_saved, failed_count = get_global_stats()
        max_pages = rds.get(_redis_key("max_pages"))
        max_pages = int(max_pages) if max_pages else 0
        progress = f"{done_count}/{max_pages}" if max_pages else f"{done_count}/?"
        pct = f"{done_count*100//max_pages}%" if max_pages else "?%"
        elapsed = int(time.time() - start_time)
        speed = f"{local_saved/(elapsed/60):.0f}" if elapsed > 60 else "-"
        eta_min = f"{(max_pages - done_count) * (elapsed / done_count) / 60:.0f}" if done_count > 0 and max_pages else "?"
        sample = f"{items[0].get('f12','')} {items[0].get('f14','')}" if items else ""
        _log(f"第{page}页({tag}) {len(items)}条入库 [{sample}...] 进度{progress}({pct}) 本机{local_saved}条 全局{total_saved}条 速度{speed}条/分 ETA~{eta_min}min 失败{failed_count}")

        time.sleep(PAGE_SLEEP)

def save_to_db(items):
    """将数据保存到 MySQL，存在则全量更新"""
    if not items:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 没有获取到任何数据，取消入库。")
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
                    clean_value(item.get('f66')),   # super_large_inflow (超大单净流入，旧列保留)
                    clean_value(item.get('f64')),   # super_large_abs_inflow (超大单流入绝对值)
                    clean_value(item.get('f65')),   # super_large_outflow (超大单流出)
                    clean_value(item.get('f69')),   # super_large_ratio
                    clean_value(item.get('f70')),   # large_abs_inflow (大单流入绝对值)
                    clean_value(item.get('f71')),   # large_abs_outflow (大单流出绝对值)
                    clean_value(item.get('f72')),   # large_inflow (大单净流入，旧列保留)
                    clean_value(item.get('f75')),   # large_ratio
                    clean_value(item.get('f76')),   # medium_abs_inflow (中单流入绝对值)
                    clean_value(item.get('f77')),   # medium_abs_outflow (中单流出绝对值)
                    clean_value(item.get('f78')),   # medium_inflow (中单净流入，旧列保留)
                    clean_value(item.get('f81')),   # medium_ratio
                    clean_value(item.get('f82')),   # small_abs_inflow (小单流入绝对值)
                    clean_value(item.get('f83')),   # small_abs_outflow (小单流出绝对值)
                    clean_value(item.get('f84')),   # small_inflow (小单净流入，旧列保留)
                    clean_value(item.get('f87')),   # small_ratio
                    ts                              # data_timestamp
                )
                values_list.append(values)

            cursor.executemany(sql, values_list)

        connection.commit()
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 成功将 {len(items)} 条数据写入数据库！")

    except Exception as e:
        connection.rollback()
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 数据库操作失败: {e}")
    finally:
        connection.close()

def job():
    """定时任务主体：包含交易时间拦截逻辑"""
    now = datetime.now()
    current_time = now.strftime("%H:%M")
    time_str = now.strftime('%Y-%m-%d %H:%M:%S')

    # 1. 周末不抓取
    if now.weekday() >= 5:
        print(f"[{time_str}] 周末休市，跳过抓取。")
        return

    # 2. 定义A股交易时间段
    is_morning = "09:25" <= current_time <= "11:35"
    is_afternoon = "13:00" <= current_time <= "16:00"

    if not (is_morning or is_afternoon):
        print(f"[{time_str}] 当前非交易时间，跳过抓取。")
        return

    print(f"[{time_str}] 处于交易时间段，开始执行逐页采集+即时入库任务...")

    # 清理昨日Redis残留key
    if REDIS_AVAILABLE:
        _cleanup_yesterday_redis()

    # 逐页采集并即时入库
    fetch_and_save()

if __name__ == "__main__":
    job()

    # 每 10 分钟执行一次，配合 PAGE_SLEEP=15s/页 + 限速自适应退避
    schedule.every(10).minutes.do(job)

    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 采集服务已启动，设置间隔: 10 分钟。")

    while True:
        schedule.run_pending()
        time.sleep(1)
