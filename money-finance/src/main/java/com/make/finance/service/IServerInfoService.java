package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.ServerInfo;

/**
 * 服务器有效期管理（MySQL5.7兼容版）Service接口
 *
 * @author erqi
 * @date 2025-10-15
 */
public interface IServerInfoService {

    /**
     * 查询服务器有效期管理（MySQL5.7兼容版）
     *
     * @param id 服务器有效期管理（MySQL5.7兼容版）主键
     * @return 服务器有效期管理（MySQL5.7兼容版）
     */
    public ServerInfo selectServerInfoById(Long id);

    /**
     * 查询服务器有效期管理（MySQL5.7兼容版）列表
     *
     * @param serverInfo 服务器有效期管理（MySQL5.7兼容版）
     * @return 服务器有效期管理（MySQL5.7兼容版）集合
     */
    public List<ServerInfo> selectServerInfoList(ServerInfo serverInfo);

    /**
     * 新增服务器有效期管理（MySQL5.7兼容版）
     *
     * @param serverInfo 服务器有效期管理（MySQL5.7兼容版）
     * @return 结果
     */
    public int insertServerInfo(ServerInfo serverInfo);

    /**
     * 修改服务器有效期管理（MySQL5.7兼容版）
     *
     * @param serverInfo 服务器有效期管理（MySQL5.7兼容版）
     * @return 结果
     */
    public int updateServerInfo(ServerInfo serverInfo);

    /**
     * 批量删除服务器有效期管理（MySQL5.7兼容版）
     *
     * @param ids 需要删除的服务器有效期管理（MySQL5.7兼容版）主键集合
     * @return 结果
     */
    public int deleteServerInfoByIds(Long[] ids);

    /**
     * 删除服务器有效期管理（MySQL5.7兼容版）信息
     *
     * @param id 服务器有效期管理（MySQL5.7兼容版）主键
     * @return 结果
     */
    public int deleteServerInfoById(Long id);
}
