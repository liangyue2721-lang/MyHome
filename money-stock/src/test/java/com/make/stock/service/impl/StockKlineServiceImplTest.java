package com.make.stock.service.impl;

import com.make.stock.domain.StockKline;
import com.make.stock.domain.vo.StockRankingStat;
import com.make.stock.mapper.StockKlineMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StockKlineServiceImplTest {

    @Mock
    private StockKlineMapper stockKlineMapper;

    @InjectMocks
    private StockKlineServiceImpl stockKlineService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testSelectStockRanking_WeeklyGain() {
        // Mock Dates
        // Assume today is Monday 2023-10-30 for stability, or calculate dynamically.
        // Actually the service uses LocalDate.now(), which is hard to mock without PowerMock or refactoring.
        // But we can rely on the logic being relative to "now".

        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(java.time.DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate thisMonday = today.with(java.time.DayOfWeek.MONDAY);

        // Mock Data
        List<StockKline> klines = new ArrayList<>();

        // Stock A: Has data for last week and this week
        // Last Week: 100
        StockKline k1 = new StockKline();
        k1.setStockCode("A");
        k1.setTradeDate(Date.valueOf(lastMonday));
        k1.setClose(new BigDecimal("100"));
        klines.add(k1);

        // This Week: 110 (10% gain)
        StockKline k2 = new StockKline();
        k2.setStockCode("A");
        k2.setTradeDate(Date.valueOf(thisMonday));
        k2.setClose(new BigDecimal("110"));
        klines.add(k2);

        // Stock B: Only last week data (should be INCLUDED with 0% gain)
        StockKline k3 = new StockKline();
        k3.setStockCode("B");
        k3.setTradeDate(Date.valueOf(lastMonday));
        k3.setClose(new BigDecimal("100"));
        klines.add(k3);

        // Stock C: Only this week data (should be skipped)
        StockKline k4 = new StockKline();
        k4.setStockCode("C");
        k4.setTradeDate(Date.valueOf(thisMonday));
        k4.setClose(new BigDecimal("100"));
        klines.add(k4);

        when(stockKlineMapper.selectStockKlineByRange(any(), any())).thenReturn(klines);
        when(stockKlineMapper.selectStockNames()).thenReturn(Collections.emptyList());

        List<StockRankingStat> result = stockKlineService.selectStockRanking("WEEKLY_GAIN");

        // Expect A and B (B has 0% gain because no data for this week)
        assertEquals(2, result.size());
        // A should be first (10% gain)
        // B should be second (0% gain)
        // Note: Sort order depends on implementation (descending gain). 10% > 0%.
        assertEquals("A", result.get(0).getStockCode());
        assertEquals("B", result.get(1).getStockCode());
    }

    @Test
    void testSelectStockRanking_WeeklyGain_EmptyThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(java.time.DayOfWeek.MONDAY).minusWeeks(1);

        List<StockKline> klines = new ArrayList<>();

        // Stock B: Only last week data (should be INCLUDED with 0% gain after fix)
        // Before fix: Excluded
        StockKline k3 = new StockKline();
        k3.setStockCode("B");
        k3.setTradeDate(Date.valueOf(lastMonday));
        k3.setClose(new BigDecimal("100"));
        klines.add(k3);

        when(stockKlineMapper.selectStockKlineByRange(any(), any())).thenReturn(klines);
        when(stockKlineMapper.selectStockNames()).thenReturn(Collections.emptyList());

        List<StockRankingStat> result = stockKlineService.selectStockRanking("WEEKLY_GAIN");

        // After fix, expect 1 result (B with 0% gain)
        assertEquals(1, result.size());
        assertEquals("B", result.get(0).getStockCode());
        assertEquals(BigDecimal.ZERO, result.get(0).getPrevValue().subtract(result.get(0).getCurrentValue()));
    }
}
