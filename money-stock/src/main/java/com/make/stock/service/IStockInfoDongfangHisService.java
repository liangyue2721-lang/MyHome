package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.StockInfoDongfangHis;

/**
 * 东方财富历史Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IStockInfoDongfangHisService {

    /**
     * 查询东方财富历史
     *
     * @param id 东方财富历史主键
     * @return 东方财富历史
     */
    public StockInfoDongfangHis selectStockInfoDongfangHisById(Long id);

    /**
     * 查询东方财富历史列表
     *
     * @param stockInfoDongfangHis 东方财富历史
     * @return 东方财富历史集合
     */
    public List<StockInfoDongfangHis> selectStockInfoDongfangHisList(StockInfoDongfangHis stockInfoDongfangHis);

    /**
     * 新增东方财富历史
     *
     * @param stockInfoDongfangHis 东方财富历史
     * @return 结果
     */
    public int insertStockInfoDongfangHis(StockInfoDongfangHis stockInfoDongfangHis);

    /**
     * 修改东方财富历史
     *
     * @param stockInfoDongfangHis 东方财富历史
     * @return 结果
     */
    public int updateStockInfoDongfangHis(StockInfoDongfangHis stockInfoDongfangHis);

    /**
     * 批量删除东方财富历史
     *
     * @param ids 需要删除的东方财富历史主键集合
     * @return 结果
     */
    public int deleteStockInfoDongfangHisByIds(Long[] ids);

    /**
     * 删除东方财富历史信息
     *
     * @param id 东方财富历史主键
     * @return 结果
     */
    public int deleteStockInfoDongfangHisById(Long id);

    /**
     * 批量新增东方财富历史
     *
     * @return 结果
     */
    public int batchInsertStockInfoDongfangHis(List<StockInfoDongfangHis> stockInfoDongfangHis);

    /**
     * 批量查询东方财富历史
     * @return 结果
     */
    public List<StockInfoDongfangHis>  findByCodeAndCreateTime(String code, String createTime);


}
