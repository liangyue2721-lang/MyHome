package com.make.common.constant;

/**
 * 缓存的key 常量
 *
 * @author ruoyi
 */
public class CacheConstants {
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 通用缓存工具类，支持任意对象类型的缓存操作 redis key
     */
    public static final String GENERIC_CACHE_KEY =  "generic_cache_pre:";

    /**
     * 登录用户 token 映射前缀。
     * Key 格式：user:login:username:<username>
     * 存储用户登录后对应 token。
     * 推荐设置过期时间（例如 30 分钟或按登录策略）。
     */
    public static final String USER_LOGIN_TOKEN_PREFIX = "user:login:username_";

    /**
     * 其他用户相关缓存前缀。
     * Key 格式：user:username:<username>:<field>
     * 用于缓存用户信息中某个字段（如 profile, settings 等）。
     * 示例：user:username:alice:profile -> 用户 alice 的 profile 信息
     */
    public static final String USER_FIELD_PREFIX = "user:username_";

    /**
     * 首页数据缓存前缀（用于存储首页展示内容的整体快照）。
     * 推荐 Key 模式：cache:homepage:data
     * 例如：
     *   cache:homepage:data
     *   cache:homepage:data:lang:en
     *   （可根据需要加可选字段，如语言、版本号等）
     *
     * 设置合理的 TTL，例如几分钟至十几分钟，以适合首页内容的更新频率。
     */
    public static final String HOMEPAGE_CACHE_PREFIX = "finance:";
    
    /**
     * 实时股票数据 Redis 缓存键名
     * <p>
     * 用于存储所有实时股票数据的Redis键名
     * </p>
     */
    public static final String REALTIME_STOCK_ALL_KEY = "stock:realtime:all";
    
    /**
     * 实时股票数据 Redis 缓存键名
     * <p>
     * 用于存储东方财富实时股票数据的Redis键名
     * </p>
     */
    public static final String REALTIME_STOCK_ALL_DONGFANG_KEY = "stock:realtime:dongfang";
    
    /**
     * 实时股票数据 Redis 缓存键名
     * <p>
     * 用于存储单个股票实时数据的Redis键名前缀
     * </p>
     */
    public static final String REALTIME_STOCK_SINGLE_KEY = "REALTIME_STOCK_SINGLE:";
    
    /**
     * 实时股票代码集合 Redis 缓存键名
     * <p>
     * 用于存储所有股票代码的Redis集合键名
     * </p>
     */
    public static final String REALTIME_STOCK_CODES_KEY = "REALTIME_STOCK_CODES";
}