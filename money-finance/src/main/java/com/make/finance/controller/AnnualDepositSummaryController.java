package com.make.finance.controller;

import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.make.common.utils.SecurityUtils;
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
import com.make.finance.domain.AnnualDepositSummary;
import com.make.finance.service.IAnnualDepositSummaryService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 年度存款统计Controller
 *
 * @author erqi
 * @date 2025-07-20
 */
@RestController
@RequestMapping("/finance/annual_deposit_summary")
public class AnnualDepositSummaryController extends BaseController {

    @Autowired
    private IAnnualDepositSummaryService annualDepositSummaryService;

    /**
     * 查询年度存款统计列表
     */
    @PreAuthorize("@ss.hasPermi('finance:annual_deposit_summary:list')")
    @GetMapping("/list")
    public TableDataInfo list(AnnualDepositSummary annualDepositSummary) {
        startPage();
        List<AnnualDepositSummary> list = annualDepositSummaryService.selectAnnualDepositSummaryList(annualDepositSummary);
        return getDataTable(list);
    }

    /**
     * Get current user's annual deposit summary for the current year.
     */
    @GetMapping("/current-user-summary")
    public AjaxResult getCurrentUserSummary() {
        Long userId = SecurityUtils.getUserId();
        int currentYear = LocalDate.now().getYear();
        return success(annualDepositSummaryService.queryAnnualDepositSummaryByYearAndUser(currentYear, userId));
    }

    /**
     * 导出年度存款统计列表
     */
    @PreAuthorize("@ss.hasPermi('finance:annual_deposit_summary:export')")
    @Log(title = "年度存款统计", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AnnualDepositSummary annualDepositSummary) {
        List<AnnualDepositSummary> list = annualDepositSummaryService.selectAnnualDepositSummaryList(annualDepositSummary);
        ExcelUtil<AnnualDepositSummary> util = new ExcelUtil<AnnualDepositSummary>(AnnualDepositSummary.class);
        util.exportExcel(response, list, "年度存款统计数据");
    }

    /**
     * 获取年度存款统计详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:annual_deposit_summary:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(annualDepositSummaryService.selectAnnualDepositSummaryById(id));
    }

    /**
     * 新增年度存款统计
     */
    @PreAuthorize("@ss.hasPermi('finance:annual_deposit_summary:add')")
    @Log(title = "年度存款统计", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AnnualDepositSummary annualDepositSummary) {
        return toAjax(annualDepositSummaryService.insertAnnualDepositSummary(annualDepositSummary));
    }

    /**
     * 修改年度存款统计
     */
    @PreAuthorize("@ss.hasPermi('finance:annual_deposit_summary:edit')")
    @Log(title = "年度存款统计", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AnnualDepositSummary annualDepositSummary) {
        return toAjax(annualDepositSummaryService.updateAnnualDepositSummary(annualDepositSummary));
    }

    /**
     * 删除年度存款统计
     */
    @PreAuthorize("@ss.hasPermi('finance:annual_deposit_summary:remove')")
    @Log(title = "年度存款统计", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(annualDepositSummaryService.deleteAnnualDepositSummaryByIds(ids));
    }
}
