package com.make.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪工具类
 * 
 * 用于生成全局唯一的追踪ID，便于在分布式系统中跟踪请求链路
 * 
 * @author make
 */
public class TraceIdUtil {
    
    private static final String TRACE_ID_KEY = "traceId";
    
    /**
     * 生成一个新的追踪ID
     * 
     * @return 基于UUID的追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
    
    /**
     * 将追踪ID放入MDC中，便于日志输出
     * 
     * @param traceId 追踪ID
     */
    public static void putTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }
    
    /**
     * 从MDC中获取当前追踪ID
     * 
     * @return 当前追踪ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * 清除MDC中的追踪ID
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
}