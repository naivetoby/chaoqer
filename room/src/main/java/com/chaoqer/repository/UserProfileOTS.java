package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserProfileOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserProfileOTS.class);

    @Autowired
    private Environment env;
    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public UserProfileResult getProfile(String uid) {
        String userProfileSelfDbKey = RedisKeyGenerator.getUserProfileKey(uid);
        if (RedisUtil.isKeyExist(redisTemplate, userProfileSelfDbKey)) {
            return RedisUtil.getObject(redisTemplate, userProfileSelfDbKey, UserProfileResult.class);
        }
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("user_profile", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        // 设置读取某些列
        criteria.addColumnsToGet("nickname");
        criteria.addColumnsToGet("avatar");
        criteria.addColumnsToGet("bio");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            UserProfileResult userProfileResult = new UserProfileResult();
            userProfileResult.setUid(uid);
            userProfileResult.setNickname(row.getLatestColumn("nickname").getValue().asString());
            userProfileResult.setAvatar(row.getLatestColumn("avatar").getValue().asString());
            if (row.contains("bio")) {
                userProfileResult.setBio(row.getLatestColumn("bio").getValue().asString());
            }
            return userProfileResult;
        }
        return null;
    }

}
