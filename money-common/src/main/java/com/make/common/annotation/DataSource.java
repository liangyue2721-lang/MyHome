package com.make.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.make.common.enums.DataSourceType;

/**
 * 自定义多数据源切换注解
 * <p>
 * 该注解用于实现多数据源的动态切换，支持在方法级别和类级别使用。
 * 通过在方法或类上添加此注解，可以指定要使用的数据源类型
 * </p>
 * 
 * 优先级：先方法，后类，如果方法覆盖了类上的数据源类型，以方法的为准，否则以类上的为准
 *
 * @author ruoyi
 */
// 指定注解可以应用在方法和类上
@Target({ ElementType.METHOD, ElementType.TYPE })
// 指定注解在运行时保留，可以通过反射获取
@Retention(RetentionPolicy.RUNTIME)
// 指定注解将被包含在JavaDoc中
@Documented
// 指定注解可以被继承
@Inherited
public @interface DataSource
{
    /**
     * 切换数据源名称
     * <p>
     * 指定要切换到的数据源类型，默认使用主数据源(MASTER)
     * </p>
     *
     * @return 数据源类型，默认为DataSourceType.MASTER
     */
    public DataSourceType value() default DataSourceType.MASTER;
}