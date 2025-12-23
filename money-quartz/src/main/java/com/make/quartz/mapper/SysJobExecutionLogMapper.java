package com.make.quartz.mapper;

import java.util.List;

import com.make.quartz.domain.SysJobExecutionLog;

/**
 * 任务执行历史记录Mapper接口
 *
 * @author erqi
 * @date 2025-12-23
 */
public interface SysJobExecutionLogMapper {

    /**
     * 查询任务执行历史记录
     *
     * @param id 任务执行历史记录主键
     * @return 任务执行历史记录
     */
    public SysJobExecutionLog selectSysJobExecutionLogById(Long id);

    /**
     * 查询任务执行历史记录列表
     *
     * @param sysJobExecutionLog 任务执行历史记录
     * @return 任务执行历史记录集合
     */
    public List<SysJobExecutionLog> selectSysJobExecutionLogList(SysJobExecutionLog sysJobExecutionLog);

    /**
     * 新增任务执行历史记录
     *
     * @param sysJobExecutionLog 任务执行历史记录
     * @return 结果
     */
    public int insertSysJobExecutionLog(SysJobExecutionLog sysJobExecutionLog);

    /**
     * 修改任务执行历史记录
     *
     * @param sysJobExecutionLog 任务执行历史记录
     * @return 结果
     */
    public int updateSysJobExecutionLog(SysJobExecutionLog sysJobExecutionLog);

    /**
     * 删除任务执行历史记录
     *
     * @param id 任务执行历史记录主键
     * @return 结果
     */
    public int deleteSysJobExecutionLogById(Long id);

    /**
     * 批量删除任务执行历史记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysJobExecutionLogByIds(Long[] ids);
}
