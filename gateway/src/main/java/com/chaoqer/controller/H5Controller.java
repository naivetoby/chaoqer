package com.chaoqer.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.annotation.ParamVerifySkip;
import com.chaoqer.annotation.UserLoginSkip;
import com.chaoqer.client.club.ClubClient;
import com.chaoqer.client.event.EventClient;
import com.chaoqer.client.room.RoomClient;
import com.chaoqer.client.user.UserBlockClient;
import com.chaoqer.client.user.UserCardClient;
import com.chaoqer.client.user.UserProfileClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.club.ClubIdDTO;
import com.chaoqer.common.entity.club.ClubResult;
import com.chaoqer.common.entity.event.EventIdDTO;
import com.chaoqer.common.entity.event.EventResult;
import com.chaoqer.common.entity.room.RoomCacheResult;
import com.chaoqer.common.entity.room.RoomIdDTO;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.GatewayProperties;
import com.chaoqer.util.FixedUtil;
import com.chaoqer.util.WXUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcResult;
import vip.toby.rpc.entity.ServerResult;
import vip.toby.rpc.entity.ServerStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping(value = "h5")
public class H5Controller {

    @Value("${wx.appId}")
    private String appId;
    @Value("${wx.appSecret}")
    private String appSecret;

    @Autowired
    private RoomClient roomClient;
    @Autowired
    private ClubClient clubClient;
    @Autowired
    private EventClient eventClient;
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

    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "redirect")
    public void getIndex(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(CommonUtil.nullToDefault(request.getParameter("redirect_uri"), CommonUtil.getHost(request)));
    }

    /**
     * 获取微信Config
     */
    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "weixin/config", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getWeiXinConfig(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        return ServerResult.buildSuccessResult(WXUtil.getConfig(redisTemplate, appId, appSecret, request.getParameter("url"))).toString();
    }

    /**
     * 获取活动缓存
     */
    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "room/{roomCacheId:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getRoomCache(
            @PathVariable String roomCacheId,
            @RequestAttribute Authed authed
    ) {
        RoomCacheResult roomCacheResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomCacheKey(roomCacheId), RoomCacheResult.class);
        if (roomCacheResult != null) {
            RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomCacheResult.getRoomId()), RoomResult.class);
            if (roomResult == null) {
                RoomIdDTO roomIdDTO = new RoomIdDTO();
                roomIdDTO.setRoomId(roomCacheResult.getRoomId());
                RpcResult roomRpcResult = roomClient.getRoom(authed.buildDTO(roomIdDTO));
                // 错误直接跳过
                if (roomRpcResult.getServerStatus() != ServerStatus.SUCCESS || roomRpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
                    roomCacheResult.setClosed(1);
                }
            }
            return ServerResult.buildSuccessResult(roomCacheResult).toString();
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode()).toString();
    }

    /**
     * 获取圈子
     */
    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "club/{clubId:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getClub(
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
     * 获取用户资料
     */
    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "profile/{uid:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
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
     * 获取日程
     */
    @UserLoginSkip
    @ParamVerifySkip
    @RequestMapping(method = RequestMethod.GET, path = "event/{eventId:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            EventIdDTO eventIdDTO,
            @PathVariable String eventId,
            @RequestAttribute Authed authed
    ) {
        EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), EventResult.class);
        if (eventResult != null) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, eventResult)).toString();
        }
        eventIdDTO.setEventId(eventId);
        RpcResult eventRpcResult = eventClient.getEvent(authed.buildDTO(eventIdDTO));
        // 错误直接跳过
        if (eventRpcResult.getServerStatus() == ServerStatus.SUCCESS && eventRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) eventRpcResult.getServerResult().getResult(), EventResult.class))).toString();
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode()).toString();
    }

}
