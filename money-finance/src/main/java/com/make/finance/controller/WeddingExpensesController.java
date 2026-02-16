package com.make.finance.controller;

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
import com.make.finance.domain.WeddingExpenses;
import com.make.finance.service.IWeddingExpensesService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 婚礼订婚支出流水Controller
 *
 * @author erqi
 * @date 2026-02-16
 */
@RestController
@RequestMapping("/finance/expenses")
public class WeddingExpensesController extends BaseController {

    @Autowired
    private IWeddingExpensesService weddingExpensesService;

    /**
     * 查询婚礼订婚支出流水列表
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:list')")
    @GetMapping("/list")
    public TableDataInfo list(WeddingExpenses weddingExpenses) {
        startPage();
        weddingExpenses.setUserId(SecurityUtils.getUserId());
        List<WeddingExpenses> list = weddingExpensesService.selectWeddingExpensesList(weddingExpenses);
        return getDataTable(list);
    }

    /**
     * 查询婚礼订婚支出流水统计
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:list')")
    @GetMapping("/stats")
    public AjaxResult stats(WeddingExpenses weddingExpenses) {
        weddingExpenses.setUserId(SecurityUtils.getUserId());
        return AjaxResult.success(weddingExpensesService.selectWeddingExpensesStats(weddingExpenses));
    }

    /**
     * 导出婚礼订婚支出流水列表
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:export')")
    @Log(title = "婚礼订婚支出流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WeddingExpenses weddingExpenses) {
        List<WeddingExpenses> list = weddingExpensesService.selectWeddingExpensesList(weddingExpenses);
        ExcelUtil<WeddingExpenses> util = new ExcelUtil<WeddingExpenses>(WeddingExpenses.class);
        util.exportExcel(response, list, "婚礼订婚支出流水数据");
    }

    /**
     * 获取婚礼订婚支出流水详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(weddingExpensesService.selectWeddingExpensesById(id));
    }

    /**
     * 新增婚礼订婚支出流水
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:add')")
    @Log(title = "婚礼订婚支出流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WeddingExpenses weddingExpenses) {
        weddingExpenses.setUserId(SecurityUtils.getUserId());
        return toAjax(weddingExpensesService.insertWeddingExpenses(weddingExpenses));
    }

    /**
     * 修改婚礼订婚支出流水
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:edit')")
    @Log(title = "婚礼订婚支出流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WeddingExpenses weddingExpenses) {
        weddingExpenses.setUserId(SecurityUtils.getUserId());
        return toAjax(weddingExpensesService.updateWeddingExpenses(weddingExpenses));
    }

    /**
     * 删除婚礼订婚支出流水
     */
    @PreAuthorize("@ss.hasPermi('finance:expenses:remove')")
    @Log(title = "婚礼订婚支出流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(weddingExpensesService.deleteWeddingExpensesByIds(ids));
    }
}
