# Master Election Mechanism Analysis

## Overview
This document outlines the current implementation of the Master Election mechanism in the Quartz scheduling system, specifically within `SchedulerManager`. It explains the logic flow, Redis key usage, and the design behavior that leads to the requirement for manual intervention in master failover scenarios.

## Execution Chain

*   **Class**: `com.make.quartz.util.SchedulerManager`
*   **Method**: `masterElection()`
*   **Trigger**: Spring `@Scheduled` task, running every 30 seconds (`fixedRate = 30000`).
*   **Distributed Lock**: `MASTER_ELECTION_LOCK` (Redisson Lock)
*   **Master Registry Key**: `SCHEDULER_MASTER` (Redis String)

## Logic Flow

The `masterElection` method performs the following steps:

1.  **Acquire Lock**: Attempts to acquire the distributed lock `MASTER_ELECTION_LOCK` with a 3-second wait time and 30-second lease.
2.  **Check Master Key**: Reads the value of the Redis key `SCHEDULER_MASTER`.
3.  **Condition A: No Master Key**
    *   If the key is `null`, the current node registers itself as the master.
    *   **Action**: `SET SCHEDULER_MASTER <CURRENT_NODE_ID> EX 60`
    *   **Result**: Current node becomes Master.
4.  **Condition B: Master Key Exists**
    *   The system checks the TTL (Time To Live) of the key.
    *   **Sub-condition B1: Key Expired** (TTL <= 0)
        *   The current node registers itself as the master.
        *   **Action**: `SET SCHEDULER_MASTER <CURRENT_NODE_ID> EX 60`
    *   **Sub-condition B2: Key Active** (TTL > 0)
        *   **Current Behavior**: The system extends the expiration time of the *existing* key.
        *   **Action**: `EXPIRE SCHEDULER_MASTER 60`
        *   **Critical Note**: This action is performed by *any* node that runs this task, regardless of whether it is the master or not.

## The "Zombie Master" Phenomenon

The logic in **Sub-condition B2** creates a scenario where the Master identity cannot effectively switch automatically if the Master node fails but other nodes are still running.

1.  **Scenario**: Node A is Master. `SCHEDULER_MASTER` contains "Node A".
2.  **Failure**: Node A crashes.
3.  **Persistence**: Node B (Slave) runs `masterElection`.
4.  **Check**: Node B sees `SCHEDULER_MASTER` exists ("Node A") and has valid TTL.
5.  **Renewal**: Node B executes `EXPIRE SCHEDULER_MASTER 60`.
6.  **Outcome**: The key "Node A" remains in Redis, refreshed every 30 seconds by Node B (and other slaves).
7.  **Result**:
    *   Node B does not become Master (because the key exists).
    *   Node A is dead.
    *   **System State**: No active Master. Scheduled tasks (which require `isMasterNode() == true`) stop executing.

## Conclusion

Due to the unrestricted TTL renewal logic, a Master node identity persists in Redis as long as *any* node in the cluster is alive.

**Requirement for Manual Intervention**:
To switch the Master, the `SCHEDULER_MASTER` key must be manually deleted from Redis or the system must completely stop (all nodes down) to let the key expire naturally.
