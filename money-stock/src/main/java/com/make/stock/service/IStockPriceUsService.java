package com.make.stock.service;

import java.util.List;

import com.make.stock.domain .StockPriceUs;

/**
 * 美股阶段行情信息Service接口
 *
 * @author erqi
 * @date 2025-10-26
 */
public interface IStockPriceUsService {

    /**
     * 查询美股阶段行情信息
     *
     * @param id 美股阶段行情信息主键
     * @return 美股阶段行情信息
     */
    public StockPriceUs selectStockPriceUsById(Long id);

    /**
     * 查询美股阶段行情信息列表
     *
     * @param stockPriceUs 美股阶段行情信息
     * @return 美股阶段行情信息集合
     */
    public List<StockPriceUs> selectStockPriceUsList(StockPriceUs stockPriceUs);

    /**
     * 新增美股阶段行情信息
     *
     * @param stockPriceUs 美股阶段行情信息
     * @return 结果
     */
    public int insertStockPriceUs(StockPriceUs stockPriceUs);

    /**
     * 修改美股阶段行情信息
     *
     * @param stockPriceUs 美股阶段行情信息
     * @return 结果
     */
    public int updateStockPriceUs(StockPriceUs stockPriceUs);

    /**
     * 批量删除美股阶段行情信息
     *
     * @param ids 需要删除的美股阶段行情信息主键集合
     * @return 结果
     */
    public int deleteStockPriceUsByIds(Long[] ids);

    /**
     * 删除美股阶段行情信息信息
     *
     * @param id 美股阶段行情信息主键
     * @return 结果
     */
    public int deleteStockPriceUsById(Long id);
}
