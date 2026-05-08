# 股票数据采集系统 (stock_data_collector)

东方财富API数据采集，支持多机分布式协调。

## 模块

| 模块 | 文件 | 调度 | 说明 |
|------|------|------|------|
| 1 | `collectors/auction.py` | 9:15-9:25 每30s | 集合竞价快照 + 异动检测 |
| 2 | `collectors/tick.py` | 盘中每15s | 五档盘口快照 |
| 3 | `collectors/sentiment.py` | 盘中每5min | 涨跌停池 + 情绪指标 |
| 4 | `collectors/dragon_tiger.py` | 18:00 | 龙虎榜 + 席位明细 |
| 5 | `collectors/kline.py` | 15:30/15:35 | 5分钟K线 + 逐笔明细 |
| 6 | `collectors/fund_flow.py` | 盘中每10min | 个股资金流向(支持分布式) |

## 用法

```bash
pip install -r requirements.txt
python main.py
```

## 目录结构

```
stock_data_collector/
├── main.py                # 主入口
├── config.py              # 统一配置 (DB/Redis/API/建表SQL)
├── utils.py               # 通用工具函数
├── requirements.txt
├── README.md
└── collectors/
    ├── __init__.py
    ├── auction.py         # 集合竞价
    ├── tick.py            # 五档盘口
    ├── sentiment.py       # 情绪指标
    ├── dragon_tiger.py    # 龙虎榜
    ├── kline.py           # K线/逐笔
    └── fund_flow.py       # 资金流向
```

## 依赖

- MySQL (pymysql)
- Redis (可选，资金流向模块支持多机分布式协调)
- 东方财富公开API

## 配置

编辑 `config.py` 中的 `DB_CONFIG` 和 `REDIS_CONFIG`。
