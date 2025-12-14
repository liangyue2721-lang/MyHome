package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.Watchstock;

/**
 * 买入价位提醒Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IWatchstockService {
    /**
     * 查询买入价位提醒
     *
     * @param id 买入价位提醒主键
     * @return 买入价位提醒
     */
    public Watchstock selectWatchstockById(Long id);

    /**
     * 查询买入价位提醒列表
     *
     * @param watchstock 买入价位提醒
     * @return 买入价位提醒集合
     */
    public List<Watchstock> selectWatchstockList(Watchstock watchstock);

    /**
     * 新增买入价位提醒
     *
     * @param watchstock 买入价位提醒
     * @return 结果
     */
    public int insertWatchstock(Watchstock watchstock);

    /**
     * 修改买入价位提醒
     *
     * @param watchstock 买入价位提醒
     * @return 结果
     */
    public int updateWatchstock(Watchstock watchstock);

    /**
     * 批量更新买入价位提醒
     *
     * @param watchstockList 买入价位提醒列表
     * @return 结果
     */
    public int updateWatchstockBatch(List<Watchstock> watchstockList);

    /**
     * 批量删除买入价位提醒
     *
     * @param ids 需要删除的买入价位提醒主键集合
     * @return 结果
     */
    public int deleteWatchstockByIds(Long[] ids);

    /**
     * 删除买入价位提醒信息
     *
     * @param id 买入价位提醒主键
     * @return 结果
     */
    public int deleteWatchstockById(Long id);


    public List<Watchstock> getWatchstockAllList();


    public Watchstock getWatchStockByCode(String stockCode);
}