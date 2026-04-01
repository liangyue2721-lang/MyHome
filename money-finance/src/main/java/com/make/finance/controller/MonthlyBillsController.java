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
import com.make.finance.domain.MonthlyBills;
import com.make.finance.service.IMonthlyBillsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 月度账单 (单JSON架构)Controller
 *
 * @author erqi
 * @date 2026-04-01
 */
@RestController
@RequestMapping("/finance/bills")
public class MonthlyBillsController extends BaseController {

    @Autowired
    private IMonthlyBillsService monthlyBillsService;

    /**
     * 查询月度账单 (单JSON架构)列表
     */
    @PreAuthorize("@ss.hasPermi('finance:bills:list')")
    @GetMapping("/list")
    public TableDataInfo list(MonthlyBills monthlyBills) {
        startPage();
        monthlyBills.setUserId(SecurityUtils.getUserId());
        List<MonthlyBills> list = monthlyBillsService.selectMonthlyBillsList(monthlyBills);
        return getDataTable(list);
    }

    /**
     * 导出月度账单 (单JSON架构)列表
     */
    @PreAuthorize("@ss.hasPermi('finance:bills:export')")
    @Log(title = "月度账单 (单JSON架构)", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MonthlyBills monthlyBills) {
        monthlyBills.setUserId(SecurityUtils.getUserId());
        List<MonthlyBills> list = monthlyBillsService.selectMonthlyBillsList(monthlyBills);
        ExcelUtil<MonthlyBills> util = new ExcelUtil<MonthlyBills>(MonthlyBills.class);
        util.exportExcel(response, list, "月度账单 (单JSON架构)数据");
    }

    /**
     * 获取月度账单 (单JSON架构)详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:bills:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(monthlyBillsService.selectMonthlyBillsById(id));
    }

    /**
     * 新增月度账单 (单JSON架构)
     */
    @PreAuthorize("@ss.hasPermi('finance:bills:add')")
    @Log(title = "月度账单 (单JSON架构)", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MonthlyBills monthlyBills) {
        monthlyBills.setUserId(SecurityUtils.getUserId());
        return toAjax(monthlyBillsService.insertMonthlyBills(monthlyBills));
    }

    /**
     * 修改月度账单 (单JSON架构)
     */
    @PreAuthorize("@ss.hasPermi('finance:bills:edit')")
    @Log(title = "月度账单 (单JSON架构)", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MonthlyBills monthlyBills) {
        monthlyBills.setUserId(SecurityUtils.getUserId());
        return toAjax(monthlyBillsService.updateMonthlyBills(monthlyBills));
    }

    /**
     * 删除月度账单 (单JSON架构)
     */
    @PreAuthorize("@ss.hasPermi('finance:bills:remove')")
    @Log(title = "月度账单 (单JSON架构)", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(monthlyBillsService.deleteMonthlyBillsByIds(ids));
    }
}
