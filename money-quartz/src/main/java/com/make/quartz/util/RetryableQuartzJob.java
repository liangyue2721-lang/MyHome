package com.make.quartz.util;

import com.make.quartz.domain.SysJob;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 带重试机制的 Quartz Job 抽象层
 *
 * <p>职责：
 * <ul>
 *   <li>为 doExecute(context, sysJob) 增加重试机制</li>
 *   <li>子类只需要实现 executeInternal(context, sysJob)（一次执行逻辑）</li>
 * </ul>
 */
public abstract class RetryableQuartzJob extends QuartzJobWrapper {

    private static final Logger log = LoggerFactory.getLogger(RetryableQuartzJob.class);

    /**
     * 最大重试次数（不含首次执行）
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 重试间隔（毫秒）
     */
    private static final long RETRY_INTERVAL_MS = 5000L;

    @Override
    protected final void doExecute(JobExecutionContext context, SysJob sysJob) {
        Exception last = null;

        for (int attempt = 0; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                if (attempt > 0) {
                    sleepRespectInterrupt(RETRY_INTERVAL_MS);
                }
                executeInternal(context, sysJob);
                return;
            } catch (Exception e) {
                last = e;
                if (attempt >= MAX_RETRY_COUNT) {
                    throw new RuntimeException(
                            "任务执行失败，已重试 " + MAX_RETRY_COUNT + " 次", e
                    );
                }
            }
        }

        if (last != null) {
            throw new RuntimeException(last);
        }
    }


    /**
     * 睡眠并保留线程中断语义
     *
     * @param millis 睡眠毫秒
     */
    private void sleepRespectInterrupt(long millis) throws Exception {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new Exception("任务重试等待被中断", ie);
        }
    }

    /**
     * 子类实现的“一次执行”逻辑（不包含重试）
     *
     * @param context Quartz 上下文
     * @param sysJob  任务信息
     */
    protected abstract void executeInternal(JobExecutionContext context, SysJob sysJob) throws Exception;
}
