package com.chaoqer.common.util;

import com.chaoqer.common.entity.base.ClientType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.toby.rpc.entity.ErrorCode;
import vip.toby.rpc.entity.RpcResult;
import vip.toby.rpc.entity.ServerResult;
import vip.toby.rpc.entity.ServerStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * WEB端返回结果封装工具类
 *
 * @author toby
 */
public class ResponseUtil {

    private final static Logger logger = LoggerFactory.getLogger(ResponseUtil.class);

    /**
     * 返回错误码
     */
    public static String createHttpStatus(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) {
        if (response != null) {
            response.setStatus(errorCode.getCode());
            if (request != null && request.getAttribute("clientType") == ClientType.WEB && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                try {
                    String requestUrl = request.getRequestURL().toString();
                    String queryStr = request.getQueryString();
                    if (StringUtils.isNotBlank(queryStr)) {
                        queryStr = "?" + queryStr;
                    } else {
                        queryStr = "";
                    }
                    String url = URLEncoder.encode(requestUrl + queryStr, "UTF-8");
                    if (ErrorCode.getErrorCode(errorCode.getCode()) == ErrorCode.AUTHORIZED_FAILED) {
                        // TODO 跳转到登录页面
                        // response.sendRedirect(CommonUtil.getHost(request) + "h5/login.html?redirect_uri=" + url);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * 返回结果
     */
    public static String createRpcResult(HttpServletRequest request, HttpServletResponse response, RpcResult rpcResult) {
        ServerStatus serverStatus = rpcResult.getServerStatus();
        ServerResult serverResult = rpcResult.getServerResult();
        switch (serverStatus) {
            case NOT_EXIST:
                return createHttpStatus(request, response, ErrorCode.NOT_FOUND);
            case UNAVAILABLE:
                return createHttpStatus(request, response, ErrorCode.SERVICE_UNAVAILABLE);
            case FAILURE:
//                if (request != null && response != null && request.getAttribute("clientType") == ClientType.WEB && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With")) && OperateErrorCode.getOperateErrorCode(serverResult.getErrorCode()) == OperateErrorCode.USERINFO_NOT_COMPLETE) {
//                    // TODO 跳转到填写用户信息页面
//                    // response.sendRedirect(CommonUtil.getHost(request) + "h5/login.html?redirect_uri=" + url);
//                }
                return createHttpStatus(request, response, ErrorCode.INTERNAL_SERVER_ERROR);
            case SUCCESS:
                return serverResult.toString();
            default:
                break;
        }
        return createHttpStatus(request, response, ErrorCode.INTERNAL_SERVER_ERROR);
    }

}