package com.make.common.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 线程池工具类 (JDK 8 兼容版)
 *
 * <p>整体功能说明：
 * - 提供项目统一可复用的线程池实例
 * - 根据 CPU 2 核 / 4 线程(Kaby Lake)优化线程池参数
 * - 提供核心线程池、股票更新线程池、调度线程池
 * - 提供自定义线程池创建方法
 * - 统一记录线程池运行状态
 * - 提供优雅关闭线程池的能力
 *
 * <p>线程池优化策略：
 * - IO 密集型线程池最佳线程数 ≈ CPU 核心数 * 2～4（你的 CPU = 2 核 → 推荐 4～8）
 * - 因此 corePoolSize=4, maximumPoolSize=8
 * - 股票更新线程池独立配置，不影响主业务线程池
 */
public class ThreadPoolUtil {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    /* =========================================================================
     * 1. 核心业务线程池（适合 IO 密集型任务，数据库、HTTP、磁盘等）
     * ========================================================================= */
    private static final ExecutorService CORE_EXECUTOR = new ThreadPoolExecutor(
            8,                         // 核心线程数，CPU核心数 * 2（适配 IO 密集）
            16,                         // 最大线程数，CPU核心数 * 4
            60L, TimeUnit.SECONDS,     // 空闲线程存活时间
            new LinkedBlockingQueue<>(2000),        // 队列容量适中
            createNamedThreadFactory("core-pool"),  // 自定义线程工厂便于排查
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略（调用者线程执行）
    );

    /* =========================================================================
     * 2. 股票利润更新线程池（独立，不影响核心业务线程）
     * ========================================================================= */
    private static final ExecutorService WATCH_STOCK_EXECUTOR = new ThreadPoolExecutor(
            4,                         // 核心线程数
            6,                         // 最大线程数
            120L, TimeUnit.SECONDS,    // 空闲线程存活时间更长
            new LinkedBlockingQueue<>(1000),         // 队列容量
            createNamedThreadFactory("watch-stock-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /* =========================================================================
     * 3. 调度线程池（用于定时/延迟任务）
     * ========================================================================= */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(2, createNamedThreadFactory("scheduler"));

    /**
     * 获取核心业务线程池
     */
    public static ExecutorService getCoreExecutor() {
        logThreadPoolStatus("core-pool", CORE_EXECUTOR);
        return CORE_EXECUTOR;
    }

    /**
     * 获取股票更新线程池
     */
    public static ExecutorService getWatchStockExecutor() {
        logThreadPoolStatus("watch-stock-pool", WATCH_STOCK_EXECUTOR);
        return WATCH_STOCK_EXECUTOR;
    }

    /**
     * 获取调度线程池
     */
    public static ScheduledExecutorService getScheduler() {
        logThreadPoolStatus("scheduler", SCHEDULER);
        return SCHEDULER;
    }

    /**
     * 记录线程池当前状态（JDK 8 版本）
     */
    private static void logThreadPoolStatus(String poolName, ExecutorService executor) {
        try {
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tp = (ThreadPoolExecutor) executor;

                int activeCount = tp.getActiveCount();
                int poolSize = tp.getPoolSize();
                int corePoolSize = tp.getCorePoolSize();
                int maxPoolSize = tp.getMaximumPoolSize();
                long completed = tp.getCompletedTaskCount();
                int queueSize = tp.getQueue().size();

                logger.info(
                        "线程池 [{}] 状态 - 核心:{}, 最大:{}, 活跃:{}, 当前池大小:{}, 已完成:{}, 队列:{}",
                        poolName, corePoolSize, maxPoolSize, activeCount, poolSize, completed, queueSize
                );
            }
        } catch (Exception e) {
            logger.warn("无法获取线程池 [{}] 状态信息", poolName, e);
        }
    }

    /**
     * 记录所有线程池的状态
     */
    public static void logAllThreadPoolStatus() {
        logger.info("开始记录所有线程池状态信息");
        logThreadPoolStatus("core-pool", CORE_EXECUTOR);
        logThreadPoolStatus("watch-stock-pool", WATCH_STOCK_EXECUTOR);
        logThreadPoolStatus("scheduler", SCHEDULER);
        logger.info("完成记录所有线程池状态信息");
    }

    /**
     * 提交一个延迟任务
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return SCHEDULER.schedule(task, delay, unit);
    }

    /**
     * 创建一个自定义线程池
     */
    public static ExecutorService createCustomThreadPool(
            int core, int max, int queueCapacity, String prefix) {

        return new ThreadPoolExecutor(
                core,
                max,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                createNamedThreadFactory(prefix),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 创建命名线程工厂（JDK 8 可用）
     */
    private static ThreadFactory createNamedThreadFactory(final String prefix) {
        return new ThreadFactory() {

            private final AtomicInteger num = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName(prefix + "-thread-" + num.getAndIncrement()); // 设置线程名
                t.setDaemon(false);
                return t;
            }
        };
    }

    /**
     * 优雅关闭线程池
     */
    public static void shutdownGracefully(ExecutorService executor, long timeout, TimeUnit unit) {
        executor.shutdown();  // 禁止提交新任务
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                executor.shutdownNow(); // 超时强制关闭
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
