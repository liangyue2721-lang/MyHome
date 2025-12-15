package com.make.web.controller.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.make.common.core.domain.AjaxResult;
import com.make.framework.web.domain.Server;
import com.make.framework.web.domain.server.ClusterThreadPoolInfo;
import com.make.framework.config.ThreadPoolMonitor;
import com.make.framework.config.ServerInfoCollector;

import java.util.List;
import java.util.Map;

/**
 * 服务器监控
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController {
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);

    @Autowired
    private Server server;

    @Autowired
    private ClusterThreadPoolInfo clusterThreadPoolInfo;

    @Autowired
    private ThreadPoolMonitor threadPoolMonitor;

    @Autowired
    private ServerInfoCollector serverInfoCollector;

    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception {
        server.copyTo();
        return AjaxResult.success(server);
    }

    /**
     * 获取集群环境下所有节点的线程池信息（从内存中获取）
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/clusterThreadPool")
    public AjaxResult getClusterThreadPoolInfo() {
        try {
            Map<String, List<Map<String, Object>>> clusterInfo = server.getClusterThreadPoolInfo();
            logger.info("🌐 获取集群线程池信息（内存模式），节点数量: {}", clusterInfo.size());
            return AjaxResult.success(clusterInfo);
        } catch (Exception e) {
            logger.error("💥 获取集群线程池信息失败", e);
            return AjaxResult.error("获取集群线程池信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取集群环境下所有节点的线程池信息（从Redis中获取）
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/clusterThreadPoolRedis")
    public AjaxResult getClusterThreadPoolInfoFromRedis() {
        try {
            Map<String, List<Map<String, Object>>> clusterInfo = threadPoolMonitor.getClusterThreadPoolInfoFromRedis();
            logger.info("🌐 获取集群线程池信息（Redis模式），节点数量: {}", clusterInfo.size());
            return AjaxResult.success(clusterInfo);
        } catch (Exception e) {
            logger.error("💥 获取集群线程池信息失败", e);
            return AjaxResult.error("获取集群线程池信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取集群环境下所有节点的线程池聚合统计信息（从Redis中获取）
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/aggregatedThreadPoolRedis")
    public AjaxResult getAggregatedThreadPoolInfoFromRedis() {
        try {
            Map<String, Object> aggregatedInfo = threadPoolMonitor.getAggregatedThreadPoolInfoFromRedis();
            logger.info("🌐 获取集群线程池聚合信息（Redis模式），节点数量: {}", aggregatedInfo.get("nodeCount"));
            return AjaxResult.success(aggregatedInfo);
        } catch (Exception e) {
            logger.error("💥 获取集群线程池聚合信息失败", e);
            return AjaxResult.error("获取集群线程池聚合信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取网络流量信息
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/networkTraffic")
    public AjaxResult getNetworkTrafficInfo() throws Exception {
        server.copyTo();
        return AjaxResult.success(server.getNetworkTraffic());
    }

    /**
     * 获取集群环境下所有节点的服务器信息（从Redis中获取）
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/clusterServerRedis")
    public AjaxResult getClusterServerInfoFromRedis() {
        try {
            Map<String, Map<String, Object>> clusterInfo = serverInfoCollector.getAllNodeServerInfoFromRedis();
            logger.info("🌐 获取集群服务器信息（Redis模式），节点数量: {}", clusterInfo.size());
            return AjaxResult.success(clusterInfo);
        } catch (Exception e) {
            logger.error("💥 获取集群服务器信息失败", e);
            return AjaxResult.error("获取集群服务器信息失败: " + e.getMessage());
        }
    }
}