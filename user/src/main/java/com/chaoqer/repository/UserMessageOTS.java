package com.chaoqer.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.DeleteStatus;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.push.UserMessagePageResult;
import com.chaoqer.common.entity.push.UserMessageResult;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.common.util.TablestoreUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class UserMessageOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserMessageOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public UserMessageResult getUserMessageResult(String uid, long messageId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.fromLong(messageId));
        PrimaryKey userMessagePrimaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("user_message", userMessagePrimaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        // 设置读取某些列
        criteria.addColumnsToGet("origin_push_type");
        criteria.addColumnsToGet("origin_uid");
        criteria.addColumnsToGet("message_type");
        criteria.addColumnsToGet("message_body");
        criteria.addColumnsToGet("unread");
        criteria.addColumnsToGet("deleted");
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            DeleteStatus deleteStatus = DeleteStatus.getDeleteStatus((int) row.getLatestColumn("deleted").getValue().asLong());
            if (deleteStatus == DeleteStatus.NORMAL) {
                UserMessageResult userMessageResult = new UserMessageResult();
                userMessageResult.setUid(row.getPrimaryKey().getPrimaryKeyColumn("uid").getValue().asString());
                userMessageResult.setMessageId(row.getPrimaryKey().getPrimaryKeyColumn("message_id").getValue().asLong());
                userMessageResult.setOriginPushType((int) row.getLatestColumn("origin_push_type").getValue().asLong());
                if (row.contains("origin_uid")) {
                    userMessageResult.setOriginUid(row.getLatestColumn("origin_uid").getValue().asString());
                }
                userMessageResult.setMessageType((int) row.getLatestColumn("message_type").getValue().asLong());
                userMessageResult.setMessageBody(JSON.parseObject(row.getLatestColumn("message_body").getValue().asString()));
                userMessageResult.setUnread((int) row.getLatestColumn("unread").getValue().asLong());
                userMessageResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                return userMessageResult;
            }
        }
        return null;
    }

    public JSONObject getUserMessagePageResultList(String uid, Page page) {
        int offset = 1;
        List<UserMessagePageResult> userMessagePageResultList = new ArrayList<>(page.getCount());
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        if (StringUtils.isBlank(page.getLastId())) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.INF_MAX);
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.fromLong(Long.parseLong(page.getLastId())));
        }
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        Pair<List<Row>, PrimaryKey> data;
        while (userMessagePageResultList.size() < page.getCount() && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "user_message_index", null, Direction.BACKWARD, startKey, endKey, offset, page.getCount());
            for (Row row : data.getFirst()) {
                long messageId = row.getPrimaryKey().getPrimaryKeyColumn("message_id").getValue().asLong();
                UserMessagePageResult userMessagePageResult = new UserMessagePageResult();
                userMessagePageResult.setSequenceId(Long.toString(messageId));
                userMessagePageResult.setMessageId(messageId);
                userMessagePageResultList.add(userMessagePageResult);
            }
            startKey = data.getSecond();
        }
        JSONObject result = new JSONObject();
        result.put("page", page);
        result.put("list", userMessagePageResultList);
        return result;
    }

    public long getUserMessageUnReadTotal(String uid) {
        String userMessageUnReadTotalKey = RedisKeyGenerator.getUserMessageUnReadTotalKey(uid);
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("club_member_unread_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        primaryKeyBuilder.addPrimaryKeyColumn("unread", PrimaryKeyValue.fromLong(1));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey startKey = primaryKeyBuilder.build();
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(startKey);
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        primaryKeyBuilder.addPrimaryKeyColumn("unread", PrimaryKeyValue.fromLong(1));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.INF_MAX);
        PrimaryKey endKey = primaryKeyBuilder.build();
        rangeRowQueryCriteria.setExclusiveEndPrimaryKey(endKey);
        // 设置读取最新版本
        rangeRowQueryCriteria.setMaxVersions(1);
        long total = 0;
        while (true) {
            GetRangeResponse getRangeResponse = dataSyncClient.getRange(new GetRangeRequest(rangeRowQueryCriteria));
            List<Row> rows = getRangeResponse.getRows();
            if (rows == null) {
                break;
            }
            total += rows.size();
            // 如果nextStartPrimaryKey不为null，则继续读取
            if (getRangeResponse.getNextStartPrimaryKey() != null) {
                rangeRowQueryCriteria.setInclusiveStartPrimaryKey(getRangeResponse.getNextStartPrimaryKey());
            } else {
                break;
            }
        }
        // 删除缓存
        RedisUtil.delObject(redisTemplate, userMessageUnReadTotalKey);
        RedisUtil.increment(redisTemplate, userMessageUnReadTotalKey, total);
        RedisUtil.setExpireTime(redisTemplate, userMessageUnReadTotalKey, 1, TimeUnit.DAYS);
        return total;
    }

    public void asyncUpdateUserMessageRead(String uid, long messageId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.fromLong(messageId));
        PrimaryKey userMessagePrimaryKey = primaryKeyBuilder.build();
        // 更新 user_message 表
        RowUpdateChange userMessageUpdateChange = new RowUpdateChange("user_message", userMessagePrimaryKey);
        userMessageUpdateChange.put("unread", ColumnValue.fromLong(0));
        userMessageUpdateChange.put("update_time", ColumnValue.fromLong(System.currentTimeMillis()));
        // 设置条件更新，行条件检查为期望原行存在
        userMessageUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
        // 异步更新
        dataAsyncClient.updateRow(new UpdateRowRequest(userMessageUpdateChange), new TableStoreCallback<UpdateRowRequest, UpdateRowResponse>() {
            @Override
            public void onCompleted(UpdateRowRequest req, UpdateRowResponse res) {
                logger.debug("asyncUpdateUserMessageRead Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(UpdateRowRequest req, Exception ex) {
                if (!ex.getMessage().equals("Condition check failed.")) {
                    logger.error("asyncUpdateUserMessageRead Error! {}", ex.getMessage(), ex);
                }
            }
        });
    }

    public void asyncUpdateUserMessageDeleted(String uid, long messageId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.fromLong(messageId));
        PrimaryKey userMessagePrimaryKey = primaryKeyBuilder.build();
        // 更新 user_message 表
        RowUpdateChange userMessageUpdateChange = new RowUpdateChange("user_message", userMessagePrimaryKey);
        userMessageUpdateChange.put("unread", ColumnValue.fromLong(1));
        userMessageUpdateChange.put("deleted", ColumnValue.fromLong(DeleteStatus.DELETED_BY_USER.getStatus()));
        userMessageUpdateChange.put("update_time", ColumnValue.fromLong(System.currentTimeMillis()));
        // 设置条件更新，行条件检查为期望原行存在
        userMessageUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
        // 异步更新
        dataAsyncClient.updateRow(new UpdateRowRequest(userMessageUpdateChange), new TableStoreCallback<UpdateRowRequest, UpdateRowResponse>() {
            @Override
            public void onCompleted(UpdateRowRequest req, UpdateRowResponse res) {
                logger.debug("asyncUpdateUserMessageDeleted Success! requestId: {}", res.getRequestId());

            }

            @Override
            public void onFailed(UpdateRowRequest req, Exception ex) {
                if (!ex.getMessage().equals("Condition check failed.")) {
                    logger.error("asyncUpdateUserMessageDeleted Error! {}", ex.getMessage(), ex);
                }
            }
        });
    }

}
