package com.make.framework.web.domain.server;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 集群环境下线程池信息实体类
 * 用于收集和存储多节点的线程池信息
 *
 * @author make
 */
@Component
public class ClusterThreadPoolInfo {
    
    /**
     * 存储各节点线程池信息的Map
     * key: 节点标识(如IP地址或主机名)
     * value: 该节点的线程池信息
     */
    private final Map<String, Map<String, Object>> nodeThreadPoolInfoMap = new ConcurrentHashMap<>();
    
    /**
     * 添加节点线程池信息
     * 
     * @param nodeId 节点标识
     * @param threadPoolInfo 线程池信息
     */
    public void addNodeThreadPoolInfo(String nodeId, Map<String, Object> threadPoolInfo) {
        nodeThreadPoolInfoMap.put(nodeId, threadPoolInfo);
    }
    
    /**
     * 获取所有节点的线程池信息
     * 
     * @return 所有节点的线程池信息
     */
    public Map<String, Map<String, Object>> getAllNodeThreadPoolInfo() {
        return nodeThreadPoolInfoMap;
    }
    
    /**
     * 获取指定节点的线程池信息
     * 
     * @param nodeId 节点标识
     * @return 指定节点的线程池信息
     */
    public Map<String, Object> getNodeThreadPoolInfo(String nodeId) {
        return nodeThreadPoolInfoMap.get(nodeId);
    }
    
    /**
     * 移除过期的节点信息（超过指定时间未更新的节点）
     * 
     * @param expireTime 过期时间（毫秒）
     */
    public void removeExpiredNodes(long expireTime) {
        // 该功能需要结合Redis或其他存储实现
        // 这里仅作为接口定义
    }
    
    /**
     * 清空所有节点信息
     */
    public void clearAllNodes() {
        nodeThreadPoolInfoMap.clear();
    }
    
    /**
     * 获取节点数量
     * 
     * @return 节点数量
     */
    public int getNodeCount() {
        return nodeThreadPoolInfoMap.size();
    }
}