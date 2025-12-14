package com.make.web.service;

import com.make.finance.domain.vo.LoanTotalRepaymentPieChart;
import com.make.finance.domain.vo.LoanTotalWithInterestRepaymentPieChart;
import com.make.finance.domain.vo.MonthlyExpenditureBarChart;
import com.make.finance.domain.vo.TotalAmount;
import com.make.stock.domain.SalesData;

import java.util.List;
import java.util.Map;

/**
 * 首页 饼形图与柱状图接口
 *
 * @author 84522
 */
public interface IPieChartService {

    /**
     * 获取交易类型饼图数据。
     *
     * @param id 用户ID
     * @return 包含交易类型饼图数据的列表
     */
    List<Map<String, Object>> getTransactionTypePieChartData(Long id);

    /**
     * 获取总金额饼图数据。
     *
     * @param id 用户ID
     * @return 包含总金额饼图数据的列表
     */
    List<Map<String, Object>> getTotalAmountPieChartData(Long id);

    /**
     * 获取总金额数据。
     *
     * @param id 用户ID
     * @return 包含总金额数据的列表
     */
    List<TotalAmount> getTotalAmountChartData(Long id);

    /**
     * 获取月度支出柱状图数据。
     *
     * @param id 用户ID
     * @return 包含月度支出柱状图数据的列表
     */
    List<MonthlyExpenditureBarChart> getMonthlyExpenditureBarChart(Long id);

    /**
     * 获取贷款总额偿还饼形图数据列表。
     *
     * @param id 用户ID
     * @return 包含贷款总额偿还饼形图数据项的列表。
     */
    List<LoanTotalRepaymentPieChart> getRepaymentPieChart(Long id);

    /**
     * 获取贷款总额加利息偿还饼形图数据列表。
     *
     * @param id 用户ID
     * @return 包含贷款总额加利息偿还饼形图数据项的列表。
     */
    List<LoanTotalWithInterestRepaymentPieChart> getTotalRepaymentPieChart(Long id);

    /**
     * 获取交易类型柱状图数据。
     *
     * @param id 用户ID
     * @return 包含交易类型饼图数据的列表
     */
    List<Map<String, Object>> getWechatAlipayData(Long id);

    /**
     * 获取当月消费收入比。
     *
     * @param id 用户ID
     * @return 包含交易类型饼图数据的列表
     */
    List<LoanTotalWithInterestRepaymentPieChart> getMonthIncomeExpenseRatio(Long id);

    /**
     * 获取当年消费收入比。
     *
     * @param id 用户ID
     * @return 包含年度收支数据的列表
     */
    List<LoanTotalWithInterestRepaymentPieChart> getYearIncomeExpenseRatio(Long id);

    /**
     * 获取利润折线图数据
     *
     * @param id 用户ID
     * @return 包含利润数据的列表
     */
    List<SalesData> getProfitLineData(Long id);

    /**
     * 获取每月收入柱状图数据
     *
     * @param id 用户ID
     * @return 包含每月收入数据的列表
     */
    List<MonthlyExpenditureBarChart> getMonthlyIncomeBarChart(Long id);

    /**
     * 渲染贷款还款比较图表
     *
     * @param id 用户ID
     * @return 包含贷款还款数据的列表
     */
    List<MonthlyExpenditureBarChart> renderLoanRepaymentComparisonChart(Long id);

    /**
     * 获取许可证检查结果。
     *
     * @return 许可证检查结果字符串
     */
    String getLicenseCheck();

}