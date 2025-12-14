package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockKlineTask;
import org.apache.ibatis.annotations.Param;

/**
 * 股票K线数据任务Mapper接口
 *
 * @author erqi
 * @date 2025-11-03
 */
public interface StockKlineTaskMapper {

    /**
     * 查询股票K线数据任务
     *
     * @param id 股票K线数据任务主键
     * @return 股票K线数据任务
     */
    public StockKlineTask selectStockKlineTaskById(Long id);

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
     * 删除股票K线数据任务
     *
     * @param id 股票K线数据任务主键
     * @return 结果
     */
    public int deleteStockKlineTaskById(Long id);

    /**
     * 批量删除股票K线数据任务
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockKlineTaskByIds(Long[] ids);

    List<StockKlineTask> getStockAllTask(@Param("nodeId") int nodeId);

    void batchFinishTask(@Param("list") List<Long> taskIds);
}
