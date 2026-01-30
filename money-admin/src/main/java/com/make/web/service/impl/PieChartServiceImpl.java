package com.make.web.service.impl;

import com.alibaba.fastjson2.JSON;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.make.common.constant.CacheConstants;
import com.make.common.core.redis.RedisCache;
import com.make.common.exception.business.BusinessException;
import com.make.finance.domain.BankCardTransactions;
import com.make.finance.domain.Expense;
import com.make.finance.domain.Income;
import com.make.finance.domain.LoanRepayments;
import com.make.finance.domain.ServerInfo;
import com.make.finance.domain.TransactionCategories;
import com.make.finance.domain.TransactionRecords;
import com.make.finance.domain.UserAccounts;
import com.make.finance.domain.vo.LoanTotalRepaymentPieChart;
import com.make.finance.domain.vo.LoanTotalWithInterestRepaymentPieChart;
import com.make.finance.domain.vo.MonthlyExpenditureBarChart;
import com.make.finance.domain.vo.TotalAmount;
import com.make.finance.mapper.TransactionRecordsMapper;
import com.make.finance.service.IBankCardTransactionsService;
import com.make.finance.service.IExpenseService;
import com.make.finance.service.IIncomeService;
import com.make.finance.service.ILoanRepaymentsService;
import com.make.finance.service.IServerInfoService;
import com.make.finance.service.IUserAccountsService;
import com.make.stock.domain.SalesData;
import com.make.stock.service.ISalesDataService;
import com.make.web.service.IPieChartService;
import com.make.finance.service.ITransactionCategoriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PieChartServiceImpl implements IPieChartService {

    private static final Logger logger = LoggerFactory.getLogger(PieChartServiceImpl.class);


    @Resource
    private TransactionRecordsMapper transactionRecordsMapper;

    @Resource
    private IBankCardTransactionsService iBankCardTransactionsService;

    @Resource
    private ILoanRepaymentsService iLoanRepaymentsService;

    @Resource
    private ITransactionCategoriesService transactionCategoriesService;

    @Resource
    private IIncomeService incomeService;

    @Resource
    private IExpenseService expenseService;

    @Resource
    private IUserAccountsService userAccountsService;

    @Resource
    private ISalesDataService salesDataService;

    @Resource
    private IServerInfoService serverInfoService;

    @Resource
    private RedisCache redisCache;

    private static final Integer TIME_OUT_HOURS = 1;

    /**
     * 获取交易类型饼图数据。
     *
     * @return 包含交易类型饼图数据的列表
     */
    @Override
    public List<Map<String, Object>> getTransactionTypePieChartData(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "TransactionTypePieChartData" + "_" + id;
        List<Map<String, Object>> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }
        List<Map<String, Object>> dbData = transactionRecordsMapper.selectTransactionTypePieChartData(id);
        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }

    /**
     * 获取总金额饼图数据。
     *
     * @return 包含总金额饼图数据的列表
     */
    @Override
    public List<Map<String, Object>> getTotalAmountPieChartData(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "TotalAmountPieChartData" + "_" + id;
        List<Map<String, Object>> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }
        List<Map<String, Object>> dbData = transactionRecordsMapper.selectTotalAmountPieChartData(id);

        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }

    /**
     * 获取总金额数据。
     * 该方法获取过去一年内每个月的总支出金额数据，并将其封装为TotalAmount对象存储在列表中返回。
     *
     * @return 包含总金额数据的列表
     */
    @Override
    public List<TotalAmount> getTotalAmountChartData(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "TotalAmountChartData" + "_" + id;
        List<TotalAmount> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }
        List<TotalAmount> dbData = new ArrayList<>();
        TransactionRecords transactionRecord = new TransactionRecords();
        transactionRecord.setUserId(id);
        List<TransactionRecords> transactionRecords = transactionRecordsMapper.selectTransactionRecordsList(transactionRecord);
        // 获取当前日期的前一年的日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();
        // 日期格式化工具，用于将 Date 转换为 "yyyy-MM" 格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
        // 用于存储每月总金额的 Map
        TreeMap<String, Double> monthlyTotals = new TreeMap<>();
        // 手动计算每月总交易金额
        for (TransactionRecords record : transactionRecords) {
            // 过滤过去一年的记录
            if (record.getTransactionTime().after(oneYearAgo) && !record.getTransactionType().contains("退款") && !record.getInOut().contains("收入")) {
                // 格式化为 "yyyy-MM"
                String month = dateFormat.format(record.getTransactionTime());
                // 累加每月总金额
                monthlyTotals.put(month,
                        monthlyTotals.getOrDefault(month, 0.00) + record.getAmount().doubleValue());
            }
        }
        for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
            BigDecimal bd = new BigDecimal(Double.toString(entry.getValue()));
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            double value = bd.doubleValue();
            TotalAmount totalAmount = new TotalAmount(entry.getKey(), value);
            dbData.add(totalAmount);
        }
        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }

    /**
     * 获取最近一年的每月支出柱状图数据。
     *
     * @return 每月支出柱状图数据列表，包含月份、收入、支出
     */
    @Override
    public List<MonthlyExpenditureBarChart> getMonthlyExpenditureBarChart(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "MonthlyExpenditureBarChart" + "_" + id;

//        // 1. 优先从缓存中读取数据
//        List<MonthlyExpenditureBarChart> cacheData = redisCache.getCacheList(cacheKeyPie);
//        if (!CollectionUtils.isEmpty(cacheData)) {
//            return cacheData;
//        }

        // 2. 获取当前日期和往前推一年的起始日期
        LocalDate currentDate = LocalDate.now();
        LocalDate oneYearAgoDate = currentDate.minusYears(1).withDayOfMonth(1); // 上一年1号

        // 获取最近的完整月最后一天，例如 2025-06-30
        LocalDate lastFullMonth = currentDate.minusMonths(1);
        YearMonth lastYearMonth = YearMonth.from(lastFullMonth);
        LocalDate endDate = lastYearMonth.atEndOfMonth();

        // 3. 格式化为 yyyy-MM-dd 字符串，用于查询
        String startDateStr = oneYearAgoDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 4. 查询这一年内的所有银行卡交易记录
        List<BankCardTransactions> bankCardTransactions = iBankCardTransactionsService.queryBankCardTransactionsYearList(id, startDateStr, endDateStr);

        // 5. 查询当前用户账户的银行列表（排除互转）
        List<UserAccounts> userAccounts = userAccountsService.selectUserAccountsList(new UserAccounts());
        List<String> counterPartyList = userAccounts.stream()
                .map(UserAccounts::getBankCardNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 6. 使用 TreeMap 确保按月份排序，分别聚合收入与支出
        Map<YearMonth, BigDecimal> totalIncomeByMonth = new TreeMap<>();
        Map<YearMonth, BigDecimal> totalExpenseByMonth = new TreeMap<>();

        // 7. 聚合处理交易记录
        for (BankCardTransactions transaction : bankCardTransactions) {
            String counterParty = transaction.getCounterParty();

            // 排除互转、还款、证券转账（模糊匹配）
            boolean isExcluded = counterPartyList.stream().anyMatch(counterParty::contains)
                    || counterParty.contains("还款")
                    || counterParty.contains("中国银河证券");
            if (isExcluded) {
                continue;
            }

            BigDecimal amount = transaction.getAmount();
            LocalDate date = transaction.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            YearMonth ym = YearMonth.from(date);

            if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                totalIncomeByMonth.merge(ym, amount, BigDecimal::add);
            } else {
                totalExpenseByMonth.merge(ym, amount.abs(), BigDecimal::add);
            }
        }

        // 8. 合并结果为 DTO 列表
        List<MonthlyExpenditureBarChart> result = new ArrayList<>();
        for (YearMonth ym : totalIncomeByMonth.keySet()) {
            BigDecimal income = totalIncomeByMonth.getOrDefault(ym, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal expense = totalExpenseByMonth.getOrDefault(ym, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            result.add(new MonthlyExpenditureBarChart(ym.toString(), income.doubleValue(), expense.doubleValue()));
        }

        // 9. 写入 Redis 缓存，有效期 24 小时
        if (!result.isEmpty()) {
            redisCache.setCacheList(cacheKeyPie, result);
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }

        return result;
    }

    /**
     * 获取贷款总额偿还饼形图数据列表。
     *
     * @return 包含贷款总额偿还饼形图数据项的列表。
     */
    @Override
    public List<LoanTotalRepaymentPieChart> getRepaymentPieChart(Long id) {

        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "RepaymentPieChart" + "_" + id;
        List<LoanTotalRepaymentPieChart> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }

        List<LoanTotalRepaymentPieChart> dbData = new ArrayList<>();
        //1、获取全部数据
        LoanRepayments loanRepayment = new LoanRepayments();
        loanRepayment.setUserId(id);
        List<LoanRepayments> loanRepaymentsList = iLoanRepaymentsService.selectLoanRepaymentsChart(loanRepayment);
        // 2.累加每月偿还的本金
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        for (LoanRepayments loanRepayments : loanRepaymentsList) {
            if (loanRepayments.getIsSettled().equals(1)) {
                totalPrincipal = totalPrincipal.add(loanRepayments.getPrincipal());
            }
        }

        // 3.添加累加的本金到列表中返回
        dbData.add(new LoanTotalRepaymentPieChart("已偿还本金", totalPrincipal.doubleValue()));
        dbData.add(new LoanTotalRepaymentPieChart("未偿还本金",
                new BigDecimal("660000").subtract(totalPrincipal).setScale(2, RoundingMode.HALF_UP).doubleValue()));

        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }

    /**
     * 获取贷款总额加利息偿还饼形图数据列表。
     *
     * @return 包含贷款总额加利息偿还饼形图数据项的列表。
     */
    @Override
    public List<LoanTotalWithInterestRepaymentPieChart> getTotalRepaymentPieChart(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "TotalRepaymentPieChart" + "_" + id;
        List<LoanTotalWithInterestRepaymentPieChart> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }

        List<LoanTotalWithInterestRepaymentPieChart> dbData = new ArrayList<>();
        LoanRepayments loanRepayment = new LoanRepayments();
        loanRepayment.setUserId(id);
        List<LoanRepayments> loanRepaymentsList = iLoanRepaymentsService.selectLoanRepaymentsChart(loanRepayment);

        // 1.分别累加每月偿还的本金与利息
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalInterestAmount = BigDecimal.ZERO;

        for (LoanRepayments loanRepayments : loanRepaymentsList) {
            if (loanRepayments.getIsSettled().equals(1)) {
                totalPrincipal = totalPrincipal.add(loanRepayments.getPrincipal());
                totalInterest = totalInterest.add(loanRepayments.getInterest());
            }
            totalInterestAmount = totalInterestAmount.add(loanRepayments.getInterest());
        }
        // 2.添加累加每月偿还的本金与利息到列表中
        dbData.add(new LoanTotalWithInterestRepaymentPieChart("已偿还本金", totalPrincipal.doubleValue()));
        dbData.add(new LoanTotalWithInterestRepaymentPieChart("已偿还利息", totalInterest.doubleValue()));
        dbData.add(new LoanTotalWithInterestRepaymentPieChart("未还利息",
                totalInterestAmount.subtract(totalInterest).setScale(2, RoundingMode.HALF_UP).doubleValue()));
        if (id.equals(2L)) {
            dbData.add(new LoanTotalWithInterestRepaymentPieChart("未还本金",
                    new BigDecimal("660000").subtract(totalPrincipal).setScale(2, RoundingMode.HALF_UP).doubleValue()));
        } else if (id.equals(3L)) {
            dbData.add(new LoanTotalWithInterestRepaymentPieChart("未还本金",
                    new BigDecimal("0").subtract(totalPrincipal).setScale(2, RoundingMode.HALF_UP).doubleValue()));
        } else {
            dbData.add(new LoanTotalWithInterestRepaymentPieChart("未还本金",
                    new BigDecimal("0").subtract(totalPrincipal).setScale(2, RoundingMode.HALF_UP).doubleValue()));
        }

        // 3.添加累加每月偿还的本金与利息到列表中返回
        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }

    /**
     * 获取总金额柱状图数据。
     *
     * @return 包含总金额柱状图数据的列表，每个 Map 包含分类名称和累计金额
     */
    @Override
    public List<Map<String, Object>> getWechatAlipayData(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "WechatAlipayData" + "_" + id;
        List<Map<String, Object>> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }

        // 获取交易分类数据（例如：餐饮、住房、交通、娱乐、医疗等）
        List<TransactionCategories> transactionCategories = transactionCategoriesService.selectTransactionCategoriesList(new TransactionCategories());

        // 初始化各分类关键词（存放 JSON 格式的关键词数组字符串）
        String restaurantKeywords = "";
        String accommodationKeywords = "";
        String transportationKeywords = "";
        String entertainmentKeywords = "";
        String medicalKeywords = "";

        // 根据分类名称设置对应的关键词
        for (TransactionCategories category : transactionCategories) {
            String categoryName = category.getCategoryName();
            String keywordJson = category.getKeyword();
            switch (categoryName) {
                case "餐饮":
                    restaurantKeywords = keywordJson;
                    break;
                case "住房":
                    accommodationKeywords = keywordJson;
                    break;
                case "交通":
                    transportationKeywords = keywordJson;
                    break;
                case "娱乐":
                    entertainmentKeywords = keywordJson;
                    break;
                case "医疗":
                    medicalKeywords = keywordJson;
                    break;
                default:
                    break;
            }
        }

        // 初始化返回列表，每个 Map 存放分类名称和金额
        // 顺序分别为：0-餐饮, 1-住房, 2-交通, 3-娱乐, 4-医疗, 5-其他
        List<Map<String, Object>> dbData = new ArrayList<>();
        dbData.add(new HashMap<String, Object>() {{
            put("category", "餐饮");
        }});
        dbData.add(new HashMap<String, Object>() {{
            put("category", "住房");
        }});
        dbData.add(new HashMap<String, Object>() {{
            put("category", "交通");
        }});
        dbData.add(new HashMap<String, Object>() {{
            put("category", "娱乐");
        }});
        dbData.add(new HashMap<String, Object>() {{
            put("category", "医疗");
        }});
        dbData.add(new HashMap<String, Object>() {{
            put("category", "其他");
        }});

        // 获取交易记录数据，返回的数据中包含分类（category）和金额（amount）
        List<Map<String, Object>> transactionData = transactionRecordsMapper.selectTotalAmountColumnChartData(id);
        if (transactionData.size() <= 0) {
            return dbData;
        }
        // 遍历所有交易记录，根据分类匹配关键词，将金额累加到对应分类中
        for (Map<String, Object> record : transactionData) {
            String recordCategory = record.get("category").toString();
            BigDecimal amount = new BigDecimal(record.get("amount").toString());

            if (isKeywordInString(restaurantKeywords, recordCategory)) {
                // 如果记录的分类与餐饮关键词匹配，则累加到餐饮分类（索引 0）
                addAmount(dbData.get(0), amount);
            } else if (isKeywordInString(accommodationKeywords, recordCategory)) {
                // 如果记录的分类与住房关键词匹配，则累加到住房分类（索引 1）
                addAmount(dbData.get(1), amount);
            } else if (isKeywordInString(transportationKeywords, recordCategory)) {
                // 如果记录的分类与交通关键词匹配，则累加到交通分类（索引 2）
                addAmount(dbData.get(2), amount);
            } else if (isKeywordInString(entertainmentKeywords, recordCategory)) {
                // 如果记录的分类与娱乐关键词匹配，则累加到娱乐分类（索引 3）
                addAmount(dbData.get(3), amount);
            } else if (isKeywordInString(medicalKeywords, recordCategory)) {
                // 如果记录的分类与医疗关键词匹配，则累加到医疗分类（索引 4）
                addAmount(dbData.get(4), amount);
            } else {
                // 不匹配以上任何关键词的，归入“其他”分类（索引 5）
                addAmount(dbData.get(5), amount);
            }
        }

        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }


    /**
     * 获取每月收入与支出的比例数据，用于饼图展示。
     * 如果当前日期在15号或之后，则取当月的数据；
     * 否则，取上个月的数据。
     *
     * @return 包含收入和支出数据的饼图数据列表
     */
    @Override
    public List<LoanTotalWithInterestRepaymentPieChart> getMonthIncomeExpenseRatio(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "MonthIncomeExpenseRatio" + "_" + id;
        List<LoanTotalWithInterestRepaymentPieChart> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }
        // 获取当前日期
        LocalDate now = LocalDate.now();

        // 根据当前日期判断应使用哪个日期范围
        LocalDate startDate;
        LocalDate endDate;
        // 使用当月的日期范围
        YearMonth currentMonth = YearMonth.from(now);
        // 当月的1号
        startDate = currentMonth.atDay(1);
        // 当月的最后一天
        endDate = currentMonth.atEndOfMonth();

        // 准备结果数据列表
        List<LoanTotalWithInterestRepaymentPieChart> dbData = new ArrayList<>();

        try {
            // 获取支出总额
            BigDecimal expenseAmount = expenseService.getCurrentMonthExpenseTotal(id, startDate, endDate);

            // 获取收入总额，调整后的收入为月初到月末的实际净收入
            BigDecimal incomeAmount = incomeService.getCurrentMonthIncomeTotal(id, startDate, endDate);
            if (null == expenseAmount || null == incomeAmount) {
                return dbData;
            }
            BigDecimal netIncomeAmount = incomeAmount.subtract(expenseAmount);

            // 创建并添加支出数据
            LoanTotalWithInterestRepaymentPieChart expense = createPieData("支出", expenseAmount);
            dbData.add(expense);

            // 创建并添加收入数据
            LoanTotalWithInterestRepaymentPieChart income = createPieData("结余", netIncomeAmount);
            dbData.add(income);

        } catch (Exception e) {
            // 捕获并处理可能的数据获取异常
            logger.error("获取收入与支出数据失败", e);
            throw new BusinessException("获取收入与支出数据失败", e);
        }

        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;
    }

    @Override
    public List<LoanTotalWithInterestRepaymentPieChart> getYearIncomeExpenseRatio(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "YearIncomeExpenseRatio" + "_" + id;
        List<LoanTotalWithInterestRepaymentPieChart> cacheData = redisCache.getCacheList(cacheKeyPie);

        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }
        // 获取当前日期
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        // 设置当年的开始和结束日期
        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);

        // 如果当前日期在6月1日之前，则使用上一年的日期范围
        if (now.isBefore(LocalDate.of(currentYear, 1, 15))) {
            currentYear = currentYear - 1;
            startDate = LocalDate.of(currentYear, 1, 1);
            endDate = LocalDate.of(currentYear, 12, 31);
        }

        List<LoanTotalWithInterestRepaymentPieChart> dbData = new ArrayList<>();

        try {
            // 获取支出总额
            BigDecimal expenseAmount = expenseService.getCurrentMonthExpenseTotal(id, startDate, endDate);

            // 获取收入总额，调整后的收入为月初到月末的实际净收入
            BigDecimal incomeAmount = incomeService.getCurrentMonthIncomeTotal(id, startDate, endDate);

            if (null == expenseAmount || null == incomeAmount) {
                return dbData;
            }

            BigDecimal netIncomeAmount = incomeAmount.subtract(expenseAmount);

            // 创建并添加支出数据
            LoanTotalWithInterestRepaymentPieChart expense = createPieData("支出", expenseAmount);
            dbData.add(expense);

            // 创建并添加收入数据
            LoanTotalWithInterestRepaymentPieChart income = createPieData("结余", netIncomeAmount);
            dbData.add(income);

        } catch (Exception e) {
            // 捕获异常并记录日志
            System.out.println("获取收入与支出数据失败：" + e.getMessage());
            throw new BusinessException("获取收入与支出数据失败", e);
        }

        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, TIME_OUT_HOURS, TimeUnit.HOURS);
        }
        return dbData;

    }

    @Override
    public Map<String, Object> getProfitLineData(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "ProfitLineData_v2" + "_" + id;
        Map<String, Object> cacheData = redisCache.getCacheMap(cacheKeyPie);
        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }
        List<SalesData> lineData = salesDataService.selectSalesDataCurrentYear(id);
        List<SalesData> barData = salesDataService.selectSalesDataYearlyMax(id);

        Map<String, Object> result = new HashMap<>();
        result.put("line", lineData);
        result.put("bar", barData);

        if (!CollectionUtils.isEmpty(lineData) || !CollectionUtils.isEmpty(barData)) {
            redisCache.setCacheMap(cacheKeyPie, result);
            // 设置这个键的过期时间为 24 小时
            redisCache.setExpireTime(cacheKeyPie, 10, TimeUnit.MINUTES);
        }
        return result;
    }

    /**
     * 创建并初始化饼图数据对象。
     *
     * @param category 分类名称，如“支出”或“收入”
     * @param amount   金额
     * @return 初始化后的饼图数据对象
     */
    private LoanTotalWithInterestRepaymentPieChart createPieData(String category, BigDecimal amount) {
        LoanTotalWithInterestRepaymentPieChart pieData = new LoanTotalWithInterestRepaymentPieChart();
        pieData.setCategory(category);
        pieData.setAmount(amount.doubleValue());
        return pieData;
    }


    /**
     * 辅助方法：累加金额到指定 Map 中的 "amount" 字段
     *
     * @param map    要累加的 Map
     * @param amount 需要累加的金额
     */
    private void addAmount(Map<String, Object> map, BigDecimal amount) {
        if (map.get("amount") == null) {
            map.put("amount", amount);
        } else {
            BigDecimal currentAmount = new BigDecimal(map.get("amount").toString());
            map.put("amount", currentAmount.add(amount));
        }
    }

    /**
     * 检查给定文本是否包含 JSON 数组中定义的任一个关键词
     * 要求文本必须包含关键词的整个字符串（作为完整子串出现）
     *
     * @param keywordJson JSON 格式的关键词数组字符串，每个元素包含 "keyword" 字段
     * @param text        需要检查的文本
     * @return 如果文本包含任一完整关键词则返回 true，否则返回 false
     */
    private static boolean isKeywordInString(String keywordJson, String text) {
        // 使用 Gson 解析 JSON 格式的关键词数组
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(keywordJson, JsonArray.class);

        // 遍历每个关键词对象，判断文本中是否包含该关键词
        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            String keyword = jsonObject.get("keyword").getAsString();
            // 判断 text 是否包含完整的关键词
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取每月的收入与支出柱状图数据
     * 优先读取 Redis 缓存，缓存未命中则从数据库中查询，并按月统计收入和支出
     * 缓存结果存储 1 小时
     *
     * @return 每月收入与支出的柱状图数据列表
     */
    @Override
    /**
     * 获取用户近 12 个月的收入、支出与结余柱状图数据（优先使用缓存）
     *
     * @param id 用户ID
     * @return 近 12 个月的收入、支出、结余数据列表
     */
    public List<MonthlyExpenditureBarChart> getMonthlyIncomeBarChart(Long id) {
        // ========================== 1. 构建缓存 Key 并尝试读取缓存 ==========================
        String cacheKeyBar = CacheConstants.HOMEPAGE_CACHE_PREFIX + "MonthlyIncomeBarChart_" + id;

        List<MonthlyExpenditureBarChart> cacheData = redisCache.getCacheList(cacheKeyBar);
        if (!CollectionUtils.isEmpty(cacheData)) {
            // ✅ 缓存命中则直接返回
            return cacheData;
        }

        // ========================== 2. 计算近 12 个月的时间范围 ==========================
        LocalDate now = LocalDate.now();
        LocalDate startMonth = now.minusMonths(11).withDayOfMonth(1); // 起点为 12 个月前的第一天
        LocalDate endMonth = now.with(TemporalAdjusters.lastDayOfMonth()); // 当前月的最后一天

        Date startDate = Date.from(startMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // ========================== 3. 查询数据库中收入与支出数据 ==========================
        Income incomeParam = new Income();
        incomeParam.setUserId(id);
        incomeParam.setStartDate(startDate);
        incomeParam.setEndDate(endDate);

        Expense expenseParam = new Expense();
        expenseParam.setUserId(id);
        expenseParam.setStartDate(startDate);
        expenseParam.setEndDate(endDate);

        List<Income> incomes = incomeService.selectIncomeList(incomeParam);
        List<Expense> expenses = expenseService.selectExpenseList(expenseParam);

        // ========================== 4. 按月汇总收入与支出 ==========================
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 按月汇总收入金额
        Map<String, BigDecimal> incomeMap = incomes.stream()
                .filter(i -> i.getIncomeDate() != null)
                .collect(Collectors.groupingBy(
                        i -> formatter.format(i.getIncomeDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)),
                        Collectors.reducing(BigDecimal.ZERO, Income::getAmount, BigDecimal::add)
                ));

        // 按月汇总支出金额
        Map<String, BigDecimal> expenseMap = expenses.stream()
                .filter(e -> e.getExpenseDate() != null)
                .collect(Collectors.groupingBy(
                        e -> formatter.format(e.getExpenseDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // ========================== 5. 构建完整的 12 个月时间序列 ==========================
        List<String> months = Stream.iterate(startMonth, date -> date.plusMonths(1))
                .limit(12)
                .map(formatter::format)
                .collect(Collectors.toList());

        // ========================== 6. 合并汇总数据并构建返回列表 ==========================
        List<MonthlyExpenditureBarChart> result = months.stream().map(month -> {
            MonthlyExpenditureBarChart chart = new MonthlyExpenditureBarChart();
            chart.setTransactionTime(month);

            BigDecimal income = incomeMap.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = expenseMap.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal balance = income.subtract(expense).setScale(2, RoundingMode.HALF_UP);

            // ✅ 保留两位小数并转为 double
            chart.setSupportInAmount(income.setScale(2, RoundingMode.HALF_UP).doubleValue());
            chart.setSupportOutAmount(expense.setScale(2, RoundingMode.HALF_UP).doubleValue());
            chart.setBalanceAmount(balance.doubleValue());
            return chart;
        }).collect(Collectors.toList());

        // ========================== 7. 缓存结果并设置过期时间 ==========================
        if (!CollectionUtils.isEmpty(result)) {
            redisCache.setCacheList(cacheKeyBar, result);
            redisCache.setExpireTime(cacheKeyBar, 1, TimeUnit.HOURS);
        }

        // ========================== 8. 返回最终结果 ==========================
        return result;
    }


    /**
     * 构建最近 12 个月的贷款还款比较柱状图数据，显式代码只有一次循环。
     */
    @Override
    public List<MonthlyExpenditureBarChart> renderLoanRepaymentComparisonChart(Long id) {
        String cacheKeyPie = CacheConstants.HOMEPAGE_CACHE_PREFIX + "renderLoanRepaymentComparisonChart" + "_" + id;

        // 1. 优先从缓存中读取数据
        List<MonthlyExpenditureBarChart> cacheData = redisCache.getCacheList(cacheKeyPie);
        if (!CollectionUtils.isEmpty(cacheData)) {
            return cacheData;
        }

        // 2. 查询数据库中的还款数据，时间范围：当月往前推 12 个月（共 12 个月，含当月）
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, 31);
        Date endDate = now.getTime();
        now.add(Calendar.YEAR, -1);
        now.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = now.getTime();

        LoanRepayments query = new LoanRepayments();
        query.setUserId(id);
        query.setStartDate(startDate);
        query.setEndDate(endDate);

        List<LoanRepayments> loanRepaymentsList = iLoanRepaymentsService.selectLoanRepaymentsChart(query);

        // 3. 使用 Stream 聚合各月还款金额（无显式循环）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        Map<String, BigDecimal> monthAmountMap = CollectionUtils.isEmpty(loanRepaymentsList)
                ? Collections.emptyMap()
                : loanRepaymentsList.stream()
                .filter(r -> r.getRepaymentDate() != null && r.getTotalPrincipalAndInterest() != null)
                .collect(Collectors.groupingBy(
                        r -> sdf.format(r.getRepaymentDate()),
                        Collectors.reducing(BigDecimal.ZERO, LoanRepayments::getTotalPrincipalAndInterest, BigDecimal::add)));

        // 4. 用一次循环完成补全 12 个月并构建图表数据
        List<MonthlyExpenditureBarChart> dbData = new ArrayList<>(12);
        Calendar iter = Calendar.getInstance();
        iter.set(Calendar.DAY_OF_MONTH, 1);
        iter.add(Calendar.MONTH, -11); // 从 11 个月前开始
        for (int i = 0; i < 12; i++) { // **唯一显式循环**
            String monthKey = sdf.format(iter.getTime());
            BigDecimal amount = monthAmountMap.getOrDefault(monthKey, BigDecimal.ZERO);
            dbData.add(new MonthlyExpenditureBarChart(monthKey, amount.doubleValue()));
            iter.add(Calendar.MONTH, 1);
        }

        // 5. 缓存数据并设置过期时间为 1 小时
        if (!CollectionUtils.isEmpty(dbData)) {
            redisCache.setCacheList(cacheKeyPie, dbData);
            redisCache.setExpireTime(cacheKeyPie, 1, TimeUnit.HOURS);
        }

        return dbData;
    }


    /**
     * 检查服务器授权状态并更新剩余天数
     *
     * <p>逻辑说明：
     * 1. 查询所有状态为“active”的服务器；
     * 2. 解析服务器到期日期（兼容 Date 或字符串类型）；
     * 3. 计算与当前日期的剩余天数；
     * 4. 更新数据库中对应字段；
     * 5. 返回更新结果（JSON格式）。</p>
     *
     * @return JSON字符串形式的服务器授权检查结果
     */
    @Override
    public String getLicenseCheck() {
        // 1. 构建查询条件，仅获取状态为 active 的服务器
        ServerInfo query = new ServerInfo();
        query.setStatus("active");

        // 2. 查询服务器信息列表
        List<ServerInfo> serverList = serverInfoService.selectServerInfoList(query);
        if (serverList == null || serverList.isEmpty()) {
            return JSON.toJSONString(Collections.emptyList());
        }

        // 3. 获取当前日期
        LocalDate now = LocalDate.now();

        // 4. 遍历服务器列表，计算并更新剩余天数
        for (ServerInfo info : serverList) {
            LocalDate expireDate = null;

            try {
                Object expireObj = info.getExpireDate();
                if (expireObj != null) {
                    if (expireObj instanceof Date) {
                        // ✅ 如果是 java.util.Date 类型，直接转换
                        expireDate = ((Date) expireObj).toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    } else {
                        // ✅ 若是字符串，如 "Thu Aug 06 00:00:00 CST 2026"
                        String expireStr = expireObj.toString().trim();

                        // 若为标准格式 yyyy-MM-dd
                        if (expireStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            expireDate = LocalDate.parse(expireStr);
                        } else {
                            // 否则按 Date.toString() 默认格式解析
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                            Date parsed = sdf.parse(expireStr);
                            expireDate = parsed.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("服务器 [{}] 的到期日期格式错误: {}", info.getServerName(), info.getExpireDate(), e);
                continue;
            }

            // 无到期日期则跳过
            if (expireDate == null) {
                logger.debug("服务器 [{}] 未设置到期日期，跳过处理。", info.getServerName());
                continue;
            }

            // 5. 计算剩余天数（若已过期则为0）
            long remainingDays = ChronoUnit.DAYS.between(now, expireDate);
            remainingDays = Math.max(remainingDays, 0);

            // 更新实体字段
            info.setRemindDays(remainingDays);

            // 6. 更新数据库中到期天数字段
            try {
                serverInfoService.updateServerInfo(info);
                logger.info("服务器 [{}] 到期日 [{}]，剩余天数 [{}] 天，已更新。",
                        info.getServerName(), expireDate, remainingDays);
            } catch (Exception e) {
                logger.error("更新服务器 [{}] 到期信息时出错: {}", info.getServerName(), e.getMessage(), e);
            }
        }

        // 7. 返回更新后的结果（转为 JSON）
        return JSON.toJSONString(serverList);
    }


}
