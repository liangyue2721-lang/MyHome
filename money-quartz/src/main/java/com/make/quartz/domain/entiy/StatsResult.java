package com.make.quartz.domain.entiy;

import java.math.BigDecimal;

public class StatsResult {
    public BigDecimal weekLow;
    public BigDecimal weekHigh;
    public BigDecimal yearLow;
    public BigDecimal yearHigh;

    // 私有化构造器，避免误用
    public StatsResult(BigDecimal weekLow, BigDecimal weekHigh,
                        BigDecimal yearLow, BigDecimal yearHigh) {
        this.weekLow = weekLow;
        this.weekHigh = weekHigh;
        this.yearLow = yearLow;
        this.yearHigh = yearHigh;
    }

    /**
     * 仅周统计
     */
    public static StatsResult ofWeek(BigDecimal weekLow, BigDecimal weekHigh) {
        return new StatsResult(weekLow, weekHigh, null, null);
    }

    /**
     * 仅年统计
     */
    public static StatsResult ofYear(BigDecimal yearLow, BigDecimal yearHigh) {
        return new StatsResult(null, null, yearLow, yearHigh);
    }

    /**
     * 周 + 年统计
     */
    public static StatsResult ofAll(BigDecimal weekLow, BigDecimal weekHigh,
                                                                 BigDecimal yearLow, BigDecimal yearHigh) {
        return new StatsResult(weekLow, weekHigh, yearLow, yearHigh);
    }
}
