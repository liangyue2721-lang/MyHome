package com.make.quartz.controller;

import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.common.core.page.TableDataInfo;
import com.make.quartz.domain.StockTaskStatus;
import com.make.quartz.service.stock.queue.StockTaskQueueService;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 股票刷新任务监控
 */
@RestController
@RequestMapping("/monitor/stock-task")
public class StockTaskMonitorController extends BaseController {

    @Resource
    private StockTaskQueueService stockTaskQueueService;

    @Resource
    private IWatchstockService watchstockService;

    /**
     * 获取股票刷新任务状态列表
     * 合并 DB 中的股票信息和 Redis 中的运行状态
     */
    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        // 1. Get all stocks from DB
        List<Watchstock> stocks = watchstockService.selectWatchstockList(null);

        // 2. Get all status from Redis
        List<StockTaskStatus> statuses = stockTaskQueueService.getAllStatuses();
        Map<String, StockTaskStatus> statusMap = statuses.stream()
                .collect(Collectors.toMap(StockTaskStatus::getStockCode, Function.identity(), (k1, k2) -> k1));

        // 3. Merge
        List<Map<String, Object>> result = new ArrayList<>();
        for (Watchstock stock : stocks) {
            Map<String, Object> map = new HashMap<>();
            map.put("stockCode", stock.getCode());
            map.put("stockName", stock.getName()); // Assuming name field exists, if not use code

            StockTaskStatus status = statusMap.get(stock.getCode());
            if (status != null) {
                map.put("status", status.getStatus());
                map.put("occupiedByNode", status.getOccupiedByNode());
                map.put("occupiedTime", status.getOccupiedTime());
                map.put("lastResult", status.getLastResult());
                map.put("traceId", status.getTraceId());
            } else {
                map.put("status", "IDLE");
            }
            result.add(map);
        }

        return getDataTable(stocks, result); // Use helper to wrap paginated result if stocks was paginated
        // Note: startPage() affects the next select. 'stocks' is a PageHelper collection.
        // We need to return the transformed list but keep pagination info from 'stocks'.
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
