package com.make.finance.enums;

/**
 * CSV文件处理状态枚举类
 * <p>
 * 定义CSV文件处理过程中可能出现的各种状态信息
 * </p>
 *
 * @author 84522
 */
public enum CsvCodeEnum {
    /**
     * 客户端运行状态
     * <p>
     * 表示CSV处理客户端当前处于正常运行状态
     * </p>
     */
    CSV_ENUM_RUNNING("running"),

    /**
     * 客户端连接异常状态
     * <p>
     * 表示无法连接到CSV处理客户端的异常状态
     * </p>
     */
    CSV_ENUM_CLIENT_EXCEPTION("Can not connect to client!"),

    /**
     * Future获取异常状态
     * <p>
     * 表示在获取异步处理结果时发生异常的状态
     * </p>
     */
    CSV_ENUM_FUTURE_GET("Future Get");

    /**
     * 状态内容描述
     * <p>
     * 用于表示当前状态的详细描述信息
     * </p>
     */
    private String concent;

    /**
     * 构造方法
     *
     * @param concent 状态内容描述
     */
    CsvCodeEnum(String concent) {
        // 初始化状态内容描述
        this.concent = concent;
    }

    /**
     * 获取状态内容描述
     *
     * @return 状态内容描述
     */
    public String getStr() {
        // 返回状态内容描述
        return concent;
    }
}