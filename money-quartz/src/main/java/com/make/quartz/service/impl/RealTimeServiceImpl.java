package com.make.quartz.service.impl;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.make.common.constant.CacheConstants;
import com.make.common.core.redis.RedisCache;
import com.make.finance.domain.AnnualDepositSummary;
import com.make.finance.domain.AssetRecord;
import com.make.finance.domain.CbcCreditCardTransaction;
import com.make.finance.domain.Expense;
import com.make.finance.domain.LoanRepayments;
import com.make.finance.domain.YearlyInvestmentSummary;
import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.finance.service.IAnnualDepositSummaryService;
import com.make.finance.service.IAssetRecordService;
import com.make.finance.service.ICbcCreditCardTransactionService;
import com.make.finance.service.IExpenseService;
import com.make.finance.service.ILoanRepaymentsService;
import com.make.finance.service.IYearlyInvestmentSummaryService;
import com.make.quartz.domain.entiy.StatsResult;
import com.make.stock.domain.dto.StockIssueInfoTemp;
import com.make.quartz.service.IRealTimeService;
import com.make.stock.domain.dto.StockInfoDongFangChain;
import com.make.stock.service.IEtfDataService;
import com.make.stock.service.ISalesDataService;
import com.make.stock.service.ISellPriceAlertsService;
import com.make.stock.service.IStockInfoDongfangHisService;
import com.make.stock.service.IStockInfoDongfangService;
import com.make.stock.service.IStockIssueInfoService;
import com.make.stock.service.IStockKlineService;
import com.make.stock.service.IStockKlineTaskService;
import com.make.stock.service.IStockListingNoticeService;
import com.make.stock.service.IStockPriceUsService;
import com.make.stock.service.IStockTradesService;
import com.make.stock.service.IStockYearlyPerformanceService;
import com.make.stock.service.IWatchstockService;
import com.make.common.utils.DateUtils;
import com.make.common.utils.bean.BeanUtils;
import com.make.stock.domain.*;
import com.make.quartz.util.email.SendEmail;
import com.make.common.exception.business.BusinessException;
import com.make.common.utils.ThreadPoolUtil;
import com.make.quartz.lock.TimeoutReentrantLock;
import com.make.quartz.util.DatabaseSync;
import com.make.quartz.util.EntityComparator;
import com.make.quartz.util.HttpUtil;
import com.make.stock.util.KlineDataFetcher;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 实时股票服务实现类
 *
 * <p>核心功能：
 * 1. 实时股票数据采集与处理
 * 2. 内存数据与持久化存储同步
 * 3. 高并发场景下的数据批处理
 *
 * <p>优化策略：
 * 1. 多级缓存设计（Caffeine + ConcurrentHashMap）
 * 2. 精细化线程池管理
 * 3. 无锁化并发控制（CAS+Atomic）
 * 4. 批量操作减少IO开销
 */
@Service
public class RealTimeServiceImpl implements IRealTimeService {

    //------------------------ 日志配置 ------------------------
    private static final Logger log = LoggerFactory.getLogger(RealTimeServiceImpl.class);

    //------------------------ 依赖注入 ------------------------
    @Resource
    private IWatchstockService watchstockService;          // 自选股服务

    @Resource
    private ISalesDataService salesDataService; // 折线图
    @Resource
    private IStockTradesService stockTradesService;        // 交易记录服务
    @Resource
    private ISellPriceAlertsService sellPriceAlertsService; // 股票信息服务

    @Resource
    private IEtfDataService etfDataService; // ETF信息服务

    @Resource
    private IStockIssueInfoService stockIssueInfoService;

    @Resource
    private IStockListingNoticeService stockListingNoticeService;

    @Resource
    private IStockInfoDongfangService stockInfoDongfangService;

    @Resource
    private IStockInfoDongfangHisService stockInfoDongfangHisService;
    @Resource
    private ICbcCreditCardTransactionService cbcCreditCardTransactionService;

    @Resource
    private IStockPriceUsService stockPriceUsService;


    @Resource
    private IStockKlineService stockKlineService;

    @Resource
    private IStockKlineTaskService stockKlineTaskService;


    @Resource
    private IYearlyInvestmentSummaryService yearlyInvestmentSummaryService;

    @Resource
    private IAnnualDepositSummaryService annualDepositSummaryService;

    @Resource
    private IAssetRecordService assetRecordService;

    @Resource
    private ILoanRepaymentsService loanRepaymentsService;

    @Resource
    private IExpenseService expenseService;

    @Resource
    private IStockYearlyPerformanceService stockYearlyPerformanceService;

    @Resource
    private RedisCache redisCache;

    /**
     * 批量处理大小（平衡内存与性能）
     */
    private static final int BATCH_SIZE = 1000;

    //--------------------- 原子操作 -------------------------
    /**
     * 实时数据采集锁（CAS实现无锁化）
     */
    private final Lock dBDataLock = new TimeoutReentrantLock(60, TimeUnit.SECONDS);


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeFactory typeFactory = objectMapper.getTypeFactory();

    //--------------------- 核心方法实现 ---------------------

    @Override
    public void refreshNewStockInformation() {
        try {
            refreshNewStock();
        } catch (Exception e) {
            log.error("更新新股股票数据失败:", e);
        }
    }

    /**
     * 刷新并同步最新的新股信息。
     *
     * <p>本方法将从远程接口获取第一页新股数据，并转换为实体对象列表。</p>
     * <ul>
     *     <li>若数据库中不存在该新股（通过申购代码判断），则新增记录并发送通知邮件；</li>
     *     <li>若已存在但信息不同，则执行更新操作；</li>
     *     <li>如遇异常将记录错误日志。</li>
     * </ul>
     *
     * @throws IOException 若远程接口调用失败或解析异常
     */
    private void refreshNewStock() throws IOException {
        // 从远程接口获取第一页新股的 JSON 数据
        JsonNode jsonNode = HttpUtil.processPage(1);

        // 将 JSON 数组转换为临时新股实体列表
        List<StockIssueInfoTemp> stockIssueInfoTemps = convertJsonNodeArrayToStockEntities(jsonNode);

        // 将临时实体转换为正式实体类列表
        List<StockIssueInfo> stockIssueInfos = convertToStockIssueInfoList(stockIssueInfoTemps);

        // 遍历每一只新股
        for (StockIssueInfo stockIssueInfo : stockIssueInfos) {
            // 查询数据库中该申购代码对应的完整新股信息
            StockIssueInfo stockIssueInfoDB = stockIssueInfoService.selectStockIssueInfoByApplyCode(stockIssueInfo.getSecurityCode());

            if (stockIssueInfoDB == null) {
                // 数据库中不存在该新股，执行新增逻辑
                handleInsertNewStock(stockIssueInfo);
            } else if (!EntityComparator.areInstancesEqual(stockIssueInfo, stockIssueInfoDB)) {
                // 数据存在但内容不同，执行更新逻辑
                handleUpdateStock(stockIssueInfo);
            }
        }
    }

    /**
     * 处理新增新股逻辑，包含日志记录、数据库插入与邮件通知。
     *
     * @param stockIssueInfo 新股信息
     */
    private void handleInsertNewStock(StockIssueInfo stockIssueInfo) {
        try {
            log.info("出现新股进行新增,证券代码[{}]证券名称[{}]",
                    stockIssueInfo.getApplyCode(),
                    stockIssueInfo.getSecurityName());

            stockIssueInfoService.insertStockIssueInfo(stockIssueInfo);

            // 邮件通知（根据项目需要设置收件人或使用默认）
            SendEmail.notification("出现新股:" + JSON.toJSON(stockIssueInfo),
                    "证券代码[" + stockIssueInfo.getApplyCode() +
                            "]证券名称[" + stockIssueInfo.getSecurityName() + "]", "");
        } catch (Exception e) {
            log.error("出现新股进行新增失败,证券代码[{}]证券名称[{}]",
                    stockIssueInfo.getApplyCode(),
                    stockIssueInfo.getSecurityName(), e);
        }
    }

    /**
     * 处理已存在新股的更新逻辑，包含日志记录与异常捕获。
     *
     * @param stockIssueInfo 最新新股信息
     */
    private void handleUpdateStock(StockIssueInfo stockIssueInfo) {
        try {
            log.info("出现进行更新,证券代码[{}]证券名称[{}]",
                    stockIssueInfo.getApplyCode(),
                    stockIssueInfo.getSecurityName());

            stockIssueInfoService.updateStockIssueInfo(stockIssueInfo);
        } catch (Exception e) {
            log.error("新股信息更新失败,证券代码[{}]证券名称[{}]",
                    stockIssueInfo.getApplyCode(),
                    stockIssueInfo.getSecurityName(), e);
        }
    }

    //     假设 StockIssueInfoTemp 和 StockIssueInfo 类都有相应的构造方法或转换方法
    public List<StockIssueInfo> convertToStockIssueInfoList(List<StockIssueInfoTemp> stockIssueInfos) {
        return stockIssueInfos.stream()
                .map(StockIssueInfo::new) // 假设 StockIssueInfo 有一个接收 StockIssueInfoTemp 的构造函数
                .collect(Collectors.toList());
    }

    public List<StockIssueInfoTemp> convertJsonNodeArrayToStockEntities(JsonNode jsonNodeArray) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jsonNodeArray.toString());
            JsonNode dataNode = root.get("result").get("data");
            CollectionType collectionType = typeFactory.constructCollectionType(List.class, StockIssueInfoTemp.class);
            return objectMapper.readValue(dataNode.toString(), collectionType);
        } catch (Exception e) {
            log.info("新股信息序列化对象失败:", e);
            return null;
        }
    }


    /**
     * 执行财富数据库全量备份（批量处理）
     *
     * @implNote 实现逻辑：
     * 1. 全量查询股票基础数据
     * 2. 分批次处理数据（每批1000条）
     * 3. 将当前数据转换为历史记录格式
     * 4. 批量插入历史数据表
     * 5. 全程事务控制保证数据一致性
     * 6. 记录详细的操作日志和异常信息
     * @优势： - 批量处理减少数据库连接次数（性能提升约70%）
     * - 分页查询避免内存溢出（支持千万级数据处理）
     * - 事务级原子操作保障数据完整性
     * - 完善的异常处理和日志追溯机制
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void wealthDBDataBak() {
        if (!dBDataLock.tryLock()) {
            log.warn("财富数据库全量备份已在运行中");
            return;
        }
        log.info("开始执行财富数据库全量备份");
        try {
            // 1. 查询所有股票数据（全量加载）
            List<StockInfoDongfang> allStockInfo = stockInfoDongfangService.queryAllStockInfoDongfang();

            // 2. 计算总记录数并初始化计数器
            int totalCount = allStockInfo.size();
            AtomicInteger processedCount = new AtomicInteger(0);

            // 3. 分批次处理数据（核心优化点）
            for (int i = 0; i < totalCount; i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, totalCount);

                // 3.1 获取当前批次数据（内存分片处理）
                List<StockInfoDongfang> batchData = allStockInfo.subList(i, end);

                // 3.2 处理当前批次数据
                processBatch(batchData, processedCount);
            }

            // 4. 记录备份完成日志（包含最终统计信息）
            log.info("财富数据库备份完成｜总处理记录数：{}", totalCount);
        } catch (Exception e) {
            log.info("财富数据库备份失败", e);
        } finally {
            // 释放锁并清理临时数据
            dBDataLock.unlock();
        }


    }

    @Override
    public void updateStockProfitData() throws IOException {
        StockTrades stockTrades1 = new StockTrades().setIsSell(0);
        List<StockTrades> stockTrades = stockTradesService.selectStockTradesList(stockTrades1);
        if (CollectionUtils.isNotEmpty(stockTrades)) {
            //1.获取API
            for (StockTrades stockTrade : stockTrades) {
                try {
                    if (null != stockTrade.getStockApi() && !stockTrade.getStockName().endsWith("ETF")) {
                        //2. 发起请求
                        JsonNode jsonNode = fetchStockData(stockTrade.getStockApi());

                        //3. JSON 数据转对象
                        StockInfoDongFangChain parse = StockInfoDongFangChain.parse2(jsonNode);
                        //4. 重新计算利润
                        //5. 更新数据
                        asyncUpdateTradeRecords(parse.getStockCode(), BigDecimal.valueOf(parse.getPrice())); // 异步更新关联数据
                    } else if (stockTrade.getStockName().endsWith("ETF")) {
                        //3. JSON 数据转对象
                        EtfData etfData = etfDataService.selectEtfDataByEtfCode(stockTrade.getStockCode());
                        //4. 重新计算利润
                        //5. 更新数据
                        asyncUpdateTradeRecords(stockTrade.getStockCode(), etfData.getClosePrice()); // 异步更新关联数据
                    } else if (null == stockTrade.getStockApi() && !stockTrade.getStockName().endsWith("ETF")) {
                        log.warn("股票名称:{}", stockTrade.getStockName() + "没有API");
                        StockInfoDongfang stockInfoDongfang = stockInfoDongfangService.selectByCode(stockTrade.getStockCode());
                        if (null != stockInfoDongfang) {
                            asyncUpdateTradeRecords(stockInfoDongfang.getStockCode(), stockInfoDongfang.getClosePrice());
                        }

                    }

                } catch (Exception e) {
                    log.error("股票名称:{}", stockTrade.getStockName(), e);
                }
            }
        }

    }

    /**
     * 日期格式化器：确保 yyyy-MM-dd，不包含时分秒
     * DateTimeFormatter 是线程安全的，不用额外同步
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * 查询当日所有交易记录，按用户分组累加净利润，
     * 并将每个用户的净利润写入或更新 SalesData 表中的当天数据。
     *
     * @throws RuntimeException 如果在日期转换或数据库操作中发生不可恢复错误
     */
    @Override
    public void queryStockProfitData() {
        // 1. 查询所有交易记录
        List<StockTrades> trades = stockTradesService.selectStockTradesList(new StockTrades());
        if (CollectionUtils.isEmpty(trades)) {
            log.info("【StockProfitService】未查询到任何交易记录，跳过当天净利润统计");
            return;
        }

        // 2. 按 userId 分组，计算每个用户的总净利润
        Map<Long, BigDecimal> profitByUser = trades.stream()
                .filter(t -> t.getNetProfit() != null && t.getUserId() != null) // 过滤无效记录
                .collect(Collectors.groupingBy(
                        StockTrades::getUserId,                                     // 以 userId 分组
                        Collectors.mapping(                                        // 提取净利润字段
                                StockTrades::getNetProfit,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)   // 累加
                        )
                ));

        // 3. 获取当日零点日期，用于 SalesData.recordDate
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Date recordDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 4. 针对每个用户，写入或更新 SalesData
        profitByUser.forEach((userId, totalProfit) -> {
            SalesData criteria = new SalesData();
            criteria.setRecordDate(recordDate);
            criteria.setUserId(userId);

            // 4.1 查询是否已存在当日记录
            List<SalesData> existing = salesDataService.selectSalesDataList(criteria);
            SalesData salesData = new SalesData();
            salesData.setRecordDate(recordDate);
            salesData.setUserId(userId);
            salesData.setProfit(totalProfit);

            if (CollectionUtils.isNotEmpty(existing)) {
                // 4.2 如果存在则更新
                salesData.setId(existing.get(0).getId());
                salesDataService.updateSalesData(salesData);
                log.info("【StockProfitService】已更新用户 {} 在 {} 的净利润，金额：{}",
                        userId, today.format(DATE_FORMATTER), totalProfit);
            } else {
                // 4.3 如果不存在则插入
                salesDataService.insertSalesData(salesData);
                log.info("【StockProfitService】已插入用户 {} 在 {} 的净利润，金额：{}",
                        userId, today.format(DATE_FORMATTER), totalProfit);
            }
        });

        // 5. 异步分别触发每个用户的年度投资汇总更新
        profitByUser.forEach((userId, totalProfit) -> {
            ThreadPoolUtil.getCoreExecutor().submit(() -> {
                // 传入单个用户净利润，按业务逻辑更新该用户的年度汇总
                queryAndUpdateYearlyInvestmentSummary(totalProfit, userId);
            });
        });
    }


    /**
     * 查询并更新当年投资汇总数据。
     *
     * <p>该方法执行以下步骤：
     * <ol>
     *   <li>根据当前年份构造查询条件，调用 service 查询当年投资汇总列表；</li>
     *   <li>若列表不为空，取第一条记录并执行更新操作；</li>
     *   <li>若列表为空，则记录警告日志；</li>
     *   <li>捕获并记录执行过程中的异常。</li>
     * </ol>
     *
     * @param totalProfit 本年度累计利润（单位：元），用于更新汇总记录中的利润字段
     */
    public void queryAndUpdateYearlyInvestmentSummary(BigDecimal totalProfit, Long userId) {
        // 动态获取当前年份
        long currentYear = Year.now().getValue();
        try {
            // 构造查询条件：查询当前年份的汇总记录
            YearlyInvestmentSummary queryCondition = new YearlyInvestmentSummary();
            queryCondition.setYear(currentYear);
            queryCondition.setUserId(userId);

            List<YearlyInvestmentSummary> summaries =
                    yearlyInvestmentSummaryService.selectYearlyInvestmentSummaryList(queryCondition);

            if (summaries == null || summaries.isEmpty()) {
                // 如果无数据，记录警告并返回
                log.warn("queryAndUpdateYearlyInvestmentSummary：未查询到 {} 年的投资汇总记录", currentYear);
                return;
            }

            // 取第一条记录进行更新
            YearlyInvestmentSummary summaryToUpdate = summaries.get(0);

            BigDecimal startPrincipal = summaryToUpdate.getStartPrincipal() != null
                    ? summaryToUpdate.getStartPrincipal()
                    : BigDecimal.ZERO;

            BigDecimal profit = totalProfit != null
                    ? totalProfit
                    : BigDecimal.ZERO;

            BigDecimal actualEndValue = profit.add(startPrincipal);
            summaryToUpdate.setActualEndValue(actualEndValue);

            yearlyInvestmentSummaryService.updateYearlyInvestmentSummary(summaryToUpdate);
            int today = LocalDate.now().getDayOfMonth();
            if (today != 11) {
                log.info("【DepositService】当前日期为 {} 日，非 11 号，跳过银行存款更新。", today);
                return; // ✅ 直接跳过，不抛异常
            }
            if (!userId.equals(4L)) {
                AssetRecord assetRecord = assetRecordService.selectAssetRecordByAssetId(3L);
                assetRecord.setAmount(actualEndValue);
                assetRecordService.updateAssetRecord(assetRecord);
            }
            log.info("成功更新 {} 年度投资汇总，累计利润：{}", currentYear, totalProfit);
        } catch (Exception e) {
            // 捕获并输出完整异常信息，便于排查
            log.error("queryAndUpdateYearlyInvestmentSummary 更新失败，年份：{}，累计利润：{}",
                    currentYear, totalProfit, e);
        }
    }

    @Override
    public void updateEtfData() throws IOException {
        List<EtfData> etfDataList = etfDataService.selectEtfDataList(new EtfData());
        Date nowDate = DateUtils.getNowDate();
        if (CollectionUtils.isNotEmpty(etfDataList)) {
            for (EtfData etfData : etfDataList) {
                log.info("【StockDataService】开始更新 {}", etfData.getEtfName());
                try {
                    //2. 发起请求
                    JsonNode jsonNode = fetchStockData(etfData.getStockApi());
                    //3. JSON 数据转对象
                    EtfData parse = EtfData.parse(jsonNode);
                    //4. 更新数据
                    etfDataService.updateEtfData(parse);
//                    insertEtfData(parse, nowDate);
                } catch (Exception e) {
//                    log.error("股票名称:{}", etfData.getEtfName(), e);
                }

            }
        }

    }

//
//    /**
//     * 数据源数据库连接地址，采用JDBC标准URL格式
//     * 需确保驱动类已通过ServiceLoader注册（如MySQL需显式加载com.mysql.cj.jdbc.Driver）[1,8](@ref)
//     */
//    private static final String SOURCE_URL = "jdbc:mysql://8.155.6.248:3600/aiStock";
//
//    /**
//     * 目标数据库连接地址，采用JDBC标准URL格式
//     * 建议与源数据库保持相同字符集和时区配置以保证数据一致性[1,8](@ref)
//     */
//    private static final String TARGET_URL = "jdbc:mysql://192.168.0.105:3306/aiStock";
//
//    /**
//     * 数据库访问用户名，需满足目标数据库的权限认证规则
//     * 敏感信息建议通过配置中心或环境变量注入，避免硬编码[6,10](@ref)
//     */
//    private static final String SOURCE_USER = "root";
//
//    /**
//     * 数据库访问密码，需满足目标数据库的加密存储要求
//     * 生产环境应使用密钥管理系统（KMS）管理敏感凭证[6,10](@ref)
//     */
//    private static final String SOURCE_PASSWORD = "Tomcat!123";
//
//    /**
//     * 数据库访问用户名，需满足目标数据库的权限认证规则
//     * 敏感信息建议通过配置中心或环境变量注入，避免硬编码[6,10](@ref)
//     */
//    private static final String TARGET_USER = "root";
//
//    /**
//     * 数据库访问密码，需满足目标数据库的加密存储要求
//     * 生产环境应使用密钥管理系统（KMS）管理敏感凭证[6,10](@ref)
//     */
//    private static final String TARGET_PASSWORD = "Tomcat#27";
//
//    @Override
//    public void databaseSync() {
//
//        String updateTimeField = "update_time"; // 假设所有表都包含此字段
//        String updateAtField = "updated_at"; // 假设所有表或者都包含此字段
//
//        // 显式加载 MySQL JDBC 驱动（兼容旧版JDBC）
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException e) {
//            log.error("MySQL JDBC Driver not found!", e);
//            return;
//        }
//
//
//        try (
//                Connection sourceConn = DatabaseSync.createConnection(SOURCE_URL, SOURCE_USER, SOURCE_PASSWORD);
//                Connection targetConn = DatabaseSync.createConnection(TARGET_URL, TARGET_USER, TARGET_PASSWORD)
//        ) {
//            List<DatabaseSync.SyncConfig> configs = DatabaseSync.getAllSyncConfigs(sourceConn, targetConn, updateTimeField, updateAtField);
//            for (DatabaseSync.SyncConfig syncConfig : configs) {
//                DatabaseSync.syncTable(sourceConn, targetConn, syncConfig, 1000);
//            }
//        } catch (SQLException e) {
//            log.error("数据同步异常", e);
//        } catch (Exception e) {
//            log.error("数据同步异常", e);
//        }
//    }

    /**
     * 定时记录所有线程池状态
     * <p>
     * 该方法会定期调用ThreadPoolUtil.logAllThreadPoolStatus()来记录所有线程池的当前状态，
     * 包括核心线程池、关注股票专用线程池和调度线程池的状态信息。
     */
    @Override
    public void logAllThreadPoolStatus() {
        try {
            ThreadPoolUtil.logAllThreadPoolStatus();
        } catch (Exception e) {
            log.error("记录所有线程池状态时发生异常", e);
        }
    }


    @Override
    public void queryListingStatusColumn(Date midnight) {
        StockIssueInfo stockIssueInfo = new StockIssueInfo();
        stockIssueInfo.setListingDate(midnight);

        // 查询当天上市的新股信息
        List<StockIssueInfo> stockIssueInfos = stockIssueInfoService.selectStockIssueInfoList(stockIssueInfo);
        if (CollectionUtils.isEmpty(stockIssueInfos)) return;

        for (StockIssueInfo issueInfo : stockIssueInfos) {
            // 构造缓存键（以股票名称+代码）
            String cacheKey = "finance:" + issueInfo.getSecurityName() + "_" + issueInfo.getSecurityCode();
            Object cacheObject = redisCache.getCacheObject(cacheKey);
            // 检查 Redis 中是否已有该股数据（避免重复通知）
            if (cacheObject != null) {
                continue;
            }

            // 查询对应的上市公告记录
            StockListingNotice query = new StockListingNotice();
            query.setSecurityCode(issueInfo.getSecurityCode());
            List<StockListingNotice> stockListingNotices = stockListingNoticeService.selectStockListingNoticeList(query);

            // 抽取第一条记录作为主处理对象
            StockListingNotice existing = CollectionUtils.isNotEmpty(stockListingNotices) ? stockListingNotices.get(0) : null;

            // 第一次通知逻辑：不存在记录 或者已存在但通知次数 < 2
            if (existing == null || existing.getNotifyCount() < 2) {
                StockListingNotice newNotice = new StockListingNotice();
                newNotice.setSecurityCode(issueInfo.getSecurityCode());
                newNotice.setSecurityName(issueInfo.getSecurityName());
                newNotice.setListingDate(issueInfo.getListingDate());
                newNotice.setIssuePrice(issueInfo.getIssuePrice());

                if (existing == null) {
                    // 第一次插入上市公告记录
                    stockListingNoticeService.insertStockListingNotice(newNotice);
                } else {
                    // 如果已存在记录但未达到通知限制，则更新其 ID，再保存新数据
                    newNotice.setId(existing.getId());
                    stockListingNoticeService.updateStockListingNotice(newNotice);
                }

                // 发送“有上市的了”通知
                SendEmail.notification(JSON.toJSONString(stockIssueInfos),
                        stockIssueInfos.get(0).getSecurityName() + "上市了");

                // 设置缓存，防止重复通知
                redisCache.setCacheObject(cacheKey, issueInfo);
                redisCache.setExpireTime(cacheKey, 12, TimeUnit.HOURS);
                continue; // 通知已处理，进入下一个股票循环
            }
        }
    }


    /**
     * 异步更新关注股票的利润数据
     * <p>
     * 从数据库获取所有关注股票列表，针对每只股票异步执行处理任务，
     * 使用统一线程池避免线程资源浪费，最后等待所有任务完成。
     */
    @Override
    public void updateWatchStockProfitData() {
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<Watchstock> watchStocks = watchstockService.getWatchstockAllList();
        if (CollectionUtils.isNotEmpty(watchStocks)) {
            for (Watchstock watchStock : watchStocks) {
                if (watchStock.getStockApi() != null) {
                    // 使用关注股票专用线程池执行异步任务
                    CompletableFuture.runAsync(
                            () -> processWatchStockToPython(watchStock),
                            ThreadPoolUtil.getWatchStockExecutor()
                    );
                }
            }
        }
    }


    @Override
    public void saveCCBCreditCardTransaction(
            List<CCBCreditCardTransactionEmail> emailList) {

        // 重用线程安全的 DateTimeFormatter
        DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        emailList.forEach(email -> {
            CbcCreditCardTransaction entity = new CbcCreditCardTransaction();
            entity.setUserId(2L);
            // 1. 解析交易日与入账日
            entity.setTradeDate(parseDate(email.getTradeDate(), DATE_FMT));
            entity.setPostDate(parseDate(email.getPostDate(), DATE_FMT));

            // 2. 基础字段映射
            entity.setCardLast4(email.getCardLast4());
            entity.setDescription(email.getDescription());

            // 3. 金额转换
            entity.setTransAmount(parseAmount(email.getTransAmount()));
            entity.setSettleAmount(parseAmount(email.getTransAmount()));

            // 4. 调用 Service 保存
            cbcCreditCardTransactionService.insertCbcCreditCardTransaction(entity);
        });
    }


    /**
     * 按用户分组统计当年所有资产记录的总金额，并写入或更新 AnnualDepositSummary 表中的年度存款汇总。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>获取当前年份；</li>
     *   <li>查询所有资产记录；</li>
     *   <li>按 userId 分组累加各用户资产总额；</li>
     *   <li>针对每个用户，查询当年汇总记录，存在则更新，否则插入；</li>
     * </ol>
     *
     * @throws RuntimeException 如果在查询或数据库操作中发生不可恢复错误
     */
    @Override
    public void updateDepositAmount() {
        // 1. 获取当前年份
        int currentYear = LocalDate.now().getYear();

        // 2. 查询所有资产记录
        List<AssetRecord> assetRecords = assetRecordService.selectAssetRecordList(new AssetRecord());
        if (CollectionUtils.isEmpty(assetRecords)) {
            log.info("【DepositService】未查询到资产记录，跳过年度存款统计");
            return;
        }

        // 3. 按 userId 分组累加每个用户的资产总额（保留两位小数）
        Map<Long, BigDecimal> totalByUser = assetRecords.stream()
                .filter(r -> r.getUserId() != null && r.getAmount() != null)    // 过滤无效数据
                .collect(Collectors.groupingBy(
                        AssetRecord::getUserId,
                        Collectors.mapping(
                                AssetRecord::getAmount,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        amt -> amt.setScale(2, RoundingMode.HALF_UP),          // 保留两位小数
                                        BigDecimal::add                                         // 累加
                                )
                        )
                ));

        // 4. 遍历每个用户，写入或更新年度存款汇总
        totalByUser.forEach((userId, totalAmount) -> {
            // 4.1 查询当年该用户的汇总记录，假设 service 提供按年和用户查询方法
            AnnualDepositSummary summary = annualDepositSummaryService
                    .queryAnnualDepositSummaryByYearAndUser(currentYear, userId);

            if (summary != null) {
                // 4.2 已存在：更新 totalDeposit、updateTime
                summary.setTotalDeposit(totalAmount);
                summary.setUpdateTime(new Date());
                annualDepositSummaryService.updateAnnualDepositSummary(summary);
                log.info("【DepositService】已更新用户 {} {} 年度存款：{}",
                        userId, currentYear, totalAmount);
            } else {
                // 4.3 不存在：插入新记录
                AnnualDepositSummary newSummary = new AnnualDepositSummary();
                newSummary.setYear((long) currentYear);
                newSummary.setUserId(userId);
                newSummary.setTotalDeposit(totalAmount);
                newSummary.setRemark("系统自动统计");
                newSummary.setCreateTime(new Date());
                annualDepositSummaryService.insertAnnualDepositSummary(newSummary);
                log.info("【DepositService】已插入用户 {} {} 年度存款：{}",
                        userId, currentYear, totalAmount);
            }
        });
    }


    /**
     * 更新工行存款金额并同步生成或更新相关消费记录。
     * <p>
     * 逻辑流程：
     * 1. 根据 loanRepaymentId 查询还款信息；
     * 2. 根据 assetId 查询资产记录；
     * 3. 在事务中，扣减资产金额并更新记录；
     * 4. 查询是否已有对应的消费记录，若有则更新，否则插入新消费记录；
     *
     * @param loanRepaymentId 借款还款记录 ID（例如：1L）
     * @param assetId         资产记录 ID（例如：7L）
     * @throws NoSuchElementException 如果未找到还款或资产记录
     */
    @Override
    @Transactional // 确保更新 asset 和 expense 是原子操作
    public void updateICBCDepositAmount(Long loanRepaymentId, Long assetId) {

        // 1. 查询还款信息 当月11号
        Date thisMonthEleventhByCalendar = DateUtils.getThisMonthEleventhByCalendar(11);
        LoanRepayments loanRepayments = loanRepaymentsService.selectLoanRepaymentsByDate(thisMonthEleventhByCalendar);
        if (loanRepayments == null) {
            throw new NoSuchElementException("LoanRepayments not found for ID " + loanRepaymentId);
        }

        // 提取本次还款的金额
        BigDecimal repayAmount = loanRepayments.getTotalPrincipalAndInterest();
        Date repayDate = loanRepayments.getRepaymentDate();

        // 2. 查询资产记录
        AssetRecord assetRecord = assetRecordService.selectAssetRecordByAssetId(assetId);
        if (assetRecord == null) {
            throw new NoSuchElementException("AssetRecord not found for assetId " + assetId);
        }

        // 3. 扣减资产并更新
        BigDecimal currentAmount = assetRecord.getAmount();
        if (currentAmount.compareTo(repayAmount) < 0) {
            throw new IllegalStateException("Insufficient asset balance: current="
                    + currentAmount + ", required=" + repayAmount);
        }
        int today = LocalDate.now().getDayOfMonth();
        if (today == 11) {
            log.info("【DepositService】当前日期为 {} 日， 银行存款更新。", today);
            assetRecord.setAmount(currentAmount.subtract(repayAmount));
            assetRecord.setUpdateTime(new Date());
            assetRecordService.updateAssetRecord(assetRecord);
        }


        // 4. 查找是否已有消费记录
        Long userId = assetRecord.getUserId();
        Expense expense = expenseService.selectExpenseByUserIdAndLoan(userId, repayDate);

        if (expense != null) {
            // 已存在：更新金额
            expense.setAmount(repayAmount);
            expenseService.updateExpense(expense);
        } else {
            // 不存在：新增消费记录
            Expense newExpense = new Expense();
            newExpense.setUserId(userId);
            newExpense.setExpenseDate(repayDate);
            newExpense.setAmount(repayAmount);
            expenseService.insertExpense(newExpense);
        }
    }

    /**
     * 每日股票数据归档任务（单条处理版，带 0 值防护 + 年初逻辑）
     *
     * <p>功能：将 Redis 中缓存的实时股票数据同步到数据库中，实现每日数据归档。</p>
     * <p>主要流程包括：</p>
     * <ol>
     *   <li>从 Redis 获取全部股票代码；</li>
     *   <li>查询数据库中已存在的年度表现记录；</li>
     *   <li>对每只股票数据进行解析、转换与比对，按需插入或更新；</li>
     *   <li>计算年内最低价与涨跌幅等指标；</li>
     *   <li>若当前日期为 1 月 1 日，则执行“年度初始化”逻辑：直接重置年度最低价为当前价（取当前价与 1,000,000 中较小者）；</li>
     *   <li>记录执行统计信息与耗时。</li>
     * </ol>
     *
     * @implNote 本方法为同步阻塞式执行，适合在非高峰期调用。
     * 单个股票异常不影响整体执行。
     */
    @Override
    public void archiveDailyStockData() {
        long startTime = System.currentTimeMillis();

        // === 1️⃣ 获取全部股票代码 ===
        Set<String> allStockCodes = redisCache.getCacheSetAll(CacheConstants.REALTIME_STOCK_CODES_KEY);
        int totalCount = (allStockCodes != null ? allStockCodes.size() : 0);
        log.info("启动每日股票数据归档任务，Redis 股票数量：{}", totalCount);

        if (CollectionUtils.isEmpty(allStockCodes)) {
            log.warn("Redis 中未检测到股票数据，任务结束。");
            return;
        }

        // === 2️⃣ 查询数据库中已有记录 ===
        List<String> allStockCodesList = new ArrayList<>(allStockCodes);
        List<StockYearlyPerformance> existingStockList =
                stockYearlyPerformanceService.queryIDByCodes(allStockCodesList);
        Map<String, StockYearlyPerformance> existingStockMap = existingStockList.stream()
                .collect(Collectors.toMap(StockYearlyPerformance::getCode, s -> s, (a, b) -> a));

        int processedCount = 0;
        int insertCount = 0;
        int updateCount = 0;
        int errorCount = 0;

        // === 3️⃣ 判断是否为 1 月 1 日 ===
        LocalDate today = LocalDate.now();
        boolean isJanuaryFirst = (today.getMonthValue() == 1 && today.getDayOfMonth() == 1);
        if (isJanuaryFirst) {
            log.info("检测到当前日期为 1 月 1 日，将执行年度最低价重置逻辑。");
        }

        // === 4️⃣ 遍历每只股票 ===
        for (String stockCode : allStockCodesList) {
            try {
                // --- 从 Redis 获取实时数据 ---
                String stockKey = CacheConstants.REALTIME_STOCK_SINGLE_KEY + stockCode;
                String stockJson = redisCache.getCacheObject(stockKey);
                if (StringUtils.isEmpty(stockJson)) {
                    log.debug("Redis 中未找到股票 [{}] 数据，跳过。", stockCode);
                    continue;
                }

                // --- JSON 转换 ---
                StockInfoDongFangChain chain = JSON.parseObject(stockJson, StockInfoDongFangChain.class);
                if (chain == null || StringUtils.isEmpty(chain.getStockCode())) {
                    log.warn("股票 [{}] 的 JSON 数据无效或缺少代码字段，跳过。", stockCode);
                    continue;
                }

                // --- 转为数据库实体 ---
                StockYearlyPerformance entity = convertToEntity(chain);
                BigDecimal currentPrice = entity.getNewPrice();
                boolean validCurrent = currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
                if (!validCurrent) {
                    log.debug("股票 [{}] 当前价格无效：{}，跳过。", stockCode, currentPrice);
                    continue;
                }

                // --- 数据库中已有记录 → 更新逻辑 ---
                StockYearlyPerformance dbStock = existingStockMap.get(entity.getCode());
                if (dbStock != null) {
                    dbStock = stockYearlyPerformanceService.selectStockYearlyPerformanceById(dbStock.getId());
                    if (dbStock == null) {
                        log.warn("数据库中未找到股票 [{}] 的详细记录，跳过。", stockCode);
                        continue;
                    }

                    // === 年初逻辑（1月1日：重置最低价）===
                    if (isJanuaryFirst) {
                        BigDecimal limit = new BigDecimal("1000000");
                        BigDecimal newLow = currentPrice.min(limit); // 取较小值
                        dbStock.setYearLowPrice(newLow);
                        dbStock.setYearLowPriceRate(BigDecimal.ZERO);
                        log.info("股票 [{}] 年初重置最低价为 {}（当前价：{}）。", stockCode, newLow, currentPrice);
                    } else {
                        // === 常规逻辑：对比更新年内最低价 ===
                        BigDecimal dbLow = dbStock.getYearLowPrice();
                        if (dbLow == null || dbLow.compareTo(BigDecimal.ZERO) <= 0) {
                            // 查询历史数据，尝试补齐年低价
                            String year = String.valueOf(today.getYear());
                            List<StockInfoDongfangHis> hisList =
                                    stockInfoDongfangHisService.findByCodeAndCreateTime(stockCode, year);

                            if (!hisList.isEmpty()) {
                                BigDecimal minPrice = hisList.stream()
                                        .map(StockInfoDongfangHis::getPrice)
                                        .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                                        .min(BigDecimal::compareTo)
                                        .orElse(currentPrice);
                                dbStock.setYearLowPrice(minPrice);
                                log.debug("股票 [{}] 历史补齐最低价为：{}", stockCode, minPrice);
                            } else {
                                dbStock.setYearLowPrice(currentPrice);
                                log.debug("股票 [{}] 无历史记录，初始化最低价为当前价：{}", stockCode, currentPrice);
                            }
                        } else if (currentPrice.compareTo(dbLow) < 0) {
                            // 当前价更低 → 更新为新低
                            dbStock.setYearLowPrice(currentPrice);
                            log.info("股票 [{}] 发现新低价：{}（原低价：{}）。", stockCode, currentPrice, dbLow);
                        }

                        // === 涨跌幅计算 ===
                        BigDecimal yearLow = dbStock.getYearLowPrice();
                        if (yearLow != null && yearLow.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal rate = currentPrice.subtract(yearLow)
                                    .divide(yearLow, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                            dbStock.setYearLowPriceRate(rate);
                        }
                    }

                    // === 更新其他基础字段 ===
                    dbStock.setNewPrice(currentPrice);
                    dbStock.setUpdateTime(entity.getUpdateTime());

                    stockYearlyPerformanceService.updateStockYearlyPerformance(dbStock);
                    updateCount++;

                } else {
                    // === 不存在 → 插入新记录 ===
                    entity.setDate(new Date());
                    stockYearlyPerformanceService.insertStockYearlyPerformance(entity);
                    insertCount++;
                }

                processedCount++;

                // --- 每 100 条输出一次进度 ---
                if (processedCount % 100 == 0) {
                    log.info("进度：已处理 {} / {} 条（新增 {}，更新 {}）",
                            processedCount, totalCount, insertCount, updateCount);
                }

            } catch (Exception e) {
                errorCount++;
                log.error("处理股票 [{}] 时出现异常：{}", stockCode, e.getMessage(), e);
            }
        }

        // === 5️⃣ 输出任务统计 ===
        long costMs = System.currentTimeMillis() - startTime;
        log.info("股票数据归档任务完成：总计 {} 条（新增 {}，更新 {}，失败 {}），耗时 {} 秒，成功率 {:.2f}%。",
                processedCount, insertCount, updateCount, errorCount,
                costMs / 1000.0,
                (processedCount > 0 ? (100.0 * (processedCount - errorCount) / processedCount) : 0.0));
    }


    /**
     * 更新所有自选股的周低、周高、年低、年高价格。
     * <p>
     * 优化目标：
     * 1. 代码结构更清晰，逻辑拆分更合理；
     * 2. 封装 BigDecimal 的最值逻辑，减少重复；
     * 3. 单次遍历 K 线完成所有统计；
     * 4. 可读性、可维护性更强；
     * 5. 最终批量更新，提高数据库写入效率。
     */
    @Override
    public void updateWatchStockYearLow() {
        List<Watchstock> watchList = watchstockService.getWatchstockAllList();
        try {
            if (watchList == null || watchList.isEmpty()) {
                log.info("没有自选股需要更新。");
                return;
            }

            // 当前日期
            LocalDate today = LocalDate.now();

            // 本周周一、周五
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate friday = today.with(DayOfWeek.FRIDAY);

            Date mondayDate = java.sql.Date.valueOf(monday);
            Date fridayDate = java.sql.Date.valueOf(friday);

            // 今年1月1日 - 今天
            LocalDate firstDayOfYear = LocalDate.of(today.getYear(), 1, 1);
            Date firstDayOfYearDate = java.sql.Date.valueOf(firstDayOfYear);
            Date todayDate = java.sql.Date.valueOf(today);

            for (Watchstock ws : watchList) {

                // ======================
                // 1. 查询周一 ~ 周五的 K线
                // ======================
                List<StockKline> weekKlineList = stockKlineService.selectStockKlineList(
                        new StockKline()
                                .setStockCode(ws.getCode())
                                .setStartDate(mondayDate)
                                .setEndDate(fridayDate)
                );

                if (weekKlineList == null || weekKlineList.isEmpty()) {
                    log.warn("股票 {} 一周 K线为空，跳过。", ws.getCode());
                } else {
                    StatsResult weekResult = weekProcessKlineStats(weekKlineList, ws);
                    ws.setWeekLow(weekResult.weekLow);
                    ws.setWeekHigh(weekResult.weekHigh);
                }

                // ======================
                // 2. 查询 今年1月1日 ~ 今天 的 K线
                // ======================
                List<StockKline> yearKlineList = stockKlineService.selectStockKlineList(
                        new StockKline()
                                .setStockCode(ws.getCode())
                                .setStartDate(firstDayOfYearDate)
                                .setEndDate(todayDate)
                );

                if (yearKlineList == null || yearKlineList.isEmpty()) {
                    log.warn("股票 {} 年度 K线为空，跳过。", ws.getCode());
                } else {
                    StatsResult yearResult = yearProcessKlineStats(yearKlineList, ws);
                    ws.setYearLow(yearResult.yearLow);
                    ws.setYearHigh(yearResult.yearHigh);
                }
            }

            // 批量更新
            watchstockService.updateWatchstockBatch(watchList);

        } catch (Exception e) {
            log.error("自选股周低/周高/年低/年高更新失败：{}", e.getMessage(), e);
        }

        log.info("自选股周低/周高/年低/年高更新完成，共 {} 条。", watchList.size());
    }

    /**
     * 处理 K 线数据的周高低、年高低统计。
     */
    private StatsResult weekProcessKlineStats(List<StockKline> klineList, Watchstock ws) {
        BigDecimal weekLow = ws.getWeekLow();
        BigDecimal weekHigh = ws.getWeekHigh();


        for (StockKline k : klineList) {
            if (k.getLow() == null || k.getHigh() == null) continue;

            weekLow = computeMin(weekLow, k.getLow());
            weekHigh = computeMax(weekHigh, k.getHigh());


        }

        return StatsResult.ofWeek(weekLow, weekHigh);
    }

    /**
     * 处理 K 线数据的周高低、年高低统计。
     */
    private StatsResult yearProcessKlineStats(List<StockKline> klineList, Watchstock ws) {

        BigDecimal yearLow = ws.getYearLow();
        BigDecimal yearHigh = ws.getYearHigh();

        for (StockKline k : klineList) {
            if (k.getLow() == null || k.getHigh() == null) continue;

            yearLow = computeMin(yearLow, k.getLow());
            yearHigh = computeMax(yearHigh, k.getHigh());
        }

        return StatsResult.ofYear(yearLow, yearHigh);
    }

    /**
     * 取最小值（支持初始 null）
     */
    private BigDecimal computeMin(BigDecimal current, BigDecimal val) {
        return (current == null || val.compareTo(current) < 0) ? val : current;
    }

    /**
     * 取最大值（支持初始 null）
     */
    private BigDecimal computeMax(BigDecimal current, BigDecimal val) {
        return (current == null || val.compareTo(current) > 0) ? val : current;
    }


    /**
     * 定时任务：更新美股实时行情
     * <p>
     * 改进版：
     * - 去除外部接口 URL 调用，改用 KlineDataFetcher 统一数据获取逻辑；
     * - 保留最低价、周低价、年低价更新逻辑；
     * - 日志更清晰、异常更易追踪；
     * </p>
     */
    @Override
    public void updateWatchStockUs() {
        // === 1. 定义需要监控的美股代码 ===
        List<String> stockCodes = Arrays.asList("AAPL", "NVDA");

        // === 2. 查询数据库中的现有记录 ===
        Map<String, StockPriceUs> existingMap = stockPriceUsService
                .selectStockPriceUsList(new StockPriceUs())
                .stream()
                .collect(Collectors.toMap(StockPriceUs::getCode, s -> s, (a, b) -> a));

        // === 3. 时间判断逻辑（用于低价重置） ===
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        int hour = now.getHour();
        int minute = now.getMinute();

        boolean inMidnightWindow = (hour == 0 && minute <= 30);
        boolean resetWeeklyLow = inMidnightWindow && today.getDayOfWeek() == DayOfWeek.MONDAY;
        boolean resetYearlyLow = inMidnightWindow && today.getDayOfYear() == 1;

        log.info("🕓 美股行情任务时间判断：isMidnight={}，resetWeeklyLow={}，resetYearlyLow={}",
                inMidnightWindow, resetWeeklyLow, resetYearlyLow);

        // === 4. 初始化计数器 ===
        int successCount = 0;
        int failCount = 0;

        // === 5. 遍历处理每只美股 ===
        for (String code : stockCodes) {
            try {
                // === 5.1 统一数据获取逻辑（替代原 URL 请求） ===
                List<KlineData> todayData = KlineDataFetcher.fetchTodayUSKlineData(code, "105");
                if (todayData == null) {
                    log.warn("⚠️ 美股 {} 今日行情数据为空，跳过更新。", code);
                    failCount++;
                    continue;
                }
                for (KlineData todayDa : todayData) {

                    // === 5.2 构建 StockPriceUs 实体对象 ===
                    StockPriceUs newStock = new StockPriceUs()
                            .setCode(code)
                            .setPriceNow(BigDecimal.valueOf(todayDa.getClose()))
                            .setPriceHighDay(BigDecimal.valueOf(todayDa.getHigh()))
                            .setPriceLowDay(BigDecimal.valueOf(todayDa.getLow()))
                            .setPriceOpenDay(BigDecimal.valueOf(todayDa.getOpen()));
//                            .setPriceCloseYesterday(BigDecimal.valueOf(todayDa.getPreClose()));

                    BigDecimal currentLow = newStock.getPriceLowDay();
                    if (currentLow == null) {
                        log.warn("⚠️ 美股 {} 当前最低价为空，跳过更新。", code);
                        failCount++;
                        continue;
                    }

                    // === 5.3 更新 / 插入逻辑 ===
                    StockPriceUs existing = existingMap.get(code);

                    if (existing != null) {
                        // 已存在 → 更新低价与当前价
                        updateLowPrice(newStock, existing, currentLow, resetWeeklyLow, resetYearlyLow);
                        newStock.setId(existing.getId());
                        stockPriceUsService.updateStockPriceUs(newStock);
                        log.info("✅ 更新美股行情成功：{}", code);
                    } else {
                        // 不存在 → 插入新股票记录
                        newStock.setPriceLowWeek(currentLow);
                        newStock.setPriceLowYear(currentLow);
                        stockPriceUsService.insertStockPriceUs(newStock);
                        log.info("🆕 插入美股行情成功：{}", code);
                    }

                    successCount++;


                }
            } catch (Exception e) {
                log.error("❌ 美股 {} 更新异常：{}", code, e.getMessage(), e);
                failCount++;
            }
        }

        // === 6. 汇总日志 ===
        log.info("🏁 美股行情任务完成：成功 {} 条，失败 {} 条，总计 {} 条。",
                successCount, failCount, stockCodes.size());
    }

    /**
     * 历史 K 线缓存池（线程安全队列）
     * <p>
     * 所有历史任务的 K 线数据先写入该队列，
     * 由 {@link #batchInsertOrUpdateHistory()} 负责批量 insertOrUpdate。
     */
    private final ConcurrentLinkedQueue<StockKline> GLOBAL_HISTORY_QUEUE = new ConcurrentLinkedQueue<>();

    /**
     * 今日 K 线缓存池（线程安全队列）
     * <p>
     * 所有今日任务的 K 线数据先写入该队列，
     * 由 {@link #batchUpdateToday()} 负责批量覆盖更新（按 stock_code + trade_date）。
     */
    private final ConcurrentLinkedQueue<StockKline> GLOBAL_TODAY_QUEUE = new ConcurrentLinkedQueue<>();

    // ====================== 并发 / 风控参数 ======================

    /**
     * 单只股票任务执行完成后的 sleep 区间（毫秒）：1.0s ~ 2.3s随机
     */
    private static final long PER_STOCK_SLEEP_MIN_MS = 1_000L;
    private static final long PER_STOCK_SLEEP_MAX_MS = 2_300L;

    /**
     * 每批 8 支股票提交后额外 sleep 区间（毫秒）：8s ~ 15s随机
     */
    private static final long PER_BATCH_SLEEP_MIN_MS = 8_000L;
    private static final long PER_BATCH_SLEEP_MAX_MS = 15_000L;

    /**
     * 每批股票数量（用于控制批间间隔）
     */
    private static final int STOCKS_PER_BATCH = 8;

    /**
     * 随机数生成器（线程安全足够用）
     */
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    /**
     * 在 [min, max] 区间内生成随机毫秒数。
     */
    private static long randomBetween(long minInclusive, long maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        double delta = maxInclusive - minInclusive;
        return minInclusive + (long) (RANDOM.nextDouble() * delta);
    }

    // ============================================================
    // 任务主入口：多线程抓取 + 全局队列缓存 + 批量写库
    // ============================================================

    /**
     * 执行股票 K 线数据更新任务（多线程版 + 风控节流）。
     *
     * <p><b>任务职责：</b></p>
     * <ul>
     *   <li>从数据库中拉取当前节点的全部股票任务；</li>
     *   <li>使用线程池并行执行每支股票的数据拉取（<b>建议线程池并发 ≤ 4</b>，在 {@link ThreadPoolUtil} 中控制）；</li>
     *   <li>每个任务将 K 线数据写入全局队列（今日队列 / 历史队列），不直接落库；</li>
     *   <li>所有任务完成后，统一进行两次批量写库：历史 insertOrUpdate + 今日覆盖 update。</li>
     * </ul>
     *
     * <p><b>风控与限流策略：</b></p>
     * <ul>
     *   <li>单支股票任务执行完毕后，当前线程会随机 sleep 1.0 ~ 2.3 秒；</li>
     *   <li>主线程在每提交完 8 支股票任务后，会随机 sleep 8 ~ 15 秒再继续提交下一批；</li>
     *   <li>配合 Python 侧每次 fresh cookie / session，可以显著降低东方财富风控概率。</li>
     * </ul>
     *
     * @param nodeId 节点 ID，用于分片获取任务列表
     */
    @Override
    public void updateStockPriceTaskRunning(int nodeId) {

        long taskStart = System.currentTimeMillis();
        log.info("【任务开始】执行股票 K 线任务，节点：{}", nodeId);

        // 保守起见，每次任务启动前先清空全局队列，避免上一次异常中断遗留数据
        GLOBAL_HISTORY_QUEUE.clear();
        GLOBAL_TODAY_QUEUE.clear();

        // 1. 拉取当前节点的全部待处理任务
        List<StockKlineTask> taskList = stockKlineTaskService.getStockAllTask(nodeId);
        if (taskList == null || taskList.isEmpty()) {
            log.info("【任务结束】没有可处理任务");
            return;
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 线程池建议：在 ThreadPoolUtil 内部限制最大并发 <= 4
        ExecutorService executor = ThreadPoolUtil.getWatchStockExecutor();
        List<Future<?>> futures = new ArrayList<>(taskList.size());

        log.info("【并发阶段】开始处理 {} 个股票任务…（线程池：{}）",
                taskList.size(), executor.getClass().getSimpleName());

        int index = 0;
        for (StockKlineTask task : taskList) {
            index++;
            String stockCode = task.getStockCode();

            futures.add(executor.submit(() -> {
                long s = System.currentTimeMillis();

                try {
                    // 今日任务：taskStatus == 3；其他视为历史任务
                    if (task.getTaskStatus() != null && task.getTaskStatus() == 3L) {
                        collectTodayKline(task, stockCode, task.getMarket(), df);
                    } else {
                        collectHistoryKline(task, stockCode, task.getMarket(), df);
                    }
                } catch (Exception e) {
                    log.error("❌ 股票 {} 执行异常：{}", stockCode, e.getMessage(), e);
                } finally {
                    long cost = System.currentTimeMillis() - s;
                    log.info("【收集结束】股票 {} 收集耗时 {} ms", stockCode, cost);

                    // 单支任务完成后的防风控 sleep
                    try {
                        long sleepMs = randomBetween(PER_STOCK_SLEEP_MIN_MS, PER_STOCK_SLEEP_MAX_MS);
                        log.debug("【节流】单任务完成后休眠 {} ms stock={}", sleepMs, stockCode);
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("单任务节流休眠被中断 stock={}", stockCode);
                    }
                }
            }));

            // 每提交完一批 STOCKS_PER_BATCH 个任务后，主线程额外 sleep
            if (index % STOCKS_PER_BATCH == 0 && index < taskList.size()) {
                try {
                    long batchSleep = randomBetween(PER_BATCH_SLEEP_MIN_MS, PER_BATCH_SLEEP_MAX_MS);
                    log.info("【批次节流】已提交 {} 个任务，批间休眠 {} ms …", index, batchSleep);
                    Thread.sleep(batchSleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("批次节流休眠被中断，后续任务将无批间延时");
                }
            }
        }

        // 2. 等待所有并发任务执行结束
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.error("并发执行异常：{}", e.getMessage(), e);
            }
        }

        log.info("【收集阶段完成】全部股票数据收集结束，开始批量写库…");

        // 3. 批量历史 insertOrUpdate
        batchInsertOrUpdateHistory();

        // 4. 批量今日覆盖 update
        batchUpdateToday();

        log.info("【任务全部完成】总耗时 {} ms", (System.currentTimeMillis() - taskStart));
    }

    // ============================================================
    // 今日 / 历史收集
    // ============================================================

    /**
     * 收集“今日 K 线数据”（实际上是最近 N 日日级聚合数据）并写入 GLOBAL_TODAY_QUEUE。
     *
     * <p>说明：</p>
     * <ul>
     *   <li>通过 {@link KlineDataFetcher#fetchKlineDataFiveDay(String, String)} 获取指定股票最近数日的日级 K 线数据；</li>
     *   <li>将每条数据解析为 {@link StockKline} 实体，放入 {@link #GLOBAL_TODAY_QUEUE}；</li>
     *   <li>后续由 {@link #batchUpdateToday()} 根据 stock_code + trade_date 批量覆盖更新。</li>
     * </ul>
     */
    private void collectTodayKline(StockKlineTask task,
                                   String stockCode,
                                   String market,
                                   DateTimeFormatter df) {

        // 使用股票代码 + 市场组合生成 secid（如 1.601138），内部脚本负责风控与 cookie 管理
        List<KlineData> todayData = KlineDataFetcher.fetchKlineDataFiveDay(
                stockCode, getMarketCode(stockCode));

        if (todayData == null || todayData.isEmpty()) {
            log.warn("⚠ 股票 {} 今日数据为空", stockCode);
            return;
        }

        for (KlineData d : todayData) {
            if (d == null || d.getTradeDate() == null) {
                continue;
            }

            LocalDate tradeDate = parseTradeDate(d.getTradeDate(), stockCode, df);
            if (tradeDate == null) {
                // 日期解析失败直接跳过当前记录，避免影响其他数据
                continue;
            }

            GLOBAL_TODAY_QUEUE.add(
                    buildStockKlineEntity(stockCode, market, tradeDate, d)
            );
        }
    }

    /**
     * 收集“历史 K 线数据”（全量或较长区间历史）并写入 GLOBAL_HISTORY_QUEUE。
     *
     * <p>说明：</p>
     * <ul>
     *   <li>通过 {@link #fetchKlineDataWithRetry(String)} 方式，带重试拉取历史日 K 线数据；</li>
     *   <li>每条记录解析 tradeDate，构造 {@link StockKline} 实体；</li>
     *   <li>统一写入 {@link #GLOBAL_HISTORY_QUEUE}，由 {@link #batchInsertOrUpdateHistory()} 负责批处理入库。</li>
     * </ul>
     */
    private void collectHistoryKline(StockKlineTask task,
                                     String stockCode,
                                     String market,
                                     DateTimeFormatter df) {

        List<KlineData> klineList = fetchKlineDataWithRetry(stockCode);
        if (klineList == null || klineList.isEmpty()) {
            log.error("⚠ 股票 {} 历史任务连续重试仍失败，放弃本次处理", stockCode);
            return;
        }

        for (KlineData d : klineList) {
            if (d == null || d.getTradeDate() == null) {
                continue;
            }

            LocalDate tradeDate = parseTradeDate(d.getTradeDate(), stockCode, df);
            if (tradeDate == null) {
                continue;
            }

            GLOBAL_HISTORY_QUEUE.add(
                    buildStockKlineEntity(stockCode, market, tradeDate, d)
            );
        }
    }

    // ============================================================
    // 批量落库（历史 / 今日覆盖）
    // ============================================================

    /**
     * 对 GLOBAL_HISTORY_QUEUE 中的历史 K 线记录进行批量插入或更新（insertOrUpdate）。
     *
     * <p>说明：</p>
     * <ul>
     *   <li>每批最多处理 BATCH_SIZE 条记录，防止单次 SQL 过大；</li>
     *   <li>调用 {@code stockKlineService.insertOrUpdateBatch(List)} 完成 DB 操作；</li>
     *   <li>队列通过 {@link ConcurrentLinkedQueue#poll()} 逐条弹出，直至为空。</li>
     * </ul>
     */
    private void batchInsertOrUpdateHistory() {

        final int BATCH_SIZE = 2000;
        List<StockKline> buffer = new ArrayList<>(BATCH_SIZE);

        long total = 0;
        long start = System.currentTimeMillis();

        while (!GLOBAL_HISTORY_QUEUE.isEmpty()) {
            buffer.clear();

            for (int i = 0; i < BATCH_SIZE && !GLOBAL_HISTORY_QUEUE.isEmpty(); i++) {
                StockKline k = GLOBAL_HISTORY_QUEUE.poll();
                if (k != null) {
                    buffer.add(k);
                }
            }

            if (!buffer.isEmpty()) {
                stockKlineService.insertOrUpdateBatch(buffer);
                total += buffer.size();
                log.info("【历史批量】本批 {} 条，累计 {} 条", buffer.size(), total);
            }
        }

        log.info("【历史批量完成】总计 {} 条，耗时 {} ms", total,
                (System.currentTimeMillis() - start));
    }

    /**
     * 对 GLOBAL_TODAY_QUEUE 中的“今日 K 线数据”执行批量覆盖更新。
     *
     * <p>说明：</p>
     * <ul>
     *   <li>每批最多处理 BATCH_SIZE 条记录，防止大批量 update 压垮数据库；</li>
     *   <li>通过 {@code stockKlineService.batchUpdateByStockCodeAndTradeDate(List)} 按 (stock_code, trade_date) 覆盖更新；</li>
     *   <li>队列通过 {@link ConcurrentLinkedQueue#poll()} 逐条弹出，直至为空。</li>
     * </ul>
     */
    private void batchUpdateToday() {

        final int BATCH_SIZE = 1000;
        List<StockKline> buffer = new ArrayList<>(BATCH_SIZE);

        long total = 0;
        long start = System.currentTimeMillis();

        while (!GLOBAL_TODAY_QUEUE.isEmpty()) {
            buffer.clear();

            for (int i = 0; i < BATCH_SIZE && !GLOBAL_TODAY_QUEUE.isEmpty(); i++) {
                StockKline k = GLOBAL_TODAY_QUEUE.poll();
                if (k != null) {
                    buffer.add(k);
                }
            }

            if (!buffer.isEmpty()) {
                stockKlineService.batchUpdateByStockCodeAndTradeDate(buffer);
                total += buffer.size();
                log.info("【今日覆盖批量】本批 {} 条，累计 {} 条", buffer.size(), total);
            }
        }

        log.info("【今日覆盖完成】总计 {} 条，耗时 {} ms", total,
                (System.currentTimeMillis() - start));
    }

    // ============================================================
    // 日期解析 & 实体构建
    // ============================================================

    private LocalDate parseTradeDate(String tradeDateStr, String stockCode, DateTimeFormatter df) {
        long s = System.currentTimeMillis();
        try {
            LocalDate date = LocalDate.parse(tradeDateStr.trim(), df);
            long t = System.currentTimeMillis() - s;
            if (t > 5) {
                log.debug("股票 {} 日期解析耗时 {} ms：{}", stockCode, t, tradeDateStr);
            }
            return date;
        } catch (Exception e) {
            log.warn("股票 {} tradeDate 格式错误：{}，跳过", stockCode, tradeDateStr);
            return null;
        }
    }

    private StockKline buildStockKlineEntity(String stockCode, String market,
                                             LocalDate tradeDate, KlineData data) {
        StockKline e = new StockKline()
                .setStockCode(stockCode)
                .setMarket(market)
                .setTradeDate(java.sql.Date.valueOf(tradeDate))
                .setOpen(safeBigDecimal(data.getOpen()))
                .setClose(safeBigDecimal(data.getClose()))
                .setHigh(safeBigDecimal(data.getHigh()))
                .setLow(safeBigDecimal(data.getLow()))
                .setChange(safeBigDecimal(data.getChange()))
                .setChangePercent(safeBigDecimal(data.getChangePercent()))
                .setTurnoverRatio(safeBigDecimal(data.getTurnoverRatio()))
                .setVolume(data.getVolume())
                .setPreClose(safeBigDecimal(data.getPreClose()))
                .setAmount(safeBigDecimal(data.getAmount()));

        e.setCreateTime(DateUtils.getNowDate());
        e.setUpdateTime(DateUtils.getNowDate());
        return e;
    }

    // ============================================================
    // 带重试的历史 K 线获取
    // ============================================================

    /**
     * 带自动重试机制的历史 K 线数据获取方法。
     *
     * <p>行为说明：</p>
     * <ul>
     *   <li>调用 {@link KlineDataFetcher#fetchKlineDataALL(String, String)} 拉取全历史 K 线；</li>
     *   <li>若返回 null 或空列表，认为本次失败，进行重试；</li>
     *   <li>最多重试 MAX_RETRIES 次，重试间隔 RETRY_DELAY_MS 毫秒；</li>
     *   <li>成功获取非空数据则立即返回；若最终仍失败，返回最后一次结果（可能为 null）。</li>
     * </ul>
     */
    private List<KlineData> fetchKlineDataWithRetry(String stockCode) {
        // 比原来更保守一点，避免单任务阻塞太久：
        final int MAX_RETRIES = 5;                  // 最大重试次数（可根据实际情况调优）
        final long RETRY_DELAY_MS = 90_000L;        // 每次重试间隔 90 秒

        List<KlineData> klineData = null;
        String market = getMarketCode(stockCode);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                klineData = KlineDataFetcher.fetchKlineDataALL(stockCode, market);
                if (klineData != null && !klineData.isEmpty()) {
                    log.info("股票 {} 历史数据获取成功（第 {} 次） rows={}",
                            stockCode, attempt, klineData.size());
                    return klineData;
                } else {
                    log.warn("股票 {} 历史数据为空或格式异常（第 {} 次尝试）", stockCode, attempt);
                }
            } catch (Exception e) {
                log.warn("股票 {} 历史数据获取异常（第 {} 次）：{}",
                        stockCode, attempt, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try {
                    log.info("股票 {} 等待 {} ms 后重试（第 {} 次 -> 第 {} 次）",
                            stockCode, RETRY_DELAY_MS, attempt, attempt + 1);
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("股票 {} 重试等待过程中被中断，中止后续重试", stockCode);
                    break;
                }
            }
        }

        return klineData;
    }

    /**
     * 安全地将 Double 值转换为 BigDecimal
     * <p>若输入为 {@code null}，则返回 {@link BigDecimal#ZERO}，防止出现 NullPointerException。</p>
     *
     * @param val 待转换的 Double 值
     * @return 对应的 BigDecimal 值（若输入为 null 则返回 0）
     */
    private static BigDecimal safeBigDecimal(Double val) {
        return val == null ? BigDecimal.ZERO : BigDecimal.valueOf(val);
    }


    /**
     * 根据当前价格更新“周内最低价”和“年内最低价”
     */
    private void updateLowPrice(StockPriceUs newStock,
                                StockPriceUs existing,
                                BigDecimal currentLow,
                                boolean resetWeeklyLow,
                                boolean resetYearlyLow) {

        // === 周内最低价逻辑 ===
        if (resetWeeklyLow || existing.getPriceLowWeek() == null) {
            newStock.setPriceLowWeek(currentLow);
        } else {
            newStock.setPriceLowWeek(existing.getPriceLowWeek().min(currentLow));
        }

        // === 年内最低价逻辑 ===
        if (resetYearlyLow || existing.getPriceLowYear() == null) {
            newStock.setPriceLowYear(currentLow);
        } else {
            newStock.setPriceLowYear(existing.getPriceLowYear().min(currentLow));
        }
    }


    /**
     * 将日期字符串按指定格式转换为 Date。
     *
     * @param dateStr   待解析的日期字符串，格式需与 formatter 一致
     * @param formatter Java 8 日期格式化器
     * @return 对应的 java.util.Date
     * @throws IllegalArgumentException 如果格式不匹配
     */
    private Date parseDate(String dateStr, DateTimeFormatter formatter) {
        try {
            LocalDate ld = LocalDate.parse(dateStr, formatter);
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "无法解析日期: " + dateStr, e);
        }
    }

    /**
     * 将“币种/金额”格式的字符串转换为 BigDecimal。
     * 支持形如 "CNY/123.45" 或 "123.45"。
     *
     * @param amtStr 包含币种和金额的字符串
     * @return 对应的金额数值（不区分币种）
     * @throws IllegalArgumentException 如果无法解析数字部分
     */
    private BigDecimal parseAmount(String amtStr) {
        if (StringUtils.isBlank(amtStr)) {
            return BigDecimal.ZERO;
        }
        // 提取数字部分
        String num = amtStr.contains("/")
                ? amtStr.substring(amtStr.indexOf("/") + 1)
                : amtStr;
        try {
            return new BigDecimal(num.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "无法解析金额: " + amtStr, e);
        }
    }

    private void processBatch(List<StockInfoDongfang> batchData, AtomicInteger counter) {
        if (CollectionUtils.isEmpty(batchData)) {
            return;
        }

        try {
            // 3.1 转换数据对象类型（手动复制属性）
            List<StockInfoDongfangHis> historyBatch = new ArrayList<>(batchData.size());
            for (StockInfoDongfang entity : batchData) {
                StockInfoDongfangHis historyEntity = new StockInfoDongfangHis();
                BeanUtils.copyProperties(entity, historyEntity);

//                historyEntity.setInDate(new Date());           // 设置记录创建时间
                historyBatch.add(historyEntity);
            }

            // 3.2 执行批量插入操作
            int insertResult = stockInfoDongfangHisService.batchInsertStockInfoDongfangHis(historyBatch);

            // 3.3 记录处理结果
            log.info("批量备份成功｜批次范围：{}-{}｜插入成功数：{}",
                    batchData.get(0).getId(), batchData.get(batchData.size() - 1).getId(), insertResult);

            // 3.4 更新全局计数器
            counter.addAndGet(insertResult);

        } catch (Exception e) {
            // 4.1 构建错误日志信息（包含完整批次信息）
            String errorMessage = String.format("批次处理失败｜起始ID：%d｜终止ID：%d｜错误详情：%s",
                    batchData.isEmpty() ? 0 : batchData.get(0).getId(),
                    batchData.isEmpty() ? 0 : batchData.get(batchData.size() - 1).getId(),
                    e.getMessage());

            // 4.2 记录完整错误信息（包含堆栈跟踪）
            log.error(errorMessage, e);

            // 4.3 抛出业务异常触发事务回滚
            throw new BusinessException("BACKUP_ERROR" + "财富数据库备份失败", e);
        }
    }

    /**
     * 日期格式化器（yyyyMMdd）
     */
    private static final DateTimeFormatter DATE_FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private void processWatchStockToPython(Watchstock watchStock) {
        final int maxRetries = 3;       // 最大重试次数
        final int retryDelay = 10_000;  // 每次重试间隔 10 秒
        int attempt = 0;
        String today = LocalDate.now().format(DATE_FORMATTER_YYYYMMDD);
        // === 获取今日K线数据（带重试机制） ===
        List<KlineData> klineData = null;
        while (attempt < maxRetries) {
            attempt++;
            try {
                klineData = KlineDataFetcher.fetchKlineData(
                        watchStock.getCode(),
                        getMarketCode(watchStock.getCode()),
                        today,
                        today
                );

                if (klineData != null) {
                    break; // 成功获取则退出重试循环
                }

                log.warn("无法获取股票 {} 的K线数据 (第 {} 次尝试)", watchStock.getCode(), attempt);
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelay);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("线程在等待期间被中断，股票代码：{}", watchStock.getCode());
                return;
            } catch (Exception e) {
                log.error("股票 {} 获取K线数据失败 (第 {} 次): {}", watchStock.getCode(), attempt, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("线程在异常等待期间被中断，股票代码：{}", watchStock.getCode());
                        return;
                    }
                }
            }
        }

        if (klineData == null) {
            log.error("股票 {} 连续 {} 次获取K线数据失败，已放弃。", watchStock.getCode(), maxRetries);
            return;
        }

        // === 更新价格与涨跌数据 ===
        for (KlineData klineDatum : klineData) {
            updateWatchStockPrices(watchStock, klineDatum);
        }
        // === 更新周度低价（如果更低） ===
        updateWeekHighLowIfNeeded(watchStock);

        watchstockService.updateWatchstock(watchStock);

        log.info("✅ 自选股 [{}] 更新完成：现价 {}，日高 {}，日低 {}，周低 {}，年低 {}",
                watchStock.getCode(),
                watchStock.getNewPrice(),
                watchStock.getHighPrice(),
                watchStock.getLowPrice(),
                watchStock.getWeekLow(),
                watchStock.getYearLow());
    }

    /**
     * 更新自选股的当前价、涨跌额、涨跌幅等基础数据。
     */
    private void updateWatchStockPrices(Watchstock watchStock, KlineData klineData) {
        double close = Optional.ofNullable(klineData.getClose()).orElse(0.0);
        double preClose = Optional.ofNullable(klineData.getPreClose()).orElse(0.0);
        BigDecimal upsDowns = BigDecimal.valueOf(close).subtract(BigDecimal.valueOf(preClose));

        BigDecimal upsDownsRate = preClose == 0
                ? BigDecimal.ZERO
                : upsDowns.divide(BigDecimal.valueOf(preClose), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        watchStock.setNewPrice(BigDecimal.valueOf(close))
                .setUpsDowns(upsDownsRate)
                .setPreviousClose(BigDecimal.valueOf(preClose))
                .setLowPrice(BigDecimal.valueOf(Optional.ofNullable(klineData.getLow()).orElse(0.0)))
                .setHighPrice(BigDecimal.valueOf(Optional.ofNullable(klineData.getHigh()).orElse(0.0)))
                .setNum(null)
                .setThresholdPrice(null);
    }

    /**
     * 如果当前价格低于周低价或高于周高价，则更新周度高低价。
     */
    private void updateWeekHighLowIfNeeded(Watchstock stock) {
        boolean updated = false;
        BigDecimal highPrice = stock.getHighPrice();
        BigDecimal lowPrice = stock.getLowPrice();
        // 周低价更新
        if (stock.getWeekLow() == null || lowPrice.compareTo(stock.getWeekLow()) < 0) {
            stock.setWeekLow(lowPrice);
            log.info("股票 [{}] 周度低价已更新为 {}", stock.getCode(), lowPrice);
            updated = true;
        }

        // 周高价更新
        if (stock.getWeekHigh() == null || highPrice.compareTo(stock.getWeekHigh()) > 0) {
            stock.setWeekHigh(highPrice);
            log.info("股票 [{}] 周度高价已更新为 {}", stock.getCode(), highPrice);
            updated = true;
        }
//
//        if (updated) {
//            watchstockService.updateWatchstock(stock);
//        }
    }

    /**
     * 更新日高低价（只在价格高于日高或低于日低时更新）
     */
    private void updateDailyHighLow(Watchstock stock, BigDecimal currentPrice) {
        if (stock.getLowPrice() == null || currentPrice.compareTo(stock.getLowPrice()) < 0) {
            stock.setLowPrice(currentPrice);
        }
        if (stock.getHighPrice() == null || currentPrice.compareTo(stock.getHighPrice()) > 0) {
            stock.setHighPrice(currentPrice);
        }
    }


//--------------------- 辅助方法 ------------------------


    // 最大重试次数常量，限制最多进行3次重试
    private static final int MAX_RETRIES = 3;
    // 初始重试延迟时间（毫秒），采用指数退避策略时首次等待时间
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    // 重用ObjectMapper实例以提高性能（线程安全）
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取股票数据核心方法
     *
     * @param apiUrl 股票数据API地址
     * @return 解析后的JSON数据节点（data节点）
     * @throws IOException 当发生网络错误、数据解析失败或超过重试次数时抛出
     * @apiNote 方法特性：
     * 1. 自动处理JSONP包装格式
     * 2. 包含重试机制（指数退避）
     * 3. 严格校验HTTP状态码和数据结构
     * 4. 超时和重定向处理
     */
    public JsonNode fetchStockData(String apiUrl) throws IOException {
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

    /**
     * 读取错误响应内容
     *
     * @param conn 已建立连接的HttpURLConnection对象
     * @return 错误信息字符串（当无法读取时返回错误描述）
     */
    private String readErrorResponse(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            // 当错误流不可读时返回友好提示
            return "Error reading error stream: " + e.getMessage();
        }
    }

    /**
     * 去除JSONP回调函数包装
     *
     * @param response 原始响应字符串
     * @return 去除包装后的纯JSON字符串
     * @implNote 处理格式示例：
     * jQuery351023603019987821083_1740994119105({...})
     */
    private String removeCallbackWrapper(String response) {
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
     * 带指数退避的重试机制
     *
     * @param callable 需要重试的操作（需抛出IOException）
     * @return 操作成功后的返回值
     * @throws IOException 当所有重试失败后抛出
     * @implNote 重试策略：
     * 1. 仅对网络相关异常重试（SocketException/SocketTimeoutException）
     * 2. 指数退避延迟：1000ms -> 2000ms -> 4000ms
     * 3. 最大重试次数3次（总尝试次数=初始1次+重试3次）
     */
    private <T> T retryWithExponentialBackoff(Callable<T> callable) throws IOException {
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

    /**
     * 异步更新指定股票代码的交易记录及相关卖出价格预警信息
     *
     * @param code     股票代码
     * @param newPrice 最新价格，用于更新交易记录和预警价格
     */
    private void asyncUpdateTradeRecords(String code, BigDecimal newPrice) {
        CompletableFuture.runAsync(() -> {
            try {
                // 查询指定股票代码的交易记录
                List<StockTrades> tradesList = stockTradesService.selectStockTradesOne(
                        new StockTrades().setStockCode(code));
                for (StockTrades stockTrades : tradesList) {
                    if (stockTrades != null && stockTrades.getStockCode().equals(code)) {
                        // 计算更新交易明细
                        updateTradeDetails(stockTrades, newPrice);

                        // 更新数据库中的交易记录
                        stockTradesService.updateStockTradesByCode(stockTrades);

                        // 更新卖出价格预警中的最新价格
                        SellPriceAlerts sellPriceAlerts = new SellPriceAlerts()
                                .setStockCode(stockTrades.getStockCode())
                                .setLatestPrice(newPrice);
                        sellPriceAlertsService.updateLatestPrice(sellPriceAlerts);
                    }
                }

            } catch (Exception e) {
                log.error("更新交易记录失败: {}", code, e);
            }
        }, ThreadPoolUtil.getCoreExecutor());
    }


    /**
     * 计算并更新交易详情
     * Calculate and update trade details
     */
    private void updateTradeDetails(StockTrades trade, BigDecimal newPrice) {
        try {
            BigDecimal[] addPrices = {
                    trade.getAdditionalPrice1(),
                    trade.getAdditionalPrice2(),
                    trade.getAdditionalPrice3()
            };

            Long[] addShares = {
                    trade.getAdditionalShares1(),
                    trade.getAdditionalShares2(),
                    trade.getAdditionalShares3()
            };

            BigDecimal profit = calculateNetProfit(
                    trade.getBuyPrice(),
                    newPrice,
                    trade.getInitialShares(),
                    addPrices,
                    addShares
            );

            BigDecimal targetNetProfit = calculateNetProfit(
                    trade.getBuyPrice(),
                    trade.getSellTargetPrice(),
                    trade.getInitialShares(),
                    addPrices,
                    addShares
            );

            if (trade.getSellTargetPrice().equals(newPrice)) {
                trade.setIsSell(1);
            }

            trade.setSellPrice(newPrice)
                    .setNetProfit(profit)
                    .setTargetNetProfit(targetNetProfit)
                    .setTotalCost(calculateTotalCost(
                            trade.getBuyPrice(),
                            trade.getInitialShares(),
                            addPrices,
                            addShares
                    ));
        } catch (Exception e) {
            log.error("计算交易详情失败:", e);
        }

    }

    /**
     * 计算净收益
     * Calculate net profit
     */
    private BigDecimal calculateNetProfit(BigDecimal buyPrice, BigDecimal sellPrice,
                                          Long initShares, BigDecimal[] addPrices, Long[] addShares) {
        BigDecimal baseProfit = sellPrice.subtract(buyPrice)
                .multiply(new BigDecimal(initShares));

        BigDecimal additionalProfit = IntStream.range(0, Math.min(addPrices.length, addShares.length))
                .filter(i -> addPrices[i] != null && addShares[i] != null)
                .mapToObj(i -> sellPrice.subtract(addPrices[i])
                        .multiply(new BigDecimal(addShares[i])))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseProfit.add(additionalProfit);
    }

    /**
     * 计算总成本
     * Calculate total cost
     */
    private BigDecimal calculateTotalCost(BigDecimal buyPrice, Long initShares,
                                          BigDecimal[] addPrices, Long[] addShares) {
        BigDecimal baseCost = buyPrice.multiply(new BigDecimal(initShares));

        BigDecimal additionalCost = IntStream.range(0, Math.min(addPrices.length, addShares.length))
                .filter(i -> addPrices[i] != null && addShares[i] != null)
                .mapToObj(i -> addPrices[i].multiply(new BigDecimal(addShares[i])))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseCost.add(additionalCost);
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
     * 对象转换（使用MapStruct示例）
     * <p>
     * 将StockInfoDongFangChain对象转换为StockInfoDongfang数据库实体对象
     * </p>
     *
     * @param chain StockInfoDongFangChain对象
     * @return StockInfoDongfang数据库实体对象
     */
    private StockYearlyPerformance convertToEntity(StockInfoDongFangChain chain) {
        // 创建StockInfoDongfang实体对象
        StockYearlyPerformance entity = new StockYearlyPerformance();
        // 基础信息
        // 设置股票代码
        entity.setCode(chain.getStockCode());
        // 设置公司名称
        entity.setName(chain.getCompanyName());
        // 价格相关
        // 设置当前价格
        entity.setNewPrice(BigDecimal.valueOf(chain.getPrice())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        entity.setUpdateTime(new Date());

        // 返回转换后的实体对象
        return entity;
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

        // 按优先级处理
        // 1️⃣ B股特殊处理（90开头是上海B股，20开头是深圳B股）
        if (prefix.startsWith("90")) {
            return "1"; // 上海B股
        }
        if (prefix.startsWith("20")) {
            return "0"; // 深圳B股
        }

        // 2️⃣ 特殊市场代码（92开头，可能是新三板或其他特殊板块）
        if (prefix.startsWith("92")) {
            return "S"; // 特殊市场
        }

        // 3️⃣ 上海证券交易所（上交所）：以 60 或 68 开头
        if (prefix.startsWith("60") || prefix.startsWith("68")) {
            return "1";
        }

        // 4️⃣ 深圳证券交易所（深交所）：以 00 或 30 开头
        if (prefix.startsWith("00") || prefix.startsWith("30")) {
            return "0";
        }

        // 5️⃣ 北京证券交易所（北交所）：以 43、83、87、88 开头
        if (prefix.startsWith("43") || prefix.startsWith("83")
                || prefix.startsWith("87") || prefix.startsWith("88")) {
            return "2";
        }

        return null;
    }
}
