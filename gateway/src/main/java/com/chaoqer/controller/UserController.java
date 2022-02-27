package com.chaoqer.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.user.UserBlockClient;
import com.chaoqer.client.user.UserCardClient;
import com.chaoqer.client.user.UserMessageClient;
import com.chaoqer.client.user.UserProfileClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.push.UserMessageIdDTO;
import com.chaoqer.common.entity.push.UserMessagePageResult;
import com.chaoqer.common.entity.push.UserMessageResult;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileDTO;
import com.chaoqer.common.entity.user.UserProfilePageResult;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.entity.user.group.InitProfileGroup;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.common.util.ResponseUtil;
import com.chaoqer.entity.GatewayProperties;
import com.chaoqer.util.FixedUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcResult;
import vip.toby.rpc.entity.ServerResult;
import vip.toby.rpc.entity.ServerStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "user", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    @Autowired
    private UserCardClient userCardClient;
    @Autowired
    private UserBlockClient userBlockClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GatewayProperties gatewayProperties;
    @Autowired
    private UserProfileClient userProfileClient;
    @Autowired
    private UserMessageClient userMessageClient;

    /**
     * 初始化用户资料
     */
    @RequestMapping(method = RequestMethod.POST, path = "profile/init")
    public String postProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated({InitProfileGroup.class}) @RequestBody UserProfileDTO userProfileDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, userProfileClient.postProfile(authed.buildDTO(userProfileDTO)));
    }

    /**
     * 更新用户资料
     */
    @RequestMapping(method = RequestMethod.PUT, path = "profile")
    public String putProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody UserProfileDTO userProfileDTO,
            @RequestAttribute Authed authed
    ) {
        RpcResult userProfileRpcResult = userProfileClient.putProfile(authed.buildDTO(userProfileDTO));
        // 错误直接跳过
        if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class), gatewayProperties.getLiveUidList())).toString();
        }
        return ResponseUtil.createRpcResult(request, response, userProfileRpcResult);
    }

    /**
     * 获取用户资料
     */
    @RequestMapping(method = RequestMethod.GET, path = "{uid:[a-f0-9]{32}}/profile", consumes = MediaType.ALL_VALUE)
    public String getProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String uid,
            @RequestAttribute Authed authed
    ) {
        UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(uid), UserProfileResult.class);
        if (userProfileResult != null) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, userProfileResult, gatewayProperties.getLiveUidList())).toString();
        }
        UidDTO uidDTO = new UidDTO();
        uidDTO.setUid(uid);
        RpcResult userProfileRpcResult = userProfileClient.getProfile(authed.buildDTO(uidDTO));
        // 错误直接跳过
        if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class), gatewayProperties.getLiveUidList())).toString();
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode()).toString();
    }

    /**
     * 拉黑用户
     */
    @RequestMapping(method = RequestMethod.POST, path = "{uid:[a-f0-9]{32}}/block")
    public String postUserBlock(
            HttpServletRequest request,
            HttpServletResponse response,
            UidDTO uidDTO,
            @PathVariable String uid,
            @RequestAttribute Authed authed
    ) {
        uidDTO.setUid(uid);
        return ResponseUtil.createRpcResult(request, response, userBlockClient.postUserBlock(authed.buildDTO(uidDTO)));
    }

    /**
     * 取消拉黑用户
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "{uid:[a-f0-9]{32}}/block")
    public String deleteUserBlock(
            HttpServletRequest request,
            HttpServletResponse response,
            UidDTO uidDTO,
            @PathVariable String uid,
            @RequestAttribute Authed authed
    ) {
        uidDTO.setUid(uid);
        return ResponseUtil.createRpcResult(request, response, userBlockClient.deleteUserBlock(authed.buildDTO(uidDTO)));
    }

    /**
     * 分页获取黑名单列表
     */
    @RequestMapping(method = RequestMethod.GET, path = "block", consumes = MediaType.ALL_VALUE)
    public String getUserBlockPageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            PageDTO pageDTO,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        pageDTO.setPage(page);
        RpcResult rpcResult = userBlockClient.getUserBlockPageResultList(authed.buildDTO(pageDTO));
        if (rpcResult.getServerStatus() != ServerStatus.SUCCESS || rpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
            return ResponseUtil.createRpcResult(request, response, rpcResult);
        }
        JSONObject result = (JSONObject) rpcResult.getServerResult().getResult();
        // 缓存
        List<UserProfilePageResult> userProfilePageResultList = new ArrayList<>();
        result.getJSONArray("list").forEach(obj -> {
            UserProfilePageResult userProfilePageResult = JSON.toJavaObject((JSON) obj, UserProfilePageResult.class);
            userProfilePageResultList.add(userProfilePageResult);
            UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(userProfilePageResult.getUid()), UserProfileResult.class);
            if (userProfileResult != null) {
                userProfilePageResult.setUserProfileResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, userProfileResult, gatewayProperties.getLiveUidList()));
            } else {
                UidDTO uidDTO = new UidDTO();
                uidDTO.setUid(userProfilePageResult.getUid());
                RpcResult userProfileRpcResult = userProfileClient.getProfile(authed.buildDTO(uidDTO));
                // 错误直接跳过
                if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    userProfilePageResult.setUserProfileResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class), gatewayProperties.getLiveUidList()));
                }
            }
        });
        result.put("list", userProfilePageResultList);
        return ServerResult.buildSuccessResult(result).toString();
    }

    /**
     * 分页获取消息列表
     */
    @RequestMapping(method = RequestMethod.GET, path = "message", consumes = MediaType.ALL_VALUE)
    public String getUserMessagePageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            PageDTO pageDTO,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        pageDTO.setPage(page);
        RpcResult rpcResult = userMessageClient.getUserMessagePageResultList(authed.buildDTO(pageDTO));
        if (rpcResult.getServerStatus() != ServerStatus.SUCCESS || rpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
            return ResponseUtil.createRpcResult(request, response, rpcResult);
        }
        JSONObject result = (JSONObject) rpcResult.getServerResult().getResult();
        // 缓存
        List<UserMessagePageResult> userProfilePageResultList = new ArrayList<>();
        result.getJSONArray("list").forEach(obj -> {
            UserMessagePageResult userMessagePageResult = JSON.toJavaObject((JSON) obj, UserMessagePageResult.class);
            userProfilePageResultList.add(userMessagePageResult);
            UserMessageResult userMessageResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserMessageKey(userMessagePageResult.getMessageId()), UserMessageResult.class);
            if (userMessageResult != null) {
                userMessagePageResult.setUserMessageResult(userMessageResult);
            } else {
                UserMessageIdDTO userMessageIdDTO = new UserMessageIdDTO();
                userMessageIdDTO.setMessageId(userMessagePageResult.getMessageId());
                RpcResult userProfileRpcResult = userMessageClient.getUserMessage(authed.buildDTO(userMessageIdDTO));
                // 错误直接跳过
                if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    userMessagePageResult.setUserMessageResult(JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserMessageResult.class));
                }
            }
        });
        result.put("list", userProfilePageResultList);
        return ServerResult.buildSuccessResult(result).toString();
    }

    /**
     * 获取未读消息总数
     */
    @RequestMapping(method = RequestMethod.GET, path = "message/unread_total", consumes = MediaType.ALL_VALUE)
    public String getUserMessageUnReadTotal(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestAttribute Authed authed
    ) {
        Long unreadTotal = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserMessageUnReadTotalKey(authed.getAuthedUid()), Long.class);
        if (unreadTotal == null) {
            RpcResult rpcResult = userMessageClient.getUserMessageUnReadTotal(authed.buildDTO());
            if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                unreadTotal = Long.parseLong(rpcResult.getServerResult().getResult().toString());
            }
        }
        return ServerResult.buildSuccessResult(CommonUtil.nullToDefault(unreadTotal, 0)).toString();
    }

    /**
     * 发送名片
     */
    @RequestMapping(method = RequestMethod.POST, path = "{uid:[a-f0-9]{32}}/card")
    public String postUserCard(
            HttpServletRequest request,
            HttpServletResponse response,
            UidDTO uidDTO,
            @PathVariable String uid,
            @RequestAttribute Authed authed
    ) {
        uidDTO.setUid(uid);
        return ResponseUtil.createRpcResult(request, response, userCardClient.postUserCard(authed.buildDTO(uidDTO)));
    }

    /**
     * 删除名片
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "{uid:[a-f0-9]{32}}/card")
    public String deleteUserCard(
            HttpServletRequest request,
            HttpServletResponse response,
            UidDTO uidDTO,
            @PathVariable String uid,
            @RequestAttribute Authed authed
    ) {
        uidDTO.setUid(uid);
        return ResponseUtil.createRpcResult(request, response, userCardClient.deleteUserCard(authed.buildDTO(uidDTO)));
    }

    /**
     * 分页获取名片列表
     */
    @RequestMapping(method = RequestMethod.GET, path = "card", consumes = MediaType.ALL_VALUE)
    public String getUserCardPageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            PageDTO pageDTO,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        pageDTO.setPage(page);
        RpcResult rpcResult = userCardClient.getUserCardPageResultList(authed.buildDTO(pageDTO));
        if (rpcResult.getServerStatus() != ServerStatus.SUCCESS || rpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
            return ResponseUtil.createRpcResult(request, response, rpcResult);
        }
        JSONObject result = (JSONObject) rpcResult.getServerResult().getResult();
        // 缓存
        List<UserProfilePageResult> userProfilePageResultList = new ArrayList<>();
        result.getJSONArray("list").forEach(obj -> {
            UserProfilePageResult userProfilePageResult = JSON.toJavaObject((JSON) obj, UserProfilePageResult.class);
            userProfilePageResultList.add(userProfilePageResult);
            UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(userProfilePageResult.getUid()), UserProfileResult.class);
            if (userProfileResult != null) {
                userProfilePageResult.setUserProfileResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, userProfileResult, gatewayProperties.getLiveUidList()));
            } else {
                UidDTO uidDTO = new UidDTO();
                uidDTO.setUid(userProfilePageResult.getUid());
                RpcResult userProfileRpcResult = userProfileClient.getProfile(authed.buildDTO(uidDTO));
                // 错误直接跳过
                if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    userProfilePageResult.setUserProfileResult(FixedUtil.fixedUserProfileResult(userBlockClient, userCardClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class), gatewayProperties.getLiveUidList()));
                }
            }
        });
        result.put("list", userProfilePageResultList);
        return ServerResult.buildSuccessResult(result).toString();
    }

}
