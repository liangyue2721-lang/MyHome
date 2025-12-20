package com.make.quartz.service.common;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 分区批量执行器
 * <p>
 * 通用工具类，用于将大集合拆分为小批次，利用线程池并发处理，并收集结果。
 * 替代手动 Lists.partition + Future 循环的重复代码。
 * </p>
 */
@Component
public class PartitionExecutor {

    private static final Logger log = LoggerFactory.getLogger(PartitionExecutor.class);

    /**
     * 并发批量处理
     *
     * @param items       待处理的元素列表
     * @param batchSize   每批次大小
     * @param executor    线程池
     * @param processor   处理函数（输入单个元素，返回结果；若返回 null 则表示处理失败或忽略）
     * @param traceId     链路ID（用于日志）
     * @param taskName    任务名称（用于日志）
     * @param <T>         输入类型
     * @param <R>         输出类型
     * @return 处理成功的非空结果列表
     */
    public <T, R> List<R> execute(List<T> items, int batchSize, ExecutorService executor,
                                  Function<T, R> processor, String traceId, String taskName) {

        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        long start = System.currentTimeMillis();
        List<List<T>> partitions = Lists.partition(items, batchSize);
        List<R> allResults = new ArrayList<>();
        int totalProcessed = 0;

        for (List<T> batch : partitions) {
            List<Future<R>> futures = new ArrayList<>();

            // 提交批次任务
            for (T item : batch) {
                futures.add(executor.submit(() -> {
                    try {
                        return processor.apply(item);
                    } catch (Exception e) {
                        log.error("[PartitionExecutor] {} 处理异常 Item={} TraceId={} err={}",
                                taskName, item, traceId, e.getMessage());
                        return null;
                    }
                }));
            }

            // 收集批次结果
            for (Future<R> future : futures) {
                try {
                    // 设置单个任务的超时时间，防止卡死
                    R result = future.get(30, TimeUnit.SECONDS);
                    if (result != null) {
                        allResults.add(result);
                    }
                } catch (Exception e) {
                    log.error("[PartitionExecutor] {} 获取结果失败 TraceId={} err={}", taskName, traceId, e.getMessage());
                }
            }

            totalProcessed += batch.size();
        }

        long cost = System.currentTimeMillis() - start;
        if (log.isDebugEnabled()) {
             log.debug("[PartitionExecutor] {} 完成 | TraceId={} | Total={} | Success={} | Cost={}ms",
                taskName, traceId, items.size(), allResults.size(), cost);
        }

        return allResults;
    }
}
