package com.make.finance.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 资产状态枚举类，定义资产在系统中的生命周期状态
 * <p>
 * 枚举实例通过{@code code}和{@code name}两个属性明确表示业务状态，
 * 遵循金融领域资产状态标准定义，线程安全且不可变
 * </p>
 *
 * @author Ruoyi项目组
 * @since 2025.03
 */
public enum AssetStatusEnum {

    /**
     * 正常可用状态，资产可进行交易和流转
     */
    VALID(1, "有效"),

    /**
     * 临时冻结状态，资产因风控或监管要求被暂时锁定
     */
    FROZEN(0, "冻结"),

    /**
     * 终止状态，资产已完成最终结算并退出系统
     */
    LIQUIDATED(9, "已清算");

    /**
     * 状态编码
     * <p>
     * 用于数据库存储和接口传输的数字编码
     * </p>
     */
    private final int code;
    
    /**
     * 状态描述
     * <p>
     * 用于界面展示和日志输出的中文描述
     * </p>
     */
    private final String name;

    /**
     * 私有构造器确保枚举实例的不可变性
     *
     * @param code 状态编码，要求全局唯一
     * @param name 状态描述，需与业务文档定义一致
     */
    private AssetStatusEnum(int code, String name) {
        // 初始化状态编码
        this.code = code;
        // 初始化状态描述
        this.name = name;
    }

    /**
     * 获取状态编码（通常用于数据库存储和接口传输）
     *
     * @return 状态对应的数字编码
     */
    public int getCode() {
        // 返回状态编码
        return code;
    }

    /**
     * 获取状态描述（通常用于界面展示和日志输出）
     *
     * @return 状态的中文描述
     */
    public String getName() {
        // 返回状态描述
        return name;
    }

    /**
     * 描述与枚举实例的映射关系，用于快速查找
     */
    private static final Map<String, AssetStatusEnum> DESCRIPTION_TO_ENUM_MAP = new HashMap<>();

    static {
        // 类加载时自动初始化状态映射表
        // 遍历所有枚举实例
        for (AssetStatusEnum assetType : AssetStatusEnum.values()) {
            // 将状态描述与枚举实例建立映射关系
            DESCRIPTION_TO_ENUM_MAP.put(assetType.getName(), assetType);
        }
    }

    /**
     * 获取所有状态描述与枚举实例的映射关系（返回不可修改的副本）
     * <p>
     * 典型应用场景：生成状态下拉列表时调用
     * </p>
     *
     * @return 包含完整映射关系的Map集合
     */
    public static Map<String, AssetStatusEnum> getAllDescriptions() {
        // 返回映射关系的副本，避免外部修改
        return new HashMap<>(DESCRIPTION_TO_ENUM_MAP);
    }

    /**
     * 根据状态描述获取对应枚举实例
     *
     * @param description 状态描述（需严格匹配枚举定义）
     * @return 对应的枚举实例，未找到时返回null
     * @throws IllegalArgumentException 当传入空值或无效描述时抛出
     */
    public static AssetStatusEnum fromDescription(String description) {
        // 检查传入的状态描述是否为null
        if (description == null) {
            // 如果为null，抛出非法参数异常
            throw new IllegalArgumentException("状态描述不能为null");
        }
        // 根据状态描述从映射表中获取对应的枚举实例
        return DESCRIPTION_TO_ENUM_MAP.get(description);
    }

    /**
     * 根据状态编码获取对应枚举实例
     *
     * @param code 状态编码（需与枚举定义一致）
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 当传入无效编码时抛出
     */
    public static AssetStatusEnum getByCode(int code) {
        // 遍历所有枚举实例
        for (AssetStatusEnum status : values()) {
            // 比较状态编码是否匹配
            if (status.getCode() == code) {
                // 如果匹配，返回对应的枚举实例
                return status;
            }
        }
        // 如果未找到匹配的编码，抛出非法参数异常
        throw new IllegalArgumentException("无效的状态码: " + code);
    }
}