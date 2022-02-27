package com.chaoqer;

import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.verticle.RoomVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.Set;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    private RoomVerticle roomVerticle;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostConstruct
    public void deployVerticle() {
        // 清空缓存
        String prefixRoomMemberListKey = RedisKeyGenerator.getRoomMemberListKey("*");
        Set<String> roomMemberListKeys = redisTemplate.keys(prefixRoomMemberListKey);
        if (roomMemberListKeys != null && !roomMemberListKeys.isEmpty()) {
            // 所有活动
            for (String roomMemberListKey : roomMemberListKeys) {
                RedisUtil.delObject(redisTemplate, roomMemberListKey);
            }
        }
        // 开启IM服务器
        Vertx.vertx(new VertxOptions().setWorkerPoolSize(100)).deployVerticle(roomVerticle);
    }

}