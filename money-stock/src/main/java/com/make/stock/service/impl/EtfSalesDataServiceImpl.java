package com.make.stock.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.EtfSalesDataMapper;
import com.make.stock.domain.EtfSalesData;
import com.make.stock.service.IEtfSalesDataService;

/**
 * eft折线图数据Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class EtfSalesDataServiceImpl implements IEtfSalesDataService {

    @Autowired
    private EtfSalesDataMapper etfSalesDataMapper;

    /**
     * 查询eft折线图数据
     *
     * @param id eft折线图数据主键
     * @return eft折线图数据
     */
    @Override
    public EtfSalesData selectEtfSalesDataById(Long id) {
        return etfSalesDataMapper.selectEtfSalesDataById(id);
    }

    /**
     * 查询eft折线图数据列表
     *
     * @param etfSalesData eft折线图数据
     * @return eft折线图数据
     */
    @Override
    public List<EtfSalesData> selectEtfSalesDataList(EtfSalesData etfSalesData) {
        return etfSalesDataMapper.selectEtfSalesDataList(etfSalesData);
    }

    /**
     * 新增eft折线图数据
     *
     * @param etfSalesData eft折线图数据
     * @return 结果
     */
    @Override
    public int insertEtfSalesData(EtfSalesData etfSalesData) {
        etfSalesData.setCreateTime(DateUtils.getNowDate());
        return etfSalesDataMapper.insertEtfSalesData(etfSalesData);
    }

    /**
     * 修改eft折线图数据
     *
     * @param etfSalesData eft折线图数据
     * @return 结果
     */
    @Override
    public int updateEtfSalesData(EtfSalesData etfSalesData) {
        etfSalesData.setUpdateTime(DateUtils.getNowDate());
        return etfSalesDataMapper.updateEtfSalesData(etfSalesData);
    }

    /**
     * 批量删除eft折线图数据
     *
     * @param ids 需要删除的eft折线图数据主键
     * @return 结果
     */
    @Override
    public int deleteEtfSalesDataByIds(Long[] ids) {
        return etfSalesDataMapper.deleteEtfSalesDataByIds(ids);
    }

    /**
     * 删除eft折线图数据信息
     *
     * @param id eft折线图数据主键
     * @return 结果
     */
    @Override
    public int deleteEtfSalesDataById(Long id) {
        return etfSalesDataMapper.deleteEtfSalesDataById(id);
    }

    /**
     * 查询ETF折线数据列表
     *
     * @param etfSalesData ETF折线数据
     * @return ETF折线数据
     */
    @Override
    public List<EtfSalesData> queryEtfSalesDataListByName(EtfSalesData etfSalesData) {
        return etfSalesDataMapper.selectEtfSalesDataListByName(etfSalesData);
    }

    @Override
    public List<EtfSalesData> batchQueryByEtfList(List<String> queryList) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(3);
//        etfSalesDataMapper.delTime();
        return etfSalesDataMapper.queryByCodesAndTime(queryList, startTime);
    }
}
