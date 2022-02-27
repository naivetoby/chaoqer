package com.chaoqer.controller;

import com.alibaba.fastjson.JSONObject;
import com.chaoqer.annotation.ParamVerifySkip;
import com.chaoqer.annotation.UserLoginSkip;
import com.chaoqer.client.push.PushAsyncClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.push.AlertMessageDTO;
import com.chaoqer.common.entity.push.AttachMessageDTO;
import com.chaoqer.common.entity.push.JPushIdDTO;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.ServerResult;

@Controller
@RequestMapping("push")
public class PushController {

    private final static Logger logger = LoggerFactory.getLogger(PushController.class);

    @Autowired
    private PushAsyncClient pushAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RequestMapping(method = RequestMethod.POST, path = "update/{jPushId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String updateDevice(
            @PathVariable String jPushId,
            JPushIdDTO jPushIdDTO,
            @RequestAttribute Authed authed
    ) {
        String uid = authed.getAuthedUid();
        jPushIdDTO.setJPushId(jPushId);
        // 是否暂时已禁用updateJPushDevice API
        String jPushUpdateApiDisableKey = RedisKeyGenerator.getJPushUpdateApiDisableKey();
        if (RedisUtil.isKeyExist(redisTemplate, jPushUpdateApiDisableKey)) {
            logger.warn("更新JPush设备API暂时被禁用, uid: {}, jPushId: {}", uid, jPushId);
            return ServerResult.build(OperateStatus.SUCCESS).toString();
        }
        // 增加缓存
        String jPushUserDeviceIdKey = RedisKeyGenerator.getJPushUserDeviceIdKey(uid);
        if (StringUtils.isNotBlank(jPushId) && !jPushId.equals(RedisUtil.getString(redisTemplate, jPushUserDeviceIdKey))) {
            pushAsyncClient.updateDevice(authed.buildDTO(jPushIdDTO));
        } else {
            // 已上传, 忽略
            logger.info("更新JPush设备已忽略, uid: {}, jPushId: {}", uid, jPushId);
        }
        return ServerResult.build(OperateStatus.SUCCESS).toString();
    }

    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET)
    public String getIndex() {
        return "push";
    }

    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.POST, path = "template", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public String pushTemplateMessage(
            @RequestParam String uid,
            @RequestParam int originPushType,
            @RequestParam(required = false, defaultValue = "") String originUid,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String url,
            @RequestParam int store,
            @RequestParam int alert
    ) {
        AlertMessageDTO alertMessageDTO = new AlertMessageDTO();
        alertMessageDTO.setUid(uid);
        alertMessageDTO.setOriginPushType(originPushType);
        alertMessageDTO.setOriginUid(originUid);
        alertMessageDTO.setTitle(title);
        alertMessageDTO.setContent(escapesJavaScript(content));
        alertMessageDTO.setUrl(url);
        alertMessageDTO.setStore(store);
        alertMessageDTO.setAlert(alert);
        pushAsyncClient.pushAlertMessage(alertMessageDTO);
        return "推送成功";
    }

    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.POST, path = "attach", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public String pushAttachMessage(
            @RequestParam String uid,
            @RequestParam int originPushType,
            @RequestParam(required = false, defaultValue = "") String originUid,
            @RequestParam int attachType,
            @RequestParam(required = false, defaultValue = "") String attachBody,
            @RequestParam int store
    ) {
        AttachMessageDTO attachMessageDTO = new AttachMessageDTO();
        attachMessageDTO.setUid(uid);
        attachMessageDTO.setOriginPushType(originPushType);
        attachMessageDTO.setOriginUid(originUid);
        attachMessageDTO.setAttachType(attachType);
        JSONObject attachBodyJSON = null;
        if (StringUtils.isNotBlank(attachBody)) {
            try {
                attachBodyJSON = JSONObject.parseObject(attachBody);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (attachBodyJSON != null) {
            attachMessageDTO.setAttachBody(attachBodyJSON);
        }
        attachMessageDTO.setStore(store);
        pushAsyncClient.pushAttachMessage(attachMessageDTO);
        return "推送成功";
    }

    private String escapesJavaScript(String content) {
        content = content.replace("\u0008", " ");
        content = content.replace("\u0009", " ");
        content = content.replace("\u000B", " ");
        content = content.replace("\u000C", " ");
        content = content.replace("\u00A0", " ");
        content = content.replace("\u2028", "\n");
        content = content.replace("\u2029", "\n");
        content = content.replace("\uFEFF", " ");
        content = content.replace("\r", "\n").replace("\\r", "\n").replace("\\n", "\n");
        return content;
    }

}
