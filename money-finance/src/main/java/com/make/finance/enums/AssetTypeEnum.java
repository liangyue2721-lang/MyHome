package com.make.finance.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 资产类型枚举类，表示不同类型的资产
 * <p>
 * 该枚举类包含以下资产类型：
 * <ul>
 *     <li>{@code CURRENT_ASSET}：流动资产</li>
 *     <li>{@code INVESTMENT_ASSET}：投资资产</li>
 *     <li>{@code FIXED_ASSET}：固定资产</li>
 * </ul>
 * </p>
 * <p>
 * 每个枚举值关联一个描述字符串和一个标志数字，提供了获取描述、标志和根据描述获取枚举实例的方法
 * </p>
 *
 * @author Devil
 */
public enum AssetTypeEnum {
    /**
     * 流动资产
     * <p>
     * 指预计在一个正常营业周期内或一个营业周期内变现、出售或耗用的资产
     * </p>
     */
    CURRENT_ASSET("流动资产", 1),

    /**
     * 投资资产
     * <p>
     * 指企业为通过分配来增加财富、或为谋求其他利益，而将资产让渡给其他单位所获得的另一项资产
     * </p>
     */
    INVESTMENT_ASSET("投资资产", 2),

    /**
     * 固定资产
     * <p>
     * 指企业为生产商品、提供劳务、出租或经营管理而持有的、使用寿命超过一个会计年度的有形资产
     * </p>
     */
    FIXED_ASSET("固定资产", 3);

    /**
     * 资产类型的描述
     * <p>
     * 用于界面展示和日志输出的中文描述
     * </p>
     */
    private final String description;

    /**
     * 资产类型的标志
     * <p>
     * 用于数据库存储和接口传输的数字编码
     * </p>
     */
    private final int num;

    /**
     * 描述与枚举实例的映射关系
     * <p>
     * 用于快速查找枚举实例，提高查询效率
     * </p>
     */
    private static final Map<String, AssetTypeEnum> DESCRIPTION_TO_ENUM_MAP = new HashMap<>();

    // 静态块，在枚举类加载时初始化DESCRIPTION_TO_ENUM_MAP
    static {
        // 遍历所有枚举实例
        for (AssetTypeEnum assetType : AssetTypeEnum.values()) {
            // 将资产类型描述与枚举实例建立映射关系
            DESCRIPTION_TO_ENUM_MAP.put(assetType.description, assetType);
        }
    }

    /**
     * 构造方法
     *
     * @param description 资产类型的描述
     * @param num         资产类型的标志
     */
    AssetTypeEnum(String description, int num) {
        // 初始化资产类型的描述
        this.description = description;
        // 初始化资产类型的标志
        this.num = num;
    }

    /**
     * 获取资产类型的描述
     *
     * @return 资产类型的描述
     */
    public String getDescription() {
        // 返回资产类型的描述
        return description;
    }

    /**
     * 获取资产类型的标志
     *
     * @return 资产类型的标志
     */
    public int getNum() {
        // 返回资产类型的标志
        return num;
    }

    /**
     * 获取所有资产类型的描述与枚举实例的映射关系
     *
     * @return 包含所有资产类型描述与枚举实例的映射关系的 {@code Map}
     */
    public static Map<String, AssetTypeEnum> getAllDescriptions() {
        // 返回描述与枚举实例的映射关系
        return DESCRIPTION_TO_ENUM_MAP;
    }

    /**
     * 根据描述字符串获取对应的枚举实例
     *
     * @param description 资产类型的描述
     * @return 对应的枚举实例，如果找不到则返回 {@code null}
     */
    public static AssetTypeEnum fromDescription(String description) {
        // 根据描述从映射表中获取对应的枚举实例
        return DESCRIPTION_TO_ENUM_MAP.get(description);
    }
}