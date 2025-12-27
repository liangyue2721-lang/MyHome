package com.make.stock.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.make.stock.domain.KlineData;
import com.make.stock.domain.dto.EtfRealtimeInfo;
import com.make.stock.domain.dto.PythonResponse;
import com.make.stock.domain.dto.StockRealtimeInfo;
import com.make.stock.exception.PythonServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class KlineDataFetcher {

    private static String pythonServiceUrl;
    private static RestTemplate restTemplate;
    private static WebClient webClient;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrlConfig;

    @Value("${python.service.timeout:5000}")
    private int timeoutMillis;

    @PostConstruct
    public void init() {
        pythonServiceUrl = pythonServiceUrlConfig;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        restTemplate = new RestTemplate(factory);

        webClient = WebClient.builder()
                .baseUrl(pythonServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("KlineDataFetcher initialized with Python Service URL: {}, Timeout: {}ms", pythonServiceUrl, timeoutMillis);
    }

    // ========================== 核心调用方法 ==========================

    private static <T> PythonResponse<T> callPythonSync(
            String path, Map<String, Object> body,
            TypeReference<PythonResponse<T>> typeReference
    ) {

        String url = pythonServiceUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Python service non-2xx: {} body={}", response.getStatusCode(), response.getBody());
                throw new PythonServiceException(response.getStatusCodeValue(), response.getBody());
            }
            return JSON.parseObject(response.getBody(), typeReference);

        } catch (Exception e) {
            log.error("Exception calling Python service [" + path + "]: " + e.getMessage(), e);
            throw new PythonServiceException(500, e.getMessage());
        }
    }

    /**
     * 异步调用（WebClient）
     * 返回 Mono<PythonResponse<T>>
     */
    private static <T> Mono<PythonResponse<T>> callPythonAsync(
            String path, Map<String, Object> body,
            TypeReference<PythonResponse<T>> typeReference
    ) {
        return webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> JSON.parseObject(json, typeReference))
                .doOnError(e -> log.error("Async call python error: {}", e.getMessage()));
    }

    // ========================== 对外 API ==========================

    /**
     * 获取沪深 kline（日线）
     */
    public static List<KlineData> fetchKlineData(String secid, String market) {
        return fetchKlineDataRange(secid, market, null, null);
    }

    public static List<KlineData> fetchKlineDataRange(
            String secid, String market,
            String startDate, String endDate
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        if (startDate != null) body.put("beg", startDate);
        if (endDate != null) body.put("end", endDate);

        PythonResponse<List<KlineData>> resp = callPythonSync(
                "/stock/kline/range",
                body,
                new TypeReference<>() {
                }
        );

        return safeUnwrap(resp);
    }

    /**
     * 今日 K 线
     */
    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        return fetchKlineDataRange(secid, market, threeDaysAgo, today);
    }

    /**
     * 今日美股 K 线
     */
    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        return fetchUSKlineData(secid, market, DATE_FORMATTER.format(LocalDate.now()),
                DATE_FORMATTER.format(LocalDate.now()));
    }

    public static List<KlineData> fetchUSKlineData(
            String secid, String market,
            String startDate, String endDate
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("secid", secid);
        body.put("market", market);
        body.put("beg", startDate);
        body.put("end", endDate);

        PythonResponse<List<KlineData>> resp = callPythonSync(
                "/stock/kline/us",
                body,
                new TypeReference<>() {
                }
        );
        return safeUnwrap(resp);
    }

    /**
     * 按 ndays 方式趋势 K 线
     */
    public static List<KlineData> fetchKlineDataFiveDay(String secid, String market) {
        if (secid == null || market == null) {
            log.warn("fetchKlineDataFiveDay invalid input secid={}, market={}", secid, market);
            return Collections.emptyList();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 5);

        PythonResponse<List<KlineData>> resp = callPythonSync(
                "/stock/kline",
                body,
                new TypeReference<>() {
                }
        );
        return safeUnwrap(resp);
    }

    /**
     * 历史（ALL）K 线
     */
    public static List<KlineData> fetchKlineDataAll(String secid, String market) {
        if (secid == null || market == null) return Collections.emptyList();

        Map<String, Object> body = new HashMap<>();
        body.put("secid", formatFullSecid(secid, market));
        body.put("ndays", 100000);

        PythonResponse<List<KlineData>> resp = callPythonSync(
                "/stock/kline",
                body,
                new TypeReference<>() {
                }
        );
        return safeUnwrap(resp);
    }

    /**
     * 实时行情
     */
    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        Map<String, Object> body = Map.of("url", apiUrl);
        PythonResponse<StockRealtimeInfo> resp = callPythonSync(
                "/stock/realtime",
                body,
                new TypeReference<>() {
                }
        );
        return resp != null ? resp.getData() : null;
    }

    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        Map<String, Object> body = Map.of("url", apiUrl);
        PythonResponse<EtfRealtimeInfo> resp = callPythonSync(
                "/etf/realtime",
                body,
                new TypeReference<>() {
                }
        );
        return resp != null ? resp.getData() : null;
    }

    // ========================== 辅助 & 安全抽离 ==========================

    private static String formatFullSecid(String secid, String market) {
        return market + "." + secid;
    }

    private static <T> List<T> safeUnwrap(PythonResponse<List<T>> resp) {
        if (resp == null) {
            log.warn("safeUnwrap: resp is null");
            return Collections.emptyList();
        }
        if (resp.getData() == null) {
            log.warn("safeUnwrap: data is null, provider={}, quality={}",
                    resp.getMeta() == null ? null : resp.getMeta().getProvider(),
                    resp.getMeta() == null ? null : resp.getMeta().getQuality());
            return Collections.emptyList();
        }
        return resp.getData();
    }
}
