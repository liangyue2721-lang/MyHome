package com.make.common.filter;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.make.common.utils.StringUtils;
import com.make.common.utils.ip.IpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 全局请求日志过滤器
 * 作用：拦截进入系统的所有 HTTP 请求，在控制台/日志文件中打印详细的请求报文与响应状态。
 * * 【避坑指南】：
 * 1. @Order(100)：这里的优先级必须低于若依自带的 RepeatableFilter（建议设为 -100）。
 * 必须等 RepeatableFilter 把流包装成 RepeatedlyRequestWrapper 后，本过滤器才能安全读取流。
 * 2. 绝对不能调用 close() 关流：本拦截器只做“旁路窥探”，不能阻断数据的正常流通。
 *
 * @author system
 */
@Component
@Order(100)
public class RequestLogFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    // Jackson 的 JSON 处理工具，用于将单行的 JSON 字符串格式化为漂亮的多行结构
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("====== 全局请求日志过滤器 (RequestLogFilter) 初始化完成 ======");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 仅拦截并处理 HTTP 类型的请求
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 1. 提取基础请求信息
            String method = httpRequest.getMethod();
            String requestURI = httpRequest.getRequestURI();
            String queryString = httpRequest.getQueryString();
            String remoteAddr = IpUtils.getIpAddr(httpRequest); // 获取经过 Nginx 穿透后的真实外网 IP
            String contentType = httpRequest.getContentType();

            // 2. 开始构建日志字符串
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n==================== 请求开始 ====================\n");
            logBuilder.append("【请求方式】: ").append(method).append("\n");
            logBuilder.append("【请求 URL】: ").append(requestURI).append("\n");

            // 如果 URL 后面带有 ?id=1 这种参数，也记录下来
            if (StringUtils.isNotEmpty(queryString)) {
                logBuilder.append("【请求参数】: ").append(queryString).append("\n");
            }

            // 3. 提取请求头 (安全起见，Token 和 Cookie 使用 *** 脱敏掩码处理，防止泄露)
            logBuilder.append("【请求头】: \n");
            logBuilder.append("  - User-Agent: ").append(StringUtils.defaultString(httpRequest.getHeader("User-Agent"), "N/A")).append("\n");
            logBuilder.append("  - Authorization: ").append(
                    StringUtils.isNotEmpty(httpRequest.getHeader("Authorization")) ? "Bearer *** (已隐藏)" : "N/A").append("\n");
            logBuilder.append("  - Cookie: ").append(
                    StringUtils.isNotEmpty(httpRequest.getHeader("Cookie")) ? "*** (已隐藏)" : "N/A").append("\n");

            logBuilder.append("【IP 地址】: ").append(remoteAddr).append("\n");
            logBuilder.append("【Content-Type】: ").append(StringUtils.defaultString(contentType, "N/A")).append("\n");

            // 4. 读取请求体 (Body)
            // 规定：只读取 POST/PUT/PATCH 操作，且数据格式必须为 JSON (忽略文件上传等二进制流，防止内存溢出)
            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))
                    && StringUtils.isNotEmpty(contentType)
                    && contentType.contains("application/json")) {

                try {
                    String requestBody = getRequestBody(httpRequest);
                    if (StringUtils.isNotEmpty(requestBody)) {
                        // 尝试将枯燥的单行 JSON 字符串美化 (Pretty Print)
                        Object jsonObj = objectMapper.readTree(requestBody);
                        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
                        logBuilder.append("【请求体 (Body)】: \n").append(prettyJson).append("\n");
                    }
                } catch (Exception e) {
                    logBuilder.append("【请求体 (Body)】: [解析失败或数据非标准 JSON]\n");
                    log.debug("读取/格式化请求体失败: {}", e.getMessage());
                }
            }

            // 5. 记录开始时间，放行请求进入真正的业务代码 (Controller)
            long startTime = System.currentTimeMillis();
            try {
                // 【核心放行】：将请求交给后续的 Filter 和 Controller 去处理
                chain.doFilter(request, response);

                // 请求处理完毕，准备返回响应给前端
                long executionTime = System.currentTimeMillis() - startTime;
                int status = httpResponse.getStatus();

                logBuilder.append("【响应状态码】: ").append(status).append("\n");
                logBuilder.append("【接口耗时】: ").append(executionTime).append(" ms\n");
                logBuilder.append("==================== 请求结束 ====================");

                // 根据响应的 HTTP 状态码，智能选择控制台日志打印的颜色/级别
                if (status >= 500) {
                    log.error(logBuilder.toString()); // 内部错误标红
                } else if (status >= 400) {
                    log.warn(logBuilder.toString());  // 认证失败、参数错误等标黄
                } else {
                    log.info(logBuilder.toString());  // 正常请求常规打印
                }

            } catch (Exception e) {
                // 如果 Controller 层抛出了未捕获的严重异常，在这里捕获并记录时间
                long executionTime = System.currentTimeMillis() - startTime;
                logBuilder.append("【接口耗时】: ").append(executionTime).append(" ms\n");
                logBuilder.append("【异常信息】: ").append(e.getMessage()).append("\n");
                logBuilder.append("==================== 请求异常 ====================");
                log.error(logBuilder.toString(), e);

                // 记录完日志后，继续向上抛出，让全局异常处理器 (GlobalExceptionHandler) 去处理
                throw e;
            }

        } else {
            // 如果不是 HTTP 请求（极少见情况），直接放行
            chain.doFilter(request, response);
        }
    }

    /**
     * 安全地读取请求体内容
     * * @param request HTTP 请求对象 (此时应已经是 RepeatedlyRequestWrapper)
     *
     * @return 请求体字符串
     * @throws IOException
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;

        // 【严禁使用 try-with-resources】
        // 这里的 reader 绝对不能调用 close() 方法关闭！
        // 因为这是一个包装后的内存流，后续的 Controller (@RequestBody) 还需要再次读取它。
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    @Override
    public void destroy() {
        log.info("====== 全局请求日志过滤器 (RequestLogFilter) 已销毁 ======");
    }
}
