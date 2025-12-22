package com.make.quartz.service;

import com.make.quartz.domain.SysJob;

import java.util.List;

/**
 * 定时任务服务接口（Redis-only 调度模型）
 *
 * <p>说明：
 * - 接口层仍然保留“任务管理”的语义（CRUD / 启停 / 校验）
 * - 不再暴露 Quartz Scheduler / Trigger 概念
 * - 所有“调度行为”在实现层通过 Redis 队列完成
 */
public interface ISysJobService {

    /**
     * 查询任务列表
     *
     * @param sysJob 查询条件
     * @return 任务列表
     */
    List<SysJob> selectJobList(SysJob sysJob);

    /**
     * 查询任务详情
     *
     * @param jobId 任务ID
     * @return 任务
     */
    SysJob selectJobById(Long jobId);

    /**
     * 查询全部任务
     *
     * @return 全部任务
     */
    List<SysJob> selectJobAll();

    /**
     * 新增任务
     *
     * <p>行为：
     * - 写入数据库
     * - 若状态为启用（status=0），计算下一次执行时间并入 Redis 延迟队列
     */
    int insertJob(SysJob job);

    /**
     * 更新任务
     *
     * <p>行为：
     * - 更新数据库
     * - 若状态为启用（status=0），重新计算下一次执行时间并入 Redis
     */
    int updateJob(SysJob job);

    /**
     * 删除任务
     *
     * <p>说明：
     * - 仅删除数据库记录
     * - Redis 中的历史/待执行消息允许自然过期（幂等）
     */
    int deleteJob(SysJob job);

    /**
     * 修改任务状态
     *
     * <p>status=0：启用 → 计算下一次执行并入 Redis
     * <br>status!=0：暂停 → 不再续入队
     */
    int changeStatus(SysJob job);

    /**
     * 校验 Cron 表达式是否合法
     *
     * @param cronExpression cron 表达式
     * @return true-合法；false-非法
     */
    boolean checkCronExpressionIsValid(String cronExpression);
}
