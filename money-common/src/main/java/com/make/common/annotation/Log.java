package com.make.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.make.common.enums.BusinessType;
import com.make.common.enums.OperatorType;

/**
 * 自定义操作日志记录注解
 * <p>
 * 该注解用于记录用户操作日志，可以记录操作模块、业务类型、操作人员等信息。
 * 支持记录请求参数和响应结果，便于系统审计和问题追踪
 * </p>
 *
 * @author ruoyi
 */
// 指定注解可以应用在参数和方法上
@Target({ ElementType.PARAMETER, ElementType.METHOD })
// 指定注解在运行时保留，可以通过反射获取
@Retention(RetentionPolicy.RUNTIME)
// 指定注解将被包含在JavaDoc中
@Documented
public @interface Log
{
    /**
     * 模块
     * <p>
     * 指定操作所属的业务模块名称，如"用户管理"、"角色管理"等
     * </p>
     *
     * @return 模块名称，默认为空字符串
     */
    public String title() default "";

    /**
     * 功能
     * <p>
     * 指定具体的操作功能类型，如新增、修改、删除等
     * </p>
     *
     * @return 业务类型，默认为BusinessType.OTHER
     */
    public BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类别
     * <p>
     * 指定操作人员的类别，如后台用户、前台用户等
     * </p>
     *
     * @return 操作人员类别，默认为OperatorType.MANAGE
     */
    public OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求的参数
     * <p>
     * 控制是否记录请求参数信息，对于敏感信息可以通过excludeParamNames属性排除
     * </p>
     *
     * @return 是否保存请求参数，默认为true
     */
    public boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的参数
     * <p>
     * 控制是否记录响应结果信息
     * </p>
     *
     * @return 是否保存响应参数，默认为true
     */
    public boolean isSaveResponseData() default true;

    /**
     * 排除指定的请求参数
     * <p>
     * 指定不需要记录的请求参数名称，用于排除敏感信息如密码等
     * </p>
     *
     * @return 需要排除的参数名称数组，默认为空数组
     */
    public String[] excludeParamNames() default {};
}
