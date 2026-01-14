package com.make.stock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stock Quartz Configuration Properties
 * Replicated from money-quartz for decoupling.
 */
@Component("stockProperties")
@ConfigurationProperties(prefix = "quartz")
public class StockProperties {

    /**
     * Stock Poll Workers
     */
    private int stockPollWorkers = 5;

    public int getStockPollWorkers() {
        return stockPollWorkers;
    }

    public void setStockPollWorkers(int stockPollWorkers) {
        this.stockPollWorkers = stockPollWorkers;
    }

    // Add other fields if needed by Stock module
}
