package com.chaoqer.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chaoqer.annotation.UserLoginSkip;
import com.chaoqer.client.account.AccountClient;
import com.chaoqer.client.app.AppActiveLogAsyncClient;
import com.chaoqer.common.entity.account.AccountAuthDTO;
import com.chaoqer.common.entity.app.AppUserActiveLogDTO;
import com.chaoqer.common.entity.base.AccountStatus;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.ClientInfo;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.util.HttpContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import vip.toby.rpc.entity.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * 用户登录拦截器
 *
 * @author toby
 */
@Component
public class UserLoginInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private AccountClient accountClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AppActiveLogAsyncClient appActiveLogAsyncClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 获取客户端信息
        ClientInfo clientInfo = (ClientInfo) request.getAttribute("clientInfo");
        // 跳过 UserLoginSkip 的注解方法
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization)) {
            UserLoginSkip userLoginSkip = handlerMethod.getMethodAnnotation(UserLoginSkip.class);
            if (userLoginSkip != null) {
                // 传递认证信息
                Authed authed = new Authed();
                authed.setClientInfo(clientInfo);
                request.setAttribute("authed", authed);
                // 记录用户活跃日志
                createUserActiveLog(request, authed);
                return true;
            }
            response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
            return false;
        }
        String uidAndTokenStr = DigestUtil.base64Decode(authorization.substring(6));
        String[] uidAndToken = uidAndTokenStr.split(":");
        if (uidAndToken.length == 2) {
            String uid = uidAndToken[0];
            String token = uidAndToken[1];
            String accountTokenKey = RedisKeyGenerator.getAccountTokenKey(uid);
            String accountStatusKey = RedisKeyGenerator.getAccountStatusKey(uid);
            if (RedisUtil.isKeyExist(redisTemplate, accountTokenKey) && RedisUtil.isKeyExist(redisTemplate, accountStatusKey)) {
                // 校验token
                if (token.equals(RedisUtil.getString(redisTemplate, accountTokenKey))) {
                    // 校验用户状态
                    return accountStatusAuth(request, response, uid, clientInfo, accountStatusKey);
                }
                response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
                return false;
            }
            // 缓存不存在的情况, 远程调用
            AccountAuthDTO accountAuthDTO = new AccountAuthDTO();
            accountAuthDTO.setUid(uid);
            accountAuthDTO.setToken(token);
            RpcResult rpcResult = accountClient.accountAuth(accountAuthDTO);
            if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                // 校验账号状态
                if (RedisUtil.isKeyExist(redisTemplate, accountStatusKey)) {
                    return accountStatusAuth(request, response, uid, clientInfo, accountStatusKey);
                }
            }
        }
        response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
        return false;
    }

    // 校验账号状态
    private boolean accountStatusAuth(HttpServletRequest request, HttpServletResponse response, String uid, ClientInfo clientInfo, String accountStatusKey) throws IOException {
        AccountStatus accountStatus = AccountStatus.getAccountStatus(Integer.parseInt(RedisUtil.getString(redisTemplate, accountStatusKey)));
        // 验证通过
        if (accountStatus == AccountStatus.NORMAL) {
            // 传递认证信息
            Authed authed = new Authed();
            authed.setAuthedUid(uid);
            authed.setClientInfo(clientInfo);
            request.setAttribute("authed", authed);
            // 记录用户活跃日志
            createUserActiveLog(request, authed);
            return true;
        }
        // 用户资料未完善
        if (accountStatus == AccountStatus.DEFAULT) {
            // 正初始化用户资料 或 退出账号 或 上传图片
            if (request.getRequestURI().equals("/user/profile/init") || request.getRequestURI().equals("/account/logout") || request.getRequestURI().equals("/aliyun/image/upload") || request.getRequestURI().equals("/user/message/unread_total") || request.getRequestURI().startsWith("/push/update/")) {
                // 传递认证信息
                Authed authed = new Authed();
                authed.setAuthedUid(uid);
                authed.setClientInfo(clientInfo);
                request.setAttribute("authed", authed);
                // 记录用户活跃日志
                createUserActiveLog(request, authed);
                return true;
            }
            // 获取其他数据
            if (clientInfo.getVersionCode() <= 26) {
                // 旧版本
                if (request.getMethod().equalsIgnoreCase("GET") && request.getRequestURI().equals("/club") || request.getRequestURI().equals("/event")) {
                    ServerResult serverResult = ServerResult.build(OperateStatus.SUCCESS);
                    response.setContentType(APPLICATION_JSON_VALUE);
                    PrintWriter writer = response.getWriter();
                    writer.print(serverResult.toString());
                    writer.close();
                    response.flushBuffer();
                    return false;
                }
            } else {
                // 新版本
                if (request.getMethod().equalsIgnoreCase("GET") && request.getRequestURI().equals("/room") || request.getRequestURI().equals("/club") || request.getRequestURI().equals("/event")) {
                    ServerResult serverResult = ServerResult.build(OperateStatus.SUCCESS);
                    response.setContentType(APPLICATION_JSON_VALUE);
                    PrintWriter writer = response.getWriter();
                    writer.print(serverResult.toString());
                    writer.close();
                    response.flushBuffer();
                    return false;
                }
            }
            // 返回错误码
            ServerResult serverResult = ServerResult.buildFailureMessage(OperateErrorCode.USER_PROFILE_NOT_COMPLETE.getMessage()).errorCode(OperateErrorCode.USER_PROFILE_NOT_COMPLETE.getCode());
            response.setContentType(APPLICATION_JSON_VALUE);
            PrintWriter writer = response.getWriter();
            writer.print(serverResult.toString());
            writer.close();
            response.flushBuffer();
            return false;
        }
        response.setStatus(ErrorCode.AUTHORIZED_FAILED.getCode());
        return false;
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
