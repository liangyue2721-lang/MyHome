package com.make.quartz.service.impl;

import java.util.List;
        import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.quartz.mapper.SysJobExecutionLogMapper;
import com.make.quartz.domain.SysJobExecutionLog;
import com.make.quartz.service.ISysJobExecutionLogService;

/**
 * 任务执行历史记录Service业务层处理
 *
 * @author erqi
 * @date 2025-12-23
 */
@Service
public class SysJobExecutionLogServiceImpl implements ISysJobExecutionLogService {

    @Autowired
    private SysJobExecutionLogMapper sysJobExecutionLogMapper;

    /**
     * 查询任务执行历史记录
     *
     * @param id 任务执行历史记录主键
     * @return 任务执行历史记录
     */
    @Override
    public SysJobExecutionLog selectSysJobExecutionLogById(Long id) {
        return sysJobExecutionLogMapper.selectSysJobExecutionLogById(id);
    }

    /**
     * 查询任务执行历史记录列表
     *
     * @param sysJobExecutionLog 任务执行历史记录
     * @return 任务执行历史记录
     */
    @Override
    public List<SysJobExecutionLog> selectSysJobExecutionLogList(SysJobExecutionLog sysJobExecutionLog) {
        return sysJobExecutionLogMapper.selectSysJobExecutionLogList(sysJobExecutionLog);
    }

    /**
     * 新增任务执行历史记录
     *
     * @param sysJobExecutionLog 任务执行历史记录
     * @return 结果
     */
    @Override
    public int insertSysJobExecutionLog(SysJobExecutionLog sysJobExecutionLog) {
                sysJobExecutionLog.setCreateTime(DateUtils.getNowDate());
            return sysJobExecutionLogMapper.insertSysJobExecutionLog(sysJobExecutionLog);
    }

    /**
     * 修改任务执行历史记录
     *
     * @param sysJobExecutionLog 任务执行历史记录
     * @return 结果
     */
    @Override
    public int updateSysJobExecutionLog(SysJobExecutionLog sysJobExecutionLog) {
        return sysJobExecutionLogMapper.updateSysJobExecutionLog(sysJobExecutionLog);
    }

    /**
     * 批量删除任务执行历史记录
     *
     * @param ids 需要删除的任务执行历史记录主键
     * @return 结果
     */
    @Override
    public int deleteSysJobExecutionLogByIds(Long[] ids) {
        return sysJobExecutionLogMapper.deleteSysJobExecutionLogByIds(ids);
    }

    /**
     * 删除任务执行历史记录信息
     *
     * @param id 任务执行历史记录主键
     * @return 结果
     */
    @Override
    public int deleteSysJobExecutionLogById(Long id) {
        return sysJobExecutionLogMapper.deleteSysJobExecutionLogById(id);
    }
}
