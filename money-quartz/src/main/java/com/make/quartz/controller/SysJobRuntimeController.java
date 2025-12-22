package com.make.quartz.controller;

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
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.service.ISysJobRuntimeService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 实时任务（待执行 / 执行中）Controller
 *
 * @author erqi
 * @date 2025-12-22
 */
@RestController
@RequestMapping("/quartz/runtime")
public class SysJobRuntimeController extends BaseController {

    @Autowired
    private ISysJobRuntimeService sysJobRuntimeService;

/**
 * 查询实时任务（待执行 / 执行中）列表
 */
@PreAuthorize("@ss.hasPermi('quartz:runtime:list')")
@GetMapping("/list")
    public TableDataInfo list(SysJobRuntime sysJobRuntime) {
        startPage();
        List<SysJobRuntime> list = sysJobRuntimeService.selectSysJobRuntimeList(sysJobRuntime);
        return getDataTable(list);
    }

    /**
     * 导出实时任务（待执行 / 执行中）列表
     */
    @PreAuthorize("@ss.hasPermi('quartz:runtime:export')")
    @Log(title = "实时任务（待执行 / 执行中）", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJobRuntime sysJobRuntime) {
        List<SysJobRuntime> list = sysJobRuntimeService.selectSysJobRuntimeList(sysJobRuntime);
        ExcelUtil<SysJobRuntime> util = new ExcelUtil<SysJobRuntime>(SysJobRuntime. class);
        util.exportExcel(response, list, "实时任务（待执行 / 执行中）数据");
    }

    /**
     * 获取实时任务（待执行 / 执行中）详细信息
     */
    @PreAuthorize("@ss.hasPermi('quartz:runtime:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(sysJobRuntimeService.selectSysJobRuntimeById(id));
    }

    /**
     * 新增实时任务（待执行 / 执行中）
     */
    @PreAuthorize("@ss.hasPermi('quartz:runtime:add')")
    @Log(title = "实时任务（待执行 / 执行中）", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysJobRuntime sysJobRuntime) {
        return toAjax(sysJobRuntimeService.insertSysJobRuntime(sysJobRuntime));
    }

    /**
     * 修改实时任务（待执行 / 执行中）
     */
    @PreAuthorize("@ss.hasPermi('quartz:runtime:edit')")
    @Log(title = "实时任务（待执行 / 执行中）", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysJobRuntime sysJobRuntime) {
        return toAjax(sysJobRuntimeService.updateSysJobRuntime(sysJobRuntime));
    }

    /**
     * 删除实时任务（待执行 / 执行中）
     */
    @PreAuthorize("@ss.hasPermi('quartz:runtime:remove')")
    @Log(title = "实时任务（待执行 / 执行中）", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sysJobRuntimeService.deleteSysJobRuntimeByIds(ids));
    }
}
