package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.ClientInfo;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AccountOTS {

    private final static Logger logger = LoggerFactory.getLogger(AccountOTS.class);

    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void asyncUpdateAccountAuthExpireTime(String uid) {
        String accountUpdateExpireTimeKey = RedisKeyGenerator.getAccountUpdateExpireTimeKey(uid);
        if (!RedisUtil.isKeyExist(redisTemplate, accountUpdateExpireTimeKey)) {
            // 当前时间
            long now = System.currentTimeMillis();
            // 登录过期时间默认30天
            int expireTimeDuration = 30 * 24 * 60 * 60;
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            PrimaryKey accountAuthPrimaryKey = primaryKeyBuilder.build();
            // 更新 account_auth 表
            RowUpdateChange accountAuthRowUpdateChange = new RowUpdateChange("account_auth", accountAuthPrimaryKey);
            accountAuthRowUpdateChange.put(new Column("expire_time", ColumnValue.fromLong(now + expireTimeDuration * 1000L)));
            accountAuthRowUpdateChange.put(new Column("update_time", ColumnValue.fromLong(now)));
            // 设置条件更新，行条件检查为期望原行存在
            accountAuthRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            // 异步更新
            dataAsyncClient.updateRow(new UpdateRowRequest(accountAuthRowUpdateChange), new TableStoreCallback<UpdateRowRequest, UpdateRowResponse>() {
                @Override
                public void onCompleted(UpdateRowRequest req, UpdateRowResponse res) {
                    // 更新缓存
                    RedisUtil.setExpireTime(redisTemplate, RedisKeyGenerator.getAccountTokenKey(uid), expireTimeDuration);
                    RedisUtil.setExpireTime(redisTemplate, RedisKeyGenerator.getAccountSecretKey(uid), expireTimeDuration);

                    // 存一天的缓存
                    RedisUtil.setObject(redisTemplate, accountUpdateExpireTimeKey, true, 1, TimeUnit.DAYS);

                    logger.debug("asyncUpdateAccountAuthExpireTime Success! requestId: {}", res.getRequestId());
                }

                @Override
                public void onFailed(UpdateRowRequest req, Exception ex) {
                    if (!ex.getMessage().equals("Condition check failed.")) {
                        logger.error("asyncUpdateAccountAuthExpireTime Error! {}", ex.getMessage(), ex);
                    }
                }
            });
        }
    }

    public void asyncUpdateAccountActiveTime(String uid, ClientInfo clientInfo) {
        String accountUpdateActiveTimeKey = RedisKeyGenerator.getAccountUpdateActiveTimeKey(uid);
        if (!RedisUtil.isKeyExist(redisTemplate, accountUpdateActiveTimeKey)) {
            // 当前时间
            long now = System.currentTimeMillis();
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            PrimaryKey accountAuthPrimaryKey = primaryKeyBuilder.build();
            // 更新 account_auth 表
            RowUpdateChange accountAuthRowUpdateChange = new RowUpdateChange("account", accountAuthPrimaryKey);
            accountAuthRowUpdateChange.put(new Column("active_time", ColumnValue.fromLong(now)));
            accountAuthRowUpdateChange.put(new Column("client_type", ColumnValue.fromLong(clientInfo.getClientType())));
            accountAuthRowUpdateChange.put(new Column("version_name", ColumnValue.fromString(clientInfo.getVersionName())));
            accountAuthRowUpdateChange.put(new Column("version_code", ColumnValue.fromLong(clientInfo.getVersionCode())));
            // 设置条件更新，行条件检查为期望原行存在
            accountAuthRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            // 异步更新
            dataAsyncClient.updateRow(new UpdateRowRequest(accountAuthRowUpdateChange), new TableStoreCallback<UpdateRowRequest, UpdateRowResponse>() {
                @Override
                public void onCompleted(UpdateRowRequest req, UpdateRowResponse res) {
                    // 存一分钟的缓存
                    RedisUtil.setObject(redisTemplate, accountUpdateActiveTimeKey, true, 1, TimeUnit.MINUTES);
                    logger.debug("asyncUpdateAccountActiveTime Success! requestId: {}", res.getRequestId());
                }

                @Override
                public void onFailed(UpdateRowRequest req, Exception ex) {
                    if (!ex.getMessage().equals("Condition check failed.")) {
                        logger.error("asyncUpdateAccountActiveTime Error! {}", ex.getMessage(), ex);
                    }
                }
            });
        }
    }

}
