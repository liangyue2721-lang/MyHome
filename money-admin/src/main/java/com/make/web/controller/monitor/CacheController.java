package com.make.web.controller.monitor;

import java.util.*;

import com.alibaba.fastjson2.JSON;
import com.make.common.core.redis.RedisCache;
import com.make.system.config.CacheRegistry;
import com.make.system.domain.CacheMetadata;
import com.make.system.service.ICacheMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.make.common.constant.CacheConstants;
import com.make.common.core.domain.AjaxResult;
import com.make.common.utils.StringUtils;
import com.make.system.domain.SysCache;

/**
 * 缓存监控控制器
 * 提供对 Redis 缓存的基本监控与管理功能，包括查看缓存信息、键值、删除等操作。
 * <p>
 * 支持功能：
 * - 获取Redis信息、命令统计、缓存大小等监控数据
 * - 获取缓存名称列表
 * - 查看指定缓存的所有Key
 * - 查看某个缓存Key的值
 * - 删除指定缓存前缀的所有Key
 * - 删除指定Key
 * - 清空所有缓存
 * <p>
 * 权限控制基于 Spring Security 表达式 @PreAuthorize。
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/cache")
public class CacheController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 预定义缓存名称列表，用于展示或清理指定类型缓存
     */
    private static List<SysCache> CACHES = CacheRegistry.getCaches();

    @Autowired
    private RedisCache redisCache;


    /**
     * 获取 Redis 运行基本信息，包括服务器信息、命令统计信息和当前数据库大小。
     *
     * @return AjaxResult 包含 Redis info、commandstats 和 dbSize 等信息
     * @throws Exception 获取 Redis 信息失败时抛出异常
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception {
        // 获取 Redis 服务器基本信息
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info());

        // 获取 Redis 命令统计信息
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));

        // 获取 Redis 数据库键数量
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.dbSize());

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);

        // 构建命令调用统计数据，用于前端图表展示
        List<Map<String, String>> pieList = new ArrayList<>();
        commandStats.stringPropertyNames().forEach(key -> {
            Map<String, String> data = new HashMap<>(2);
            String property = commandStats.getProperty(key);
            data.put("name", StringUtils.removeStart(key, "cmdstat_")); // 去除前缀
            data.put("value", StringUtils.substringBetween(property, "calls=", ",usec")); // 提取调用次数
            pieList.add(data);
        });
        result.put("commandStats", pieList);
        return AjaxResult.success(result);
    }

    /**
     * 获取预设缓存名称列表，用于缓存清理或查看缓存键。
     *
     * @return AjaxResult 包含缓存名称和描述信息
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getNames")
    public AjaxResult cache() {
        return AjaxResult.success(CACHES);
    }

    /**
     * 获取指定缓存前缀下的所有缓存键。
     *
     * @param cacheName 缓存前缀（如 login_tokens:）
     * @return AjaxResult 包含所有匹配的缓存键集合
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getKeys/{cacheName}")
    public AjaxResult getCacheKeys(@PathVariable String cacheName) {
        Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        return AjaxResult.success(new TreeSet<>(cacheKeys));
    }

    /**
     * 获取指定缓存键的值。
     *
     * @param cacheName 缓存前缀名称
     * @param cacheKey  缓存键
     * @return AjaxResult 包含键值信息
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public AjaxResult getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey) {
        DataType type = redisTemplate.type(cacheKey);
        String cacheValue = "";

        switch (type.code()) {
            case "string":
                cacheValue = redisTemplate.opsForValue().get(cacheKey);
                break;
            case "list":
                cacheValue = JSON.toJSONString(redisTemplate.opsForList().range(cacheKey, 0, -1));
                break;
            case "hash":
                cacheValue = JSON.toJSONString(redisTemplate.opsForHash().entries(cacheKey));
                break;
            case "set":
                cacheValue = JSON.toJSONString(redisTemplate.opsForSet().members(cacheKey));
                break;
            case "zset":
                cacheValue = JSON.toJSONString(redisTemplate.opsForZSet().rangeWithScores(cacheKey, 0, -1));
                break;
            case "none":
                return AjaxResult.error("Key 不存在");
            default:
                return AjaxResult.error("不支持的 Redis 数据类型: " + type.code());
        }

        SysCache sysCache = new SysCache(cacheName, cacheKey, cacheValue);
        return AjaxResult.success(sysCache);
    }

    /**
     * 清除指定缓存前缀下的所有缓存数据。
     *
     * @param cacheName 缓存前缀
     * @return AjaxResult 操作结果
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public AjaxResult clearCacheName(@PathVariable String cacheName) {
        Collection<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }

    /**
     * 删除指定缓存键。
     *
     * @param cacheKey 缓存键
     * @return AjaxResult 操作结果
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheKey/{cacheKey}")
    public AjaxResult clearCacheKey(@PathVariable String cacheKey) {
        redisTemplate.delete(cacheKey);
        return AjaxResult.success();
    }

    /**
     * 清空 Redis 中所有缓存数据（慎用）。
     *
     * @return AjaxResult 操作结果
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheAll")
    public AjaxResult clearCacheAll() {
        Collection<String> cacheKeys = redisTemplate.keys("*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }
}
