package com.make.common.utils;

import java.util.Collection;

import com.make.common.constant.CacheConstants;
import com.make.common.utils.spring.SpringUtils;
import com.make.common.core.redis.RedisCache;

/**
 * 通用缓存工具类，支持任意对象类型的缓存操作
 * <p>
 * 提供设置、获取、删除、清空缓存等功能，并通过Redis缓存存储对象。
 *
 * @author make
 */
public class CacheUtils {

    /**
     * 设置缓存对象
     *
     * @param key   缓存键
     * @param value 缓存值（任意对象）
     * @param <T>   缓存值类型
     */
    public static <T> void setCache(String key, T value) {
        SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), value);
    }

    /**
     * 获取缓存对象
     *
     * @param key   缓存键
     * @param clazz 期望返回的类型Class
     * @param <T>   缓存值类型
     * @return 缓存中的对象值，若不存在返回null
     */
    public static <T> T getCache(String key, Class<T> clazz) {
        return SpringUtils.getBean(RedisCache.class).getCacheObject(getCacheKey(key));
    }

    /**
     * 删除指定缓存
     *
     * @param key 缓存键
     */
    public static void removeCache(String key) {
        SpringUtils.getBean(RedisCache.class).deleteObject(getCacheKey(key));
    }

    /**
     * 清空所有以指定前缀开头的缓存
     *
     * @param prefix 缓存键前缀
     */
    public static void clearCacheByPrefix(String pattern, String prefix) {
        Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(pattern + prefix + "*");
        SpringUtils.getBean(RedisCache.class).deleteObject(keys);
    }

    /**
     * 清空所有以指定前缀开头的缓存
     *
     * @param prefix 缓存键前缀
     */
    public static void clearCacheByPrefix(String prefix) {
        Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(CacheConstants.GENERIC_CACHE_KEY + prefix + "*");
        SpringUtils.getBean(RedisCache.class).deleteObject(keys);
    }

    /**
     * 构建带有前缀的缓存键
     *
     * @param key 原始键名
     * @return 完整缓存键（带前缀）
     */
    public static String getCacheKey(String pattern, String key) {
        return pattern + key;
    }

    /**
     * 构建带有前缀的缓存键
     *
     * @param key 原始键名
     * @return 完整缓存键（带前缀）
     */
    public static String getCacheKey(String key) {
        return CacheConstants.GENERIC_CACHE_KEY + key;
    }
}
