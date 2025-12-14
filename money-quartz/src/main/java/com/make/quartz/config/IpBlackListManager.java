package com.make.quartz.config;

import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * IP黑名单管理器
 * 用于管理不允许执行任务分配和任务消费的IP地址
 *
 * @author make
 */
@Component
public class IpBlackListManager {
    
    private static final Logger log = LoggerFactory.getLogger(IpBlackListManager.class);
    
    /**
     * Redis中存储IP黑名单的key
     */
    private static final String IP_BLACKLIST_KEY = "quartz:ip:blacklist";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 当前节点IP地址
     */
    private String currentNodeIp;
    
    @PostConstruct
    public void init() {
        // 获取当前节点IP
        currentNodeIp = IpUtils.getHostIp();
        log.info("初始化IP黑名单管理器，当前节点IP: {}", currentNodeIp);
    }
    
    /**
     * 检查IP是否在黑名单中
     *
     * @param ip IP地址
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isIpBlacklisted(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        Boolean isMember = redisTemplate.opsForSet().isMember(IP_BLACKLIST_KEY, ip);
        return Boolean.TRUE.equals(isMember);
    }
    
    /**
     * 检查当前节点IP是否在黑名单中
     *
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isCurrentNodeIpBlacklisted() {
        return isIpBlacklisted(currentNodeIp);
    }
    
    /**
     * 将IP添加到黑名单
     *
     * @param ip IP地址
     */
    public void addToBlackList(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        
        redisTemplate.opsForSet().add(IP_BLACKLIST_KEY, ip);
        log.info("IP地址 {} 已添加到黑名单", ip);
    }
    
    /**
     * 将多个IP添加到黑名单
     *
     * @param ips IP地址集合
     */
    public void addToBlackList(Set<String> ips) {
        if (CollectionUtils.isEmpty(ips)) {
            return;
        }
        
        redisTemplate.opsForSet().add(IP_BLACKLIST_KEY, ips.toArray(new String[0]));
        log.info("IP地址 {} 已添加到黑名单", ips);
    }
    
    /**
     * 从黑名单中移除IP
     *
     * @param ip IP地址
     */
    public void removeFromBlackList(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        
        redisTemplate.opsForSet().remove(IP_BLACKLIST_KEY, ip);
        log.info("IP地址 {} 已从黑名单中移除", ip);
    }
    
    /**
     * 获取所有黑名单IP
     *
     * @return 黑名单IP集合
     */
    public Set<String> getBlackList() {
        return redisTemplate.opsForSet().members(IP_BLACKLIST_KEY);
    }
    
    /**
     * 清空黑名单
     */
    public void clearBlackList() {
        redisTemplate.delete(IP_BLACKLIST_KEY);
        log.info("IP黑名单已清空");
    }
    
    /**
     * 获取当前节点IP
     *
     * @return 当前节点IP
     */
    public String getCurrentNodeIp() {
        return currentNodeIp;
    }
}