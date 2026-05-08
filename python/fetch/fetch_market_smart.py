"""
市场智能感知数据采集 - 主入口

模块1: 集合竞价全息数据 (9:15-9:25)
模块2: 五档盘口快照数据 (盘中)
模块3: 市场全局情绪指标
模块4: 龙虎榜与异动公告 (盘后)
模块5: K线数据 + 逐笔成交明细 (盘后/全天)

首次启动时自动执行一次全量采集，之后按定时调度运行。
"""

import time
import schedule
from datetime import datetime

from market_smart_utils import _log, init_db
from module_auction import job_auction
from module_tick import job_tick
from module_sentiment import job_sentiment
from module_dragon_tiger import job_dragon_tiger
from module_kline import job_kline, job_tick_detail


# =====================================================================
# 定时调度任务
# =====================================================================

def job_auction_loop():
    """集合竞价循环采集: 9:15-9:25每30秒一次"""
    now = datetime.now()
    current_time = now.strftime("%H:%M")

    if now.weekday() >= 5:
        return

    if not ("09:15" <= current_time <= "09:25"):
        return

    job_auction()


def job_tick_loop():
    """盘中盘口循环采集: 9:30-11:35 / 13:00-15:05"""
    now = datetime.now()
    current_time = now.strftime("%H:%M")

    if now.weekday() >= 5:
        return

    is_morning = "09:30" <= current_time <= "11:35"
    is_afternoon = "13:00" <= current_time <= "15:05"

    if not (is_morning or is_afternoon):
        return

    job_tick()


def job_sentiment_loop():
    """市场情绪循环采集: 盘中每5分钟"""
    now = datetime.now()
    current_time = now.strftime("%H:%M")

    if now.weekday() >= 5:
        return

    is_trading = ("09:25" <= current_time <= "11:35") or ("13:00" <= current_time <= "15:05")
    if not is_trading:
        return

    job_sentiment()


def job_kline_loop():
    """K线盘后采集: 15:30拉取当天5分钟线"""
    now = datetime.now()
    if now.weekday() >= 5:
        return
    job_kline()


def job_tick_detail_loop():
    """逐笔明细盘后采集: 15:35拉取全天逐笔"""
    now = datetime.now()
    if now.weekday() >= 5:
        return
    job_tick_detail()


def job_dragon_tiger_loop():
    """龙虎榜盘后采集: 18:00执行"""
    now = datetime.now()
    if now.weekday() >= 5:
        return
    job_dragon_tiger()


# =====================================================================
# 首次启动全量采集
# =====================================================================

def run_all_once():
    """首次启动时执行所有模块一次，用于验证脚本和补充数据"""
    _log("=" * 50)
    _log("首次启动，执行全量采集验证...")
    _log("=" * 50)

    modules = [
        ("集合竞价", job_auction),
        ("市场情绪", job_sentiment),
        ("盘口快照", job_tick),
        ("K线数据", job_kline),
        ("逐笔明细", job_tick_detail),
        ("龙虎榜", job_dragon_tiger),
    ]

    for name, func in modules:
        _log(f"--- 验证采集: {name} ---")
        try:
            func()
        except Exception as e:
            _log(f"验证采集 {name} 异常: {type(e).__name__}: {e}")
        time.sleep(2)

    _log("全量采集验证完成，进入定时调度模式")


# =====================================================================
# 主入口
# =====================================================================

def main():
    """主入口: 初始化 + 首次全量采集 + 注册定时任务"""
    # 初始化数据库表
    init_db()

    _log("市场智能感知系统启动...")

    # 首次启动: 全量采集一次
    run_all_once()

    # 注册定时任务
    schedule.every(30).seconds.do(job_auction_loop)
    schedule.every(15).seconds.do(job_tick_loop)
    schedule.every(5).minutes.do(job_sentiment_loop)
    schedule.every().day.at("15:30").do(job_kline_loop)
    schedule.every().day.at("15:35").do(job_tick_detail_loop)
    schedule.every().day.at("18:00").do(job_dragon_tiger_loop)

    _log("定时任务已注册: 竞价30s/盘口15s/情绪5min/K线15:30/逐笔15:35/龙虎榜18:00")

    while True:
        schedule.run_pending()
        time.sleep(1)


if __name__ == "__main__":
    main()
