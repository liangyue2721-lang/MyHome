package com.make.stock.service.scheduled.stock;

/**
 * 自选股任务服务接口
 * 专门处理自选股相关的定时任务逻辑
 */
public interface WatchTaskService {
    void executeWatchTask(String traceId);
}
