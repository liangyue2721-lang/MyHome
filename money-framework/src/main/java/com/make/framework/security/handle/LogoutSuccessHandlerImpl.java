package com.make.framework.security.handle;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.make.common.constant.Constants;
import com.make.common.core.domain.AjaxResult;
import com.make.common.core.domain.model.LoginUser;
import com.make.common.utils.MessageUtils;
import com.make.common.utils.ServletUtils;
import com.make.common.utils.StringUtils;
import com.make.framework.manager.AsyncManager;
import com.make.framework.manager.factory.AsyncFactory;
import com.make.framework.web.service.TokenService;

/**
 * 自定义退出处理类 返回成功
 * 
 * @author ruoyi
 */
@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    @Autowired
    private TokenService tokenService;

    /**
     * 退出处理
     * 
     * @return
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            String userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            // 记录用户退出日志
            String message = getMessage("user.logout.success", "退出成功");
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, message));
        }
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(getMessage("user.logout.success", "退出成功"))));
    }
    
    /**
     * 获取国际化消息，如果不存在则返回默认消息
     * 
     * @param code 消息代码
     * @param defaultMessage 默认消息
     * @return 消息内容
     */
    private String getMessage(String code, String defaultMessage) {
        try {
            return MessageUtils.message(code);
        } catch (NoSuchMessageException e) {
            return defaultMessage;
        }
    }
}