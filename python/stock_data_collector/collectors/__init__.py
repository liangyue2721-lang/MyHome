"""
数据采集模块包
"""

from collectors.auction import job_auction
from collectors.tick import job_tick
from collectors.sentiment import job_sentiment
from collectors.dragon_tiger import job_dragon_tiger
from collectors.kline import job_kline, job_tick_detail
from collectors.fund_flow import job_fund_flow

__all__ = [
    'job_auction',
    'job_tick',
    'job_sentiment',
    'job_dragon_tiger',
    'job_kline',
    'job_tick_detail',
    'job_fund_flow',
]
