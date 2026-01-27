package com.make.common.constant;

/**
 * Kafka Topic Constants
 */
public class KafkaTopics {

    /**
     * Stock Related Topics
     */
    public static final String TOPIC_STOCK_REFRESH = "stock.refresh";
    public static final String TOPIC_WATCH_STOCK_US = "stock.watch.us";
    public static final String TOPIC_QUERY_LISTING = "stock.query.listing";
    public static final String TOPIC_NEW_STOCK_INFO = "stock.new.info";
    public static final String TOPIC_STOCK_PROFIT_UPDATE = "stock.profit.update";
    public static final String TOPIC_STOCK_PROFIT_QUERY = "stock.profit.query";
    public static final String TOPIC_ETF_UPDATE = "stock.etf.update";
    public static final String TOPIC_WATCH_STOCK_PROFIT = "stock.watch.profit";
    public static final String TOPIC_ARCHIVE_DAILY_STOCK = "stock.archive.daily";
    public static final String TOPIC_WATCH_STOCK_YEAR_LOW = "stock.watch.year.low";
    public static final String TOPIC_STOCK_PRICE_TASK = "stock.price.task";
    public static final String TOPIC_ETF_TASK = "stock.etf.task";
    public static final String TOPIC_KLINE_TASK = "stock.kline.task";
    public static final String TOPIC_STOCK_TICK_TASK = "stock.tick.task";

    /**
     * Finance Related Topics
     */
    public static final String TOPIC_DEPOSIT_UPDATE = "finance.deposit.update";
    public static final String TOPIC_ICBC_DEPOSIT_UPDATE = "finance.icbc.deposit.update";
    public static final String TOPIC_CCB_CREDIT_CARD = "finance.ccb.credit.card";

    /**
     * System Related Topics
     */
    public static final String TOPIC_SYSTEM_BACKUP = "system.backup";

}
