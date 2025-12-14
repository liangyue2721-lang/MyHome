package com.make.finance.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.ServerInfoMapper;
import com.make.finance.domain.ServerInfo;
import com.make.finance.service.IServerInfoService;

/**
 * 服务器有效期管理（MySQL5.7兼容版）Service业务层处理
 *
 * @author erqi
 * @date 2025-10-15
 */
@Service
public class ServerInfoServiceImpl implements IServerInfoService {

    @Autowired
    private ServerInfoMapper serverInfoMapper;

    /**
     * 查询服务器有效期管理（MySQL5.7兼容版）
     *
     * @param id 服务器有效期管理（MySQL5.7兼容版）主键
     * @return 服务器有效期管理（MySQL5.7兼容版）
     */
    @Override
    public ServerInfo selectServerInfoById(Long id) {
        return serverInfoMapper.selectServerInfoById(id);
    }

    /**
     * 查询服务器有效期管理（MySQL5.7兼容版）列表
     *
     * @param serverInfo 服务器有效期管理（MySQL5.7兼容版）
     * @return 服务器有效期管理（MySQL5.7兼容版）
     */
    @Override
    public List<ServerInfo> selectServerInfoList(ServerInfo serverInfo) {
        return serverInfoMapper.selectServerInfoList(serverInfo);
    }

    /**
     * 新增服务器有效期管理（MySQL5.7兼容版）
     *
     * @param serverInfo 服务器有效期管理（MySQL5.7兼容版）
     * @return 结果
     */
    @Override
    public int insertServerInfo(ServerInfo serverInfo) {
        serverInfo.setCreateTime(DateUtils.getNowDate());
        return serverInfoMapper.insertServerInfo(serverInfo);
    }

    /**
     * 修改服务器有效期管理（MySQL5.7兼容版）
     *
     * @param serverInfo 服务器有效期管理（MySQL5.7兼容版）
     * @return 结果
     */
    @Override
    public int updateServerInfo(ServerInfo serverInfo) {
        serverInfo.setUpdateTime(DateUtils.getNowDate());
        return serverInfoMapper.updateServerInfo(serverInfo);
    }

    /**
     * 批量删除服务器有效期管理（MySQL5.7兼容版）
     *
     * @param ids 需要删除的服务器有效期管理（MySQL5.7兼容版）主键
     * @return 结果
     */
    @Override
    public int deleteServerInfoByIds(Long[] ids) {
        return serverInfoMapper.deleteServerInfoByIds(ids);
    }

    /**
     * 删除服务器有效期管理（MySQL5.7兼容版）信息
     *
     * @param id 服务器有效期管理（MySQL5.7兼容版）主键
     * @return 结果
     */
    @Override
    public int deleteServerInfoById(Long id) {
        return serverInfoMapper.deleteServerInfoById(id);
    }
}
