package com.make.stock.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockInfoDongfangMapper;
import com.make.stock.domain.StockInfoDongfang;
import com.make.stock.service.IStockInfoDongfangService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 东方财富股票Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class StockInfoDongfangServiceImpl implements IStockInfoDongfangService {

    @Autowired
    private StockInfoDongfangMapper stockInfoDongfangMapper;

    /**
     * 查询东方财富股票
     *
     * @param id 东方财富股票主键
     * @return 东方财富股票
     */
    @Override
    public StockInfoDongfang selectStockInfoDongfangById(Long id) {
        return stockInfoDongfangMapper.selectStockInfoDongfangById(id);
    }

    /**
     * 查询东方财富股票列表
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 东方财富股票
     */
    @Override
    public List<StockInfoDongfang> selectStockInfoDongfangList(StockInfoDongfang stockInfoDongfang) {
        return stockInfoDongfangMapper.selectStockInfoDongfangList(stockInfoDongfang);
    }

    /**
     * 新增东方财富股票
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 结果
     */
    @Override
    public int insertStockInfoDongfang(StockInfoDongfang stockInfoDongfang) {
        stockInfoDongfang.setCreateTime(DateUtils.getNowDate());
        return stockInfoDongfangMapper.insertStockInfoDongfang(stockInfoDongfang);
    }

    /**
     * 修改东方财富股票
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 结果
     */
    @Override
    public int updateStockInfoDongfang(StockInfoDongfang stockInfoDongfang) {
        stockInfoDongfang.setUpdateTime(DateUtils.getNowDate());
        return stockInfoDongfangMapper.updateStockInfoDongfang(stockInfoDongfang);
    }

    /**
     * 批量删除东方财富股票
     *
     * @param ids 需要删除的东方财富股票主键
     * @return 结果
     */
    @Override
    public int deleteStockInfoDongfangByIds(Long[] ids) {
        return stockInfoDongfangMapper.deleteStockInfoDongfangByIds(ids);
    }

    /**
     * 删除东方财富股票信息
     *
     * @param id 东方财富股票主键
     * @return 结果
     */
    @Override
    public int deleteStockInfoDongfangById(Long id) {
        return stockInfoDongfangMapper.deleteStockInfoDongfangById(id);
    }


    @Override
    public StockInfoDongfang selectByCode(String stockCode) {
        return stockInfoDongfangMapper.selectByCode(stockCode);
    }


    @Override
    public List<StockInfoDongfang> queryIDByCodes(List<String> stockCodes) {
        return stockInfoDongfangMapper.selectBatchByCodes(stockCodes);
    }


    @Override
    public List<StockInfoDongfang> queryAllStockInfoDongfang() {
        return stockInfoDongfangMapper.selectStockInfoDongfangList(new StockInfoDongfang());
    }

    /**
     * 批量更新东方财富
     *
     * @return 结果
     */
    @Override
    @Transactional
    public int batchUpdateStockInfoDongfang(List<StockInfoDongfang> stockInfoDongFangs) {
        return stockInfoDongfangMapper.batchUpdateStockInfoDongfang(stockInfoDongFangs);
    }

    /**
     * 批量新增东方财富
     *
     * @return 结果
     */
    @Override
    @Transactional
    public int batchInsertStockInfoDongfang(List<StockInfoDongfang> stockInfoDongFangs) {
        return stockInfoDongfangMapper.batchInsertStockInfoDongfang(stockInfoDongFangs);
    }


}
