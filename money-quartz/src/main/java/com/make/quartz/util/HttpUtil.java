package com.make.quartz.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.make.common.core.redis.RedisCache;
import com.make.stock.domain.StockConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * HTTP工具类
 * <p>
 * 提供发送HTTP请求和解析响应数据的功能，主要用于获取股票相关信息的API数据
 * </p>
 */
public class HttpUtil {

    // 日志记录器，用于记录HTTP请求和数据处理过程中的日志信息
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);


    @Resource
    private static RedisCache redisCache;

    // 重用ObjectMapper实例以提高性能（线程安全）
    private static final ObjectMapper mapper = new ObjectMapper();

    // 最大重试次数常量，限制最多进行3次重试
    private static final int MAX_RETRIES = 3;
    // 初始重试延迟时间（毫秒），采用指数退避策略时首次等待时间
    private static final long INITIAL_RETRY_DELAY_MS = 1000;


    /**
     * 处理单个分页请求
     * <p>
     * 通过StockConfigProperties获取API URL配置，发送HTTP请求并解析返回的JSON数据
     * </p>
     *
     * @param pageNumber 当前页码
     * @return 解析后的JSON数据节点
     * @throws IOException 网络请求或数据解析过程中可能抛出的异常
     */
    public static JsonNode processPage(int pageNumber) throws IOException {
        // 初始化JSON节点为空
        JsonNode jsonNode = null;
        try {
            // 1. 从配置中获取API请求URL
            String apiUrl = StockConfigProperties.getInstance().getStockIssueInfoApiUrl();

            // 2. 发送HTTP请求并解析响应数据
            jsonNode = fetchAndParseStockData(apiUrl);
        } catch (Exception e) {
//            log.error("分页 {} 数据处理失败", pageNumber, e);
            // 重新抛出异常
            throw e;
        }
        // 返回解析后的JSON数据
        return jsonNode;
    }

    /**
     * 发送HTTP GET请求并解析JSON数据
     * <p>
     * 本方法通过HttpURLConnection发送GET请求，支持处理JSON或JSONP格式响应数据，
     * 并将响应内容解析为Jackson的JsonNode对象返回。
     * </p>
     *
     * <p>
     * 功能点：
     * <ul>
     *   <li>设置连接与读取超时时间</li>
     *   <li>校验HTTP响应状态码</li>
     *   <li>支持JSONP格式的剥离（如有callback包装）</li>
     * </ul>
     * </p>
     *
     * @param apiUrl API请求地址，必须是合法的HTTP或HTTPS地址
     * @return 解析后的JsonNode对象，表示响应的JSON结构
     * @throws IOException 网络请求失败或响应格式不符合预期时抛出
     */
    private static JsonNode fetchAndParseStockData(String apiUrl) throws IOException {
        // 创建HttpURLConnection对象并设置为GET方法
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");

        // 设置请求超时时间（毫秒）
        conn.setConnectTimeout(5000); // 连接超时：5秒
        conn.setReadTimeout(5000);    // 读取超时：5秒

        // 设置请求头（模拟浏览器请求）
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            // 校验响应码是否为200（OK）
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("请求失败，HTTP响应码: " + responseCode);
            }

            // 读取整个响应内容为一个字符串
            String response = reader.lines().collect(Collectors.joining());

            // 如果是JSONP格式（如callback({...})），剥离外层
            if (response.startsWith("callback(") && response.endsWith(")")) {
                response = response.substring("callback(".length(), response.length() - 1);
            }

            // 使用Jackson的ObjectMapper解析JSON字符串为JsonNode对象
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response);
        } finally {
            // 确保断开HTTP连接
            conn.disconnect();
        }
    }

    public static JsonNode fetchStockData(String apiUrl) throws IOException {
        return retryWithExponentialBackoff(() -> {
            HttpURLConnection conn = null;
            try {
                // 创建并配置HTTP连接
                conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");

                // 设置合理的超时时间（单位：毫秒）
                conn.setConnectTimeout(15000);  // 15秒连接超时
                conn.setReadTimeout(15000);     // 15秒读取超时

                // 添加User-Agent避免被服务器拒绝
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MyApp/1.0)");

                // 自动跟随重定向（处理3xx状态码）
                conn.setInstanceFollowRedirects(true);

                // 获取并验证HTTP状态码
                int statusCode = conn.getResponseCode();
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    // 读取错误响应体内容
                    String errorMsg = readErrorResponse(conn);
                    throw new IOException("HTTP " + statusCode + ": " + errorMsg);
                }

                // 使用UTF-8编码读取响应流
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                    // 将响应内容合并为单个字符串
                    String response = reader.lines().collect(Collectors.joining());

                    // 去除JSONP包装（如果有）
                    String jsonData = removeCallbackWrapper(response);

                    // 解析JSON并获取data节点
                    JsonNode rootNode = mapper.readTree(jsonData);
                    JsonNode dataNode = rootNode.path("data");

                    // 验证data节点存在性
                    if (dataNode.isMissingNode()) {
                        throw new IOException("Missing 'data' node in JSON");
                    }
                    return dataNode;
                }

            } finally {
                // 确保断开连接释放资源
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String readErrorResponse(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            // 当错误流不可读时返回友好提示
            return "Error reading error stream: " + e.getMessage();
        }
    }

    private static <T> T retryWithExponentialBackoff(Callable<T> callable) throws IOException {
        int retryCount = 0;
        long delay = INITIAL_RETRY_DELAY_MS;

        while (true) {  // 通过retryCount限制实际循环次数
            try {
                return callable.call();
            } catch (SocketException | SocketTimeoutException e) {
                // 只对网络相关异常进行重试
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    throw new IOException("Failed after " + MAX_RETRIES + " retries", e);
                }

                // 打印重试日志（实际项目建议使用Logger）
                System.out.printf("Retry %d/%d after %dms due to: %s%n",
                        retryCount, MAX_RETRIES, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    // 恢复中断状态并抛出
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }
                delay *= 2;  // 指数退避：每次延迟时间翻倍
            } catch (Exception e) {
                // 包装非IOException为受检异常
                throw new IOException("Unhandled exception in request", e);
            }
        }
    }

    private static String removeCallbackWrapper(String response) {
        // 查找第一个左括号和最后一个右括号
        int start = response.indexOf('(');
        int end = response.lastIndexOf(')');

        // 验证是否找到有效包裹结构
        if (start != -1 && end != -1 && end > start) {
            // 截取括号内的JSON内容
            return response.substring(start + 1, end);
        }
        // 无包裹结构时返回原始内容
        return response;
    }

    /**
     * 发送HTTP GET请求并解析JSON数据（模拟浏览器行为）
     * <p>
     * 本方法通过设置常见的浏览器请求头（User-Agent、Accept等）来模拟真实浏览器访问，
     * 有助于绕过部分服务器的反爬机制。支持自定义Cookie，并自动处理重定向。
     * </p>
     *
     * @param apiUrl API请求地址，必须是合法的HTTP或HTTPS地址
     * @param cookie 可选的Cookie字符串，用于身份认证或会话维持
     * @return 解析后的JsonNode对象，表示响应的JSON结构
     * @throws IOException 网络请求失败或响应格式不符合预期时抛出
     */
    private static JsonNode fetchAndParseStockDataSimulateBrowser(String apiUrl, String cookie) throws IOException {
        // 创建并配置HTTP连接
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);  // 设置连接超时时间为5秒
        conn.setReadTimeout(5000);     // 设置读取超时时间为5秒

        // 设置常见浏览器请求头，模拟浏览器行为
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        conn.setRequestProperty("Connection", "keep-alive");
//        conn.setRequestProperty("Referer", "https://example.com/");  // 根据目标站点填写合适的Referer
        conn.setRequestProperty("Host", new URL(apiUrl).getHost());  // 自动设置Host头部

        // 如果提供了Cookie，则添加到请求头中
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }

        // 允许自动跟随重定向（处理3xx状态码）
        conn.setInstanceFollowRedirects(true);

        // 获取并验证HTTP响应状态码
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("请求失败，HTTP 响应码: " + responseCode);
        }

        // 使用UTF-8编码读取响应流
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String response = reader.lines().collect(Collectors.joining());
        reader.close();

        // 注意：如果返回的是压缩内容(gzip/deflate)，还需解压InputStream（此处省略）

        // 解析JSON并返回根节点
        return mapper.readTree(response);
    }


}
