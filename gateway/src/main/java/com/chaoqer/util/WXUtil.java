package com.chaoqer.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.common.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WXUtil {

    private final static Logger logger = LoggerFactory.getLogger(WXUtil.class);

    public static String getOpenIdByCode(StringRedisTemplate redisTemplate, String code, String appId, String appSecret) {
        try {
            if (StringUtils.isNotBlank(code)) {
                String openIdKey = RedisKeyGenerator.getOpenIdCodeKey(code);
                String openId = RedisUtil.getString(redisTemplate, openIdKey);
                if (StringUtils.isNotBlank(openId)) {
                    return openId;
                }
                int n = 0;
                do {
                    String info = HttpClientUtil.get("https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId + "&secret=" + appSecret + "&code=" + code + "&grant_type=authorization_code", null);
                    openId = JSON.parseObject(info).getString("openid");
                } while (StringUtils.isBlank(openId) && n++ < 100);
                if (StringUtils.isNotBlank(openId)) {
                    RedisUtil.setObject(redisTemplate, openIdKey, openId, 30, TimeUnit.MINUTES);
                    return openId;
                } else {
                    logger.error("微信服务器异常, 无法获取openid");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    public static Map<String, String> getConfig(StringRedisTemplate redisTemplate, String appId, String appSecret, String url) {
        Map<String, String> config = new HashMap<>();
        config.put("nonceStr", DigestUtil.getUUID());
        config.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        config.put("url", url);
        config.put("jsapi_ticket", getCachedJsApiTicket(redisTemplate, appId, appSecret));
        config.put("signature", DigestUtil.sha1(CommonUtil.createLinkString(config)));
        config.put("appId", appId);
        return config;
    }

    private static String getCachedJsApiTicket(StringRedisTemplate redisTemplate, String appId, String appSecret) {
        String jsApiTicketKey = RedisKeyGenerator.getWXJsApiTicketKey();
        // 如果不存在，同步调用将信息存入缓存
        if (!RedisUtil.isKeyExist(redisTemplate, jsApiTicketKey)) {
            try {
                // 获取access_token
                String result = HttpClientUtil.get("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret, null);
                JSONObject data = JSON.parseObject(result);
                String accessToken = data.getString("access_token");
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getWXAccessTokenKey(), accessToken, 60 * 60 * 2);
                logger.info("Get WeiXin ACCESS_TOKEN Success, ACCESS_TOKEN: " + accessToken);

                // 获取jsapi_ticket
                result = HttpClientUtil.get("https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + accessToken + "&type=jsapi", null);
                data = JSON.parseObject(result);
                String jsapiTicket = data.getString("ticket");
                RedisUtil.setObject(redisTemplate, jsApiTicketKey, jsapiTicket, 60 * 60 * 2);
                logger.info("Get WeiXin JS_API_TICKET Success, JS_API_TICKET: " + jsapiTicket);
            } catch (Exception e) {
                logger.error("Get WeiXin JS_API_TICKET Error: " + e.getMessage());
            }
        }
        // 读缓存
        return RedisUtil.getString(redisTemplate, jsApiTicketKey);
    }
}
