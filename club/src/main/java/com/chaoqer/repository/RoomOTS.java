package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.room.RoomResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoomOTS {

    private final static Logger logger = LoggerFactory.getLogger(RoomOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;

    public List<String> getOpenedClubRoomIdList(String clubId) {
        List<String> roomIdList = new ArrayList<>();
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("room_meta_club_id_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("closed", PrimaryKeyValue.fromLong(0));
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MIN);
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(primaryKeyBuilder.build());
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("closed", PrimaryKeyValue.fromLong(0));
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MAX);
        rangeRowQueryCriteria.setExclusiveEndPrimaryKey(primaryKeyBuilder.build());
        // 设置读取最新版本
        rangeRowQueryCriteria.setMaxVersions(1);
        // 查询
        while (true) {
            GetRangeResponse getRangeResponse = dataSyncClient.getRange(new GetRangeRequest(rangeRowQueryCriteria));
            List<Row> rows = getRangeResponse.getRows();
            if (rows == null) {
                break;
            }
            for (Row row : rows) {
                roomIdList.add(row.getPrimaryKey().getPrimaryKeyColumn("room_id").getValue().asString());
            }
            // 如果nextStartPrimaryKey不为null，则继续读取
            if (getRangeResponse.getNextStartPrimaryKey() != null) {
                rangeRowQueryCriteria.setInclusiveStartPrimaryKey(getRangeResponse.getNextStartPrimaryKey());
            } else {
                break;
            }
        }
        return roomIdList;
    }

    public List<String> getAllClubRoomIdList(String clubId) {
        List<String> roomIdList = new ArrayList<>();
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("room_meta_club_id_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("closed", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MIN);
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(primaryKeyBuilder.build());
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("closed", PrimaryKeyValue.INF_MAX);
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MAX);
        rangeRowQueryCriteria.setExclusiveEndPrimaryKey(primaryKeyBuilder.build());
        // 设置读取最新版本
        rangeRowQueryCriteria.setMaxVersions(1);
        // 查询
        while (true) {
            GetRangeResponse getRangeResponse = dataSyncClient.getRange(new GetRangeRequest(rangeRowQueryCriteria));
            List<Row> rows = getRangeResponse.getRows();
            if (rows == null) {
                break;
            }
            for (Row row : rows) {
                roomIdList.add(row.getPrimaryKey().getPrimaryKeyColumn("room_id").getValue().asString());
            }
            // 如果nextStartPrimaryKey不为null，则继续读取
            if (getRangeResponse.getNextStartPrimaryKey() != null) {
                rangeRowQueryCriteria.setInclusiveStartPrimaryKey(getRangeResponse.getNextStartPrimaryKey());
            } else {
                break;
            }
        }
        return roomIdList;
    }

    public RoomResult getRoom(String roomId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("room_meta", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        criteria.addColumnsToGet("uid");
        criteria.addColumnsToGet("name");
        criteria.addColumnsToGet("club_id");
        criteria.addColumnsToGet("closed");
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            int closed = (int) row.getLatestColumn("closed").getValue().asLong();
            if (closed == 0) {
                RoomResult roomResult = new RoomResult();
                roomResult.setRoomId(roomId);
                roomResult.setUid(row.getLatestColumn("uid").getValue().asString());
                if (row.contains("name")) {
                    roomResult.setName(row.getLatestColumn("name").getValue().asString());
                }
                if (row.contains("club_id")) {
                    roomResult.setClubId(row.getLatestColumn("club_id").getValue().asString());
                }
                roomResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                return roomResult;
            }
        }
        return null;
    }

    public void asyncSaveUserRoom(String roomId, String uid, long now) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        // 存 user_room 表
        RowPutChange userRoomRowPutChange = new RowPutChange("user_room", primaryKeyBuilder.build());
        // 插入值
        userRoomRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
        userRoomRowPutChange.addColumn("sequence_id", ColumnValue.fromString(now + ":" + roomId));
        // 异步更新
        dataAsyncClient.putRow(new PutRowRequest(userRoomRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("asyncSaveUserRoom Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                logger.error("asyncSaveUserRoom Error! {}", ex.getMessage(), ex);
            }
        });
    }

    public void asyncDeleteUserRoom(String roomId, String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        // 删除 user_room 表
        RowDeleteChange userRoomRowPutChange = new RowDeleteChange("user_room", primaryKeyBuilder.build());
        // 异步更新
        dataAsyncClient.deleteRow(new DeleteRowRequest(userRoomRowPutChange), new TableStoreCallback<DeleteRowRequest, DeleteRowResponse>() {
            @Override
            public void onCompleted(DeleteRowRequest req, DeleteRowResponse res) {
                logger.debug("asyncDeleteUserRoom Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(DeleteRowRequest req, Exception ex) {
                logger.error("asyncDeleteUserRoom Error! {}", ex.getMessage(), ex);
            }
        });
    }

}
