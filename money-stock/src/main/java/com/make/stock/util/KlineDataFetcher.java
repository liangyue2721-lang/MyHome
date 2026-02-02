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
 * 描述：股票数据获取器 - Java与Python数据服务的统一访问入口
 * =========================================================
 * <p>
 * 【核心职责】
 * 1. 提供统一的Java ↔ Python股票数据服务访问接口
 * 2. 实现严格的全局请求限流机制
 * 3. 处理各种股票数据类型的获取和转换
 * <p>
 * 【🔥 全局严格限流模式 (Global Strict Rate Limit)】
 * <p>
 * 规则：全局每 2 秒允许发送 1 个请求 (QPS = 0.5)
 * <p>
 * 目的：
 * 1. 极其严格地保护 Python 服务
 * 2. 防止被上游数据源 (如东方财富) 封禁 IP
 * 3. 所有请求变为串行执行，并强制间隔
 * <p>
 * 逻辑：
 * 任何调用此类的线程，若距离上次请求不足 2 秒，
 * 将自动阻塞 (Thread.sleep) 直到满足时间间隔。
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
     * Python服务URL
     */
    private static String pythonServiceUrl;
    /**
     * REST模板实例
     */
    private static RestTemplate restTemplate;

    /**
     * 日期格式化器，格式：yyyyMMdd
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Python服务URL配置，从配置文件注入
     */
    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrlConfig;

    /**
     * 请求超时时间配置，单位：毫秒
     */
    @Value("${python.service.timeout:10000}")
    private int timeoutMillis;

    /* =====================================================
     * 二、全局限流核心结构 (每2秒1请求)
     * ===================================================== */

    /**
     * 全局锁对象，用于同步限流操作
     */
    private static final Object GLOBAL_LOCK = new Object();
    /**
     * 上一次请求的时间戳
     */
    private static long lastRequestTime = 0L;
    /**
     * 请求间隔时间，单位：毫秒
     */
    private static final long INTERVAL_MS = 2000L; // 2000ms = 2秒

    /**
     * ⏳ 强制获取限流许可
     * <p>
     * 功能：确保请求间隔符合限流规则，不足时间间隔时会阻塞当前线程
     * <p>
     * 逻辑：
     * 1. 锁住全局对象
     * 2. 计算距离上次请求过去多久
     * 3. 如果不足 2秒，强制 sleep 补足时间
     * 4. 更新 lastRequestTime
     *
     * @throws RuntimeException 如果等待期间线程被中断
     */
    private static void enforceGlobalRateLimit() {
        synchronized (GLOBAL_LOCK) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;

            if (elapsed < INTERVAL_MS) {
                long sleepTime = INTERVAL_MS - elapsed;
                try {
                    log.info("⚡ 触发全局限流，当前线程需等待 {} ms", sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("限流等待期间被中断", e);
                }
            }
            // 更新最后请求时间（注意：是在 sleep 之后，真正发起请求前更新）
            lastRequestTime = System.currentTimeMillis();
        }
    }

    /**
     * 初始化方法，在Bean创建后执行
     * 功能：配置REST模板和Python服务URL
     */
    @PostConstruct
    public void init() {
        pythonServiceUrl = pythonServiceUrlConfig;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);    // 设置连接超时
        factory.setReadTimeout(timeoutMillis);        // 设置读取超时

        restTemplate = new RestTemplate(factory);

        log.info("KlineDataFetcher 初始化完成：启用全局严格限流 (1 req / 2s)");
    }

    /* =====================================================
     * 三、核心 HTTP 调用模板（已接入限流）
     * ===================================================== */

    /**
     * 通用 Python 同步调用模板
     *
     * @param <T>         返回类型泛型
     * @param throttleKey 限流键（兼容保留参数，仅用于日志）
     * @param path        Python服务路径
     * @param body        请求体数据
     * @param typeRef     返回类型引用
     * @return 解析后的响应数据
     * @throws PythonServiceException 当HTTP状态码非2xx或解析失败时抛出
     */
    private static <T> T callPythonSyncData(
            String throttleKey, // 兼容保留参数名，但此处仅用于日志
            String path,
            Map<String, Object> body,
            TypeReference<T> typeRef
    ) {
        // ① 全局限流检查 (会阻塞)
        enforceGlobalRateLimit();

        try {
            String url = pythonServiceUrl + path;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            // 检查HTTP状态码
            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                throw new PythonServiceException(
                        response.getStatusCodeValue(), response.getBody());
            }

            // 解析JSON响应
            return JSON.parseObject(response.getBody(), typeRef);

        } catch (JSONException e) {
            throw new PythonServiceException(502, "Python 返回非法 JSON");
        } catch (Exception e) {
            log.error("调用 Python 服务异常: {}", e.getMessage());
            throw new PythonServiceException(500, "Python 服务不可用");
        }
        // 注意：不再需要 finally release，因为是基于时间的限流，不是基于引用计数的
    }

    /* =====================================================
     * 四、🔥 通用 JSON 代理能力
     * ===================================================== */

    /**
     * 获取原始JSON数据
     *
     * @param targetUrl 目标URL
     * @return 原始JSON对象
     * @throws PythonServiceException 当请求失败时抛出
     */
    public static Object fetchRawJson(String targetUrl) {
        // ① 全局限流检查
        enforceGlobalRateLimit();

        try {
            String url = pythonServiceUrl + "/proxy/json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity =
                    new HttpEntity<>(Map.of("url", targetUrl), headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            return JSON.parse(response.getBody());
        } catch (Exception e) {
            throw new PythonServiceException(500, "Fetch Raw JSON Error: " + e.getMessage());
        }
    }

    /**
     * 要求对象必须为JSONObject类型
     *
     * @param raw 原始对象
     * @return JSONObject实例
     * @throws PythonServiceException 当对象不是JSONObject时抛出
     */
    public static JSONObject requireObject(Object raw) {
        if (raw instanceof JSONObject obj) return obj;
        throw new PythonServiceException(502, "Expected JSON Object");
    }

    /**
     * 要求对象必须为JSONArray类型
     *
     * @param raw 原始对象
     * @return JSONArray实例
     * @throws PythonServiceException 当对象不是JSONArray时抛出
     */
    public static JSONArray requireArray(Object raw) {
        if (raw instanceof JSONArray arr) return arr;
        throw new PythonServiceException(502, "Expected JSON Array");
    }

    /**
     * 将原始对象映射为指定类型的Java对象
     *
     * @param <T>   目标类型泛型
     * @param raw   原始对象
     * @param clazz 目标类
     * @return 映射后的Java对象
     * @throws PythonServiceException 当对象不是JSONObject时抛出
     */
    public static <T> T mapObject(Object raw, Class<T> clazz) {
        return requireObject(raw).toJavaObject(clazz);
    }

    /**
     * 将原始对象映射为指定类型的Java对象列表
     *
     * @param <T>   目标类型泛型
     * @param raw   原始对象
     * @param clazz 目标类
     * @return 映射后的Java对象列表
     * @throws PythonServiceException 当对象不是JSONArray时抛出
     */
    public static <T> List<T> mapArray(Object raw, Class<T> clazz) {
        return requireArray(raw).toJavaList(clazz);
    }

    /* =====================================================
     * 五、对外业务 API
     * ===================================================== */

    /**
     * 获取股票K线数据（默认时间范围）
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return K线数据列表
     */
    public static List<KlineData> fetchKlineData(String secid, String market) {
        return fetchKlineDataRange(secid, market, null, null);
    }

    /**
     * 获取指定时间范围的股票K线数据
     *
     * @param secid     股票代码
     * @param market    市场代码
     * @param startDate 开始日期（格式：yyyyMMdd）
     * @param endDate   结束日期（格式：yyyyMMdd）
     * @return K线数据列表
     */
    public static List<KlineData> fetchKlineDataRange(
            String secid, String market,
            String startDate, String endDate) {

        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        return callPythonSyncData(
                "kline_range",
                "/stock/kline/range",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取最近三天的K线数据（包含今天）
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 最近三天的K线数据列表
     */
    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        return fetchKlineDataRange(secid, market, threeDaysAgo, today);
    }

    /**
     * 获取美股今日K线数据
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 今日K线数据列表
     */
    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return fetchUSKlineData(secid, market, today, today);
    }

    /**
     * 获取美股指定时间范围的K线数据
     *
     * @param secid     股票代码
     * @param market    市场代码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return K线数据列表
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
                "us_kline",
                "/stock/kline/us",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取最近5天的K线数据
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 最近5天的K线数据列表
     */
    public static List<KlineData> fetchKlineDataFiveDay(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 5);

        return callPythonSyncData(
                "kline_5d",
                "/stock/kline",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取所有可用的K线数据
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 所有K线数据列表
     */
    public static List<KlineData> fetchKlineDataAll(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 100000); // 使用大数字表示获取所有数据

        return callPythonSyncData(
                "kline_all",
                "/stock/kline",
                body,
                new TypeReference<List<KlineData>>() {
                }
        );
    }

    /**
     * 获取股票实时信息
     *
     * @param apiUrl API URL
     * @return 股票实时信息对象
     */
    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        return callPythonSyncData(
                "realtime_stock",
                "/stock/realtime",
                Map.of("url", apiUrl),
                new TypeReference<StockRealtimeInfo>() {
                }
        );
    }

    /**
     * 获取股票快照信息
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 股票快照信息对象
     */
    public static StockRealtimeInfo fetchStockSnapshot(String secid, String market) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));

        return callPythonSyncData(
                "snapshot",
                "/stock/snapshot",
                body,
                new TypeReference<StockRealtimeInfo>() {
                }
        );
    }

    /**
     * 获取ETF实时信息
     *
     * @param apiUrl API URL
     * @return ETF实时信息对象
     */
    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        return callPythonSyncData(
                "realtime_etf",
                "/etf/realtime",
                Map.of("url", apiUrl),
                new TypeReference<EtfRealtimeInfo>() {
                }
        );
    }

    /**
     * 获取股票分笔数据
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 分笔数据JSON数组
     */
    public static JSONArray fetchStockTicks(String secid, String market) {
        return callPythonSyncData(
                "ticks",
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
     * 格式化完整的股票标识符
     *
     * @param secid  股票代码
     * @param market 市场代码
     * @return 格式化的完整股票标识符（市场.股票代码）
     */
    private static String formatFullSecid(String secid, String market) {
        return market + "." + secid;
    }
}