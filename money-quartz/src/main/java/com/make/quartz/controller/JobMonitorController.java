package com.make.quartz.controller;

import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.service.ISysJobExecutionLogService;
import com.make.quartz.service.ISysJobRuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务监控控制器
 *
 * @author erqi
 * @date 2025-12-22
 */
@RestController
@RequestMapping("/monitor/job")
public class JobMonitorController extends BaseController {

    @Autowired
    private ISysJobRuntimeService sysJobRuntimeService;

    @Autowired
    private ISysJobExecutionLogService sysJobExecutionLogService;

    /**
     * 获取任务状态概览
     *
     * @return 任务状态统计
     */
    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/status-summary")
    public AjaxResult getStatusSummary() {
        int pending = sysJobRuntimeService.countByStatus("WAITING");
        int executing = sysJobRuntimeService.countByStatus("RUNNING");
        int completed = sysJobExecutionLogService.countAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", pending);
        stats.put("executing", executing);
        stats.put("completed", completed);

        return success(stats);
    }

    /**
     * 获取任务队列详情（用于服务器监控页）
     *
     * @return 活跃任务列表
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/queue/details")
    public AjaxResult getQueueDetails() {
        List<SysJobRuntime> list = sysJobRuntimeService.selectActiveJobs();
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null) {
            for (SysJobRuntime job : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("queueName", job.getJobGroup());
                map.put("taskId", job.getJobId() + "_" + job.getJobName());
                map.put("targetNode", job.getNodeId());
                map.put("priority", "NORMAL"); // Default priority
                map.put("status", job.getStatus());
                map.put("traceId", job.getExecutionId());
                map.put("enqueueTime", job.getEnqueueTime());
                map.put("completionTime", null);
                map.put("retryCount", job.getRetryCount());
                result.add(map);
            }
        }
        return success(result);
    }
}
