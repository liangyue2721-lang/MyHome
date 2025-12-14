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
import com.make.finance.domain.Expense;
import com.make.finance.service.IExpenseService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 消费Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/expense")
public class ExpenseController extends BaseController {
    @Autowired
    private IExpenseService expenseService;

    /**
     * 查询消费列表
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:list')")
    @GetMapping("/list")
    public TableDataInfo list(Expense expense) {
        startPage();
        List<Expense> list = expenseService.selectExpenseList(expense);
        return getDataTable(list);
    }

    /**
     * 导出消费列表
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:export')")
    @Log(title = "消费", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Expense expense) {
        List<Expense> list = expenseService.selectExpenseList(expense);
        ExcelUtil<Expense> util = new ExcelUtil<Expense>(Expense.class);
        util.exportExcel(response, list, "消费数据");
    }

    /**
     * 获取消费详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:query')")
    @GetMapping(value = "/{expenseId}")
    public AjaxResult getInfo(@PathVariable("expenseId") Long expenseId) {
        return success(expenseService.selectExpenseByExpenseId(expenseId));
    }

    /**
     * 新增消费
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:add')")
    @Log(title = "消费", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Expense expense) {
        return toAjax(expenseService.insertExpense(expense));
    }

    /**
     * 修改消费
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:edit')")
    @Log(title = "消费", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Expense expense) {
        return toAjax(expenseService.updateExpense(expense));
    }

    /**
     * 删除消费
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:remove')")
    @Log(title = "消费", businessType = BusinessType.DELETE)
    @DeleteMapping("/{expenseIds}")
    public AjaxResult remove(@PathVariable Long[] expenseIds) {
        return toAjax(expenseService.deleteExpenseByExpenseIds(expenseIds));
    }

    /**
     * 查询消费列表
     */
    @PreAuthorize("@ss.hasPermi('finance:expense:syncExpense')")
    @GetMapping("/syncExpense")
    public TableDataInfo syncExpense(Expense expense) {
        expenseService.syncExpense();
        startPage();
        List<Expense> list = expenseService.selectExpenseList(expense);
        return getDataTable(list);
    }
}
