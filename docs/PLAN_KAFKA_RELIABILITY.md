# Kafka 消费者可靠性实施方案 (Detailed Reliability Implementation Plan)

## 1. 概述 (Overview)
本方案旨在构建高可靠的 Kafka 消费端，通过 **Redis 幂等性** 解决重复消费问题，通过 **死信队列 (DLQ)** 解决毒丸消息导致的分区阻塞问题。

---

## 步骤 1：基于 Redis 的幂等性机制 (AOP + Redis)

### 1.1 设计原理
Kafka 的 `At-Least-Once` 语义决定了消费者必须处理重复消息。我们采用 **"检查-执行-标记"** 模式。

*   **存储：** Redis String
*   **Key 格式：** `kafka:consumed:{prefix}:{traceId}`
*   **Value：** `1`
*   **TTL：** 24-48 小时 (覆盖最大可能的重试周期)

### 1.2 核心组件实现细节

#### A. 自定义注解 `@IdempotentConsumer`
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentConsumer {
    String key();      // SpEL 表达式，如 "#record.key()" 或 "#message.traceId"
    String prefix();   // 业务前缀，如 "stock:refresh:"
    long expire() default 86400; // 过期时间
}
```

#### B. 切面逻辑 (`IdempotentConsumerAspect`)
```java
@Around("@annotation(idempotent)")
public Object around(ProceedingJoinPoint joinPoint, IdempotentConsumer idempotent) {
    // 1. 解析 Key
    String uniqueKey = parseKey(idempotent.key(), joinPoint);
    String redisKey = idempotent.prefix() + uniqueKey;

    // 2. 检查是否已消费
    if (redisTemplate.hasKey(redisKey)) {
        log.warn("[Idempotent] Skip duplicate message: {}", redisKey);
        return null; // 幂等跳过，视为消费成功
    }

    // 3. 执行业务逻辑
    try {
        Object result = joinPoint.proceed();

        // 4. 标记为已消费 (仅成功时)
        // 注意：此处存在极小概率的并发重复消费 (Race Condition)，
        // 但对于"刷新行情"这类覆盖型业务是可接受的。
        // 若需严格 Exactly-Once，需使用 setIfAbsent (SETNX) 锁住整个执行过程，
        // 但会降低并发吞吐。本方案选择 "最终一致性" 策略。
        redisTemplate.opsForValue().set(redisKey, "1", idempotent.expire(), TimeUnit.SECONDS);

        return result;
    } catch (Throwable e) {
        // 5. 异常时不写入 Redis，允许 Kafka 重试
        throw e;
    }
}
```

---

## 步骤 2：全局错误处理与 DLQ 配置

### 2.1 错误处理策略 (`DefaultErrorHandler`)
Spring Kafka 2.8+ 推荐使用 `DefaultErrorHandler` 替代旧的 `SeekToCurrentErrorHandler`。

*   **重试策略：** 固定间隔 1000ms，重试 3 次。
*   **兜底策略：** 重试耗尽后，投递到死信队列。

### 2.2 配置代码示例
```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
    // 1. 死信发布器
    // 默认投递到 topic.DLQ，保持原 Partition
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

    // 2. 回退策略 (Backoff)
    FixedBackOff backOff = new FixedBackOff(1000L, 3); // 间隔1s，重试3次

    // 3. 构建 Handler
    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

    // 4. 定义哪些异常不重试 (可选)
    // handler.addNotRetryableExceptions(IllegalArgumentException.class);

    return handler;
}
```

### 2.3 `application.yml` 配置
```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false # 关闭自动提交
      ack-mode: RECORD          # 每处理一条提交一次 offset (配合 ErrorHandler 使用)
```

---

## 步骤 3：应用到业务消费者

需修改以下消费者类，添加注解：

### 3.1 股票模块 (`money-stock`)
*   **类：** `com.make.stock.service.consumer.StockTaskConsumer`
*   **方法：** `handleStockRefresh`
    *   **注解：** `@IdempotentConsumer(key = "#record.key()", prefix = "kafka:stock:refresh:")`
    *   **说明：** 假设 Producer 发送时将 `traceId` 放入 Key 中。

### 3.2 财务模块 (`money-finance`)
*   **类：** `com.make.finance.service.impl.FinanceTaskConsumer`
*   **方法：** `handleFinanceRefresh`
    *   **注解：** `@IdempotentConsumer(key = "#record.key()", prefix = "kafka:finance:refresh:")`

---

## 步骤 4：验证与测试策略

### 4.1 单元测试 (Unit Test)
*   **目标：** 验证 AOP 逻辑。
*   **用例：**
    1.  **Duplicate Test:** Mock Redis 返回 Key 存在 -> Verify `joinPoint.proceed()` **未被调用**。
    2.  **Failure Test:** Mock `joinPoint.proceed()` 抛出异常 -> Verify Redis `set` **未被调用**。

### 4.2 集成冒烟测试 (Integration Smoke Test)

| 场景 | 操作步骤 | 预期结果 |
| :--- | :--- | :--- |
| **正常消费** | 发送 Key="T-101" | 1. 业务执行成功<br>2. Redis 存在 `kafka:consumed:stock:refresh:T-101` |
| **重复消费** | 再次发送 Key="T-101" | 1. 日志打印 `[Idempotent] Skip duplicate` <br>2. 业务逻辑未重复执行 |
| **业务重试** | 模拟业务抛出 `RuntimeException` | 1. 控制台出现 3 次重试日志 (间隔 1s) <br>2. Redis 中 **无** Key |
| **死信投递** | 模拟持续失败 (重试耗尽) | 1. 消息出现在 `original-topic.DLQ`<br>2. 主 Topic offset 前进 (不阻塞后续消息) |

### 4.3 运维监控
*   **监控点：**
    *   Redis Key 增长速率 (反映任务量)。
    *   DLQ Topic 消息积压量 (反映系统异常率)。
