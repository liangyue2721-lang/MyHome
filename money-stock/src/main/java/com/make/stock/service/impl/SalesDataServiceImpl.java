package com.make.stock.service.impl;

import java.util.List;
        import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.SalesDataMapper;
import com.make.stock.domain.SalesData;
import com.make.stock.service.ISalesDataService;

/**
 * 利润折线图数据Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class SalesDataServiceImpl implements ISalesDataService {

    @Autowired
    private SalesDataMapper salesDataMapper;

    /**
     * 查询利润折线图数据
     *
     * @param id 利润折线图数据主键
     * @return 利润折线图数据
     */
    @Override
    public SalesData selectSalesDataById(Long id) {
        return salesDataMapper.selectSalesDataById(id);
    }

    /**
     * 查询利润折线图数据列表
     *
     * @param salesData 利润折线图数据
     * @return 利润折线图数据
     */
    @Override
    public List<SalesData> selectSalesDataList(SalesData salesData) {
        return salesDataMapper.selectSalesDataList(salesData);
    }

    /**
     * 新增利润折线图数据
     *
     * @param salesData 利润折线图数据
     * @return 结果
     */
    @Override
    public int insertSalesData(SalesData salesData) {
                salesData.setCreateTime(DateUtils.getNowDate());
            return salesDataMapper.insertSalesData(salesData);
    }

    /**
     * 修改利润折线图数据
     *
     * @param salesData 利润折线图数据
     * @return 结果
     */
    @Override
    public int updateSalesData(SalesData salesData) {
                salesData.setUpdateTime(DateUtils.getNowDate());
        return salesDataMapper.updateSalesData(salesData);
    }

    /**
     * 批量删除利润折线图数据
     *
     * @param ids 需要删除的利润折线图数据主键
     * @return 结果
     */
    @Override
    public int deleteSalesDataByIds(Long[] ids) {
        return salesDataMapper.deleteSalesDataByIds(ids);
    }

    /**
     * 删除利润折线图数据信息
     *
     * @param id 利润折线图数据主键
     * @return 结果
     */
    @Override
    public int deleteSalesDataById(Long id) {
        return salesDataMapper.deleteSalesDataById(id);
    }

    /**
     * 查询今年数据
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public List<SalesData> selectSalesDataCurrentYear(Long userId) {
        return salesDataMapper.selectSalesDataCurrentYear(userId);
    }

    /**
     * 查询每年最新的一条数据
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public List<SalesData> selectSalesDataYearlyMax(Long userId) {
        return salesDataMapper.selectSalesDataYearlyMax(userId);
    }
}
