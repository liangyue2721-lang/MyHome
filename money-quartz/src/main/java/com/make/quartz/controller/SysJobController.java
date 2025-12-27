package com.make.quartz.controller;

import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.common.core.page.TableDataInfo;
import com.make.common.utils.StringUtils;
import com.make.quartz.domain.SysJob;
import com.make.quartz.service.ISysJobService;
import com.make.quartz.util.TaskDistributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务调度控制器（Redis-only 调度模型）
 *
 * <p>说明：
 * - 对外接口与原 Quartz 版本保持一致
 * - 内部实现已切换为 Redis 队列调度
 * - 不再直接操作 Quartz Scheduler
 */
@RestController
@RequestMapping("/monitor/job")
public class SysJobController extends BaseController {

    @Autowired
    private ISysJobService jobService;

    @Autowired
    private TaskDistributor taskDistributor;

    /**
     * 查询定时任务列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysJob sysJob) {
        startPage();
        List<SysJob> list = jobService.selectJobList(sysJob);
        return getDataTable(list);
    }

    /**
     * 查询定时任务详细
     */
    @GetMapping(value = "/{jobId}")
    public AjaxResult getInfo(@PathVariable("jobId") Long jobId) {
        return success(jobService.selectJobById(jobId));
    }

    /**
     * 新增定时任务
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysJob job) {
        // cron 校验（保持原有功能）
        if (!jobService.checkCronExpressionIsValid(job.getCronExpression())) {
            return error("新增任务失败，Cron 表达式不正确");
        }

        int rows = jobService.insertJob(job);
        return rows > 0 ? success() : error("新增任务失败");
    }

    /**
     * 修改定时任务
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysJob job) {
        if (!jobService.checkCronExpressionIsValid(job.getCronExpression())) {
            return error("修改任务失败，Cron 表达式不正确");
        }

        int rows = jobService.updateJob(job);
        return rows > 0 ? success() : error("修改任务失败");
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/{jobIds}")
    public AjaxResult remove(@PathVariable Long[] jobIds) {
        int rows = 0;
        for (Long jobId : jobIds) {
            SysJob job = new SysJob();
            job.setJobId(jobId);
            rows += jobService.deleteJob(job);
        }
        return rows > 0 ? success() : error("删除任务失败");
    }

    /**
     * 修改任务状态（启用 / 暂停）
     *
     * <p>语义保持不变：
     * - status=0：启用 → Redis 入队下一次
     * - status!=0：暂停 → 不再续入队
     */
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysJob job) {
        int rows = jobService.changeStatus(job);
        return rows > 0 ? success() : error("状态修改失败");
    }

    /**
     * 立即执行一次任务
     *
     * <p>原 Quartz 行为：scheduler.triggerJob
     * <br>现 Redis 行为：立即 enqueue 到 Redis 队列
     */
    @PutMapping("/run")
    public AjaxResult run(@RequestBody SysJob job) {
        if (job == null || job.getJobId() == null) {
            return error("任务ID不能为空");
        }

        SysJob dbJob = jobService.selectJobById(job.getJobId());
        if (dbJob == null) {
            return error("任务不存在");
        }

        // 立即执行：通过 Redis 队列投递
        // 使用 scheduleJob(job, 0) 来立即执行
        taskDistributor.scheduleJob(dbJob, 0);

        return success("任务已提交执行");
    }
}
