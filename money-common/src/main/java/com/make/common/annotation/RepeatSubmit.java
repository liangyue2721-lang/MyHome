package com.make.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解防止表单重复提交
 * <p>
 * 该注解用于防止用户重复提交表单数据，通过在方法上添加此注解，
 * 可以限制在指定时间间隔内不能重复调用该方法，
 * 从而避免因网络延迟或用户误操作导致的重复提交问题
 * </p>
 *
 * @author ruoyi
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit
{
    /**
     * 间隔时间(ms)，小于此时间视为重复提交
     * <p>
     * 设置防重复提交的时间间隔，单位为毫秒。
     * 如果用户在该时间间隔内重复提交表单，则视为重复提交并阻止执行
     * </p>
     *
     * @return 间隔时间，默认为5000毫秒(5秒)
     */
    public int interval() default 5000;

    /**
     * 提示消息
     * <p>
     * 当检测到重复提交时，向用户显示的提示消息
     * </p>
     *
     * @return 提示消息，默认为"不允许重复提交，请稍候再试"
     */
    public String message() default "不允许重复提交，请稍候再试";
}
