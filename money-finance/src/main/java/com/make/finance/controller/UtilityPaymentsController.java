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
import com.make.finance.domain.UtilityPayments;
import com.make.finance.service.IUtilityPaymentsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 用户水电费缴纳记录Controller
 *
 * @author 贰柒
 * @date 2025-05-27
 */
@RestController
@RequestMapping("/finance/payments")
public class UtilityPaymentsController extends BaseController {
    @Autowired
    private IUtilityPaymentsService utilityPaymentsService;

    /**
     * 查询用户水电费缴纳记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:payments:list')")
    @GetMapping("/list")
    public TableDataInfo list(UtilityPayments utilityPayments) {
        startPage();
        List<UtilityPayments> list = utilityPaymentsService.selectUtilityPaymentsList(utilityPayments);
        return getDataTable(list);
    }

    /**
     * 导出用户水电费缴纳记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:payments:export')")
    @Log(title = "用户水电费缴纳记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UtilityPayments utilityPayments) {
        List<UtilityPayments> list = utilityPaymentsService.selectUtilityPaymentsList(utilityPayments);
        ExcelUtil<UtilityPayments> util = new ExcelUtil<UtilityPayments>(UtilityPayments.class);
        util.exportExcel(response, list, "用户水电费缴纳记录数据");
    }

    /**
     * 获取用户水电费缴纳记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:payments:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Integer id) {
        return success(utilityPaymentsService.selectUtilityPaymentsById(id));
    }

    /**
     * 新增用户水电费缴纳记录
     */
    @PreAuthorize("@ss.hasPermi('finance:payments:add')")
    @Log(title = "用户水电费缴纳记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UtilityPayments utilityPayments) {
        return toAjax(utilityPaymentsService.insertUtilityPayments(utilityPayments));
    }

    /**
     * 修改用户水电费缴纳记录
     */
    @PreAuthorize("@ss.hasPermi('finance:payments:edit')")
    @Log(title = "用户水电费缴纳记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UtilityPayments utilityPayments) {
        return toAjax(utilityPaymentsService.updateUtilityPayments(utilityPayments));
    }

    /**
     * 删除用户水电费缴纳记录
     */
    @PreAuthorize("@ss.hasPermi('finance:payments:remove')")
    @Log(title = "用户水电费缴纳记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Integer[] ids) {
        return toAjax(utilityPaymentsService.deleteUtilityPaymentsByIds(ids));
    }

    @Log(title = "查询用户水电费当年记录")
    @GetMapping("/query")
    public TableDataInfo query(UtilityPayments utilityPayments) {
        List<UtilityPayments> list = utilityPaymentsService.queryUtilityPaymentsList(utilityPayments);
        return getDataTable(list);
    }
}
