package com.make.stock.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.WatchstockMapper;
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;

/**
 * 买入价位提醒Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class WatchstockServiceImpl implements IWatchstockService {
    @Autowired
    private WatchstockMapper watchstockMapper;

    /**
     * 查询买入价位提醒
     *
     * @param id 买入价位提醒主键
     * @return 买入价位提醒
     */
    @Override
    public Watchstock selectWatchstockById(Long id) {
        return watchstockMapper.selectWatchstockById(id);
    }

    /**
     * 查询买入价位提醒列表
     *
     * @param watchstock 买入价位提醒
     * @return 买入价位提醒
     */
    @Override
    public List<Watchstock> selectWatchstockList(Watchstock watchstock) {
        return watchstockMapper.selectWatchstockList(watchstock);
    }

    /**
     * 新增买入价位提醒
     *
     * @param watchstock 买入价位提醒
     * @return 结果
     */
    @Override
    public int insertWatchstock(Watchstock watchstock) {
        watchstock.setDate(DateUtils.getNowDate());
        return watchstockMapper.insertWatchstock(watchstock);
    }

    /**
     * 修改买入价位提醒
     *
     * @param watchstock 买入价位提醒
     * @return 结果
     */
    @Override
    public int updateWatchstock(Watchstock watchstock) {
        watchstock.setUpdatedAt(DateUtils.getNowDate());
        return watchstockMapper.updateWatchstock(watchstock);
    }

    /**
     * 批量更新买入价位提醒
     *
     * @param watchstockList 买入价位提醒列表
     * @return 结果
     */
    @Override
    public int updateWatchstockBatch(List<Watchstock> watchstockList) {
        // 设置更新时间
        for (Watchstock watchstock : watchstockList) {
            watchstock.setUpdatedAt(DateUtils.getNowDate());
        }
        return watchstockMapper.updateWatchstockBatchById(watchstockList);
    }

    /**
     * 批量删除买入价位提醒
     *
     * @param ids 需要删除的买入价位提醒主键
     * @return 结果
     */
    @Override
    public int deleteWatchstockByIds(Long[] ids) {
        return watchstockMapper.deleteWatchstockByIds(ids);
    }

    /**
     * 删除买入价位提醒信息
     *
     * @param id 买入价位提醒主键
     * @return 结果
     */
    @Override
    public int deleteWatchstockById(Long id) {
        return watchstockMapper.deleteWatchstockById(id);
    }

    /**
     * 查询买入价位提醒列表
     *
     * @return 买入价位提醒
     */
    @Override
    public List<Watchstock> getWatchstockAllList() {
        return watchstockMapper.getWatchstockAllList();
    }

    @Override
    public Watchstock getWatchStockByCode(String stockCode) {
        return watchstockMapper.getWatchStockByCode(stockCode);
    }
}
