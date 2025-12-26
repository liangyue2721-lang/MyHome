# 脚本使用示例汇总

本文档汇总当前项目中各个脚本的**标准调用方式、参数说明以及典型用途**。所有脚本均遵循统一约定：

- **stdout 仅输出 JSON**（无多余日志）
- 适合被 Java / Go / Python 子进程直接调用
- 反爬策略已内置（Playwright 真实浏览器 + Cookie 预热）

---

## 1. kline_playwright.py

**用途**：
获取东方财富 **日级 K 线**，带 normalize 清洗与字段补齐。

### 调用方式
```bash
python kline_playwright.py [stock_code] [market]
```

### 示例
```bash
# 沪市 工业富联
python kline_playwright.py 601138 1

# 深市 平安银行
python kline_playwright.py 000001 0
```

### 输出
```json
[{
  "trade_date": "2024-12-20",
  "trade_time": "2024-12-20T00:00:00",
  "stock_code": "601138",
  "open": 12.35,
  "close": 12.48,
  "high": 12.62,
  "low": 12.30,
  "volume": 183456789,
  "amount": 2289456789.0,
  "pre_close": 12.35,
  "change": 0.13,
  "change_pct": 1.05,
  "turnover_ratio": 2.31
}]
```

---

## 2. eastmoney_kline_fetcher.py

**用途**：
Playwright 访问东财历史 K 线，支持多 URL 模式自动切换。

### 调用方式
```bash
python eastmoney_kline_fetcher.py
```

（脚本内可直接修改 stock_code / market）

---

## 3. eastmoney_kline_playwright_single_market.py

**用途**：
固定 market 的东财历史 K 线获取（不自动切换市场）。

### 调用方式
```bash
python eastmoney_kline_playwright_single_market.py <code> <market> <beg> <end>
```

### 示例
```bash
python eastmoney_kline_playwright_single_market.py 601138 1 20200101 20241231
```

---

## 4. hybrid_eastmoney_baidu_daily.py

**用途**：
多源兜底（日级）：
东财 trends2 → 百度分钟 → 新浪快照。

### 调用方式
```bash
python hybrid_eastmoney_baidu_daily.py <code> [ndays]
```

### 示例
```bash
python hybrid_eastmoney_baidu_daily.py 600000 5
```

---

## 5. hybrid_kline_trends.py

**用途**：
浏览器常驻 + 熔断机制的**分钟 → 日线聚合终极版**。

### 调用方式
```bash
python hybrid_kline_trends.py <secid> [ndays]
```

### 示例
```bash
python hybrid_kline_trends.py 1.600000 10
```

---

## 6. trends2_playwright_logger.py

**用途**：
趋势数据抓取 + 文件日志（stdout 保持纯 JSON）。

### 调用方式
```bash
python trends2_playwright_logger.py <code> [ndays]
```

---

## 7. realtime_fetcher.py

**用途**：
东财实时行情任意 URL → **标准 Java DTO JSON**。

### 调用方式
```bash
python realtime_fetcher.py <api_url>
```

### 示例
```bash
python realtime_fetcher.py "https://push2.eastmoney.com/api/qt/stock/get?..."
```

---

## 8. fetch_stock_realtime.py

**用途**：
股票实时行情抓取（标准字段映射）。

### 调用方式
```bash
python fetch_stock_realtime.py <api_url>
```

---

## 9. etf_realtime_fetcher.py

**用途**：
ETF 实时行情标准化输出。

### 调用方式
```bash
python etf_realtime_fetcher.py <api_url>
```

---

## 使用建议

- **历史日线**：优先 `kline_playwright.py`
- **分钟兜底聚合**：`hybrid_eastmoney_baidu_daily.py`
- **高可靠生产级**：`hybrid_kline_trends.py`
- **实时行情（Java）**：`realtime_fetcher.py / etf_realtime_fetcher.py`

---

如需：
- 自动生成 `Makefile`
- 统一 CLI 参数规范
- 输出 Schema / OpenAPI 定义

可以在此文档基础上继续扩展。

