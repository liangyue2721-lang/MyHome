package com.make.stock.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockPriceUsMapper;
import com.make.stock.domain.StockPriceUs;
import com.make.stock.service.IStockPriceUsService;

/**
 * 美股阶段行情信息Service业务层处理
 *
 * @author erqi
 * @date 2025-10-26
 */
@Service
public class StockPriceUsServiceImpl implements IStockPriceUsService {

    @Autowired
    private StockPriceUsMapper stockPriceUsMapper;

    /**
     * 查询美股阶段行情信息
     *
     * @param id 美股阶段行情信息主键
     * @return 美股阶段行情信息
     */
    @Override
    public StockPriceUs selectStockPriceUsById(Long id) {
        return stockPriceUsMapper.selectStockPriceUsById(id);
    }

    /**
     * 查询美股阶段行情信息列表
     *
     * @param stockPriceUs 美股阶段行情信息
     * @return 美股阶段行情信息
     */
    @Override
    public List<StockPriceUs> selectStockPriceUsList(StockPriceUs stockPriceUs) {
        return stockPriceUsMapper.selectStockPriceUsList(stockPriceUs);
    }

    /**
     * 新增美股阶段行情信息
     *
     * @param stockPriceUs 美股阶段行情信息
     * @return 结果
     */
    @Override
    public int insertStockPriceUs(StockPriceUs stockPriceUs) {
        stockPriceUs.setCreateTime(DateUtils.getNowDate());
        return stockPriceUsMapper.insertStockPriceUs(stockPriceUs);
    }

    /**
     * 修改美股阶段行情信息
     *
     * @param stockPriceUs 美股阶段行情信息
     * @return 结果
     */
    @Override
    public int updateStockPriceUs(StockPriceUs stockPriceUs) {
        stockPriceUs.setUpdateTime(DateUtils.getNowDate());
        return stockPriceUsMapper.updateStockPriceUs(stockPriceUs);
    }

    /**
     * 批量删除美股阶段行情信息
     *
     * @param ids 需要删除的美股阶段行情信息主键
     * @return 结果
     */
    @Override
    public int deleteStockPriceUsByIds(Long[] ids) {
        return stockPriceUsMapper.deleteStockPriceUsByIds(ids);
    }

    /**
     * 删除美股阶段行情信息信息
     *
     * @param id 美股阶段行情信息主键
     * @return 结果
     */
    @Override
    public int deleteStockPriceUsById(Long id) {
        return stockPriceUsMapper.deleteStockPriceUsById(id);
    }
}
