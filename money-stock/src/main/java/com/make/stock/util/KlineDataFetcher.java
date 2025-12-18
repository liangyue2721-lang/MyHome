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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * K线数据获取工具类 — 调用 Python 脚本（Playwright 或 API 方式）获取股票 K 线数据。
 * 支持：
 * <ul>
 *     <li>沪深股市 K 线数据获取</li>
 *     <li>美股实时数据获取</li>
 *     <li>指定日期区间的数据查询</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *     List<KlineData> list = KlineDataFetcher.fetchKlineData("601138", "1");
 *     List<KlineData> listWithDate = KlineDataFetcher.fetchKlineData("601138", "1", "20240101", "20240331");
 *     KlineData todayUS = KlineDataFetcher.fetchTodayUSKlineData("NVDA", "105");
 * </pre>
 *
 * @author
 * @since 2025-11
 */
@Component
public class KlineDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(KlineDataFetcher.class);

    /**
     * Python 脚本根目录路径（通过配置文件注入）
     */
    @Value("${python.script.path:python}")
    private String pythonScriptPathConfig;

    /**
     * Python 服务 URL
     */
    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrlConfig;

    /**
     * 静态变量，用于静态方法访问配置路径
     */
    private static String pythonScriptPath;
    private static String pythonServiceUrl;
    private static RestTemplate restTemplate;

    /**
     * 日期格式化器（yyyyMMdd）
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 初始化静态变量（在 Spring 容器完成依赖注入后执行）
     */
    @PostConstruct
    public void init() {
        pythonScriptPath = pythonScriptPathConfig;
        pythonServiceUrl = pythonServiceUrlConfig;
        restTemplate = new RestTemplate();
        logger.info("KlineDataFetcher initialized with python script path: {}", pythonScriptPath);
        logger.info("Python Service URL: {}", pythonServiceUrl);
    }

    // ========================== 主方法接口 ==========================

    /**
     * 获取沪深股市 K 线数据（默认全量）
     *
     * @param secid  证券代码（如：601138）
     * @param market 市场代码（1 表示沪市，0 表示深市）
     * @return K 线数据列表
     */
    public static List<KlineData> fetchKlineData(String secid, String market) {
        logger.info("Fetching K-line data for stock: {}, market: {}", secid, market);
        return fetchKlineData(secid, market, null, null);
    }

    /**
     * 获取指定时间段的沪深股市 K 线数据
     *
     * @param secid     证券代码
     * @param market    市场代码
     * @param startDate 开始日期（yyyyMMdd）
     * @param endDate   结束日期（yyyyMMdd）
     * @return K 线数据列表
     */
    public static List<KlineData> fetchKlineData(String secid, String market, String startDate, String endDate) {
        String script = buildScriptPath("kline_playwright.py");
        logger.info("Fetching K-line data for stock: {}, market: {}, from: {} to: {}", secid, market, startDate, endDate);
        return runPythonScript(script, secid, market, startDate, endDate);
    }

    /**
     * 获取今日沪深股市 K 线数据（默认取近三天数据防止当日无交易）
     *
     * @param secid  证券代码
     * @param market 市场代码
     * @return 今日及近三天的 K 线数据
     */
    public static List<KlineData> fetchTodayKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DATE_FORMATTER);
        logger.info("Fetching today's K-line data for stock: {}, market: {}, range: {} to {}", secid, market, threeDaysAgo, today);
        return fetchKlineData(secid, market, threeDaysAgo, today);
    }

    /**
     * 获取今日美股实时 K 线数据（仅取最新一条）
     *
     * @param secid  美股代码（如：AAPL、NVDA）
     * @param market 市场代码（如：105）
     * @return 今日最新 K 线数据
     */
    public static List<KlineData> fetchTodayUSKlineData(String secid, String market) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        logger.info("Fetching today's US K-line data for stock: {}, market: {}, date: {}", secid, market, today);
        return fetchUSKlineData(secid, market, today, today);
    }

    /**
     * 获取美股 K 线数据
     *
     * @param secid     美股代码
     * @param market    市场代码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return K 线数据列表
     */
    public static List<KlineData> fetchUSKlineData(String secid, String market, String startDate, String endDate) {
        String script = buildScriptPath("fetch_stock_realtime.py");
        logger.info("Fetching US K-line data for stock: {}, market: {}, from: {} to: {}", secid, market, startDate, endDate);
        return runPythonScript(script, secid, market, startDate, endDate);
    }

    // ========================== 私有辅助方法 ==========================

    /**
     * 根据系统平台获取 Python 命令。
     * Windows：python
     * Linux / macOS：python3
     *
     * @return Python 命令字符串
     */
    private static String getPythonCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        String pythonCmd = os.contains("win") ? "python" : "python3";
        logger.debug("Determined Python command based on OS '{}': {}", os, pythonCmd);
        return pythonCmd;
    }

    /**
     * 获取沪深股市 K 线数据（使用 HTTP 调用 Python Service）
     *
     * @param secid  证券代码（如：601138）
     * @param market 市场代码（1 表示沪市，0 表示深市）
     * @return K 线数据列表
     */
    public static List<KlineData> fetchKlineDataFiveDay(String secid, String market) {
        String fullSecid = market + "." + secid;
        logger.info("Fetching K-line data via Service for stock: {}", fullSecid);

        try {
            if (restTemplate == null) {
                // Fallback if init didn't run (e.g. static context test)
                restTemplate = new RestTemplate();
                pythonServiceUrl = "http://localhost:8000";
            }

            String url = pythonServiceUrl + "/stock/kline";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("secid", fullSecid);
            requestBody.put("ndays", 5);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return JSON.parseObject(response.getBody(), new TypeReference<List<KlineData>>() {});
            } else {
                logger.error("Service returned status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Failed to fetch kline data via service, falling back to script: {}", e.getMessage());
            // Fallback to script if service fails
            String script = buildScriptPath("trends2_playwright_logger.py");
            return runPythonScript(script, market + "." + secid, "5");
        }
    }

    /**
     * 拉取指定股票的历史日 K 线数据（全历史）
     *
     * <p>说明：</p>
     * <ul>
     *   <li>该方法通过调用 Python 脚本（Playwright）访问东方财富全历史 K 线接口</li>
     *   <li>secid + market 会组合成 {market}.{secid}，例如：1.601138</li>
     *   <li>Python 脚本执行后，会通过标准输出（stdout）返回一行压缩 JSON</li>
     *   <li>runPythonScript 会负责将 Python 的 JSON 转换为 {@code List<KlineData>}</li>
     *   <li>运行过程中产生的日志不输出到控制台，而是写入 Python 独立日志文件（logs/full_kline.log）</li>
     * </ul>
     *
     * <p>核心适用场景：</p>
     * <ul>
     *   <li>初始化股票历史行情数据库（全历史 K 线）</li>
     *   <li>每日增量补全（append-only）</li>
     *   <li>批量股票池扫描、回测数据准备</li>
     * </ul>
     *
     * <p>数据源特点：</p>
     * <ul>
     *   <li>接口：push2his.eastmoney.com/api/qt/stock/kline/get</li>
     *   <li>返回日级别行情（klt=101）</li>
     *   <li>包含 open / close / high / low / volume / amount / pre_close 等</li>
     *   <li>数据范围：beg=0 ~ end=20500101，即全历史</li>
     * </ul>
     *
     * @param secid  股票代码（不含市场标识），如：601138
     * @param market 市场类型（0 深证、1 上证、2 北交所）
     * @return 股票全历史 K 线列表（标准结构 {@code KlineData}）
     * @throws RuntimeException 如果 Python 脚本执行失败、数据无法解析、或 JSON 转换异常
     *
     *                          <p>注意事项：</p>
     *                          <ul>
     *                            <li>**必须保证 Java → Python 参数使用 UTF-8 传输**，否则 secid 可能乱码（变成“ã1.601138”）导致返回空数据</li>
     *                            <li>如果需要跨平台稳定执行，建议在 runPythonScript 内设置：
     *                                {@code pb.environment().put("PYTHONIOENCODING", "utf-8");}</li>
     *                            <li>Python 控制台只返回一行 JSON，不包含日志</li>
     *                            <li>Python 日志写入 logs/full_kline.log，便于生产排查</li>
     *                          </ul>
     */
    public static List<KlineData> fetchKlineDataALL(String secid, String market) {
        // 定位 Python 文件路径：例如 /opt/apps/python/full_kline_playwright.py
        String script = buildScriptPath("hybrid_kline_trends.py");

        // 日志：标记任务开始（不会影响 Python 日志）
        logger.info("Fetching ALL K-line data for stock: {}, market: {}", secid, market);

        // 运行 Python 脚本，参数格式 market.secid，例如：1.601138
        return runPythonScript(script, String.valueOf("\"" + market + "." + secid + "\""));
    }


    /**
     * 构造脚本完整路径。
     *
     * @param scriptName 脚本文件名
     * @return 脚本完整路径
     */
    private static String buildScriptPath(String scriptName) {
        if (pythonScriptPath == null || pythonScriptPath.isEmpty()) {
            logger.debug("Using default script path for script: {}", scriptName);
            return "python/" + scriptName;
        }
        String fullPath = pythonScriptPath.endsWith("/") ? pythonScriptPath + scriptName : pythonScriptPath + "/" + scriptName;
        logger.info("Built full script path: {}", fullPath);
        return fullPath;
    }

    /**
     * 高鲁棒性 Python 脚本执行器（企业级版本）
     */
    private static List<KlineData> runPythonScript(String scriptPath, String... args) {

        List<String> command = new ArrayList<>();
        command.add(getPythonExecutable());
        command.add(scriptPath);

        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.trim().isEmpty()) {
                    command.add(arg);
                }
            }
        }

        logger.info("执行 Python 脚本: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);
        builder.environment().put("PYTHONIOENCODING", "utf-8");

        try {
            Process process = builder.start();

            StreamCollector stdout = new StreamCollector(process.getInputStream(), "STDOUT");
            StreamCollector stderr = new StreamCollector(process.getErrorStream(), "STDERR");

            stdout.start();
            stderr.start();

            int exitCode = process.waitFor();
            stdout.join();
            stderr.join();

            String out = stdout.getContent().trim();
            String err = stderr.getContent().trim();

            logger.info("Python 脚本退出码: {}", exitCode);

            if (exitCode != 0) {
                logger.error("Python 脚本执行失败: {}", err);
                return null;
            }

            if (out.isEmpty() || "None".equalsIgnoreCase(out)) {
                logger.warn("Python 返回空");
                return null;
            }

            // UTF-BOM remove
            out = out.replace("\uFEFF", "").trim();

            // -------- 关键：提取 JSON 数组 --------
            Matcher m = Pattern.compile("\\[[\\s\\S]*\\]").matcher(out);
            if (!m.find()) {
                logger.error("未找到 JSON 数组，原始输出={}", out);
                return null;
            }

            String jsonArray = m.group(0);
            logger.debug("最终解析 JSON={}", jsonArray);

            return JSON.parseObject(jsonArray, new TypeReference<List<KlineData>>() {
            });

        } catch (Exception e) {
            logger.error("执行 Python 脚本异常: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 自动寻找 Python 可执行路径（python3 > python > py）
     */
    private static String getPythonExecutable() {

        if (isCommandAvailable("python3")) return "python3";
        if (isCommandAvailable("python")) return "python";
        if (isCommandAvailable("py")) return "py";

        logger.warn("未找到 python 命令，使用默认 python");
        return "python";
    }

    /**
     * 判断系统命令是否可用
     */
    private static boolean isCommandAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "--version").start();
            return p.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 并发 Stream 读取器，避免阻塞
     */
    private static class StreamCollector extends Thread {

        private final InputStream input;
        private final String name;
        private final StringBuilder buffer = new StringBuilder();

        StreamCollector(InputStream input, String name) {
            this.input = input;
            this.name = name;
        }

        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }
            } catch (IOException e) {
                // 忽略读取异常
            }
        }

        public String getContent() {
            return buffer.toString();
        }
    }


    /**
     * 读取输入流内容为字符串。
     *
     * @param inputStream 输入流
     * @return 字符串内容
     * @throws IOException I/O 异常
     */
    private static String readStream(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        }
    }

    // ========================== 测试入口 ==========================

    public static void main(String[] args) {
        try {
            // Test Service calls
            EtfRealtimeInfo etfInfo = fetchEtfRealtimeInfo("https://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fltt=2&invt=2&fields=f57,f58,f43,f60,f46,f44,f45,f47,f48,f52,f20,f152&secid=1.510050");
            System.out.println("Service Test - ETF: " + (etfInfo != null ? etfInfo.getCompanyName() : "null"));

            List<KlineData> klineData3 = fetchKlineDataFiveDay("601138", "1");
            System.out.println("Service Test - Kline: " + (klineData3 != null ? klineData3.size() : "null"));


        } catch (Exception e) {
            System.err.println("数据获取过程中发生异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取单只股票实时行情（调用 realtime_fetcher.py）
     *
     * @param apiUrl 东财 push2 接口链接，如：
     *               "https://push2.eastmoney.com/api/qt/stock/get?...&secid=1.600036"
     * @return StockRealtimeInfo 或 null（失败 / 无数据）
     */
    public static StockRealtimeInfo fetchRealtimeInfo(String apiUrl) {
        String script = buildScriptPath("realtime_fetcher.py");
        logger.info("Fetching realtime stock info with script: {} url: {}", script, apiUrl);

        // 执行 Python 脚本
        List<String> command = new ArrayList<>();
        command.add(getPythonExecutable());
        command.add(script);
        command.add(apiUrl);

        logger.info("执行 Python 实时行情脚本: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);
        builder.environment().put("PYTHONIOENCODING", "utf-8");

        try {
            Process process = builder.start();

            StreamCollector stdout = new StreamCollector(process.getInputStream(), "STDOUT");
            StreamCollector stderr = new StreamCollector(process.getErrorStream(), "STDERR");

            stdout.start();
            stderr.start();

            int exitCode = process.waitFor();
            stdout.join();
            stderr.join();

            String out = stdout.getContent().trim();
            String err = stderr.getContent().trim();

            logger.info("realtime_fetcher.py 退出码: {}", exitCode);
            if (exitCode != 0) {
                logger.error("实时行情脚本执行失败: {}", err);
                return null;
            }

            if (out.isEmpty() || "None".equalsIgnoreCase(out)) {
                logger.warn("实时行情返回为空");
                return null;
            }

            // 去掉 UTF BOM
            out = out.replace("\uFEFF", "").trim();
            logger.debug("realtime_fetcher 输出: {}", out);

            // 解析 JSON 为 StockRealtimeInfo
            StockRealtimeInfo info = JSON.parseObject(out, StockRealtimeInfo.class);
            return info;

        } catch (Exception e) {
            logger.error("执行实时行情获取异常: ", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 获取 ETF 实时行情信息 (Refactored to use Python Service)
     */
    public static EtfRealtimeInfo fetchEtfRealtimeInfo(String apiUrl) {
        logger.info("Fetching ETF realtime info via Service: {}", apiUrl);
        try {
             if (restTemplate == null) {
                restTemplate = new RestTemplate();
                pythonServiceUrl = "http://localhost:8000";
            }

            String url = pythonServiceUrl + "/etf/realtime";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("url", apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<EtfRealtimeInfo> response = restTemplate.postForEntity(url, entity, EtfRealtimeInfo.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("Service returned status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch ETF data via service, falling back to script. Error: {}", e.getMessage());
            // Fallback Logic
            String script = buildScriptPath("etf_realtime_fetcher.py");
            List<String> cmd = new ArrayList<>();
            cmd.add(getPythonExecutable());
            cmd.add(script);
            cmd.add(apiUrl);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            try {
                Process p = pb.start();
                StreamCollector out = new StreamCollector(p.getInputStream(), "OUT");
                StreamCollector err = new StreamCollector(p.getErrorStream(), "ERR");
                out.start();
                err.start();
                int code = p.waitFor();
                out.join();
                err.join();
                if (code != 0) {
                    logger.error("ETF 实时行情脚本失败: {}", err.getContent());
                    return null;
                }
                String s = out.getContent().replace("\uFEFF", "").trim();
                if (s.isEmpty()) return null;
                return JSON.parseObject(s, EtfRealtimeInfo.class);
            } catch (Exception ex) {
                logger.error("ETF 实时行情异常", ex);
                return null;
            }
        }
    }

}
