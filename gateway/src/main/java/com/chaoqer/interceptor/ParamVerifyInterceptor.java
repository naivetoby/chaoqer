package com.chaoqer.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chaoqer.annotation.ParamVerifySkip;
import com.chaoqer.client.account.AccountClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.util.HttpContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import vip.toby.rpc.entity.ErrorCode;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcResult;
import vip.toby.rpc.entity.ServerStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * 参数校验拦截器
 *
 * @author toby
 */
@Component
public class ParamVerifyInterceptor extends HandlerInterceptorAdapter {

    private final static Logger logger = LoggerFactory.getLogger(ParamVerifyInterceptor.class);

    @Value("${gateway.verify.accessSecret}")
    private String verifyDefaultAccessSecret;
    @Value("${gateway.verify.delimiter}")
    private String verifyDelimiter;
    @Autowired
    private AccountClient accountClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 跳过 LoginSkip 的注解方法
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        ParamVerifySkip paramVerifySkip = handlerMethod.getMethodAnnotation(ParamVerifySkip.class);
        if (paramVerifySkip != null) {
            return true;
        }
        String accessSecret;
        // 获取认证信息
        Authed authed = (Authed) request.getAttribute("authed");
        // 已通过认证并且是用户登录
        if (authed != null && authed.isUserLogin()) {
            String accountSecretKey = RedisKeyGenerator.getAccountSecretKey(authed.getAuthedUid());
            if (RedisUtil.isKeyExist(redisTemplate, accountSecretKey)) {
                accessSecret = RedisUtil.getString(redisTemplate, accountSecretKey);
            } else {
                RpcResult rpcResult = accountClient.getAccountSecret(authed.buildDTO());
                if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    accessSecret = rpcResult.getServerResult().getResult().toString();
                } else {
                    response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
                    return false;
                }
            }
        } else {
            // 开放接口, 使用默认accessSecret
            accessSecret = verifyDefaultAccessSecret;
        }
        // 判断参数的提交类型
        Map<String, String> params = new HashMap<>();
        String contentType = request.getHeader("Content-Type");
        // JSON类型的参数
        if (APPLICATION_JSON_VALUE.equals(contentType)) {
            JSONObject parameterMap = JSON.parseObject(HttpContextUtils.getBodyString(request));
            for (String key : parameterMap.keySet()) {
                Object value = parameterMap.get(key);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    params.put(key, JSONObject.toJSONString(value, SerializerFeature.MapSortField));
                } else {
                    params.put(key, parameterMap.getString(key));
                }
            }
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (String key : parameterMap.keySet()) {
                params.put(key, parameterMap.get(key)[0]);
            }
        }
        if (!params.containsKey("sign")) {
            response.setStatus(ErrorCode.PARAMS_NOT_VALID.getCode());
            return false;
        }
        if (!params.containsKey("ts")) {
            response.setStatus(ErrorCode.PARAMS_NOT_VALID.getCode());
            return false;
        }
        String sign = params.get("sign").replace("\n", "");
        params.remove("sign");
        if (!sign.equalsIgnoreCase(getSign(params, accessSecret))) {
            response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
            return false;
        }
        return true;

    }

    private String getSign(Map<String, String> params, String accessSecret) {
        List<String> paramList = new ArrayList<>();
        for (String key : params.keySet()) {
            paramList.add(String.format("%s=%s", key, params.get(key)));
        }
        // 排序
        Collections.sort(paramList);
        paramList.add(String.format("accessSecret=%s", accessSecret));
        // 加密
        String paramString = StringUtils.join(paramList, verifyDelimiter);
        return DigestUtil.md5(paramString);
    }

}
