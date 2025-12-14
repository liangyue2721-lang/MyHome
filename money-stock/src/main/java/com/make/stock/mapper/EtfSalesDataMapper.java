package com.make.stock.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.make.stock.domain.EtfSalesData;
import org.apache.ibatis.annotations.Param;

/**
 * eft折线图数据Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface EtfSalesDataMapper {

    /**
     * 查询eft折线图数据
     *
     * @param id eft折线图数据主键
     * @return eft折线图数据
     */
    public EtfSalesData selectEtfSalesDataById(Long id);

    /**
     * 查询eft折线图数据列表
     *
     * @param etfSalesData eft折线图数据
     * @return eft折线图数据集合
     */
    public List<EtfSalesData> selectEtfSalesDataList(EtfSalesData etfSalesData);

    /**
     * 新增eft折线图数据
     *
     * @param etfSalesData eft折线图数据
     * @return 结果
     */
    public int insertEtfSalesData(EtfSalesData etfSalesData);

    /**
     * 修改eft折线图数据
     *
     * @param etfSalesData eft折线图数据
     * @return 结果
     */
    public int updateEtfSalesData(EtfSalesData etfSalesData);

    /**
     * 删除eft折线图数据
     *
     * @param id eft折线图数据主键
     * @return 结果
     */
    public int deleteEtfSalesDataById(Long id);

    /**
     * 批量删除eft折线图数据
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteEtfSalesDataByIds(Long[] ids);

    /**
     * 查询ETF折线数据列表
     *
     * @param etfSalesData ETF折线数据
     * @return ETF折线数据集合
     */
    public List<EtfSalesData> selectEtfSalesDataListByName(EtfSalesData etfSalesData);


    List<EtfSalesData> queryByCodesAndTime(@Param("etfCodes") List<String> queryList, @Param("startTime") LocalDateTime startTime);

    int delTime();
}
