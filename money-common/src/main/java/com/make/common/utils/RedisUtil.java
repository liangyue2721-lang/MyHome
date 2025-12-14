package com.make.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 设置缓存对象
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   对象类型
     */
    public <T> void setObject(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            // 记录日志或处理异常
        }
    }

    /**
     * 获取缓存对象
     *
     * @param key   缓存键
     * @param clazz 对象类型
     * @param <T>   对象类型
     * @return 缓存对象
     */
    public <T> T getObject(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            // 记录日志或处理异常
            return null;
        }
    }

    /**
     * 设置缓存列表
     *
     * @param key      缓存键
     * @param dataList 缓存列表
     * @param <T>      对象类型
     */
    public <T> void setList(String key, List<T> dataList) {
        try {
            String json = objectMapper.writeValueAsString(dataList);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            // 记录日志或处理异常
        }
    }

    /**
     * 获取缓存列表
     *
     * @param key   缓存键
     * @param clazz 对象类型
     * @param <T>   对象类型
     * @return 缓存列表
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            // 记录日志或处理异常
            return Collections.emptyList();
        }
    }

    /**
     * 设置缓存集合
     *
     * @param key     缓存键
     * @param dataSet 缓存集合
     * @param <T>     对象类型
     */
    public <T> void setSet(String key, Set<T> dataSet) {
        try {
            String json = objectMapper.writeValueAsString(dataSet);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            // 记录日志或处理异常
        }
    }

    /**
     * 获取缓存集合
     *
     * @param key   缓存键
     * @param clazz 对象类型
     * @param <T>   对象类型
     * @return 缓存集合
     */
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Collections.emptySet();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(Set.class, clazz));
        } catch (IOException e) {
            // 记录日志或处理异常
            return Collections.emptySet();
        }
    }

    /**
     * 设置缓存映射
     *
     * @param key     缓存键
     * @param dataMap 缓存映射
     * @param <K>     键类型
     * @param <V>     值类型
     */
    public <K, V> void setMap(String key, Map<K, V> dataMap) {
        try {
            String json = objectMapper.writeValueAsString(dataMap);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            // 记录日志或处理异常
        }
    }

    /**
     * 获取缓存映射
     *
     * @param key        缓存键
     * @param keyClass   键类型
     * @param valueClass 值类型
     * @param <K>        键类型
     * @param <V>        值类型
     * @return 缓存映射
     */
    public <K, V> Map<K, V> getMap(String key, Class<K> keyClass, Class<V> valueClass) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (IOException e) {
            // 记录日志或处理异常
            return Collections.emptyMap();
        }
    }

    /**
     * 设置缓存过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void setExpire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    //// 缓存对象
    //User user = new User("John", 30);
    //redisUtil.setObject("user:1", user);
    //
    //// 获取缓存对象
    //User cachedUser = redisUtil.getObject("user:1", User.class);
    //
    //// 缓存列表
    //List<User> userList = Arrays.asList(new User("Alice", 25), new User("Bob", 28));
    //redisUtil.setList("user:list", userList);
    //
    //// 获取缓存列表
    //List<User> cachedUserList = redisUtil.getList("user:list", User.class);
}
