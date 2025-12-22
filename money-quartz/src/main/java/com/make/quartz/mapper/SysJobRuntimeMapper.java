package com.make.quartz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.make.quartz.domain.SysJobRuntime;

/**
 * 实时任务（待执行 / 执行中）Mapper接口
 *
 * @author erqi
 * @date 2025-12-22
 */
public interface SysJobRuntimeMapper {

    /**
     * 查询实时任务（待执行 / 执行中）
     *
     * @param id 实时任务（待执行 / 执行中）主键
     * @return 实时任务（待执行 / 执行中）
     */
    public SysJobRuntime selectSysJobRuntimeById(Long id);

    /**
     * 查询实时任务（待执行 / 执行中）列表
     *
     * @param sysJobRuntime 实时任务（待执行 / 执行中）
     * @return 实时任务（待执行 / 执行中）集合
     */
    public List<SysJobRuntime> selectSysJobRuntimeList(SysJobRuntime sysJobRuntime);

    /**
     * 新增实时任务（待执行 / 执行中）
     *
     * @param sysJobRuntime 实时任务（待执行 / 执行中）
     * @return 结果
     */
    public int insertSysJobRuntime(SysJobRuntime sysJobRuntime);

    /**
     * 修改实时任务（待执行 / 执行中）
     *
     * @param sysJobRuntime 实时任务（待执行 / 执行中）
     * @return 结果
     */
    public int updateSysJobRuntime(SysJobRuntime sysJobRuntime);

    /**
     * 删除实时任务（待执行 / 执行中）
     *
     * @param id 实时任务（待执行 / 执行中）主键
     * @return 结果
     */
    public int deleteSysJobRuntimeById(Long id);

    /**
     * 批量删除实时任务（待执行 / 执行中）
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysJobRuntimeByIds(Long[] ids);

    /**
     * 统计指定任务正在运行或等待的数量
     *
     * @param jobId 任务ID
     * @return 数量
     */
    int countRunningOrWaiting(@Param("jobId") Long jobId);

    /**
     * 尝试抢占任务（状态 WAITING -> RUNNING）
     *
     * @param executionId 执行ID
     * @param nodeId 当前节点ID
     * @return 影响行数（1表示抢占成功）
     */
    int updateStatusToRunning(@Param("executionId") String executionId, @Param("nodeId") String nodeId);

    /**
     * 根据 executionId 删除运行时任务
     *
     * @param executionId 执行ID
     * @return 影响行数
     */
    int deleteByExecutionId(@Param("executionId") String executionId);

    /**
     * 根据 executionId 查询运行时任务
     *
     * @param executionId 执行ID
     * @return 运行时任务
     */
    SysJobRuntime selectSysJobRuntimeByExecutionId(@Param("executionId") String executionId);
}
