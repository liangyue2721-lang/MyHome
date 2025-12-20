package com.make.quartz.service.finance.impl;

import com.make.common.core.redis.RedisCache;
import com.make.common.utils.DateUtils;
import com.make.finance.domain.AnnualDepositSummary;
import com.make.finance.domain.AssetRecord;
import com.make.finance.domain.Expense;
import com.make.finance.domain.LoanRepayments;
import com.make.finance.service.IAnnualDepositSummaryService;
import com.make.finance.service.IAssetRecordService;
import com.make.finance.service.IExpenseService;
import com.make.finance.service.ILoanRepaymentsService;
import com.make.quartz.service.common.DistributedLockService;
import com.make.quartz.service.finance.DepositService;
import com.make.quartz.service.stock.ProfitService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 存款服务实现类
 * 迁移自 IFinanceTaskServiceImpl
 */
@Service
public class DepositServiceImpl implements DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositServiceImpl.class);

    @Resource
    private IAnnualDepositSummaryService annualDepositSummaryService;

    @Resource
    private IAssetRecordService assetRecordService;

    @Resource
    private ILoanRepaymentsService loanRepaymentsService;

    @Resource
    private IExpenseService expenseService;

    @Resource
    private RedisCache redisCache;

    // 使用 Lazy 注入避免潜在循环依赖，虽然这里可能不需要，但ProfitService可能会反向引用Facade
    @Resource
    @Lazy
    private ProfitService profitService;

    @Override
    @Transactional
    public void refreshDepositAmount() {
        log.info("【DepositService】开始刷新年度存款与工行存款数据");
        updateAnnualDepositSummary();
        // 更新工商银行存款数据（固定参数：loanRepaymentId=1L, assetId=7L）
        updateBankDepositAmount(BankType.ICBC, 1L, 7L);

        // 调用利润服务
        if (profitService != null) {
            profitService.queryStockProfitData();
        } else {
            log.warn("【DepositService】ProfitService未注入，跳过queryStockProfitData");
        }

        log.info("【DepositService】刷新存款数据完成");
    }

    @Override
    public void updateAnnualDepositSummary() {
        int currentYear = LocalDate.now().getYear();
        List<AssetRecord> records = assetRecordService.selectAssetRecordList(new AssetRecord());

        if (CollectionUtils.isEmpty(records)) {
            log.info("【DepositService】未查询到资产记录，跳过年度统计");
            return;
        }

        Map<Long, BigDecimal> totalByUser = calculateUserTotal(records);
        totalByUser.forEach((userId, total) -> upsertAnnualDeposit(currentYear, userId, total));
    }

    @Override
    public void updateICBCDepositAmount(Long loanRepaymentId, Long assetId) {
         updateBankDepositAmount(BankType.ICBC, loanRepaymentId, assetId);
    }

    private void updateBankDepositAmount(BankType bankType, Long loanRepaymentId, Long assetId) {
        // 1. 日期检查
        int today = LocalDate.now().getDayOfMonth();
        if (today != 11) {
            log.info("【DepositService】当前日期为 {} 日，非 11 号，跳过银行存款更新。", today);
            return;
        }

        if (bankType == null) {
            throw new IllegalArgumentException("Bank type cannot be null");
        }

        if (bankType == BankType.ICBC) {
             processICBCDepositAmount(loanRepaymentId, assetId);
        } else {
             throw new UnsupportedOperationException("Unsupported bank type: " + bankType);
        }
    }

    private void processICBCDepositAmount(Long loanRepaymentId, Long assetId) {
        LoanRepayments repayment = findLoanRepaymentForCurrentMonth(loanRepaymentId);
        AssetRecord asset = findAndValidateAsset(assetId, repayment.getTotalPrincipalAndInterest());
        if (asset != null) {
            deductAssetBalance(asset, repayment.getTotalPrincipalAndInterest());
            syncExpenseRecord(asset.getUserId(), repayment);
        }
    }

    private Map<Long, BigDecimal> calculateUserTotal(List<AssetRecord> records) {
        return records.stream()
                .filter(r -> r.getUserId() != null && r.getAmount() != null)
                .collect(Collectors.groupingBy(
                        AssetRecord::getUserId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                r -> r.getAmount().setScale(2, RoundingMode.HALF_UP),
                                BigDecimal::add
                        )
                ));
    }

    private void upsertAnnualDeposit(int year, Long userId, BigDecimal total) {
        AnnualDepositSummary summary = annualDepositSummaryService
                .queryAnnualDepositSummaryByYearAndUser(year, userId);

        Date now = new Date();

        if (summary != null) {
            summary.setTotalDeposit(total);
            summary.setUpdateTime(now);
            annualDepositSummaryService.updateAnnualDepositSummary(summary);
            log.debug("【DepositService】更新年度存款 | 用户:{} 年份:{} 金额:{}", userId, year, total);
        } else {
            AnnualDepositSummary newSummary = new AnnualDepositSummary();
            newSummary.setYear((long) year);
            newSummary.setUserId(userId);
            newSummary.setTotalDeposit(total);
            newSummary.setRemark("系统自动统计");
            newSummary.setCreateTime(now);
            annualDepositSummaryService.insertAnnualDepositSummary(newSummary);
            log.debug("【DepositService】插入年度存款 | 用户:{} 年份:{} 金额:{}", userId, year, total);
        }
    }

    private LoanRepayments findLoanRepaymentForCurrentMonth(Long loanRepaymentId) {
        Date repayDate = DateUtils.getThisMonthEleventhByCalendar(11);
        LoanRepayments repayment = loanRepaymentsService.selectLoanRepaymentsByDate(repayDate);

        if (repayment == null) {
            throw new NoSuchElementException("未找到还款记录，ID: " + loanRepaymentId);
        }
        return repayment;
    }

    private AssetRecord findAndValidateAsset(Long assetId, BigDecimal repayAmount) {
        markLoanSynced(assetId);
        if (isLoanSyncedToday(assetId)) {
            return null;
        }
        AssetRecord asset = assetRecordService.selectAssetRecordByAssetId(assetId);
        if (asset == null) {
            throw new NoSuchElementException("未找到资产记录，ID: " + assetId);
        }
        if (asset.getAmount().compareTo(repayAmount) < 0) {
            throw new IllegalStateException(String.format(
                    "资产余额不足: 当前=%s, 需要=%s", asset.getAmount(), repayAmount));
        }
        markLoanSynced(assetId);
        return asset;
    }

    private boolean isLoanSyncedToday(Long assetId) {
        String key = "loan:sync:" + assetId + ":" + LocalDate.now();
        return redisCache.hasKey(key);
    }

    private void markLoanSynced(Long assetId) {
        String key = "loan:sync:" + assetId + ":" + LocalDate.now();
        redisCache.setCacheObjectWithExpire(key, "1", 1, TimeUnit.DAYS);
    }

    private void deductAssetBalance(AssetRecord asset, BigDecimal amount) {
        asset.setAmount(asset.getAmount().subtract(amount));
        asset.setUpdateTime(new Date());
        assetRecordService.updateAssetRecord(asset);
        log.debug("【DepositService】已扣减资产余额 | 用户:{} 新余额:{}", asset.getUserId(), asset.getAmount());
    }

    private void syncExpenseRecord(Long userId, LoanRepayments repayment) {
        Expense expense = expenseService.selectExpenseByUserIdAndLoan(userId, repayment.getRepaymentDate());

        if (expense != null) {
            expense.setAmount(repayment.getTotalPrincipalAndInterest());
            expenseService.updateExpense(expense);
            log.debug("【DepositService】更新消费记录 | 用户:{} 日期:{} 金额:{}", userId, repayment.getRepaymentDate(), expense.getAmount());
        } else {
            Expense newExpense = new Expense();
            newExpense.setUserId(userId);
            newExpense.setExpenseDate(repayment.getRepaymentDate());
            newExpense.setAmount(repayment.getTotalPrincipalAndInterest());
            expenseService.insertExpense(newExpense);
            log.debug("【DepositService】插入消费记录 | 用户:{} 日期:{} 金额:{}", userId, repayment.getRepaymentDate(), newExpense.getAmount());
        }
    }

    private enum BankType {
        ICBC, CCB, ABC, BOC
    }
}
