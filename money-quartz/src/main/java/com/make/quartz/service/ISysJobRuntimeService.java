package com.make.quartz.service;

import java.util.List;

import com.make.quartz.domain.SysJobRuntime;

/**
 * 实时任务（待执行 / 执行中）Service接口
 *
 * @author erqi
 * @date 2025-12-22
 */
public interface ISysJobRuntimeService {

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
     * 批量删除实时任务（待执行 / 执行中）
     *
     * @param ids 需要删除的实时任务（待执行 / 执行中）主键集合
     * @return 结果
     */
    public int deleteSysJobRuntimeByIds(Long[] ids);

    /**
     * 删除实时任务（待执行 / 执行中）信息
     *
     * @param id 实时任务（待执行 / 执行中）主键
     * @return 结果
     */
    public int deleteSysJobRuntimeById(Long id);

    public List<SysJobRuntime> selectActiveJobs();
}
