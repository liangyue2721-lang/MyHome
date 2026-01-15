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
     * 优化后：分页查 Redis 状态，再反查 DB 补充信息
     */
    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list(Integer pageNum, Integer pageSize) {
        if (pageNum == null) pageNum = 1;
        if (pageSize == null) pageSize = 10;

        // 1. Get paginated statuses from Redis directly
        List<StockTaskStatus> taskList = stockTaskQueueService.getStatusesPaginated(pageNum, pageSize);
        long total = stockTaskQueueService.getTotalStatusCount();

        if (taskList.isEmpty()) {
            return getDataTable(new ArrayList<>(), total);
        }

        // 2. Collect StockCodes
        Set<String> stockCodes = taskList.stream()
                .map(StockTaskStatus::getStockCode)
                .collect(Collectors.toSet());

        // 3. Bulk Fetch Stock Info from DB (Optimization: Map code -> name)
        // Assume watchstockService has a method to get map, or we iterate (less efficient but okay for 10 items)
        Map<String, String> nameMap = new HashMap<>();
        for (String code : stockCodes) {
            Watchstock ws = watchstockService.getWatchStockByCode(code);
            if (ws != null) {
                nameMap.put(code, ws.getName());
            } else {
                nameMap.put(code, "Unknown/Deleted");
            }
        }

        // 4. Build Result
        List<Map<String, Object>> result = new ArrayList<>();
        for (StockTaskStatus task : taskList) {
            Map<String, Object> map = new HashMap<>();
            map.put("stockCode", task.getStockCode());
            map.put("stockName", nameMap.get(task.getStockCode()));
            map.put("status", task.getStatus());
            map.put("occupiedByNode", task.getOccupiedByNode());
            map.put("occupiedTime", task.getOccupiedTime());
            map.put("lastResult", task.getLastResult());
            map.put("traceId", task.getTraceId());
            map.put("lastUpdateTime", task.getLastUpdateTime());
            result.add(map);
        }

        return getDataTable(result, total);
    }

    private TableDataInfo getDataTable(List<Map<String, Object>> rows, long total) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(rows);
        rspData.setTotal(total);
        return rspData;
    }

    private int getPriority(StockTaskStatus status) {
        return PRIORITY_MAP.getOrDefault(status.getStatus(), 99);
    }

}
