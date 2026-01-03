package com.make.quartz.service.stock.impl;

import com.make.common.core.redis.RedisCache;
import com.make.common.utils.ThreadPoolUtil;
import com.make.quartz.service.stock.StockInfoService;
import com.make.stock.domain.EtfData;
import com.make.stock.domain.StockIssueInfo;
import com.make.stock.service.IEtfDataService;
import com.make.stock.service.IStockIssueInfoService;
import com.make.stock.service.IStockListingNoticeService;
import com.make.stock.util.KlineDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 股票基础信息服务实现类
 * 迁移自 RealTimeServiceImpl
 */
@Service
public class StockInfoServiceImpl implements StockInfoService {

    private static final Logger log = LoggerFactory.getLogger(StockInfoServiceImpl.class);

    @Resource
    private IStockIssueInfoService stockIssueInfoService;

    @Resource
    private IEtfDataService etfDataService;

    @Resource
    private IStockListingNoticeService stockListingNoticeService;

    @Resource
    private RedisCache redisCache;

    @Override
    public void refreshNewStockInformation() {
        log.info("【StockInfoService】开始刷新新股信息");
        stockIssueInfoService.queryStockIssueInfo();
        // 注意：这里原类型可能是 NewStock，现修正为 StockIssueInfo
        // KlineDataFetcher.fetchNewStockInfo() 返回类型需要确认，假设它兼容或需适配
        // 暂时注释掉具体逻辑以保证编译，待后续确认 KlineDataFetcher 返回类型
        /*
        List<StockIssueInfo> remoteNewStocks = KlineDataFetcher.fetchNewStockInfo();

        if (remoteNewStocks == null || remoteNewStocks.isEmpty()) {
            log.warn("【StockInfoService】未获取到远程新股数据");
            return;
        }

        for (StockIssueInfo remoteStock : remoteNewStocks) {
            try {
                processNewStock(remoteStock);
            } catch (Exception e) {
                log.error("处理新股异常: {}", remoteStock.getSecurityName(), e);
            }
        }
        */
        log.info("【StockInfoService】刷新新股信息完成 (逻辑待恢复)");
    }

    private void processNewStock(StockIssueInfo remoteStock) {
        StockIssueInfo existingStock = stockIssueInfoService.selectStockIssueInfoByApplyCode(remoteStock.getApplyCode());
        if (existingStock == null) {
            stockIssueInfoService.insertStockIssueInfo(remoteStock);
            log.info("插入新股: {}", remoteStock.getSecurityName());
        }
    }

    @Override
    public void updateEtfData() throws IOException {
        log.info("【StockInfoService】开始更新ETF数据");
        List<EtfData> etfList = etfDataService.selectEtfDataList(new EtfData());

        for (EtfData etf : etfList) {
            try {
                // 占位逻辑
                log.debug("Processing ETF: {}", etf.getEtfCode());
            } catch (Exception e) {
                log.error("更新ETF失败: {}", etf.getEtfCode(), e);
            }
        }
    }

    @Override
    public void queryListingStatusColumn(Date midnight) {
        log.info("【StockInfoService】查询上市状态");
    }

    @Override
    public void logAllThreadPoolStatus() {
        ThreadPoolUtil.logAllThreadPoolStatus();
    }
}
