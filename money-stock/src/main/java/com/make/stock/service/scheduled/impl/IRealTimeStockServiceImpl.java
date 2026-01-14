package com.make.stock.service.scheduled.impl;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.make.common.constant.CacheConstants;
import com.make.common.exception.business.BusinessException;
import com.make.common.utils.ThreadPoolUtil;
import com.make.stock.domain.StockConfigProperties;
import com.make.stock.service.scheduled.IRealTimeStockService;
import com.make.stock.util.DateUtil;
import com.make.stock.util.email.SendEmail;
import com.make.stock.domain.KlineData;
import com.make.stock.domain.StockInfoDongfang;
import com.make.stock.domain.StockInfoDongfangHis;
import com.make.stock.domain.StockListingNotice;
import com.make.stock.domain.dto.StockDataChain;
import com.make.stock.domain.dto.StockInfoDongFangChain;
import com.make.stock.service.IStockInfoDongfangHisService;
import com.make.stock.service.IStockInfoDongfangService;
import com.make.stock.service.IStockListingNoticeService;
import com.make.stock.util.KlineDataFetcher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.make.common.core.redis.RedisCache;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


/**
 * 实时股票数据服务实现类
 * <p>
 * 该类负责处理实时股票数据的获取、缓存和同步到数据库等功能。
 * 主要包括从东方财富等数据源获取实时股票数据，
 * 将数据缓存到Redis中，并定期同步到数据库中
 * </p>
 *
 * @author 84522
 */
@Service
public class IRealTimeStockServiceImpl implements IRealTimeStockService {

    /**
     * 日志记录器，用于记录服务执行过程中的日志信息
     */
    private static final Logger log = LoggerFactory.getLogger(IRealTimeStockServiceImpl.class);


    /**
     * 东方财富股票信息服务，用于查询和操作股票信息数据
     */
    @Resource
    private IStockInfoDongfangService stockInfoDongfangService;

    @Resource
    private IStockListingNoticeService stockListingNoticeService;

    /**
     * 获取动态阈值配置
     * <p>
     * 该方法用于获取动态阈值配置，目前返回空的周和月阈值列表
     * </p>
     *
     * @return 包含周和月阈值列表的Map
     */
    @Override
    public Map<String, List<String>> getDynamicThreshold() {
        // 创建用于存储阈值配置的Map
        Map<String, List<String>> dataMap = new HashMap<>();
        // 创建周阈值列表
        List<String> weekList = new ArrayList<>();
        // 创建月阈值列表
        List<String> monthList = new ArrayList<>();
        // 将周阈值列表添加到Map中
        dataMap.put("week", weekList);
        // 将月阈值列表添加到Map中
        dataMap.put("month", monthList);
        // 返回阈值配置Map
        return dataMap;
    }

    /**
     * Redis缓存服务，用于缓存实时股票数据
     */
    @Resource
    private RedisCache redisCache;

    // 全局线程安全 Map，用于合并多线程数据，key 为股票代码，value 为股票对象
    private final ConcurrentHashMap<String, StockInfoDongFangChain> globalStockMap = new ConcurrentHashMap<>();


    // ======================== ✅ 新实现逻辑 BEGIN ========================

    /**
     * 刷新 Redis 中缓存的实时股票数据（替代原先的内存变量）
     * <p>
     * 逻辑步骤：
     * 1. 判断当前是否为交易时间（节假日或非交易时段不更新）
     * 2. 多线程分页拉取实时股票数据
     * 3. 合并结果后写入 Redis，并设置缓存过期时间（1小时）
     * </p>
     */
    @Override
    public void refreshInMemoryMapEntries() {
        // 判断当前是否为交易时间
        if (!DateUtil.isCurrentTimeInRange()) {
            // 如果不处于交易时间，记录日志并返回
            log.info("不处于交易时间,不需要更新实时股票数据");
            return;
        }

        try {
            // 拉取并合并全部实时数据
            List<StockDataChain> allStockList = collectRealTimeAllStock();

            // 判断获取到的股票数据是否为空
            if (!allStockList.isEmpty()) {
//                // 写入 Redis 并设置缓存时间
//                redisCache.setCacheList(REALTIME_STOCK_ALL_KEY, allStockList);
//                // 设置缓存过期时间为1小时
//                redisCache.setExpireTime(REALTIME_STOCK_ALL_KEY, 1, TimeUnit.HOURS);
//                // 记录成功缓存日志
//                log.info("实时股票数据已成功缓存至 Redis，共 {} 条", allStockList.size());
            } else {
                // 如果股票数据为空，记录警告日志
                log.warn("实时股票数据为空，未写入缓存");
            }
        } catch (Exception e) {
            // 记录刷新失败的错误日志
            log.error("实时股票数据刷新失败", e);
        }
    }

    /**
     * 收集所有股票的实时数据，分批分页处理并汇总结果
     * <p>
     * 通过多线程方式并发处理多个分页请求，提高数据获取效率
     * </p>
     *
     * @return 包含所有分页处理结果的股票数据链表
     */
    private List<StockDataChain> collectRealTimeAllStock() {
        // 创建用于存储所有股票数据的列表
        List<StockDataChain> resultList = new ArrayList<>();
        // 创建用于存储Future结果的列表
        List<Future<List<StockDataChain>>> futures = new ArrayList<>();

//        // 分页提交任务，使用自定义线程池执行
//        for (int page = 1; page <= 283; page++) {
//            // 定义当前页码的final变量，以便在lambda表达式中使用
//            final int currentPage = page;
        // 提交分页处理任务到线程池，并将Future结果添加到列表中
        futures.add(ThreadPoolUtil.getCoreExecutor().submit(() -> processPage()));
//        }

        // 等待所有分页任务完成，并汇总所有结果
        for (Future<List<StockDataChain>> future : futures) {
            try {
                // 获取分页处理结果
                List<StockDataChain> pageList = future.get();
                // 将分页结果添加到总结果列表中
                if (pageList != null) {
                    resultList.addAll(pageList);
                }
            } catch (InterruptedException e) {
                // 记录分页任务被中断的错误日志
                log.error("分页任务被中断", e);
                // 恢复中断状态
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // 记录分页任务执行异常的错误日志
                log.error("分页任务执行异常", e);
            }
        }

        // 返回所有股票数据列表
        return resultList;
    }


    /**
     * 拉取并处理单页实时股票数据
     * <p>
     * 通过API获取指定页码的股票数据，并解析为StockDataChain对象列表
     * </p>
     *
     * @return 当前页的所有股票数据
     */
    private List<StockDataChain> processPage() {
        // 创建用于存储当前页股票数据的列表
        List<StockDataChain> pageResult = new ArrayList<>();
        try {
            // 构建API请求URL
            String apiUrl = buildApiUrl();
            // 发送HTTP请求并解析股票JSON数据
            JsonNode stockData = fetchAndParseStockData(apiUrl);

            // 判断返回的数据是否为数组格式
            if (stockData.isArray()) {
                // 遍历数组中的每个元素
                for (JsonNode element : stockData) {
                    // 创建StockDataChain对象
                    StockDataChain stockDataChain = createStockDataChain(element);
                    // 将对象添加到结果列表中
                    pageResult.add(stockDataChain);
                }
            } else {
                // 如果数据格式异常，记录警告日志
                log.warn("分页 {} 的数据格式异常");
            }
        } catch (Exception e) {
            // 记录分页数据处理失败的错误日志
            log.error("分页 {} 数据处理失败", e);
        }
        // 返回当前页股票数据列表
        return pageResult;
    }


    /**
     * 创建 StockDataChain 对象（安全解析版本）
     * <p>
     * 通过调用StockDataChain的parse方法将JsonNode解析为StockDataChain对象
     * </p>
     *
     * @param element JSON节点数据
     * @return 解析后的StockDataChain对象
     */
    private StockDataChain createStockDataChain(JsonNode element) {
        // 调用StockDataChain的parse方法解析JSON节点数据
        return StockDataChain.parse(element);
    }


    /**
     * 构建 API 请求 URL
     * <p>
     * 根据页码参数格式化API请求URL
     * </p>
     *
     * @return 格式化后的API请求URL
     */
    private String buildApiUrl() {
        // 使用String.format方法将页码参数格式化到API URL中
        return String.format(
                StockConfigProperties.getApiUrl() // 直接通过类名调用静态方法
//                pageNumber // 直接使用int值，String.format会自动处理类型转换
        );
    }

    /**
     * 发送 HTTP 请求并解析股票 JSON 数据
     * <p>
     * 通过HttpURLConnection发送GET请求，处理JSONP格式的响应数据并解析为JsonNode对象
     * </p>
     *
     * @param apiUrl 接口地址
     * @return JsonNode 解析后的 diff 数据节点
     * @throws IOException 请求或解析异常
     */
    private JsonNode fetchAndParseStockData(String apiUrl) throws IOException {
        // 初始化HttpURLConnection对象
        HttpURLConnection conn = null;
        // 初始化BufferedReader对象
        BufferedReader reader = null;

        try {
            // 创建HttpURLConnection对象并设置请求方法为GET
            conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            // 设置请求方法为GET
            conn.setRequestMethod("GET");
            // 设置连接超时时间为5000毫秒
            conn.setConnectTimeout(5000);
            // 设置读取超时时间为5000毫秒
            conn.setReadTimeout(5000);
            // 设置Accept请求头
            conn.setRequestProperty("Accept", "application/json,*/*");

            // 获取响应状态码
            int statusCode = conn.getResponseCode();
            // 判断状态码是否为HTTP_OK
            if (statusCode != HttpURLConnection.HTTP_OK) {
                // 如果状态码不是HTTP_OK，抛出IOException异常
                throw new IOException("请求失败，状态码: " + statusCode);
            }

            // 创建BufferedReader对象用于读取响应数据
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            // 读取响应数据
            String response = reader.lines().collect(Collectors.joining());

            // 去除回调函数名称，获取纯JSON数据
            String jsonData = removeCallbackFunction(response);
            // 创建ObjectMapper对象
            ObjectMapper mapper = new ObjectMapper();
            // 解析JSON数据并返回diff节点
            return mapper.readTree(jsonData).path("data").path("diff");
        } finally {
            // 关闭BufferedReader
            if (reader != null) {
                try {
                    // 关闭reader
                    reader.close();
                } catch (IOException ignore) {
                    // 忽略关闭异常
                }
            }
            // 断开HTTP连接
            if (conn != null) {
                // 断开连接
                conn.disconnect();
            }
        }
    }

    /**
     * 发送 HTTP 请求并解析股票 JSON 数据
     * <p>
     * 如果返回 JSONP 格式，则自动提取括号内的 JSON；
     * 如果返回标准 JSON，则直接解析。
     * </p>
     *
     * @param apiUrl 接口地址
     * @return JsonNode data 节点
     * @throws IOException 请求或解析异常
     */
    private JsonNode fetchAdataStockData(String apiUrl) throws IOException {
        // 添加重试机制，最多重试3次
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpURLConnection conn = null;
            BufferedReader reader = null;

            try {
                conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                // 增加连接和读取超时时间到10秒
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json,*/*");
                conn.setRequestProperty("Accept-Encoding", "gzip");

                int statusCode = conn.getResponseCode();
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("请求失败，状态码: " + statusCode);
                }

                InputStream inputStream = conn.getInputStream();
                if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
                    inputStream = new GZIPInputStream(inputStream);
                }

                String response = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining());

                // 仅在返回是 JSONP 时才去掉回调函数
                String jsonData = response.trim();
                if (jsonData.startsWith("{") && jsonData.endsWith("}")) {
                    // 标准JSON，直接使用
                } else if (jsonData.contains("(") && jsonData.contains(")")) {
                    jsonData = removeCallbackFunction(jsonData);
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonData);

                // 返回 data 节点
                return root.path("data");
            } catch (SocketTimeoutException e) {
                log.warn("第 {} 次尝试获取数据超时，URL: {}", attempt, apiUrl);
                if (attempt == maxRetries) {
                    // 如果已经是最后一次尝试，抛出异常
                    throw new IOException("获取数据多次超时，已达到最大重试次数: " + maxRetries, e);
                }
                // 等待一段时间再重试
                try {
                    Thread.sleep(2000); // 等待2秒再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("重试过程中被中断", ie);
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignore) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        // 理论上不会到达这里
        throw new IOException("获取数据失败，未知错误");
    }


    /**
     * 去除回调函数名称，即去掉回调函数名称和括号
     * <p>
     * 处理JSONP格式的响应数据，提取其中的纯JSON数据
     * </p>
     *
     * @param jsonResponse 回调函数包裹的JSON字符串
     * @return 处理后的纯JSON字符串
     */
    public static String removeCallbackFunction(String jsonResponse) {
        // 判断是否有回调函数的包裹：前缀和后缀分别是 '(' 和 ')'
        if (jsonResponse != null && jsonResponse.length() > 2 &&
                jsonResponse.contains("(") && jsonResponse.contains(")")) {
            // 查找第一个 '(' 和最后一个 ')'
            int startIndex = jsonResponse.indexOf("(") + 1;
            int endIndex = jsonResponse.lastIndexOf(")");

            // 如果 startIndex 和 endIndex 合理，截取中间的内容
            if (startIndex > 0 && endIndex > startIndex) {
                // 截取JSON数据部分
                jsonResponse = jsonResponse.substring(startIndex, endIndex);
            }
        }
        // 返回处理后的JSON字符串
        return jsonResponse;
    }

    /**
     * 批量将缓存中的实时股票数据同步到数据库中
     * <p>
     * 数据来源：Redis 中的实时股票列表缓存（key = stock:realtime:all）
     * 同步策略：
     * - 若数据库中已存在，则进行 update
     * - 否则执行 insert
     * </p>
     * <p>
     * 每 1000 条批量提交一次
     * </p>
     *
     * @throws BusinessException 当数据同步过程中发生错误时抛出
     */
    public void batchSyncStockDataToDB() {
        // 从 Redis 中获取缓存的实时股票数据列表
        List<StockInfoDongFangChain> cacheList = redisCache.getCacheList(CacheConstants.REALTIME_STOCK_ALL_DONGFANG_KEY);
        // 记录启动财富数据同步日志
        log.info("启动财富数据同步，待处理记录数：{}", cacheList != null ? cacheList.size() : 0);

        // 判断缓存列表是否为空
        if (CollectionUtils.isEmpty(cacheList)) {
            // 如果缓存中没有任何股票数据，记录警告日志并返回
            log.warn("缓存中没有任何股票数据，跳过同步任务");
            return;
        }

        // 收集所有股票代码，添加空值检查
        Set<String> allStockCodes = cacheList != null ?
                cacheList.stream()
                        // 映射获取股票代码
                        .map(StockInfoDongFangChain::getStockCode)
                        // 收集为Set集合
                        .collect(Collectors.toSet()) :
                // 如果cacheList为null，则创建空的Set
                new HashSet<>();

        // 将Set集合转换为List
        List<String> allStockCodesList = new ArrayList<>(allStockCodes);

        // 批量查询已存在的股票信息
        List<StockInfoDongfang> existingStockList = stockInfoDongfangService.queryIDByCodes(allStockCodesList);

        // 转换为 Map 方便判断是否存在
        Map<String, StockInfoDongfang> existingStockMap = existingStockList.stream()
                // 收集为Map，键为股票代码，值为股票信息对象
                .collect(Collectors.toMap(StockInfoDongfang::getStockCode, stockInfo -> stockInfo));

        // 创建用于存储插入数据的列表
        List<StockInfoDongfang> insertBatch = new ArrayList<>();
        // 创建用于存储更新数据的列表
        List<StockInfoDongfang> updateBatch = new ArrayList<>();

        // 设置批次大小为1000
        int batchSize = 1000;
        // 初始化已处理记录数为0
        int processedCount = 0;

        // 判断缓存列表是否为null
        if (cacheList == null) {
            // 如果缓存列表为null，直接返回
            return;
        }
        Map<String, StockInfoDongfang> listingNoticeMap = new HashMap<>();
        StockListingNotice listingNotice = new StockListingNotice();
        // 遍历缓存列表中的每个股票数据
        for (StockInfoDongFangChain chain : cacheList) {
            // 查询所有上市通知记录
            listingNotice.setSecurityCode(chain.getStockCode());
            List<StockListingNotice> stockListingNotices = stockListingNoticeService.selectStockListingNoticeList(listingNotice);

            // 增加已处理记录数
            processedCount++;
            try {
                // 将实时数据转换为数据库实体
                StockInfoDongfang entity = convertToEntity(chain);
                // 设置类型为2（东方财富标识）
                entity.setType(2L);
                if (!CollectionUtils.isEmpty(stockListingNotices)) {
                    for (StockListingNotice stockListingNotice : stockListingNotices) {
                        listingNoticeMap.put(stockListingNotice.getSecurityCode(), entity);
                    }
                }
                // 判断数据库中是否已存在该股票数据
                if (existingStockMap.containsKey(entity.getStockCode())) {
                    // 已存在，执行更新
                    StockInfoDongfang existing = existingStockMap.get(entity.getStockCode());
                    // 设置ID
                    entity.setId(existing.getId());
                    // 将实体添加到更新批次列表中
                    updateBatch.add(entity);
                } else {
                    // 不存在，执行插入
                    // 将实体添加到插入批次列表中
                    insertBatch.add(entity);
                }

                // 达到批次大小时，批量操作数据库
                if (insertBatch.size() >= batchSize) {
                    // 批量插入股票信息
                    stockInfoDongfangService.batchInsertStockInfoDongfang(insertBatch);
                    // 记录批量插入成功日志
                    log.info("东方财富批量插入成功，插入记录数：{}", insertBatch.size());
                    // 清空插入批次列表
                    insertBatch.clear();
                }

                if (updateBatch.size() >= batchSize) {
                    // 批量更新股票信息
                    stockInfoDongfangService.batchUpdateStockInfoDongfang(updateBatch);
                    // 记录批量更新成功日志
                    log.info("东方财富批量更新成功，更新记录数：{}", updateBatch.size());
                    // 清空更新批次列表
                    updateBatch.clear();
                }

                // 每 100 条记录输出进度
                if (processedCount % 100 == 0) {
                    // 记录数据同步进度日志
                    log.info("东方财富数据同步进度，已处理：{}条", processedCount);
                }
            } catch (Exception e) {
                // 记录数据处理异常日志
                log.error("东方财富数据处理异常，股票代码：{}，错误详情：{}", chain.getStockCode(), e.getMessage(), e);
                // 抛出业务异常
                throw new BusinessException("DATA_SYNC_ERROR，股票数据同步失败，代码：" + chain.getStockCode(), e);
            }
        }

        // 处理剩余未满批次的数据
        if (!insertBatch.isEmpty()) {
            // 批量插入剩余的股票信息
            stockInfoDongfangService.batchInsertStockInfoDongfang(insertBatch);
            // 记录批量插入成功日志
            log.info("东方财富批量插入成功，插入记录数：{}", insertBatch.size());
        }

        if (!updateBatch.isEmpty()) {
            // 批量更新剩余的股票信息
            stockInfoDongfangService.batchUpdateStockInfoDongfang(updateBatch);
            // 记录批量更新成功日志
            log.info("东方财富批量更新成功，更新记录数：{}", updateBatch.size());
        }

        // 记录数据同步完成日志
        log.info("东方财富数据同步完成，总处理记录数：{}", processedCount);

        // 提交异步任务：根据股票代码查询 Redis 内存中的数据并更新
        ThreadPoolUtil.getCoreExecutor().submit(() -> {
            try {
                // 从 Redis 中获取最新的实时股票数据
                List<StockInfoDongFangChain> latestCacheList = redisCache.getCacheList(CacheConstants.REALTIME_STOCK_ALL_DONGFANG_KEY);
                if (CollectionUtils.isEmpty(latestCacheList)) {
                    log.warn("Redis 中没有找到实时股票数据，跳过更新");
                    return;
                }

                // 查询所有上市通知记录
                List<StockListingNotice> stockListingNotices = stockListingNoticeService.selectStockListingNoticeList(new StockListingNotice());

                // 处理每条上市通知记录
                for (StockListingNotice stockListingNotice : stockListingNotices) {
                    // 根据证券代码获取最新的股票数据
                    StockInfoDongfang stockInfoDongFang = listingNoticeMap.get(stockListingNotice.getSecurityCode());
                    if (stockInfoDongFang != null) {
                        // 将最新数据转换为数据库实体
                        if (stockInfoDongFang.getPrice() != null) {
                            // 更新当前价格
                            stockListingNotice.setCurrentPrice(stockInfoDongFang.getPrice());
                            // 处理已存在的股票上市记录
                            processExistingStockListing(stockListingNotice);
                        }
                    }
                }

                // 记录更新完成日志
                log.info("根据 Redis 数据更新股票信息完成，共处理 {} 条记录", stockListingNotices.size());
            } catch (Exception e) {
                // 记录异常日志
                log.error("根据 Redis 数据更新股票信息时发生异常", e);
            }
        });
    }

    /**
     * 对象转换（使用MapStruct示例）
     * <p>
     * 将StockInfoDongFangChain对象转换为StockInfoDongfang数据库实体对象
     * </p>
     *
     * @param chain StockInfoDongFangChain对象
     * @return StockInfoDongfang数据库实体对象
     */
    private StockInfoDongfang convertToEntity(StockInfoDongFangChain chain) {
        // 创建StockInfoDongfang实体对象
        StockInfoDongfang entity = new StockInfoDongfang();

        // 基础信息
        // 设置股票代码
        entity.setStockCode(chain.getStockCode());
        // 设置股票简称
        entity.setTicker(generateTicker(chain.getStockCode()));
        // 设置公司名称
        entity.setCompanyName(chain.getCompanyName());
        // 设置市场类别
        entity.setMarketCategory(BigDecimal.valueOf(chain.getMarketType()));

        // 价格相关
        // 设置当前价格
        entity.setPrice(BigDecimal.valueOf(chain.getPrice())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        // 设置开盘价
        entity.setOpenPrice(BigDecimal.valueOf(chain.getOpenPrice())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        // 设置最高价
        entity.setHighPrice(BigDecimal.valueOf(chain.getHighPrice())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        // 设置最低价
        entity.setLowPrice(BigDecimal.valueOf(chain.getLowPrice())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        // 设置收盘价
        entity.setClosePrice(BigDecimal.valueOf(chain.getPrevClose())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));

        // 成交数据
        // 设置成交量
        entity.setVolume(BigDecimal.valueOf(chain.getVolume()));
        // 设置成交额
        entity.setTradingVolume(BigDecimal.valueOf(chain.getTurnover()));
//        entity.setAmplitude(chain.getAmplitude());
//        entity.setTurnoverRate(chain.getTurnoverRate());

        // 资金流向
//        entity.setMainFundsInflow(chain.getMainFundsInflow());
//        entity.setInstitutionalFlow(chain.getInstitutionalFlow());
//        entity.setRetailFlow(chain.getRetailFlow());
//
//        // 财务指标
//        entity.setPeRatio(chain.getPeRatio());
//        entity.setPbRatio(chain.getPbRatio());
//        entity.setEps(chain.getEps());
//        entity.setGrossMargin(chain.getGrossMargin());
//        entity.setRoe(chain.getRoe());
//
//        // 流通数据
//        entity.setCirculatingMarketValue(chain.getCirculatingMarketValue());
//        entity.setCirculatingShares(chain.getCirculatingShares());
        // 设置总股本
        entity.setTotalShares(BigDecimal.valueOf(chain.getTotalShares()));

        // 技术分析指标
        // 设置涨跌额
        entity.setNetChange(BigDecimal.valueOf(chain.getNetChange())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        // 设置涨跌幅
        entity.setNetChangePercentage(BigDecimal.valueOf(chain.getNetChangePercentage())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
//        entity.setVolumeRatio(chain.getVolumeRatio());
//        entity.setCommissionRatio(chain.getCommissionRatio());
//        entity.setCommissionDifference(chain.getCommissionDifference());
//        entity.setVolumePriceTrend(chain.getVolumePriceTrend());
//        entity.setDividendYield(chain.getDividendYield());

        // 额外处理
        // 1. 市值计算（若需要单独存储）
        // 判断价格和流通股本是否为空
        if (chain.getPrice() != null && chain.getCirculatingShares() != null) {
            // 计算市值
            entity.setMarketValue(BigDecimal.valueOf(chain.getPrice() * chain.getCirculatingShares()));
        }
//
//        // 2. 委比有效性校验
//        if (chain.getCommissionBuy() != null && chain.getCommissionSell() != null) {
//            double commissionRatio = (chain.getCommissionBuy() - chain.getCommissionSell()) /
//                    (chain.getCommissionBuy() + chain.getCommissionSell());
//            entity.setCommissionRatio(commissionRatio);
//        }

        // 3. 时间戳处理（假设需要）
        // 设置更新时间
        entity.setUpdateTime(new Date());

        // 返回转换后的实体对象
        return entity;
    }

    /**
     * 将 StockInfoChain 实例转换为 StockInfoDongfang 数据库实体对象
     * <p>
     * 说明：
     * - 金额类字段（如价格、成交额）统一除以 100 处理（来源为分、厘等单位）
     * - 非必需字段增加 null 判断，避免空指针
     * - 所有字段含义参照东财接口字段映射说明
     * </p>
     *
     * @param chain StockInfoChain 实例（来自缓存链表或接口返回）
     * @return StockInfoDongfang 实体对象
     */
    private StockInfoDongfang convertToEntity(StockDataChain chain) {
        // 创建StockInfoDongfang实体对象
        StockInfoDongfang entity = new StockInfoDongfang();

        // 基础信息映射
        // 设置股票代码
        entity.setStockCode(chain.getSecurityCode());
        // 设置股票简称
        entity.setTicker(generateTicker(chain.getSecurityCode()));
        // 设置公司名称
        entity.setCompanyName(chain.getSecurityName());
        // 设置市场类别
        entity.setMarketCategory(BigDecimal.valueOf(chain.getMarketCode()));

        // 实时价格数据（价格单位：分 → 元）
        // 判断最新价格是否为空
        if (chain.getLatestPrice() != null) {
            // 设置当前价格
            entity.setPrice(chain.getLatestPrice().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }

        // 判断涨跌额是否为空
        if (chain.getChangeAmount() != null) {
            // 设置涨跌额
            entity.setNetChange(chain.getChangeAmount().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }

        // 判断涨跌幅是否为空
        if (chain.getChangePercent() != null) {
            // 设置涨跌幅
            entity.setNetChangePercentage(chain.getChangePercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }

        // 成交量、成交额
        // 判断成交量是否为空
        if (chain.getVolume() != null) {
            // 设置成交量
            entity.setVolume(BigDecimal.valueOf(chain.getVolume()));
        }

        // 判断成交额是否为空
        if (chain.getTurnover() != null) {
            // 设置成交额
            entity.setTradingVolume(chain.getTurnover());
        }

//        // 市值数据
//        entity.setMarketValue(chain.getTotalMarketValue());           // 总市值
//        entity.setCirculatingMarketValue(chain.getCirculatingValue()); // 流通市值
//
//        // 财务指标
//        entity.setPeRatio(chain.getPeRatio());                        // 市盈率
//        entity.setPbRatio(chain.getPbRatio());                        // 市净率

        // 技术指标及股本信息
        // 判断总市值和最新价格是否为空
        if (chain.getTotalMarketValue() != null && chain.getLatestPrice() != null) {
            // 总市值 ÷ 当前股价 = 总股本（股）
            try {
                // 计算总股本
                BigDecimal shares = chain.getTotalMarketValue()
                        .divide(chain.getLatestPrice(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)); // 注意价格单位转换
                // 设置总股本
                entity.setTotalShares(shares);
            } catch (ArithmeticException ignored) {
                // 忽略计算异常
            }
        }

        // 停牌状态等标志（如需存储，可扩展字段）
//        entity.setSuspended(chain.isSuspended() ? 1 : 0);          // 停牌标志（0/1）
//        entity.setStFlag(chain.isStRiskWarning() ? 1 : 0);         // ST风险标志（0/1）

        // 更新时间戳（如原始字段含毫秒时间戳）
        // 判断更新时间戳是否为空
        if (chain.getUpdateTimestamp() != null) {
            // 设置更新时间
            entity.setUpdateTime(new Date(chain.getUpdateTimestamp()));
        } else {
            // 使用当前时间作为更新时间
            entity.setUpdateTime(new Date());
        }

        // 返回转换后的实体对象
        return entity;
    }


    /**
     * 生成股票代码简称
     * <p>
     * Generate stock ticker symbol
     * </p>
     *
     * @param stockCode 股票代码
     * @return 股票简称
     */
    private String generateTicker(String stockCode) {
        // 判断股票代码长度是否大于3
        return stockCode.length() > 3 ? stockCode.substring(0, 3) : stockCode;
    }

    /**
     * 刷新内存中的东方财富股票数据
     * <p>
     * 该方法用于刷新内存中的东方财富股票数据，
     * 在交易时间内通过API获取最新的股票数据并存储到Redis中
     * </p>
     */
    @Override
    public void refreshWealthInMemoryMapEntries() {
        // 判断当前是否为交易时间
        if (!DateUtil.isCurrentTimeInRange()) {
            // 如果不处于交易时间，记录日志并返回
            log.info("不处于交易时间,不需要更新Redis中东方财富的股票数据");
            return;
        }
        try {
            // 收集实时东方财富股票数据
            collectRealTimeWealthAllStock();
        } catch (Exception e) {
            // 记录更新失败的错误日志
            log.error("更新Redis中东方财富的股票数据失败", e);
        }
        // 记录更新完成日志
        log.info("更新Redis中东方财富的股票数据完成");
    }

    /**
     * 并发收集来自多个API的实时股票数据，覆盖沪深京A股、创业板、科创板等多个板块
     * <p>
     * 通过线程池并行执行API请求，提高数据采集效率。
     * 使用工具类线程池，替代直接使用CORE_EXECUTOR。
     * </p>
     */
    private void collectRealTimeWealthAllStock() {
        try {
            // 1. 获取配置单例，读取各板块的API地址（支持配置热更新）
            StockConfigProperties config = StockConfigProperties.getInstance();

            // 2. 收集所有板块对应的API地址列表
            List<String> apiList = Arrays.asList(
                    config.getShanghaiShenzhenBeijingAStockApi(),   // 沪深京A股（主板）
                    config.getShanghaiAStockApi(),                   // 上证A股（600/601/603等）
                    config.getShenzhenAStockApi(),                   // 深证A股（000/001/002等）
                    config.getChiNextStockApi(),                      // 创业板（300）
                    config.getStarMarketStockApi(),                   // 科创板（688）
                    config.getBeijingAStockApi(),                      // 北交所（83/87开头）
                    config.getBStockApi(),                             // B股（美元/港元计价）
                    config.getIpoStockApi(),                           // 新股申购
                    config.getStStockApi(),                            // 风险警示板
                    config.getShanghaiStockConnectApi(),              // 沪股通
                    config.getShenzhenStockConnectApi()               // 深股通
            );

            // 3. 将每个API地址对应的请求任务封装为Future，准备提交线程池
            List<Future<Void>> futures = new ArrayList<>();
            // 遍历API地址列表
            for (String apiUrl : apiList) {
                // 将每个API请求任务提交到线程池并保存Future
                Future<Void> future = ThreadPoolUtil.getCoreExecutor().submit(() -> {
                    processApiRequest(apiUrl);
                    return null;
                });
                futures.add(future);
            }

            // 4. 等待所有任务完成
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    log.error("任务被中断", e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("任务执行异常", e);
                }
            }

        } catch (Exception e) {
            // 5. 异常捕获，保证异常日志记录，避免任务悄然失败
            log.error("API请求数据处理失败", e);
        }
    }


    /**
     * 处理单个API请求及分页数据获取
     * <p>
     * 该方法用于处理一个API地址的请求，分页获取股票数据并进行数据处理。
     * 通过替换API请求中的分页参数，循环请求并获取各页的数据。
     * </p>
     *
     * @param apiUrl API地址：提供具体的API接口地址，通过分页获取股票数据
     */
    private void processApiRequest(String apiUrl) {
        try {
//            // 通过循环处理分页获取数据，示例处理的是第一页到第600页的股票数据
            for (int i = 1; i <= 600; i++) {
                // 动态替换API请求中的分页参数，将"pn=1"替换为实际的页码
                String formattedUrl = apiUrl.replace("pn=1", "pn=" + i);
//            String formattedUrl = apiUrl;
                try {
                    // 发起HTTP请求并解析API响应的股票数据
                    JsonNode stockData = fetchAndParseStockData(formattedUrl);

                    // 校验数据格式，确保API返回的内容是有效的JSON数据
                    if (stockData == null) {
                        // 如果API响应为空，则跳过该请求，并继续下一个分页请求
                        log.warn("API响应为空: {}", formattedUrl);
                        // 继续下一个分页请求
                        return;
                    }

                    // 处理有效数据：检查数据是否是数组格式并处理
                    if (stockData.isArray()) {
                        // 如果返回的数据是一个数组，调用方法处理股票数据
//                    processWealthStockAllData(stockData);
                        processWealthStockAllDataJSON(stockData);
                    } else {
                        // 如果API请求返回的数据格式异常，则认为该页数据无效，跳出循环
                        log.warn("API请求超时数据格式异常，", apiUrl);
                        // 跳出循环
                        return;
                    }
                } catch (Exception e) {
                    // 捕获并记录异常，继续尝试下一个分页请求
                    log.warn("API响应异常: {}", formattedUrl);
                }
            }
        } catch (Exception e) {
            // 捕获整个请求处理过程中的异常，并记录日志
            log.error("API数据处理失败，API: {}", apiUrl, e);
        }
    }


    /**
     * 批量将 Redis 中的实时股票数据同步到数据库
     * <p>
     * 新版本：基于 Redis 单条存储方案
     * - Redis 结构：
     * 1. 每条股票数据存储在 key：REALTIME_STOCK_SINGLE:<stockCode>
     * 2. 所有股票代码存储在 Set：REALTIME_STOCK_CODES
     * - 同步逻辑：
     * 1. 从 Redis Set 获取所有股票代码
     * 2. 批量获取每个股票的最新数据
     * 3. 判断数据库中是否存在，决定 insert 或 update
     * 4. 每 1000 条批量提交
     * </p>
     *
     * @throws BusinessException 当同步过程中发生错误时抛出
     */
    @Override
    public void batchSyncStockDataToDB2() {
        // 1. 获取 Redis 中存储的所有股票代码
        Set<String> allStockCodes = redisCache.getCacheSetAll(CacheConstants.REALTIME_STOCK_CODES_KEY);

        log.info("启动股票数据同步，Redis 中股票数量：{}", (allStockCodes != null ? allStockCodes.size() : 0));

        if (CollectionUtils.isEmpty(allStockCodes)) {
            log.warn("Redis 中没有股票数据，跳过同步任务");
            return;
        }

        // 2. 将 Set 转为 List
        List<String> allStockCodesList = new ArrayList<>(allStockCodes);

        // 3. 批量查询数据库中已存在的股票信息
        List<StockInfoDongfang> existingStockList = stockInfoDongfangService.queryIDByCodes(allStockCodesList);
        Map<String, StockInfoDongfang> existingStockMap = existingStockList.stream()
                .collect(Collectors.toMap(StockInfoDongfang::getStockCode, stock -> stock));

        // 4. 创建插入和更新的批次列表
        List<StockInfoDongfang> insertBatch = new ArrayList<>();
        List<StockInfoDongfang> updateBatch = new ArrayList<>();
        int batchSize = 1000;
        int processedCount = 0;

        // 5. 遍历股票代码，逐条获取 Redis 数据并处理
        for (String stockCode : allStockCodesList) {
            try {
                // 拼接单条股票 Redis key
                String stockKey = CacheConstants.REALTIME_STOCK_SINGLE_KEY + stockCode;

                // 从 Redis 获取股票数据 JSON
                String stockJson = redisCache.getCacheObject(stockKey);
                if (StringUtils.isEmpty(stockJson)) {
                    continue; // 如果 Redis 没有数据，跳过
                }

                // 解析 JSON 为对象
                StockInfoDongFangChain chain = JSON.parseObject(stockJson, StockInfoDongFangChain.class);

                // 转换为数据库实体
                StockInfoDongfang entity = convertToEntity(chain);
                entity.setType(2L); // 东方财富标识

                // 判断数据库中是否已存在
                if (existingStockMap.containsKey(entity.getStockCode())) {
                    // 已存在，更新
                    entity.setId(existingStockMap.get(entity.getStockCode()).getId());
                    updateBatch.add(entity);
                } else {
                    // 不存在，插入
                    insertBatch.add(entity);
                }

                processedCount++;

                // 达到批次大小，执行数据库操作
                if (insertBatch.size() >= batchSize) {
                    stockInfoDongfangService.batchInsertStockInfoDongfang(insertBatch);
                    log.info("批量插入成功，插入 {} 条", insertBatch.size());
                    insertBatch.clear();
                }
                if (updateBatch.size() >= batchSize) {
                    stockInfoDongfangService.batchUpdateStockInfoDongfang(updateBatch);
                    log.info("批量更新成功，更新 {} 条", updateBatch.size());
                    updateBatch.clear();
                }

                // 每 100 条输出进度日志
                if (processedCount % 100 == 0) {
                    log.info("同步进度：已处理 {} 条", processedCount);
                }
            } catch (Exception e) {
                log.error("处理股票代码 {} 时发生异常：{}", stockCode, e.getMessage(), e);
                throw new BusinessException("DATA_SYNC_ERROR，股票数据同步失败，代码：" + stockCode, e);
            }
        }

        // 6. 处理剩余未提交的数据
        if (!insertBatch.isEmpty()) {
            stockInfoDongfangService.batchInsertStockInfoDongfang(insertBatch);
            log.info("剩余数据批量插入完成，数量：{}", insertBatch.size());
        }
        if (!updateBatch.isEmpty()) {
            stockInfoDongfangService.batchUpdateStockInfoDongfang(updateBatch);
            log.info("剩余数据批量更新完成，数量：{}", updateBatch.size());
        }

        log.info("股票数据同步完成，总处理 {} 条", processedCount);


        // 7. 异步任务：处理上市通知逻辑（基于最新 Redis 数据）
        ThreadPoolUtil.getCoreExecutor().submit(() -> {
            try {
                // 查询所有上市通知记录
                List<StockListingNotice> stockListingNotices = stockListingNoticeService.selectStockListingNoticeList(new StockListingNotice());

                // 处理每条上市通知记录
                for (StockListingNotice stockListingNotice : stockListingNotices) {
                    // 拼接单条股票 Redis key
                    String stockKey = CacheConstants.REALTIME_STOCK_SINGLE_KEY + stockListingNotice.getSecurityCode();

                    // 从 Redis 获取股票数据 JSON
                    String stockJson = redisCache.getCacheObject(stockKey);
                    if (StringUtils.isEmpty(stockJson)) {
                        continue; // 如果 Redis 没有数据，跳过
                    }
                    if (stockListingNotice.getSecurityCode().equals("001221")) {
                        log.info("001221: {}", stockJson);
                    }
                    // 解析 JSON 为对象
                    StockInfoDongFangChain chain = JSON.parseObject(stockJson, StockInfoDongFangChain.class);

                    // 转换为数据库实体
                    StockInfoDongfang stockInfoDongFang = convertToEntity(chain);
                    stockInfoDongFang.setType(2L); // 东方财富标识

                    // 根据证券代码获取最新的股票数据
                    if (stockInfoDongFang.getPrice() != null) {
                        // 更新当前价格
                        stockListingNotice.setCurrentPrice(stockInfoDongFang.getPrice());
                        // 处理已存在的股票上市记录
                        processExistingStockListing(stockListingNotice);
                    } else {
                        log.info("股票代码：{}，价格为空", stockListingNotice.getSecurityCode());
                    }
                }

                // 记录更新完成日志
                log.info("根据 Redis 数据更新股票信息完成，共处理 {} 条记录", stockListingNotices.size());
            } catch (Exception e) {
                // 记录异常日志
                log.error("根据 Redis 数据更新股票信息时发生异常", e);
            }
        });

//        // 7. 异步任务：处理上市通知逻辑（基于最新 Redis 数据）
//        ThreadPoolUtil.getCoreExecutor().submit(() -> {
//            try {
//                List<StockListingNotice> stockListingNotices = stockListingNoticeService
//                        .selectStockListingNoticeList(new StockListingNotice());
//                for (StockListingNotice stockListingNotice : stockListingNotices) {
//                    String stockKey = "REALTIME_STOCK_SINGLE:" + stockListingNotice.getSecurityCode();
//                    String stockJson = redisCache.getCacheObject(stockKey);
//                    if (StringUtils.isNotEmpty(stockJson)) {
//                        StockInfoDongFangChain chain = JSON.parseObject(stockJson, StockInfoDongFangChain.class);
//                        if (chain.getPrice() != null) {
//                            stockListingNotice.setCurrentPrice(BigDecimal.valueOf(chain.getPrice()));
//                            processExistingStockListing(stockListingNotice);
//                        }
//                    }
//                }
//                log.info("异步更新上市通知完成，处理 {} 条记录", stockListingNotices.size());
//            } catch (Exception e) {
//                log.error("异步更新上市通知时发生异常", e);
//            }
//        });
    }

    @Resource
    private IStockInfoDongfangHisService stockInfoDongfangHisService;

    /**
     * 异步获取所有东方财富股票历史数据并批量入库（分离数据生产与入库）
     */
    @Override
    public void getHistoryDataStock() {
        // 1️⃣ 获取全部股票列表
        List<StockInfoDongfang> stockList = stockInfoDongfangService.selectStockInfoDongfangList(new StockInfoDongfang());
        if (CollectionUtils.isEmpty(stockList)) {
            log.warn("未获取到任何股票数据，任务结束。");
            return;
        }

        // 成功与失败计数
        int successCount = 0;
        int failCount = 0;

        // 数据生产阶段结果集合（所有股票的历史数据）
        List<StockInfoDongfangHis> allDataList = new ArrayList<>();

        // ===========================
        // 🔹 第1阶段：数据生产（只采集数据，不入库）
        // ===========================
        for (StockInfoDongfang stock : stockList) {
            try {
                List<KlineData> klineData = KlineDataFetcher.fetchKlineData(stock.getStockCode(), getMarketCode(stock.getStockCode()));
                if (klineData == null || klineData.isEmpty()) {
                    log.warn("股票 {} 数据为空或格式错误", stock.getStockCode());
                    failCount++;
                    continue;
                }

                for (KlineData klineDatum : klineData) {
                    StockInfoDongfangHis entity = new StockInfoDongfangHis();
                    entity.setCompanyName(stock.getCompanyName());
                    entity.setStockCode(stock.getStockCode());
                    entity.setInDate(parseDate(klineDatum.getTradeDate()));
                    entity.setPrice(klineDatum.getClose() != null ? BigDecimal.valueOf(klineDatum.getClose()) : BigDecimal.ZERO);
                    entity.setVolume(klineDatum.getVolume() != null ? new BigDecimal(klineDatum.getVolume()) : BigDecimal.ZERO);
                    entity.setNetChange(klineDatum.getChange() != null ? BigDecimal.valueOf(klineDatum.getChange()) : BigDecimal.ZERO);
                    entity.setNetChangePercentage(klineDatum.getChangePercent() != null ? BigDecimal.valueOf(klineDatum.getChangePercent()) : BigDecimal.ZERO);
                    entity.setHighPrice(klineDatum.getHigh() != null ? BigDecimal.valueOf(klineDatum.getHigh()) : BigDecimal.ZERO);
                    entity.setLowPrice(klineDatum.getLow() != null ? BigDecimal.valueOf(klineDatum.getLow()) : BigDecimal.ZERO);
                    entity.setOpenPrice(klineDatum.getOpen() != null ? BigDecimal.valueOf(klineDatum.getOpen()) : BigDecimal.ZERO);
                    entity.setClosePrice(klineDatum.getClose() != null ? BigDecimal.valueOf(klineDatum.getClose()) : BigDecimal.ZERO);
                    entity.setTradingVolume(klineDatum.getVolume() != null ? BigDecimal.valueOf(klineDatum.getVolume()) : BigDecimal.ZERO);
                    entity.setType(BigDecimal.valueOf(3));
                    entity.setMarketCategory(stock.getMarketCategory());
                    allDataList.add(entity);
                }

            } catch (Exception e) {
                log.error("处理股票 {} 历史数据时异常", stock.getStockCode(), e);
                failCount++;
            }
        }

        // ===========================
        // 🔹 第2阶段：数据入库（按批次批量插入）
        // ===========================
        if (CollectionUtils.isEmpty(allDataList)) {
            log.warn("未生成任何可入库的历史数据，任务结束。");
            return;
        }

        int batchSize = 2000;
        List<StockInfoDongfangHis> batchBuffer = new ArrayList<>(batchSize);

        for (StockInfoDongfangHis his : allDataList) {
            batchBuffer.add(his);
            if (batchBuffer.size() >= batchSize) {
                try {
                    stockInfoDongfangHisService.batchInsertStockInfoDongfangHis(new ArrayList<>(batchBuffer));
                    successCount += batchBuffer.size();
                    log.info("批量插入 {} 条历史数据", batchBuffer.size());
                } catch (Exception e) {
                    log.error("批量插入失败：", e);
                    failCount += batchBuffer.size();
                } finally {
                    batchBuffer.clear();
                }
            }
        }

        // 处理最后剩余的批次
        if (!batchBuffer.isEmpty()) {
            try {
                stockInfoDongfangHisService.batchInsertStockInfoDongfangHis(new ArrayList<>(batchBuffer));
                log.info("插入剩余批次 {} 条历史数据", batchBuffer.size());
                successCount += batchBuffer.size();
            } catch (Exception e) {
                log.error("最后批量插入失败：", e);
                failCount += batchBuffer.size();
            }
        }

        log.info("历史数据获取任务完成 ✅ 成功: {}，失败: {}", successCount, failCount);
    }


    /**
     * 获取股票当天历史数据（仅保存当天日期的数据）
     * 去除多线程与远程请求，统一使用 KlineDataFetcher.fetchKlineData() 获取。
     * 全局使用 java.util.Date，不使用 LocalDate。
     */
    @Override
    public void getHistoryDataStockNewDay() {
        // 1️⃣ 获取全部股票列表
        List<StockInfoDongfang> stockList = stockInfoDongfangService.selectStockInfoDongfangList(new StockInfoDongfang());
        if (CollectionUtils.isEmpty(stockList)) {
            log.warn("未获取到任何股票数据，任务结束。");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        // 2️⃣ 批量插入缓冲区
        List<StockInfoDongfangHis> batchBuffer = new ArrayList<>();
        int batchSize = 2000;

        // 3️⃣ 获取今天日期（只保留年月日）
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = sdf.format(today);

        for (StockInfoDongfang stock : stockList) {
            try {
                // 4️⃣ 获取K线数据（全量）
                List<KlineData> klineData = KlineDataFetcher.fetchKlineData(stock.getStockCode(), getMarketCode(stock.getStockCode()));
                if (klineData == null || klineData.isEmpty()) {
                    log.warn("股票 {} 数据为空或格式错误", stock.getStockCode());
                    failCount++;
                    continue;
                }

                List<StockInfoDongfangHis> tempList = new ArrayList<>();

                for (KlineData klineDatum : klineData) {
                    // 5️⃣ 解析日期并仅保留当天数据
                    Date tradeDate = parseDate(klineDatum.getTradeDate());
                    if (tradeDate == null) {
                        log.warn("股票 {} 日期解析失败: {}", stock.getStockCode(), klineDatum.getTradeDate());
                        continue;
                    }
                    String tradeDateStr = sdf.format(tradeDate);
                    if (!todayStr.equals(tradeDateStr)) {
                        continue; // 跳过非今日数据
                    }

                    // 6️⃣ 构建入库实体
                    StockInfoDongfangHis entity = new StockInfoDongfangHis();
                    entity.setCompanyName(stock.getCompanyName());
                    entity.setStockCode(stock.getStockCode());
                    entity.setInDate(tradeDate);
                    entity.setPrice(klineDatum.getClose() != null ? BigDecimal.valueOf(klineDatum.getClose()) : BigDecimal.ZERO);
                    entity.setVolume(klineDatum.getVolume() != null ? new BigDecimal(klineDatum.getVolume()) : BigDecimal.ZERO);
                    entity.setNetChange(klineDatum.getChange() != null ? BigDecimal.valueOf(klineDatum.getChange()) : BigDecimal.ZERO);
                    entity.setNetChangePercentage(klineDatum.getChangePercent() != null ? BigDecimal.valueOf(klineDatum.getChangePercent()) : BigDecimal.ZERO);
                    entity.setHighPrice(klineDatum.getHigh() != null ? BigDecimal.valueOf(klineDatum.getHigh()) : BigDecimal.ZERO);
                    entity.setLowPrice(klineDatum.getLow() != null ? BigDecimal.valueOf(klineDatum.getLow()) : BigDecimal.ZERO);
                    entity.setOpenPrice(klineDatum.getOpen() != null ? BigDecimal.valueOf(klineDatum.getOpen()) : BigDecimal.ZERO);
                    entity.setClosePrice(klineDatum.getClose() != null ? BigDecimal.valueOf(klineDatum.getClose()) : BigDecimal.ZERO);
                    entity.setTradingVolume(klineDatum.getVolume() != null ? BigDecimal.valueOf(klineDatum.getVolume()) : BigDecimal.ZERO);
                    entity.setType(BigDecimal.valueOf(3));
                    entity.setMarketCategory(stock.getMarketCategory());
                    tempList.add(entity);
                }

                // 若当天数据为空，则跳过
                if (tempList.isEmpty()) {
                    log.debug("股票 {} 无今日数据", stock.getStockCode());
                    continue;
                }

                // 7️⃣ 加入批量缓冲
                //batchBuffer.addAll(tempList);
                //if (batchBuffer.size() >= batchSize) {
                //    try {
                //        List<StockInfoDongfangHis> insertBatch = new ArrayList<>(batchBuffer);
                //        batchBuffer.clear();
                //        stockInfoDongfangHisService.batchInsertStockInfoDongfangHis(insertBatch);
                //        log.info("批量插入 {} 条当日历史数据", insertBatch.size());
                //        successCount += insertBatch.size();
                //    } catch (Exception e) {
                //        log.error("批量插入失败：", e);
                //        failCount += tempList.size();
                //    }
                //}
            } catch (Exception e) {
                log.error("处理股票 {} 当日历史数据时异常", stock.getStockCode(), e);
                failCount++;
            }
        }

        // 8️⃣ 插入最后剩余批次
        if (!batchBuffer.isEmpty()) {
            try {
                stockInfoDongfangHisService.batchInsertStockInfoDongfangHis(new ArrayList<>(batchBuffer));
                log.info("插入剩余批次 {} 条当日历史数据", batchBuffer.size());
                successCount += batchBuffer.size();
            } catch (Exception e) {
                log.error("最后批量插入失败：", e);
                failCount += batchBuffer.size();
            }
        }

        log.info("当日历史数据获取任务完成 ✅ 成功: {}，失败: {}", successCount, failCount);
    }


    /**
     * 处理股票数据并逐条更新到 Redis
     * <p>
     * 将 JSON 数组格式的股票数据解析为对象，并逐条写入 Redis：
     * 1. 使用股票代码作为唯一标识，每条数据单独缓存
     * 2. 如果 Redis 中已存在该股票数据，则覆盖更新（保留最新）
     * 3. 同时维护一个股票代码集合，方便后续全量查询
     * </p>
     *
     * @param stockData JSON 数组格式的股票数据
     */
    private void processWealthStockAllDataJSON(JsonNode stockData) {
        try {
            // 遍历 JSON 数组中的每个元素
            for (JsonNode element : stockData) {
                // 解析 JSON 节点为 StockInfoDongFangChain 对象
                StockInfoDongFangChain stock = StockInfoDongFangChain.parse(element);
                String securityCode = stock.getStockCode();

                // 单条股票缓存的 Redis 键，例如 REALTIME_STOCK_SINGLE:600000
                String stockKey = CacheConstants.REALTIME_STOCK_SINGLE_KEY + securityCode;

                // 将对象序列化为 JSON 字符串
                String stockJson = JSON.toJSONString(stock);
                if (securityCode.equals("001221")) {
                    log.info("001221: {}", stockJson);
                }
                // 写入 Redis：单条股票缓存，有效期 1 小时（覆盖旧值）
                redisCache.deleteObject(stockKey);
                redisCache.setCacheObject(stockKey, stockJson);

                // 将股票代码加入股票集合，用于全量查询
                redisCache.addCacheSetValue(CacheConstants.REALTIME_STOCK_CODES_KEY, securityCode);
            }

            // 记录日志，输出本次处理股票数量
            log.info("东方实时股票数据已逐条写入 Redis，共 {} 条", stockData.size());
        } catch (Exception e) {
            log.error("逐条写入股票数据到 Redis 失败", e);
//            throw new RuntimeException("处理股票数据异常", e);
        }
    }

    /**
     * 处理股票数据并合并到全局缓存，然后写入 Redis
     * <p>
     * 将 JSON 数组格式的股票数据解析为对象，并与全局缓存合并：
     * 1. 如果股票代码重复，保留最新的数据（后来的覆盖之前的）
     * 2. 合并后，统一写入 Redis 缓存，缓存有效期为 1 小时
     * </p>
     *
     * @param stockData JSON 数组格式的股票数据
     */
    private void processWealthStockAllData(JsonNode stockData) {
        try {
            // 遍历 JSON 数组中的每个元素
            for (JsonNode element : stockData) {
                // 解析 JSON 节点为 StockInfoDongFangChain 对象
                StockInfoDongFangChain stock = StockInfoDongFangChain.parse(element);
                // 将股票对象放入全局 ConcurrentHashMap，使用股票代码作为唯一标识
                // 如果股票代码重复，后出现的记录会覆盖之前的记录（保留最新的数据）
                globalStockMap.put(stock.getStockCode(), stock);
            }

            // 合并后的全量股票数据转换为列表
            List<StockInfoDongFangChain> stockList = new ArrayList<>(globalStockMap.values());

            // 写入 Redis 缓存：key 为全市场缓存键名，例如 "REALTIME_STOCK_ALL"
            redisCache.setCacheList(CacheConstants.REALTIME_STOCK_ALL_DONGFANG_KEY, stockList);

            // 设置缓存过期时间为 1 小时
            redisCache.setExpireTime(CacheConstants.REALTIME_STOCK_ALL_DONGFANG_KEY, 1, TimeUnit.HOURS);

            // 记录日志，输出当前全局缓存数据条数
            log.info("东方实时股票数据已合并并缓存至 Redis，共 {} 条", stockList.size());
        } catch (Exception e) {
            // 记录错误日志
            log.error("解析并缓存股票数据失败", e);
            // 抛出运行时异常
            throw new RuntimeException("处理股票数据异常", e);
        }
    }

    /**
     * 根据证券代码（ticker）返回所属板块的中文名称。
     *
     * @param ticker 证券代码（如 "600519"）
     * @return 所属板块中文名（如 "沪市主板"），若无法识别则返回 "未知板块"
     */
    private String getChineseBoardName(String ticker) {
        if (ticker == null || ticker.length() < 3) {
            return "未知板块";
        }

        // 截取前三位代码
        String prefix3 = ticker.substring(0, 3);

        if (prefix3.startsWith("300") || prefix3.startsWith("301")) {
            return "创业板";
        } else if (prefix3.startsWith("688")) {
            return "科创板";
        } else if (prefix3.startsWith("200")) {
            return "深市B股";
        } else if (prefix3.startsWith("900")) {
            return "沪市B股";
        } else if (prefix3.startsWith("002")) {
            return "中小板";
        } else if (prefix3.startsWith("600") || prefix3.startsWith("601")
                || prefix3.startsWith("603") || prefix3.startsWith("605")) {
            return "沪市主板";
        } else if (prefix3.startsWith("000") || prefix3.startsWith("001")
                || prefix3.startsWith("003")) {
            return "深市主板";
        } else if (ticker.startsWith("8") || ticker.startsWith("920")) {
            return "北交所";
        }

        return "未知板块";
    }

    /**
     * 处理已存在的股票上市记录
     *
     * @param existing 已存在的上市公告记录
     */
    private void processExistingStockListing(StockListingNotice existing) {
        if (existing == null || existing.getCurrentPrice() == null || existing.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal issuePrice = existing.getIssuePrice();
        BigDecimal currentPrice = existing.getCurrentPrice();

        // 计算净利润和利润率
        BigDecimal netProfit = BigDecimal.ZERO;
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (issuePrice != null && issuePrice.compareTo(BigDecimal.ZERO) > 0) {
            netProfit = currentPrice.subtract(issuePrice);
            profitMargin = netProfit.divide(issuePrice, 4, RoundingMode.HALF_UP);
        }
        existing.setNetProfit(netProfit);
        existing.setProfitMargin(profitMargin);

        // 获取板块信息
        String boardName = getChineseBoardName(existing.getSecurityCode());

        // 检查是否需要发送通知
        boolean needUpdate = false;
        Integer notifyCount = existing.getNotifyCount() != null ? existing.getNotifyCount() : 0;

        if (notifyCount < 2 && profitMargin.compareTo(new BigDecimal("-0.8")) < 0) {
            if (isMainBoard(boardName)) {
                if (sendNotification(existing, "新股可以买了")) {
                    existing.setNotifyCount(notifyCount + 1);
                    needUpdate = true;
                }
            }
        } else if (notifyCount < 3 && profitMargin.compareTo(new BigDecimal("0.8")) > 0) {
            if (isMainBoard(boardName)) {
                if (sendNotification(existing, "新股可以卖了")) {
                    existing.setNotifyCount(notifyCount + 1);
                    needUpdate = true;
                }
            }
        }

        // 只在必要时更新数据库
//        if (needUpdate || existing.getCurrentPrice() != null) {
        existing.setUpdatedAt(new Date());
        stockListingNoticeService.updateStockListingNotice(existing);
//        }
    }

    /**
     * 判断是否主板
     */
    private boolean isMainBoard(String boardName) {
        return !"未知板块".equals(boardName) &&
                (boardName.equals("沪市主板") || boardName.equals("中小板") || boardName.equals("深市主板"));
    }

    /**
     * 发送通知并记录日志
     */
    private boolean sendNotification(StockListingNotice existing, String message) {
        try {
            SendEmail.notification(JSON.toJSONString(existing), existing.getSecurityName() + message);
            log.info("发送通知成功：{}，股票：{}", message, existing.getSecurityCode());
            return true;
        } catch (Exception e) {
            log.error("发送通知失败：股票：{}，原因：{}", existing.getSecurityCode(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将字符串日期解析为Date对象
     *
     * @param dateStr 日期字符串，格式为 yyyy-MM-dd
     * @return Date对象
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dateStr);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }

    /**
     * 根据股票代码判断所属交易所
     *
     * @param stockCode 股票代码，例如 "600519"、"300750"、"430047"
     * @return 市场代码：
     * <ul>
     *     <li>"1" - 上海证券交易所（上交所）</li>
     *     <li>"0" - 深圳证券交易所（深交所）</li>
     *     <li>"2" - 北京证券交易所（北交所）</li>
     *     <li>null - 无法识别的代码</li>
     * </ul>
     */
    public static String getMarketCode(String stockCode) {
        if (stockCode == null || stockCode.length() < 2) {
            return null;
        }

        String prefix = stockCode.substring(0, 2);

        // 1️⃣ 上海证券交易所（上交所）：以 60 或 68 开头
        if (prefix.startsWith("60") || prefix.startsWith("68")) {
            return "1";
        }

        // 2️⃣ 深圳证券交易所（深交所）：以 00 或 30 开头
        if (prefix.startsWith("00") || prefix.startsWith("30")) {
            return "0";
        }

        // 3️⃣ 北京证券交易所（北交所）：以 43、83、87、88 开头
        if (prefix.startsWith("43") || prefix.startsWith("83")
                || prefix.startsWith("87") || prefix.startsWith("88")) {
            return "2";
        }

        // 未匹配返回 null
        return null;
    }

    /**
     * 获取交易所后缀标识（例如用于拼接股票代码）
     *
     * @param stockCode 股票代码
     * @return 交易所后缀，例如 ".SH"、".SZ"、".BJ"；无法识别返回 null
     */
    public static String getExchangeSuffix(String stockCode) {
        String marketCode = getMarketCode(stockCode);
        if (marketCode == null) {
            return null;
        }

        switch (marketCode) {
            case "1":
                return ".SH"; // 上海证券交易所
            case "0":
                return ".SZ"; // 深圳证券交易所
            case "2":
                return ".BJ"; // 北京证券交易所
            default:
                return null;
        }
    }
    public static void main(String[] args) {
        String base = "192.168.1.";
        int start = 2;
        int end = 111;

        // 固定 10 个线程
        ExecutorService pool = Executors.newFixedThreadPool(10);

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        for (int i = start; i <= end; i++) {
            final String ip = base + i;

            pool.execute(() -> {
                try {
                    Process process = Runtime.getRuntime().exec(
                            isWindows ? "ping -n 1 " + ip : "ping -c 1 " + ip
                    );

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream())
                    );

                    String line;
                    boolean reachable = false;

                    while ((line = reader.readLine()) != null) {
                        if (line.contains("ttl") || line.contains("TTL")) {
                            reachable = true;
                        }
                    }

                    process.waitFor();

                    if (reachable) {
                        System.out.println("🟢 在线: " + ip);
                    } else {
                        System.out.println("🔴 离线: " + ip);
                    }

                } catch (Exception e) {
                    System.out.println("⚠️ 探测失败: " + ip + " - " + e.getMessage());
                }
            });
        }

        // 不再提交新任务，等待执行完
        pool.shutdown();
    }
}
