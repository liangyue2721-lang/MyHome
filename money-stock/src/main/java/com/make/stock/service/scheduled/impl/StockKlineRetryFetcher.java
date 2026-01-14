package com.make.stock.service.scheduled.impl;

import com.make.stock.domain.KlineData;
import com.make.stock.util.KlineDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 股票K线数据重试获取器
 * <p>
 * 负责通过KlineDataFetcher获取股票K线数据，并在获取失败时实现重试机制，
 * 提高数据获取的成功率和稳定性。
 * </p>
 */
@Component
public class StockKlineRetryFetcher {

    private static final Logger log = LoggerFactory.getLogger(StockKlineRetryFetcher.class);

    /**
     * 最大重试次数
     * <p>设置为30次，意味着在最坏情况下会尝试30次获取数据</p>
     */
    private static final int MAX_RETRIES = 30;
    
    /**
     * 重试间隔时间（毫秒）
     * <p>设置为3分钟，生产环境中可以通过配置文件进行调整</p>
     */
    private static final long RETRY_DELAY_MS = 3 * 60_000L;

    /**
     * 带重试机制的数据获取方法
     * <p>
     * 通过KlineDataFetcher获取股票K线数据，如果获取失败或数据为空，
     * 会按照预设的最大重试次数和重试间隔进行重试。
     * </p>
     *
     * @param stockCode 股票代码
     * @param market    市场代码
     * @return 获取到的K线数据列表，获取失败返回null
     */
    public List<KlineData> fetchWithRetry(String stockCode, String market) {
        // 初始化K线数据列表
        List<KlineData> klineData = null;

        // 按照最大重试次数进行循环尝试
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // 调用KlineDataFetcher获取K线数据
                klineData = KlineDataFetcher.fetchKlineData(stockCode, market);
                // 如果成功获取到非空数据，则直接返回
                if (klineData != null && !klineData.isEmpty()) {
                    return klineData;
                } else {
                    // 如果获取到的数据为空，记录警告日志
                    log.warn("股票 {} 数据为空 (第 {} 次尝试)", stockCode, attempt);
                }
            } catch (Exception e) {
                // 如果获取过程中发生异常，记录警告日志
                log.warn("获取股票 {} 数据异常 (第 {} 次): {}", stockCode, attempt, e.getMessage());
            }

            // 如果还未达到最大重试次数，则等待一段时间后继续重试
            if (attempt < MAX_RETRIES) {
                try {
                    // 线程休眠指定时间后继续重试
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    // 如果线程休眠被中断，则记录警告日志并退出重试循环
                    Thread.currentThread().interrupt();
                    log.warn("重试等待被中断，停止后续重试：{}", stockCode);
                    break;
                }
            }
        }

        // 返回最终获取到的数据（可能为null）
        return klineData;
    }
}