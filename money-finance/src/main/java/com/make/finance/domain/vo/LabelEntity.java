package com.make.finance.domain.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 标签实体类，用于封装具有标签和值的数据对象。
 *
 * @author [你的名字]
 * @since [版本号]
 */

public class LabelEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;
    private String value;

    /**
     * 默认构造函数，初始化一个空的LabelEntity。
     */
    public LabelEntity() {
    }

    /**
     * 构造函数，用于初始化具有指定标签和值的LabelEntity。
     *
     * @param label 标签，表示该对象的标识或名称
     * @param value 值，与标签对应的具体内容
     */
    public LabelEntity(String label, String value) {
        this.label = label;
        this.value = value;
    }

    /**
     * 获取标签。
     *
     * @return 标签，不可能为null，可能为空字符串
     */
    public String getLabel() {
        return label;
    }

    /**
     * 设置标签。
     *
     * @param label 新的标签
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * 获取值。
     *
     * @return 值，不可能为null，可能为空字符串
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置值。
     *
     * @param value 新的值
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "LabelEntity{" +
                "label='" + label + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelEntity that = (LabelEntity) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, value);
    }
}
