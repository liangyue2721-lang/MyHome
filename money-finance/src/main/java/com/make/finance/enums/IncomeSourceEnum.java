package com.make.finance.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 收入来源枚举类
 * <p>
 * 定义系统中所有可能的收入来源类型，包括工资、投资、兼职等
 * </p>
 */
public enum IncomeSourceEnum {
    /**
     * 工资收入
     * <p>
     * 来自正式工作的薪资收入
     * </p>
     */
    WAGE("工资"),
    
    /**
     * 投资收入
     * <p>
     * 来自股票、基金、债券等投资的收益
     * </p>
     */
    INVESTMENT("投资"),
    
    /**
     * 兼职收入
     * <p>
     * 来自非正式工作或临时工作的收入
     * </p>
     */
    PART_TIME_JOB("兼职"),
    
    /**
     * 其他收入
     * <p>
     * 不属于上述分类的其他类型收入
     * </p>
     */
    OTHER("其他"),
    
    /**
     * 公积金收入
     * <p>
     * 来自住房公积金的收入
     * </p>
     */
    PROVIDENT_FUND("公积金");

    /**
     * 收入来源描述
     * <p>
     * 用于界面展示和日志输出的中文描述
     * </p>
     */
    private final String source;
    
    /**
     * 收入来源描述与枚举名称的映射关系
     * <p>
     * 用于快速查找枚举实例，提高查询效率
     * </p>
     */
    private static final Map<String, String> sourceMap = new HashMap<>();

    // 静态块，在枚举类加载时初始化sourceMap
    static {
        // 遍历所有枚举实例
        for (IncomeSourceEnum source : IncomeSourceEnum.values()) {
            // 将枚举名称与收入来源描述建立映射关系
            sourceMap.put(source.name(), source.source);
        }
    }

    /**
     * 构造方法
     *
     * @param source 收入来源描述
     */
    IncomeSourceEnum(String source) {
        // 初始化收入来源描述
        this.source = source;
    }

    /**
     * 获取收入来源描述
     *
     * @return 收入来源描述
     */
    public String getSource() {
        // 返回收入来源描述
        return source;
    }

    /**
     * 获取所有收入来源的键值对
     *
     * @return 包含所有收入来源键值对的Map
     */
    public static Map<String, String> getAllSources() {
        // 返回收入来源的映射关系
        return sourceMap;
    }

    /**
     * 根据描述字符串获取对应的枚举值
     *
     * @param text 描述字符串
     * @return 对应的枚举值，找不到则返回null
     */
    public static IncomeSourceEnum fromString(String text) {
        // 遍历所有枚举实例
        for (IncomeSourceEnum source : IncomeSourceEnum.values()) {
            // 比较收入来源描述是否匹配（忽略大小写）
            if (source.source.equalsIgnoreCase(text)) {
                // 如果匹配，返回对应的枚举实例
                return source;
            }
        }
        // 如果未找到匹配的描述，返回null
        return null;
    }

    /**
     * 根据枚举名称获取对应的收入来源描述
     *
     * @param description 枚举名称
     * @return 对应的收入来源描述，找不到则返回null
     */
    public static String fromDescription(String description) {
        // 根据枚举名称从映射表中获取对应的收入来源描述
        return sourceMap.get(description);
    }
}