package com.make.web.service;

import com.make.finance.domain.AssetRecord;

/**
 * 异步接口
 *
 * @author 84522
 */
public interface ISyncService {

    /**
     * 同步股票交易记录到本地数据库
     *
     * @return 最新同步的{@link AssetRecord}对象，包含：
     *         <ul>
     *             <li><b>transactionId</b>：唯一交易ID</li>
     *             <li><b>executeTime</b>：交易执行时间（ISO8601格式）</li>
     *             <li><b>price</b>：成交价格（Double类型）</li>
     *             <li><b>quantity</b>：交易数量（BigDecimal类型）</li>
     *         </ul>
     * @apiNote 该方法会覆盖本地早于24小时的旧数据
     * @reference 参考证券交易API文档v3.2
     */
    AssetRecord syncStockTrades();

}
