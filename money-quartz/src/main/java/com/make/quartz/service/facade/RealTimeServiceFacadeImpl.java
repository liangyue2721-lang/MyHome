package com.make.quartz.service.facade;

import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.quartz.service.IRealTimeService;
import com.make.quartz.service.finance.CreditCardService;
import com.make.quartz.service.finance.DepositService;
import com.make.quartz.service.stock.KlineAggregatorService;
import com.make.quartz.service.stock.ProfitService;
import com.make.quartz.service.stock.StockInfoService;
import com.make.quartz.service.stock.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 实时股票服务 Facade 实现类
 */
@Service("realTimeServiceImpl")
public class RealTimeServiceFacadeImpl implements IRealTimeService {

    private static final Logger log = LoggerFactory.getLogger(RealTimeServiceFacadeImpl.class);

    @Resource
    private StockInfoService stockInfoService;

    @Resource
    private ProfitService profitService;

    @Resource
    private WatchService watchService;

    @Resource
    private DepositService depositService;

    @Resource
    private CreditCardService creditCardService;

    @Resource
    private KlineAggregatorService klineAggregatorService;

    @Override
    public void refreshNewStockInformation() {
        stockInfoService.refreshNewStockInformation();
    }

    @Override
    public void wealthDBDataBak() {
        // 修正：wealthDBDataBak 是全量备份任务，不应映射到 archiveDailyStockData（日线归档）
        // 由于目前 ProfitService/StockInfoService 尚未实现备份逻辑，此处暂时仅记录日志防止误执行
        log.warn("【Facade】wealthDBDataBak 逻辑尚未迁移，本次执行跳过 (TODO: 迁移全量备份逻辑)");
    }

    @Override
    public void updateStockProfitData() throws IOException {
        profitService.updateStockProfitData();
    }

    @Override
    public void queryStockProfitData() {
        profitService.queryStockProfitData();
    }

    @Override
    public void updateEtfData() throws IOException {
        stockInfoService.updateEtfData();
    }

    @Override
    public void logAllThreadPoolStatus() {
        stockInfoService.logAllThreadPoolStatus();
    }

    @Override
    public void queryListingStatusColumn(Date midnight) {
        stockInfoService.queryListingStatusColumn(midnight);
    }

    @Override
    public void updateWatchStockProfitData() {
        watchService.updateWatchStockProfitData();
    }

    @Override
    public void saveCCBCreditCardTransaction(List<CCBCreditCardTransactionEmail> emailList) {
        creditCardService.saveCCBCreditCardTransaction(emailList);
    }

    @Override
    public void updateDepositAmount() {
        depositService.updateAnnualDepositSummary();
    }

    @Override
    public void updateICBCDepositAmount(Long loanRepaymentId, Long assetId) {
        depositService.updateICBCDepositAmount(loanRepaymentId, assetId);
    }

    @Override
    public void archiveDailyStockData() {
        profitService.archiveDailyStockData();
    }

    @Override
    public void updateWatchStockYearLow() {
        watchService.updateWatchStockYearLow();
    }

    @Override
    public void updateWatchStockUs() {
        watchService.updateWatchStockUs();
    }

    @Override
    public void updateStockPriceTaskRunning(int nodeId) {
        klineAggregatorService.runStockKlineTask(nodeId);
    }
}
