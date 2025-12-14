package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.EtfPriceAlerts;

/**
 * ETF买入卖出价格提醒Mapper接口
 *
 * @author erqi
 * @date 2025-06-24
 */
public interface EtfPriceAlertsMapper {

    /**
     * 查询ETF买入卖出价格提醒
     *
     * @param id ETF买入卖出价格提醒主键
     * @return ETF买入卖出价格提醒
     */
    public EtfPriceAlerts selectEtfPriceAlertsById(Long id);

    /**
     * 查询ETF买入卖出价格提醒列表
     *
     * @param etfPriceAlerts ETF买入卖出价格提醒
     * @return ETF买入卖出价格提醒集合
     */
    public List<EtfPriceAlerts> selectEtfPriceAlertsList(EtfPriceAlerts etfPriceAlerts);

    /**
     * 新增ETF买入卖出价格提醒
     *
     * @param etfPriceAlerts ETF买入卖出价格提醒
     * @return 结果
     */
    public int insertEtfPriceAlerts(EtfPriceAlerts etfPriceAlerts);

    /**
     * 修改ETF买入卖出价格提醒
     *
     * @param etfPriceAlerts ETF买入卖出价格提醒
     * @return 结果
     */
    public int updateEtfPriceAlerts(EtfPriceAlerts etfPriceAlerts);

    /**
     * 删除ETF买入卖出价格提醒
     *
     * @param id ETF买入卖出价格提醒主键
     * @return 结果
     */
    public int deleteEtfPriceAlertsById(Long id);

    /**
     * 批量删除ETF买入卖出价格提醒
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteEtfPriceAlertsByIds(Long[] ids);
}
