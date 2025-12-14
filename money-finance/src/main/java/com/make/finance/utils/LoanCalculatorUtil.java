package com.make.finance.utils;

import com.make.finance.domain.LoanRepayments;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 贷款计算器工具类
 * <p>
 * 提供多种贷款计算方法，包括等额本金、等额本息等还款方式的计算，
 * 以及基于LPR利率的月供计算等功能
 * </p>
 *
 * @author 贰柒
 * @date 2024-11-14
 */
public class LoanCalculatorUtil {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(LoanCalculatorUtil.class);

    /**
     * 计算等额本金还款法每月应还本金和利息
     * <p>
     * 等额本金还款法是指每月偿还同等数额的本金和剩余本金在当月所产生的利息，
     * 随着本金逐月减少，每月的还款额也逐渐减少
     * </p>
     *
     * @param totalAmount          贷款总额
     * @param floatingInterestRate 年利率（百分比）
     * @param term                 贷款期限（月）
     * @return 每月还款详情的 LoanRepayments 列表
     */
    public static List<LoanRepayments> calculateEqualPrincipal(BigDecimal totalAmount, BigDecimal floatingInterestRate, Long term) {
        // 创建用于存储每月还款详情的列表
        List<LoanRepayments> repayments = new ArrayList<>();

        // 将年利率从百分比转换为小数形式
        BigDecimal annualInterestRate = floatingInterestRate.divide(BigDecimal.valueOf(100));

        // 计算月利率
        BigDecimal monthRate = annualInterestRate.divide(BigDecimal.valueOf(12), 4, BigDecimal.ROUND_HALF_UP);

        // 计算每月本金支付额
        BigDecimal monthPrincipal = totalAmount.divide(BigDecimal.valueOf(term), 2, BigDecimal.ROUND_HALF_UP);

        // 初始化剩余金额为贷款总额
        BigDecimal remainingAmount = totalAmount;
        // 循环计算每月还款详情
        for (int i = 1; i <= term; i++) {
            // 计算当月利息（剩余金额 × 月利率）
            BigDecimal interest = remainingAmount.multiply(monthRate).setScale(2, BigDecimal.ROUND_HALF_UP);

            // 设定固定的每月本金支付额
            BigDecimal principal = monthPrincipal;

            // 计算总月还款额（本金 + 分息）
            BigDecimal totalPayment = principal.add(interest);

            // 更新剩余金额（减去本月本金支付额）
            remainingAmount = remainingAmount.subtract(principal);

            // 创建当月还款对象
            LoanRepayments repayment = new LoanRepayments(remainingAmount, term, LocalDate.now().plusMonths(i),
                    principal, interest, totalPayment, floatingInterestRate);

            // 将还款详情添加到列表中
            repayments.add(repayment);

        }

        // 返回每月还款详情列表
        return repayments;
    }


    /**
     * 计算等额本息方式的房贷
     * <p>
     * 等额本息还款法是指每月以相等的额度偿还贷款本息，
     * 其中本金所占比例逐月递增，利息所占比例逐月递减
     * </p>
     *
     * @param totalLoanAmount      贷款总额
     * @param floatingInterestRate 浮动利率
     * @param loanTermInMonths     贷款期数（月）
     * @return 包含每月还款信息的List对象
     */
    public static List<LoanRepayments> calculateInterestAndPrincipal(BigDecimal totalLoanAmount, BigDecimal floatingInterestRate, int loanTermInMonths) {
        // 计算月利率
//        BigDecimal monthlyInterestRate = floatingInterestRate.divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal monthlyInterestRate = floatingInterestRate.divide(BigDecimal.valueOf(12), 4, BigDecimal.ROUND_HALF_UP);
        // 计算月利率+1
        BigDecimal monthlyRatePlusOne = monthlyInterestRate.add(BigDecimal.ONE);
        // 计算每月应还款额
        BigDecimal monthlyPayment = totalLoanAmount.multiply(monthlyInterestRate)
                .multiply(monthlyRatePlusOne.pow(loanTermInMonths))
                .divide(monthlyRatePlusOne.pow(loanTermInMonths).subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);

        // 创建用于存储每月还款信息的列表
        List<LoanRepayments> resultList = new ArrayList<>();

        // 初始化还款日期为当前日期
        LocalDate paymentDate = LocalDate.now();

        // 循环计算每月还款详情
        for (int i = 1; i <= loanTermInMonths; i++) {
            // 计算利息（贷款总额 × 月利率）
            BigDecimal interest = totalLoanAmount.multiply(monthlyInterestRate);
            // 计算本金（每月还款额 - 利息）
            BigDecimal principal = monthlyPayment.subtract(interest);
            // 更新剩余总额（贷款总额 - 本金）
            totalLoanAmount = totalLoanAmount.subtract(principal);
            // 更新还款日期（当前日期加一个月）
            paymentDate = paymentDate.plusMonths(1);

            // 创建每月还款信息对象
            LoanRepayments info = new LoanRepayments(totalLoanAmount, (long) i, paymentDate, principal, interest, monthlyPayment, floatingInterestRate);
            // 将还款信息添加到列表中
            resultList.add(info);


        }

        // 返回每月还款信息列表
        return resultList;
    }


    /**
     * 计算按月等额本金的每月还款额
     * <p>
     * 等额本金还款法是指每月偿还同等数额的本金和剩余本金在当月所产生的利息，
     * 随着本金逐月减少，每月的还款额也逐渐减少
     * </p>
     *
     * @param principal  贷款本金
     * @param months     贷款期月数
     * @param annualRate 年利率（例如5%表示为0.05）
     * @return 每月利息列表
     */
    public static List<BigDecimal> calculateMonthlyPayment(BigDecimal principal, int months, BigDecimal annualRate) {
        // 创建用于存储每月利息的列表
        List<BigDecimal> list = new ArrayList<>();
        try {
            // 计算月利率
            BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
            // 计算每月应还本金
            BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), 4, RoundingMode.HALF_UP);
            // 初始化剩余本金为贷款本金
            BigDecimal remainingPrincipal = principal;


            log.info("贷款本金: {}", principal);
            log.info("贷款期月数: {}", months);
            log.info("年利率: {}%", annualRate.multiply(BigDecimal.valueOf(100)));
            log.info("月利率: {}%", monthlyRate.multiply(BigDecimal.valueOf(100)));
            log.info("----------------------------------");

            // 循环计算每月还款额
            for (int i = 1; i <= months; i++) {
                // 计算每月还款额 = 每月应还本金 + (剩余本金 × 月利率)
                BigDecimal monthlyPayment = monthlyPrincipal.add(remainingPrincipal.multiply(monthlyRate)).setScale(2, RoundingMode.HALF_UP);
                // 更新剩余本金（剩余本金 - 每月应还本金）
                remainingPrincipal = remainingPrincipal.subtract(monthlyPrincipal).setScale(2, RoundingMode.HALF_UP);
                // 将每月利息添加到列表中
                list.add(remainingPrincipal.multiply(monthlyRate));
                log.info("第 {} 月还款额: {}", i, monthlyPayment);
            }
        } catch (Exception e) {
            log.error("计算每月还款额异常", e);
        }

        // 返回每月利息列表
        return list;
    }


    /**
     * 计算基于LPR和基点的调整后月供
     *
     * @param remainingPrincipal 剩余本金总和（单位：元，必须大于0）
     * @param latestLPR          最新LPR值（年利率，例如0.042表示4.2%）
     * @param basisPoints        基点（BP，如-20表示减20BP，+50表示加50BP）
     * @param remainingMonths    剩余还款月数（必须大于0）
     * @param repaymentMethod    还款方式（1:等额本息, 2:等额本金）
     * @return 调整后的月供金额（单位：元，四舍五入保留两位小数）
     * @throws IllegalArgumentException 参数非法时抛出异常
     *                                  <p>
     *                                  使用示例：
     *                                  BigDecimal monthlyPayment = calculateLPRBasedPayment(
     *                                  new BigDecimal("1000000.00"),
     *                                  new BigDecimal("0.042"),  // 最新LPR 4.2%
     *                                  new BigDecimal("-20"),     // 基点-20BP（实际利率4.0%）
     *                                  360,
     *                                  1
     *                                  );
     */
    public static BigDecimal calculateLPRBasedPayment(
            BigDecimal remainingPrincipal,
            BigDecimal latestLPR,
            BigDecimal basisPoints,
            int remainingMonths,
            int repaymentMethod) {

        // 参数校验
        validateParameters(remainingPrincipal, latestLPR, basisPoints, remainingMonths);

        // 计算实际利率 = LPR + 基点（基点需转为小数，如-20BP = -0.002）
        BigDecimal actualInterestRate = latestLPR.add(
                basisPoints.divide(BigDecimal.valueOf(10000), 10, RoundingMode.HALF_UP)
        );

        // 转换为月利率
        BigDecimal monthlyRate = actualInterestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        // 根据还款方式计算月供
        switch (repaymentMethod) {
            case 1: // 等额本息
                // 计算等额本息月供
                return calculateEqualInstallment(remainingPrincipal, monthlyRate, remainingMonths);
            case 2: // 等额本金（首月月供）
                // 计算等额本金首月月供
                return calculateEqualPrincipalFirstMonth(remainingPrincipal, monthlyRate, remainingMonths);
            default:
                // 抛出非法参数异常
                throw new IllegalArgumentException("无效的还款方式，可选值：1（等额本息）, 2（等额本金）");
        }
    }

    /**
     * 等额本息计算
     * <p>
     * 等额本息还款法是指每月以相等的额度偿还贷款本息，
     * 其中本金所占比例逐月递增，利息所占比例逐月递减
     * </p>
     *
     * @param principal  贷款本金
     * @param monthlyRate 月利率
     * @param months      还款月数
     * @return 每月还款额
     */
    private static BigDecimal calculateEqualInstallment(
            BigDecimal principal,
            BigDecimal monthlyRate,
            int months) {

        // 处理零利率情况
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) { // 处理零利率
            // 零利率时每月还款额 = 贷款本金 / 还款月数
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        // 公式：(principal * monthlyRate * (1 + monthlyRate)^months) / ((1 + monthlyRate)^months - 1)
        BigDecimal temp = BigDecimal.ONE.add(monthlyRate).pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(temp);
        BigDecimal denominator = temp.subtract(BigDecimal.ONE);

        // 计算每月还款额
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * 等额本金计算（返回首月月供）
     * <p>
     * 等额本金还款法是指每月偿还同等数额的本金和剩余本金在当月所产生的利息，
     * 首月月供最高，之后逐月递减
     * </p>
     *
     * @param principal  贷款本金
     * @param monthlyRate 月利率
     * @param months      还款月数
     * @return 首月月供
     */
    private static BigDecimal calculateEqualPrincipalFirstMonth(
            BigDecimal principal,
            BigDecimal monthlyRate,
            int months) {

        // 计算每月应还本金
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), 10, RoundingMode.HALF_UP);
        // 计算首月利息
        BigDecimal firstMonthInterest = principal.multiply(monthlyRate);

        // 计算首月月供（每月应还本金 + 首月利息）
        return monthlyPrincipal.add(firstMonthInterest).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 参数合法性校验
     * <p>
     * 对贷款计算所需的参数进行合法性校验，确保参数在合理范围内
     * </p>
     *
     * @param principal   贷款本金
     * @param latestLPR   最新LPR值
     * @param basisPoints 基点
     * @param months      还款月数
     */
    private static void validateParameters(
            BigDecimal principal,
            BigDecimal latestLPR,
            BigDecimal basisPoints,
            int months) {

        // 校验剩余本金
        if (principal.compareTo(BigDecimal.ZERO) <= 0) {
            // 如果剩余本金小于等于0，抛出非法参数异常
            throw new IllegalArgumentException("剩余本金必须大于零");
        }

        // 校验LPR范围（通常LPR在0~10%之间）
        if (latestLPR.compareTo(BigDecimal.ZERO) < 0 || latestLPR.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            // 如果LPR值不在合理范围内，抛出非法参数异常
            throw new IllegalArgumentException("LPR值应在0~0.1（0%~10%）之间");
        }

        // 校验基点范围（通常基点调整在-100BP~+200BP之间）
        if (basisPoints.compareTo(BigDecimal.valueOf(-100)) < 0 ||
                basisPoints.compareTo(BigDecimal.valueOf(200)) > 0) {
            // 如果基点值不在合理范围内，抛出非法参数异常
            throw new IllegalArgumentException("基点值应在-100~+200 BP之间");
        }

        // 校验剩余月数
        if (months <= 0) {
            // 如果剩余月数小于等于0，抛出非法参数异常
            throw new IllegalArgumentException("剩余月数必须大于零");
        }
    }

    /**
     * 计算消费收入比
     * <p>
     * 消费收入比是指消费支出占收入的比例，用于衡量个人或家庭的消费水平和储蓄能力
     * </p>
     *
     * @param income      收入
     * @param consumption 消费支出
     * @return 消费收入比
     */
    public static double calculateConsumptionIncomeRatio(double income, double consumption) {
        // 防止除数为0，确保消费支出小于收入
        if (income == 0 || consumption >= income) {
            // 如果收入为0或消费支出大于等于收入，抛出非法参数异常
            throw new IllegalArgumentException("收入必须大于0且消费支出必须小于收入");
        }

        // 计算消费收入比
        double ratio = consumption / income;

        // 返回结果
        return ratio;
    }

    // 测试用例
    public static void main(String[] args) {
        // 示例：贷款100万，LPR 4.2%，基点-20BP（实际利率4.0%），30年等额本息
        BigDecimal payment = calculateLPRBasedPayment(
                new BigDecimal("627000.00"),
                new BigDecimal("0.031"),
                new BigDecimal("-0.318"),
                227,
                2
        );
        System.out.println("调整后月供：" + payment + "元");  // 应输出4774.15元
    }
}