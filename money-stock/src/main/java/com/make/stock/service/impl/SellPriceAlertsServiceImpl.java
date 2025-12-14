package com.make.stock.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.SellPriceAlertsMapper;
import com.make.stock.domain.SellPriceAlerts;
import com.make.stock.service.ISellPriceAlertsService;

/**
 * 卖出价位提醒Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class SellPriceAlertsServiceImpl implements ISellPriceAlertsService {
    @Autowired
    private SellPriceAlertsMapper sellPriceAlertsMapper;

    /**
     * 查询卖出价位提醒
     *
     * @param id 卖出价位提醒主键
     * @return 卖出价位提醒
     */
    @Override
    public SellPriceAlerts selectSellPriceAlertsById(Long id) {
        return sellPriceAlertsMapper.selectSellPriceAlertsById(id);
    }

    /**
     * 查询卖出价位提醒列表
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 卖出价位提醒
     */
    @Override
    public List<SellPriceAlerts> selectSellPriceAlertsList(SellPriceAlerts sellPriceAlerts) {
        return sellPriceAlertsMapper.selectSellPriceAlertsList(sellPriceAlerts);
    }

    /**
     * 新增卖出价位提醒
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 结果
     */
    @Override
    public int insertSellPriceAlerts(SellPriceAlerts sellPriceAlerts) {
        return sellPriceAlertsMapper.insertSellPriceAlerts(sellPriceAlerts);
    }

    /**
     * 修改卖出价位提醒
     *
     * @param sellPriceAlerts 卖出价位提醒
     * @return 结果
     */
    @Override
    public int updateSellPriceAlerts(SellPriceAlerts sellPriceAlerts) {
        return sellPriceAlertsMapper.updateSellPriceAlerts(sellPriceAlerts);
    }

    /**
     * 批量删除卖出价位提醒
     *
     * @param ids 需要删除的卖出价位提醒主键
     * @return 结果
     */
    @Override
    public int deleteSellPriceAlertsByIds(Long[] ids) {
        return sellPriceAlertsMapper.deleteSellPriceAlertsByIds(ids);
    }

    /**
     * 删除卖出价位提醒信息
     *
     * @param id 卖出价位提醒主键
     * @return 结果
     */
    @Override
    public int deleteSellPriceAlertsById(Long id) {
        return sellPriceAlertsMapper.deleteSellPriceAlertsById(id);
    }


    @Override
    public int updateLatestPrice(SellPriceAlerts sellPriceAlerts) {
        return sellPriceAlertsMapper.updateLatestPrice(sellPriceAlerts);
    }
}
