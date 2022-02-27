package com.chaoqer.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chaoqer.client.account.AccountClient;
import com.chaoqer.client.app.AppActiveLogAsyncClient;
import com.chaoqer.common.entity.app.AppUserActiveLogDTO;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.util.HttpContextUtils;
import com.chaoqer.util.WXUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import vip.toby.rpc.entity.ErrorCode;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * 微信拦截器
 *
 * @author TKJ
 */
@Component
public class H5LoginInterceptor extends HandlerInterceptorAdapter {

    private final static Logger logger = LoggerFactory.getLogger(H5LoginInterceptor.class);

    @Value("${wx.appId}")
    private String appId;
    @Value("${wx.appSecret}")
    private String appSecret;

    @Autowired
    private AccountClient accountClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AppActiveLogAsyncClient appActiveLogAsyncClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length != 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase("chaoqer_wx_code")) {
                    String openId = RedisUtil.getString(redisTemplate, RedisKeyGenerator.getOpenIdCodeKey(cookie.getValue()));
                    if (StringUtils.isNotBlank(openId)) {
                        return true;
                    }
                    // 清理cookie
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        // 同步请求
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            String url = request.getRequestURL().toString();
            String queryStr = request.getQueryString();
            if (StringUtils.isNotBlank(queryStr)) {
                queryStr = "?" + queryStr;
            } else {
                queryStr = "";
            }
            String code = request.getParameter("code");
            if (StringUtils.isBlank(code)) {
                response.sendRedirect(getWXRedirectUrl(url + queryStr));
                return false;
            }
            String openId = WXUtil.getOpenIdByCode(redisTemplate, code, appId, appSecret);
            // 不存在openID
            if (StringUtils.isBlank(openId)) {
                response.sendRedirect(getWXRedirectUrl(url + queryStr));
                return false;
            }
            // 存入cookie
            String wxCode = DigestUtil.getUUID();
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getOpenIdCodeKey(wxCode), openId, 30, TimeUnit.DAYS);
            Cookie cookie = new Cookie("chaoqer_wx_code", wxCode);
            cookie.setPath(request.getContextPath() + "/");
            cookie.setMaxAge(60 * 60 * 24 * 30);
            cookie.setDomain(CommonUtil.getDomain(request));
            response.addCookie(cookie);
            response.sendRedirect(CommonUtil.nullToDefault(request.getParameter("redirect_uri"), CommonUtil.getHost(request)));
            return false;
        }
        response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
        return false;
    }

    private String getWXRedirectUrl(String url) {
        try {
            return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appId + "&redirect_uri=" + URLEncoder.encode(url, "UTF-8") + "&response_type=code&scope=snsapi_base#wechat_redirect";
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        return url;
    }

    // 记录用户活跃日志
    private void createUserActiveLog(HttpServletRequest request, Authed authed) {
        // 判断参数的提交类型
        Map<String, String[]> params = new HashMap<>();
        String contentType = request.getHeader("Content-Type");
        // JSON类型的参数
        if (APPLICATION_JSON_VALUE.equals(contentType)) {
            JSONObject parameterMap = JSON.parseObject(HttpContextUtils.getBodyString(request));
            for (String key : parameterMap.keySet()) {
                Object value = parameterMap.get(key);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    params.put(key, new String[]{JSONObject.toJSONString(value, SerializerFeature.MapSortField)});
                } else {
                    params.put(key, new String[]{parameterMap.getString(key)});
                }
            }
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (String key : parameterMap.keySet()) {
                params.put(key, parameterMap.get(key));
            }
        }
        AppUserActiveLogDTO appUserActiveLogDTO = new AppUserActiveLogDTO();
        appUserActiveLogDTO.setApiPath(request.getRequestURI());
        appUserActiveLogDTO.setParameterMap(params);
        appUserActiveLogDTO.setIp(request.getAttribute("ip").toString());
        appActiveLogAsyncClient.createAppUserActiveLog(authed.buildDTO(appUserActiveLogDTO, true));
    }

}
