package com.make.generator.domain;

import com.make.common.core.domain.BaseEntity;
import com.make.common.utils.StringUtils;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 代码生成业务字段表 gen_table_column
 *
 * 支持链式调用，保留原有辅助方法如 isPk、readConverterExp 等
 *
 * @author ruoyi
 */
@Data
@Accessors(chain = true)
public class GenTableColumn extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编号 */
    private Long columnId;

    /** 归属表编号 */
    private Long tableId;

    /** 列名称 */
    private String columnName;

    /** 列描述 */
    private String columnComment;

    /** 列类型 */
    private String columnType;

    /** JAVA类型 */
    private String javaType;

    /** JAVA字段名 */
    @NotBlank(message = "Java属性不能为空")
    private String javaField;

    /** 是否主键（1是） */
    private String isPk;

    /** 是否自增（1是） */
    private String isIncrement;

    /** 是否必填（1是） */
    private String isRequired;

    /** 是否为插入字段（1是） */
    private String isInsert;

    /** 是否编辑字段（1是） */
    private String isEdit;

    /** 是否列表字段（1是） */
    private String isList;

    /** 是否查询字段（1是） */
    private String isQuery;

    /** 查询方式（EQ等于、NE不等于、GT大于、LT小于、LIKE模糊、BETWEEN范围） */
    private String queryType;

    /** 显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件、image图片上传控件、upload文件上传控件、editor富文本控件） */
    private String htmlType;

    /** 字典类型 */
    private String dictType;

    /** 排序 */
    private Integer sort;

    /** 获取 Java 字段名首字母大写形式 */
    public String getCapJavaField() {
        return StringUtils.capitalize(javaField);
    }

    /** 是否主键 */
    public boolean isPk() {
        return isTrue(isPk);
    }

    /** 是否自增 */
    public boolean isIncrement() {
        return isTrue(isIncrement);
    }

    /** 是否必填 */
    public boolean isRequired() {
        return isTrue(isRequired);
    }

    /** 是否插入字段 */
    public boolean isInsert() {
        return isTrue(isInsert);
    }

    /** 是否编辑字段 */
    public boolean isEdit() {
        return isTrue(isEdit);
    }

    /** 是否列表字段 */
    public boolean isList() {
        return isTrue(isList);
    }

    /** 是否查询字段 */
    public boolean isQuery() {
        return isTrue(isQuery);
    }

    /** 是否超级字段（代码生成时忽略） */
    public boolean isSuperColumn() {
        return isSuperColumn(javaField);
    }

    public static boolean isSuperColumn(String javaField) {
        return StringUtils.equalsAnyIgnoreCase(javaField,
                // BaseEntity
                "createBy", "createTime", "updateBy", "updateTime", "remark",
                // TreeEntity
                "parentName", "parentId", "orderNum", "ancestors");
    }

    /** 是否可用字段（用于生成页面时的白名单） */
    public boolean isUsableColumn() {
        return isUsableColumn(javaField);
    }

    public static boolean isUsableColumn(String javaField) {
        return StringUtils.equalsAnyIgnoreCase(javaField, "parentId", "orderNum", "remark");
    }

    /** 构建字典转换表达式 */
    public String readConverterExp() {
        String remarks = StringUtils.substringBetween(this.columnComment, "（", "）");
        if (StringUtils.isEmpty(remarks)) {
            return columnComment;
        }
        StringBuilder sb = new StringBuilder();
        for (String value : remarks.split(" ")) {
            if (StringUtils.isNotEmpty(value) && value.length() > 1) {
                sb.append(value.charAt(0)).append("=").append(value.substring(1)).append(",");
            }
        }
        return sb.length() > 0 ? sb.deleteCharAt(sb.length() - 1).toString() : columnComment;
    }

    /** 通用判断字段是否为 "1" 的布尔表示 */
    private boolean isTrue(String value) {
        return "1".equals(value);
    }
}