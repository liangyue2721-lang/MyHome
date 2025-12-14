package com.make.quartz.config;

import com.make.common.util.TraceIdUtil;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * TraceId过滤器
 * 
 * 用于在请求处理前生成并设置TraceId，在请求处理完成后清除TraceId
 * 确保每个请求都有唯一的TraceId，便于日志追踪和问题排查
 */
@Component
public class TraceIdFilter implements Filter {

    /**
     * 过滤器初始化方法
     * 
     * @param filterConfig 过滤器配置
     * @throws ServletException Servlet异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化操作（如有需要）
    }

    /**
     * 核心过滤方法
     * 
     * @param request  Servlet请求
     * @param response Servlet响应
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // 生成并设置TraceId
        String traceId = generateTraceId(request);
        TraceIdUtil.putTraceId(traceId);
        
        try {
            // 继续执行过滤器链
            chain.doFilter(request, response);
        } finally {
            // 清除TraceId，防止内存泄漏
            TraceIdUtil.clearTraceId();
        }
    }

    /**
     * 生成TraceId
     * 
     * @param request Servlet请求
     * @return TraceId字符串
     */
    private String generateTraceId(ServletRequest request) {
        // 尝试从请求头中获取TraceId
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String traceId = httpRequest.getHeader("X-Trace-Id");
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
        }
        
        // 如果请求头中没有TraceId，则生成新的
        return TraceIdUtil.generateTraceId();
    }

    /**
     * 过滤器销毁方法
     */
    @Override
    public void destroy() {
        // 清理操作（如有需要）
    }
}