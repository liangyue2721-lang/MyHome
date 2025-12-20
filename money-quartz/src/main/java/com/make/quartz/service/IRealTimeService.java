package com.make.quartz.service;

import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 实时股票服务接口
 *
 * @deprecated 建议使用新的拆分服务：
 * <ul>
 *   <li>{@link com.make.quartz.service.stock.StockInfoService}</li>
 *   <li>{@link com.make.quartz.service.stock.ProfitService}</li>
 *   <li>{@link com.make.quartz.service.stock.WatchService}</li>
 *   <li>{@link com.make.quartz.service.finance.DepositService}</li>
 * </ul>
 */
@Deprecated
public interface IRealTimeService {

    void refreshNewStockInformation();

    void wealthDBDataBak();

    void updateStockProfitData() throws IOException;

    void queryStockProfitData();

    void updateEtfData() throws IOException;

    void logAllThreadPoolStatus();

    void queryListingStatusColumn(Date midnight);

    void updateWatchStockProfitData();

    void saveCCBCreditCardTransaction(List<CCBCreditCardTransactionEmail> emailList);

    void updateDepositAmount();

    void updateICBCDepositAmount(Long loanRepaymentId, Long assetId);

    void archiveDailyStockData();

    void updateWatchStockYearLow();

    void updateWatchStockUs();

    void updateStockPriceTaskRunning(int nodeId);
}
