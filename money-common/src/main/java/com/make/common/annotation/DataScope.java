package com.make.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限过滤注解
 * <p>
 * 该注解用于实现数据权限控制，通过在方法上添加此注解，
 * 可以根据用户的角色和权限动态过滤查询结果，
 * 确保用户只能访问其有权限查看的数据
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
public @interface DataScope
{
    /**
     * 部门表的别名
     * <p>
     * 用于在SQL查询中标识部门表的别名，以便动态添加部门数据权限过滤条件
     * </p>
     *
     * @return 部门表别名，默认为空字符串
     */
    public String deptAlias() default "";

    /**
     * 用户表的别名
     * <p>
     * 用于在SQL查询中标识用户表的别名，以便动态添加用户数据权限过滤条件
     * </p>
     *
     * @return 用户表别名，默认为空字符串
     */
    public String userAlias() default "";

    /**
     * 权限字符（用于多个角色匹配符合要求的权限）默认根据权限注解@ss获取，多个权限用逗号分隔开来
     * <p>
     * 用于指定需要匹配的权限字符，支持多个权限的组合匹配
     * </p>
     *
     * @return 权限字符，默认为空字符串
     */
    public String permission() default "";
}