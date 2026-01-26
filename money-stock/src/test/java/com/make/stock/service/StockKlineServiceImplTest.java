package com.make.stock.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.make.stock.domain.StockKline;
import com.make.stock.domain.vo.StockRankingStat;
import com.make.stock.mapper.StockKlineMapper;
import com.make.stock.service.impl.StockKlineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StockKlineServiceImplTest {

    @Mock
    private StockKlineMapper stockKlineMapper;

    @InjectMocks
    private StockKlineServiceImpl stockKlineService;

    @Test
    public void testSelectStockRanking_HighVsHigh() {
        // Setup - Simulate "Today" is end of 2024 to make logic predictable
        // However, the service uses LocalDate.now(), which I cannot easily mock without a clock injection.
        // So I will rely on "any(LocalDate.class)" for the generic call, but I know the logic uses now().
        // For this test, I'll update to use the single-arg method and match arguments leniently or infer dates.

        List<StockRankingStat> names = new ArrayList<>();
        StockRankingStat s = new StockRankingStat();
        s.setStockCode("001");
        s.setStockName("TestStock");
        names.add(s);

        when(stockKlineMapper.selectStockNames()).thenReturn(names);

        // Mock Data
        List<StockKline> currentData = new ArrayList<>();
        StockKline k1 = new StockKline();
        k1.setStockCode("001");
        k1.setHigh(new BigDecimal("200"));
        k1.setLow(new BigDecimal("150"));
        k1.setClose(new BigDecimal("180"));
        // Use a generic recent date
        k1.setTradeDate(java.sql.Date.valueOf(LocalDate.now()));
        currentData.add(k1);

        List<StockKline> prevData = new ArrayList<>();
        StockKline k2 = new StockKline();
        k2.setStockCode("001");
        k2.setHigh(new BigDecimal("100"));
        k2.setLow(new BigDecimal("50"));
        k2.setClose(new BigDecimal("80"));
        k2.setTradeDate(java.sql.Date.valueOf(LocalDate.now().minusYears(1)));
        prevData.add(k2);

        // Match any date range because exact dates depend on 'now'
        when(stockKlineMapper.selectStockKlineByRange(any(LocalDate.class), any(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate start = invocation.getArgument(0);
            LocalDate end = invocation.getArgument(1);
            if (start.getYear() == LocalDate.now().getYear()) {
                return currentData;
            } else {
                return prevData;
            }
        });

        // Execute
        List<StockRankingStat> result = stockKlineService.selectStockRanking("HIGH_VS_HIGH");

        // Verify
        assertEquals(1, result.size());
        assertEquals("001", result.get(0).getStockCode());
        assertEquals("TestStock", result.get(0).getStockName());
        // Current High 200, Prev High 100. Ratio 2.0000
        assertEquals(new BigDecimal("200"), result.get(0).getCurrentValue());
        assertEquals(new BigDecimal("100"), result.get(0).getPrevValue());
    }
}
