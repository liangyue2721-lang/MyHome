package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockInfoDongfang;
import org.apache.ibatis.annotations.Param;

/**
 * 东方财富股票Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface StockInfoDongfangMapper {

    /**
     * 查询东方财富股票
     *
     * @param id 东方财富股票主键
     * @return 东方财富股票
     */
    public StockInfoDongfang selectStockInfoDongfangById(Long id);

    /**
     * 查询东方财富股票列表
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 东方财富股票集合
     */
    public List<StockInfoDongfang> selectStockInfoDongfangList(StockInfoDongfang stockInfoDongfang);

    /**
     * 新增东方财富股票
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 结果
     */
    public int insertStockInfoDongfang(StockInfoDongfang stockInfoDongfang);

    /**
     * 修改东方财富股票
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 结果
     */
    public int updateStockInfoDongfang(StockInfoDongfang stockInfoDongfang);

    /**
     * 删除东方财富股票
     *
     * @param id 东方财富股票主键
     * @return 结果
     */
    public int deleteStockInfoDongfangById(Long id);

    /**
     * 批量删除东方财富股票
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockInfoDongfangByIds(Long[] ids);


    StockInfoDongfang selectByCode(String stockCode);


    List<StockInfoDongfang> selectBatchByCodes(@Param("codes") List<String> stockCodes);

    /**
     * 批量更新东方财富
     *
     * @return 结果
     */
    public int batchUpdateStockInfoDongfang(@Param("list") List<StockInfoDongfang> stockInfoDongFangs);

    /**
     * 批量新增东方财富
     *
     * @return 结果
     */
    public int batchInsertStockInfoDongfang(@Param("list") List<StockInfoDongfang> stockInfoDongFangs);

}
