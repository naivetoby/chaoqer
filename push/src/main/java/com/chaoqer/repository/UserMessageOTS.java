package com.chaoqer.repository;

import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.DeleteStatus;
import com.chaoqer.common.entity.push.MessageType;
import com.chaoqer.common.entity.push.OriginPushType;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserMessageOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserMessageOTS.class);

    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void asyncSaveUserMessage(String uid, OriginPushType originPushType, String originUid, MessageType messageType, JSONObject messageBody) {
        // 当前时间
        long now = System.currentTimeMillis();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("message_id", PrimaryKeyValue.AUTO_INCREMENT);
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        RowPutChange userMessageRowPutChange = new RowPutChange("user_message", primaryKey);
        // 加入属性列
        userMessageRowPutChange.addColumn("origin_push_type", ColumnValue.fromLong(originPushType.getType()));
        if (originPushType == OriginPushType.INTERACTIVE) {
            userMessageRowPutChange.addColumn("origin_uid", ColumnValue.fromString(originUid));
        }
        userMessageRowPutChange.addColumn("message_type", ColumnValue.fromLong(messageType.getType()));
        userMessageRowPutChange.addColumn("message_body", ColumnValue.fromString(messageBody.toJSONString()));
        userMessageRowPutChange.addColumn("unread", ColumnValue.fromLong(1));
        userMessageRowPutChange.addColumn("deleted", ColumnValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        userMessageRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
        // 写入数据到表格存储
        dataAsyncClient.putRow(new PutRowRequest(userMessageRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                RedisUtil.increment(redisTemplate, RedisKeyGenerator.getUserMessageUnReadTotalKey(uid));
                logger.debug("asyncSaveUserMessage Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                logger.error("asyncSaveUserMessage Error! {}", ex.getMessage(), ex);
            }
        });
    }

}
