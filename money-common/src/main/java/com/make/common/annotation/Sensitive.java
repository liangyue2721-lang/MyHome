package com.make.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.make.common.config.serializer.SensitiveJsonSerializer;
import com.make.common.enums.DesensitizedType;

/**
 * 数据脱敏注解
 * <p>
 * 该注解用于对敏感数据进行脱敏处理，如手机号、身份证号、银行卡号等。
 * 通过在实体类字段上添加此注解，可以在数据序列化为JSON时自动进行脱敏处理
 * </p>
 *
 * @author ruoyi
 */
// 指定注解在运行时保留，可以通过反射获取
@Retention(RetentionPolicy.RUNTIME)
// 指定注解只能应用在字段上
@Target(ElementType.FIELD)
// 允许在注解中嵌套其他Jackson注解
@JacksonAnnotationsInside
// 指定使用自定义的JSON序列化器进行序列化
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive
{
    /**
     * 脱敏类型
     * <p>
     * 指定具体的脱敏处理类型，如手机号、身份证号、银行卡号等
     * </p>
     *
     * @return 脱敏类型
     */
    DesensitizedType desensitizedType();
}