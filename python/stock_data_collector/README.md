# 股票数据采集系统 (stock_data_collector)

基于东方财富公开API的A股实时数据采集，覆盖竞价/盘口/情绪/龙虎榜/K线/资金流向六大维度，支持Redis多机分布式协调。

## 快速开始

```bash
pip install -r requirements.txt
python main.py
```

启动流程：初始化建表 → 首次全量采集验证 → 进入定时调度循环

## 目录结构

```
stock_data_collector/
├── main.py                # 主入口：初始化 + 首次验证 + 定时调度
├── config.py              # 统一配置：DB/Redis/API/伪装/建表SQL
├── utils.py               # 通用工具：HTTP/日志/Redis key/安全转换/建表
├── requirements.txt
├── README.md
└── collectors/
    ├── __init__.py         # 包入口，导出所有 job_* 函数
    ├── auction.py          # 模块1: 集合竞价
    ├── tick.py             # 模块2: 五档盘口
    ├── sentiment.py        # 模块3: 市场情绪
    ├── dragon_tiger.py     # 模块4: 龙虎榜
    ├── kline.py            # 模块5: K线 + 逐笔
    └── fund_flow.py        # 模块6: 资金流向
```

## 模块详情

### 模块1: 集合竞价 `collectors/auction.py`

- **调度**: 09:15-09:25 每30秒
- **API**: `push2.eastmoney.com/api/qt/clist/get`
- **入库表**: `stock_auction_snapshot`
- **采集**: 竞价价格/量/额、涨跌幅、高开幅度、未匹配买卖量
- **异动检测**: 竞价高开>3% 或 量比>5 标记异动，写入Redis供盘口模块消费

### 模块2: 五档盘口 `collectors/tick.py`

- **调度**: 09:30-11:35 / 13:00-15:05 每15秒
- **API**: `push2.eastmoney.com/api/qt/stock/get`
- **入库表**: `stock_bid_ask_snapshot`
- **采集**: 最新价、买卖五档价量、成交量/额、委比、换手率
- **监控标的**: 涨停池股票 + 竞价异动股（从Redis读取）

### 模块3: 市场情绪 `collectors/sentiment.py`

- **调度**: 09:25-11:35 / 13:00-15:05 每5分钟
- **API**: 涨停池 `getTopicZTPool` / 跌停池 `getTopicDTPool` / 连板池 `getTopicQSPool` / 昨日涨停 `getYesterdayZTPool`
- **入库表**: `stock_limit_pool`（涨跌停明细）、`stock_market_sentiment`（情绪汇总）
- **计算指标**: 涨停/跌停家数、涨跌停比、最高连板数、2进3/3进4晋级率、昨日涨停今开溢价、情绪评分(0-100)

> **重要**: 涨跌停/连板池API **必须携带 `sort` 参数**，否则返回 `rc:102` 空数据：
> - 涨停池: `sort=fbt:asc`
> - 跌停池: `sort=fund:asc`
> - 连板池: `sort=zdp:desc`

### 模块4: 龙虎榜 `collectors/dragon_tiger.py`

- **调度**: 每日 18:00
- **API**: `datacenter-web.eastmoney.com/api/data/v1/get`
- **入库表**: `stock_dragon_tiger`（榜单）、`stock_dragon_tiger_detail`（席位明细）
- **采集**: 上榜股票/原因/买卖总额/净买入、席位明细（营业部/机构/北向）

### 模块5: K线 + 逐笔 `collectors/kline.py`

- **调度**: K线 15:30 / 逐笔 15:35（盘后）
- **API**: `push2his.eastmoney.com/api/qt/stock/kline/get`（K线）、`push2.eastmoney.com/api/qt/stock/details/get`（逐笔）
- **入库表**: `stock_kline_5min`、`stock_tick_detail`
- **采集**: 5分钟OHLCV+涨跌幅/振幅/换手率；逐笔成交含集合竞价(iscca=1)
- **监控标的**: 涨停池+竞价异动股（K线≤100只，逐笔≤30只）

### 模块6: 资金流向 `collectors/fund_flow.py`

- **调度**: 09:25-11:35 / 13:00-16:00 每10分钟
- **API**: `push2.eastmoney.com/api/qt/clist/get`
- **入库表**: `stock_fund_flow`（ON DUPLICATE KEY UPDATE）
- **采集**: 主力/超大单/大单/中单/小单 净流入+流入+流出+净占比
- **分布式**: Redis协调多机分页采集，无Redis自动降级单机顺序模式

## 调度时间线

```
09:15 ═══════════════════ 竞价采集(30s)
09:25 ────┬─────────────── 情绪采集(5min)
09:30 ────┼═══════════════ 盘口采集(15s)
         │         ┌───── 资金流向(10min)
11:35 ────┴─────────┘
13:00 ────┬─────────┬───── 情绪+盘口+资金 继续采集
15:05 ────┴─────────┘
15:30 ──────────────────── K线采集
15:35 ──────────────────── 逐笔明细采集
18:00 ──────────────────── 龙虎榜采集
```

## 数据库表

| 表名 | 模块 | 唯一键 | 说明 |
|------|------|--------|------|
| `stock_auction_snapshot` | 竞价 | (date, code, time) | 每股每竞价时刻一条 |
| `stock_bid_ask_snapshot` | 盘口 | — | 全量插入，无唯一键 |
| `stock_limit_pool` | 情绪 | (date, code, type) | UP涨停/DOWN跌停 |
| `stock_market_sentiment` | 情绪 | (date, time) | 全市场情绪汇总 |
| `stock_dragon_tiger` | 龙虎榜 | (date, code, reason) | 上榜记录 |
| `stock_dragon_tiger_detail` | 龙虎榜 | — | 席位买卖明细 |
| `stock_kline_5min` | K线 | (date, code, time) | 5分钟K线 |
| `stock_tick_detail` | 逐笔 | — | 逐笔成交(含竞价) |
| `stock_fund_flow` | 资金 | (date, code) | UPSERT更新 |

所有表由 `init_db()` 启动时自动创建，SQL定义在 `config.py` 的 `CREATE_TABLE_SQL`。

## 配置

编辑 `config.py`：

```python
DB_CONFIG = {
    'host': '192.168.1.139', 'port': 3306,
    'user': 'root', 'password': '***',
    'database': 'make-vue', 'charset': 'utf8mb4',
}
REDIS_CONFIG = {
    'host': '192.168.0.100', 'port': 6379, 'db': 0,
}
```

### Redis用途

| 模块 | 用途 | Key格式 |
|------|------|---------|
| 竞价 | 异动股列表 | `market_smart:{date}:auction_hot` |
| 盘口 | 监控标的 | `market_smart:{date}:limit_up_stocks` |
| 资金流向 | 分布式分页协调 | `fund_flow:{date}:next_page/done/failed/...` |

Redis不可用时，竞价/盘口降级为空监控列表，资金流向降级为单机顺序采集。

## 分布式部署

资金流向模块支持多机并行：所有机器连同一Redis → 各自运行 `python main.py` → Redis自动协调分页 → 失败页自动重试 → 完成后key自动过期清理。其他模块多机运行会重复采集但唯一键去重不影响正确性。

## API接口汇总

| 接口 | 域名 | 模块 | 备注 |
|------|------|------|------|
| `/api/qt/clist/get` | push2 | 竞价/资金 | 列表分页 |
| `/api/qt/stock/get` | push2 | 盘口 | 单股查询，需secid |
| `/getTopicZTPool` | push2ex | 情绪 | **需sort=fbt:asc** |
| `/getTopicDTPool` | push2ex | 情绪 | **需sort=fund:asc** |
| `/getTopicQSPool` | push2ex | 情绪 | **需sort=zdp:desc** |
| `/getYesterdayZTPool` | push2ex | 情绪 | 昨日涨停 |
| `/api/qt/stock/kline/get` | push2his | K线 | 支持1/5/15/30/60min |
| `/api/qt/stock/details/get` | push2 | 逐笔 | iscca=1含竞价 |
| `/api/data/v1/get` | datacenter-web | 龙虎榜 | 报表接口 |

## 依赖

| 包 | 版本 | 用途 |
|----|------|------|
| `requests` | ≥2.28 | HTTP请求 |
| `urllib3` | ≥1.26 | 重试策略 |
| `pymysql` | ≥1.1 | MySQL连接 |
| `redis` | ≥4.5 | 分布式协调(可选) |
| `schedule` | ≥1.2 | 定时调度 |

## 常见问题

**Q: 涨停池/跌停池返回空数据(rc:102)?**
A: API已强制要求`sort`参数。确认`LIMIT_POOL_PARAMS`包含sort，且各池排序值正确(见模块3说明)。

**Q: 资金流向采集慢?**
A: 默认每页间隔8秒(`FUND_FLOW_PAGE_SLEEP`)，可调低。多机部署可显著加速。

**Q: 盘口监控股票少?**
A: 盘口只监控涨停池+竞价异动股，确保竞价模块先运行写入Redis。

**Q: 只运行单个模块?**
```python
import sys; sys.path.insert(0, '.')
from collectors.sentiment import job_sentiment
job_sentiment()
```
