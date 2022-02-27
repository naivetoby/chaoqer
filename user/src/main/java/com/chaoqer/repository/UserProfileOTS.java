package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.AccountStatus;
import com.chaoqer.common.entity.user.UserProfileDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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

    public boolean initProfile(UserProfileDTO userProfileDTO) {
        // 当前时间
        long now = System.currentTimeMillis();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(userProfileDTO.getAuthedUid()));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 局部事务
        String userProfileTransactionID = "";
        String accountTransactionID = "";
        if (CommonUtil.isProdEnvironment(env)) {
            userProfileTransactionID = dataSyncClient.startLocalTransaction(new StartLocalTransactionRequest("user_profile", primaryKey)).getTransactionID();
            accountTransactionID = dataSyncClient.startLocalTransaction(new StartLocalTransactionRequest("account", primaryKey)).getTransactionID();
        }
        try {
            // 更新 user_profile 表
            RowUpdateChange userProfileRowUpdateChange = new RowUpdateChange("user_profile", primaryKey);
            userProfileRowUpdateChange.put("nickname", ColumnValue.fromString(userProfileDTO.getNickname()));
            userProfileRowUpdateChange.put("avatar", ColumnValue.fromString(userProfileDTO.getAvatar()));
            userProfileRowUpdateChange.put("update_time", ColumnValue.fromLong(now));
            // 设置条件更新，行条件检查为期望原行存在
            userProfileRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            UpdateRowRequest userProfileUpdateRowRequest = new UpdateRowRequest(userProfileRowUpdateChange);
            if (CommonUtil.isProdEnvironment(env)) {
                userProfileUpdateRowRequest.setTransactionId(userProfileTransactionID);
            }
            dataSyncClient.updateRow(userProfileUpdateRowRequest);
            // 更新 account 表
            RowUpdateChange accountRowUpdateChange = new RowUpdateChange("account", primaryKey);
            accountRowUpdateChange.put("status", ColumnValue.fromLong(AccountStatus.NORMAL.getStatus()));
            accountRowUpdateChange.put("update_time", ColumnValue.fromLong(now));
            // 设置条件更新，行条件检查为期望原行存在
            accountRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            UpdateRowRequest accountUpdateRowRequest = new UpdateRowRequest(accountRowUpdateChange);
            if (CommonUtil.isProdEnvironment(env)) {
                accountUpdateRowRequest.setTransactionId(accountTransactionID);
            }
            dataSyncClient.updateRow(accountUpdateRowRequest);
            // 提交事务
            if (CommonUtil.isProdEnvironment(env)) {
                dataSyncClient.commitTransaction(new CommitTransactionRequest(userProfileTransactionID));
                dataSyncClient.commitTransaction(new CommitTransactionRequest(accountTransactionID));
            }
            // 用户默认状态
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountStatusKey(userProfileDTO.getAuthedUid()), AccountStatus.NORMAL.getStatus());
            return true;
        } catch (Exception e) {
            if (CommonUtil.isProdEnvironment(env)) {
                dataSyncClient.abortTransaction(new AbortTransactionRequest(userProfileTransactionID));
                dataSyncClient.abortTransaction(new AbortTransactionRequest(accountTransactionID));
            }
            logger.error("initUserInfo Error: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean putProfile(UserProfileResult userProfileResult, UserProfileDTO userProfileDTO) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(userProfileDTO.getAuthedUid()));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 更新 user_profile 表
            RowUpdateChange rowUpdateChange = new RowUpdateChange("user_profile", primaryKey);
            rowUpdateChange.put("nickname", ColumnValue.fromString(userProfileDTO.getNickname()));
            rowUpdateChange.put("avatar", ColumnValue.fromString(userProfileDTO.getAvatar()));
            if (StringUtils.isNotBlank(userProfileDTO.getBio())) {
                rowUpdateChange.put("bio", ColumnValue.fromString(userProfileDTO.getBio()));
            } else {
                if (StringUtils.isNotBlank(userProfileResult.getBio())) {
                    rowUpdateChange.deleteColumns("bio");
                }
            }
            rowUpdateChange.put("update_time", ColumnValue.fromLong(System.currentTimeMillis()));
            // 设置条件更新，行条件检查为期望原行存在
            rowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            dataSyncClient.updateRow(new UpdateRowRequest(rowUpdateChange));
            return true;
        } catch (Exception e) {
            logger.error("putProfile Error: " + e.getMessage(), e);
        }
        return false;
    }

    public Integer getAccountStatus(String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("account", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        // 设置读取某些列
        criteria.addColumnsToGet("status");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            int accountStatus = (int) row.getLatestColumn("status").getValue().asLong();
            // 缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountStatusKey(uid), accountStatus);
            return accountStatus;
        }
        return null;
    }

    public UserProfileResult getProfile(String uid) {
        // 账号状态
        String accountStatusKey = RedisKeyGenerator.getAccountStatusKey(uid);
        Integer accountStatus;
        if (!RedisUtil.isKeyExist(redisTemplate, accountStatusKey)) {
            accountStatus = getAccountStatus(uid);
        } else {
            accountStatus = RedisUtil.getObject(redisTemplate, accountStatusKey, Integer.class);
        }
        if (accountStatus != null && AccountStatus.getAccountStatus(accountStatus) == AccountStatus.NORMAL) {
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
            criteria.addColumnsToGet("figure");
            Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
            if (row != null && !row.isEmpty()) {
                UserProfileResult userProfileResult = new UserProfileResult();
                userProfileResult.setUid(uid);
                userProfileResult.setNickname(row.getLatestColumn("nickname").getValue().asString());
                userProfileResult.setAvatar(row.getLatestColumn("avatar").getValue().asString());
                if (row.contains("bio")) {
                    userProfileResult.setBio(row.getLatestColumn("bio").getValue().asString());
                }
                if (row.contains("figure")) {
                    userProfileResult.setFigure(row.getLatestColumn("figure").getValue().asString());
                }
                // 缓存一天
                RedisUtil.setObject(redisTemplate, userProfileSelfDbKey, userProfileResult, 30, TimeUnit.DAYS);
                return userProfileResult;
            }
        }
        return null;
    }

}
