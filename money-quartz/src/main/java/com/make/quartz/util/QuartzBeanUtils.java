package com.make.quartz.util;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

/**
 * Quartz 专用 Bean 工具类
 */
public class QuartzBeanUtils {

    private static final Logger log = LoggerFactory.getLogger(QuartzBeanUtils.class);

    /**
     * JSON 字符串转对象
     * @param json JSON字符串
     * @param clazz 目标类
     * @param <T> 泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("JSON parse error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 属性拷贝
     */
    public static void copyProperties(Object source, Object target) {
        BeanUtils.copyProperties(source, target);
    }
}
