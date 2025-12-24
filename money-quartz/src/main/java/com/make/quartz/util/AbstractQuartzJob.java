package com.make.quartz.util;

import com.make.common.config.RedisQuartzSemaphore;
import com.make.common.utils.ThreadPoolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job 抽象模板（不可泄漏信号量版本）
 *
 * <p>设计目标：
 * <ul>
 *   <li>Quartz 触发线程只负责“生产”（抢占互斥信号量 + 投递执行任务）</li>
 *   <li>真实业务逻辑在业务线程池中“消费”执行</li>
 *   <li>RedisQuartzSemaphore 的 release 只有一个出口（finally），避免“占锁不执行”</li>
 * </ul>
 */
public abstract class AbstractQuartzJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * Quartz Job 入口（模板方法，禁止子类覆盖）
     *
     * <p>执行流程：
     * <ol>
     *   <li>尝试抢占分布式信号量（失败则跳过）</li>
     *   <li>执行前校验（可选，允许放弃执行）</li>
     *   <li>投递到执行线程池中消费</li>
     * </ol>
     *
     * <p>不可泄漏保证：
     * <ul>
     *   <li>投递失败：catch 中 release</li>
     *   <li>投递成功：消费线程 finally 中 release（唯一出口）</li>
     * </ul>
     */
    @Override
    public final void execute(JobExecutionContext context) throws JobExecutionException {
        final String jobKey = getJobKey(context);

        // 1) 调度生产阶段：互斥判断
        if (!RedisQuartzSemaphore.tryAcquire(jobKey)) {
            return;
        }
        log.info("[JOB_SEMAPHORE_ACQUIRED] jobKey={}", jobKey);

        try {
            // 2) 调度生产阶段：前置校验
            if (!beforeExecute(context)) {
                RedisQuartzSemaphore.release(jobKey);
                return;
            }

            // 3) 投递到消费线程池
            log.info("[JOB_SUBMIT_POOL] jobKey={}", jobKey);
            ThreadPoolUtil.getCoreExecutor().execute(wrapExecute(context, jobKey));
        } catch (Exception e) {
            // 4) 投递失败兜底释放，避免“占锁不执行”
            RedisQuartzSemaphore.release(jobKey);
            throw new JobExecutionException(e);
        }
    }

    /**
     * 包装消费线程执行器
     *
     * <p>保证：
     * <ul>
     *   <li>业务异常不会吞掉</li>
     *   <li>信号量只在 finally 中释放（唯一出口）</li>
     * </ul>
     *
     * @param context Quartz 执行上下文
     * @param jobKey  任务唯一标识
     * @return 可提交到线程池的 Runnable
     */
    private Runnable wrapExecute(JobExecutionContext context, String jobKey) {
        return () -> {
            try {
                doExecute(context);
            } finally {
                // 唯一释放点：无论成功/失败/return，必释放
                RedisQuartzSemaphore.release(jobKey);
                log.info("[JOB_SEMAPHORE_RELEASED] jobKey={}", jobKey);
            }
        };
    }

    /**
     * 执行前校验（可选扩展）
     *
     * @param context Quartz 执行上下文
     * @return true-继续执行；false-放弃本次执行
     */
    protected boolean beforeExecute(JobExecutionContext context) {
        return true;
    }

    /**
     * 子类实现真实业务逻辑（在执行线程池中运行）
     *
     * @param context Quartz 执行上下文
     */
    protected abstract void doExecute(JobExecutionContext context);

    /**
     * 子类提供任务唯一 Key（分布式互斥依据）
     *
     * @param context Quartz 执行上下文
     * @return jobKey（跨实例必须一致）
     */
    protected abstract String getJobKey(JobExecutionContext context);
}
