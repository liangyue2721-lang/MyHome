package com.make.framework.config;

import com.alibaba.fastjson2.JSON;
import com.make.common.core.redis.RedisCache;
import com.make.common.utils.ip.IpUtils;
import com.make.framework.web.domain.Server;
import com.make.framework.web.domain.server.NetworkTraffic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 服务器信息收集器
 * 定期收集本节点的服务器信息并存储到Redis中，供集群监控使用
 */
@Component
public class ServerInfoCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerInfoCollector.class);
    
    @Autowired
    private Server server;
    
    @Autowired
    private RedisCache redisCache;
    
    /**
     * Redis中存储集群服务器信息的key
     */
    private static final String CLUSTER_SERVER_INFO_KEY = "cluster_server_info";
    
    /**
     * 本节点服务器信息在Redis中的过期时间（秒）
     */
    @Value("${monitor.server.info.expireTime:300}")
    private long expireTime;
    
    /**
     * 当前节点ID，格式为IP:UUID
     */
    private final String currentNodeId = getHostIp() + ":" + UUID.randomUUID().toString();
    
    /**
     * 获取当前节点ID
     * 
     * @return 当前节点ID
     */
    public String getCurrentNodeId() {
        return currentNodeId;
    }
    
    /**
     * 获取本机IP地址
     * 
     * @return IP地址字符串
     */
    private static String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("无法获取本机IP地址，使用默认值", e);
            return "127.0.0.1";
        }
    }
    
    /**
     * 定时收集并存储本节点服务器信息到Redis
     * 每10秒执行一次
     */
    @Scheduled(fixedRate = 10000)
    public void collectAndStoreServerInfo() {
        try {
            logger.debug("🔄 开始收集并存储本节点服务器信息");
            
            // 收集本节点服务器信息
            Map<String, Object> serverInfo = collectServerInfo();
            
            // 获取当前节点ID
            String nodeId = getCurrentNodeId();
            
            // 清理相同IP的旧数据，确保相同IP的以最新的为准
            cleanupSameIpOldNodes(nodeId);
            
            // 将服务器信息存储到Redis中
            if (redisCache != null) {
                // 使用安全的序列化方式
                String serverInfoJson = JSON.toJSONString(serverInfo, String.valueOf(true));
                redisCache.setCacheMapValue(CLUSTER_SERVER_INFO_KEY, nodeId, serverInfoJson);
                
                // 设置过期时间
                redisCache.expire(CLUSTER_SERVER_INFO_KEY, expireTime, TimeUnit.SECONDS);
            }
            
            logger.debug("✅ 本节点服务器信息存储完成: nodeId={}, keys={}, size={}", nodeId, serverInfo.keySet(), serverInfo.size());
        } catch (Exception e) {
            logger.warn("💥 收集并存储本节点服务器信息失败", e);
        }
    }
    
    /**
     * 清理相同IP的旧节点数据
     * 
     * @param currentNodeId 当前节点ID
     */
    private void cleanupSameIpOldNodes(String currentNodeId) {
        try {
            // 检查redisCache是否为空
            if (redisCache == null) {
                logger.warn("RedisCache 未初始化");
                return;
            }
            
            // 获取当前节点的IP地址
            String currentIp = currentNodeId.split(":")[0];
            
            // 从Redis中获取所有节点信息
            Map<String, String> nodeInfoMap = redisCache.getCacheMap(CLUSTER_SERVER_INFO_KEY);
            
            // 查找并删除相同IP的旧节点
            for (Map.Entry<String, String> entry : nodeInfoMap.entrySet()) {
                String nodeId = entry.getKey();
                String nodeIp = nodeId.split(":")[0];
                
                // 如果是相同IP但不同UUID的旧节点，则删除
                if (currentIp.equals(nodeIp) && !currentNodeId.equals(nodeId)) {
                    redisCache.deleteCacheMapValue(CLUSTER_SERVER_INFO_KEY, nodeId);
                    logger.info("🧹 清理相同IP的旧节点数据: nodeId={}", nodeId);
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 清理相同IP的旧节点数据时出现异常", e);
        }
    }
    
    /**
     * 从Redis中获取所有节点的服务器信息
     * 
     * @return 所有节点的服务器信息
     */
    public Map<String, Map<String, Object>> getAllNodeServerInfoFromRedis() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        try {
            // 检查redisCache是否为空
            if (redisCache == null) {
                logger.warn("RedisCache 未初始化");
                return result;
            }
            
            // 从Redis中获取所有节点信息
            Map<String, String> nodeInfoMap = redisCache.getCacheMap(CLUSTER_SERVER_INFO_KEY);
            
            logger.debug("🔍 从Redis中获取节点服务器信息，节点数量: {}", nodeInfoMap.size());
            
            // 反序列化节点信息
            for (Map.Entry<String, String> entry : nodeInfoMap.entrySet()) {
                String nodeId = entry.getKey();
                String nodeInfoJson = entry.getValue();
                
                // 过滤掉包含127.0.0.1的节点
                if (nodeId.contains("127.0.0.1")) {
                    logger.debug("🚫 过滤掉本地节点: {}", nodeId);
                    continue;
                }
                
                Map<String, Object> nodeInfo = JSON.parseObject(nodeInfoJson, Map.class);
                result.put(nodeId, nodeInfo);
                
                logger.debug("📄 节点{}服务器信息: {}", nodeId, nodeInfoJson);
            }
            
            logger.info("📊 从Redis中获取集群服务器信息: 节点数量={}", result.size());
        } catch (Exception e) {
            logger.error("💥 从Redis中获取集群服务器信息失败", e);
        }
        
        return result;
    }
    
    /**
     * 收集本节点服务器信息
     * 
     * @return 服务器信息Map
     */
    private Map<String, Object> collectServerInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            // 检查server是否为空
            if (server == null) {
                logger.warn("Server 对象未初始化");
                return info;
            }

            // 初始化线程池信息
            server.getThreadPoolInfo().init();
            
            // 收集服务器信息
            server.copyTo();
            
            // CPU信息
            Map<String, Object> cpuInfo = new HashMap<>();
            if (server.getCpu() != null) {
                cpuInfo.put("cpuNum", server.getCpu().getCpuNum());
                cpuInfo.put("used", server.getCpu().getUsed());
                cpuInfo.put("sys", server.getCpu().getSys());
                cpuInfo.put("free", server.getCpu().getFree());
            }
            info.put("cpu", cpuInfo);
            
            // 内存信息
            Map<String, Object> memInfo = new HashMap<>();
            if (server.getMem() != null) {
                memInfo.put("total", server.getMem().getTotal());
                memInfo.put("used", server.getMem().getUsed());
                memInfo.put("free", server.getMem().getFree());
                memInfo.put("usage", server.getMem().getUsage());
            }
            info.put("mem", memInfo);
            
            // JVM信息
            Map<String, Object> jvmInfo = new HashMap<>();
            if (server.getJvm() != null) {
                jvmInfo.put("total", server.getJvm().getTotal());
                jvmInfo.put("max", server.getJvm().getMax());
                jvmInfo.put("free", server.getJvm().getFree());
                jvmInfo.put("used", server.getJvm().getUsed());
                jvmInfo.put("usage", server.getJvm().getUsage());
                jvmInfo.put("version", server.getJvm().getVersion());
                jvmInfo.put("home", server.getJvm().getHome());
            }
            info.put("jvm", jvmInfo);
            
            // 服务器信息
            Map<String, Object> sysInfo = new HashMap<>();
            if (server.getSys() != null) {
                sysInfo.put("computerName", server.getSys().getComputerName());
                sysInfo.put("computerIp", server.getSys().getComputerIp());
                sysInfo.put("osName", server.getSys().getOsName());
                sysInfo.put("osArch", server.getSys().getOsArch());
                sysInfo.put("userDir", server.getSys().getUserDir());
            }
            info.put("sys", sysInfo);
            
            // 磁盘信息
            if (server.getSysFiles() != null) {
                info.put("sysFiles", server.getSysFiles());
            }
            
            // 网络流量信息
            NetworkTraffic networkTraffic = server.getNetworkTraffic();
            if (networkTraffic != null && networkTraffic.getInterfaces() != null) {
                // 只收集非本地回环接口的网络流量信息
                List<NetworkTraffic.NetworkInterfaceInfo> interfaces = networkTraffic.getInterfaces().stream()
                    .filter(iface -> iface != null && iface.getName() != null && !iface.getName().startsWith("lo"))
                    .collect(Collectors.toList());
                info.put("networkTraffic", interfaces); // 只传递接口列表而不是整个对象
            }
            
            logger.debug("🔄 收集本节点服务器信息完成: keys={}, size={}", info.keySet(), info.size());
        } catch (Exception e) {
            logger.warn("💥 收集本节点服务器信息失败", e);
            // 出现异常时返回空信息
        }
        
        return info;
    }
}