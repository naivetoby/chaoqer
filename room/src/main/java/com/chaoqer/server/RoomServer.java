package com.chaoqer.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.ClubClient;
import com.chaoqer.client.PushAsyncClient;
import com.chaoqer.client.UserBlockClient;
import com.chaoqer.client.UserProfileClient;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.club.ClubIdDTO;
import com.chaoqer.common.entity.club.ClubResult;
import com.chaoqer.common.entity.push.AlertMessageDTO;
import com.chaoqer.common.entity.room.*;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.ClubOTS;
import com.chaoqer.repository.RoomOTS;
import com.chaoqer.repository.UserCardOTS;
import com.chaoqer.repository.UserProfileOTS;
import com.chaoqer.util.RtcTokenBuilder;
import com.chaoqer.verticle.RoomVerticle;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RpcServer(value = "room", threadNum = 4, type = RpcType.SYNC)
public class RoomServer {

    @Autowired
    private RoomOTS roomOTS;
    @Autowired
    private ClubOTS clubOTS;
    @Autowired
    private ClubClient clubClient;
    @Autowired
    private UserCardOTS userCardOTS;
    @Autowired
    private RoomVerticle roomVerticle;
    @Autowired
    private UserProfileOTS userProfileOTS;
    @Autowired
    private PushAsyncClient pushAsyncClient;
    @Autowired
    private UserBlockClient userBlockClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserProfileClient userProfileClient;

    @Value("${agora.customer-key}")
    private String customerKey;
    @Value("${agora.customer-secret}")
    private String customerSecret;

    // 创建活动
    @RpcServerMethod
    public ServerResult postRoom(@Validated PostRoomDTO postRoomDTO) {
        String uid = postRoomDTO.getAuthedUid();
        String name = escapesJavaScript(postRoomDTO.getName()).replace("\r\n", " ").replace("\n", " ");
        String clubId = postRoomDTO.getClubId();
        int inviteOnly = postRoomDTO.getInviteOnly();
        if (inviteOnly > 0) {
            inviteOnly = 1;
            clubId = "";
        }
        // 圈子是否存在，用户是否在圈子里
        if (StringUtils.isNotBlank(clubId) && !clubOTS.isClubMember(clubId, uid)) {
            return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode());
        }
        String roomId = DigestUtil.getUUID();
        long now = System.currentTimeMillis();
        if (roomOTS.saveRoom(roomId, uid, name, clubId, inviteOnly, now)) {
            // 非公开圈子
            if (StringUtils.isNotBlank(clubId)) {
                // 异步扩散到当前圈子的所有用户
                clubOTS.getClubMemberUidList(clubId).forEach(clubMemberUid -> roomOTS.asyncSaveUserRoom(roomId, clubMemberUid, now));
            } else if (inviteOnly == 0) {
                // 公开活动表
                roomOTS.asyncSaveUserPublicRoom(roomId, now);
            }
            return ServerResult.buildSuccessResult(roomOTS.getRoom(roomId));
        }
        return ServerResult.buildFailureMessage("创建活动失败");
    }

    // 获取活动
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getRoom(@Validated RoomIdDTO roomIdDTO) {
        String roomId = roomIdDTO.getRoomId();
        RoomResult roomResult = roomOTS.getRoom(roomId);
        if (roomResult != null) {
            // 默认3天缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), roomResult, 3, TimeUnit.DAYS);
            return ServerResult.buildSuccessResult(roomResult);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode());
    }

    // 创建活动缓存
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult buildRoomCache(@Validated RoomIdDTO roomIdDTO) {
        String authedUid = roomIdDTO.getAuthedUid();
        String roomId = roomIdDTO.getRoomId();
        RoomCacheResult roomCacheResult = roomOTS.getRoomCache(roomId);
        if (roomCacheResult != null) {
            // 分享者
            UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(authedUid), UserProfileResult.class);
            if (userProfileResult == null) {
                UidDTO uidDTO = new UidDTO();
                uidDTO.setUid(authedUid);
                RpcResult userProfileRpcResult = userProfileClient.getProfile(uidDTO);
                if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    userProfileResult = JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class);
                }
            }
            roomCacheResult.setFromMember(userProfileResult);
            // 圈子信息
            String clubId = roomCacheResult.getClubId();
            if (StringUtils.isNotBlank(clubId)) {
                ClubIdDTO clubIdDTO = new ClubIdDTO();
                clubIdDTO.setClubId(clubId);
                ClubResult clubResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getClubKey(clubId), ClubResult.class);
                if (clubResult == null) {
                    RpcResult rpcResult = clubClient.getClub(clubIdDTO);
                    if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                        clubResult = JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), ClubResult.class);
                    }
                }
                roomCacheResult.setClubResult(clubResult);
            }
            // 成员信息
            List<UserProfileResult> memberList = new ArrayList<>();
            Map<String, String> members = RedisUtil.hgetall(redisTemplate, RedisKeyGenerator.getRoomMemberListKey(roomCacheResult.getRoomId()));
            if (!members.isEmpty()) {
                members.keySet().stream().limit(6).forEach(uid -> {
                    UserProfileResult tempUserProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(uid), UserProfileResult.class);
                    if (tempUserProfileResult == null) {
                        UidDTO uidDTO = new UidDTO();
                        uidDTO.setUid(uid);
                        RpcResult userProfileRpcResult = userProfileClient.getProfile(uidDTO);
                        if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                            tempUserProfileResult = JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class);
                        }
                    }
                    if (tempUserProfileResult != null) {
                        memberList.add(tempUserProfileResult);
                    }
                });
            }
            roomCacheResult.setMemberList(memberList);
            roomCacheResult.setMemberTotal(members.keySet().size());
            // 默认30天缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getRoomCacheKey(roomCacheResult.getRoomCacheId()), roomCacheResult, 30, TimeUnit.DAYS);
            return ServerResult.buildSuccessResult(roomCacheResult.getRoomCacheId());
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode());
    }

    // 获取活动声网Token
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getRoomAgoraToken(@Validated RoomIdDTO roomIdDTO) {
        String roomId = roomIdDTO.getRoomId();
        // 判断活动是否存在
        RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), RoomResult.class);
        if (roomResult == null) {
            roomResult = roomOTS.getRoom(roomId);
        }
        // 判断用户是否对此活动可见
        if (roomResult != null) {
            // 判断是否有被speaker拉黑
            Map<String, String> memberList = RedisUtil.hgetall(redisTemplate, RedisKeyGenerator.getRoomMemberListKey(roomId));
            if (!memberList.isEmpty()) {
                for (Map.Entry<String, String> member : memberList.entrySet()) {
                    // 所有speaker
                    int role = JSONObject.parseObject(member.getValue()).getIntValue("role");
                    if (role == 2) {
                        Integer blocked = 0;
                        if (roomIdDTO.isUserLogin()) {
                            blocked = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserBlockKey(member.getKey(), roomIdDTO.getAuthedUid()), Integer.class);
                            if (blocked == null) {
                                UidDTO uidDTO = new UidDTO();
                                uidDTO.setAuthedUid(member.getKey());
                                uidDTO.setUid(roomIdDTO.getAuthedUid());
                                RpcResult rpcResult = userBlockClient.isUserBlock(uidDTO);
                                if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                                    blocked = Integer.parseInt(rpcResult.getServerResult().getResult().toString());
                                }
                            }
                        }
                        if (CommonUtil.nullToDefault(blocked, 0) == 1) {
                            return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_ALLOW_JOIN.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_ALLOW_JOIN.getCode());
                        }
                    }
                }
            }
            JSONObject result = new JSONObject();
            result.put("roomId", roomId);
            result.put("token", buildTokenByRoomId(roomId));
            return ServerResult.buildSuccessResult(result);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode());
    }

    // 获取活动列表
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getRoomPageResultList(PageDTO pageDTO) {
        Page page = pageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        return ServerResult.buildSuccessResult(roomOTS.getRoomPageResultList(pageDTO.getAuthedUid(), page));
    }

    // 关闭活动
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult closeRoom(@Validated RoomIdDTO roomIdDTO) {
        String roomId = roomIdDTO.getRoomId();
        RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), RoomResult.class);
        if (roomResult == null) {
            roomResult = roomOTS.getRoom(roomId);
        }
        if (roomResult != null) {
            String roomMemberListKey = RedisKeyGenerator.getRoomMemberListKey(roomId);
            // 1. 主持人强制关闭房间
            // 2. 管理员直接解散房间
            if (JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, roomIdDTO.getAuthedUid()), "{}")).getIntValue("role") == 2 || "00000000000000000000000000000000".equals(roomIdDTO.getAuthedUid())) {
                // 关闭房间
                if (roomOTS.closeRoom(roomId)) {
                    // 异步扩散删除到当前圈子的所有用户
                    if (StringUtils.isNotBlank(roomResult.getClubId())) {
                        clubOTS.getClubMemberUidList(roomResult.getClubId()).forEach(clubMemberUid -> roomOTS.asyncDeleteUserRoom(roomId, clubMemberUid));
                    } else if (roomResult.getInviteOnly() == 0) {
                        // 删除公开活动
                        roomOTS.asyncDeleteUserPublicRoom(roomId);
                    }
                    // 删除缓存
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId));
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomAgoraTokenKey(roomId));
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomMemberListKey(roomId));
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomRequestDataKey(roomId));
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomResponseDataKey(roomId));
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomPinDataKey(roomId));
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomLiveDataKey(roomId));
                    // 关闭所有客户端
                    JSONObject close = new JSONObject();
                    close.put("event", "close");
                    roomVerticle.getVertx().eventBus().publish(roomId, close.toJSONString());
                    return ServerResult.build(OperateStatus.SUCCESS);
                }
            }
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode());
    }

    // 邀请用户加入活动
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult roomInvite(@Validated RoomInviteDTO roomInviteDTO) {
        String authedUid = roomInviteDTO.getAuthedUid();
        String uid = roomInviteDTO.getUid();
        String roomId = roomInviteDTO.getRoomId();
        RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), RoomResult.class);
        if (roomResult == null) {
            roomResult = roomOTS.getRoom(roomId);
        }
        if (roomResult != null) {
            // 存在名片夹中
            if (userCardOTS.isUserSendCard(uid, authedUid)) {
                // 是否已经在房间里
                Map<String, String> members = RedisUtil.hgetall(redisTemplate, RedisKeyGenerator.getRoomMemberListKey(roomResult.getRoomId()));
                if (!members.containsKey(uid)) {
                    pushAsyncClient.pushAlertMessage(AlertMessageDTO.buildRoomInviteMessage(userProfileOTS.getProfile(authedUid), uid, roomResult));
                }
            }
            return ServerResult.build(OperateStatus.SUCCESS).message("邀请成功");
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode());
    }

    private String buildTokenByRoomId(String roomId) {
        String roomAgoraTokenKey = RedisKeyGenerator.getRoomAgoraTokenKey(roomId);
        String token = RedisUtil.getString(redisTemplate, roomAgoraTokenKey);
        if (StringUtils.isBlank(token)) {
            RtcTokenBuilder rtcTokenBuilder = new RtcTokenBuilder();
            int timestamp = (int) (System.currentTimeMillis() / 1000 + 3600 * 24);
            token = rtcTokenBuilder.buildTokenWithUid(customerKey, customerSecret, roomId, 0, RtcTokenBuilder.Role.Role_Publisher, timestamp);
            RedisUtil.setObject(redisTemplate, roomAgoraTokenKey, token, 23, TimeUnit.HOURS);
        }
        return token;
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
        return content;
    }

}
