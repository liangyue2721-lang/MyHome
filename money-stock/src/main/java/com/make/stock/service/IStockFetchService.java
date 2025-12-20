package com.make.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.make.stock.domain.KlineData;

import java.io.IOException;
import java.util.List;

/**
 * 统一股票数据抓取服务
 * <p>
 * 提供股票实时行情、K线数据、ETF等数据的抓取能力。
 * 屏蔽底层的 HTTP 请求、重试、反爬虫处理等细节。
 */
public interface IStockFetchService {

    /**
     * 获取股票实时数据（原始 JSON）
     *
     * @param apiUrl API URL
     * @return JsonNode
     */
    JsonNode fetchStockData(String apiUrl);

    /**
     * 获取指定股票的全量历史 K 线数据
     *
     * @param stockCode 股票代码
     * @param market    市场代码
     * @return K 线数据列表
     */
    List<KlineData> fetchKlineDataAll(String stockCode, String market);

    /**
     * 获取指定股票最近 5 日的 K 线数据（日级聚合）
     *
     * @param stockCode 股票代码
     * @param market    市场代码
     * @return K 线数据列表
     */
    List<KlineData> fetchKlineDataFiveDay(String stockCode, String market);

    /**
     * 获取指定时间范围的 K 线数据
     *
     * @param stockCode 股票代码
     * @param market    市场代码
     * @param startDate 开始日期 yyyyMMdd
     * @param endDate   结束日期 yyyyMMdd
     * @return K 线数据列表
     */
    List<KlineData> fetchKlineData(String stockCode, String market, String startDate, String endDate);

    /**
     * 获取美股今日行情
     *
     * @param stockCode 美股代码
     * @param ndays     天数
     * @return K 线数据列表
     */
    List<KlineData> fetchTodayUSKlineData(String stockCode, String ndays);
}
