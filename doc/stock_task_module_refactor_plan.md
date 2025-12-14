# 股票任务模块细化拆分与优化方案

> 针对以下类：
> - IFinanceTaskServiceImpl
> - IRealTimeStockServiceImpl
> - IStockTaskServiceImpl
> - RealTimeServiceImpl
> - StockETFrocessor
> - StockKlineBuilder
> - StockKlineProcessor
> - StockKlineRepositoryService
> - StockKlineRetryFetcher
> - StockKlineTaskExecutor
> - StockMarketResolver
> - StockWatchProcessor
> - SysJobLogServiceImpl
> - SysJobServiceImpl
> - WatchStockUpdater
>
> 本文进行模块化拆分、职责分层与重构建议，便于代码可维护性、扩展性与测试隔离。

---

# 1. 核心问题总结

1. **抓取逻辑散落在多个 Service/Processor/Updater 中**，存在多套 retry、sleep、风控节流策略。
2. **StockKlineProcessor 与 StockKlineTaskExecutor 均承担数据抓取 + 解析 + 分发 + 落库职责**，边界模糊。
3. **WatchStockProcessor / StockETFrocessor / RealTimeServiceImpl / IRealTimeStockServiceImpl** 有类似业务语义，属于不同数据体系，但抓取模式重复。
4. **SysJobLogServiceImpl / SysJobServiceImpl / TaskMonitoringService** 职责交叉，需要更清晰的事件中心模型。
5. **StockMarketResolver 粒度过低**，不应存在散落判断，应变成统一的工具或“领域对象属性”。
6. **Repository 与 Processor 混合过深**，Processor 不应该决定写库策略（队列 or 批量），而是交给 RepositoryService。

---

# 2. 最终目标架构

```
Quartz Job  →  TaskExecutor  →  DomainProcessor  →  DomainRepository
                                 |                  |
                                 |                  +--- 批量 insert/update
                                 +--- DomainFetch → RetryPolicy
```

```
↘ DomainFetch  =  StockFetchService
                      + RetryPolicy
                      + RateLimiter
                      + CookieManager
```

> 所有抓取业务（实时、自选、ETF、日级、分钟级）必须依赖同一抓取基础服务，不允许单独写重试、sleep。

---

# 3. 按类拆分与重构建议

## 3.1 IFinanceTaskServiceImpl

### 当前问题
- 任务执行、日志、调度、业务接口混合
- 可能只是调用 Stock 模块或 Quartz

### 优化方向
- 只保留「金融任务调度入口」：发起 Quartz Job 或 TaskExecutor
- 不直接做抓取、解析、写库
- 属于 **Application Service**

### 重构后职责
```
public interface FinanceTaskService
    → schedule finance related jobs
    → query finance job status
```

> 任何与数据抓取、处理的逻辑必须从这里移除

---

## 3.2 IRealTimeStockServiceImpl / RealTimeServiceImpl

### 当前问题
- 实时行情抓取逻辑与 StockWatchProcessor 交叉
- 可能存在独立 retry / sleep

### 优化方向
- **不允许直接请求东财**，必须走 StockFetchService
- 业务语义：实时行情 vs 定时扫描 vs ETF = 三种 Processor

### 重构结构
```
RealTimeServiceImpl: 实时行情业务封装
StockWatchProcessor: 自选股行情业务封装
StockETFProcessor: ETF 行情业务封装

三者调用：StockFetchService.fetchRealtime(stockCode)
```

> fetch 层必须统一，不允许三个模块各写一个 fetch

---

## 3.3 IStockTaskServiceImpl

### 当前问题
- stock task 管理、K 线抓取、写库混合

### 优化方向
- 单一职责：任务调度、查询 task 表、批量执行 task
- 不实现抓取与 K 线构建

> 将抓取全部迁移至 StockKlineTaskExecutor

---

## 3.4 StockETFrocessor

### 当前问题
- ETF Processor 与 WatchStockProcessor/RealTimeServiceImpl 存在结构重复
- 抓取方式与股票行情本质一致

### 优化方向
- ETFProcessor 只负责：
  - ETF 专用指标计算
  - ETF 特有数据拆解
  - ETF 序列化为 ETFEntity

> 抓取与 retry 不允许在这里写

---

## 3.5 StockKlineBuilder

### 当前问题
- builder + parseTradeDate + BigDecimal safeParse OK
- 但它不应该做 fetch 与 queue

### 优化
- 维持领域构建（DTO → DO）
- **脱离 Processor 调用**

> Processor 调用 builder，但 builder 永远不持有 fetch/repository

---

## 3.6 StockKlineProcessor

### 当前问题
- 同时负责：fetch + parse + build + 分发 insert/update
- 原有全局队列逻辑 + 新 ProcessResult 逻辑并存

### 终态
```
class StockKlineProcessor {
   ProcessResult execute(stockTask)
       → 调用 StockFetchService.fetchDaily/trends
       → builder.build
       → 解析成 insertList / updateList
       → return ProcessResult
}
```
- 不再持有：GLOBAL_HISTORY_QUEUE / GLOBAL_TODAY_QUEUE
- 不再管理：batchInsert、batchUpdate

> batch 写库移至 StockKlineRepositoryService

---

## 3.7 StockKlineRepositoryService

### 当前问题
- 写库、exists、批量 insert/update 多处调用
- Processor 和 TaskExecutor 都能直接调写库

### 最佳拆分
```
DomainRepository API：
   batchInsert(List)
   batchUpdate(List)
   exists(stockCode, dates)

Transaction Script：
   persist(ProcessResult)
```

- Processor 不允许直接写库
- TaskExecutor 负责「聚合 ProcessResult 并 commit」

---

## 3.8 StockKlineRetryFetcher

### 当前问题
- fetchWithRetry 内部包含 retry + sleep
- 与 RealTime / WatchStock / ETF 重复

### 替代方案
```
StockFetchService
   → RetryPolicy (maxRetry, retryDelay)
   → RateLimiter (per-second/per-stock)
   → CookieManager
```

### 移除类：StockKlineRetryFetcher
- 迁移至通用 StockFetchService

---

## 3.9 StockKlineTaskExecutor

### 当前问题
- 并发、收集、解析、写库全部混合

### 重构方案
```
StockKlineTaskExecutor
   run(tasks):
      for each task parallel
         result = processor.execute(task)
      aggregate all results
      repository.persist(results)
```

### 注意
- executor 不 fetch
- executor 不 build
- executor 不 parse

---

## 3.10 StockMarketResolver

### 当前问题
- if(code startsWith) 判断散落在多个类

### 终态
```
DomainValue: MarketCode
   SH / SZ / HK / ETF
```
- builder/processor 不判断 stock code
- stockcode 构造时即可得 market
- 或建：StockIdentity(stockCode, market)

---

## 3.11 StockWatchProcessor

### 当前问题
- Watch 与 RealTime 与 ETF 的抓取逻辑冗余

### 最终职责
```
StockWatchProcessor
   execute watch business
   → 调用 StockFetchService.fetchRealtime
```
- 不解析、不 retry、不 rateLimit

---

## 3.12 WatchStockUpdater

### 当前问题
- Updater 与 WatchProcessor 功能重复

### 建议
- 将 Updater 行为合并进 WatchProcessor 或 Executor 层
- 不再存在两套 API

---

## 3.13 SysJobServiceImpl / SysJobLogServiceImpl

### 当前问题
- job 配置与 job 执行日志跨多服务

### 最终拆分
```
SysJobService
   manage job config
   manage schedule

SysJobLogService
   write execution log
   query logs

TaskMonitoringService (可选)
   基于 log 的统计视图与告警
```

---

# 4. 基础抽象建议

## 4.1 抓取层统一
```
StockFetchService
   fetchRealtime()
   fetchDaily()
   fetchTrends()
   fetchETF()

内部：
   RetryPolicy
   RateLimiter
   CookieManager
   HttpClient/PythonInvoker
```

### 所有抓取模块必须依赖它，不允许外部直接 fetch

---

## 4.2 处理层统一
```
StockKlineProcessor
StockWatchProcessor
StockETFProcessor
```
- Processor 为 **纯业务转换单元**
- 不处理 fetch，不写库

---

## 4.3 执行层统一
```
StockKlineTaskExecutor
WatchTaskExecutor
ETFTaskExecutor
```
- Executor 负责 orchestrate：并发 + 汇总 + 调 repository

> Processor = 业务转换
> Executor = 聚合落库

---

# 5. 重构优先级

> 按最小风险 + 最大收益排序

1) **抽象出 StockFetchService（取代所有 retry fetch）**
2) **收敛 Processor → Executor → Repository 三层职责**
3) **删除 GLOBAL 队列版逻辑**
4) **Job 层所有类变成 AbstractQuartzJob 子类 + Executor 调用**
5) **Service 层去除包装型/跨界实现**
6) **MarketResolver 映射入 StockIdentity**

---

# 6. 重构完成后的结构图

```
Quartz Job
   ↓
TaskExecutor
   ↓
DomainProcessor
   ↓
ProcessResult (insert/update)
   ↓
Repository.persist(result)
```

```
DomainProcessor
   ↓
StockFetchService
   + RetryPolicy
   + RateLimiter
   + CookieManager
```

---

# 7. 重构输出物

---

# 8. Quartz / Scheduler 层任务类细化拆分与优化

本节针对以下调度类：

- DatabaseBackupTask
- FixedTimeTask
- GoldViewTask
- RealTimeTask
- RyTask
- ScheduledTask
- SupperTask
- SysJobLogCleanupTask

它们目前存在以下共同问题：

## 8.1 当前共同问题

1. **Job 类中包含太多业务逻辑**：调度类执行 SQL、调用 Service、发起抓取、落库、异常处理等都混合在一起。
2. **缺乏统一的任务执行框架**：每类 Job 自己执行，不复用：
   - 日志处理
   - 锁机制
   - 超时控制
   - 异常通知
3. **Quartz Job = 调度行为 + 业务行为混合**：违反单一职责，难以测试。
4. **部分 Job 与 Executor 或 Processor 逻辑重复**。

---

## 8.2 重构整体思路

所有 Quartz Job 类必须遵循统一模式：

```
QuartzJob  -------->  TaskExecutor
                     (统一执行框架)
                          ↓
                     DomainProcessor
                          ↓
                     DomainRepository
```

即：
- Job **不负责业务逻辑，只负责调度与参数传递**
- Job **不负责写库、不负责抓取、不负责重试**
- Job **所有业务能力来自 Executor**

---

## 8.3 统一的抽象任务模型

### 引入：`AbstractScheduledTask`

```
public abstract class AbstractScheduledTask extends QuartzJobBean {

    @Resource TaskExecutor executor;
    @Resource SysJobLogService logService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        try {
            executor.run(buildTask(context));
            logService.success(taskInfo);
        } catch (Exception e) {
            logService.error(taskInfo, e);
        }
    }

    protected abstract DomainTask buildTask(JobExecutionContext ctx);
}
```

> 核心：Quartz Job 只负责“构建 DomainTask + 丢给 Executor”

---

# 9. 各任务类的细化拆分方案

## 9.1 DatabaseBackupTask

### 原问题
- Job 类内部执行备份脚本、异常处理、日志记录等

### 重构方案
```
QuartzJob: DatabaseBackupTask → 构建 BackupDomainTask
Executor: DatabaseBackupExecutor → 执行脚本/超时/重试
Repository: BackupLogRepository
```

### 优势
- Job 类不会有逻辑
- executor 可复用、可单元测试

---

## 9.2 FixedTimeTask

### 职责定位
- 固定频率执行的数据任务：如每日 02:00 执行

### 拆分
- 不包含任何业务代码，只交给 Executor

---

## 9.3 GoldViewTask

### 当前问题
- 自己抓取黄金行情
- 自己落库
- 自己 sleep

### 重构
```
QuartzJob: GoldViewTask
   ↓
DomainTask: FetchGoldPriceTask
   ↓
GoldProcessor → 调用 FetchService
GoldRepository → batch write
```

### 注意
- 禁止 GoldViewTask 自己 retry / sleep / http

---

## 9.4 RealTimeTask

### 当前问题
- 调用实时股票行情、落库

### 重构
```
QuartzJob → DomainTask: RealTimeStockTask
Processor: StockRealtimeProcessor
Executor: RealTimeTaskExecutor
Repository: StockRealtimeRepository
```

---

## 9.5 RyTask / ScheduledTask / SupperTask

### 共同问题
- 多个 Job 类结构重复
- 存在内部 if/else 分发不同业务 Task

### 重构方式
**不要多个 Job 类，同时存在多个业务分支**

引入：`DomainTaskType + TaskPipeline`

```
QuartzJob: GenericScheduledJob
   ↓
TaskType = GOLD / REALTIME / KLINE / BACKUP
   ↓
Executor.selectProcessor(TaskType)
```

> Job 类数量减少 70%，复用性提升

---

## 9.6 SysJobLogCleanupTask

### 当前问题
- cleanup job 自己执行 SQL，自己落库

### 优化
```
DomainTask: LogCleanupTask
Executor: CleanupExecutor
Repository: SysJobLogRepository
QuartzJob: SysJobLogCleanupTask
```

QuartzJob 不执行 SQL

---

# 10. Quartz 调度层最终结构

```
job/
   GenericScheduledJob.java      ← 唯一通用调度类
   SysJobLogCleanupJob.java     ← 系统内部特例

executor/
   KlineTaskExecutor
   BackupTaskExecutor
   RealTimeTaskExecutor
   GoldTaskExecutor
   CleanupTaskExecutor

processor/
   StockKlineProcessor
   StockRealtimeProcessor
   GoldProcessor

repository/
   StockKlineRepository
   StockRealtimeRepository
   BackupRepository
   JobLogRepository
```

---

# 11. 重构后的关键收益

1. Job 层极其轻量，只有 5% 业务代码
2. Executor 可被单测，支持并发、rate limit、事务
3. Processor 专注数据处理
4. Repository 专注持久化
5. **去除冗余：重复任务类：RyTask、SupperTask、ScheduledTask → 删除或合并**
6. GoldViewTask / RealTimeTask / KlineTask → 不直接抓取


- 删除：StockKlineRetryFetcher, WatchStockUpdater
- 简化：SysJobServiceImpl, SysJobLogServiceImpl
- 拆分：IStockTaskServiceImpl, RealTimeServiceImpl
- 分离：StockKlineTaskExecutor & StockKlineProcessor
- 引入：
  - StockFetchService
  - RetryPolicy
  - RateLimiter
  - DomainRepository.persist(result)
  - StockIdentity
```

