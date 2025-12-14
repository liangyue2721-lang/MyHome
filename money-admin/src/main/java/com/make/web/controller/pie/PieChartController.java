package com.make.web.controller.pie;

import com.alibaba.fastjson2.JSON;
import com.make.finance.domain.vo.LoanTotalRepaymentPieChart;
import com.make.stock.domain.EtfSalesData;
import com.make.finance.domain.vo.LoanTotalWithInterestRepaymentPieChart;
import com.make.finance.domain.vo.MonthlyExpenditureBarChart;
import com.make.stock.domain.SalesData;
import com.make.finance.domain.vo.TotalAmount;
import com.make.stock.service.IEtfSalesDataService;
import com.make.web.service.IPieChartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 首页 饼形图与柱状图控制层
 *
 * @author 84522
 */
@RestController
@RequestMapping("/finance/pieChart")
public class PieChartController {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(PieChartController.class);


    @Resource
    private IPieChartService pieChartService;


    @Resource
    private IEtfSalesDataService etfSalesDataService;

    /**
     * 查询交易类型饼形图数据
     *
     * @param ids 用户ID数组
     * @return 交易类型饼形图数据
     */
    @GetMapping("/transactionTypePieChart/{ids}")
    public List<Map<String, Object>> getTransactionTypePieChart(@PathVariable Long[] ids) {
        return pieChartService.getTransactionTypePieChartData(ids[0]);
    }

    /**
     * 查询交易总额饼形图数据
     *
     * @param ids 用户ID数组
     * @return 交易总额饼形图数据
     */
    @GetMapping("/totalAmountPieChart/{ids}")
    public List<Map<String, Object>> getTotalAmountPieChart(@PathVariable Long[] ids) {
        List<Map<String, Object>> totalAmountPieChartData = pieChartService.getTotalAmountPieChartData(ids[0]);
        JSON.toJSON(totalAmountPieChartData);
        return pieChartService.getTotalAmountPieChartData(ids[0]);
    }

    /**
     * 查询微信支付宝数据
     *
     * @param ids 用户ID数组
     * @return 微信支付宝数据
     */
    @GetMapping("/getWechatAlipayData/{ids}")
    public List<Map<String, Object>> getWechatAlipayData(@PathVariable Long[] ids) {
        List<Map<String, Object>> totalAmountPieChartData = pieChartService.getWechatAlipayData(ids[0]);
        JSON.toJSON(totalAmountPieChartData);
        return pieChartService.getWechatAlipayData(ids[0]);
    }


    /**
     * 查询柱形图数据
     *
     * @param ids 用户ID数组
     * @return 柱形图数据
     */
    @GetMapping("/totalAmountChart/{ids}")
    public List<TotalAmount> getTotalAmountChart(@PathVariable Long[] ids) {
        return pieChartService.getTotalAmountChartData(ids[0]);
    }

    /**
     * 查询收入/支出柱形图数据
     *
     * @param ids 用户ID数组
     * @return 收入/支出柱形图数据
     */
    @GetMapping("/totalMonthlyExpenditureBarChart/{ids}")
    public List<MonthlyExpenditureBarChart> getMonthlyExpenditureBarChart(@PathVariable Long[] ids) {
        return pieChartService.getMonthlyExpenditureBarChart(ids[0]);
    }

    /**
     * 查询贷款总额偿还比例数据
     *
     * @param ids 用户ID数组
     * @return 贷款总额偿还比例数据
     */
    @GetMapping("/repaymentPieChart/{ids}")
    public List<LoanTotalRepaymentPieChart> getRepaymentPieChart(@PathVariable Long[] ids) {
        return pieChartService.getRepaymentPieChart(ids[0]);
    }

    /**
     * 查询贷款总额加利息偿还比例数据
     *
     * @param ids 用户ID数组
     * @return 贷款总额加利息偿还比例数据
     */
    @GetMapping("/totalRepaymentPieChart/{ids}")
    public List<LoanTotalWithInterestRepaymentPieChart> getTotalRepaymentPieChart(@PathVariable Long[] ids) {
        return pieChartService.getTotalRepaymentPieChart(ids[0]);
    }

    /**
     * 查询当月消费收入比
     *
     * @param ids 用户ID数组
     * @return 当月消费收入比数据
     */
    @GetMapping("/getMonthIncomeExpenseRatio/{ids}")
    public List<LoanTotalWithInterestRepaymentPieChart> getMonthIncomeExpenseRatio(@PathVariable Long[] ids) {
        return pieChartService.getMonthIncomeExpenseRatio(ids[0]);
    }

    /**
     * 查询当年消费收入比
     *
     * @param ids 用户ID数组
     * @return 当年消费收入比数据
     */
    @GetMapping("/getYearIncomeExpenseRatio/{ids}")
    public List<LoanTotalWithInterestRepaymentPieChart> getYearIncomeExpenseRatio(@PathVariable Long[] ids) {
        return pieChartService.getYearIncomeExpenseRatio(ids[0]);
    }

    /**
     * 获取利润折线图数据
     *
     * @param ids 用户ID数组
     * @return 利润折线图数据
     */
    @GetMapping("/getProfitLineData/{ids}")
    public List<SalesData> getProfitLineData(@PathVariable Long[] ids) {
        System.out.println(ids.length);
        return pieChartService.getProfitLineData(ids[0]);
    }

    /**
     * 获取多个 ETF 的利润折线图数据，优化查询性能
     *
     * @param ids 用户ID数组
     * @return 包含多个 ETF 销售数据的列表，用于绘制折线图
     */
    @GetMapping("/getETFProfitLineData/{ids}")
    public List<EtfSalesData> getETFProfitLineData(@PathVariable Long[] ids) {
        long startTime = System.currentTimeMillis();
        // 提取所有代码用于批量查询
        List<String> etfCodes = new ArrayList<>();
        etfCodes.add("159934");
        etfCodes.add("159813");
        etfCodes.add("518880");
        // 批量查询销售数据（使用 etfCode 作为条件）
        List<EtfSalesData> rawList = etfSalesDataService.batchQueryByEtfList(etfCodes);
        long endTime = System.currentTimeMillis();
        log.info("执行获取多个 ETF 的利润折线图数据,耗时{}ms", endTime - startTime);
        return rawList;
    }

    /**
     * 查询收入柱形图数据
     *
     * @param ids 用户ID数组
     * @return 收入柱形图数据
     */
    @GetMapping("/getMonthlyIncomeBarChart/{ids}")
    public List<MonthlyExpenditureBarChart> getMonthlyIncomeBarChart(@PathVariable Long[] ids) {
        return pieChartService.getMonthlyIncomeBarChart(ids[0]);
    }

    /**
     * 查询还贷柱形图数据
     *
     * @param ids 用户ID数组
     * @return 还贷柱形图数据
     */
    @GetMapping("/renderLoanRepaymentComparisonChart/{ids}")
    public List<MonthlyExpenditureBarChart> renderLoanRepaymentComparisonChart(@PathVariable Long[] ids) {
        return pieChartService.renderLoanRepaymentComparisonChart(ids[0]);
    }

    /**
     * 获取许可证检查信息
     * 包括服务器信息、许可证有效期及剩余天数等
     *
     * @return 许可证检查信息的JSON字符串
     */
    @GetMapping("/GetLicenseCheck/")
    public String renderLoanRepaymentComparisonChart() {
        return pieChartService.getLicenseCheck();
    }
}
