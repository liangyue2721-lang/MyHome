package com.make.stock.service.impl;

import java.util.Collections;
import java.util.List;
        import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockInfoDongfangHisMapper;
import com.make.stock.domain.StockInfoDongfangHis;
import com.make.stock.service.IStockInfoDongfangHisService;

/**
 * 东方财富历史Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class StockInfoDongfangHisServiceImpl implements IStockInfoDongfangHisService {

    @Autowired
    private StockInfoDongfangHisMapper stockInfoDongfangHisMapper;

    /**
     * 查询东方财富历史
     *
     * @param id 东方财富历史主键
     * @return 东方财富历史
     */
    @Override
    public StockInfoDongfangHis selectStockInfoDongfangHisById(Long id) {
        return stockInfoDongfangHisMapper.selectStockInfoDongfangHisById(id);
    }

    /**
     * 查询东方财富历史列表
     *
     * @param stockInfoDongfangHis 东方财富历史
     * @return 东方财富历史
     */
    @Override
    public List<StockInfoDongfangHis> selectStockInfoDongfangHisList(StockInfoDongfangHis stockInfoDongfangHis) {
        return stockInfoDongfangHisMapper.selectStockInfoDongfangHisList(stockInfoDongfangHis);
    }

    /**
     * 新增东方财富历史
     *
     * @param stockInfoDongfangHis 东方财富历史
     * @return 结果
     */
    @Override
    public int insertStockInfoDongfangHis(StockInfoDongfangHis stockInfoDongfangHis) {
                stockInfoDongfangHis.setCreateTime(DateUtils.getNowDate());
            return stockInfoDongfangHisMapper.insertStockInfoDongfangHis(stockInfoDongfangHis);
    }

    /**
     * 修改东方财富历史
     *
     * @param stockInfoDongfangHis 东方财富历史
     * @return 结果
     */
    @Override
    public int updateStockInfoDongfangHis(StockInfoDongfangHis stockInfoDongfangHis) {
                stockInfoDongfangHis.setUpdateTime(DateUtils.getNowDate());
        return stockInfoDongfangHisMapper.updateStockInfoDongfangHis(stockInfoDongfangHis);
    }

    /**
     * 批量删除东方财富历史
     *
     * @param ids 需要删除的东方财富历史主键
     * @return 结果
     */
    @Override
    public int deleteStockInfoDongfangHisByIds(Long[] ids) {
        return stockInfoDongfangHisMapper.deleteStockInfoDongfangHisByIds(ids);
    }

    /**
     * 删除东方财富历史信息
     *
     * @param id 东方财富历史主键
     * @return 结果
     */
    @Override
    public int deleteStockInfoDongfangHisById(Long id) {
        return stockInfoDongfangHisMapper.deleteStockInfoDongfangHisById(id);
    }


    @Override
    public int batchInsertStockInfoDongfangHis(List<StockInfoDongfangHis> stockInfoDongfangHis) {
        return stockInfoDongfangHisMapper.batchInsert(stockInfoDongfangHis);
    }

    @Override
    public List<StockInfoDongfangHis> findByCodeAndCreateTime(String code, String createTime) {
        return stockInfoDongfangHisMapper.findByCodeAndCreateTime(code, createTime);
    }
}
