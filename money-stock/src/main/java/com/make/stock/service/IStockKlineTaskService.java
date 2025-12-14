package com.make.stock.service;

import java.util.List;

import com.make.stock.domain .StockKlineTask;

/**
 * 股票K线数据任务Service接口
 *
 * @author erqi
 * @date 2025-11-03
 */
public interface IStockKlineTaskService {

    /**
     * 查询股票K线数据任务
     *
     * @param id 股票K线数据任务主键
     * @return 股票K线数据任务
     */
    public StockKlineTask selectStockKlineTaskById(String id);

    /**
     * 查询股票K线数据任务列表
     *
     * @param stockKlineTask 股票K线数据任务
     * @return 股票K线数据任务集合
     */
    public List<StockKlineTask> selectStockKlineTaskList(StockKlineTask stockKlineTask);

    /**
     * 新增股票K线数据任务
     *
     * @param stockKlineTask 股票K线数据任务
     * @return 结果
     */
    public int insertStockKlineTask(StockKlineTask stockKlineTask);

    /**
     * 修改股票K线数据任务
     *
     * @param stockKlineTask 股票K线数据任务
     * @return 结果
     */
    public int updateStockKlineTask(StockKlineTask stockKlineTask);

    /**
     * 批量删除股票K线数据任务
     *
     * @param ids 需要删除的股票K线数据任务主键集合
     * @return 结果
     */
    public int deleteStockKlineTaskByIds(String[] ids);

    /**
     * 删除股票K线数据任务信息
     *
     * @param id 股票K线数据任务主键
     * @return 结果
     */
    public int deleteStockKlineTaskById(Long id);

    List<StockKlineTask> getStockAllTask(int nodeId);

    void batchFinishTask(List<Long> successTasks);
}
