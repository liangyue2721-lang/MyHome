package com.make.quartz.task;

import com.make.common.config.RedisQuartzSemaphore;
import com.make.common.utils.ThreadPoolUtil;

/**
 * 抽象调度任务模板（不可泄漏信号量版本）
 * <p>
 * 设计目标：
 * 1. 调度线程只负责“生产调度信号”
 * 2. 不在调度线程中执行任何耗时逻辑
 * 3. RedisQuartzSemaphore 只绑定执行线程
 * 4. 任意 return / 异常都不会造成信号量泄漏
 * <p>
 * 使用方式：
 * - 子类只实现 doExecute()
 * - 子类只关注业务逻辑
 * - 不允许子类直接操作线程池或信号量
 */
public abstract class AbstractScheduledTask {

    /**
     * 调度入口（模板方法，禁止子类覆盖）
     * <p>
     * 该方法通常由 @Scheduled 或调度线程池触发
     */
    public final void execute() {
        String jobKey = getJobKey();

        // ===== 1. 调度线程阶段：仅做分布式互斥判断 =====
        if (!RedisQuartzSemaphore.tryAcquire(jobKey)) {
            // 其他实例正在消费，直接返回
            return;
        }

        try {
            // ===== 2. 执行前校验（允许放弃执行）=====
            if (!beforeExecute()) {
                // 注意：此时还未进入执行线程，必须释放信号量
                RedisQuartzSemaphore.release(jobKey);
                return;
            }

            // ===== 3. 提交到执行线程池（消费阶段）=====
            ThreadPoolUtil.getCoreExecutor().execute(
                    wrapExecute(jobKey)
            );

        } catch (Exception e) {
            // ===== 4. 提交失败兜底释放信号量 =====
            RedisQuartzSemaphore.release(jobKey);
            throw e;
        }
    }

    /**
     * 执行线程包装器
     * <p>
     * 确保：
     * - 业务异常不会吞掉
     * - 信号量一定在 finally 中释放
     */
    private Runnable wrapExecute(String jobKey) {
        return () -> {
            try {
                doExecute();
            } catch (Throwable t) {
                // 这里建议接入统一日志 / 监控
                throw t;
            } finally {
                // ===== 唯一释放点 =====
                RedisQuartzSemaphore.release(jobKey);
            }
        };
    }

    /**
     * 执行前校验（可选）
     *
     * @return false 表示本次调度不执行
     */
    protected boolean beforeExecute() {
        return true;
    }

//    /**
//     * 子类实现的真实业务逻辑（只写业务）
//     */
//    protected abstract void doExecute();

    /**
     * 任务唯一标识（用于分布式互斥）
     * <p>
     * 要求：
     * - 同一个任务在所有实例上返回值一致
     * - 建议使用 jobId / taskCode
     */
    protected abstract String getJobKey();
}
