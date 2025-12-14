package com.make.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.make.common.utils.DateUtils;
import com.make.finance.domain.vo.LoanRepaymentsChart;
import com.make.finance.utils.LoanCalculatorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.LoanRepaymentsMapper;
import com.make.finance.domain.LoanRepayments;
import com.make.finance.service.ILoanRepaymentsService;

/**
 * 贷款剩余计算Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class LoanRepaymentsServiceImpl implements ILoanRepaymentsService {
    @Autowired
    private LoanRepaymentsMapper loanRepaymentsMapper;

    /**
     * 查询贷款剩余计算
     *
     * @param id 贷款剩余计算主键
     * @return 贷款剩余计算
     */
    @Override
    public LoanRepayments selectLoanRepaymentsById(Long id) {
        return loanRepaymentsMapper.selectLoanRepaymentsById(id);
    }


    /**
     * 查询贷款还款记录列表，并进行自定义排序：
     * 1. 未结清（isSettled=0）的记录排在前面；
     * 2. 已结清（isSettled=1）的排在后面；
     * 3. 同一状态下按还款日期（repayDate）升序排序。
     */
    @Override
    public List<LoanRepayments> selectLoanRepaymentsList(LoanRepayments loanRepayments) {
        return loanRepaymentsMapper.selectLoanRepaymentsList(loanRepayments);
    }

    /**
     * 新增贷款剩余计算
     *
     * @param loanRepayments 贷款剩余计算
     * @return 结果
     */
    @Override
    public int insertLoanRepayments(LoanRepayments loanRepayments) {
        loanRepayments.setCreateTime(DateUtils.getNowDate());
        return loanRepaymentsMapper.insertLoanRepayments(loanRepayments);
    }

    /**
     * 修改贷款剩余计算
     *
     * @param loanRepayments 贷款剩余计算
     * @return 结果
     */
    @Override
    public int updateLoanRepayments(LoanRepayments loanRepayments) {
        loanRepayments.setUpdateTime(DateUtils.getNowDate());
        return loanRepaymentsMapper.updateLoanRepayments(loanRepayments);
    }

    /**
     * 批量删除贷款剩余计算
     *
     * @param ids 需要删除的贷款剩余计算主键
     * @return 结果
     */
    @Override
    public int deleteLoanRepaymentsByIds(Long[] ids) {
        return loanRepaymentsMapper.deleteLoanRepaymentsByIds(ids);
    }

    /**
     * 删除贷款剩余计算信息
     *
     * @param id 贷款剩余计算主键
     * @return 结果
     */
    @Override
    public int deleteLoanRepaymentsById(Long id) {
        return loanRepaymentsMapper.deleteLoanRepaymentsById(id);
    }


    @Override
    public void resetRate() {
        try {
            List<LoanRepayments> loanRepayments = loanRepaymentsMapper.selectUnpaidLoansList();
            BigDecimal totalSum = loanRepayments.get(0).getTotalAmount().add(BigDecimal.valueOf(2750));
            BigDecimal floatingInterestRate = loanRepayments.get(0).getFloatingInterestRate();
            // 将百分比转换为小数
            BigDecimal rate = floatingInterestRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            List<BigDecimal> bigDecimals = LoanCalculatorUtil.calculateMonthlyPayment(totalSum, loanRepayments.size(), rate);
            for (int i = 0; i < loanRepayments.size(); i++) {
                LoanRepayments loanRepayment = loanRepayments.get(i);
//                loanRepayment.setTotalPrincipalAndInterest(bigDecimals.get(i));
                loanRepayment.setInterest(bigDecimals.get(i));
                loanRepaymentsMapper.updateLoanRepayments(loanRepayment);
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    @Override
    public int updateLoanRepaymentsById(LoanRepayments loanRepaymentsObj) {

        return loanRepaymentsMapper.updateLoanRepaymentsById(loanRepaymentsObj);
    }

    @Override
    public LoanRepayments selectLoanRepaymentsByDate(Date date) {
        return loanRepaymentsMapper.selectLoanRepaymentsByDate(date);
    }


    /**
     * 查询并生成贷款还款统计图表数据，按年份汇总每年还款总额。
     *
     * @return List<LoanRepaymentsChart> 每条记录代表一个年份的还款总额，使用该年1月1日作为日期表示。
     */
    @Override
    public List<LoanRepaymentsChart> queryLoanRepaymentsChartList(Long userId) {
        // 从数据库获取所有还款记录
        LoanRepayments loanRepayment = new LoanRepayments();
        loanRepayment.setUserId(userId);
        List<LoanRepayments> loanRepayments = loanRepaymentsMapper.selectLoanRepaymentsList(loanRepayment);

        // 1. 使用 Stream 对记录按年份分组，并累加每年 totalPrincipalAndInterest
        Map<Integer, BigDecimal> sumByYear = loanRepayments.stream()
                // 过滤掉 repaymentDate 或 totalPrincipalAndInterest 为 null 的记录
                .filter(lp -> lp.getRepaymentDate() != null && lp.getTotalPrincipalAndInterest() != null)
                .collect(Collectors.groupingBy(
                        lp -> {
                            // 将 java.util.Date 转换为 Calendar 来获取年份
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(lp.getRepaymentDate());
                            return cal.get(Calendar.YEAR);
                        },
                        // 对每组的 totalPrincipalAndInterest 值求和
                        Collectors.reducing(BigDecimal.ZERO, LoanRepayments::getTotalPrincipalAndInterest, BigDecimal::add)
                ));

        // 2. 将按年份累加后的结果转换为 LoanRepaymentsChart 对象列表
        return sumByYear.entrySet().stream()
                .map(e -> {
                    LoanRepaymentsChart c = new LoanRepaymentsChart();
                    // 构造该年份代表日期（使用该年1月1日）
                    Calendar cal = Calendar.getInstance();
                    cal.clear(); // 清空所有字段
                    cal.set(Calendar.YEAR, e.getKey());
                    c.setRepaymentDate(cal.getTime());
                    // 设置累计的还款本金与利息总额
                    c.setTotalPrincipalAndInterest(e.getValue());
                    return c;
                })
                // 按年份（即 repaymentDate）升序排序
                .sorted(Comparator.comparing(LoanRepaymentsChart::getRepaymentDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanRepayments> selectLoanRepaymentsChart(LoanRepayments query) {
        return loanRepaymentsMapper.selectLoanRepaymentsChart(query);
    }

}

