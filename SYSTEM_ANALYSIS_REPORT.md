# Make Money 管理系统 - 系统分析报告

## 1. 项目背景与目标

**项目名称**：Make Money 管理系统
**核心目标**：打造一套集个人/企业财务管理、股票交易监控、工作流审批于一体的综合性企业级管理平台。

**解决的核心问题**：
1.  **资产分散管理难**：统一管理银行存款、理财、股票等多种资产。
2.  **股票数据实时性差**：解决传统爬虫难以获取动态渲染页面（如东方财富）实时数据的问题。
3.  **流程审批繁琐**：通过集成工作流引擎，规范化财务审批和业务流转。

**面向用户**：
-   **个人投资者**：监控股票账户、ETF 收益、存款规划。
-   **企业管理员**：管理部门、人员、审批流程（请假、报销等）。
-   **系统运维**：监控定时任务、服务器状态、日志审计。

---

## 2. 整体架构与模块说明

### 2.1 技术架构概览

本项目采用 **前后端分离** 的 **模块化单体 (Modular Monolith)** 架构。虽然代码库按业务分拆了模块，但在构建时主要打包为一个 Spring Boot 应用 (`money-admin` 作为入口)。

-   **前端**：Vue 2.6 + Element UI 2.15 + Vue-CLI (注意：项目 README 声称 Vue3，但实际 `package.json` 依赖为 Vue 2.x)。
-   **后端**：Spring Boot 2.5.15 + MyBatis Plus + Spring Security。
-   **中间件**：
    -   **Redis**：缓存、分布式锁、**自定义分布式消息队列** (核心)。
    -   **MySQL**：核心业务数据存储。
-   **外部服务**：
    -   **Python Service**：基于 FastAPI + Playwright 的独立微服务，用于抓取反爬虫严重的金融数据。

### 2.2 核心模块说明

| 模块名 | 职责说明 |
| :--- | :--- |
| **money-admin** | **系统入口**，聚合所有模块，提供 REST API 接口，处理 Web 请求路由。 |
| **money-framework** | **基础设施**，包含安全配置 (Security)、Web 配置、AOP 切面 (日志、幂等)、数据源配置。 |
| **money-quartz** | **任务调度核心**，不仅负责 Quartz 任务触发，还包含 **核心业务消费者 (Consumer)** 逻辑 (如 `StockTaskConsumer`)。 |
| **money-stock** | **股票业务域**，包含股票/ETF 实体、K线计算逻辑、策略算法。 |
| **money-finance** | **财务业务域**，包含资产记录、贷款还款、存款汇总逻辑。 |
| **money-common** | **通用工具**，JSON 处理、工具类、基础实体、常量定义。 |
| **money-flowable** | **工作流集成**，集成 Flowable 6.8.0 引擎，处理 BPMN 流程。 |
| **money-ui** | **前端工程**，基于 RuoYi-Vue (Vue2 版本) 定制的管理后台。 |
| **python/** | **数据采集服务**，独立 Python 进程，提供 `/stock/kline` 等数据接口供 Java 端调用。 |

### 2.3 数据流与交互逻辑 (以股票刷新为例)

1.  **触发 (Trigger)**：Quartz 定时任务 (`FixedTimeTask`) 生成批次任务。
2.  **入队 (Enqueue)**：`StockTaskQueueService` 将任务序列化 (JSON) 推入 **Redis List** (`mq:task:stock:refresh`)，并在 **Redis ZSet** 中记录状态。
3.  **消费 (Consume)**：
    -   `StockTaskConsumer` (位于 `money-quartz`) 启动后台线程池。
    -   **轮询 (Poll)**：从 Redis List 获取任务。
    -   **流控 (Semaphore)**：通过信号量限制并发数。
    -   **锁 (Redis Lock)**：获取分布式锁 (`stock:refresh:lock:{code}`) 防止重复处理。
4.  **执行 (Execute)**：调用 `IStockRefreshHandler` 进行业务处理 (调用 Python 服务获取数据 -> 存入 MySQL)。
5.  **反馈 (Ack)**：更新 Redis ZSet 中的任务状态 (SUCCESS/FAIL)，递减批次计数器。

---

## 3. 核心功能与业务流程拆解

### 3.1 股票/ETF 数据清洗与监控
-   **关键链路**：`Quartz` -> `StockTaskQueueService` -> `StockTaskConsumer` -> `Python Service` -> `DB`。
-   **核心策略**：
    -   **Python 侧**：使用 Playwright 模拟浏览器行为，绕过 JS 加密和反爬检测。
    -   **Java 侧**：
        -   **批次管理**：每个刷新周期生成唯一 `traceId`，通过 Redis 计数器监控批次完成度。
        -   **容错机制**：任务执行失败会记录日志，消费者通过 `tryLockRecovery` 机制处理“僵尸”批次。

### 3.2 财务资产管理
-   支持手动录入与 Excel 导入 (EasyExcel)。
-   **贷款管理**：通过 `LoanRepaymentsMapper` 跟踪还款计划，支持按 `user_id` + `installments` 更新还款状态。
-   **资产大屏**：通过 `Dashboard` 聚合展示存款、股票市值、理财收益。

### 3.3 工作流审批
-   集成 **Flowable 6.8.0**。
-   支持在线设计流程模型 (BPMN)，自动部署。
-   涵盖流程：我的待办、已办、发起流程 (如请假、报销)。

---

## 4. 项目亮点总结

1.  **混合语言架构 (Polyglot Persistence)**：
    -   巧妙结合 Java (强类型、生态丰富) 与 Python (爬虫生态强) 的优势。利用 Python 的 Playwright 解决 Java 难以处理的动态网页抓取问题。

2.  **自定义 Redis 分布式队列**：
    -   在不引入 Kafka/RabbitMQ 重型中间件的情况下，利用 `Redis List` + `ZSet` + `Semaphore` 实现了具备 **任务状态监控**、**并发控制**、**幂等重试** 的轻量级队列系统。
    -   实现了 **批次感知** (Batch Awareness)，只有当一批次任务全部完成时才触发下一动作。

3.  **可视化监控**：
    -   前端集成了 ECharts 图表，后端提供了详细的 Redis 队列状态查询接口，可以实时监控每个股票任务的执行状态 (WAITING, RUNNING, SUCCESS)。

4.  **模块化设计**：
    -   虽然是单体，但业务边界划分清晰 (`stock`, `finance`, `quartz` 分离)，便于未来拆分为微服务。

---

## 5. 项目优点分析

-   **部署轻量**：仅依赖 MySQL 和 Redis，无需维护 Kafka/Zookeeper 集群，适合中小规模部署。
-   **可维护性**：
    -   `money-framework` 封装了统一的异常处理、日志切面 (`kafkaLogAspect` 虽有提及但实际代码走的是 Redis 逻辑)、Web 配置，业务代码干扰少。
    -   Python 服务独立，爬虫逻辑变更不影响 Java 主程序。
-   **性能**：
    -   股票刷新采用了 **多线程 + 信号量** 模型，能充分利用 CPU，且通过 Redis 锁保证了数据一致性。
    -   前端使用 Element UI，成熟稳定。

---

## 6. 项目不足与风险点

1.  **文档与实际不符**：
    -   README 声称使用 **Vue 3**，但代码库实际为 **Vue 2.6**。这对新入职开发者会造成极大困扰。
    -   代码注释或“Memory”中提及的 "Kafka Migration" 在实际代码中并未体现 (未发现 `spring-kafka` 依赖)，仍在使用 Redis 队列。

2.  **自定义队列的局限性**：
    -   虽然 Redis 队列轻量，但缺乏专业 MQ 的高级特性 (如死信队列 DLQ、消息持久化落盘保障、精准的一次性语义)。
    -   `StockTaskQueueService` 中存在大量的 `JSON` 序列化/反序列化和 Redis 交互，高并发下可能成为网络瓶颈。

3.  **版本依赖风险**：
    -   Spring Boot 2.5.x 已停止官方支持 (End of OSS Support)。
    -   Vue 2.x 也已进入维护模式 (EOL)。

4.  **架构混淆**：
    -   `money-quartz` 模块不仅仅是调度器，还包含了 `consumer` 业务逻辑，违反了单一职责原则。理应将 Consumer 移至 `money-stock` 模块。

---

## 7. 优化建议 (按优先级排序)

1.  **修正文档与依赖说明 (High)**：
    -   立即更新 README，明确前端为 Vue 2，后端使用 Redis 队列而非 Kafka。
    -   清理代码中关于 "Kafka" 的误导性注释或未使用的配置。

2.  **前端升级 (Medium)**：
    -   制定计划将前端迁移至 Vue 3 + Vite + Element Plus，以获得更好的性能和 TypeScript 支持 (符合 README 的愿景)。

3.  **重构 Consumer 位置 (Medium)**：
    -   将 `money-quartz` 中的 `StockTaskConsumer` 及其相关 Handler 彻底迁移至 `money-stock` 模块。`money-quartz` 应仅负责生成事件/消息，不应感知具体的股票业务。

4.  **引入标准 MQ (Low - 视规模而定)**：
    -   如果任务量增长到 Redis 无法承受，或需要更高的消息可靠性，建议引入 RabbitMQ 或 Kafka 替换自定义的 `StockTaskQueueService`。

5.  **后端框架升级 (Low)**：
    -   升级至 Spring Boot 3.x (需同步升级 JDK 17+)，以获得更好的性能和安全性。
