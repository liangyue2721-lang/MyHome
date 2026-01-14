package com.make.stock.controller;

import com.make.common.core.controller.BaseController;
import com.make.common.core.page.TableDataInfo;
import com.make.stock.domain.StockTaskStatus;
import com.make.stock.service.scheduled.stock.queue.StockTaskQueueService;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 股票刷新任务监控
 * Refactored to support multi-task per stock aggregation.
 */
@RestController
@RequestMapping("/monitor/stock-task")
public class StockTaskMonitorController extends BaseController {

    @Resource
    private StockTaskQueueService stockTaskQueueService;

    @Resource
    private IWatchstockService watchstockService;

    // Aggregation Priority Map (Lower number = Higher Priority)
    private static final Map<String, Integer> PRIORITY_MAP = new HashMap<>();
    static {
        PRIORITY_MAP.put(StockTaskStatus.STATUS_RUNNING, 1);
        PRIORITY_MAP.put(StockTaskStatus.STATUS_WAITING, 2);
        PRIORITY_MAP.put(StockTaskStatus.STATUS_FAILED, 3);
        PRIORITY_MAP.put(StockTaskStatus.STATUS_SKIPPED, 4);
        PRIORITY_MAP.put(StockTaskStatus.STATUS_SUCCESS, 5);
        // IDLE or others default to 99
    }

    /**
     * 获取股票刷新任务状态列表
     * 合并 DB 中的股票信息和 Redis 中的运行状态
     */
    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        // 1. Get all stocks from DB (Paginated)
        List<Watchstock> stocks = watchstockService.selectWatchstockList(null);

        // 2. Get all active statuses from Redis
        // Note: getAllStatuses() does lazy cleanup of expired keys.
        List<StockTaskStatus> allStatuses = stockTaskQueueService.getAllStatuses();

        // 3. Group by StockCode
        Map<String, List<StockTaskStatus>> stockTaskMap = allStatuses.stream()
                .collect(Collectors.groupingBy(StockTaskStatus::getStockCode));

        // 4. Merge & Aggregate
        List<Map<String, Object>> result = new ArrayList<>();
        for (Watchstock stock : stocks) {
            Map<String, Object> map = new HashMap<>();
            String code = stock.getCode();
            map.put("stockCode", code);
            map.put("stockName", stock.getName());

            List<StockTaskStatus> tasks = stockTaskMap.getOrDefault(code, new ArrayList<>());

            if (tasks.isEmpty()) {
                map.put("status", "IDLE");
            } else {
                // A. Calculate Aggregate Status
                StockTaskStatus primaryTask = tasks.stream()
                        .min(Comparator.comparingInt(this::getPriority))
                        .orElse(tasks.get(0));

                map.put("status", primaryTask.getStatus());

                // B. Find Latest Task (by lastUpdateTime)
                StockTaskStatus latestTask = tasks.stream()
                        .max(Comparator.comparingLong(StockTaskStatus::getLastUpdateTime))
                        .orElse(tasks.get(0));

                map.put("occupiedByNode", latestTask.getOccupiedByNode());
                map.put("occupiedTime", latestTask.getOccupiedTime());
                map.put("lastResult", latestTask.getLastResult());
                map.put("traceId", latestTask.getTraceId());
                map.put("lastUpdateTime", latestTask.getLastUpdateTime());

                // C. Optional: Include top N recent tasks for debugging
                List<StockTaskStatus> recentTasks = tasks.stream()
                        .sorted(Comparator.comparingLong(StockTaskStatus::getLastUpdateTime).reversed())
                        .limit(3)
                        .collect(Collectors.toList());
                map.put("tasks", recentTasks);
            }
            result.add(map);
        }

        return getDataTable(stocks, result);
    }

    private int getPriority(StockTaskStatus status) {
        return PRIORITY_MAP.getOrDefault(status.getStatus(), 99);
    }

    // Helper to return mapped list with pagination from original list
    private TableDataInfo getDataTable(List<Watchstock> originalList, List<Map<String, Object>> mappedList) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(mappedList);
        rspData.setTotal(new com.github.pagehelper.PageInfo(originalList).getTotal());
        return rspData;
    }
}
