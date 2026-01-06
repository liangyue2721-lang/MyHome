package com.make.stock.service.impl;

import java.util.Arrays;
import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockKlineTaskMapper;
import com.make.stock.domain.StockKlineTask;
import com.make.stock.service.IStockKlineTaskService;

/**
 * 股票K线数据任务Service业务层处理
 *
 * @author erqi
 * @date 2025-11-03
 */
@Service
public class StockKlineTaskServiceImpl implements IStockKlineTaskService {

    @Autowired
    private StockKlineTaskMapper stockKlineTaskMapper;

    /**
     * 查询股票K线数据任务
     *
     * @param id 股票K线数据任务主键
     * @return 股票K线数据任务
     */
    @Override
    public StockKlineTask selectStockKlineTaskById(String id) {
        return stockKlineTaskMapper.selectStockKlineTaskById(Long.valueOf(id));
    }

    /**
     * 查询股票K线数据任务列表
     *
     * @param stockKlineTask 股票K线数据任务
     * @return 股票K线数据任务
     */
    @Override
    public List<StockKlineTask> selectStockKlineTaskList(StockKlineTask stockKlineTask) {
        return stockKlineTaskMapper.selectStockKlineTaskList(stockKlineTask);
    }

    /**
     * 新增股票K线数据任务
     *
     * @param stockKlineTask 股票K线数据任务
     * @return 结果
     */
    @Override
    public int insertStockKlineTask(StockKlineTask stockKlineTask) {
        stockKlineTask.setCreateTime(DateUtils.getNowDate());
        return stockKlineTaskMapper.insertStockKlineTask(stockKlineTask);
    }

    /**
     * 修改股票K线数据任务
     *
     * @param stockKlineTask 股票K线数据任务
     * @return 结果
     */
    @Override
    public int updateStockKlineTask(StockKlineTask stockKlineTask) {
        stockKlineTask.setUpdateTime(DateUtils.getNowDate());
        return stockKlineTaskMapper.updateStockKlineTask(stockKlineTask);
    }

    /**
     * 批量删除股票K线数据任务
     *
     * @param ids 需要删除的股票K线数据任务主键
     * @return 结果
     */
    @Override
    public int deleteStockKlineTaskByIds(String[] ids) {
        try {
            Long[] longIds = Arrays.stream(ids)
                    .map(Long::valueOf)
                    .toArray(Long[]::new);
            return stockKlineTaskMapper.deleteStockKlineTaskByIds(longIds);
        } catch (NumberFormatException e) {
            // 处理数字格式异常
            throw new IllegalArgumentException("ID格式不正确，必须为数字", e);
        }
    }

    /**
     * 删除股票K线数据任务信息
     *
     * @param id 股票K线数据任务主键
     * @return 结果
     */
    @Override
    public int deleteStockKlineTaskById(Long id) {
        return stockKlineTaskMapper.deleteStockKlineTaskById(id);
    }

    @Override
    public List<StockKlineTask> getStockAllTask(int nodeId) {
        return stockKlineTaskMapper.getStockAllTask();
    }

    @Override
    public void batchFinishTask(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        stockKlineTaskMapper.batchFinishTask(taskIds);
    }
}
