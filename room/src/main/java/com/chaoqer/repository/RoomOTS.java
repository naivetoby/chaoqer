package com.chaoqer.repository;

import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.room.RoomCacheResult;
import com.chaoqer.common.entity.room.RoomPageResult;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.TablestoreUtil;
import org.apache.commons.lang3.StringUtils;
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

    public boolean saveRoom(String roomId, String uid, String name, String clubId, int inviteOnly, long now) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 存 room_meta 表
            RowPutChange roomRowPutChange = new RowPutChange("room_meta", primaryKey);
            // 插入值
            roomRowPutChange.addColumn("uid", ColumnValue.fromString(uid));
            if (StringUtils.isNotBlank(name)) {
                roomRowPutChange.addColumn("name", ColumnValue.fromString(name));
            }
            if (StringUtils.isNotBlank(clubId)) {
                roomRowPutChange.addColumn("club_id", ColumnValue.fromString(clubId));
            }
            roomRowPutChange.addColumn("invite_only", ColumnValue.fromLong(inviteOnly));
            roomRowPutChange.addColumn("closed", ColumnValue.fromLong(0));
            roomRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            roomRowPutChange.addColumn("sequence_id", ColumnValue.fromString(now + ":" + roomId));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(roomRowPutChange));
            return true;
        } catch (Exception e) {
            logger.error("saveRoom Error: " + e.getMessage(), e);
        }
        return false;
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
        criteria.addColumnsToGet("invite_only");
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
                if (row.contains("invite_only")) {
                    roomResult.setInviteOnly((int) row.getLatestColumn("invite_only").getValue().asLong());
                }
                roomResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                return roomResult;
            }
        }
        return null;
    }

    public RoomCacheResult getRoomCache(String roomId) {
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
        criteria.addColumnsToGet("invite_only");
        criteria.addColumnsToGet("closed");
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            RoomCacheResult roomCacheResult = new RoomCacheResult();
            roomCacheResult.setRoomCacheId(DigestUtil.getUUID());
            roomCacheResult.setRoomId(roomId);
            roomCacheResult.setUid(row.getLatestColumn("uid").getValue().asString());
            if (row.contains("name")) {
                roomCacheResult.setName(row.getLatestColumn("name").getValue().asString());
            }
            if (row.contains("club_id")) {
                roomCacheResult.setClubId(row.getLatestColumn("club_id").getValue().asString());
            }
            if (row.contains("invite_only")) {
                roomCacheResult.setInviteOnly((int) row.getLatestColumn("invite_only").getValue().asLong());
            }
            roomCacheResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
            roomCacheResult.setClosed((int) row.getLatestColumn("closed").getValue().asLong());
            return roomCacheResult;
        }
        return null;
    }

    private boolean isUserClubRoom(String roomId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("room_meta", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        criteria.addColumnsToGet("club_id");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            return row.contains("club_id") && StringUtils.isNotBlank(row.getLatestColumn("club_id").getValue().asString());
        }
        return false;
    }

    public boolean closeRoom(String roomId) {
        // 当前时间
        long now = System.currentTimeMillis();
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 更新 room_meta 表
            RowUpdateChange rowUpdateChange = new RowUpdateChange("room_meta", primaryKey);
            rowUpdateChange.put("closed", ColumnValue.fromLong(1));
            rowUpdateChange.put("close_time", ColumnValue.fromLong(now));
            // 设置条件更新，行条件检查为期望原行存在
            rowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            dataSyncClient.updateRow(new UpdateRowRequest(rowUpdateChange));
            return true;
        } catch (Exception e) {
            logger.error("saveRoom Error: " + e.getMessage(), e);
        }
        return false;
    }

    public JSONObject getRoomPageResultList(String uid, Page page) {
        String lastId = page.getLastId();
        int count = page.getCount();
        boolean isUserClubRoomFlag = true;
        if (StringUtils.isNotBlank(lastId)) {
            String[] ids = lastId.split(":");
            if (ids.length == 2) {
                isUserClubRoomFlag = isUserClubRoom(lastId.split(":")[1]);
            }
        }
        List<RoomPageResult> roomPageResultList;
        if (isUserClubRoomFlag) {
            roomPageResultList = getRoomPageResultByUserRoom(uid, lastId, count);
        } else {
            roomPageResultList = getRoomPageResultByUserPublicRoom(lastId, count);
        }
        JSONObject result = new JSONObject();
        result.put("page", page);
        result.put("list", roomPageResultList);
        return result;
    }

    public boolean isUserRoomExist(String roomId, String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("user_room", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        return row != null && !row.isEmpty();
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

    public void asyncSaveUserPublicRoom(String roomId, long now) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        // 存 user_public_room 表
        RowPutChange userPublicRoomRowPutChange = new RowPutChange("user_public_room", primaryKeyBuilder.build());
        // 插入值
        userPublicRoomRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
        userPublicRoomRowPutChange.addColumn("sequence_id", ColumnValue.fromString(now + ":" + roomId));
        // 异步更新
        dataAsyncClient.putRow(new PutRowRequest(userPublicRoomRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("asyncSaveUserPublicRoom Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                logger.error("asyncSaveUserPublicRoom Error! {}", ex.getMessage(), ex);
            }
        });
    }

    public void asyncDeleteUserPublicRoom(String roomId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.fromString(roomId));
        // 删除 user_public_room 表
        RowDeleteChange userPublicRoomRowPutChange = new RowDeleteChange("user_public_room", primaryKeyBuilder.build());
        // 异步更新
        dataAsyncClient.deleteRow(new DeleteRowRequest(userPublicRoomRowPutChange), new TableStoreCallback<DeleteRowRequest, DeleteRowResponse>() {
            @Override
            public void onCompleted(DeleteRowRequest req, DeleteRowResponse res) {
                logger.debug("asyncDeleteUserPublicRoom Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(DeleteRowRequest req, Exception ex) {
                logger.error("asyncDeleteUserPublicRoom Error! {}", ex.getMessage(), ex);
            }
        });
    }

    private List<RoomPageResult> getRoomPageResultByUserRoom(String uid, String lastId, int count) {
        List<RoomPageResult> roomPageResultList = new ArrayList<>(count);
        int offset = 1;
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        if (StringUtils.isBlank(lastId)) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString(lastId));
        }
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("create_time");
        Pair<List<Row>, PrimaryKey> data;
        while (roomPageResultList.size() < count && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "user_room_index", columns, Direction.BACKWARD, startKey, endKey, offset, count);
            for (Row row : data.getFirst()) {
                RoomPageResult roomPageResult = new RoomPageResult();
                roomPageResult.setRoomId(row.getPrimaryKey().getPrimaryKeyColumn("room_id").getValue().asString());
                roomPageResult.setSequenceId(row.getPrimaryKey().getPrimaryKeyColumn("sequence_id").getValue().asString());
                roomPageResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                roomPageResultList.add(roomPageResult);
            }
            startKey = data.getSecond();
        }

        // 不够的地方继续添加
        if (roomPageResultList.size() < count) {
            roomPageResultList.addAll(getRoomPageResultByUserPublicRoom("", count - roomPageResultList.size()));
        }

        return roomPageResultList;
    }

    private List<RoomPageResult> getRoomPageResultByUserPublicRoom(String lastId, int count) {
        List<RoomPageResult> roomPageResultList = new ArrayList<>(count);
        int offset = 1;
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        if (StringUtils.isBlank(lastId)) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString(lastId));
        }
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("room_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("create_time");
        Pair<List<Row>, PrimaryKey> data;
        while (roomPageResultList.size() < count && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "user_public_room_index", columns, Direction.BACKWARD, startKey, endKey, offset, count);
            for (Row row : data.getFirst()) {
                RoomPageResult roomPageResult = new RoomPageResult();
                roomPageResult.setRoomId(row.getPrimaryKey().getPrimaryKeyColumn("room_id").getValue().asString());
                roomPageResult.setSequenceId(row.getPrimaryKey().getPrimaryKeyColumn("sequence_id").getValue().asString());
                roomPageResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                roomPageResultList.add(roomPageResult);
            }
            startKey = data.getSecond();
        }
        return roomPageResultList;
    }

}
