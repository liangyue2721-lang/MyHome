package com.make.quartz.service.impl;

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
import com.make.quartz.service.IFinanceTaskService;
import com.make.quartz.service.IRealTimeService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 财务任务服务实现类
 * 处理定期财务任务，如存款统计和银行账户同步
 */
@Service
public class IFinanceTaskServiceImpl implements IFinanceTaskService {

    //------------------------ 日志配置 ------------------------
    private static final Logger log = LoggerFactory.getLogger(IFinanceTaskServiceImpl.class);

    //------------------------ 依赖注入 ------------------------

    @Resource
    private IAnnualDepositSummaryService annualDepositSummaryService;

    @Resource
    private IAssetRecordService assetRecordService;

    @Resource
    private ILoanRepaymentsService loanRepaymentsService;

    @Resource
    private IExpenseService expenseService;
    @Resource
    private IRealTimeService realTimeService;


    @Resource
    private RedisCache redisCache;

    /**
     * 刷新存款金额主入口方法
     * 包含年度存款统计和银行存款数据更新
     */
    @Override
    @Transactional
    public void refreshDepositAmount() {
        log.info("【DepositService】开始刷新年度存款与工行存款数据");
        // 更新所有用户的年度存款汇总
        updateAnnualDepositSummary();
        // 更新工商银行存款数据（固定参数：loanRepaymentId=1L, assetId=7L）
        updateBankDepositAmount(BankType.ICBC, 1L, 7L);
        realTimeService.queryStockProfitData();
        log.info("【DepositService】刷新存款数据完成");
    }

    /**
     * 更新所有用户的年度资产汇总。
     *
     * <p>核心逻辑：</p>
     * <ol>
     *   <li>查询所有资产记录</li>
     *   <li>按用户分组计算总额</li>
     *   <li>批量插入或更新汇总表</li>
     * </ol>
     */
    private void updateAnnualDepositSummary() {
        // 获取当前年份
        int currentYear = LocalDate.now().getYear();
        // 查询所有资产记录
        List<AssetRecord> records = assetRecordService.selectAssetRecordList(new AssetRecord());

        if (CollectionUtils.isEmpty(records)) {
            log.info("【DepositService】未查询到资产记录，跳过年度统计");
            return;
        }

        // 按用户分组计算资产总额
        Map<Long, BigDecimal> totalByUser = calculateUserTotal(records);

        // 对每个用户更新或插入年度存款汇总记录
        totalByUser.forEach((userId, total) -> upsertAnnualDeposit(currentYear, userId, total));
    }

    /**
     * 按用户汇总资产总额。
     *
     * @param records 资产记录列表
     * @return 按用户ID分组的资产总额映射
     */
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

    /**
     * 根据用户与年份插入或更新汇总记录。
     *
     * @param year   年份
     * @param userId 用户ID
     * @param total  总金额
     */
    private void upsertAnnualDeposit(int year, Long userId, BigDecimal total) {
        // 查询是否存在该用户该年的汇总记录
        AnnualDepositSummary summary = annualDepositSummaryService
                .queryAnnualDepositSummaryByYearAndUser(year, userId);

        Date now = new Date();

        if (summary != null) {
            // 如果存在则更新
            summary.setTotalDeposit(total);
            summary.setUpdateTime(now);
            annualDepositSummaryService.updateAnnualDepositSummary(summary);
            log.debug("【DepositService】更新年度存款 | 用户:{} 年份:{} 金额:{}", userId, year, total);
        } else {
            // 如果不存在则新增
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

    /**
     * 更新指定银行（如工行）的存款数据。
     *
     * @param bankType        银行类型（如ICBC）
     * @param loanRepaymentId 还款记录ID
     * @param assetId         资产ID
     */
    public void updateBankDepositAmount(BankType bankType, Long loanRepaymentId, Long assetId) {

        // ======== 1. 日期检查 ========
        int today = LocalDate.now().getDayOfMonth();
        if (today != 11) {
            log.info("【DepositService】当前日期为 {} 日，非 11 号，跳过银行存款更新。", today);
            return; // ✅ 直接跳过，不抛异常
        }

        // ======== 2. 参数检查 ========
        if (bankType == null) {
            throw new IllegalArgumentException("Bank type cannot be null");
        }

        switch (bankType) {
            case ICBC:
                // 处理工行存款更新逻辑
                updateICBCDepositAmount(loanRepaymentId, assetId);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported bank type: " + bankType);
        }
    }

    /**
     * 工行存款逻辑：扣减资产并同步消费记录。
     *
     * @param loanRepaymentId 还款记录ID
     * @param assetId         资产记录ID
     */
    private void updateICBCDepositAmount(Long loanRepaymentId, Long assetId) {
        // 查找当月还款记录
        LoanRepayments repayment = findLoanRepaymentForCurrentMonth(loanRepaymentId);
        // 验证资产是否足够支付还款
        AssetRecord asset = findAndValidateAsset(assetId, repayment.getTotalPrincipalAndInterest());
        if (asset != null) {
            // 扣减资产余额
            deductAssetBalance(asset, repayment.getTotalPrincipalAndInterest());
            // 同步消费记录
            syncExpenseRecord(asset.getUserId(), repayment);
        }

    }

    /**
     * 获取本月指定日期的还款记录。
     *
     * @param loanRepaymentId 还款记录ID
     * @return 当月还款记录
     */
    private LoanRepayments findLoanRepaymentForCurrentMonth(Long loanRepaymentId) {
        // 获取本月11日的日期
        Date repayDate = DateUtils.getThisMonthEleventhByCalendar(11);
        // 根据日期查找还款记录
        LoanRepayments repayment = loanRepaymentsService.selectLoanRepaymentsByDate(repayDate);

        if (repayment == null) {
            throw new NoSuchElementException("未找到还款记录，ID: " + loanRepaymentId);
        }
        return repayment;
    }

    /**
     * 查询并验证资产余额是否足够。
     *
     * @param assetId     资产ID
     * @param repayAmount 还款金额
     * @return 资产记录
     */
    private AssetRecord findAndValidateAsset(Long assetId, BigDecimal repayAmount) {
        markLoanSynced(assetId);
        if (isLoanSyncedToday(assetId)) {
            return null;
        }
        // 根据ID查找资产记录
        AssetRecord asset = assetRecordService.selectAssetRecordByAssetId(assetId);
        if (asset == null) {
            throw new NoSuchElementException("未找到资产记录，ID: " + assetId);
        }
        // 验证资产余额是否足够
        if (asset.getAmount().compareTo(repayAmount) < 0) {
            throw new IllegalStateException(String.format(
                    "资产余额不足: 当前=%s, 需要=%s", asset.getAmount(), repayAmount));
        }

        // 验证当天是否更新过,更新过，不在更新
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

    /**
     * 扣减资产金额并更新数据库。
     *
     * @param asset  资产记录
     * @param amount 扣减金额
     */
    private void deductAssetBalance(AssetRecord asset, BigDecimal amount) {
        asset.setAmount(asset.getAmount().subtract(amount));
        asset.setUpdateTime(new Date());
        assetRecordService.updateAssetRecord(asset);
        log.debug("【DepositService】已扣减资产余额 | 用户:{} 新余额:{}", asset.getUserId(), asset.getAmount());
    }

    /**
     * 同步生成或更新消费记录。
     *
     * @param userId    用户ID
     * @param repayment 还款记录
     */
    private void syncExpenseRecord(Long userId, LoanRepayments repayment) {
        // 根据用户ID和还款日期查找消费记录
        Expense expense = expenseService.selectExpenseByUserIdAndLoan(userId, repayment.getRepaymentDate());

        if (expense != null) {
            // 如果存在则更新消费金额
            expense.setAmount(repayment.getTotalPrincipalAndInterest());
            expenseService.updateExpense(expense);
            log.debug("【DepositService】更新消费记录 | 用户:{} 日期:{} 金额:{}", userId, repayment.getRepaymentDate(), expense.getAmount());
        } else {
            // 如果不存在则创建新的消费记录
            Expense newExpense = new Expense();
            newExpense.setUserId(userId);
            newExpense.setExpenseDate(repayment.getRepaymentDate());
            newExpense.setAmount(repayment.getTotalPrincipalAndInterest());
            expenseService.insertExpense(newExpense);
            log.debug("【DepositService】插入消费记录 | 用户:{} 日期:{} 金额:{}", userId, repayment.getRepaymentDate(), newExpense.getAmount());
        }
    }

    /**
     * 银行类型枚举，用于扩展多银行逻辑。
     */
    private enum BankType {
        ICBC, CCB, ABC, BOC
    }

}
