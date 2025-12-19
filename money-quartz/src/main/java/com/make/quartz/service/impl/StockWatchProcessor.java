package com.make.quartz.service.impl;

import com.google.common.collect.Lists;
import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.domain.Watchstock;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.IWatchstockService;
import com.make.stock.util.KlineDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 关注股票数据处理器
 * <p>
 * 根据任务状态判定是处理今日更新还是历史批量更新，并组织完整的数据入库流程。
 * 是整个股票K线数据处理的核心协调者，负责串联各个组件完成数据处理任务。
 * </p>
 */
@Component
public class StockWatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(StockWatchProcessor.class);

    /**
     * 自选股更新器，用于更新自选股相关信息
     */
    @Resource
    private WatchStockUpdater watchStockUpdater;

    /**
     * 自选股服务接口，用于获取和更新自选股信息
     */
    @Resource
    private IWatchstockService watchStockService;

    /**
     * 入口：处理自选股任务（并发优化版）
     */
    public void processTask(String traceId) {
        long start = System.currentTimeMillis();
        List<Watchstock> watchstocks = watchStockService.selectWatchstockList(null);

        if (watchstocks == null || watchstocks.isEmpty()) {
            log.warn("自选股任务执行结束：没有需要更新的股票 TraceId={}", traceId);
            return;
        }

        log.info("=====【自选股任务开始】===== TraceId={} 总数：{}", traceId, watchstocks.size());

        ExecutorService executor = ThreadPoolUtil.getWatchStockExecutor();

        // 分批处理，每批 50 个 (根据需求调整)
        List<List<Watchstock>> partitions = Lists.partition(watchstocks, 50);

        int totalUpdated = 0;

        for (List<Watchstock> batch : partitions) {
            List<Future<Watchstock>> futures = new ArrayList<>();

            for (Watchstock watchstock : batch) {
                futures.add(executor.submit(() -> {
                    String code = watchstock.getCode();
                    String api = watchstock.getStockApi();
                    try {
                        log.debug("[请求股票数据] TraceId={} code={} , api={}", traceId, code, api);

                        StockRealtimeInfo stockRealtimeInfo = KlineDataFetcher.fetchRealtimeInfo(api);
                        if (stockRealtimeInfo != null) {
                            // 更新行情
                            watchstock.setNewPrice(BigDecimal.valueOf(stockRealtimeInfo.getPrice()));
                            watchstock.setHighPrice(BigDecimal.valueOf(stockRealtimeInfo.getHighPrice()));
                            watchstock.setLowPrice(BigDecimal.valueOf(stockRealtimeInfo.getLowPrice()));
                            watchstock.setPreviousClose(BigDecimal.valueOf(stockRealtimeInfo.getPrevClose()));

                            log.debug("[股票数据更新] TraceId={} code={} , 最新={} , 高={} , 低={}",
                                    traceId, code, stockRealtimeInfo.getPrice(), stockRealtimeInfo.getHighPrice(), stockRealtimeInfo.getLowPrice());

                            // 周高低逻辑
                            watchStockUpdater.updateWeekHighLowIfNeeded(watchstock);
                            return watchstock;
                        } else {
                            log.warn("[无行情数据] TraceId={} code={}", traceId, code);
                            return null;
                        }

                    } catch (Exception e) {
                        log.error("[处理异常] TraceId={} code={}, err={}", traceId, code, e);
                        return null;
                    }
                }));
            }

            // 收集当前批次结果
            List<Watchstock> batchUpdatedList = new ArrayList<>();
            for (Future<Watchstock> future : futures) {
                try {
                    Watchstock ws = future.get(30, TimeUnit.SECONDS); // 单个超时
                    if (ws != null) {
                        batchUpdatedList.add(ws);
                    }
                } catch (Exception e) {
                    log.error("[线程任务执行失败] TraceId={} err={}", traceId, e.getMessage());
                }
            }

            // 批次更新数据库
            if (!batchUpdatedList.isEmpty()) {
                log.info("批次更新数据库：TraceId={} 数量={}", traceId, batchUpdatedList.size());
                watchStockUpdater.batchUpdateWatchStock(batchUpdatedList);
                totalUpdated += batchUpdatedList.size();
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=====【自选股任务结束】===== TraceId={} 总耗时={} ms , 更新股票数量={}",
                traceId, cost, totalUpdated);
    }
}
