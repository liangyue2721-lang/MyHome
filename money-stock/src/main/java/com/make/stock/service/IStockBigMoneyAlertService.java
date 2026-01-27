package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.StockBigMoneyAlert;

/**
 * 大资金入场异动预警Service接口
 *
 * @author erqi
 * @date 2026-01-27
 */
public interface IStockBigMoneyAlertService {

    /**
     * 查询大资金入场异动预警
     *
     * @param id 大资金入场异动预警主键
     * @return 大资金入场异动预警
     */
    public StockBigMoneyAlert selectStockBigMoneyAlertById(String id);

    /**
     * 查询大资金入场异动预警列表
     *
     * @param stockBigMoneyAlert 大资金入场异动预警
     * @return 大资金入场异动预警集合
     */
    public List<StockBigMoneyAlert> selectStockBigMoneyAlertList(StockBigMoneyAlert stockBigMoneyAlert);

    /**
     * 新增大资金入场异动预警
     *
     * @param stockBigMoneyAlert 大资金入场异动预警
     * @return 结果
     */
    public int insertStockBigMoneyAlert(StockBigMoneyAlert stockBigMoneyAlert);

    /**
     * 修改大资金入场异动预警
     *
     * @param stockBigMoneyAlert 大资金入场异动预警
     * @return 结果
     */
    public int updateStockBigMoneyAlert(StockBigMoneyAlert stockBigMoneyAlert);

    /**
     * 批量删除大资金入场异动预警
     *
     * @param ids 需要删除的大资金入场异动预警主键集合
     * @return 结果
     */
    public int deleteStockBigMoneyAlertByIds(String[] ids);

    /**
     * 删除大资金入场异动预警信息
     *
     * @param id 大资金入场异动预警主键
     * @return 结果
     */
    public int deleteStockBigMoneyAlertById(String id);
}
