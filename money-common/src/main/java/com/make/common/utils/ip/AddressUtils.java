package com.make.common.utils.ip;

import com.make.common.config.RuoYiConfig;
import com.make.common.constant.Constants;
import com.make.common.utils.StringUtils;
import com.make.common.utils.http.HttpUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP地址解析工具类
 * <p>
 * 用于通过公网IP地址查询其归属地信息（省份、城市）。
 * 对于内网IP或无法查询的情况，返回预定义默认值。
 * </p>
 *
 * <p>依赖服务：
 * 使用中国电信（pconline）的 whois 服务接口：
 * {@code http://whois.pconline.com.cn/ipJson.jsp?ip=<ip>&json=true}</p>
 *
 * @author 12
 * @version 1.1
 * @since 2025-10-09
 */
public class AddressUtils {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    /**
     * IP地址查询接口
     */
    private static final String IP_URL = "http://whois.pconline.com.cn/ipJson.jsp";

    /**
     * 未知地址常量
     */
    private static final String UNKNOWN = "XX XX";

    /**
     * 默认返回地址常量
     */
    private static final String DEFAULT = "DEFAULT ADDRESS";

    /**
     * 根据IP地址获取真实地理位置
     *
     * @param ip 需要查询的IP地址
     * @return IP地址对应的地理位置（省份 + 城市），若无法获取则返回默认地址或“内网IP”
     */
    public static String getRealAddressByIP(String ip) {
        // 1. 内网地址直接返回
        if (IpUtils.internalIp(ip)) {
            return "内网IP";
        }

        // 2. 若配置未启用地址查询功能，则直接返回默认值
        if (!RuoYiConfig.isAddressEnabled()) {
            log.debug("地址查询功能未启用，返回默认地址。IP：{}", ip);
            return DEFAULT;
        }

        try {
            // 3. 构建请求参数并发送HTTP请求
            String params = "ip=" + ip + "&json=true";
            String response = HttpUtils.sendGet(IP_URL, params, Constants.GBK);

            // 4. 检查响应内容
            if (StringUtils.isEmpty(response)) {
                log.warn("地理位置查询接口无响应或返回空数据，IP：{}", ip);
                return UNKNOWN;
            }

            // 5. 解析JSON结果
            JSONObject json = JSON.parseObject(response);
            String region = json.getString("pro");   // 省份
            String city = json.getString("city");    // 城市

            // 6. 拼接返回结果
            if (StringUtils.isEmpty(region) && StringUtils.isEmpty(city)) {
                log.warn("地理位置解析结果为空，IP：{}，原始响应：{}", ip, response);
                return UNKNOWN;
            }

            return String.format("%s %s",
                    StringUtils.defaultIfEmpty(region, ""),
                    StringUtils.defaultIfEmpty(city, "")
            ).trim();

        } catch (Exception e) {
            // 7. 异常捕获与日志记录
            log.error("获取IP地理位置异常，IP：{}，错误信息：{}", ip, e.getMessage(), e);
            return UNKNOWN;
        }
    }
}
