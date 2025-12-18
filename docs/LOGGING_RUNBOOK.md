# Quartz Task Troubleshooting Runbook

This guide explains how to use the enhanced logging in `AbstractQuartzJob` to diagnose why a scheduled task is not running or is experiencing delays.

## 1. Trace a Specific Task Instance

Every execution attempt generates a `fireInstanceId`. You can use this ID to trace the complete lifecycle of a single execution attempt across all nodes.

**Search Query:** `[TASK_MONITOR]` AND `<fireInstanceId>`

**Example Lifecycle:**
```text
[TASK_MONITOR] [TRIGGER] Job triggered. Key: DEFAULT.MyJob, InstanceId: 12345, InvokeTarget: supperTask.refreshStockPrice()
[TASK_MONITOR] [LOCK_ACQUIRED] Key: DEFAULT.MyJob, InstanceId: 12345, Node: 192.168.1.10
[TASK_MONITOR] [EXECUTE_START] Starting local execution... InvokeTarget: supperTask.refreshStockPrice()
[TASK_MONITOR] [EXECUTE_END] Local execution finished... Duration: 500ms
[TASK_MONITOR] [LOCK_RELEASED] Key: DEFAULT.MyJob...
```

**Tip:** You can now grep for the method name (e.g., `runStockKlineTask` or `refreshStockPrice`) to find the relevant job execution, even if you don't know the exact Job Key.

## 2. Diagnose "Lock Skipped" (Why didn't it run?)

If a task logs `[LOCK_SKIPPED]`, it means another node holds the distributed lock.

**Log Sample:**
```text
[TASK_MONITOR] [LOCK_SKIPPED] Failed to acquire lock. Key: DEFAULT.MyJob, InstanceId: 67890, Node: 192.168.1.11, TTL: 4500ms
```

**Interpretation:**
*   **Key:** The job that was skipped.
*   **Node:** The node that *failed* to get the lock (the loser).
*   **TTL:** How many milliseconds the lock will remain valid.
    *   **Small TTL (< 5s):** Normal contention. Another node is just finishing up.
    *   **Large TTL (~30s+):** Potentially a long-running job.
    *   **TTL not decreasing:** The lock might be held by a stuck process (though Redisson watchdog usually keeps it alive while the thread is active).

**How to find the "Winner" (Who held the lock?):**
Since the lock key is `quartz:lock:DEFAULT.MyJob`, search for the **successful** acquisition log around the same time.

**Search Query:** `[LOCK_ACQUIRED]` AND `DEFAULT.MyJob`

Look for the log entry that occurred *before* your skipped event.
```text
[TASK_MONITOR] [LOCK_ACQUIRED] Key: DEFAULT.MyJob, InstanceId: 11111, Node: 192.168.1.10
```
*   **Node 192.168.1.10** is the winner.
*   Check if it ever logged `[LOCK_RELEASED]`. If not, the job is stuck on that node.

## 3. Diagnose "Stuck" Jobs

If you suspect a job is stuck:
1.  Find the last `[LOCK_ACQUIRED]` for that job.
2.  Check for a corresponding `[LOCK_RELEASED]` or `[EXECUTE_END]`.
3.  If missing, check the **Node** IP from the acquired log.
4.  Investigate that specific server (CPU, memory, thread dump).

## 4. Execution Performance

To check if a job is slow:
**Search Query:** `[EXECUTE_END]` AND `DEFAULT.MyJob`

The log contains `Duration: Xms`.
```text
[TASK_MONITOR] [EXECUTE_END] ... Duration: 15000ms
```
If duration is consistently high, the job logic needs optimization, or it should be offloaded to the distributed queue.
