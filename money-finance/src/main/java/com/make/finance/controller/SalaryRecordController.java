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
import com.make.finance.domain.SalaryRecord;
import com.make.finance.service.ISalaryRecordService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 员工工资明细Controller
 *
 * @author erqi
 * @date 2025-05-29
 */
@RestController
@RequestMapping("/finance/salary_record")
public class SalaryRecordController extends BaseController {

    @Autowired
    private ISalaryRecordService salaryRecordService;

    /**
     * 查询员工工资明细列表
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:list')")
    @GetMapping("/list")
    public TableDataInfo list(SalaryRecord salaryRecord) {
        startPage();
        salaryRecord.setUserId(SecurityUtils.getUserId());
        List<SalaryRecord> list = salaryRecordService.selectSalaryRecordList(salaryRecord);
        return getDataTable(list);
    }

    /**
     * 导出员工工资明细列表
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:export')")
    @Log(title = "员工工资明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SalaryRecord salaryRecord) {
        salaryRecord.setUserId(SecurityUtils.getUserId());
        List<SalaryRecord> list = salaryRecordService.selectSalaryRecordList(salaryRecord);
        ExcelUtil<SalaryRecord> util = new ExcelUtil<SalaryRecord>(SalaryRecord.class);
        util.exportExcel(response, list, "员工工资明细数据");
    }

    /**
     * 获取员工工资明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(salaryRecordService.selectSalaryRecordById(id));
    }

    /**
     * 新增员工工资明细
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:add')")
    @Log(title = "员工工资明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SalaryRecord salaryRecord) {
        salaryRecord.setUserId(SecurityUtils.getUserId());
        return toAjax(salaryRecordService.insertSalaryRecord(salaryRecord));
    }

    /**
     * 修改员工工资明细
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:edit')")
    @Log(title = "员工工资明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SalaryRecord salaryRecord) {
        salaryRecord.setUserId(SecurityUtils.getUserId());
        return toAjax(salaryRecordService.updateSalaryRecord(salaryRecord));
    }

    /**
     * 删除员工工资明细
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:remove')")
    @Log(title = "员工工资明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(salaryRecordService.deleteSalaryRecordByIds(ids));
    }

    /**
     * 复制单条员工工资明细
     */
    @PreAuthorize("@ss.hasPermi('finance:salary_record:copy')")
    @Log(title = "员工工资明细", businessType = BusinessType.INSERT)
    @PostMapping("/{ids}")
    public AjaxResult copy(@PathVariable Long[] ids) {
        return toAjax(salaryRecordService.copySalaryRecordById(ids[0]));
    }
}
