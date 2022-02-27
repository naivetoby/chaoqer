package com.chaoqer.verticle;

import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.AppLinkPreviewAsyncClient;
import com.chaoqer.client.RoomClient;
import com.chaoqer.common.entity.app.LinkDTO;
import com.chaoqer.common.entity.room.RoomIdDTO;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.RoomProperties;
import com.chaoqer.repository.RoomOTS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

@Component
public class RoomVerticle extends AbstractVerticle {

    private final static Logger logger = LoggerFactory.getLogger(RoomVerticle.class);

    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Autowired
    private RoomOTS roomOTS;
    @Autowired
    private RoomClient roomClient;
    @Autowired
    private RoomProperties roomProperties;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AppLinkPreviewAsyncClient appLinkPreviewAsyncClient;

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setIdleTimeout(120).setIdleTimeoutUnit(TimeUnit.SECONDS));
        server.websocketHandler(ws -> {
            final JSONObject result = userAuth(ws);
            // 校验权限
            if (result == null) {
                logger.info("closing ws-connection to client");
                // 拒绝连接
                JSONObject close = new JSONObject();
                close.put("event", "close");
                ws.writeTextMessage(close.toJSONString());
                // 3秒后服务端关闭连接
                scheduledExecutorService.schedule((Runnable) ws::close, 3, TimeUnit.SECONDS);
                return;
            }
            // RoomID
            final String roomId = result.getString("roomId");
            // RoomUID
            final String roomUid = result.getString("roomUid");
            // UID
            final String uid = result.getString("uid");
            // RUID
            final long ruid = result.getLongValue("ruid");
            // TextHandlerID
            final String textHandlerId = ws.binaryHandlerID();
            // 缓存
            final String roomMemberListKey = RedisKeyGenerator.getRoomMemberListKey(roomId);
            final String roomRequestDataKey = RedisKeyGenerator.getRoomRequestDataKey(roomId);
            final String roomResponseDataKey = RedisKeyGenerator.getRoomResponseDataKey(roomId);
            final String roomPinDataKey = RedisKeyGenerator.getRoomPinDataKey(roomId);
            final String roomLiveDataKey = RedisKeyGenerator.getRoomLiveDataKey(roomId);
//            final String roomMemberRoleKey = RedisKeyGenerator.getRoomMemberRoleKey(roomId, uid);
            int role = 0;
            // 创建房间的人
            if (roomUid.equals(uid)) {
                Map<String, String> memberList = RedisUtil.hgetall(redisTemplate, roomMemberListKey);
                // 当前没有主持人
                if (memberList.isEmpty() || memberList.entrySet().stream().noneMatch(member -> JSONObject.parseObject(member.getValue()).getIntValue("role") == 2)) {
                    role = 2;
                }
            }
            JSONObject oldMember = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, uid), "{}"));
            if (oldMember.containsKey("textHandlerId")) {
                String oldTextHandlerId = oldMember.getString("textHandlerId");
                int oldRole = oldMember.getIntValue("role");
                // 已经有同一个账号的其他连接
                logger.info("already connected, closing ws-connection to client, uid: " + uid + ", oldTextHandlerId: " + oldTextHandlerId);
                // 强制关闭其他连接
                JSONObject close = new JSONObject();
                close.put("textHandlerId", oldTextHandlerId);
                close.put("close", true);
                vertx.eventBus().publish(roomId, close.toJSONString());
                // 上一个
                if (role != 2 && (oldRole == 2 || oldRole == 1)) {
                    role = oldRole;
                }
            }
            // 继承缓存的身份
//            int cacheRole = Integer.parseInt(CommonUtil.nullToDefault(RedisUtil.getString(redisTemplate, roomMemberRoleKey), "0"));
//            if (role != 2 && (cacheRole == 2 || cacheRole == 1)) {
//                role = cacheRole;
//            }
            // 连接成功
            logger.info("opening ws-connection to client, roomId: " + roomId + ", uid: " + uid + ", ruid: " + ruid + ", textHandlerId: " + textHandlerId);
            // 缓存连接信息
            RedisUtil.hset(redisTemplate, roomMemberListKey, uid, buildJson(ruid, role, textHandlerId, "", "", System.currentTimeMillis()));
            // 1. 通知客户端房间里所有成员信息
            JSONObject connected = new JSONObject();
            connected.put("event", "connected");
            connected.put("sender", uid);
            connected.put("data", RedisUtil.hgetall(redisTemplate, roomMemberListKey));
            ws.writeTextMessage(connected.toJSONString());
            // 2. 通知客户端房间里钉信息
            JSONObject pin = new JSONObject();
            pin.put("event", "pin");
            pin.put("sender", uid);
            pin.put("action", "list");
            pin.put("data", RedisUtil.hgetall(redisTemplate, roomPinDataKey));
            ws.writeTextMessage(pin.toJSONString());
            // 3. 通知客户端房间里live信息
            String roomLiveData = RedisUtil.getString(redisTemplate, roomLiveDataKey);
            if (StringUtils.isNotBlank(roomLiveData)) {
                ws.writeTextMessage(roomLiveData);
            }
            // 4. 通知房间内其他客户端加入房间事件
            JSONObject join = new JSONObject();
            join.put("event", "connection");
            join.put("sender", uid);
            join.put("action", "join");
            join.put("data", RedisUtil.hget(redisTemplate, roomMemberListKey, uid));
            vertx.eventBus().publish(roomId, join.toJSONString());
            // 如果当前是主持人, 分发举手列表
            if (role == 2) {
                JSONObject invite = new JSONObject();
                invite.put("event", "invite");
                invite.put("sender", uid);
                invite.put("action", "list");
                invite.put("data", RedisUtil.hgetall(redisTemplate, roomRequestDataKey).keySet());
                ws.writeTextMessage(invite.toJSONString());
            }
            // 订阅消息
            final MessageConsumer<String> messageConsumer = vertx.eventBus().consumer(roomId, message -> {
                String data = message.body();
                if (StringUtils.isBlank(data) || ws.isClosed()) {
                    return;
                }
                JSONObject dataJson = JSONObject.parseObject(data);
                JSONObject selfMember = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, uid), "{}"));
                // 强制挤下线
                if (dataJson.getBooleanValue("close")) {
                    // 需要关闭一个连接
                    if (textHandlerId.equals(dataJson.getString("textHandlerId"))) {
                        // 提示Client不需要再重新连接
                        JSONObject closeData = new JSONObject();
                        closeData.put("event", "close");
                        ws.writeTextMessage(closeData.toJSONString());
                        // 3秒后服务端关闭连接
                        scheduledExecutorService.schedule((Runnable) ws::close, 3, TimeUnit.SECONDS);
                    }
                    return;
                }
                // 邀请与被邀请
                if (dataJson.getString("event").equals("invite")) {
                    String action = dataJson.getString("action");
                    // 举手(分发给所有主持人)
                    if (action.equals("request") && selfMember.getIntValue("role") == 2) {
                        ws.writeTextMessage(data);
                        logger.info("write message to client: " + data);
                    }
                    // 邀请(发送给被邀请的人)
                    if (action.equals("response") && uid.equals(dataJson.getString("data"))) {
                        ws.writeTextMessage(data);
                        logger.info("write message to client: " + data);
                        // 删除缓存
                        RedisUtil.hdel(redisTemplate, roomRequestDataKey, uid);
                    }
                    return;
                }
                // 被设置为主持人
                if (dataJson.getString("event").equals("connection") && dataJson.getString("action").equals("update")) {
                    // 发布消息
                    ws.writeTextMessage(data);
                    logger.info("write message to client: " + data);
                    if (uid.equals(dataJson.getString("sender")) && selfMember.getIntValue("role") == 2) {
                        // 发布举手列表
                        JSONObject invite = new JSONObject();
                        invite.put("event", "invite");
                        invite.put("sender", uid);
                        invite.put("action", "list");
                        invite.put("data", RedisUtil.hgetall(redisTemplate, roomRequestDataKey).keySet());
                        ws.writeTextMessage(invite.toJSONString());
                    }
                    return;
                }
                // 活动关闭
                if (dataJson.getString("event").equals("close")) {
                    ws.writeTextMessage(data);
                    // 3秒后服务端关闭连接
                    scheduledExecutorService.schedule((Runnable) ws::close, 3, TimeUnit.SECONDS);
                    return;
                }
                // 正常发布
                ws.writeTextMessage(data);
                logger.info("write message to client: " + data);
            });
            // 连接正常
            ws.textMessageHandler(data -> {
                try {
                    if (StringUtils.isBlank(data)) {
                        return;
                    }
                    JSONObject dataJson = JSONObject.parseObject(data);
                    // 心跳
                    if (dataJson.getString("event").equals("heartbeat")) {
                        return;
                    }
                    // 校验用户自己
                    if (!dataJson.getString("sender").equals(uid)) {
                        return;
                    }
                    logger.info("receive message from client: " + data);
                    JSONObject selfMember = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, uid), "{}"));
                    // live直播
                    if (dataJson.getString("event").equals("live")) {
                        String action = dataJson.getString("action");
                        // 打开直播功能
                        if (action.equals("open") && !RedisUtil.isKeyExist(redisTemplate, roomLiveDataKey) && selfMember.getIntValue("role") == 2) {
                            RedisUtil.setObject(redisTemplate, roomLiveDataKey, data);
                            vertx.eventBus().publish(roomId, data);
                            return;
                        }
                        // 关闭直播功能
                        if (action.equals("close") && RedisUtil.isKeyExist(redisTemplate, roomLiveDataKey) && selfMember.getIntValue("role") == 2) {
                            RedisUtil.delObject(redisTemplate, roomLiveDataKey);
                            vertx.eventBus().publish(roomId, data);
                            return;
                        }
                        JSONObject roomLiveDataJson = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.getString(redisTemplate, roomLiveDataKey), "{}"));
                        String lastAction = roomLiveDataJson.getString("action");
                        String lastSender = roomLiveDataJson.getString("sender");
                        // 上直播台
                        if (action.equals("up") && (lastAction.equals("open") || lastAction.equals("down")) && (selfMember.getIntValue("role") == 2 || selfMember.getIntValue("role") == 1)) {
                            RedisUtil.setObject(redisTemplate, roomLiveDataKey, data);
                            vertx.eventBus().publish(roomId, data);
                            return;
                        }
                        // 下直播台
                        if (action.equals("down") && lastAction.equals("up")) {
                            // 主持人或者自己可以下直播台
                            if (selfMember.getIntValue("role") == 2 || uid.equals(lastSender)) {
                                RedisUtil.setObject(redisTemplate, roomLiveDataKey, data);
                                vertx.eventBus().publish(roomId, data);
                            }
                            return;
                        }
                        return;
                    }
                    // 钉Tool(主持人)
                    if (dataJson.getString("event").equals("pin") && selfMember.getIntValue("role") == 2) {
                        String action = dataJson.getString("action");
                        // 钉
                        if (action.equals("add")) {
                            JSONObject pinData = dataJson.getJSONObject("data");
                            String md5 = pinData.getString("md5");
                            // 如果已存在
                            if (StringUtils.isNotBlank(RedisUtil.hget(redisTemplate, roomPinDataKey, md5))) {
                                return;
                            }
                            String pinTool = pinData.getString("tool");
                            String pinToolData = pinData.getString("toolData");
                            String toUid = pinData.getString("toUid");
                            pinData.put("createTime", System.currentTimeMillis());
                            JSONObject toMember = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, toUid), "{}"));
                            if (pinTool.equals("image") || pinTool.equals("link")) {
                                RedisUtil.hset(redisTemplate, roomPinDataKey, md5, pinData.toJSONString());
                                dataJson.put("data", pinData.toJSONString());
                                vertx.eventBus().publish(roomId, dataJson.toJSONString());
                                // 清除toUid的tool
                                if (toMember.getString("tool").equals(pinTool) && toMember.getString("toolData").equals(pinToolData)) {
                                    RedisUtil.hset(redisTemplate, roomMemberListKey, toUid, buildJson(toMember.getIntValue("ruid"), toMember.getIntValue("role"), toMember.getString("textHandlerId"), "", "", toMember.getLongValue("createTime")));
                                    JSONObject clear = new JSONObject();
                                    clear.put("event", "tool");
                                    clear.put("sender", toUid);
                                    clear.put("action", "clear");
                                    clear.put("data", pinToolData);
                                    vertx.eventBus().publish(roomId, clear.toJSONString());
                                    return;
                                }
                            }
                            return;
                        }
                        // 取消钉
                        if (action.equals("remove")) {
                            String md5 = dataJson.getString("data");
                            String pinData = RedisUtil.hget(redisTemplate, roomPinDataKey, md5);
                            // 如果不存在
                            if (StringUtils.isBlank(pinData)) {
                                return;
                            }
                            RedisUtil.hdel(redisTemplate, roomPinDataKey, md5);
                            vertx.eventBus().publish(roomId, data);
                            return;
                        }
                        return;
                    }
                    // 设置tool
                    if (dataJson.getString("event").equals("tool")) {
                        String tool = dataJson.getString("action");
                        String toolData = dataJson.getString("data");
                        // Speaker 或者 主持人才能发送Tool
                        if (selfMember.getIntValue("role") == 2 || selfMember.getIntValue("role") == 1) {
                            // 清除
                            if (tool.equals("clear")) {
                                // 上一次的tool
                                String memberToolData = selfMember.getString("toolData");
                                if (memberToolData.equals(toolData)) {
                                    RedisUtil.hset(redisTemplate, roomMemberListKey, uid, buildJson(selfMember.getIntValue("ruid"), selfMember.getIntValue("role"), textHandlerId, "", "", selfMember.getLongValue("createTime")));
                                    vertx.eventBus().publish(roomId, data);
                                    return;
                                }
                            }
                            if (tool.equals("icon") || tool.equals("image") || tool.equals("link")) {
                                // 表情缓存15秒
                                if (tool.equals("icon")) {
                                    RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getRoomMemberIconKey(roomId, uid), toolData);
                                    RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getRoomMemberIconTimeoutKey(roomId, uid), "1", 14);
                                } else {
                                    if (StringUtils.isBlank(toolData)) {
                                        tool = "";
                                    }
                                    // 处理link相关的数据
                                    if (tool.equals("link")) {
                                        // 长度限制
                                        if (toolData.length() > 500) {
                                            toolData = toolData.substring(0, 500);
                                            dataJson.put("data", toolData);
                                            data = dataJson.toJSONString();
                                        }
                                        // 链接预览
                                        LinkDTO linkDTO = new LinkDTO();
                                        linkDTO.setLink(toolData);
                                        appLinkPreviewAsyncClient.postLink(linkDTO);
                                    }
                                    RedisUtil.hset(redisTemplate, roomMemberListKey, uid, buildJson(selfMember.getIntValue("ruid"), selfMember.getIntValue("role"), textHandlerId, tool, toolData, selfMember.getLongValue("createTime")));
                                }
                                vertx.eventBus().publish(roomId, data);
                                return;
                            }
                        }
                        return;
                    }
                    String toUid = dataJson.getString("data");
                    JSONObject toMember = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, toUid), "{}"));
                    // 设置身份
                    if (dataJson.getString("event").equals("role")) {
                        boolean validData = false;
                        int action = dataJson.getIntValue("action");
                        // 1. 设置观众(当前身份是主持人或者自己)
                        if (action == 0 && (selfMember.getIntValue("role") == 2 || uid.equals(toUid))) {
                            if (toMember.getIntValue("role") == 2 || toMember.getIntValue("role") == 1) {
                                RedisUtil.hset(redisTemplate, roomMemberListKey, toUid, buildJson(toMember.getIntValue("ruid"), action, toMember.getString("textHandlerId"), "", "", System.currentTimeMillis()));
                                // 删除身份缓存
                                RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getRoomMemberRoleKey(roomId, toUid));
                                validData = true;
                            }
                        }
                        // 2. 设置主持人(当前身份是主持人并且对方是Speaker)
                        if (action == 2 && selfMember.getIntValue("role") == 2) {
                            if (toMember.getIntValue("role") == 1) {
                                RedisUtil.hset(redisTemplate, roomMemberListKey, toUid, buildJson(toMember.getIntValue("ruid"), action, toMember.getString("textHandlerId"), toMember.getString("tool"), toMember.getString("toolData"), System.currentTimeMillis()));
                                validData = true;
                            }
                        }
                        // 3. 设置Speaker
                        if (action == 1 && uid.equals(toUid) && selfMember.getIntValue("role") == 0 && StringUtils.isNotBlank(RedisUtil.hget(redisTemplate, roomResponseDataKey, uid))) {
                            RedisUtil.hset(redisTemplate, roomMemberListKey, uid, buildJson(selfMember.getIntValue("ruid"), action, selfMember.getString("textHandlerId"), selfMember.getString("tool"), selfMember.getString("toolData"), System.currentTimeMillis()));
                            validData = true;
                            // 删除被邀请记录
                            RedisUtil.hdel(redisTemplate, roomResponseDataKey, uid);
                        }
                        if (validData) {
                            // 更新用户身份
                            JSONObject update = new JSONObject();
                            update.put("event", "connection");
                            update.put("sender", toUid);
                            update.put("action", "update");
                            update.put("data", RedisUtil.hget(redisTemplate, roomMemberListKey, toUid));
                            vertx.eventBus().publish(roomId, update.toJSONString());
                            // 如果当前正在视频直播
                            JSONObject roomLiveDataJson = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.getString(redisTemplate, roomLiveDataKey), "{}"));
                            String lastAction = roomLiveDataJson.getString("action");
                            String lastSender = roomLiveDataJson.getString("sender");
                            // 被设置为观众
                            if (action == 0 && "up".equals(lastAction) && toUid.equals(lastSender)) {
                                roomLiveDataJson.put("action", "down");
                                RedisUtil.setObject(redisTemplate, roomLiveDataKey, roomLiveDataJson.toJSONString());
                                JSONObject live = new JSONObject();
                                live.put("event", "live");
                                live.put("sender", toUid);
                                live.put("action", "down");
                                vertx.eventBus().publish(roomId, live.toJSONString());
                            }
                        }
                        return;
                    }
                    // 邀请与被邀请
                    if (dataJson.getString("event").equals("invite")) {
                        String action = dataJson.getString("action");
                        // 举手(只限观众)
                        if (action.equals("request") && selfMember.getIntValue("role") == 0) {
                            // 强制删除被邀请记录
                            RedisUtil.hdel(redisTemplate, roomResponseDataKey, uid);
                            // 去重
                            if (StringUtils.isBlank(RedisUtil.hget(redisTemplate, roomRequestDataKey, uid))) {
                                RedisUtil.hset(redisTemplate, roomRequestDataKey, uid, "1");
                                vertx.eventBus().publish(roomId, data);
                                return;
                            }
                        }
                        // 邀请(只限主持人, 被邀请人只能是观众)
                        if (action.equals("response") && selfMember.getIntValue("role") == 2 && toMember.getIntValue("role") == 0) {
                            // 去重
                            if (StringUtils.isBlank(RedisUtil.hget(redisTemplate, roomResponseDataKey, toUid))) {
                                RedisUtil.hset(redisTemplate, roomResponseDataKey, toUid, "1");
                                vertx.eventBus().publish(roomId, data);
                                return;
                            }
                        }
                        return;
                    }
                    // 强制静音
                    if (dataJson.getString("event").equals("mute")) {
                        if (selfMember.getIntValue("role") == 2) {
                            vertx.eventBus().publish(roomId, data);
                            return;
                        }
                        return;
                    }
                    vertx.eventBus().publish(roomId, data);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
            // 连接断开
            ws.closeHandler(arg0 -> {
                logger.info("closing ws-connection to client, roomId: " + roomId + ", uid: " + uid + ", ruid: " + ruid + ", textHandlerId: " + textHandlerId);
                // 解绑事件
                messageConsumer.unregister();
                // 当前用户
                JSONObject selfMember = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.hget(redisTemplate, roomMemberListKey, uid), "{}"));
                // 同一客户端
                if (textHandlerId.equals(selfMember.getString("textHandlerId"))) {
                    int selfMemberRole = selfMember.getIntValue("role");
//                    if (selfMemberRole == 2 || selfMemberRole == 1) {
//                        // 暂存身份10分钟
//                        RedisUtil.setObject(redisTemplate, roomMemberRoleKey, selfMemberRole, 10, TimeUnit.MINUTES);
//                    }
                    // 通知房间内其他客户端离开房间事件
                    JSONObject leave = new JSONObject();
                    leave.put("event", "connection");
                    leave.put("sender", uid);
                    leave.put("action", "leave");
                    vertx.eventBus().publish(roomId, leave.toJSONString());
                    // 如果当前正在视频直播
                    JSONObject roomLiveDataJson = JSONObject.parseObject(CommonUtil.nullToDefault(RedisUtil.getString(redisTemplate, roomLiveDataKey), "{}"));
                    String lastAction = roomLiveDataJson.getString("action");
                    String lastSender = roomLiveDataJson.getString("sender");
                    if ("up".equals(lastAction) && uid.equals(lastSender)) {
                        roomLiveDataJson.put("action", "down");
                        RedisUtil.setObject(redisTemplate, roomLiveDataKey, roomLiveDataJson.toJSONString());
                        JSONObject live = new JSONObject();
                        live.put("event", "live");
                        live.put("sender", uid);
                        live.put("action", "down");
                        vertx.eventBus().publish(roomId, live.toJSONString());
                    }
                    // 主持人
                    if (selfMemberRole == 2) {
                        // 当前房间所有用户
                        Map<String, String> memberList = RedisUtil.hgetall(redisTemplate, roomMemberListKey);
                        int hostTotal = 0;
                        JSONObject speakerMember = null;
                        for (Map.Entry<String, String> memberEntry : memberList.entrySet()) {
                            JSONObject member = JSONObject.parseObject(memberEntry.getValue());
                            int roleValue = member.getIntValue("role");
                            if (roleValue == 2) {
                                hostTotal++;
                            } else if (roleValue == 1) {
                                if (speakerMember == null || member.getIntValue("ruid") < speakerMember.getIntValue("ruid")) {
                                    speakerMember = member;
                                    speakerMember.put("uid", memberEntry.getKey());
                                }
                            }
                        }
                        // 少于两个主持人
                        if (hostTotal < 2) {
                            // 至少有一个Speaker
                            if (speakerMember != null) {
                                RedisUtil.hset(redisTemplate, roomMemberListKey, speakerMember.getString("uid"), buildJson(speakerMember.getIntValue("ruid"), 2, speakerMember.getString("textHandlerId"), speakerMember.getString("tool"), speakerMember.getString("toolData"), System.currentTimeMillis()));
                                // 更新用户身份
                                JSONObject update = new JSONObject();
                                update.put("event", "connection");
                                update.put("sender", speakerMember.getString("uid"));
                                update.put("action", "update");
                                update.put("data", RedisUtil.hget(redisTemplate, roomMemberListKey, speakerMember.getString("uid")));
                                vertx.eventBus().publish(roomId, update.toJSONString());
                            } else {
                                // 关闭房间
                                RoomIdDTO roomIdDTO = new RoomIdDTO();
                                roomIdDTO.setRoomId(roomId);
                                roomIdDTO.setAuthedUid(uid);
                                roomClient.closeRoom(roomIdDTO);
                            }
                        }
                    }
                    // 删除缓存
                    RedisUtil.hdel(redisTemplate, roomMemberListKey, uid);
                }
                // 强制删除被邀请记录
                RedisUtil.hdel(redisTemplate, roomResponseDataKey, uid);
            });
            // 连接异常
            ws.exceptionHandler(e -> {
                logger.error("ws-connection to client exception, roomId: " + roomId + ", uid: " + uid + ", ruid: " + ruid + ", textHandlerId: " + textHandlerId);
                // 关闭连接
                ws.close();
            });
        });
        // 监听端口
        server.listen(18081, res -> {
            if (res.succeeded()) {
                logger.info("room-ws listen on port [18081]");
            }
        });
    }

    private JSONObject userAuth(ServerWebSocket ws) {
        try {
            String authorization = ws.headers().get(AUTHORIZATION);
            if (StringUtils.isBlank(authorization)) {
                return null;
            }
            String uidAndTokenStr = DigestUtil.base64Decode(authorization.substring(6));
            String[] uidAndToken = uidAndTokenStr.split(":");
            if (uidAndToken.length == 2) {
                String uid = uidAndToken[0];
                String token = uidAndToken[1];
                // 校验token
                if (token.equals(RedisUtil.getString(redisTemplate, RedisKeyGenerator.getAccountTokenKey(uid)))) {
                    String roomId = ws.path().substring(1);
                    if (StringUtils.isNotBlank(roomId)) {
                        RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), RoomResult.class);
                        if (roomResult == null) {
                            roomResult = roomOTS.getRoom(roomId);
                        }
                        if (roomResult != null) {
                            long ruid = getRuidByCreateTime(roomId, roomResult.getCreateTime());
                            if (ruid == -1) {
                                logger.warn("getRuidByCreateTime Error!");
                                return null;
                            }
                            JSONObject result = new JSONObject();
                            result.put("uid", uid);
                            result.put("roomId", roomId);
                            result.put("ruid", ruid);
                            result.put("roomUid", roomResult.getUid());
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private String buildJson(long ruid, int role, String textHandlerId, String tool, String toolData, long createTime) {
        JSONObject data = new JSONObject();
        data.put("ruid", ruid);
        data.put("role", role);
        data.put("textHandlerId", textHandlerId);
        data.put("tool", tool);
        data.put("toolData", toolData);
        data.put("createTime", createTime);
        return data.toJSONString();
    }

    private long getRuidByCreateTime(String roomId, long createTime) {
        int n = 0;
        long ruid = (System.currentTimeMillis() - createTime) / 100;
        while (!RedisUtil.setLockKeyIfAbsent(redisTemplate, RedisKeyGenerator.getRoomRuidKey(roomId, ruid), 10)) {
            if (n++ > 1000) {
                return -1;
            }
            ruid++;
        }
        return ruid;
    }

}
