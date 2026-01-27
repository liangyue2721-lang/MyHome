package com.make.stock.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
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
 * 【核心职责】
 * Java ↔ Python 股票数据服务的统一访问入口，负责：
 * <p>
 * 1️⃣ K 线数据（JSON Array → 强类型 List<KlineData>）
 * 2️⃣ 股票 / ETF 实时行情（JSON Object → 强类型 DTO）
 * 3️⃣ 🔥 通用 JSON 代理（JSON Object / Array → 自动识别）
 * <p>
 * 【设计约束（非常重要）】
 * - Python 端不使用统一 Response 包装
 * - Java 端必须解析“裸 JSON”
 * - 不能假设返回一定是 Object 或 Array
 * <p>
 * 【已验证支持的结构】
 * - 东财 IPO：Object → Object → Array
 * - K 线：Array
 * - 实时行情：Object
 * =========================================================
 */
@Slf4j
@Component
public class KlineDataFetcher {

    /* =====================================================
     * 基础配置
     * ===================================================== */

    /**
     * Python 服务基础地址（如：http://localhost:8000）
     */
    private static String pythonServiceUrl;

    /**
     * Spring RestTemplate（同步调用）
     */
    private static RestTemplate restTemplate;

    /**
     * K 线日期格式
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

    /* =====================================================
     * 一、原有强类型调用（保持不变）
     * ===================================================== */

    /**
     * 调用 Python 服务并解析为指定强类型
     * <p>
     * ⚠ 使用前提：
     * - 明确知道 Python 返回的是 Object 或 Array
     * - 并且能直接映射为目标 TypeReference
     *
     * @param path    Python 接口路径
     * @param body    请求体
     * @param typeRef 返回类型
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

        // 把请求体序列化成 JSON，用于日志
        String bodyJson = null;
        try {
            bodyJson = JSON.toJSONString(body);
        } catch (Exception ignore) {
            bodyJson = String.valueOf(body);
        }

        ResponseEntity<String> response;
        try {
            // 发起 HTTP POST 请求
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("HTTP call failed: url={}, body={}", url, truncate(bodyJson), e);
            throw new PythonServiceException(500, "Python service unreachable");
        }

        // 检查 HTTP 状态码
        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {
            log.error("HTTP status not ok: url={}, status={}, body={}",
                    url, response.getStatusCodeValue(), truncate(bodyJson));
            throw new PythonServiceException(
                    response.getStatusCodeValue(), response.getBody());
        }

        try {
            // 解析 JSON 响应
            return JSON.parseObject(response.getBody(), typeRef);
        } catch (JSONException e) {
            log.error("JSON parse error, url={}, reqBody={}, respBody={}",
                    url, truncate(bodyJson), truncate(response.getBody()), e);
            throw new PythonServiceException(502, "Invalid JSON from python");
        }
    }


    /* =====================================================
     * 二、🔥 新增：通用 JSON 代理能力
     * ===================================================== */

    /**
     * 调用 Python /proxy/json
     * <p>
     * 【返回说明】
     * - JSONObject：如 IPO / 实时行情
     * - JSONArray ：如 K 线 / 列表接口
     * <p>
     * ⚠ 不做任何结构假设
     */
    public static Object fetchRawJson(String targetUrl) {
        String url = pythonServiceUrl + "/proxy/json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构造请求体，包含目标 URL
        HttpEntity<Object> entity =
                new HttpEntity<>(Map.of("url", targetUrl), headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("Python proxy call failed", e);
            throw new PythonServiceException(500, "Python service unreachable");
        }

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {
            throw new PythonServiceException(
                    response.getStatusCodeValue(), response.getBody());
        }

        try {
            // 自动解析为 JSONObject 或 JSONArray
            return JSON.parse(response.getBody());
        } catch (JSONException e) {
            throw new PythonServiceException(502, "Invalid JSON from python");
        }
    }

    /**
     * 要求返回必须是 JSONObject
     */
    public static JSONObject requireObject(Object raw) {
        if (raw instanceof JSONObject obj) {
            return obj;
        }
        throw new PythonServiceException(502, "Expected JSON Object");
    }

    /**
     * 要求返回必须是 JSONArray
     */
    public static JSONArray requireArray(Object raw) {
        if (raw instanceof JSONArray arr) {
            return arr;
        }
        throw new PythonServiceException(502, "Expected JSON Array");
    }

    /**
     * JSONObject → Java Entity
     */
    public static <T> T mapObject(Object raw, Class<T> clazz) {
        return requireObject(raw).toJavaObject(clazz);
    }

    /**
     * JSONArray → List<Entity>
     */
    public static <T> List<T> mapArray(Object raw, Class<T> clazz) {
        return requireArray(raw).toJavaList(clazz);
    }

    /* =====================================================
     * 三、对外业务 API（原样保留）
     * ===================================================== */

    public static List<KlineData> fetchKlineData(String secid, String market) {
        return fetchKlineDataRange(secid, market, null, null);
    }

    /**
     * 获取指定时间范围的 K 线数据
     * <p>
     * 调用 Python 的 /stock/kline/range 接口
     *
     * @param secid     股票代码
     * @param market    市场代码
     * @param startDate 开始日期 (yyyyMMdd)
     * @param endDate   结束日期 (yyyyMMdd)
     * @return K 线数据列表
     */
    public static List<KlineData> fetchKlineDataRange(
            String secid, String market,
            String startDate, String endDate) {

        Map<String, Object> body = new HashMap<>();
        // 构造完整 secid (market.code)
        body.put("secid", formatFullSecid(secid, market));
        // 添加起止时间参数
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        // 调用 Python 接口获取区间 K 线
        return callPythonSyncData(
                "/stock/kline/range",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        return fetchKlineDataRange(secid, market, threeDaysAgo, today);
    }

    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return fetchUSKlineData(secid, market, today, today);
    }

    /**
     * 获取美股 K 线数据
     * <p>
     * 调用 Python 的 /stock/kline/us 接口
     *
     * @param secid     股票代码
     * @param market    市场标识 (105/106)
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return K 线数据列表
     */
    public static List<KlineData> fetchUSKlineData(
            String secid, String market,
            String startDate, String endDate) {

        Map<String, Object> body = new HashMap<>();
        // 美股接口参数：secid 和 market 分开传递
        body.put("secid", secid);
        body.put("market", market);
        body.put("beg", startDate);
        body.put("end", endDate);

        // 调用 Python 美股 K 线接口
        return callPythonSyncData(
                "/stock/kline/us",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

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

    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        return callPythonSyncData(
                "/stock/realtime",
                Map.of("url", apiUrl),
                new TypeReference<StockRealtimeInfo>() {
                }
        );
    }

    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        return callPythonSyncData(
                "/etf/realtime",
                Map.of("url", apiUrl),
                new TypeReference<EtfRealtimeInfo>() {
                }
        );
    }

    public static JSONArray fetchStockTicks(String secid, String market) {
        return callPythonSyncData(
                "/stock/ticks",
                Map.of("secid", formatFullSecid(secid, market)),
                new TypeReference<JSONArray>() {
                }
        );
    }

    /* =====================================================
     * 工具方法
     * ===================================================== */

    /**
     * 生成完整 secid，如 SH.600000
     */
    private static String formatFullSecid(String secid, String market) {
        return market + "." + secid;
    }

    /**
     * 日志截断，防止刷屏
     */
    private static String truncate(String s) {
        return s.length() > 2000 ? s.substring(0, 2000) + "..." : s;
    }
}
