package com.chaoqer.interceptor;

import com.chaoqer.client.account.AccountClient;
import com.chaoqer.common.entity.base.ClientInfo;
import com.chaoqer.common.entity.base.ClientType;
import com.chaoqer.common.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 设备登录拦截器
 *
 * @author toby
 */
@Component
public class ClientLoginInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private AccountClient accountClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 客户端信息
        ClientInfo clientInfo = new ClientInfo();
        // 获取客户端类型, 默认为WEB
        ClientType clientType = ClientType.getClientType(Integer.parseInt(CommonUtil.nullToDefault(request.getHeader("Client-Type"), "0")));
        clientInfo.setClientType(clientType.getType());
        clientInfo.setVersionName(CommonUtil.nullToDefault(request.getHeader("Version-Name"), "UNKNOWN"));
        clientInfo.setVersionCode(Integer.parseInt(CommonUtil.nullToDefault(request.getHeader("Version-Code"), "0")));
        request.setAttribute("clientInfo", clientInfo);
        request.setAttribute("ip", getClientIp(request));
        return true;
    }

    /**
     * 兼容nginx的ip获取
     */
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Real-IP");
        if (StringUtils.isBlank(clientIp) || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
            if (StringUtils.isBlank(clientIp)) {
                clientIp = "未知IP";
            }
            // 解决某些机器获取IP异常的bug
            if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
                clientIp = "127.0.0.1";
            }
        }
        return clientIp;
    }

}
