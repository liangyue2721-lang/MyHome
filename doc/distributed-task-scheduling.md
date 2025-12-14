# 基于Redis与Quartz的分布式任务调度系统设计与实现

## 1. 系统概述

### 1.1 项目背景
基于SpringBoot+Vue前后端分离的企业级管理系统，主要用于财务管理、股票交易、工作流集成等业务场景。系统需要支持分布式任务调度功能，以满足企业用户在复杂业务流程处理中的需求。

### 1.2 设计目标
实现一个基于Redis与Quartz的分布式任务调度系统，保证任务在多个节点间平均分配，避免单点压力与过载，并支持任务执行时间过长的处理。

### 1.3 核心特性
- **负载均衡**：根据节点负载情况智能分配任务
- **高可用性**：支持节点故障自动检测和任务迁移
- **主节点选举**：通过Redis实现主节点选举机制
- **分布式锁**：使用Redis实现分布式锁，确保任务执行的唯一性
- **可扩展性**：模块化设计，易于扩展和维护

## 2. 系统架构设计

### 2.1 架构图
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              负载均衡器/Nginx                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼────────────────────────────┐
        │                           │                            │
┌─────────────────┐      ┌─────────────────┐        ┌─────────────────┐
│   节点 Node A   │      │   节点 Node B   │        │   节点 Node C   │
│                 │      │                 │        │                 │
│  Quartz Scheduler  │      │  Quartz Scheduler  │        │  Quartz Scheduler  │
│  TaskDistributor   │      │  TaskDistributor   │        │  TaskDistributor   │
│  NodeRegistry      │      │  NodeRegistry      │        │  NodeRegistry      │
│  RedisLockUtil     │      │  RedisLockUtil     │        │  RedisLockUtil     │
└─────────────────┘      └─────────────────┘        └─────────────────┘
        │                           │                            │
        └───────────────────────────┼────────────────────────────┘
                                    │
                            ┌───────────────┐
                            │    Redis      │
                            │               │
                            │  节点注册信息   │
                            │  节点负载信息   │
                            │  任务队列信息   │
                            │  分布式锁管理   │
                            └───────────────┘
```

### 2.2 工作流程
1. **节点注册与心跳**：
   - 每个节点启动时通过NodeRegistry向Redis注册自己
   - 定期更新心跳和负载信息

2. **任务调度触发**：
   - Quartz定时任务被触发时，AbstractQuartzJob处理任务执行逻辑

3. **分布式决策**：
   - 通过TaskDistributor决定任务在哪个节点执行
   - 使用RedisLockUtil保证任务分配的唯一性

4. **负载均衡**：
   - 根据各节点负载情况选择最合适的节点执行任务

5. **故障转移**：
   - NodeMonitor监控节点状态，处理节点失联后的任务迁移

## 3. 核心组件设计

### 3.1 NodeRegistry - 节点注册与心跳管理器
负责节点注册、心跳更新、负载上报等功能。

#### 主要方法
- `registerNode()`：注册当前节点到Redis
- `updateHeartbeat()`：更新心跳和负载信息
- `calculateNodeUsage()`：计算节点负载使用率

#### 关键代码示例
```java
/**
 * 计算节点负载使用率
 * 综合考虑CPU使用率、内存使用率和线程池使用情况
 * 
 * @return 负载使用率，范围0-1
 */
private double calculateNodeUsage() {
    try {
        // 获取内存使用率
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) heapUsed / heapMax;
        
        // 获取系统负载（如果有）
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osMXBean.getSystemLoadAverage();
        double cpuUsage = (systemLoad >= 0) ? systemLoad / osMXBean.getAvailableProcessors() : 0.5; // 默认50%
        
        // 综合计算，内存占60%，CPU占40%
        return memoryUsage * 0.6 + cpuUsage * 0.4;
    } catch (Exception e) {
        log.warn("计算节点负载时发生异常，使用默认负载值", e);
        return 0.5; // 默认50%负载
    }
}
```

### 3.2 TaskDistributor - 任务分发器
用于在集群环境中将任务分发到负载较低的节点执行。

#### 主要方法
- `shouldExecuteLocally()`：判断任务是否应该在当前节点执行
- `findLowestLoadNode()`：查找负载最低的节点
- `acquireTaskLock()`：获取任务执行锁
- `distributeTaskToNode()`：将任务分发到指定节点

### 3.3 RedisLockUtil - Redis分布式锁工具
基于Redis的SETNX命令实现分布式锁，确保在分布式环境下的互斥访问。

#### 主要方法
- `tryLock()`：尝试获取分布式锁
- `releaseLock()`：释放分布式锁
- `renewLock()`：续期分布式锁

#### 关键代码示例
```java
/**
 * 尝试获取分布式锁
 * 
 * @param lockKey 锁的键
 * @param lockValue 锁的值，一般使用唯一标识如UUID
 * @param expireTime 过期时间（秒），防止死锁
 * @return true-获取锁成功，false-获取锁失败
 */
public boolean tryLock(String lockKey, String lockValue, long expireTime) {
    try {
        // 使用SET命令的NX和EX选项原子性地设置键和过期时间
        // NX: 只有键不存在时才设置
        // EX: 设置键的过期时间（秒）
        Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    } catch (Exception e) {
        log.error("获取分布式锁失败，锁键: {}", lockKey, e);
        return false;
    }
}
```

### 3.4 AbstractQuartzJob - 抽象Quartz任务
带分布式锁的抽象Quartz Job，实现任务执行前后的通用逻辑。

#### 主要方法
- `execute()`：任务执行入口，集成分布式调度逻辑
- `doExecute()`：抽象方法，由子类实现具体业务逻辑

### 3.5 SchedulerManager - 调度管理器
负责主节点选举、线程池监控和任务分发。

#### 主要方法
- `masterElection()`：定时进行主节点选举
- `isMasterNode()`：判断当前节点是否为主节点
- `getJobIsMasterNode()`：获取任务是否需要在主节点执行

### 3.6 NodeMonitor - 节点监控器
负责监控集群中各节点状态，处理节点失联后的任务迁移等故障转移操作。

#### 主要方法
- `checkNodeStatus()`：定时检查节点状态
- `handleOfflineNode()`：处理失联节点
- `redistributeTask()`：重新分配任务到其他节点

## 4. Redis Key 命名规范

| Key前缀 | 用途 | 示例 |
|---------|------|------|
| `SCHEDULER_NODES` | 存储所有注册节点的集合 | `SCHEDULER_NODES` |
| `SCHEDULER_NODE:{nodeId}` | 特定节点的信息 | `SCHEDULER_NODE:192.168.1.100:uuid` |
| `SCHEDULER_NODE:{nodeId}:USAGE` | 节点负载使用率 | `SCHEDULER_NODE:192.168.1.100:uuid:USAGE` |
| `SCHEDULER_NODE:{nodeId}:HEARTBEAT` | 节点心跳时间戳 | `SCHEDULER_NODE:192.168.1.100:uuid:HEARTBEAT` |
| `SCHEDULER_MASTER` | 当前主节点信息 | `SCHEDULER_MASTER` |
| `SCHEDULER_LOCK:{taskId}` | 任务执行锁 | `SCHEDULER_LOCK:task123` |
| `JOB_MASTER_NODE:{jobId}` | 任务是否需要主节点执行 | `JOB_MASTER_NODE:job456` |

## 5. 故障转移与任务接管机制

### 5.1 节点失联检测
- 每个节点定期（每30秒）向Redis发送心跳
- 如果某个节点超过2分钟没有更新心跳，则认为该节点失联

### 5.2 任务迁移机制
- 当检测到节点失联时，系统会清理该节点的相关信息
- 将失联节点任务队列中的任务迁移到全局任务队列
- 其他在线节点会定期检查全局任务队列并获取任务执行

### 5.3 主节点故障转移
- 系统每30秒进行一次主节点选举
- 如果当前主节点失联，其他节点会竞争成为新的主节点
- 使用Redis分布式锁确保选举过程的互斥性

### 5.4 任务去重机制
- 使用分布式锁确保同一任务不会被多个节点同时执行
- 任务锁有过期时间，防止节点宕机导致锁无法释放

## 6. 技术规范与最佳实践

### 6.1 分布式定时任务执行控制规范
1. **主节点选举**：
   - 使用Redis实现主节点选举
   - 通过分布式锁确保只有一个节点能成为主节点
   - 定期更新主节点状态实现故障转移

2. **任务执行控制**：
   - 实现任务是否必须在主节点执行的配置
   - 使用分布式锁防止任务重复执行
   - 保持任务执行的幂等性

3. **负载均衡策略**：
   - 主节点优先执行任务
   - 当负载超过阈值时，将任务分发给负载较低的节点执行
   - 通过消息队列通知目标节点执行任务

4. **任务抢占机制**：
   - 通过任务锁实现节点抢占机制（以`task:lock:`为前缀）
   - 节点通过尝试获取任务锁来抢占任务执行权
   - 谁先获取到锁，谁就获得任务的执行权
   - 当主节点负载超过阈值时，主节点会主动释放任务锁
   - 通过短暂等待让其他节点有机会抢占任务
   - 如果再次获取到锁，说明没有其他节点抢占，继续在主节点执行

5. **任务分配规范**：
   - 在进行任务分配时，需要综合考虑节点的负载情况和内存使用率
   - 当节点内存使用率超过85%时，不应再向该节点分配新任务，以避免内存不足导致的问题

### 6.2 线程池配置与分布式任务调度优化经验
1. **线程池配置优化原则**:
   - 线程池大小应基于CPU核心数动态配置
   - 核心线程数建议设置为CPU核心数的2倍
   - 最大线程数建议设置为CPU核心数的4-8倍
   - 队列容量应根据预期负载合理设置

2. **任务分离最佳实践**:
   - 将消息监听和任务处理分离到不同的线程池
   - 为不同类型的任务创建专门的线程池
   - 避免不同任务类型相互阻塞

## 7. 总结

我们实现了一个完整的基于Redis与Quartz的分布式任务调度系统，具有以下特点：

1. **负载均衡**：根据节点负载情况智能分配任务
2. **高可用性**：支持节点故障自动检测和任务迁移
3. **主节点选举**：通过Redis实现主节点选举机制
4. **分布式锁**：使用Redis实现分布式锁，确保任务执行的唯一性
5. **可扩展性**：模块化设计，易于扩展和维护

该系统能够满足多节点任务执行时自动均衡分配的需求，并在节点宕机时自动迁移任务，确保任务的可靠执行。