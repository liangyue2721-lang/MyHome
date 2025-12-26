package com.make.quartz.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.quartz.mapper.StockRefreshExecuteRecordMapper;
import com.make.quartz.domain.StockRefreshExecuteRecord;
import com.make.quartz.service.IStockRefreshExecuteRecordService;

/**
 * 刷新任务执行记录Service业务层处理
 *
 * @author erqi
 * @date 2025-12-26
 */
@Service
public class StockRefreshExecuteRecordServiceImpl implements IStockRefreshExecuteRecordService {

    @Autowired
    private StockRefreshExecuteRecordMapper stockRefreshExecuteRecordMapper;

    /**
     * 查询刷新任务执行记录
     *
     * @param id 刷新任务执行记录主键
     * @return 刷新任务执行记录
     */
    @Override
    public StockRefreshExecuteRecord selectStockRefreshExecuteRecordById(String id) {
        return stockRefreshExecuteRecordMapper.selectStockRefreshExecuteRecordById(id);
    }

    /**
     * 查询刷新任务执行记录列表
     *
     * @param stockRefreshExecuteRecord 刷新任务执行记录
     * @return 刷新任务执行记录
     */
    @Override
    public List<StockRefreshExecuteRecord> selectStockRefreshExecuteRecordList(StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        return stockRefreshExecuteRecordMapper.selectStockRefreshExecuteRecordList(stockRefreshExecuteRecord);
    }

    /**
     * 新增刷新任务执行记录
     *
     * @param stockRefreshExecuteRecord 刷新任务执行记录
     * @return 结果
     */
    @Override
    public int insertStockRefreshExecuteRecord(StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        stockRefreshExecuteRecord.setCreateTime(DateUtils.getNowDate());
        return stockRefreshExecuteRecordMapper.insertStockRefreshExecuteRecord(stockRefreshExecuteRecord);
    }

    /**
     * 修改刷新任务执行记录
     *
     * @param stockRefreshExecuteRecord 刷新任务执行记录
     * @return 结果
     */
    @Override
    public int updateStockRefreshExecuteRecord(StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        stockRefreshExecuteRecord.setUpdateTime(DateUtils.getNowDate());
        return stockRefreshExecuteRecordMapper.updateStockRefreshExecuteRecord(stockRefreshExecuteRecord);
    }

    /**
     * 批量删除刷新任务执行记录
     *
     * @param ids 需要删除的刷新任务执行记录主键
     * @return 结果
     */
    @Override
    public int deleteStockRefreshExecuteRecordByIds(String[] ids) {
        return stockRefreshExecuteRecordMapper.deleteStockRefreshExecuteRecordByIds(ids);
    }

    /**
     * 删除刷新任务执行记录信息
     *
     * @param id 刷新任务执行记录主键
     * @return 结果
     */
    @Override
    public int deleteStockRefreshExecuteRecordById(String id) {
        return stockRefreshExecuteRecordMapper.deleteStockRefreshExecuteRecordById(id);
    }
}
