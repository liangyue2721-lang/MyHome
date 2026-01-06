package com.make.stock.domain.vo;

import java.math.BigDecimal;

/**
 * Stock Ranking Statistics VO
 */
public class StockRankingStat {
    private String stockCode;
    private String stockName;
    private BigDecimal currentValue;
    private BigDecimal prevValue;

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(BigDecimal prevValue) {
        this.prevValue = prevValue;
    }
}
