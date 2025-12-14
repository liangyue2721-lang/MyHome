package com.make.stock.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.EtfDataMapper;
import com.make.stock.domain.EtfData;
import com.make.stock.service.IEtfDataService;

/**
 * ETF交易数据Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class EtfDataServiceImpl implements IEtfDataService {

    @Autowired
    private EtfDataMapper etfDataMapper;

    /**
     * 查询ETF交易数据
     *
     * @param etfCode ETF交易数据主键
     * @return ETF交易数据
     */
    @Override
    public EtfData selectEtfDataByEtfCode(String etfCode) {
        return etfDataMapper.selectEtfDataByEtfCode(etfCode);
    }

    /**
     * 查询ETF交易数据列表
     *
     * @param etfData ETF交易数据
     * @return ETF交易数据
     */
    @Override
    public List<EtfData> selectEtfDataList(EtfData etfData) {
        return etfDataMapper.selectEtfDataList(etfData);
    }

    /**
     * 新增ETF交易数据
     *
     * @param etfData ETF交易数据
     * @return 结果
     */
    @Override
    public int insertEtfData(EtfData etfData) {
        etfData.setCreateTime(DateUtils.getNowDate());
        return etfDataMapper.insertEtfData(etfData);
    }

    /**
     * 修改ETF交易数据
     *
     * @param etfData ETF交易数据
     * @return 结果
     */
    @Override
    public int updateEtfData(EtfData etfData) {
        etfData.setUpdateTime(DateUtils.getNowDate());
        return etfDataMapper.updateEtfData(etfData);
    }

    /**
     * 批量删除ETF交易数据
     *
     * @param etfCodes 需要删除的ETF交易数据主键
     * @return 结果
     */
    @Override
    public int deleteEtfDataByEtfCodes(String[] etfCodes) {
        return etfDataMapper.deleteEtfDataByEtfCodes(etfCodes);
    }

    /**
     * 删除ETF交易数据信息
     *
     * @param etfCode ETF交易数据主键
     * @return 结果
     */
    @Override
    public int deleteEtfDataByEtfCode(String etfCode) {
        return etfDataMapper.deleteEtfDataByEtfCode(etfCode);
    }

    @Override
    public void batchUpdateEtfData(List<EtfData> updatedList) {
        etfDataMapper.batchUpdateEtfData(updatedList);
    }
}
