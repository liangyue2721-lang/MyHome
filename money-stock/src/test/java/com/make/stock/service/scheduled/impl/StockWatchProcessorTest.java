package com.make.stock.service.scheduled.impl;

import com.make.common.constant.KafkaTopics;
import com.make.common.core.NodeRegistry;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockWatchProcessorTest {

    @InjectMocks
    private StockWatchProcessor stockWatchProcessor;

    @Mock
    private IWatchstockService watchStockService;

    @Mock
    private StockTaskQueueService stockTaskQueueService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private NodeRegistry nodeRegistry;

    @BeforeEach
    public void setUp() {
        // Ensure the processor is "running"
        ReflectionTestUtils.setField(stockWatchProcessor, "running", new AtomicBoolean(true));
    }

    @Test
    public void testSubmitTask() {
        String stockCode = "AAPL";
        stockWatchProcessor.submitTask(stockCode);

        // Verify active state refreshed
        verify(stockTaskQueueService).refreshActiveState(eq(stockCode), anyString());

        // Verify Redis Enqueue (Replaces Kafka)
        verify(stockTaskQueueService).enqueue(any());
        // Verify Kafka NOT sent
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    public void testRunWatchdog_Master_RestartInactive() {
        // Setup Master
        when(nodeRegistry.isMaster()).thenReturn(true);

        // Setup Stocks
        Watchstock w1 = new Watchstock(); w1.setCode("AAPL");
        Watchstock w2 = new Watchstock(); w2.setCode("GOOG");
        when(watchStockService.selectWatchstockList(null)).thenReturn(Arrays.asList(w1, w2));

        // Setup Active States: AAPL active, GOOG inactive
        when(stockTaskQueueService.checkActiveStates(anyList())).thenReturn(Arrays.asList(true, false));

        stockWatchProcessor.runWatchdog();

        // Verify GOOG restarted (Enqueued)
        verify(stockTaskQueueService, times(1)).enqueue(argThat(task -> "GOOG".equals(task.getStockCode())));

        // Verify AAPL NOT restarted
        // Only 1 enqueue total
        verify(stockTaskQueueService, times(1)).enqueue(any());
    }

    @Test
    public void testRunWatchdog_NotMaster() {
        when(nodeRegistry.isMaster()).thenReturn(false);

        stockWatchProcessor.runWatchdog();

        verify(watchStockService, never()).selectWatchstockList(any());
        verify(stockTaskQueueService, never()).enqueue(any());
    }
}
