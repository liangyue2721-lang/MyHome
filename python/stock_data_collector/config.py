"""
股票数据采集系统 - 统一配置
包含: 数据库/Redis/API接口/伪装/建表SQL
"""

import socket
import redis

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
}

# ================= Redis配置 =================
REDIS_CONFIG = {
    'host': '192.168.0.100',
    'port': 6379,
    'db': 0,
    'decode_responses': True,
}
rds = redis.Redis(**REDIS_CONFIG)
REDIS_AVAILABLE = False
try:
    rds.ping()
    REDIS_AVAILABLE = True
except Exception:
    pass

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

# ================= API接口地址 =================
# 集合竞价 (clist endpoint)
AUCTION_LIST_URL = "https://push2.eastmoney.com/api/qt/clist/get"
AUCTION_FIELDS = "f12,f14,f13,f2,f3,f6,f17,f18,f86,f87,f88"
AUCTION_FS = "m:0+t:6+f:!2,m:0+t:13+f:!2,m:0+t:80+f:!2,m:1+t:2+f:!2,m:1+t:23+f:!2,m:0+t:7+f:!2,m:1+t:3+f:!2"

# 五档盘口 (stock/get endpoint, 字段含义与clist不同)
STOCK_SNAPSHOT_URL = "https://push2.eastmoney.com/api/qt/stock/get"
SNAPSHOT_FIELDS = (
    # 基础: 最新价/最高/最低/今开/昨收/涨跌幅/成交量/成交额/量比/代码/名称/市场
    "f43,f44,f45,f46,f60,f71,f47,f48,f50,f57,f58,f107,"
    # 买一~买五(价,量): f19/f20, f17/f18, f15/f16, f13/f14, f11/f12
    "f19,f20,f17,f18,f15,f16,f13,f14,f11,f12,"
    # 卖一~卖五(价,量): f39/f40, f37/f38, f35/f36, f33/f34, f31/f32
    "f39,f40,f37,f38,f35,f36,f33,f34,f31,f32,"
    # 委比/换手率/内外盘
    "f161,f168,f49,f531"
)

# 涨跌停/连板
LIMIT_UP_URL = "https://push2ex.eastmoney.com/getTopicZTPool"
LIMIT_DOWN_URL = "https://push2ex.eastmoney.com/getTopicDTPool"
CONSECUTIVE_BOARD_URL = "https://push2ex.eastmoney.com/getTopicQSPool"
YESTERDAY_ZT_URL = "https://push2ex.eastmoney.com/getYesterdayZTPool"
LIMIT_POOL_PARAMS = {
    'ut': '7eea3edcaed734bea9cbfc24409ed989',
    'dpt': 'wz.ztzt',
    'Pageindex': 0,
    'pagesize': 500,
    'sort': 'fbt:asc',  # 必填! 按封板时间升序; 连板池用 zdp:desc 覆盖
}

# K线/逐笔明细
KLINE_HISTORY_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get"
TICK_DETAIL_URL = "https://push2.eastmoney.com/api/qt/stock/details/get"

# 龙虎榜
DRAGON_TIGER_URL = "https://datacenter-web.eastmoney.com/api/data/v1/get"
DRAGON_TIGER_DETAIL_URL = "https://datacenter-web.eastmoney.com/api/data/v1/get"

# 资金流向 (clist endpoint, 与竞价共用基础URL)
FUND_FLOW_URL = "https://push2.eastmoney.com/api/qt/clist/get"
FUND_FLOW_FIELDS = "f12,f14,f13,f2,f3,f6,f62,f184,f64,f65,f66,f69,f70,f71,f72,f75,f76,f77,f78,f81,f82,f83,f84,f87,f124"
FUND_FLOW_FS = "m:0+t:6+f:!2,m:0+t:13+f:!2,m:0+t:80+f:!2,m:1+t:2+f:!2,m:1+t:23+f:!2,m:0+t:7+f:!2,m:1+t:3+f:!2"

# ================= 资金流向特有常量 =================
FUND_FLOW_PAGE_SIZE = 500
FUND_FLOW_MAX_RETRIES = 3
FUND_FLOW_RETRY_BACKOFF = 2
FUND_FLOW_PAGE_SLEEP = 8
FUND_FLOW_RATE_LIMIT_COOLDOWN = 60

# ================= 建表SQL =================
CREATE_TABLE_SQL = {
    'auction': """
        CREATE TABLE IF NOT EXISTS stock_auction_snapshot (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            stock_name VARCHAR(20) NOT NULL COMMENT '股票名称',
            market_type INT DEFAULT 0 COMMENT '市场类型: 0深圳 1上海',
            auction_price DECIMAL(10,2) DEFAULT NULL COMMENT '竞价价格',
            auction_volume BIGINT DEFAULT NULL COMMENT '竞价量(手)',
            auction_amount DECIMAL(18,2) DEFAULT NULL COMMENT '竞价金额(元)',
            auction_change_pct DECIMAL(10,2) DEFAULT NULL COMMENT '竞价涨跌幅%',
            prev_close DECIMAL(10,2) DEFAULT NULL COMMENT '昨日收盘价',
            auction_open_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '高开幅度%(竞价价相对昨收)',
            unmatched_buy BIGINT DEFAULT NULL COMMENT '未匹配买量(手)',
            unmatched_sell BIGINT DEFAULT NULL COMMENT '未匹配卖量(手)',
            auction_time TIME DEFAULT NULL COMMENT '采集时刻(HH:MM:SS)',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            UNIQUE KEY uk_auction (trade_date, stock_code, auction_time),
            INDEX idx_date (trade_date),
            INDEX idx_code (stock_code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集合竞价快照'
    """,
    'bid_ask': """
        CREATE TABLE IF NOT EXISTS stock_bid_ask_snapshot (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            stock_name VARCHAR(20) NOT NULL COMMENT '股票名称',
            market_type INT DEFAULT 0 COMMENT '市场类型: 0深圳 1上海',
            latest_price DECIMAL(10,2) DEFAULT NULL COMMENT '最新价',
            change_pct DECIMAL(10,2) DEFAULT NULL COMMENT '涨跌幅%',
            buy1_price DECIMAL(10,2) DEFAULT NULL COMMENT '买一价',
            buy1_vol INT DEFAULT NULL COMMENT '买一量(手)',
            buy2_price DECIMAL(10,2) DEFAULT NULL COMMENT '买二价',
            buy2_vol INT DEFAULT NULL COMMENT '买二量(手)',
            buy3_price DECIMAL(10,2) DEFAULT NULL COMMENT '买三价',
            buy3_vol INT DEFAULT NULL COMMENT '买三量(手)',
            buy4_price DECIMAL(10,2) DEFAULT NULL COMMENT '买四价',
            buy4_vol INT DEFAULT NULL COMMENT '买四量(手)',
            buy5_price DECIMAL(10,2) DEFAULT NULL COMMENT '买五价',
            buy5_vol INT DEFAULT NULL COMMENT '买五量(手)',
            sell1_price DECIMAL(10,2) DEFAULT NULL COMMENT '卖一价',
            sell1_vol INT DEFAULT NULL COMMENT '卖一量(手)',
            sell2_price DECIMAL(10,2) DEFAULT NULL COMMENT '卖二价',
            sell2_vol INT DEFAULT NULL COMMENT '卖二量(手)',
            sell3_price DECIMAL(10,2) DEFAULT NULL COMMENT '卖三价',
            sell3_vol INT DEFAULT NULL COMMENT '卖三量(手)',
            sell4_price DECIMAL(10,2) DEFAULT NULL COMMENT '卖四价',
            sell4_vol INT DEFAULT NULL COMMENT '卖四量(手)',
            sell5_price DECIMAL(10,2) DEFAULT NULL COMMENT '卖五价',
            sell5_vol INT DEFAULT NULL COMMENT '卖五量(手)',
            total_volume BIGINT DEFAULT NULL COMMENT '成交量(手)',
            total_amount DECIMAL(18,2) DEFAULT NULL COMMENT '成交额(元)',
            bid_ask_imbalance DECIMAL(10,4) DEFAULT NULL COMMENT '委比(买量-卖量)/(买量+卖量)',
            snapshot_time TIME DEFAULT NULL COMMENT '快照时刻(HH:MM:SS)',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            INDEX idx_date (trade_date),
            INDEX idx_code (stock_code),
            INDEX idx_time (trade_date, snapshot_time)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='五档盘口快照'
    """,
    'limit_pool': """
        CREATE TABLE IF NOT EXISTS stock_limit_pool (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            stock_name VARCHAR(20) NOT NULL COMMENT '股票名称',
            market_type INT DEFAULT 0 COMMENT '市场类型: 0深圳 1上海',
            limit_type VARCHAR(4) NOT NULL COMMENT '涨跌停类型: UP涨停/DOWN跌停',
            latest_price DECIMAL(10,2) DEFAULT NULL COMMENT '涨停价/跌停价',
            change_pct DECIMAL(10,2) DEFAULT NULL COMMENT '涨跌幅%',
            consecutive_boards INT DEFAULT 0 COMMENT '连板数(1=首板 2=二连板...)',
            seal_amount DECIMAL(18,2) DEFAULT NULL COMMENT '封单金额(万元)',
            first_limit_time TIME DEFAULT NULL COMMENT '首次封板时间',
            last_limit_time TIME DEFAULT NULL COMMENT '最后封板时间',
            open_times INT DEFAULT 0 COMMENT '开板次数(0=未开板)',
            turnover_rate DECIMAL(10,2) DEFAULT NULL COMMENT '换手率%',
            industry VARCHAR(20) DEFAULT NULL COMMENT '所属行业板块',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            UNIQUE KEY uk_limit (trade_date, stock_code, limit_type),
            INDEX idx_date (trade_date),
            INDEX idx_boards (trade_date, consecutive_boards),
            INDEX idx_type (trade_date, limit_type)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='涨跌停股池'
    """,
    'sentiment': """
        CREATE TABLE IF NOT EXISTS stock_market_sentiment (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            snapshot_time TIME NOT NULL COMMENT '快照时刻(HH:MM:SS)',
            limit_up_count INT DEFAULT 0 COMMENT '涨停家数',
            limit_down_count INT DEFAULT 0 COMMENT '跌停家数',
            limit_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '涨跌停比(涨停/跌停)',
            max_consecutive_boards INT DEFAULT 0 COMMENT '市场最高连板数',
            board2_progress_rate DECIMAL(10,2) DEFAULT NULL COMMENT '2进3晋级率%',
            board3_progress_rate DECIMAL(10,2) DEFAULT NULL COMMENT '3进4晋级率%',
            yesterday_limit_up_open_pct DECIMAL(10,2) DEFAULT NULL COMMENT '昨日涨停池今开平均溢价%',
            yesterday_limit_up_avg_change DECIMAL(10,2) DEFAULT NULL COMMENT '昨日涨停池今日平均涨幅%',
            sentiment_score DECIMAL(10,2) DEFAULT NULL COMMENT '情绪评分(0-100, 50为中性)',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            UNIQUE KEY uk_sentiment (trade_date, snapshot_time),
            INDEX idx_date (trade_date)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场情绪指标'
    """,
    'dragon_tiger': """
        CREATE TABLE IF NOT EXISTS stock_dragon_tiger (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            stock_name VARCHAR(20) NOT NULL COMMENT '股票名称',
            change_pct DECIMAL(10,2) DEFAULT NULL COMMENT '涨跌幅%',
            reason VARCHAR(100) DEFAULT NULL COMMENT '上榜原因(涨幅偏离/换手率/振幅等)',
            buy_amount DECIMAL(18,2) DEFAULT NULL COMMENT '买入总额(万元)',
            sell_amount DECIMAL(18,2) DEFAULT NULL COMMENT '卖出总额(万元)',
            net_amount DECIMAL(18,2) DEFAULT NULL COMMENT '净买入(万元)',
            north_net_buy DECIMAL(18,2) DEFAULT NULL COMMENT '沪股通/深股通净买入(万元)',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            UNIQUE KEY uk_dragon (trade_date, stock_code, reason),
            INDEX idx_date (trade_date),
            INDEX idx_code (stock_code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='龙虎榜'
    """,
    'dragon_tiger_detail': """
        CREATE TABLE IF NOT EXISTS stock_dragon_tiger_detail (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            stock_name VARCHAR(20) NOT NULL COMMENT '股票名称',
            seat_name VARCHAR(50) NOT NULL COMMENT '营业部名称',
            seat_code VARCHAR(20) DEFAULT NULL COMMENT '营业部代码',
            buy_amount DECIMAL(18,2) DEFAULT 0 COMMENT '买入金额(万元)',
            sell_amount DECIMAL(18,2) DEFAULT 0 COMMENT '卖出金额(万元)',
            net_amount DECIMAL(18,2) DEFAULT 0 COMMENT '净买入(万元)',
            is_institution TINYINT DEFAULT 0 COMMENT '席位类型: 0普通营业部 1机构专用 2沪股通/深股通',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            INDEX idx_date (trade_date),
            INDEX idx_code (stock_code),
            INDEX idx_seat (seat_name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='龙虎榜席位明细'
    """,
    'kline_5min': """
        CREATE TABLE IF NOT EXISTS stock_kline_5min (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            kline_time DATETIME NOT NULL COMMENT 'K线时间(YYYY-MM-DD HH:MM)',
            open_price DECIMAL(10,2) DEFAULT NULL COMMENT '开盘价',
            close_price DECIMAL(10,2) DEFAULT NULL COMMENT '收盘价',
            high_price DECIMAL(10,2) DEFAULT NULL COMMENT '最高价',
            low_price DECIMAL(10,2) DEFAULT NULL COMMENT '最低价',
            volume BIGINT DEFAULT NULL COMMENT '成交量(手)',
            amount DECIMAL(18,2) DEFAULT NULL COMMENT '成交额(元)',
            change_pct DECIMAL(10,2) DEFAULT NULL COMMENT '涨跌幅%',
            amplitude DECIMAL(10,2) DEFAULT NULL COMMENT '振幅%',
            turnover_rate DECIMAL(10,2) DEFAULT NULL COMMENT '换手率%',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            UNIQUE KEY uk_kline (trade_date, stock_code, kline_time),
            INDEX idx_date (trade_date),
            INDEX idx_code (stock_code),
            INDEX idx_time (kline_time)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='5分钟K线'
    """,
    'tick_detail': """
        CREATE TABLE IF NOT EXISTS stock_tick_detail (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            tick_time TIME NOT NULL COMMENT '成交时间(HH:MM:SS)',
            price DECIMAL(10,2) DEFAULT NULL COMMENT '成交价',
            volume INT DEFAULT NULL COMMENT '成交量(手)',
            direction TINYINT DEFAULT 0 COMMENT '方向: 1买盘 2卖盘 4中性盘',
            amount DECIMAL(18,2) DEFAULT NULL COMMENT '成交额(元)',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            INDEX idx_date_code (trade_date, stock_code),
            INDEX idx_time (trade_date, tick_time),
            INDEX idx_code (stock_code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐笔成交明细'
    """,
    'fund_flow': """
        CREATE TABLE IF NOT EXISTS stock_fund_flow (
            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
            trade_date DATE NOT NULL COMMENT '交易日期',
            stock_code VARCHAR(10) NOT NULL COMMENT '股票代码',
            stock_name VARCHAR(20) DEFAULT NULL COMMENT '股票名称',
            market_type INT DEFAULT 0 COMMENT '市场类型: 0深圳 1上海',
            latest_price DECIMAL(10,2) DEFAULT NULL COMMENT '最新价',
            change_percent DECIMAL(10,2) DEFAULT NULL COMMENT '涨跌幅%',
            total_amount DECIMAL(18,2) DEFAULT NULL COMMENT '成交额(元)',
            main_net_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '主力净流入(元)',
            main_inflow_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '主力净占比%',
            super_large_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '超大单净流入(元)',
            super_large_abs_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '超大单流入(元)',
            super_large_outflow DECIMAL(18,2) DEFAULT NULL COMMENT '超大单流出(元)',
            super_large_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '超大单净占比%',
            large_abs_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '大单流入(元)',
            large_abs_outflow DECIMAL(18,2) DEFAULT NULL COMMENT '大单流出(元)',
            large_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '大单净流入(元)',
            large_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '大单净占比%',
            medium_abs_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '中单流入(元)',
            medium_abs_outflow DECIMAL(18,2) DEFAULT NULL COMMENT '中单流出(元)',
            medium_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '中单净流入(元)',
            medium_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '中单净占比%',
            small_abs_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '小单流入(元)',
            small_abs_outflow DECIMAL(18,2) DEFAULT NULL COMMENT '小单流出(元)',
            small_inflow DECIMAL(18,2) DEFAULT NULL COMMENT '小单净流入(元)',
            small_ratio DECIMAL(10,2) DEFAULT NULL COMMENT '小单净占比%',
            data_timestamp BIGINT DEFAULT NULL COMMENT '数据时间戳(unix)',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            UNIQUE KEY uk_fund_flow (trade_date, stock_code),
            INDEX idx_date (trade_date),
            INDEX idx_code (stock_code)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个股资金流向'
    """,
}
