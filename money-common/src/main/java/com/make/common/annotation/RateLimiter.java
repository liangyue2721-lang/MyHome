package com.make.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.make.common.constant.CacheConstants;
import com.make.common.enums.LimitType;

/**
 * 限流注解
 * <p>
 * 该注解用于实现接口访问限流功能，防止因突发流量导致系统过载。
 * 支持基于时间窗口的限流策略，可以限制单位时间内的访问次数
 * </p>
 *
 * @author ruoyi
 */
// 指定注解只能应用在方法上
@Target(ElementType.METHOD)
// 指定注解在运行时保留，可以通过反射获取
@Retention(RetentionPolicy.RUNTIME)
// 指定注解将被包含在JavaDoc中
@Documented
public @interface RateLimiter
{
    /**
     * 限流key
     * <p>
     * 用于标识限流规则的唯一键值，不同key对应不同的限流规则
     * </p>
     *
     * @return 限流key，默认使用CacheConstants.RATE_LIMIT_KEY
     */
    public String key() default CacheConstants.RATE_LIMIT_KEY;

    /**
     * 限流时间,单位秒
     * <p>
     * 定义限流统计的时间窗口大小，单位为秒
     * </p>
     *
     * @return 限流时间窗口大小，默认为60秒
     */
    public int time() default 60;

    /**
     * 限流次数
     * <p>
     * 在指定时间窗口内允许的最大访问次数
     * </p>
     *
     * @return 限流次数，默认为100次
     */
    public int count() default 100;

    /**
     * 限流类型
     * <p>
     * 指定限流的策略类型，如默认策略或基于IP的限流策略
     * </p>
     *
     * @return 限流类型，默认为LimitType.DEFAULT
     */
    public LimitType limitType() default LimitType.DEFAULT;
}