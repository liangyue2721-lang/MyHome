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
        // Setup
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

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
        k1.setTradeDate(java.sql.Date.valueOf(start));
        currentData.add(k1);

        List<StockKline> prevData = new ArrayList<>();
        StockKline k2 = new StockKline();
        k2.setStockCode("001");
        k2.setHigh(new BigDecimal("100"));
        k2.setLow(new BigDecimal("50"));
        k2.setClose(new BigDecimal("80"));
        k2.setTradeDate(java.sql.Date.valueOf(start.minusYears(1)));
        prevData.add(k2);

        when(stockKlineMapper.selectStockKlineByRange(start, end)).thenReturn(currentData);
        when(stockKlineMapper.selectStockKlineByRange(start.minusYears(1), end.minusYears(1))).thenReturn(prevData);

        // Execute
        List<StockRankingStat> result = stockKlineService.selectStockRanking("HIGH_VS_HIGH", start, end);

        // Verify
        assertEquals(1, result.size());
        assertEquals("001", result.get(0).getStockCode());
        assertEquals("TestStock", result.get(0).getStockName());
        // Current High 200, Prev High 100. Ratio 2.0000
        assertEquals(new BigDecimal("200"), result.get(0).getCurrentValue());
        assertEquals(new BigDecimal("100"), result.get(0).getPrevValue());
    }
}
