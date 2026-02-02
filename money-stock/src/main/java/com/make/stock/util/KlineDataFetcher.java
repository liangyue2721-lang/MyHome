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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * =========================================================
 * 类名：KlineDataFetcher
 * =========================================================
 * <p>
 * 【核心职责】
 * Java ↔ Python 股票数据服务的统一访问入口，负责：
 * 1️⃣ K 线数据（JSON Array → 强类型 List<KlineData>）
 * 2️⃣ 股票 / ETF 实时行情（JSON Object → 强类型 DTO）
 * 3️⃣ 🔥 通用 JSON 代理（JSON Object / Array → 自动识别）
 * <p>
 * ---------------------------------------------------------
 * 【极其重要的设计：方法级并发保护（Concurrency Guard）】
 * <p>
 * 本类不做 QPS 限流、不做时间窗口限流。
 * 本类只做：Per-Endpoint Concurrency Guard。
 * <p>
 * 语义：
 * - 每个业务方法（throttleKey）最多允许 2 个请求“同时在飞”
 * - 请求结束立刻释放并发许可
 * - 各方法之间互不影响
 * <p>
 * 目的：
 * - Python 是真实压力点
 * - 防止高并发瞬间打爆 Python
 * - 不限制发送频率，只限制活跃请求数
 * <p>
 * 示例：
 * fetchKlineDataRange 同时 ≤ 2
 * fetchUSKlineData   同时 ≤ 2
 * fetchRealtimeInfo  同时 ≤ 2
 * ……互不影响
 * <p>
 * ---------------------------------------------------------
 * 【JSON 解析设计约束（非常重要）】
 * - Python 不返回统一 Response 包装
 * - Java 必须解析“裸 JSON”
 * - 不能假设返回一定是 Object 或 Array
 * <p>
 * =========================================================
 */
@Slf4j
@Component
public class KlineDataFetcher {

    /* =====================================================
     * 一、基础配置
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

    /* =====================================================
     * 二、方法级并发控制核心结构
     * ===================================================== */

    /**
     * 每个 throttleKey 对应一个独立的并发信号量
     * <p>
     * key：方法名（如 fetchKlineDataRange）
     * value：Semaphore(2)
     * <p>
     * 语义：该方法同时最多 2 个请求在飞
     */
    private static final ConcurrentHashMap<String, Semaphore> semaphoreMap =
            new ConcurrentHashMap<>();

    /**
     * 获取方法并发许可
     * 若当前已有 2 个请求在飞，则阻塞等待
     *
     * @param key 方法级限流 key
     */
    private static void acquire(String key) {
        Semaphore semaphore = semaphoreMap.computeIfAbsent(key, k -> new Semaphore(2));
        try {
            semaphore.acquire(); // 阻塞直到获得并发许可
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取并发许可被中断", e);
        }
    }

    /**
     * 释放方法并发许可
     * 请求结束必须调用
     *
     * @param key 方法级限流 key
     */
    private static void release(String key) {
        Semaphore semaphore = semaphoreMap.get(key);
        if (semaphore != null) {
            semaphore.release(); // 立即释放，允许下一个请求进入
        }
    }

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

        log.info("KlineDataFetcher 初始化完成：启用方法级并发保护");
    }

    /* =====================================================
     * 三、核心 HTTP 调用模板（强类型）
     * ===================================================== */

    /**
     * 通用 Python 同步调用模板
     * <p>
     * 统一流程：
     * 1. 获取方法并发许可
     * 2. 发起 HTTP 请求
     * 3. 解析 JSON
     * 4. finally 中释放并发许可
     *
     * @param throttleKey 方法级并发控制 key
     * @param path        Python 接口路径
     * @param body        请求体
     * @param typeRef     返回类型
     */
    private static <T> T callPythonSyncData(
            String throttleKey,
            String path,
            Map<String, Object> body,
            TypeReference<T> typeRef
    ) {
        // ① 获取并发许可
        acquire(throttleKey);

        try {
            String url = pythonServiceUrl + path;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                throw new PythonServiceException(
                        response.getStatusCodeValue(), response.getBody());
            }

            // ② 解析裸 JSON 为目标类型
            return JSON.parseObject(response.getBody(), typeRef);

        } catch (JSONException e) {
            throw new PythonServiceException(502, "Python 返回非法 JSON");
        } catch (Exception e) {
            throw new PythonServiceException(500, "Python 服务不可用");
        } finally {
            // ③ 请求结束立即释放并发许可
            release(throttleKey);
        }
    }

    /* =====================================================
     * 四、🔥 通用 JSON 代理能力
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
        String key = "fetchRawJson";
        acquire(key);
        try {
            String url = pythonServiceUrl + "/proxy/json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity =
                    new HttpEntity<>(Map.of("url", targetUrl), headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            return JSON.parse(response.getBody());
        } finally {
            release(key);
        }
    }

    /**
     * 要求返回必须是 JSONObject
     */
    public static JSONObject requireObject(Object raw) {
        if (raw instanceof JSONObject obj) return obj;
        throw new PythonServiceException(502, "Expected JSON Object");
    }

    /**
     * 要求返回必须是 JSONArray
     */
    public static JSONArray requireArray(Object raw) {
        if (raw instanceof JSONArray arr) return arr;
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
     * 五、对外业务 API（完整保留）
     * ===================================================== */

    /**
     * 获取全部 K 线数据（无时间范围）
     */
    public static List<KlineData> fetchKlineData(String secid, String market) {
        return fetchKlineDataRange(secid, market, null, null);
    }

    /**
     * 获取指定时间范围的 K 线数据
     */
    public static List<KlineData> fetchKlineDataRange(
            String secid, String market,
            String startDate, String endDate) {

        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        return callPythonSyncData(
                "fetchKlineDataRange",
                "/stock/kline/range",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取今日（近三天窗口）K 线
     */
    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        return fetchKlineDataRange(secid, market, threeDaysAgo, today);
    }

    /**
     * 获取今日美股 K 线
     */
    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return fetchUSKlineData(secid, market, today, today);
    }

    /**
     * 获取美股 K 线数据
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
                "fetchUSKlineData",
                "/stock/kline/us",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取最近 5 天 K 线
     */
    public static List<KlineData> fetchKlineDataFiveDay(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 5);

        return callPythonSyncData(
                "fetchKlineDataFiveDay",
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
                "fetchKlineDataAll",
                "/stock/kline",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取股票实时行情
     */
    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        return callPythonSyncData(
                "fetchRealtimeInfo",
                "/stock/realtime",
                Map.of("url", apiUrl),
                new TypeReference<StockRealtimeInfo>() {
                }
        );
    }

    /**
     * 获取实时快照
     */
    public static StockRealtimeInfo fetchStockSnapshot(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));

        return callPythonSyncData(
                "fetchStockSnapshot",
                "/stock/snapshot",
                body,
                new TypeReference<StockRealtimeInfo>() {
                }
        );
    }

    /**
     * 获取 ETF 实时行情
     */
    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        return callPythonSyncData(
                "fetchEtfRealtimeInfo",
                "/etf/realtime",
                Map.of("url", apiUrl),
                new TypeReference<EtfRealtimeInfo>() {
                }
        );
    }

    /**
     * 获取逐笔成交（Ticks）
     */
    public static JSONArray fetchStockTicks(String secid, String market) {
        return callPythonSyncData(
                "fetchStockTicks",
                "/stock/ticks",
                Map.of("secid", formatFullSecid(secid, market)),
                new TypeReference<JSONArray>() {
                }
        );
    }

    /* =====================================================
     * 六、工具方法
     * ===================================================== */

    /**
     * 生成完整 secid，如 SH.600000
     */
    private static String formatFullSecid(String secid, String market) {
        return market + "." + secid;
    }
}
