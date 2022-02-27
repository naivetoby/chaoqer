package com.chaoqer.repository;

import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.DeleteStatus;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.event.EventPageResult;
import com.chaoqer.common.entity.event.EventResult;
import com.chaoqer.common.util.TablestoreUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class EventOTS {

    private final static Logger logger = LoggerFactory.getLogger(EventOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean saveEvent(String eventId, String uid, String name, String desc, LinkedHashSet<String> memberUidList, long eventTime, long now) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 存 event_meta 表
            RowPutChange eventRowPutChange = new RowPutChange("event_meta", primaryKey);
            // 插入值
            eventRowPutChange.addColumn("uid", ColumnValue.fromString(uid));
            eventRowPutChange.addColumn("name", ColumnValue.fromString(name));
            if (StringUtils.isNotBlank(desc)) {
                eventRowPutChange.addColumn("desc", ColumnValue.fromString(desc));
            }
            eventRowPutChange.addColumn("member_uid_list", ColumnValue.fromString(StringUtils.join(memberUidList, ",")));
            eventRowPutChange.addColumn("event_time", ColumnValue.fromLong(eventTime));
            if (now > 0) {
                eventRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            }
            eventRowPutChange.addColumn("deleted", ColumnValue.fromLong(DeleteStatus.NORMAL.getStatus()));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(eventRowPutChange));
            return true;
        } catch (Exception e) {
            logger.error("saveEvent Error: " + e.getMessage(), e);
        }
        return false;
    }

    public EventResult getEvent(String eventId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("event_meta", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        criteria.addColumnsToGet("uid");
        criteria.addColumnsToGet("name");
        criteria.addColumnsToGet("desc");
        criteria.addColumnsToGet("member_uid_list");
        criteria.addColumnsToGet("event_time");
        criteria.addColumnsToGet("room_id");
        criteria.addColumnsToGet("create_time");
        criteria.addColumnsToGet("deleted");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            int deleted = (int) row.getLatestColumn("deleted").getValue().asLong();
            if (deleted == 0) {
                EventResult eventResult = new EventResult();
                eventResult.setEventId(eventId);
                eventResult.setUid(row.getLatestColumn("uid").getValue().asString());
                eventResult.setName(row.getLatestColumn("name").getValue().asString());
                if (row.contains("desc")) {
                    eventResult.setDesc(row.getLatestColumn("desc").getValue().asString());
                }
                eventResult.setMemberUidList(Arrays.asList(row.getLatestColumn("member_uid_list").getValue().asString().split(",")));
                eventResult.setEventTime(row.getLatestColumn("event_time").getValue().asLong());
                if (row.contains("room_id")) {
                    eventResult.setRoomId(row.getLatestColumn("room_id").getValue().asString());
                }
                eventResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                return eventResult;
            }
        }
        return null;
    }

    public boolean deleteEvent(String eventId) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 更新 event_meta 表
            RowUpdateChange rowUpdateChange = new RowUpdateChange("event_meta", primaryKey);
            rowUpdateChange.put("deleted", ColumnValue.fromLong(DeleteStatus.DELETED_BY_USER.getStatus()));
            // 设置条件更新，行条件检查为期望原行存在
            rowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            dataSyncClient.updateRow(new UpdateRowRequest(rowUpdateChange));
            return true;
        } catch (Exception e) {
            logger.error("deleteEvent Error: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean updateEvent(String eventId, String roomId) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 更新 event_meta 表
            RowUpdateChange rowUpdateChange = new RowUpdateChange("event_meta", primaryKey);
            rowUpdateChange.put("room_id", ColumnValue.fromString(roomId));
            // 设置条件更新，行条件检查为期望原行存在
            rowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            dataSyncClient.updateRow(new UpdateRowRequest(rowUpdateChange));
            return true;
        } catch (Exception e) {
            logger.error("updateEvent Error: " + e.getMessage(), e);
        }
        return false;
    }

    public JSONObject getEventPageResultList(String uid, Page page) {
        int offset = 1;
        List<EventPageResult> eventPageResultList = new ArrayList<>(page.getCount());
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        if (StringUtils.isBlank(page.getLastId())) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString((System.currentTimeMillis() - 60 * 60 * 1000L) + ":00000000000000000000000000000000"));
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString(page.getLastId()));
        }
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.INF_MAX);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("event_time");
        Pair<List<Row>, PrimaryKey> data;
        while (eventPageResultList.size() < page.getCount() && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "user_public_event_index", columns, Direction.FORWARD, startKey, endKey, offset, page.getCount());
            for (Row row : data.getFirst()) {
                EventPageResult eventPageResult = new EventPageResult();
                eventPageResult.setEventId(row.getPrimaryKey().getPrimaryKeyColumn("event_id").getValue().asString());
                eventPageResult.setSequenceId(row.getPrimaryKey().getPrimaryKeyColumn("sequence_id").getValue().asString());
                eventPageResult.setEventTime(row.getLatestColumn("event_time").getValue().asLong());
                eventPageResultList.add(eventPageResult);
            }
            startKey = data.getSecond();
        }
        JSONObject result = new JSONObject();
        result.put("page", page);
        result.put("list", eventPageResultList);
        return result;
    }

    public void asyncSaveUserPublicEvent(String eventId, long eventTime) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
        // 存 user_public_room 表
        RowPutChange userPublicRoomRowPutChange = new RowPutChange("user_public_event", primaryKeyBuilder.build());
        // 插入值
        userPublicRoomRowPutChange.addColumn("event_time", ColumnValue.fromLong(eventTime));
        userPublicRoomRowPutChange.addColumn("sequence_id", ColumnValue.fromString(eventTime + ":" + eventId));
        // 异步更新
        dataAsyncClient.putRow(new PutRowRequest(userPublicRoomRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("asyncSaveUserPublicEvent Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                logger.error("asyncSaveUserPublicEvent Error! {}", ex.getMessage(), ex);
            }
        });
    }

    public void asyncDeleteUserPublicEvent(String eventId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
        // 删除 user_public_room 表
        RowDeleteChange userPublicRoomRowPutChange = new RowDeleteChange("user_public_event", primaryKeyBuilder.build());
        // 异步更新
        dataAsyncClient.deleteRow(new DeleteRowRequest(userPublicRoomRowPutChange), new TableStoreCallback<DeleteRowRequest, DeleteRowResponse>() {
            @Override
            public void onCompleted(DeleteRowRequest req, DeleteRowResponse res) {
                logger.debug("asyncDeleteUserPublicEvent Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(DeleteRowRequest req, Exception ex) {
                logger.error("asyncDeleteUserPublicEvent Error! {}", ex.getMessage(), ex);
            }
        });
    }

    public boolean saveEventNotify(String eventId, String uid) {
        long now = System.currentTimeMillis();
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            // 存 event_notify 表
            RowPutChange eventNotifyRowPutChange = new RowPutChange("event_notify", primaryKeyBuilder.build());
            // 插入值
            eventNotifyRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(eventNotifyRowPutChange));
            return true;
        } catch (Exception e) {
            logger.error("saveEventNotify Error: " + e.getMessage(), e);
        }
        return false;
    }

    public int isEventNotify(String eventId, String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("event_notify", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            return 1;
        }
        return 0;
    }

    public List<String> getEventNotifyUidList(String eventId) {
        List<String> uidList = new ArrayList<>();
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("event_notify");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MIN);
        PrimaryKey startKey = primaryKeyBuilder.build();
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(startKey);
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("event_id", PrimaryKeyValue.fromString(eventId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MAX);
        PrimaryKey endKey = primaryKeyBuilder.build();
        rangeRowQueryCriteria.setExclusiveEndPrimaryKey(endKey);
        // 设置读取最新版本
        rangeRowQueryCriteria.setMaxVersions(1);
        while (true) {
            GetRangeResponse getRangeResponse = dataSyncClient.getRange(new GetRangeRequest(rangeRowQueryCriteria));
            List<Row> rows = getRangeResponse.getRows();
            if (rows == null) {
                break;
            }
            for (Row row : rows) {
                uidList.add(row.getPrimaryKey().getPrimaryKeyColumn("uid").getValue().asString());
            }
            // 如果nextStartPrimaryKey不为null，则继续读取
            if (getRangeResponse.getNextStartPrimaryKey() != null) {
                rangeRowQueryCriteria.setInclusiveStartPrimaryKey(getRangeResponse.getNextStartPrimaryKey());
            } else {
                break;
            }
        }
        return uidList;
    }

}
