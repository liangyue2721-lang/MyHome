package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.Watchstock;
import org.apache.ibatis.annotations.Param;

/**
 * 买入价位提醒Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface WatchstockMapper {
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
     * 删除买入价位提醒
     *
     * @param id 买入价位提醒主键
     * @return 结果
     */
    public int deleteWatchstockById(Long id);

    /**
     * 批量删除买入价位提醒
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWatchstockByIds(Long[] ids);

    /**
     * 批量更新买入价位提醒
     *
     * @param watchstockList 买入价位提醒列表
     * @return 结果
     */
    public int updateWatchstockBatchById(@Param("list") List<Watchstock> watchstockList);

    /**
     * 查询所有买入价位提醒列表
     *
     * @return 买入价位提醒集合
     */
    public List<Watchstock> getWatchstockAllList();

    Watchstock getWatchStockByCode(@Param("stockCode") String stockCode);
}
