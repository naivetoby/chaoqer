package com.chaoqer.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.club.ClubClient;
import com.chaoqer.client.user.UserBlockClient;
import com.chaoqer.client.user.UserCardClient;
import com.chaoqer.client.user.UserProfileClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.club.*;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfilePageResult;
import com.chaoqer.common.entity.user.UserProfileResult;
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
@RequestMapping(value = "club", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class ClubController {

    @Autowired
    private ClubClient clubClient;
    @Autowired
    private UserCardClient userCardClient;
    @Autowired
    private UserBlockClient userBlockClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserProfileClient userProfileClient;
    @Autowired
    private GatewayProperties gatewayProperties;

    /**
     * 创建圈子
     */
    @RequestMapping(method = RequestMethod.POST)
    public String postClub(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody PostClubDTO postClubDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, clubClient.postClub(authed.buildDTO(postClubDTO)));
    }

    /**
     * 获取圈子
     */
    @RequestMapping(method = RequestMethod.GET, path = "{clubId:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE)
    public String getClub(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String clubId,
            @RequestAttribute Authed authed
    ) {
        ClubResult clubResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getClubKey(clubId), ClubResult.class);
        if (clubResult != null) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedClubResult(clubClient, redisTemplate, authed, clubResult)).toString();
        }
        ClubIdDTO clubIdDTO = new ClubIdDTO();
        clubIdDTO.setClubId(clubId);
        RpcResult clubRpcResult = clubClient.getClub(authed.buildDTO(clubIdDTO));
        // 错误直接跳过
        if (clubRpcResult.getServerStatus() == ServerStatus.SUCCESS && clubRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedClubResult(clubClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) clubRpcResult.getServerResult().getResult(), ClubResult.class))).toString();
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode()).toString();
    }

    /**
     * 分页获取圈子列表
     */
    @RequestMapping(method = RequestMethod.GET, consumes = MediaType.ALL_VALUE)
    public String getClubPageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            PageDTO pageDTO,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        pageDTO.setPage(page);
        RpcResult rpcResult = clubClient.getClubPageResultList(authed.buildDTO(pageDTO));
        if (rpcResult.getServerStatus() != ServerStatus.SUCCESS || rpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
            return ResponseUtil.createRpcResult(request, response, rpcResult);
        }
        JSONObject result = (JSONObject) rpcResult.getServerResult().getResult();
        // 缓存
        List<ClubPageResult> clubPageResultList = new ArrayList<>();
        result.getJSONArray("list").forEach(obj -> {
            ClubPageResult clubPageResult = JSON.toJavaObject((JSON) obj, ClubPageResult.class);
            clubPageResultList.add(clubPageResult);
            ClubResult clubResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getClubKey(clubPageResult.getClubId()), ClubResult.class);
            if (clubResult != null) {
                clubPageResult.setClubResult(FixedUtil.fixedClubResult(clubClient, redisTemplate, authed, clubResult));
            } else {
                ClubIdDTO clubIdDTO = new ClubIdDTO();
                clubIdDTO.setClubId(clubPageResult.getClubId());
                RpcResult clubRpcResult = clubClient.getClub(authed.buildDTO(clubIdDTO));
                // 错误直接跳过
                if (clubRpcResult.getServerStatus() == ServerStatus.SUCCESS && clubRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    clubPageResult.setClubResult(FixedUtil.fixedClubResult(clubClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) clubRpcResult.getServerResult().getResult(), ClubResult.class)));
                }
            }
        });
        result.put("list", clubPageResultList);
        return ServerResult.buildSuccessResult(result).toString();
    }

    /**
     * 分页获取圈子成员列表
     */
    @RequestMapping(method = RequestMethod.GET, path = "{clubId:[a-f0-9]{32}}/member", consumes = MediaType.ALL_VALUE)
    public String getClubMemberPageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            ClubIdPageDTO clubIdPageDTO,
            @PathVariable String clubId,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        clubIdPageDTO.setClubId(clubId);
        clubIdPageDTO.setPage(page);
        RpcResult rpcResult = clubClient.getClubMemberPageResultList(authed.buildDTO(clubIdPageDTO));
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
     * 加入圈子
     */
    @RequestMapping(method = RequestMethod.POST, path = "{clubId:[a-f0-9]{32}}/join")
    public String joinClub(
            HttpServletRequest request,
            HttpServletResponse response,
            ClubIdDTO clubIdDTO,
            @PathVariable String clubId,
            @RequestAttribute Authed authed
    ) {
        clubIdDTO.setClubId(clubId);
        return ResponseUtil.createRpcResult(request, response, clubClient.joinClub(authed.buildDTO(clubIdDTO)));
    }

    /**
     * 退出圈子
     */
    @RequestMapping(method = RequestMethod.POST, path = "{clubId:[a-f0-9]{32}}/leave")
    public String leaveClub(
            HttpServletRequest request,
            HttpServletResponse response,
            ClubIdDTO clubIdDTO,
            @PathVariable String clubId,
            @RequestAttribute Authed authed
    ) {
        clubIdDTO.setClubId(clubId);
        return ResponseUtil.createRpcResult(request, response, clubClient.leaveClub(authed.buildDTO(clubIdDTO)));
    }

}
