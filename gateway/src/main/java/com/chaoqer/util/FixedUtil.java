package com.chaoqer.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.club.ClubClient;
import com.chaoqer.client.event.EventClient;
import com.chaoqer.client.room.RoomClient;
import com.chaoqer.client.user.UserBlockClient;
import com.chaoqer.client.user.UserCardClient;
import com.chaoqer.client.user.UserProfileClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.club.ClubIdDTO;
import com.chaoqer.common.entity.club.ClubResult;
import com.chaoqer.common.entity.event.EventIdDTO;
import com.chaoqer.common.entity.event.EventResult;
import com.chaoqer.common.entity.room.RoomIdDTO;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcResult;
import vip.toby.rpc.entity.ServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FixedUtil {

    public static RoomResult fixedRoomResult(ClubClient clubClient, UserProfileClient userProfileClient, StringRedisTemplate redisTemplate, Authed authed, RoomResult roomResult) {
        String clubId = roomResult.getClubId();
        if (StringUtils.isNotBlank(clubId)) {
            ClubIdDTO clubIdDTO = new ClubIdDTO();
            clubIdDTO.setClubId(clubId);
            // 圈子信息
            ClubResult clubResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getClubKey(clubId), ClubResult.class);
            if (clubResult == null) {
                RpcResult rpcResult = clubClient.getClub(clubIdDTO);
                if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    clubResult = JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), ClubResult.class);
                }
            }
            roomResult.setClubResult(clubResult);
        }
        // 成员信息
        List<UserProfileResult> memberList = new ArrayList<>();
        Map<String, String> members = RedisUtil.hgetall(redisTemplate, RedisKeyGenerator.getRoomMemberListKey(roomResult.getRoomId()));
        if (!members.isEmpty()) {
            members.keySet().stream().limit(6).forEach(uid -> {
                UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(uid), UserProfileResult.class);
                if (userProfileResult == null) {
                    UidDTO uidDTO = new UidDTO();
                    uidDTO.setUid(uid);
                    RpcResult userProfileRpcResult = userProfileClient.getProfile(authed.buildDTO(uidDTO));
                    if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                        userProfileResult = JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class);
                    }
                }
                if (userProfileResult != null) {
                    memberList.add(userProfileResult);
                }
            });
        }
        // 如果活动已经没有人参与
        if (memberList.isEmpty()) {
            // 活动创建者
            String uid = authed.getAuthedUid();
            if (uid.equals(roomResult.getUid())) {
                UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(uid), UserProfileResult.class);
                if (userProfileResult == null) {
                    UidDTO uidDTO = new UidDTO();
                    uidDTO.setUid(uid);
                    RpcResult userProfileRpcResult = userProfileClient.getProfile(authed.buildDTO(uidDTO));
                    if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                        userProfileResult = JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class);
                    }
                }
                if (userProfileResult != null) {
                    memberList.add(userProfileResult);
                }
                roomResult.setMemberList(memberList);
                return roomResult;
            }
            return null;
        }
        roomResult.setMemberList(memberList);
        roomResult.setMemberTotal(members.keySet().size());
        return roomResult;
    }

    public static ClubResult fixedClubResult(ClubClient clubClient, StringRedisTemplate redisTemplate, Authed authed, ClubResult clubResult) {
        String clubId = clubResult.getClubId();
        ClubIdDTO clubIdDTO = new ClubIdDTO();
        clubIdDTO.setClubId(clubId);
        // 成员总数
        Long memberTotal = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getClubMemberTotalKey(clubResult.getClubId()), Long.class);
        if (memberTotal == null) {
            RpcResult rpcResult = clubClient.getClubMemberTotal(clubIdDTO);
            if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                memberTotal = Long.parseLong(rpcResult.getServerResult().getResult().toString());
            }
        }
        clubResult.setMemberTotal(CommonUtil.nullToDefault(memberTotal, 0));
        // 是否在圈子里面
        int joined = 0;
        if (authed.isUserLogin()) {
            clubIdDTO.setAuthedUid(authed.getAuthedUid());
            RpcResult rpcResult = clubClient.getClubMemberJoined(clubIdDTO);
            if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                joined = Integer.parseInt(rpcResult.getServerResult().getResult().toString());
            }
        }
        clubResult.setJoined(joined);
        return clubResult;
    }

    public static UserProfileResult fixedUserProfileResult(UserBlockClient userBlockClient, UserCardClient userCardClient, StringRedisTemplate redisTemplate, Authed authed, UserProfileResult userProfileResult, List<String> liveUidList) {
        // 是否拉黑
        Integer blocked = 0;
        if (authed.isUserLogin()) {
            blocked = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserBlockKey(authed.getAuthedUid(), userProfileResult.getUid()), Integer.class);
            if (blocked == null) {
                UidDTO uidDTO = new UidDTO();
                uidDTO.setUid(userProfileResult.getUid());
                RpcResult rpcResult = userBlockClient.isUserBlock(authed.buildDTO(uidDTO));
                if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    blocked = Integer.parseInt(rpcResult.getServerResult().getResult().toString());
                }
            }
        }
        userProfileResult.setBlocked(CommonUtil.nullToDefault(blocked, 0));
        // 是否已发送卡片
        Integer sendCard = 0;
        if (authed.isUserLogin()) {
            sendCard = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserSendCardKey(authed.getAuthedUid(), userProfileResult.getUid()), Integer.class);
            if (sendCard == null) {
                UidDTO uidDTO = new UidDTO();
                uidDTO.setUid(userProfileResult.getUid());
                RpcResult rpcResult = userCardClient.isUserSendCard(authed.buildDTO(uidDTO));
                if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    sendCard = Integer.parseInt(rpcResult.getServerResult().getResult().toString());
                }
            }
        }
        userProfileResult.setSendCard(CommonUtil.nullToDefault(sendCard, 0));
        // 名片夹总数
        Long cardTotal = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserCardTotalKey(userProfileResult.getUid()), Long.class);
        if (cardTotal == null) {
            UidDTO uidDTO = new UidDTO();
            uidDTO.setUid(userProfileResult.getUid());
            RpcResult rpcResult = userCardClient.getUserCardTotal(authed.buildDTO(uidDTO));
            if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                cardTotal = Long.parseLong(rpcResult.getServerResult().getResult().toString());
            }
        }
        userProfileResult.setCardTotal(CommonUtil.nullToDefault(cardTotal, 0));
        // 是否开启live功能
        if (liveUidList.contains(userProfileResult.getUid())) {
            userProfileResult.setAllowLive(1);
        }
        // 关闭3D模型 FIXME
        userProfileResult.setFigure(null);
        return userProfileResult;
    }

    public static EventResult fixedEventResult(EventClient eventClient, ClubClient clubClient, RoomClient roomClient, UserProfileClient userProfileClient, StringRedisTemplate redisTemplate, Authed authed, EventResult eventResult) {
        String eventId = eventResult.getEventId();
        // 活动
        String roomId = eventResult.getRoomId();
        if (StringUtils.isNotBlank(roomId)) {
            RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), RoomResult.class);
            if (roomResult != null) {
                roomResult = FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, roomResult);
            } else {
                RoomIdDTO roomIdDTO = new RoomIdDTO();
                roomIdDTO.setRoomId(roomId);
                RpcResult roomRpcResult = roomClient.getRoom(authed.buildDTO(roomIdDTO));
                // 错误直接跳过
                if (roomRpcResult.getServerStatus() == ServerStatus.SUCCESS && roomRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    roomResult = FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) roomRpcResult.getServerResult().getResult(), RoomResult.class));
                }
            }
            if (roomResult != null) {
                eventResult.setRoomResult(roomResult);
            }
        }
        if (authed.isUserLogin()) {
            // 是否已预约日程提醒
            Integer eventNotify = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventNotifyKey(eventId, authed.getAuthedUid()), Integer.class);
            if (eventNotify == null) {
                EventIdDTO eventIdDTO = new EventIdDTO();
                eventIdDTO.setEventId(eventId);
                RpcResult eventNotifyRpcResult = eventClient.isEventNotify(authed.buildDTO(eventIdDTO));
                if (eventNotifyRpcResult.getServerStatus() == ServerStatus.SUCCESS && eventNotifyRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    eventNotify = Integer.parseInt(eventNotifyRpcResult.getServerResult().getResult().toString());
                }
            }
            eventResult.setNotify(CommonUtil.nullToDefault(eventNotify, 0));
        } else {
            // 主持人或者嘉宾
            List<UserProfileResult> memberList = new ArrayList<>();
            eventResult.getMemberUidList().forEach(uid -> {
                UserProfileResult userProfileResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(uid), UserProfileResult.class);
                if (userProfileResult == null) {
                    UidDTO uidDTO = new UidDTO();
                    uidDTO.setUid(uid);
                    RpcResult userProfileRpcResult = userProfileClient.getProfile(authed.buildDTO(uidDTO));
                    if (userProfileRpcResult.getServerStatus() == ServerStatus.SUCCESS && userProfileRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                        userProfileResult = JSONObject.toJavaObject((JSON) userProfileRpcResult.getServerResult().getResult(), UserProfileResult.class);
                    }
                }
                if (userProfileResult != null) {
                    memberList.add(userProfileResult);
                }
            });
            eventResult.setMemberList(memberList);
        }
        return eventResult;
    }

}
