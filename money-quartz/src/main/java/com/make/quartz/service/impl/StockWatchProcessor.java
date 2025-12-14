package com.make.quartz.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.make.common.utils.DateUtils;
import com.make.common.utils.ThreadPoolUtil;
import com.make.quartz.util.HttpUtil;
import com.make.stock.domain.KlineData;
import com.make.stock.domain.StockKline;
import com.make.stock.domain.StockKlineTask;
import com.make.stock.domain.Watchstock;
import com.make.stock.domain.dto.StockInfoDongFangChain;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.service.IStockKlineService;
import com.make.stock.service.IStockKlineTaskService;
import com.make.stock.service.IWatchstockService;
import com.make.stock.util.KlineDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
     * 新方法：：使用 ObjectMapper 解析 JSON 数据
     * ObjectMapper mapper = new ObjectMapper();
     * StockRealtimeInfo info = mapper.readValue(stdout, StockRealtimeInfo.class);
     * <p>
     * System.out.println("代码: " + info.getStockCode());
     * System.out.println("名称: " + info.getCompanyName());
     * System.out.println("最新价: " + info.getPrice());
     * System.out.println("量比: " + info.getVolumeRatio());
     */
    public void processTask() {
        long start = System.currentTimeMillis();
        List<Watchstock> watchstocks = watchStockService.selectWatchstockList(null);

        if (watchstocks == null || watchstocks.isEmpty()) {
            log.warn("自选股任务执行结束：没有需要更新的股票");
            return;
        }

        log.info("=====【自选股任务开始】===== 总数：{}", watchstocks.size());

        // ⭐ 并发更新容器，必须线程安全
        List<Watchstock> updatedList = Collections.synchronizedList(new ArrayList<>());

        // ⭐ 获取线程池
        ExecutorService executor = ThreadPoolUtil.getWatchStockExecutor();

        List<Future<?>> futures = new ArrayList<>();

        for (Watchstock watchstock : watchstocks) {
            futures.add(executor.submit(() -> {
                String code = watchstock.getCode();
                String api = watchstock.getStockApi();

                try {
                    // 日志：请求
                    log.debug("[请求股票数据] code={} , api={}", code, api);

//                    JsonNode jsonNode = HttpUtil.fetchStockData(api);
//                    StockInfoDongFangChain parse = StockInfoDongFangChain.parse2(jsonNode);
                    StockRealtimeInfo stockRealtimeInfo = KlineDataFetcher.fetchRealtimeInfo(api);
                    // 更新行情
                    watchstock.setNewPrice(BigDecimal.valueOf(stockRealtimeInfo.getPrice()));
//                    watchstock.setHighPrice(BigDecimal.valueOf(parse.getHighPrice()));
//                    watchstock.setLowPrice(BigDecimal.valueOf(parse.getLowPrice()));
                    watchstock.setHighPrice(BigDecimal.valueOf(stockRealtimeInfo.getHighPrice()));
                    watchstock.setLowPrice(BigDecimal.valueOf(stockRealtimeInfo.getLowPrice()));

                    log.debug("[股票数据更新] code={} , 最新={} , 高={} , 低={}",
                            code, stockRealtimeInfo.getPrice(), stockRealtimeInfo.getHighPrice(), stockRealtimeInfo.getLowPrice());

                    // 周高低逻辑
                    watchStockUpdater.updateWeekHighLowIfNeeded(watchstock);

                    // ⭐ 加入线程安全容器
                    updatedList.add(watchstock);
                } catch (Exception e) {
                    log.error("[处理异常] code={}, err={}", code, e.getMessage(), e);
                }
            }));
        }

        // ⭐ 等待所有任务完成（阻塞）
        for (Future<?> future : futures) {
            try {
                future.get();   // 必须等待
            } catch (Exception e) {
                log.error("[线程任务执行失败] err={}", e.getMessage());
            }
        }

        // ⭐ 批量 DB 更新（只执行一次）
        if (!updatedList.isEmpty()) {
            log.info("批量更新数据库开始：更新数量={}", updatedList.size());
            watchStockUpdater.batchUpdateWatchStock(updatedList);
            log.info("批量更新数据库完成");
        } else {
            log.warn("没有需要批量更新的自选股数据");
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=====【自选股任务结束】===== 总耗时={} ms , 更新股票数量={}",
                cost, updatedList.size());
    }
}
