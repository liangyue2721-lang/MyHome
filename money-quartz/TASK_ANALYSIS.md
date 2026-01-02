# Quartz Task Execution Analysis

## 1. Task Execution Mechanism

The `money-quartz` module uses a database-driven task scheduling system based on Quartz.

### 1.1 Core Components
*   **Database Table (`sys_job`)**: Stores job definitions. The key column is `invoke_target`, which specifies the method to execute.
*   **Execution Class (`AbstractQuartzJob` & `JobInvokeUtil`)**:
    *   `AbstractQuartzJob`: The base Quartz Job class that handles logging, locking, and exception handling.
    *   `JobInvokeUtil`: Uses Java Reflection to parse `invoke_target` (e.g., `beanName.methodName(params)`) and invoke the actual service method.

### 1.2 Execution Flow
1.  Quartz Trigger fires.
2.  `AbstractQuartzJob.execute()` is called.
3.  It retrieves the `SysJob` properties.
4.  It calls `JobInvokeUtil.invokeMethod(sysJob)`.
5.  `JobInvokeUtil` resolves the Spring Bean or Class and calls the target method.

**Critical Rule:** If a service method is not defined in `sys_job` (via `invoke_target`), Quartz will never trigger it.

---

## 2. Analysis of `StockKlineTaskExecutor`

**Symptom:** `StockKlineTaskExecutor.executeAll()` is never executed.

**Diagnosis:**
1.  **Call Chain:**
    *   `StockKlineTaskExecutor.executeAll()` is called by `KlineAggregatorServiceImpl.runStockKlineTask()`.
    *   `KlineAggregatorServiceImpl.runStockKlineTask()` is called by `IStockTaskServiceImpl.runStockKlineTask()`.
    *   `IStockTaskServiceImpl.runStockKlineTask()` is called by `SupperTask.refreshStockPrice()`.
2.  **Missing Configuration:**
    *   The entry point `SupperTask` (Bean Name: `supperTask`) has a method `refreshStockPrice()`.
    *   **The `sys_job` table does NOT contain a record for `invoke_target = 'supperTask.refreshStockPrice()'`**.
    *   Therefore, the entire chain is never triggered by Quartz.

**Conclusion:** The logic exists but is orphaned due to missing database configuration.

---

## 3. Inventory of Unexecuted Task Implementations

The following service methods in `money-quartz/src/main/java/com/make/quartz/service/impl` (and sub-packages) appear to be intended as scheduled tasks but are currently **NOT executed** via Quartz:

### 3.1 Stock Tasks (Entry Point: `supperTask.refreshStockPrice()`)
The following components are dependent on `SupperTask.refreshStockPrice()`, which is missing from `sys_job`:

| Class | Method | Description | Status |
| :--- | :--- | :--- | :--- |
| `StockKlineTaskExecutor` | `executeAll` | Fetches historical K-line data for stocks. | **Not Executed** |
| `StockETFrocessor` | `processTask` | Processes ETF data. | **Not Executed** |
| `StockWatchProcessor` | `processTask` | Processes "Watch List" stocks. <br>*(Note: This class implements `SmartLifecycle`, so it **does** run its own loop on startup, independently of Quartz. However, the Quartz trigger intended via `SupperTask` is broken.)* | **Running via Lifecycle** (Quartz trigger broken) |

### 3.2 Finance Tasks (Entry Point: `supperTask.refreshFinanceData()`)
The following component is dependent on `SupperTask.refreshFinanceData()`, which is missing from `sys_job`:

| Class | Method | Description | Status |
| :--- | :--- | :--- | :--- |
| `IFinanceTaskServiceImpl` | `refreshDepositAmount` | Refreshes annual deposit and bank balance data. | **Not Executed** |

---

## 4. Recommendations

To fix the unexecuted tasks, you must insert the corresponding job definitions into the `sys_job` table.

### 4.1 SQL to Fix Missing Tasks

```sql
-- 1. Add Stock Price Refresh Task (Triggers Kline, ETF, and WatchList logic)
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES ('Refresh Stock Price', 'DEFAULT', 'supperTask.refreshStockPrice()', '0 0/30 9-15 * * ?', '3', '1', '0', 'admin', sysdate(), 'Refreshes Stock KLines, ETFs, and Watch List');

-- 2. Add Finance Data Refresh Task
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES ('Refresh Finance Data', 'DEFAULT', 'supperTask.refreshFinanceData()', '0 0 2 * * ?', '3', '1', '0', 'admin', sysdate(), 'Refreshes Deposit and Finance Data daily at 2 AM');
```

*Note: Adjust cron expressions (`0 0/30 9-15 * * ?` and `0 0 2 * * ?`) according to actual business requirements.*
