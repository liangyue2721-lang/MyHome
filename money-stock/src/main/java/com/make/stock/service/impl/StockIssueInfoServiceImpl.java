package com.make.stock.service.impl;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.make.common.utils.DateUtils;
import com.make.stock.domain.StockConfigProperties;
import com.make.stock.util.KlineDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockIssueInfoMapper;
import com.make.stock.domain.StockIssueInfo;
import com.make.stock.service.IStockIssueInfoService;

/**
 * 新股发行信息Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class StockIssueInfoServiceImpl implements IStockIssueInfoService {


    private static final Logger log = LoggerFactory.getLogger(StockIssueInfoServiceImpl.class);

    @Autowired
    private StockIssueInfoMapper stockIssueInfoMapper;

    /**
     * 查询新股发行信息
     *
     * @param applyCode 新股发行信息主键
     * @return 新股发行信息
     */
    @Override
    public StockIssueInfo selectStockIssueInfoByApplyCode(String applyCode) {
        return stockIssueInfoMapper.selectStockIssueInfoByApplyCode(applyCode);
    }

    /**
     * 查询新股发行信息列表
     *
     * @param stockIssueInfo 新股发行信息
     * @return 新股发行信息
     */
    @Override
    public List<StockIssueInfo> selectStockIssueInfoList(StockIssueInfo stockIssueInfo) {
        return stockIssueInfoMapper.selectStockIssueInfoList(stockIssueInfo);
    }

    /**
     * 新增新股发行信息
     *
     * @param stockIssueInfo 新股发行信息
     * @return 结果
     */
    @Override
    public int insertStockIssueInfo(StockIssueInfo stockIssueInfo) {
        stockIssueInfo.setCreateTime(DateUtils.getNowDate());
        return stockIssueInfoMapper.insertStockIssueInfo(stockIssueInfo);
    }

    /**
     * 修改新股发行信息
     *
     * @param stockIssueInfo 新股发行信息
     * @return 结果
     */
    @Override
    public int updateStockIssueInfo(StockIssueInfo stockIssueInfo) {
        stockIssueInfo.setUpdateTime(DateUtils.getNowDate());
        return stockIssueInfoMapper.updateStockIssueInfo(stockIssueInfo);
    }

    /**
     * 批量删除新股发行信息
     *
     * @param applyCodes 需要删除的新股发行信息主键
     * @return 结果
     */
    @Override
    public int deleteStockIssueInfoByApplyCodes(String[] applyCodes) {
        return stockIssueInfoMapper.deleteStockIssueInfoByApplyCodes(applyCodes);
    }

    /**
     * 删除新股发行信息信息
     *
     * @param applyCode 新股发行信息主键
     * @return 结果
     */
    @Override
    public int deleteStockIssueInfoByApplyCode(String applyCode) {
        return stockIssueInfoMapper.deleteStockIssueInfoByApplyCode(applyCode);
    }

    @Override
    public String queryStockIssueInfoCode(String applyCode) {
        return stockIssueInfoMapper.selectStockIssueInfoExistCode(applyCode);
    }

    @Override
    public void queryStockIssueInfo() {
        String stockIssueInfoApiUrl = StockConfigProperties.getInstance().getStockIssueInfoApiUrl();
        Object o = KlineDataFetcher.fetchRawJson(stockIssueInfoApiUrl);
        log.info("【stockIssueInfoApiUrl】查询新股发行信息: {}", JSON.toJSONString(o));
    }

}
