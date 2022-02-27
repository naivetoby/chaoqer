package com.chaoqer.repository;

import com.alibaba.fastjson.JSON;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.entity.OriginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomOTS {

    private final static Logger logger = LoggerFactory.getLogger(RoomOTS.class);

    @Autowired
    private SyncClient dataSyncClient;

    public OriginResult getOriginResultByRoomId(String roomId) {
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
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
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
            // 组合结果
            OriginResult originResult = new OriginResult();
            originResult.setOriginId(roomId);
            originResult.setUid(roomResult.getUid());
            originResult.setCreateTime(roomResult.getCreateTime());
            originResult.setContent(JSON.toJSONString(roomResult));
            return originResult;
        }
        return null;
    }

}
