package com.make.stock.service;

import java.util.List;

import com.make.stock.domain .SalesData;

/**
 * 利润折线图数据Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface ISalesDataService {

    /**
     * 查询利润折线图数据
     *
     * @param id 利润折线图数据主键
     * @return 利润折线图数据
     */
    public SalesData selectSalesDataById(Long id);

    /**
     * 查询利润折线图数据列表
     *
     * @param salesData 利润折线图数据
     * @return 利润折线图数据集合
     */
    public List<SalesData> selectSalesDataList(SalesData salesData);

    /**
     * 新增利润折线图数据
     *
     * @param salesData 利润折线图数据
     * @return 结果
     */
    public int insertSalesData(SalesData salesData);

    /**
     * 修改利润折线图数据
     *
     * @param salesData 利润折线图数据
     * @return 结果
     */
    public int updateSalesData(SalesData salesData);

    /**
     * 批量删除利润折线图数据
     *
     * @param ids 需要删除的利润折线图数据主键集合
     * @return 结果
     */
    public int deleteSalesDataByIds(Long[] ids);

    /**
     * 删除利润折线图数据信息
     *
     * @param id 利润折线图数据主键
     * @return 结果
     */
    public int deleteSalesDataById(Long id);

    /**
     * 查询今年数据
     *
     * @param userId 用户ID
     * @return 结果
     */
    public List<SalesData> selectSalesDataCurrentYear(Long userId);

    /**
     * 查询每年最新的一条数据
     *
     * @param userId 用户ID
     * @return 结果
     */
    public List<SalesData> selectSalesDataYearlyMax(Long userId);
}
