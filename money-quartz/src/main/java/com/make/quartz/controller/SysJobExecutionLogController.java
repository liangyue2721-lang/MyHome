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
import com.make.quartz.domain.SysJobExecutionLog;
import com.make.quartz.service.ISysJobExecutionLogService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 任务执行历史记录Controller
 *
 * @author erqi
 * @date 2025-12-22
 */
@RestController
@RequestMapping("/quartz/log")
public class SysJobExecutionLogController extends BaseController {

    @Autowired
    private ISysJobExecutionLogService sysJobExecutionLogService;

/**
 * 查询任务执行历史记录列表
 */
@PreAuthorize("@ss.hasPermi('quartz:log:list')")
@GetMapping("/list")
    public TableDataInfo list(SysJobExecutionLog sysJobExecutionLog) {
        startPage();
        List<SysJobExecutionLog> list = sysJobExecutionLogService.selectSysJobExecutionLogList(sysJobExecutionLog);
        return getDataTable(list);
    }

    /**
     * 导出任务执行历史记录列表
     */
    @PreAuthorize("@ss.hasPermi('quartz:log:export')")
    @Log(title = "任务执行历史记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJobExecutionLog sysJobExecutionLog) {
        List<SysJobExecutionLog> list = sysJobExecutionLogService.selectSysJobExecutionLogList(sysJobExecutionLog);
        ExcelUtil<SysJobExecutionLog> util = new ExcelUtil<SysJobExecutionLog>(SysJobExecutionLog. class);
        util.exportExcel(response, list, "任务执行历史记录数据");
    }

    /**
     * 获取任务执行历史记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('quartz:log:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(sysJobExecutionLogService.selectSysJobExecutionLogById(id));
    }

    /**
     * 新增任务执行历史记录
     */
    @PreAuthorize("@ss.hasPermi('quartz:log:add')")
    @Log(title = "任务执行历史记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysJobExecutionLog sysJobExecutionLog) {
        return toAjax(sysJobExecutionLogService.insertSysJobExecutionLog(sysJobExecutionLog));
    }

    /**
     * 修改任务执行历史记录
     */
    @PreAuthorize("@ss.hasPermi('quartz:log:edit')")
    @Log(title = "任务执行历史记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysJobExecutionLog sysJobExecutionLog) {
        return toAjax(sysJobExecutionLogService.updateSysJobExecutionLog(sysJobExecutionLog));
    }

    /**
     * 删除任务执行历史记录
     */
    @PreAuthorize("@ss.hasPermi('quartz:log:remove')")
    @Log(title = "任务执行历史记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sysJobExecutionLogService.deleteSysJobExecutionLogByIds(ids));
    }
}
