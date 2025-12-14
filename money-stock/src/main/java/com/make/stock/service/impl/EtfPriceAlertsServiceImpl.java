package com.make.stock.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.EtfPriceAlertsMapper;
import com.make.stock.domain.EtfPriceAlerts;
import com.make.stock.service.IEtfPriceAlertsService;

/**
 * ETF买入卖出价格提醒Service业务层处理
 *
 * @author erqi
 * @date 2025-06-24
 */
@Service
public class EtfPriceAlertsServiceImpl implements IEtfPriceAlertsService {

    @Autowired
    private EtfPriceAlertsMapper etfPriceAlertsMapper;

    /**
     * 查询ETF买入卖出价格提醒
     *
     * @param id ETF买入卖出价格提醒主键
     * @return ETF买入卖出价格提醒
     */
    @Override
    public EtfPriceAlerts selectEtfPriceAlertsById(Long id) {
        return etfPriceAlertsMapper.selectEtfPriceAlertsById(id);
    }

    /**
     * 查询ETF买入卖出价格提醒列表
     *
     * @param etfPriceAlerts ETF买入卖出价格提醒
     * @return ETF买入卖出价格提醒
     */
    @Override
    public List<EtfPriceAlerts> selectEtfPriceAlertsList(EtfPriceAlerts etfPriceAlerts) {
        return etfPriceAlertsMapper.selectEtfPriceAlertsList(etfPriceAlerts);
    }

    /**
     * 新增ETF买入卖出价格提醒
     *
     * @param etfPriceAlerts ETF买入卖出价格提醒
     * @return 结果
     */
    @Override
    public int insertEtfPriceAlerts(EtfPriceAlerts etfPriceAlerts) {
        return etfPriceAlertsMapper.insertEtfPriceAlerts(etfPriceAlerts);
    }

    /**
     * 修改ETF买入卖出价格提醒
     *
     * @param etfPriceAlerts ETF买入卖出价格提醒
     * @return 结果
     */
    @Override
    public int updateEtfPriceAlerts(EtfPriceAlerts etfPriceAlerts) {
        return etfPriceAlertsMapper.updateEtfPriceAlerts(etfPriceAlerts);
    }

    /**
     * 批量删除ETF买入卖出价格提醒
     *
     * @param ids 需要删除的ETF买入卖出价格提醒主键
     * @return 结果
     */
    @Override
    public int deleteEtfPriceAlertsByIds(Long[] ids) {
        return etfPriceAlertsMapper.deleteEtfPriceAlertsByIds(ids);
    }

    /**
     * 删除ETF买入卖出价格提醒信息
     *
     * @param id ETF买入卖出价格提醒主键
     * @return 结果
     */
    @Override
    public int deleteEtfPriceAlertsById(Long id) {
        return etfPriceAlertsMapper.deleteEtfPriceAlertsById(id);
    }
}
