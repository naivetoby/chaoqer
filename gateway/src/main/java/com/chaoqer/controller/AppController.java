package com.chaoqer.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.annotation.ParamVerifySkip;
import com.chaoqer.annotation.UserLoginSkip;
import com.chaoqer.client.app.AppClient;
import com.chaoqer.common.entity.app.FeedbackDTO;
import com.chaoqer.common.entity.app.ReportDTO;
import com.chaoqer.common.entity.app.VersionResult;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.ClientInfo;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.common.util.ResponseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.ServerResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "app", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class AppController {

    private final static Logger logger = LoggerFactory.getLogger(AppController.class);

    @Autowired
    private AppClient appClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取最新版本
     */
    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "version/upgrade", consumes = MediaType.ALL_VALUE)
    public String getLatestVersion(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestAttribute Authed authed
    ) {
        ClientInfo clientInfo = authed.getClientInfo();
        VersionResult latestVersionResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getAppLatestVersionKey(clientInfo.getClientType(), clientInfo.getVersionCode()), VersionResult.class);
        if (latestVersionResult != null) {
            if (latestVersionResult.getVersionCode() <= clientInfo.getVersionCode()) {
                return ServerResult.build(OperateStatus.FAILURE).message("当前已经是最新版本").toString();
            }
            return ServerResult.buildSuccessResult(latestVersionResult).toString();
        }
        return ResponseUtil.createRpcResult(request, response, appClient.getLatestVersion(authed.buildDTO(true)));
    }

    /**
     * 提交反馈
     */
    @RequestMapping(method = RequestMethod.POST, path = "feedback")
    public String postFeedback(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody FeedbackDTO feedbackDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, appClient.postFeedback(authed.buildDTO(feedbackDTO, true)));
    }

    /**
     * 提交投诉
     */
    @RequestMapping(method = RequestMethod.POST, path = "report")
    public String postReport(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody ReportDTO reportDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, appClient.postReport(authed.buildDTO(reportDTO)));
    }

    /**
     * 获取链接预览
     */
    @RequestMapping(method = RequestMethod.GET, path = "link/preview/{linkMD5:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE)
    public String getLinkPreview(
            @PathVariable String linkMD5
    ) {
        JSONObject result = null;
        String value = RedisUtil.getString(redisTemplate, RedisKeyGenerator.getAppLinkPreviewKey(linkMD5));
        if (StringUtils.isNotBlank(value)) {
            try {
                result = JSON.parseObject(value);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (result == null) {
            result = new JSONObject();
            result.put("title", "");
            result.put("description", "");
            result.put("image", "");
            result.put("url", "");
        }
        return ServerResult.buildSuccessResult(result).toString();
    }

}
