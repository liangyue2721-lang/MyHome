package com.make.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 匿名访问不鉴权注解
 * <p>
 * 该注解用于标记控制器方法或类，表示这些方法或类中的所有方法
 * 可以被匿名访问，无需进行身份验证和权限检查
 * </p>
 *
 * @author ruoyi
 */
// 指定注解可以应用在方法和类上
@Target({ ElementType.METHOD, ElementType.TYPE })
// 指定注解在运行时保留，可以通过反射获取
@Retention(RetentionPolicy.RUNTIME)
// 指定注解将被包含在JavaDoc中
@Documented
public @interface Anonymous
{
}