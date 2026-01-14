package com.make.stock.service.scheduled;

public interface IStockTaskService {

    /**
     * 执行股票 K 线数据更新任务。
     *
     * <p>处理流程：<br>
     * 1. 为每只股票创建并执行任务，更新其 K 线数据；<br>
     * 2. 每个任务内部处理可能抛出异常，但会在任务中记录日志，不影响其他任务；<br>
     * 3. 所有任务完成后方法结束。
     *
     * @param nodeId 节点ID，用于区分不同节点的任务
     * @throws RuntimeException 当执行过程中出现非预期异常时抛出
     */
//    void updateStockPriceTaskRunning(int nodeId);
//
//    void refreshWealthInMemoryMapEntries();

    void runStockKlineTask(int nodeId);

}
