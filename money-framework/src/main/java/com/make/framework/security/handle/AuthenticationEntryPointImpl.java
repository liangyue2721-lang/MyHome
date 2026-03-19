package com.make.framework.security.handle;

import java.io.IOException;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.make.common.utils.ip.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.make.common.constant.HttpStatus;
import com.make.common.core.domain.AjaxResult;
import com.make.common.utils.ServletUtils;
import com.make.common.utils.StringUtils;

/**
 * 认证失败处理类 返回未授权
 *
 * @author ruoyi
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable
{
    private static final long serialVersionUID = -8970718410437077606L;

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEntryPointImpl.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException
    {
        int code = HttpStatus.UNAUTHORIZED;
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());

        // 记录详细的认证失败日志
        log.error("\n========== 认证失败 ==========\n" +
                        "【请求 URL】: {}\n" +
                        "【请求方式】: {}\n" +
                        "【IP 地址】: {}\n" +
                        "【User-Agent】: {}\n" +
                        "【Authorization】: {}\n" +
                        "【异常信息】: {}\n" +
                        "==================================",
                request.getRequestURI(),
                request.getMethod(),
                IpUtils.getIpAddr(request),
                request.getHeader("User-Agent"),
                StringUtils.defaultString(request.getHeader("Authorization"), "N/A"),
                e != null ? e.getMessage() : "Unknown authentication error");

        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
