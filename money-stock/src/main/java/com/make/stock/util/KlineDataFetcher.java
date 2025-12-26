package com.make.stock.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.make.stock.domain.KlineData;
import com.make.stock.domain.dto.EtfRealtimeInfo;
import com.make.stock.domain.dto.StockRealtimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * K线数据获取工具类 — 调用 Python HTTP Service 获取股票 K 线数据。
 * 支持：
 * <ul>
 *     <li>沪深股市 K 线数据获取</li>
 *     <li>美股实时数据获取</li>
 *     <li>指定日期区间的数据查询</li>
 * </ul>
 *
 * @author
 * @since 2025-11
 */
@Component
public class KlineDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(KlineDataFetcher.class);

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrlConfig;

    private static String pythonServiceUrl;
    private static RestTemplate restTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @PostConstruct
    public void init() {
        pythonServiceUrl = pythonServiceUrlConfig;
        restTemplate = new RestTemplate();
        logger.info("KlineDataFetcher initialized with Python Service URL: {}", pythonServiceUrl);
    }

    // ========================== 核心通用请求方法 ==========================

    private static <T> T post(String path, Object requestBody, Class<T> responseType) {
        return post(path, requestBody, responseType, null);
    }

    private static <T> T post(String path, Object requestBody, TypeReference<T> typeReference) {
        return post(path, requestBody, null, typeReference);
    }

    private static <T> T post(String path, Object requestBody, Class<T> responseType, TypeReference<T> typeReference) {
        try {
            if (restTemplate == null) {
                // 防御性：如果是静态上下文（如单元测试）未注入
                restTemplate = new RestTemplate();
                if (pythonServiceUrl == null) pythonServiceUrl = "http://localhost:8000";
            }

            String url = pythonServiceUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

            logger.debug("Calling Python Service: {}", url);
            // Log request body for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Python Service Request Body: {}", JSON.toJSONString(requestBody));
            }

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Python Service failed. Status: {}, Body: {}, Request: {}",
                        response.getStatusCode(), response.getBody(), JSON.toJSONString(requestBody));
                return null;
            }

            if (typeReference != null) {
                return JSON.parseObject(response.getBody(), typeReference);
            } else {
                return JSON.parseObject(response.getBody(), responseType);
            }

        } catch (Exception e) {
            // Enhanced logging: full stack trace and request details
            logger.error("Error calling Python Service [{}]. Request: {}. Exception: ",
                    path, JSON.toJSONString(requestBody), e);
            // No fallback - Service is the single source of truth.
            return null;
        }
    }

    // ========================== 公共 API ==========================

    /**
     * 获取沪深股市 K 线数据（默认全量）
     */
    public static List<KlineData> fetchKlineData(String secid, String market) {
        logger.info("Fetching K-line data for stock: {}, market: {}", secid, market);
        return fetchKlineData(secid, market, null, null);
    }

    /**
     * 获取指定时间段的沪深股市 K 线数据
     */
    public static List<KlineData> fetchKlineData(String secid, String market, String startDate, String endDate) {
        logger.info("Fetching K-line data range for stock: {}, market: {}, from: {} to: {}", secid, market, startDate, endDate);

        Map<String, String> body = new HashMap<>();
        body.put("secid", secid);
        body.put("market", market);
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        List<KlineData> result = post("/stock/kline/range", body, new TypeReference<List<KlineData>>() {});
        return result != null ? result : Collections.emptyList();
    }

    /**
     * 获取今日沪深股市 K 线数据（默认取近三天数据防止当日无交易）
     */
    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        logger.info("Fetching today's K-line data for stock: {}, market: {}", secid, market);
        return fetchKlineData(secid, market, threeDaysAgo, today);
    }

    /**
     * 获取今日美股实时 K 线数据（仅取最新一条）
     */
    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        logger.info("Fetching today's US K-line data for stock: {}, market: {}, date: {}", secid, market, today);
        return fetchUSKlineData(secid, market, today, today);
    }

    /**
     * 获取美股 K 线数据
     */
    public static List<KlineData> fetchUSKlineData(String secid, String market, String startDate, String endDate) {
        logger.info("Fetching US K-line data for stock: {}, market: {}, from: {} to: {}", secid, market, startDate, endDate);

        Map<String, String> body = new HashMap<>();
        body.put("secid", secid);
        body.put("market", market);
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        List<KlineData> result = post("/stock/kline/us", body, new TypeReference<List<KlineData>>() {});
        return result != null ? result : Collections.emptyList();
    }

    /**
     * 获取沪深股市 K 线数据（5日 Trends 聚合）
     */
    public static List<KlineData> fetchKlineDataFiveDay(String secid, String market) {
        logger.info("fetchKlineDataFiveDay [ENTRY] secid={}, market={}", secid, market);

        if (market == null || secid == null || "null".equals(market) || "null".equals(secid) || secid.startsWith("null.")) {
            logger.error("fetchKlineDataFiveDay [BLOCKED] invalid input: secid={}, market={}", secid, market);
            return Collections.emptyList();
        }

        String fullSecid = market + "." + secid;
        logger.info("fetchKlineDataFiveDay [EXECUTE] fullSecid={}", fullSecid);

        Map<String, Object> body = new HashMap<>();
        body.put("secid", fullSecid);
        body.put("ndays", 5);

        List<KlineData> result = post("/stock/kline", body, new TypeReference<List<KlineData>>() {});
        if (result == null) {
            logger.warn("fetchKlineDataFiveDay [FAILED] result is null");
            return Collections.emptyList();
        }
        logger.info("fetchKlineDataFiveDay [SUCCESS] size={}", result.size());
        return result;
    }

    /**
     * 拉取指定股票的历史日 K 线数据（全历史）
     */
    public static List<KlineData> fetchKlineDataALL(String secid, String market) {
        logger.info("fetchKlineDataALL [ENTRY] secid={}, market={}", secid, market);

        if (market == null || secid == null || "null".equals(market) || "null".equals(secid) || secid.startsWith("null.")) {
            logger.error("fetchKlineDataALL [BLOCKED] invalid input: secid={}, market={}", secid, market);
            return Collections.emptyList();
        }

        // 使用 /stock/kline (Trends) 还是 /stock/kline/range?
        // 原始代码调用 hybrid_kline_trends.py，它调用 trends2。
        // trends2 接口主要返回近期分钟/日线趋势。
        // 但 Javadoc 说 "Full History"。
        // 假设 Service 的 /stock/kline 端点复刻了 hybrid_kline_trends 的逻辑。

        String fullSecid = market + "." + secid;
        logger.info("fetchKlineDataALL [EXECUTE] fullSecid={}", fullSecid);

        // 注意：这里复用 /stock/kline，因为我们在 Service 里实现了它对应 hybrid 逻辑
        Map<String, Object> body = new HashMap<>();
        body.put("secid", fullSecid);
        body.put("ndays", 10000); // 尝试请求长周期

        List<KlineData> result = post("/stock/kline", body, new TypeReference<List<KlineData>>() {});
        if (result == null) {
            logger.warn("fetchKlineDataALL [FAILED] result is null");
            return Collections.emptyList();
        }
        logger.info("fetchKlineDataALL [SUCCESS] size={}", result.size());
        return result;
    }

    /**
     * 获取 ETF 实时行情信息
     */
    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        logger.info("Fetching ETF realtime info via Service: {}", apiUrl);
        Map<String, String> body = new HashMap<>();
        body.put("url", apiUrl);
        return post("/etf/realtime", body, EtfRealtimeInfo.class);
    }

    /**
     * 获取单只股票实时行情
     */
    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        logger.info("Fetching Stock realtime info via Service: {}", apiUrl);
        Map<String, String> body = new HashMap<>();
        body.put("url", apiUrl);
        return post("/stock/realtime", body, StockRealtimeInfo.class);
    }
}
