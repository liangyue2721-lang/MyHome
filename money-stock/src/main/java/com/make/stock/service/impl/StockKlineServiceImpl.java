package com.make.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.domain.vo.StockRankingStat;
import com.make.stock.mapper.StockKlineMapper;
import com.make.stock.domain.StockKline;
import com.make.stock.service.IStockKlineService;

/**
 * 股票K线数据Service业务层处理
 *
 * @author erqi
 * @date 2025-11-03
 */
@Service
public class StockKlineServiceImpl implements IStockKlineService {

    @Autowired
    private StockKlineMapper stockKlineMapper;

    /**
     * 查询股票K线数据
     *
     * @param id 股票K线数据主键
     * @return 股票K线数据
     */
    @Override
    public StockKline selectStockKlineById(String id) {
        return stockKlineMapper.selectStockKlineById(id);
    }

    /**
     * 查询股票K线数据列表
     *
     * @param stockKline 股票K线数据
     * @return 股票K线数据
     */
    @Override
    public List<StockKline> selectStockKlineList(StockKline stockKline) {
        return stockKlineMapper.selectStockKlineList(stockKline);
    }

    /**
     * 新增股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    @Override
    public int insertStockKline(StockKline stockKline) {
        stockKline.setCreateTime(DateUtils.getNowDate());
        return stockKlineMapper.insertStockKline(stockKline);
    }

    /**
     * 修改股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    @Override
    public int updateStockKline(StockKline stockKline) {
        stockKline.setUpdateTime(DateUtils.getNowDate());
        return stockKlineMapper.updateStockKline(stockKline);
    }

    /**
     * 批量删除股票K线数据
     *
     * @param ids 需要删除的股票K线数据主键
     * @return 结果
     */
    @Override
    public int deleteStockKlineByIds(String[] ids) {
        return stockKlineMapper.deleteStockKlineByIds(ids);
    }

    /**
     * 删除股票K线数据信息
     *
     * @param id 股票K线数据主键
     * @return 结果
     */
    @Override
    public int deleteStockKlineById(String id) {
        return stockKlineMapper.deleteStockKlineById(id);
    }

    /**
     * 根据股票代码和交易日期判断是否存在记录
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日期
     * @return 是否存在记录
     */
    @Override
    public boolean existsByStockAndDate(String stockCode, Date tradeDate) {
        return stockKlineMapper.existsByStockAndDate(stockCode, tradeDate);
    }


    /**
     * 根据股票代码和交易日期更新股票K线数据
     *
     * @param stockKline 股票K线数据
     * @return 结果
     */
    @Override
    public int updateByStockCodeAndTradeDate(StockKline stockKline) {
        stockKline.setUpdateTime(DateUtils.getNowDate());
        return stockKlineMapper.updateByStockCodeAndTradeDate(stockKline);
    }

    /**
     * 批量插入或更新股票K线数据
     *
     * @param klines 股票K线数据列表
     * @return 影响的行数
     */
    @Override
    public int insertOrUpdateBatch(List<StockKline> klines) {
        return stockKlineMapper.insertOrUpdateBatch(klines);
    }

    @Override
    public void batchUpdateByStockCodeAndTradeDate(List<StockKline> updateList) {
        stockKlineMapper.insertOrUpdateBatch(updateList);
    }

    @Override
    public List<LocalDate> selectExistsDates(String stockCode, List<LocalDate> tradeDateList) {
        return stockKlineMapper.selectExistsDates(stockCode, tradeDateList);
    }

    @Override
    public List<StockKline> queryWeekAllStockKline(String stockCode, List<LocalDate> tradeDateList) {
        return stockKlineMapper.queryWeekAllStockKline(stockCode, tradeDateList);
    }

    @Override
    public List<StockRankingStat> selectStockRanking(String type) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        if (type.startsWith("WEEKLY")) {
            // For Weekly stats, we need at least 2 weeks of data
            // Fetch last 30 days to be safe and cover holidays/weekends
            startDate = endDate.minusDays(30);
        } else {
            // For Yearly stats, default to start of current year
            startDate = LocalDate.of(endDate.getYear(), 1, 1);
        }

        return calculateStockRanking(type, startDate, endDate);
    }

    private List<StockRankingStat> calculateStockRanking(String type, LocalDate startDate, LocalDate endDate) {
        boolean isYearlyType = type.startsWith("HIGH_VS_HIGH") || type.startsWith("LOW_VS_LOW")
                || type.startsWith("LATEST_VS_HIGH") || type.startsWith("LATEST_VS_LOW");
        boolean isWeeklyType = type.startsWith("WEEKLY");

        List<StockKline> currentData = stockKlineMapper.selectStockKlineByRange(startDate, endDate);

        List<StockKline> prevData = new ArrayList<>();
        if (isYearlyType) {
            // Fix: For yearly comparison, we want the FULL previous year (Jan 1 to Dec 31)
            LocalDate prevYearStart = LocalDate.of(startDate.getYear() - 1, 1, 1);
            LocalDate prevYearEnd = LocalDate.of(startDate.getYear() - 1, 12, 31);
            prevData = stockKlineMapper.selectStockKlineByRange(prevYearStart, prevYearEnd);
        }

        // Map Stock Names
        List<StockRankingStat> nameStats = stockKlineMapper.selectStockNames();
        Map<String, String> stockNames = new HashMap<>();
        for (StockRankingStat s : nameStats) {
            if (s.getStockCode() != null && s.getStockName() != null) {
                stockNames.put(s.getStockCode(), s.getStockName());
            }
        }

        Map<String, List<StockKline>> currentGrouped = currentData.stream().collect(Collectors.groupingBy(StockKline::getStockCode));
        Map<String, List<StockKline>> prevGrouped = prevData.stream().collect(Collectors.groupingBy(StockKline::getStockCode));

        List<RankingWrapper> wrappers = new ArrayList<>();

        for (String stockCode : currentGrouped.keySet()) {
            List<StockKline> currKlines = currentGrouped.get(stockCode);
            List<StockKline> prevKlines = prevGrouped.getOrDefault(stockCode, Collections.emptyList());

            if (currKlines.isEmpty()) continue;

            BigDecimal currentVal = null;
            BigDecimal prevVal = null;
            BigDecimal sortValue = null;

            // Safe calculation with null checks
            BigDecimal currMaxHigh = currKlines.stream()
                    .map(StockKline::getHigh)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

            BigDecimal currMinLow = currKlines.stream()
                    .map(StockKline::getLow)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

            BigDecimal currClose = currKlines.stream()
                    .max(Comparator.comparing(StockKline::getTradeDate))
                    .map(StockKline::getClose).orElse(BigDecimal.ZERO);

            if (isYearlyType) {
                 if (prevKlines.isEmpty()) continue;

                 BigDecimal prevMaxHigh = prevKlines.stream()
                         .map(StockKline::getHigh)
                         .filter(Objects::nonNull)
                         .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

                 BigDecimal prevMinLow = prevKlines.stream()
                         .map(StockKline::getLow)
                         .filter(Objects::nonNull)
                         .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

                 switch (type) {
                     case "HIGH_VS_HIGH":
                         currentVal = currMaxHigh;
                         prevVal = prevMaxHigh;
                         if (prevVal.compareTo(BigDecimal.ZERO) != 0) {
                             sortValue = currentVal.divide(prevVal, 4, RoundingMode.HALF_UP);
                         }
                         break;
                     case "LOW_VS_LOW":
                         currentVal = currMinLow;
                         prevVal = prevMinLow;
                         if (prevVal.compareTo(BigDecimal.ZERO) != 0) {
                             sortValue = currentVal.divide(prevVal, 4, RoundingMode.HALF_UP);
                         }
                         break;
                     case "LATEST_VS_HIGH":
                         currentVal = currClose;
                         prevVal = prevMaxHigh;
                         if (prevVal.compareTo(BigDecimal.ZERO) != 0) {
                             sortValue = currentVal.divide(prevVal, 4, RoundingMode.HALF_UP);
                         }
                         break;
                     case "LATEST_VS_LOW":
                         currentVal = currClose;
                         prevVal = prevMinLow;
                         if (prevVal.compareTo(BigDecimal.ZERO) != 0) {
                             sortValue = currentVal.divide(prevVal, 4, RoundingMode.HALF_UP);
                         }
                         break;
                 }
            } else if (isWeeklyType) {
                LocalDate baseDate = endDate;
                LocalDate currWeekStart = baseDate.with(DayOfWeek.MONDAY);
                LocalDate prevWeekStart = currWeekStart.minusWeeks(1);

                List<StockKline> thisWeekKlines = new ArrayList<>();
                List<StockKline> lastWeekKlines = new ArrayList<>();

                for (StockKline k : currKlines) {
                    LocalDate kDate = convertToLocalDate(k.getTradeDate());
                    if (!kDate.isBefore(currWeekStart)) {
                        thisWeekKlines.add(k);
                    } else if (!kDate.isBefore(prevWeekStart) && kDate.isBefore(currWeekStart)) {
                        lastWeekKlines.add(k);
                    }
                }

                if (lastWeekKlines.isEmpty() && thisWeekKlines.isEmpty()) continue;

                // Determine previous week's closing price (baseline)
                if (!lastWeekKlines.isEmpty()) {
                    prevVal = lastWeekKlines.stream()
                            .max(Comparator.comparing(StockKline::getTradeDate))
                            .map(StockKline::getClose)
                            .orElse(BigDecimal.ZERO);
                } else {
                    // Fallback: If no data for last week, use the preClose of the first day of this week
                    StockKline firstDayOfThisWeek = thisWeekKlines.stream()
                            .min(Comparator.comparing(StockKline::getTradeDate))
                            .orElse(null);
                    if (firstDayOfThisWeek != null && firstDayOfThisWeek.getPreClose() != null) {
                        prevVal = firstDayOfThisWeek.getPreClose();
                    } else if (firstDayOfThisWeek != null) {
                        // Fallback to open price if preClose is missing (e.g. new listing)
                        prevVal = firstDayOfThisWeek.getOpen();
                    } else {
                        prevVal = BigDecimal.ZERO;
                    }
                }

                if (prevVal == null || prevVal.compareTo(BigDecimal.ZERO) == 0) continue;

                // Determine current week's latest price
                if (!thisWeekKlines.isEmpty()) {
                    currentVal = thisWeekKlines.stream()
                            .max(Comparator.comparing(StockKline::getTradeDate))
                            .map(StockKline::getClose)
                            .orElse(BigDecimal.ZERO);
                } else {
                    // Fallback: If no data for this week (e.g. Monday morning), assume no change
                    currentVal = prevVal;
                }

                sortValue = currentVal.subtract(prevVal).divide(prevVal, 4, RoundingMode.HALF_UP);

            } else {
                switch (type) {
                    case "HIGH_VS_LATEST_HIGH":
                    case "HIGH_VS_LATEST_LOW":
                        currentVal = currClose;
                        prevVal = currMaxHigh;
                        if (prevVal.compareTo(BigDecimal.ZERO) != 0) {
                            sortValue = currentVal.divide(prevVal, 4, RoundingMode.HALF_UP);
                        }
                        break;
                    case "LOW_VS_LATEST_HIGH":
                    case "LOW_VS_LATEST_LOW":
                        currentVal = currClose;
                        prevVal = currMinLow;
                         if (prevVal.compareTo(BigDecimal.ZERO) != 0) {
                            sortValue = currentVal.divide(prevVal, 4, RoundingMode.HALF_UP);
                        }
                        break;
                }
            }

            if (sortValue != null) {
                StockRankingStat stat = new StockRankingStat();
                stat.setStockCode(stockCode);
                stat.setStockName(stockNames.getOrDefault(stockCode, stockCode));
                stat.setCurrentValue(currentVal);
                stat.setPrevValue(prevVal);
                wrappers.add(new RankingWrapper(stat, sortValue));
            }
        }

        boolean isAsc = "WEEKLY_LOSS".equals(type)
                     || "HIGH_VS_LATEST_LOW".equals(type)
                     || "LOW_VS_LATEST_LOW".equals(type);

        Comparator<RankingWrapper> comparator = Comparator.comparing(w -> w.sortValue);
        if (!isAsc) {
            comparator = comparator.reversed();
        }
        return wrappers.stream()
                .sorted(comparator)
                .limit(10)
                .map(w -> w.stat)
                .collect(Collectors.toList());
    }

    private static class RankingWrapper {
        StockRankingStat stat;
        BigDecimal sortValue;

        RankingWrapper(StockRankingStat stat, BigDecimal sortValue) {
            this.stat = stat;
            this.sortValue = sortValue;
        }
    }

    private LocalDate convertToLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
