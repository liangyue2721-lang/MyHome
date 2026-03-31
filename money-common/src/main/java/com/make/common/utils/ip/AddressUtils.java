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
 * IP地址解析工具类（主备双驱动）
 * <p>
 * 优先使用 ip-api.com（中文数据），失败时自动降级到 ipquery.io。
 * </p>
 */
public class AddressUtils {

    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    // 主服务：ip-api.com (中文，免费版不支持HTTPS)
    private static final String PRIMARY_URL = "http://ip-api.com/json/";

    // 备用服务：ipquery.io (支持HTTPS)
    private static final String SECONDARY_URL = "https://api.ipquery.io/";

    private static final String UNKNOWN = "XX XX";
    private static final String DEFAULT = "DEFAULT ADDRESS";

    /**
     * 根据IP地址获取真实地理位置（主备自动切换）
     *
     * @param ip 需要查询的IP地址
     * @return 地理位置字符串（省份 城市）
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

        // 3. 尝试主服务
        String location = tryPrimaryService(ip);
        if (!UNKNOWN.equals(location)) {
            return location;
        }

        // 4. 主服务失败，尝试备用服务
        location = trySecondaryService(ip);
        if (!UNKNOWN.equals(location)) {
            return location;
        }

        // 5. 所有服务均失败
        log.error("所有IP地理位置查询服务均失败，IP：{}", ip);
        return UNKNOWN;
    }

    /**
     * 调用主服务 ip-api.com
     */
    private static String tryPrimaryService(String ip) {
        try {
            String url = PRIMARY_URL + ip + "?lang=zh-CN";
            String response = HttpUtils.sendGet(url, null, Constants.UTF8); // 确保HttpUtils支持UTF-8

            if (StringUtils.isEmpty(response)) {
                log.warn("主服务响应为空，IP：{}", ip);
                return UNKNOWN;
            }

            JSONObject json = JSON.parseObject(response);
            // 检查接口返回状态
            if (!"success".equals(json.getString("status"))) {
                log.warn("主服务返回失败状态，IP：{}，响应：{}", ip, response);
                return UNKNOWN;
            }

            String region = json.getString("regionName"); // 省份
            String city = json.getString("city");         // 城市

            if (StringUtils.isEmpty(region) && StringUtils.isEmpty(city)) {
                log.warn("主服务解析结果为空，IP：{}，响应：{}", ip, response);
                return UNKNOWN;
            }

            return String.format("%s %s",
                    StringUtils.defaultIfEmpty(region, ""),
                    StringUtils.defaultIfEmpty(city, "")
            ).trim();

        } catch (Exception e) {
            log.warn("主服务调用异常，IP：{}，错误：{}", ip, e.getMessage());
            return UNKNOWN; // 异常时返回UNKNOWN，触发备用服务
        }
    }

    /**
     * 调用备用服务 ipquery.io
     */
    private static String trySecondaryService(String ip) {
        try {
            String url = SECONDARY_URL + ip;
            String response = HttpUtils.sendGet(url, null, Constants.UTF8);

            if (StringUtils.isEmpty(response)) {
                log.warn("备用服务响应为空，IP：{}", ip);
                return UNKNOWN;
            }

            JSONObject json = JSON.parseObject(response);
            JSONObject location = json.getJSONObject("location");
            if (location == null) {
                log.warn("备用服务返回数据缺少location字段，IP：{}，响应：{}", ip, response);
                return UNKNOWN;
            }

            String region = location.getString("state");   // 省份
            String city = location.getString("city");      // 城市

            if (StringUtils.isEmpty(region) && StringUtils.isEmpty(city)) {
                log.warn("备用服务解析结果为空，IP：{}，响应：{}", ip, response);
                return UNKNOWN;
            }

            return String.format("%s %s",
                    StringUtils.defaultIfEmpty(region, ""),
                    StringUtils.defaultIfEmpty(city, "")
            ).trim();

        } catch (Exception e) {
            log.warn("备用服务调用异常，IP：{}，错误：{}", ip, e.getMessage());
            return UNKNOWN;
        }
    }
}
