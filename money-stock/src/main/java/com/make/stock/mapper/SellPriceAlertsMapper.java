package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.SellPriceAlerts;

/**
 * 卖出价位提醒Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface SellPriceAlertsMapper {
    /**
     * 查询卖出价位提醒
     *
     * @param id 卖出价位提醒主键
     * @return 卖出价位提醒
     */
    public SellPriceAlerts selectSellPriceAlertsById(Long id);

    /**
     * 查询卖出价位提醒列表
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 卖出价位提醒集合
     */
    public List<SellPriceAlerts> selectSellPriceAlertsList(SellPriceAlerts sellPriceAlerts);

    /**
     * 新增卖出价位提醒
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 结果
     */
    public int insertSellPriceAlerts(SellPriceAlerts sellPriceAlerts);

    /**
     * 修改卖出价位提醒
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 结果
     */
    public int updateSellPriceAlerts(SellPriceAlerts sellPriceAlerts);

    /**
     * 删除卖出价位提醒
     *
     * @param id 卖出价位提醒主键
     * @return 结果
     */
    public int deleteSellPriceAlertsById(Long id);

    /**
     * 批量删除卖出价位提醒
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSellPriceAlertsByIds(Long[] ids);

    /**
     * 更新卖出价位提醒中的最新价格
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 更新结果
     */
    public int updateLatestPrice(SellPriceAlerts sellPriceAlerts);
}
