package com.make.stock.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockBigMoneyAlertMapper;
import com.make.stock.domain.StockBigMoneyAlert;
import com.make.stock.service.IStockBigMoneyAlertService;

/**
 * 大资金入场异动预警Service业务层处理
 *
 * @author erqi
 * @date 2026-01-27
 */
@Service
public class StockBigMoneyAlertServiceImpl implements IStockBigMoneyAlertService {

    @Autowired
    private StockBigMoneyAlertMapper stockBigMoneyAlertMapper;

    /**
     * 查询大资金入场异动预警
     *
     * @param id 大资金入场异动预警主键
     * @return 大资金入场异动预警
     */
    @Override
    public StockBigMoneyAlert selectStockBigMoneyAlertById(String id) {
        return stockBigMoneyAlertMapper.selectStockBigMoneyAlertById(id);
    }

    /**
     * 查询大资金入场异动预警列表
     *
     * @param stockBigMoneyAlert 大资金入场异动预警
     * @return 大资金入场异动预警
     */
    @Override
    public List<StockBigMoneyAlert> selectStockBigMoneyAlertList(StockBigMoneyAlert stockBigMoneyAlert) {
        return stockBigMoneyAlertMapper.selectStockBigMoneyAlertList(stockBigMoneyAlert);
    }

    /**
     * 新增大资金入场异动预警
     *
     * @param stockBigMoneyAlert 大资金入场异动预警
     * @return 结果
     */
    @Override
    public int insertStockBigMoneyAlert(StockBigMoneyAlert stockBigMoneyAlert) {
        stockBigMoneyAlert.setCreateTime(DateUtils.getNowDate());
        return stockBigMoneyAlertMapper.insertStockBigMoneyAlert(stockBigMoneyAlert);
    }

    /**
     * 修改大资金入场异动预警
     *
     * @param stockBigMoneyAlert 大资金入场异动预警
     * @return 结果
     */
    @Override
    public int updateStockBigMoneyAlert(StockBigMoneyAlert stockBigMoneyAlert) {
        stockBigMoneyAlert.setUpdateTime(DateUtils.getNowDate());
        return stockBigMoneyAlertMapper.updateStockBigMoneyAlert(stockBigMoneyAlert);
    }

    /**
     * 批量删除大资金入场异动预警
     *
     * @param ids 需要删除的大资金入场异动预警主键
     * @return 结果
     */
    @Override
    public int deleteStockBigMoneyAlertByIds(String[] ids) {
        return stockBigMoneyAlertMapper.deleteStockBigMoneyAlertByIds(ids);
    }

    /**
     * 删除大资金入场异动预警信息
     *
     * @param id 大资金入场异动预警主键
     * @return 结果
     */
    @Override
    public int deleteStockBigMoneyAlertById(String id) {
        return stockBigMoneyAlertMapper.deleteStockBigMoneyAlertById(id);
    }
}
