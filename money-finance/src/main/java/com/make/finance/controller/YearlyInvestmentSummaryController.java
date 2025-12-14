package com.make.finance.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.make.common.annotation.Log;
import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.common.enums.BusinessType;
import com.make.finance.domain.YearlyInvestmentSummary;
import com.make.finance.service.IYearlyInvestmentSummaryService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 年度投资汇总Controller
 *
 * @author erqi
 * @date 2025-07-07
 */
@RestController
@RequestMapping("/finance/yearly_investment_summary")
public class YearlyInvestmentSummaryController extends BaseController {

    @Autowired
    private IYearlyInvestmentSummaryService yearlyInvestmentSummaryService;

    /**
     * 查询年度投资汇总列表
     */
    @PreAuthorize("@ss.hasPermi('finance:yearly_investment_summary:list')")
    @GetMapping("/list")
    public TableDataInfo list(YearlyInvestmentSummary yearlyInvestmentSummary) {
        startPage();
        boolean equals = yearlyInvestmentSummary.getUserId().equals(1L);
        if (equals) {
            yearlyInvestmentSummary.setUserId(null);
        }
        List<YearlyInvestmentSummary> list = yearlyInvestmentSummaryService.selectYearlyInvestmentSummaryList(yearlyInvestmentSummary);
        return getDataTable(list);
    }

    /**
     * 导出年度投资汇总列表
     */
    @PreAuthorize("@ss.hasPermi('finance:yearly_investment_summary:export')")
    @Log(title = "年度投资汇总", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, YearlyInvestmentSummary yearlyInvestmentSummary) {
        List<YearlyInvestmentSummary> list = yearlyInvestmentSummaryService.selectYearlyInvestmentSummaryList(yearlyInvestmentSummary);
        ExcelUtil<YearlyInvestmentSummary> util = new ExcelUtil<YearlyInvestmentSummary>(YearlyInvestmentSummary.class);
        util.exportExcel(response, list, "年度投资汇总数据");
    }

    /**
     * 获取年度投资汇总详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:yearly_investment_summary:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(yearlyInvestmentSummaryService.selectYearlyInvestmentSummaryById(id));
    }

    /**
     * 新增年度投资汇总
     */
    @PreAuthorize("@ss.hasPermi('finance:yearly_investment_summary:add')")
    @Log(title = "年度投资汇总", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody YearlyInvestmentSummary yearlyInvestmentSummary) {
        return toAjax(yearlyInvestmentSummaryService.insertYearlyInvestmentSummary(yearlyInvestmentSummary));
    }

    /**
     * 修改年度投资汇总
     */
    @PreAuthorize("@ss.hasPermi('finance:yearly_investment_summary:edit')")
    @Log(title = "年度投资汇总", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody YearlyInvestmentSummary yearlyInvestmentSummary) {
        return toAjax(yearlyInvestmentSummaryService.updateYearlyInvestmentSummary(yearlyInvestmentSummary));
    }

    /**
     * 删除年度投资汇总
     */
    @PreAuthorize("@ss.hasPermi('finance:yearly_investment_summary:remove')")
    @Log(title = "年度投资汇总", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(yearlyInvestmentSummaryService.deleteYearlyInvestmentSummaryByIds(ids));
    }
}
