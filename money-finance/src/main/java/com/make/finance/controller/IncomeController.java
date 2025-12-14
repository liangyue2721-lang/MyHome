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
import com.make.finance.domain.Income;
import com.make.finance.service.IIncomeService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 收入Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/income")
public class IncomeController extends BaseController {
    @Autowired
    private IIncomeService incomeService;

    /**
     * 查询收入列表
     */
    @PreAuthorize("@ss.hasPermi('finance:income:list')")
    @GetMapping("/list")
    public TableDataInfo list(Income income) {
        startPage();
        List<Income> list = incomeService.selectIncomeList(income);
        return getDataTable(list);
    }

    /**
     * 导出收入列表
     */
    @PreAuthorize("@ss.hasPermi('finance:income:export')")
    @Log(title = "收入", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Income income) {
        List<Income> list = incomeService.selectIncomeList(income);
        ExcelUtil<Income> util = new ExcelUtil<Income>(Income.class);
        util.exportExcel(response, list, "收入数据");
    }

    /**
     * 获取收入详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:income:query')")
    @GetMapping(value = "/{incomeId}")
    public AjaxResult getInfo(@PathVariable("incomeId") Long incomeId) {
        return success(incomeService.selectIncomeByIncomeId(incomeId));
    }

    /**
     * 新增收入
     */
    @PreAuthorize("@ss.hasPermi('finance:income:add')")
    @Log(title = "收入", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Income income) {
        return toAjax(incomeService.insertIncome(income));
    }

    /**
     * 修改收入
     */
    @PreAuthorize("@ss.hasPermi('finance:income:edit')")
    @Log(title = "收入", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Income income) {
        return toAjax(incomeService.updateIncome(income));
    }

    /**
     * 删除收入
     */
    @PreAuthorize("@ss.hasPermi('finance:income:remove')")
    @Log(title = "收入", businessType = BusinessType.DELETE)
    @DeleteMapping("/{incomeIds}")
    public AjaxResult remove(@PathVariable Long[] incomeIds) {
        return toAjax(incomeService.deleteIncomeByIncomeIds(incomeIds));
    }

//    /**
//     * 获取收入来源
//     */
//    @GetMapping("/getSourceOptions")
//    public AjaxResult getSourceOptions() {
//        return new AjaxResult(200, "成功", incomeService.getSourceOptions());
//    }

}
