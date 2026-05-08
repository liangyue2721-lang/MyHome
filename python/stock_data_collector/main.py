"""
股票数据采集系统 - 主入口

模块1: 集合竞价全息数据 (9:15-9:25)
模块2: 五档盘口快照数据 (盘中)
模块3: 市场全局情绪指标
模块4: 龙虎榜与异动公告 (盘后)
模块5: K线数据 + 逐笔成交明细 (盘后/全天)
模块6: 个股资金流向 (盘中10分钟)

用法: python main.py
"""

import time
import schedule
from datetime import datetime

from utils import _log, init_db
from collectors.auction import job_auction
from collectors.tick import job_tick
from collectors.sentiment import job_sentiment
from collectors.dragon_tiger import job_dragon_tiger
from collectors.kline import job_kline, job_tick_detail
from collectors.fund_flow import job_fund_flow


# =====================================================================
# 定时调度任务 (交易时间守卫)
# =====================================================================

def _is_weekday():
    return datetime.now().weekday() < 5


def _current_time():
    return datetime.now().strftime("%H:%M")


def job_auction_loop():
    """集合竞价循环采集: 9:15-9:25每30秒一次"""
    if not _is_weekday():
        return
    t = _current_time()
    if "09:15" <= t <= "09:25":
        job_auction()


def job_tick_loop():
    """盘中盘口循环采集: 9:30-11:35 / 13:00-15:05"""
    if not _is_weekday():
        return
    t = _current_time()
    if ("09:30" <= t <= "11:35") or ("13:00" <= t <= "15:05"):
        job_tick()


def job_sentiment_loop():
    """市场情绪循环采集: 盘中每5分钟"""
    if not _is_weekday():
        return
    t = _current_time()
    if ("09:25" <= t <= "11:35") or ("13:00" <= t <= "15:05"):
        job_sentiment()


def job_fund_flow_loop():
    """资金流向循环采集: 盘中每10分钟"""
    if not _is_weekday():
        return
    t = _current_time()
    if ("09:25" <= t <= "11:35") or ("13:00" <= t <= "16:00"):
        job_fund_flow()


def job_kline_loop():
    """K线盘后采集: 15:30拉取当天5分钟线"""
    if not _is_weekday():
        return
    job_kline()


def job_tick_detail_loop():
    """逐笔明细盘后采集: 15:35拉取全天逐笔"""
    if not _is_weekday():
        return
    job_tick_detail()


def job_dragon_tiger_loop():
    """龙虎榜盘后采集: 18:00执行"""
    if not _is_weekday():
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
        ("资金流向", job_fund_flow),
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

    _log("股票数据采集系统启动...")

    # 首次启动: 全量采集一次
    run_all_once()

    # 注册定时任务
    schedule.every(30).seconds.do(job_auction_loop)
    schedule.every(15).seconds.do(job_tick_loop)
    schedule.every(5).minutes.do(job_sentiment_loop)
    schedule.every(10).minutes.do(job_fund_flow_loop)
    schedule.every().day.at("15:30").do(job_kline_loop)
    schedule.every().day.at("15:35").do(job_tick_detail_loop)
    schedule.every().day.at("18:00").do(job_dragon_tiger_loop)

    _log("定时任务已注册:")
    _log("  竞价30s / 盘口15s / 情绪5min / 资金流向10min")
    _log("  K线15:30 / 逐笔15:35 / 龙虎榜18:00")

    while True:
        schedule.run_pending()
        time.sleep(1)


if __name__ == "__main__":
    main()
