package com.make.quartz.service.stock;

import java.io.IOException;
import java.util.Date;

/**
 * 股票基础信息服务接口
 * 负责新股、ETF、上市状态等基础数据管理
 */
public interface StockInfoService {

    /**
     * 刷新新股信息
     */
    void refreshNewStockInformation();

    /**
     * 更新ETF数据
     */
    void updateEtfData() throws IOException;

    /**
     * 查询上市状态
     * @param midnight 当日零点时间
     */
    void queryListingStatusColumn(Date midnight);

    /**
     * 记录所有线程池状态
     */
    void logAllThreadPoolStatus();

}
