package com.chaoqer.repository;

import com.alibaba.fastjson.JSON;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.entity.OriginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserOTS.class);

    @Autowired
    private SyncClient dataSyncClient;

    public OriginResult getOriginResultByUid(String uid) {
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
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            UserProfileResult userProfileResult = new UserProfileResult();
            userProfileResult.setUid(uid);
            userProfileResult.setNickname(row.getLatestColumn("nickname").getValue().asString());
            userProfileResult.setAvatar(row.getLatestColumn("avatar").getValue().asString());
            if (row.contains("bio")) {
                userProfileResult.setBio(row.getLatestColumn("bio").getValue().asString());
            }
            long createTime = row.getLatestColumn("create_time").getValue().asLong();
            // 组合结果
            OriginResult originResult = new OriginResult();
            originResult.setOriginId(uid);
            originResult.setUid(uid);
            originResult.setCreateTime(createTime);
            originResult.setContent(JSON.toJSONString(userProfileResult));
            return originResult;
        }
        return null;
    }

}
