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
import com.make.finance.domain.InvestmentRecords;
import com.make.finance.service.IInvestmentRecordsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 投资利润回报记录Controller
 *
 * @author erqi
 * @date 2025-07-05
 */
@RestController
@RequestMapping("/finance/investment_records")
public class InvestmentRecordsController extends BaseController {

    @Autowired
    private IInvestmentRecordsService investmentRecordsService;

    /**
     * 查询投资利润回报记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:investment_records:list')")
    @GetMapping("/list")
    public TableDataInfo list(InvestmentRecords investmentRecords) {
        startPage();
        List<InvestmentRecords> list = investmentRecordsService.selectInvestmentRecordsList(investmentRecords);
        return getDataTable(list);
    }

    /**
     * 导出投资利润回报记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:investment_records:export')")
    @Log(title = "投资利润回报记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, InvestmentRecords investmentRecords) {
        List<InvestmentRecords> list = investmentRecordsService.selectInvestmentRecordsList(investmentRecords);
        ExcelUtil<InvestmentRecords> util = new ExcelUtil<InvestmentRecords>(InvestmentRecords.class);
        util.exportExcel(response, list, "投资利润回报记录数据");
    }

    /**
     * 获取投资利润回报记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:investment_records:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(investmentRecordsService.selectInvestmentRecordsById(id));
    }

    /**
     * 新增投资利润回报记录
     */
    @PreAuthorize("@ss.hasPermi('finance:investment_records:add')")
    @Log(title = "投资利润回报记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody InvestmentRecords investmentRecords) {
        return toAjax(investmentRecordsService.insertInvestmentRecords(investmentRecords));
    }

    /**
     * 修改投资利润回报记录
     */
    @PreAuthorize("@ss.hasPermi('finance:investment_records:edit')")
    @Log(title = "投资利润回报记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody InvestmentRecords investmentRecords) {
        return toAjax(investmentRecordsService.updateInvestmentRecords(investmentRecords));
    }

    /**
     * 删除投资利润回报记录
     */
    @PreAuthorize("@ss.hasPermi('finance:investment_records:remove')")
    @Log(title = "投资利润回报记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(investmentRecordsService.deleteInvestmentRecordsByIds(ids));
    }
}
