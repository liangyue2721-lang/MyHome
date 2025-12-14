package com.make.framework.web.service;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.make.framework.manager.AsyncManager;
import com.make.framework.manager.factory.AsyncFactory;
import com.make.framework.security.context.AuthenticationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.make.common.constant.CacheConstants;
import com.make.common.constant.Constants;
import com.make.common.constant.UserConstants;
import com.make.common.core.domain.entity.SysUser;
import com.make.common.core.domain.model.LoginUser;
import com.make.common.core.redis.RedisCache;
import com.make.common.exception.ServiceException;
import com.make.common.exception.user.BlackListException;
import com.make.common.exception.user.CaptchaException;
import com.make.common.exception.user.CaptchaExpireException;
import com.make.common.exception.user.UserNotExistsException;
import com.make.common.exception.user.UserPasswordNotMatchException;
import com.make.common.utils.DateUtils;
import com.make.common.utils.MessageUtils;
import com.make.common.utils.StringUtils;
import com.make.common.utils.ip.IpUtils;
import com.make.system.service.ISysConfigService;
import com.make.system.service.ISysUserService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
public class SysLoginService {
    private static final Logger log = LoggerFactory.getLogger(SysLoginService.class);
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;
    // Redis 缓存键前缀：用于记录每个用户当前登录 token 的映射

    /**
     * 登录验证（支持同账号新登录挤掉旧登录）
     *
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 生成的新 token
     */
    public String login(String username, String password, String code, String uuid) {
        // 1. 校验验证码
        validateCaptcha(username, code, uuid);

        // 2. 登录前置校验（用户名/密码长度、IP 黑名单等）
        loginPreCheck(username, password);

        // 3. 用户验证
        Authentication authentication;
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 调用 UserDetailsServiceImpl.loadUserByUsername 进行用户加载和校验
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                // 记录失败日志
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(
                        username, Constants.LOGIN_FAIL,
                        getMessage("user.password.not.match", "密码错误")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(
                        username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        } finally {
            AuthenticationContextHolder.clearContext();
        }

        // 4. 登录成功记录日志
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(
                username, Constants.LOGIN_SUCCESS,
                getMessage("user.login.success", "登录成功")));

        // 5. 获取当前用户对象
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 6. 记录最后登录信息（IP、时间等）
        recordLoginInfo(loginUser.getUserId());

        // 7. 检查是否已有旧的登录 token，如果有则挤掉旧会话
        String oldToken = redisCache.getCacheObject(CacheConstants.USER_LOGIN_TOKEN_PREFIX + loginUser.getUserId());
        if (StringUtils.isNotEmpty(oldToken)) {
            // 删除旧 token 对应的登录缓存，使之前的会话失效
            String oldTok = extractLoginUserKeyFromJWT(oldToken);
            tokenService.delLoginUser(oldTok);
            if (null != oldTok && null != redisCache.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + oldTok)) {
                redisCache.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + oldTok);
            }

        }

        // 8. 生成新的 token
        String newToken = tokenService.createToken(loginUser);

        // 9. 记录当前用户的 token 映射，方便下次登录时挤掉旧 token
        redisCache.setCacheObject(CacheConstants.USER_LOGIN_TOKEN_PREFIX + loginUser.getUserId(), newToken);

        // 10. 返回新 token
        return newToken;
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

    /**
     * 从JWT令牌中提取login_user_key字段值
     * <p>
     * 该方法会验证JWT格式，解析Payload部分，并获取指定的用户标识字段
     *
     * @param jwt 待解析的JWT令牌字符串
     * @return 解析成功返回login_user_key字段值；格式错误/字段不存在时返回null
     * @implNote 处理流程：
     * 1. 拆分JWT三部分
     * 2. Base64Url解码Payload
     * 3. 解析JSON获取目标字段
     * @implWarning 注意：
     * - 不验证签名有效性
     * - 不校验时效性
     * - 仅支持UTF-8编码
     */
    public static String extractLoginUserKeyFromJWT(String jwt) {
        try {
            // 1. 拆分JWT为头部、载荷、签名三部分
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("非法的 JWT 格式，应包含三部分");
            }

            // 2. 对Payload段进行Base64Url解码（RFC 4648）
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            // 3. 解析JSON并定位目标字段
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = mapper.readTree(payloadJson);
            JsonNode keyNode = payload.path("login_user_key");  // 使用path避免NPE

            // 4. 校验字段是否存在
            if (keyNode.isMissingNode()) {
                throw new IllegalArgumentException("Payload中不存在'login_user_key'字段");
            }

            return keyNode.asText();

        } catch (Exception e) {
            // 5. 异常处理：记录详细错误日志并返回null
            log.error("【JWT解析失败】无法提取 login_user_key，Token：{}，异常信息：{}",
                    jwt, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid) {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled) {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha)) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     *
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr())) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }
}
