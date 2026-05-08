7个文件全部语法检查通过。拆分完成：

文件	行数	职责
market_smart_config.py	~230	DB/Redis/API配置 + 建表SQL
market_smart_utils.py	~100	工具函数(_log, _fetch_json, init_db等)
module_auction.py	~140	模块1：集合竞价 + 异动检测
module_tick.py	~130	模块2：五档盘口快照
module_sentiment.py	~230	模块3：涨跌停池 + 情绪指标
module_dragon_tiger.py	~170	模块4：龙虎榜 + 席位明细
fetch_market_smart.py	~110	主入口 + 定时调度 + 首次全量采集
首次启动逻辑：run_all_once() 按顺序执行 竞价→情绪→盘口→龙虎榜，每个模块间隔2秒，异常不中断后续模块，完成后进入定时调度模式。