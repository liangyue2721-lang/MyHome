package com.make.common.core.redis;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 *
 * @author ruoyi
 **/
@SuppressWarnings(value = {"unchecked", "rawtypes"})
@Component
public class RedisCache {
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.MINUTES);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection) {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext()) {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 向指定的 Redis Set 缓存中添加单个元素（安全版本）
     *
     * <p>
     * 如果 key 不存在，则创建新的 Set 并添加元素。
     * 如果 key 存在但不是 Set 类型，则先删除旧 key，再创建新的 Set。
     * </p>
     *
     * @param key   缓存键
     * @param value 要添加的元素
     * @param <T>   元素类型
     */
    public <T> void addCacheSetValue(final String key, T value) {
        // 如果 key 已存在，检查类型
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            String type = redisTemplate.type(key).code();
            if (!"set".equals(type)) {
                // 如果类型不是 set，删除旧值，避免 WRONGTYPE 错误
                redisTemplate.delete(key);
            }
        }

        // 绑定指定 key 的 Set 操作对象
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);

        // 向 Redis Set 中添加一个元素
        setOperation.add(value);
    }


    /**
     * 获取指定 Redis Set 缓存中的所有元素（安全版本）
     *
     * <p>
     * 如果 key 不存在，返回空 Set。
     * 如果 key 类型不是 Set，不抛异常，而是返回空 Set（避免 WRONGTYPE 错误）。
     * </p>
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return Redis Set 中的所有元素，如果 key 不存在或类型不匹配则返回空 Set
     */
    public <T> Set<T> getCacheSetAll(final String key) {
        // 判断 key 是否存在
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            return Collections.emptySet();
        }

        // 获取 key 的类型
        String type = redisTemplate.type(key).code();
        if (!"set".equals(type)) {
            // 如果 key 类型不是 set，避免 WRONGTYPE 异常
            return Collections.emptySet();
        }

        // 类型匹配时，获取 Set 中的所有元素
        return redisTemplate.opsForSet().members(key);
    }



    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey) {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 设置任意缓存对象并指定过期时间
     *
     * @param key      缓存键
     * @param value    缓存值，可以是基本类型、对象、集合等
     * @param timeout  超时时间
     * @param timeUnit 时间单位，如：TimeUnit.SECONDS
     * @param <T>      缓存数据类型
     */
    public <T> void setCacheObjectWithExpire(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 针对任意已存在的缓存键设置过期时间
     *
     * @param key      缓存键
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @return 设置是否成功 true=成功；false=失败
     */
    public boolean setExpireTime(final String key, final long timeout, final TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

}
