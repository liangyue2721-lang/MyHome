package com.make.stock.service;

import java.util.List;

import com.make.stock.domain .TaskAllowedIps;

/**
 * 任务允许IPService接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface ITaskAllowedIpsService {

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
     * 批量删除任务允许IP
     *
     * @param ids 需要删除的任务允许IP主键集合
     * @return 结果
     */
    public int deleteTaskAllowedIpsByIds(Integer[] ids);

    /**
     * 删除任务允许IP信息
     *
     * @param id 任务允许IP主键
     * @return 结果
     */
    public int deleteTaskAllowedIpsById(Integer id);

    /**
     * 检测指定IP地址的主机可达性状态
     *
     * <p>通过执行ICMP Echo请求（Ping）或TCP端口探测，判断目标主机是否处于可连接状态。

     */
    boolean isHostActive(String hostIp);


}
