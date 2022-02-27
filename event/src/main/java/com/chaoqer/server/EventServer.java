package com.chaoqer.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.PushAsyncClient;
import com.chaoqer.client.RoomClient;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.event.EventDTO;
import com.chaoqer.common.entity.event.EventIdDTO;
import com.chaoqer.common.entity.event.EventResult;
import com.chaoqer.common.entity.push.AlertMessageDTO;
import com.chaoqer.common.entity.room.PostRoomDTO;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.EventOTS;
import com.chaoqer.repository.UserProfileOTS;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.*;

import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RpcServer(value = "event", threadNum = 4, type = RpcType.SYNC)
public class EventServer {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private EventOTS eventOTS;
    @Autowired
    private RoomClient roomClient;
    @Autowired
    private UserProfileOTS userProfileOTS;
    @Autowired
    private PushAsyncClient pushAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 创建日程
    @RpcServerMethod
    public ServerResult postEvent(@Validated EventDTO eventDTO) {
        String uid = eventDTO.getAuthedUid();
        String eventId = DigestUtil.getUUID();
        // 必须包含自己
        LinkedHashSet<String> memberUidList = new LinkedHashSet<>(eventDTO.getMemberUidList());
        if (!memberUidList.contains(uid)) {
            return ServerResult.buildFailureMessage("参数错误");
        }
        // 标题去掉换行
        String name = escapesJavaScript(eventDTO.getName()).replace("\r\n", " ").replace("\n", " ");
        // 保存
        long now = System.currentTimeMillis();
        if (eventOTS.saveEvent(eventId, uid, name, eventDTO.getDesc(), memberUidList, eventDTO.getEventTime(), now)) {
            EventResult eventResult = eventOTS.getEvent(eventId);
            // 异步扩散到公共日程表
            eventOTS.asyncSaveUserPublicEvent(eventId, eventResult.getEventTime());
            // 推送消息
            UserProfileResult originUser = userProfileOTS.getProfile(uid);
            memberUidList.forEach(memberUid -> {
                if (!memberUid.equals(uid)) {
                    // 通知邀请的人
                    pushAsyncClient.pushAlertMessage(AlertMessageDTO.buildEventInviteMessage(originUser, memberUid, eventResult));
                }
            });
            return ServerResult.buildSuccessResult(eventResult);
        }
        return ServerResult.buildFailureMessage("创建日程失败");
    }

    // 编辑日程
    @RpcServerMethod
    public ServerResult putEvent(@Validated EventDTO eventDTO) {
        String uid = eventDTO.getAuthedUid();
        String eventId = eventDTO.getEventId();
        // 判断日程是否存在
        EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), EventResult.class);
        if (eventResult == null) {
            eventResult = eventOTS.getEvent(eventId);
        }
        if (eventResult != null) {
            // 未绑定活动
            if (StringUtils.isNotBlank(eventResult.getRoomId())) {
                return ServerResult.buildFailureMessage("无法编辑已绑定活动的日程");
            }
            // 在上一次的主持人或者嘉宾列表中
            if (eventResult.getMemberUidList().contains(uid)) {
                // 必须包含自己
                LinkedHashSet<String> memberUidList = new LinkedHashSet<>(eventDTO.getMemberUidList());
                if (!memberUidList.contains(uid)) {
                    return ServerResult.buildFailureMessage("参数错误");
                }
                // 新增的必须是名片夹中的用户 TODO
                // 标题去掉换行
                String name = escapesJavaScript(eventDTO.getName()).replace("\r\n", " ").replace("\n", " ");
                long now = eventResult.getCreateTime();
                if (eventOTS.saveEvent(eventId, uid, name, eventDTO.getDesc(), memberUidList, eventDTO.getEventTime(), now)) {
                    // 异步扩散删除到公开日程表
                    eventOTS.asyncSaveUserPublicEvent(eventId, eventDTO.getEventTime());
                    // 删除缓存
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId));
                    return ServerResult.buildSuccessResult(eventOTS.getEvent(eventId));
                }
            }
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode());
    }

    // 删除日程
    @RpcServerMethod
    public ServerResult deleteEvent(@Validated EventIdDTO eventIdDTO) {
        String eventId = eventIdDTO.getEventId();
        // 判断日程是否存在
        EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), EventResult.class);
        if (eventResult == null) {
            eventResult = eventOTS.getEvent(eventId);
        }
        if (eventResult != null) {
            // 未绑定活动
            if (StringUtils.isNotBlank(eventResult.getRoomId())) {
                return ServerResult.buildFailureMessage("无法删除已绑定活动的日程");
            }
            if (eventOTS.deleteEvent(eventId)) {
                // 异步扩散删除到公开日程表
                eventOTS.asyncDeleteUserPublicEvent(eventId);
                // 删除缓存
                RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId));
                return ServerResult.build(OperateStatus.SUCCESS);
            }
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode());
    }

    // 获取日程
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getEvent(@Validated EventIdDTO eventIdDTO) {
        String eventId = eventIdDTO.getEventId();
        EventResult eventResult = eventOTS.getEvent(eventId);
        if (eventResult != null) {
            // 默认30天缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), eventResult, 30, TimeUnit.DAYS);
            return ServerResult.buildSuccessResult(eventResult);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode());
    }

    // 获取日程列表
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getEventPageResultList(PageDTO pageDTO) {
        Page page = pageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        return ServerResult.buildSuccessResult(eventOTS.getEventPageResultList(pageDTO.getAuthedUid(), page));
    }

    // 预约日程
    @RpcServerMethod
    public ServerResult postEventNotify(@Validated EventIdDTO eventIdDTO) {
        String eventId = eventIdDTO.getEventId();
        String uid = eventIdDTO.getAuthedUid();
        // 判断日程是否存在
        EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), EventResult.class);
        if (eventResult == null) {
            eventResult = eventOTS.getEvent(eventId);
        }
        if (eventResult != null) {
            // 嘉宾
            if (eventResult.getMemberUidList().contains(uid)) {
                return ServerResult.buildFailureMessage("嘉宾或者主持人无法预约日程");
            }
            // 未绑定活动
            if (StringUtils.isNotBlank(eventResult.getRoomId())) {
                return ServerResult.buildFailureMessage("无法预约已绑定活动的日程");
            }
            // 时间已经过期
            if (eventResult.getEventTimeStatus() == 2) {
                return ServerResult.buildFailureMessage("无法预约已过期的日程");
            }
            if (eventOTS.saveEventNotify(eventId, uid)) {
                // 缓存
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getEventNotifyKey(eventId, uid), 1);
                return ServerResult.build(OperateStatus.SUCCESS);
            }
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode());
    }

    // 是否预约了日程的提醒
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult isEventNotify(@Validated EventIdDTO eventIdDTO) {
        String eventId = eventIdDTO.getEventId();
        String uid = eventIdDTO.getAuthedUid();
        // 判断日程是否存在
        EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), EventResult.class);
        if (eventResult == null) {
            eventResult = eventOTS.getEvent(eventId);
        }
        if (eventResult != null) {
            int result = eventOTS.isEventNotify(eventId, uid);
            // 缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getEventNotifyKey(eventId, uid), result);
            return ServerResult.buildSuccessResult(result);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode());
    }

    // 通过日程开始活动
    @RpcServerMethod
    public ServerResult startEvent(@Validated EventIdDTO eventIdDTO) {
        String uid = eventIdDTO.getAuthedUid();
        String eventId = eventIdDTO.getEventId();
        // 判断日程是否存在
        EventResult eventResult = eventOTS.getEvent(eventId);
        if (eventResult != null) {
            // 不在主持人或者嘉宾列表中
            if (!eventResult.getMemberUidList().contains(uid)) {
                return ServerResult.buildFailureMessage("权限不足");
            }
            // 时间不在正确时间
            if (eventResult.getEventTimeStatus() < 1) {
                return ServerResult.buildFailureMessage("日程时间未开始");
            }
            if (eventResult.getEventTimeStatus() > 1) {
                return ServerResult.buildFailureMessage("日程时间已过期");
            }
            // 已绑定活动
            if (StringUtils.isNotBlank(eventResult.getRoomId())) {
                return ServerResult.buildSuccessResult(eventOTS.getEvent(eventId));
            }
            PostRoomDTO postRoomDTO = new PostRoomDTO();
            postRoomDTO.setName(eventResult.getName());
            postRoomDTO.setAuthedUid(uid);
            RpcResult rpcResult = roomClient.postRoom(postRoomDTO);
            if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                RoomResult roomResult = JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), RoomResult.class);
                // 绑定活动到日程
                if (eventOTS.updateEvent(eventId, roomResult.getRoomId())) {
                    // 更新roomId
                    eventResult.setRoomId(roomResult.getRoomId());
                    // 删除缓存
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId));
                    // 推送
                    executorService.execute(() -> eventOTS.getEventNotifyUidList(eventId).forEach(toUid -> pushAsyncClient.pushAlertMessage(AlertMessageDTO.buildEventNotifyMessage(toUid, eventResult))));
                    return ServerResult.buildSuccessResult(eventOTS.getEvent(eventId));
                }
            }
            return ServerResult.buildFailureMessage("开始活动失败");
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode());
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
