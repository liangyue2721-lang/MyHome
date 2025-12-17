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
import com.make.finance.domain.WeddingExpense;
import com.make.finance.service.IWeddingExpenseService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 婚礼支出记录Controller
 *
 * @author erqi
 * @date 2025-12-17
 */
@RestController
@RequestMapping("/finance/weddingExpense")
public class WeddingExpenseController extends BaseController {

    @Autowired
    private IWeddingExpenseService weddingExpenseService;

/**
 * 查询婚礼支出记录列表
 */
@PreAuthorize("@ss.hasPermi('finance:weddingExpense:list')")
@GetMapping("/list")
    public TableDataInfo list(WeddingExpense weddingExpense) {
        startPage();
        List<WeddingExpense> list = weddingExpenseService.selectWeddingExpenseList(weddingExpense);
        return getDataTable(list);
    }

    /**
     * 导出婚礼支出记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:weddingExpense:export')")
    @Log(title = "婚礼支出记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WeddingExpense weddingExpense) {
        List<WeddingExpense> list = weddingExpenseService.selectWeddingExpenseList(weddingExpense);
        ExcelUtil<WeddingExpense> util = new ExcelUtil<WeddingExpense>(WeddingExpense. class);
        util.exportExcel(response, list, "婚礼支出记录数据");
    }

    /**
     * 获取婚礼支出记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:weddingExpense:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(weddingExpenseService.selectWeddingExpenseById(id));
    }

    /**
     * 新增婚礼支出记录
     */
    @PreAuthorize("@ss.hasPermi('finance:weddingExpense:add')")
    @Log(title = "婚礼支出记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WeddingExpense weddingExpense) {
        return toAjax(weddingExpenseService.insertWeddingExpense(weddingExpense));
    }

    /**
     * 修改婚礼支出记录
     */
    @PreAuthorize("@ss.hasPermi('finance:weddingExpense:edit')")
    @Log(title = "婚礼支出记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WeddingExpense weddingExpense) {
        return toAjax(weddingExpenseService.updateWeddingExpense(weddingExpense));
    }

    /**
     * 删除婚礼支出记录
     */
    @PreAuthorize("@ss.hasPermi('finance:weddingExpense:remove')")
    @Log(title = "婚礼支出记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(weddingExpenseService.deleteWeddingExpenseByIds(ids));
    }
}
