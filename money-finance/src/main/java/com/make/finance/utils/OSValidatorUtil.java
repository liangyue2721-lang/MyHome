package com.make.finance.utils;

/**
 * 操作系统验证工具类
 * <p>
 * 提供对当前运行环境操作系统的判断功能，主要用于处理不同操作系统间的兼容性问题
 * </p>
 */
public class OSValidatorUtil {

    /**
     * 判断当前系统是否是 Windows
     * <p>
     * 通过获取系统属性"os.name"并转换为小写来判断是否包含"windows"字符串，
     * 从而确定当前操作系统是否为Windows系列
     * </p>
     *
     * @return 如果是 Windows 系统返回 true，否则返回 false
     */
    public static boolean isWindows() {
        // 获取操作系统名称系统属性并转换为小写
        String osName = System.getProperty("os.name").toLowerCase();
        // 判断操作系统名称是否包含"windows"字符串
        return osName.contains("windows");
    }
}