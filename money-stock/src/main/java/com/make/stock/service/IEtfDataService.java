package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.EtfData;

/**
 * ETF交易数据Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IEtfDataService {

    /**
     * 查询ETF交易数据
     *
     * @param etfCode ETF交易数据主键
     * @return ETF交易数据
     */
    public EtfData selectEtfDataByEtfCode(String etfCode);

    /**
     * 查询ETF交易数据列表
     *
     * @param etfData ETF交易数据
     * @return ETF交易数据集合
     */
    public List<EtfData> selectEtfDataList(EtfData etfData);

    /**
     * 新增ETF交易数据
     *
     * @param etfData ETF交易数据
     * @return 结果
     */
    public int insertEtfData(EtfData etfData);

    /**
     * 修改ETF交易数据
     *
     * @param etfData ETF交易数据
     * @return 结果
     */
    public int updateEtfData(EtfData etfData);

    /**
     * 批量删除ETF交易数据
     *
     * @param etfCodes 需要删除的ETF交易数据主键集合
     * @return 结果
     */
    public int deleteEtfDataByEtfCodes(String[] etfCodes);

    /**
     * 删除ETF交易数据信息
     *
     * @param etfCode ETF交易数据主键
     * @return 结果
     */
    public int deleteEtfDataByEtfCode(String etfCode);

    void batchUpdateEtfData(List<EtfData> updatedList);
}
