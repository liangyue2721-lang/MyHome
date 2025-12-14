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
import com.make.finance.domain.HouseExpense;
import com.make.finance.service.IHouseExpenseService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 买房支出记录Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/house_expense")
public class HouseExpenseController extends BaseController {
    @Autowired
    private IHouseExpenseService houseExpenseService;

    /**
     * 查询买房支出记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:house_expense:list')")
    @GetMapping("/list")
    public TableDataInfo list(HouseExpense houseExpense) {
        startPage();
        List<HouseExpense> list = houseExpenseService.selectHouseExpenseList(houseExpense);
        return getDataTable(list);
    }

    /**
     * 导出买房支出记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:house_expense:export')")
    @Log(title = "买房支出记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HouseExpense houseExpense) {
        List<HouseExpense> list = houseExpenseService.selectHouseExpenseList(houseExpense);
        ExcelUtil<HouseExpense> util = new ExcelUtil<HouseExpense>(HouseExpense.class);
        util.exportExcel(response, list, "买房支出记录数据");
    }

    /**
     * 获取买房支出记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:house_expense:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(houseExpenseService.selectHouseExpenseById(id));
    }

    /**
     * 新增买房支出记录
     */
    @PreAuthorize("@ss.hasPermi('finance:house_expense:add')")
    @Log(title = "买房支出记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HouseExpense houseExpense) {
        return toAjax(houseExpenseService.insertHouseExpense(houseExpense));
    }

    /**
     * 修改买房支出记录
     */
    @PreAuthorize("@ss.hasPermi('finance:house_expense:edit')")
    @Log(title = "买房支出记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HouseExpense houseExpense) {
        return toAjax(houseExpenseService.updateHouseExpense(houseExpense));
    }

    /**
     * 删除买房支出记录
     */
    @PreAuthorize("@ss.hasPermi('finance:house_expense:remove')")
    @Log(title = "买房支出记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(houseExpenseService.deleteHouseExpenseByIds(ids));
    }
}
