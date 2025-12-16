package com.make.quartz.service.batch;

import com.make.stock.domain.GoldProductPrice;
import com.make.stock.service.IGoldProductPriceService;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 黄金价格批量更新服务
 *
 * <p>
 * 该服务旨在通过内存队列和定时任务，将多次独立的数据库更新操作聚合为一次批量操作，
 * 以减少数据库IO，降低CPU负载。
 * </p>
 *
 * <p><b>工作流程：</b></p>
 * <ol>
 *   <li><b>数据入队：</b>外部任务（如 GoldViewTask）调用 {@link #addUpdateTask(GoldProductPrice)} 方法，
 *       将待更新的 {@link GoldProductPrice} 对象放入一个线程安全的内存队列中。</li>
 *   <li><b>定时刷盘：</b>通过 Spring 的 {@code @Scheduled} 注解，定时触发 {@link #flushToDatabase()} 方法。</li>
 *   <li><b>批量更新：</b>{@code flushToDatabase} 方法会从队列中取出所有数据，并调用
 *       {@link IGoldProduct-PriceService} 的批量更新接口，一次性将数据写入数据库。</li>
 *   <li><b>优雅停机：</b>通过 Spring 的 {@code @PreDestroy} 注解，确保在应用关闭前，
 *       执行最后一次刷盘操作，防止内存数据丢失。</li>
 * </ol>
 */
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class GoldPriceBatchUpdateService {

    private static final Logger log = LoggerFactory.getLogger(GoldPriceBatchUpdateService.class);

    private final ConcurrentLinkedQueue<GoldProductPrice> updateQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<GoldProductPrice> insertQueue = new ConcurrentLinkedQueue<>();

    @Resource
    private IGoldProductPriceService goldProductPriceService;

    /**
     * 将待更新的黄金价格数据添加到更新队列中。
     */
    public void addUpdateTask(GoldProductPrice goldProductPrice) {
        if (goldProductPrice != null) {
            updateQueue.offer(goldProductPrice);
        }
    }

    /**
     * 将待插入的黄金价格数据添加到插入队列中。
     */
    public void addInsertTask(GoldProductPrice goldProductPrice) {
        if (goldProductPrice != null) {
            insertQueue.offer(goldProductPrice);
        }
    }

    /**
     * 定时将内存队列中的数据批量写入数据库。
     */
    @Scheduled(fixedRate = 5000)
    public void flushToDatabase() {
        processInsertQueue();
        processUpdateQueue();
    }

    private void processInsertQueue() {
        if (insertQueue.isEmpty()) {
            return;
        }

        List<GoldProductPrice> insertsToProcess = new ArrayList<>();
        while (!insertQueue.isEmpty()) {
            insertsToProcess.add(insertQueue.poll());
        }

        if (CollectionUtils.isEmpty(insertsToProcess)) {
            return;
        }

        log.info("开始执行黄金价格批量插入任务，待处理任务数：{}", insertsToProcess.size());
        try {
            long startTime = System.currentTimeMillis();
            goldProductPriceService.batchInsertGoldProductPrice(insertsToProcess);
            long endTime = System.currentTimeMillis();
            log.info("成功批量插入 {} 条黄金价格数据，耗时 {} ms", insertsToProcess.size(), (endTime - startTime));
        } catch (Exception e) {
            log.error("批量插入黄金价格数据时发生异常", e);
        }
    }

    private void processUpdateQueue() {
        if (updateQueue.isEmpty()) {
            return;
        }

        List<GoldProductPrice> updatesToProcess = new ArrayList<>();
        while (!updateQueue.isEmpty()) {
            updatesToProcess.add(updateQueue.poll());
        }

        if (CollectionUtils.isEmpty(updatesToProcess)) {
            return;
        }

        log.info("开始执行黄金价格批量更新任务，待处理任务数：{}", updatesToProcess.size());
        try {
            long startTime = System.currentTimeMillis();
            goldProductPriceService.batchUpdateGoldProductPrice(updatesToProcess);
            long endTime = System.currentTimeMillis();
            log.info("成功批量更新 {} 条黄金价格数据，耗时 {} ms", updatesToProcess.size(), (endTime - startTime));
        } catch (Exception e) {
            log.error("批量更新黄金价格数据时发生异常", e);
        }
    }

    /**
     * 在应用关闭前，执行最后一次数据刷盘操作。
     * <p>
     * {@code @PreDestroy} 注解确保了在 Spring 容器销毁该 Bean 之前，
     * 此方法会被调用，从而防止内存队列中的数据丢失。
     * </p>
     */
    @PreDestroy
    public void shutdown() {
        log.info("应用关闭，执行最后的数据刷盘操作...");
        flushToDatabase();
        log.info("数据刷盘完成。");
    }
}