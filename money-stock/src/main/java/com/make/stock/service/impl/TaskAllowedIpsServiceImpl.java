package com.make.stock.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.TaskAllowedIpsMapper;
import com.make.stock.domain.TaskAllowedIps;
import com.make.stock.service.ITaskAllowedIpsService;

/**
 * 任务允许IPService业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class TaskAllowedIpsServiceImpl implements ITaskAllowedIpsService {

    @Autowired
    private TaskAllowedIpsMapper taskAllowedIpsMapper;

    /**
     * 查询任务允许IP
     *
     * @param id 任务允许IP主键
     * @return 任务允许IP
     */
    @Override
    public TaskAllowedIps selectTaskAllowedIpsById(Integer id) {
        return taskAllowedIpsMapper.selectTaskAllowedIpsById(id);
    }

    /**
     * 查询任务允许IP列表
     *
     * @param taskAllowedIps 任务允许IP
     * @return 任务允许IP
     */
    @Override
    public List<TaskAllowedIps> selectTaskAllowedIpsList(TaskAllowedIps taskAllowedIps) {
        return taskAllowedIpsMapper.selectTaskAllowedIpsList(taskAllowedIps);
    }

    /**
     * 新增任务允许IP
     *
     * @param taskAllowedIps 任务允许IP
     * @return 结果
     */
    @Override
    public int insertTaskAllowedIps(TaskAllowedIps taskAllowedIps) {
            return taskAllowedIpsMapper.insertTaskAllowedIps(taskAllowedIps);
    }

    /**
     * 修改任务允许IP
     *
     * @param taskAllowedIps 任务允许IP
     * @return 结果
     */
    @Override
    public int updateTaskAllowedIps(TaskAllowedIps taskAllowedIps) {
        return taskAllowedIpsMapper.updateTaskAllowedIps(taskAllowedIps);
    }

    /**
     * 批量删除任务允许IP
     *
     * @param ids 需要删除的任务允许IP主键
     * @return 结果
     */
    @Override
    public int deleteTaskAllowedIpsByIds(Integer[] ids) {
        return taskAllowedIpsMapper.deleteTaskAllowedIpsByIds(ids);
    }

    /**
     * 删除任务允许IP信息
     *
     * @param id 任务允许IP主键
     * @return 结果
     */
    @Override
    public int deleteTaskAllowedIpsById(Integer id) {
        return taskAllowedIpsMapper.deleteTaskAllowedIpsById(id);
    }

    @Override
    public boolean isHostActive(String hostIp) {
        TaskAllowedIps taskAllowedIps = taskAllowedIpsMapper.selectTaskAllowedIpsByIp(hostIp);
        if (null != taskAllowedIps) {
            return true;
        }
        return false;
    }

}
