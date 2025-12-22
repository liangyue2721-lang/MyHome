package com.make.quartz.service.impl;

import java.util.List;
        import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.quartz.mapper.SysJobRuntimeMapper;
import com.make.quartz.domain.SysJobRuntime;
import com.make.quartz.service.ISysJobRuntimeService;

/**
 * 实时任务（待执行 / 执行中）Service业务层处理
 *
 * @author erqi
 * @date 2025-12-22
 */
@Service
public class SysJobRuntimeServiceImpl implements ISysJobRuntimeService {

    @Autowired
    private SysJobRuntimeMapper sysJobRuntimeMapper;

    /**
     * 查询实时任务（待执行 / 执行中）
     *
     * @param id 实时任务（待执行 / 执行中）主键
     * @return 实时任务（待执行 / 执行中）
     */
    @Override
    public SysJobRuntime selectSysJobRuntimeById(Long id) {
        return sysJobRuntimeMapper.selectSysJobRuntimeById(id);
    }

    /**
     * 查询实时任务（待执行 / 执行中）列表
     *
     * @param sysJobRuntime 实时任务（待执行 / 执行中）
     * @return 实时任务（待执行 / 执行中）
     */
    @Override
    public List<SysJobRuntime> selectSysJobRuntimeList(SysJobRuntime sysJobRuntime) {
        return sysJobRuntimeMapper.selectSysJobRuntimeList(sysJobRuntime);
    }

    /**
     * 新增实时任务（待执行 / 执行中）
     *
     * @param sysJobRuntime 实时任务（待执行 / 执行中）
     * @return 结果
     */
    @Override
    public int insertSysJobRuntime(SysJobRuntime sysJobRuntime) {
                sysJobRuntime.setCreateTime(DateUtils.getNowDate());
            return sysJobRuntimeMapper.insertSysJobRuntime(sysJobRuntime);
    }

    /**
     * 修改实时任务（待执行 / 执行中）
     *
     * @param sysJobRuntime 实时任务（待执行 / 执行中）
     * @return 结果
     */
    @Override
    public int updateSysJobRuntime(SysJobRuntime sysJobRuntime) {
                sysJobRuntime.setUpdateTime(DateUtils.getNowDate());
        return sysJobRuntimeMapper.updateSysJobRuntime(sysJobRuntime);
    }

    /**
     * 批量删除实时任务（待执行 / 执行中）
     *
     * @param ids 需要删除的实时任务（待执行 / 执行中）主键
     * @return 结果
     */
    @Override
    public int deleteSysJobRuntimeByIds(Long[] ids) {
        return sysJobRuntimeMapper.deleteSysJobRuntimeByIds(ids);
    }

    /**
     * 删除实时任务（待执行 / 执行中）信息
     *
     * @param id 实时任务（待执行 / 执行中）主键
     * @return 结果
     */
    @Override
    public int deleteSysJobRuntimeById(Long id) {
        return sysJobRuntimeMapper.deleteSysJobRuntimeById(id);
    }
}
