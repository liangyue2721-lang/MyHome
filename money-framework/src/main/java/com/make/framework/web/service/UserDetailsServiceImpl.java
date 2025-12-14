package com.make.framework.web.service;

import com.make.common.constant.CacheConstants;
import com.make.common.core.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.make.common.core.domain.entity.SysUser;
import com.make.common.core.domain.model.LoginUser;
import com.make.common.enums.UserStatus;
import com.make.common.exception.ServiceException;
import com.make.common.utils.MessageUtils;
import com.make.system.service.ISysUserService;

/**
 * 用户验证处理
 *
 * @author ruoyi
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 从用户名加载用户信息的服务实现。
     * <p>
     * 本方法优先从 Redis 缓存中读取用户信息，若缓存未命中，则回源到 MySQL 数据库查询，
     * 并将结果写入缓存以便后续请求复用，减轻数据库压力。
     * <p>
     * 同时，会对用户的状态（是否存在、是否被删除、是否被禁用）进行校验，
     * 并验证用户密码策略，最终返回封装好的 LoginUser 对象供 Spring Security 使用。
     *
     * @param username 用户名
     * @return Spring Security 的 UserDetails 对象
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 优先从 Redis 缓存中获取用户信息
        SysUser user = redisCache.getCacheObject(CacheConstants.USER_FIELD_PREFIX + username);

        // 2. 如果缓存未命中，则查询 MySQL 数据库，并将查询结果写入缓存
        if (user == null) {
            user = userService.selectUserByUserName(username);
            if (user != null) {
                // 将用户信息存入缓存，默认无过期时间，可在 redisCache 配置中调整
                redisCache.setCacheObject(CacheConstants.USER_FIELD_PREFIX + username, user);
            }
        }

        // 3. 校验用户合法性
        if (user == null) {
            // 用户不存在
            log.info("登录用户：{} 不存在.", username);
            throw new ServiceException(MessageUtils.message("user.not.exists"));
        } else if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            // 用户已被逻辑删除
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException(MessageUtils.message("user.password.delete"));
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            // 用户被停用
            log.info("登录用户：{} 已被停用.", username);
            throw new ServiceException(MessageUtils.message("user.blocked"));
        }

        // 4. 验证用户密码（主要用于登录错误次数、锁定策略等）
        passwordService.validate(user);

        // 5. 构建并返回 Spring Security 需要的 UserDetails 对象
        return createLoginUser(user);
    }

    /**
     * 创建 Spring Security 使用的 LoginUser 对象。
     *
     * @param user 系统用户实体
     * @return 封装了用户信息、部门信息及权限信息的 LoginUser
     */
    public UserDetails createLoginUser(SysUser user) {
        // LoginUser 会包含用户ID、部门ID、用户实体、菜单权限集合
        return new LoginUser(
                user.getUserId(),
                user.getDeptId(),
                user,
                permissionService.getMenuPermission(user)
        );
    }

}
