package com.chaoqer.scheduled;

import com.alibaba.fastjson.JSONObject;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.verticle.RoomVerticle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ScheduledTasks {

    private final static Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RoomVerticle roomVerticle;

    @Async
    public void roomIconDeleteTask() {
        // 活动所有发过图片的用户
        String prefixRoomMemberIconKey = RedisKeyGenerator.getRoomMemberIconKey("*", "*");
        Set<String> roomMemberIconKeys = redisTemplate.keys(prefixRoomMemberIconKey);
        if (roomMemberIconKeys != null && !roomMemberIconKeys.isEmpty()) {
            // 所有活动
            for (String roomMemberIconKey : roomMemberIconKeys) {
                String[] ids = roomMemberIconKey.replace(prefixRoomMemberIconKey.replace("*_*", ""), "").split("_");
                if (ids.length == 2) {
                    String roomId = ids[0];
                    String uid = ids[1];
                    logger.info("roomIconDeleteTask roomId: {}, uid: {}", roomId, uid);
                    String data = RedisUtil.getString(redisTemplate, roomMemberIconKey);
                    if (StringUtils.isNotBlank(data) && !RedisUtil.isKeyExist(redisTemplate, RedisKeyGenerator.getRoomMemberIconTimeoutKey(roomId, uid))) {
                        // 删除缓存
                        RedisUtil.delObject(redisTemplate, roomMemberIconKey);
                        // 通知客户端消除表情
                        JSONObject clear = new JSONObject();
                        clear.put("event", "tool");
                        clear.put("sender", uid);
                        clear.put("action", "clear");
                        clear.put("data", data);
                        roomVerticle.getVertx().eventBus().publish(roomId, clear.toJSONString());
                        logger.info("clear roomIconDeleteTask roomId: {}, uid: {}", roomId, uid);
                    }
                }
            }
        }
    }

}