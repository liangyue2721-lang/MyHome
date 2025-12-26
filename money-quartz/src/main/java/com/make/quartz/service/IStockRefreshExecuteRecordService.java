package com.make.quartz.service;

import java.util.List;

import com.make.quartz.domain.StockRefreshExecuteRecord;

/**
 * 刷新任务执行记录Service接口
 *
 * @author erqi
 * @date 2025-12-26
 */
public interface IStockRefreshExecuteRecordService {

    /**
     * 查询刷新任务执行记录
     *
     * @param id 刷新任务执行记录主键
     * @return 刷新任务执行记录
     */
    public StockRefreshExecuteRecord selectStockRefreshExecuteRecordById(String id);

    /**
     * 查询刷新任务执行记录列表
     *
     * @param stockRefreshExecuteRecord 刷新任务执行记录
     * @return 刷新任务执行记录集合
     */
    public List<StockRefreshExecuteRecord> selectStockRefreshExecuteRecordList(StockRefreshExecuteRecord stockRefreshExecuteRecord);

    /**
     * 新增刷新任务执行记录
     *
     * @param stockRefreshExecuteRecord 刷新任务执行记录
     * @return 结果
     */
    public int insertStockRefreshExecuteRecord(StockRefreshExecuteRecord stockRefreshExecuteRecord);

    /**
     * 修改刷新任务执行记录
     *
     * @param stockRefreshExecuteRecord 刷新任务执行记录
     * @return 结果
     */
    public int updateStockRefreshExecuteRecord(StockRefreshExecuteRecord stockRefreshExecuteRecord);

    /**
     * 批量删除刷新任务执行记录
     *
     * @param ids 需要删除的刷新任务执行记录主键集合
     * @return 结果
     */
    public int deleteStockRefreshExecuteRecordByIds(String[] ids);

    /**
     * 删除刷新任务执行记录信息
     *
     * @param id 刷新任务执行记录主键
     * @return 结果
     */
    public int deleteStockRefreshExecuteRecordById(String id);
}
