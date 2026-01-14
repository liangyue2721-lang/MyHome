package com.make.stock.service.scheduled.impl;

import com.google.common.collect.Lists;
import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.domain.EtfData;
import com.make.stock.domain.dto.EtfRealtimeInfo;
import com.make.stock.service.IEtfDataService;
import com.make.stock.util.KlineDataFetcher;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


@Component
public class StockETFrocessor {

    private static final Logger log = LoggerFactory.getLogger(StockETFrocessor.class);

    @Resource
    private IEtfDataService etfDataService;

    /**
     * ETF 任务执行入口
     *
     * <p>职责说明：</p>
     * <ul>
     *     <li>批量抓取 ETF 实时行情数据</li>
     *     <li>使用线程池并发处理，提高网络 I/O 性能</li>
     *     <li>等待所有并发任务完成后执行批量更新，保证数据库I/O最小化</li>
     *     <li>异常隔离机制：单支数据失败不影响整体任务</li>
     *     <li>详细日志跟踪：支持任务级 TraceId，便于排查链路问题</li>
     * </ul>
     */
    public void processTask(String traceId) {
        long start = System.currentTimeMillis();

        log.info("=====【ETF 更新任务开始】TraceId={} =====", traceId);

        List<EtfData> etfDataList = etfDataService.selectEtfDataList(null);
        if (CollectionUtils.isEmpty(etfDataList)) {
            log.warn("[ETF] TraceId={} 无数据,跳过执行", traceId);
            return;
        }

        log.info("[ETF-Task] TraceId={} 待更新数量={}", traceId, etfDataList.size());

        ExecutorService executor = ThreadPoolUtil.getWatchStockExecutor();

        // 分批处理，每批 50 个
        List<List<EtfData>> partitions = Lists.partition(etfDataList, 50);

        int totalUpdated = 0;

        for (List<EtfData> batch : partitions) {
            List<Future<EtfData>> futures = new ArrayList<>();

            for (EtfData etfData : batch) {
                futures.add(executor.submit(() -> {
                    final String code = etfData.getEtfCode();
                    String api = etfData.getStockApi();
                    try {
                        log.debug("[ETF-请求行情] TraceId={} code={} api={}", traceId, code, api);

                        EtfRealtimeInfo info = KlineDataFetcher.fetchEtfRealtimeInfo(api);
                        if (info != null) {
                            EtfData mapped = EtfData.etfRealtimeInfoToEtfData(info);
                            mapped.setId(etfData.getId());
                            return mapped;
                        } else {
                            log.warn("[ETF-无行情数据] TraceId={} code={}", traceId, code);
                            return null;
                        }
                    } catch (Exception e) {
                        log.error("[ETF-处理异常] TraceId={} code={} err={}",
                                traceId, code, e);
                        return null;
                    }
                }));
            }

            // 收集当前批次结果
            List<EtfData> batchUpdatedList = new ArrayList<>();
            for (Future<EtfData> future : futures) {
                try {
                    EtfData data = future.get(10, TimeUnit.SECONDS); // 单个超时
                    if (data != null) {
                        batchUpdatedList.add(data);
                    }
                } catch (Exception e) {
                    log.error("[ETF-线程执行失败] TraceId={} err={}", traceId, e);
                }
            }

            // 批次更新数据库
            if (!batchUpdatedList.isEmpty()) {
                log.info("[ETF-批量更新开始] TraceId={} count={}", traceId, batchUpdatedList.size());
                etfDataService.batchUpdateEtfData(batchUpdatedList);
                log.info("[ETF-批量更新完成] TraceId={}", traceId);
                totalUpdated += batchUpdatedList.size();
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=====【ETF 更新结束】TraceId={} 耗时={}ms 成功={} =====",
                traceId, cost, totalUpdated);
    }
}
