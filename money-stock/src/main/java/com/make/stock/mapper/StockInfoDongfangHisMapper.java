package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockInfoDongfangHis;
import org.apache.ibatis.annotations.Param;

/**
 * 东方财富历史Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface StockInfoDongfangHisMapper {

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
     * 删除东方财富历史
     *
     * @param id 东方财富历史主键
     * @return 结果
     */
    public int deleteStockInfoDongfangHisById(Long id);

    /**
     * 批量删除东方财富历史
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockInfoDongfangHisByIds(Long[] ids);

    int batchInsert(@Param("list") List<StockInfoDongfangHis> stockInfoDongfangHis);

    List<StockInfoDongfangHis> findByCodeAndCreateTime(String code, String createTime);
}
