package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.TaskAllowedIps;

/**
 * 任务允许IPMapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface TaskAllowedIpsMapper {

    /**
     * 查询任务允许IP
     *
     * @param id 任务允许IP主键
     * @return 任务允许IP
     */
    public TaskAllowedIps selectTaskAllowedIpsById(Integer id);

    /**
     * 查询任务允许IP列表
     *
     * @param taskAllowedIps 任务允许IP
     * @return 任务允许IP集合
     */
    public List<TaskAllowedIps> selectTaskAllowedIpsList(TaskAllowedIps taskAllowedIps);

    /**
     * 新增任务允许IP
     *
     * @param taskAllowedIps 任务允许IP
     * @return 结果
     */
    public int insertTaskAllowedIps(TaskAllowedIps taskAllowedIps);

    /**
     * 修改任务允许IP
     *
     * @param taskAllowedIps 任务允许IP
     * @return 结果
     */
    public int updateTaskAllowedIps(TaskAllowedIps taskAllowedIps);

    /**
     * 删除任务允许IP
     *
     * @param id 任务允许IP主键
     * @return 结果
     */
    public int deleteTaskAllowedIpsById(Integer id);

    /**
     * 批量删除任务允许IP
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTaskAllowedIpsByIds(Integer[] ids);

    /**
     * 根据主机IP地址查询允许执行的任务列表
     *
     * <p>通过IP地址匹配预定义的任务访问策略，返回该IP可执行的任务集合。
     * </p>
     */
    TaskAllowedIps selectTaskAllowedIpsByIp(String hostIp);
}
