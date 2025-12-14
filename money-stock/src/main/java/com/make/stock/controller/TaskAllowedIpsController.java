package com.make.stock.controller;

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
import com.make.stock.domain.TaskAllowedIps;
import com.make.stock.service.ITaskAllowedIpsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 任务允许IPController
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/task_allowed_ips")
public class TaskAllowedIpsController extends BaseController {

    @Autowired
    private ITaskAllowedIpsService taskAllowedIpsService;

    /**
     * 查询任务允许IP列表
     */
    @PreAuthorize("@ss.hasPermi('stock:task_allowed_ips:list')")
    @GetMapping("/list")
    public TableDataInfo list(TaskAllowedIps taskAllowedIps) {
        startPage();
        List<TaskAllowedIps> list = taskAllowedIpsService.selectTaskAllowedIpsList(taskAllowedIps);
        return getDataTable(list);
    }

    /**
     * 导出任务允许IP列表
     */
    @PreAuthorize("@ss.hasPermi('stock:task_allowed_ips:export')")
    @Log(title = "任务允许IP", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaskAllowedIps taskAllowedIps) {
        List<TaskAllowedIps> list = taskAllowedIpsService.selectTaskAllowedIpsList(taskAllowedIps);
        ExcelUtil<TaskAllowedIps> util = new ExcelUtil<TaskAllowedIps>(TaskAllowedIps.class);
        util.exportExcel(response, list, "任务允许IP数据");
    }

    /**
     * 获取任务允许IP详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:task_allowed_ips:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Integer id) {
        return success(taskAllowedIpsService.selectTaskAllowedIpsById(id));
    }

    /**
     * 新增任务允许IP
     */
    @PreAuthorize("@ss.hasPermi('stock:task_allowed_ips:add')")
    @Log(title = "任务允许IP", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TaskAllowedIps taskAllowedIps) {
        return toAjax(taskAllowedIpsService.insertTaskAllowedIps(taskAllowedIps));
    }

    /**
     * 修改任务允许IP
     */
    @PreAuthorize("@ss.hasPermi('stock:task_allowed_ips:edit')")
    @Log(title = "任务允许IP", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TaskAllowedIps taskAllowedIps) {
        return toAjax(taskAllowedIpsService.updateTaskAllowedIps(taskAllowedIps));
    }

    /**
     * 删除任务允许IP
     */
    @PreAuthorize("@ss.hasPermi('stock:task_allowed_ips:remove')")
    @Log(title = "任务允许IP", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Integer[] ids) {
        return toAjax(taskAllowedIpsService.deleteTaskAllowedIpsByIds(ids));
    }
}
