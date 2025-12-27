package com.make.stock.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.TypeReference;
import com.make.stock.domain.KlineData;
import com.make.stock.domain.dto.EtfRealtimeInfo;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.exception.PythonServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * =========================================================
 * 类名：KlineDataFetcher
 * =========================================================
 * <p>
 * 【职责说明】
 * Java ↔ Python 股票数据服务的统一访问入口，负责：
 * - K 线数据（返回 JSON 数组）
 * - 股票 / ETF 实时行情（返回 JSON 对象）
 * <p>
 * 【Python 服务真实返回结构】
 * <p>
 * 1️⃣ K线接口：/stock/kline /stock/kline/range /stock/kline/us
 * ----------------------------------------------------------
 * 返回：JSON Array
 * <p>
 * [
 * {
 * "trade_date": "2025-12-22",
 * "stock_code": "600000",
 * "open": 11.65,
 * "close": 11.64
 * }
 * ]
 * <p>
 * 2️⃣ 实时行情接口：/stock/realtime /etf/realtime
 * ----------------------------------------------------------
 * 返回：JSON Object
 * <p>
 * {
 * "stockCode": "601138",
 * "price": 63.84
 * }
 * <p>
 * ❗ Python 当前未使用统一 Response 包装，
 * Java 端必须直接解析裸 JSON，严禁假设 PythonResponse。
 * =========================================================
 */
@Slf4j
@Component
public class KlineDataFetcher {

    /**
     * Python 服务地址
     */
    private static String pythonServiceUrl;

    /**
     * HTTP 客户端
     */
    private static RestTemplate restTemplate;

    /**
     * 日期格式
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrlConfig;

    @Value("${python.service.timeout:5000}")
    private int timeoutMillis;

    /**
     * 初始化 HTTP 客户端
     */
    @PostConstruct
    public void init() {
        pythonServiceUrl = pythonServiceUrlConfig;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);

        restTemplate = new RestTemplate(factory);

        log.info("KlineDataFetcher initialized, pythonServiceUrl={}, timeout={}ms",
                pythonServiceUrl, timeoutMillis);
    }

    // =========================================================
    // 核心调用方法（解析裸 JSON）
    // =========================================================

    /**
     * 调用 Python 服务并解析裸 JSON 数据
     *
     * @param path    Python 接口路径
     * @param body    请求参数
     * @param typeRef 返回类型
     * @param <T>     泛型
     * @return 解析后的数据对象
     */
    private static <T> T callPythonSyncData(
            String path,
            Map<String, Object> body,
            TypeReference<T> typeRef
    ) {
        String url = pythonServiceUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("HTTP call failed: {}", url, e);
            throw new PythonServiceException(500, "Python service unreachable");
        }

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {
            throw new PythonServiceException(
                    response.getStatusCodeValue(), response.getBody());
        }

        String json = response.getBody().trim();

        try {
            return JSON.parseObject(json, typeRef);
        } catch (JSONException e) {
            log.error("JSON parse error, path={}, body={}", path, truncate(json), e);
            throw new PythonServiceException(502, "Invalid JSON from python");
        }
    }

    // =========================================================
    // 对外 API —— K线
    // =========================================================

    /**
     * 获取沪深股票日线（不指定区间）
     */
    public static List<KlineData> fetchKlineData(String secid, String market) {
        return fetchKlineDataRange(secid, market, null, null);
    }

    /**
     * 获取沪深股票区间 K 线
     */
    public static List<KlineData> fetchKlineDataRange(
            String secid, String market,
            String startDate, String endDate) {

        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        return callPythonSyncData(
                "/stock/kline/range",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取今日（近 3 日）K 线
     */
    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        return fetchKlineDataRange(secid, market, threeDaysAgo, today);
    }

    /**
     * 获取美股今日 K 线（⚠ 原始代码中的方法，已补齐）
     */
    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return fetchUSKlineData(secid, market, today, today);
    }

    /**
     * 获取美股区间 K 线
     */
    public static List<KlineData> fetchUSKlineData(
            String secid, String market,
            String startDate, String endDate) {

        Map<String, Object> body = new HashMap<>();
        body.put("secid", secid);
        body.put("market", market);
        body.put("beg", startDate);
        body.put("end", endDate);

        return callPythonSyncData(
                "/stock/kline/us",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取最近 5 日 K 线
     */
    public static List<KlineData> fetchKlineDataFiveDay(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 5);

        return callPythonSyncData(
                "/stock/kline",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取全部历史 K 线
     */
    public static List<KlineData> fetchKlineDataAll(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 100000);

        return callPythonSyncData(
                "/stock/kline",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    // =========================================================
    // 对外 API —— 实时行情
    // =========================================================

    /**
     * 股票实时行情
     */
    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        Map<String, Object> body = Map.of("url", apiUrl);
        return callPythonSyncData(
                "/stock/realtime",
                body,
                new TypeReference<StockRealtimeInfo>() {
                }
        );
    }

    /**
     * ETF 实时行情
     */
    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        Map<String, Object> body = Map.of("url", apiUrl);
        return callPythonSyncData(
                "/etf/realtime",
                body,
                new TypeReference<EtfRealtimeInfo>() {
                }
        );
    }

    // =========================================================
    // 工具方法
    // =========================================================

    /**
     * 生成完整 secid，如 SH.600000
     */
    private static String formatFullSecid(String secid, String market) {
        return market + "." + secid;
    }

    /**
     * 截断日志，避免刷屏
     */
    private static String truncate(String s) {
        return s.length() > 2000 ? s.substring(0, 2000) + "..." : s;
    }
}
