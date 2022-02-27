package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClubOTS {

    private final static Logger logger = LoggerFactory.getLogger(ClubOTS.class);

    @Autowired
    private SyncClient dataSyncClient;

    public List<String> getClubMemberUidList(String clubId) {
        List<String> uidList = new ArrayList<>();
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("club_member_club_id_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MIN);
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(primaryKeyBuilder.build());
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MAX);
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

    public boolean isClubMember(String clubId, String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("club_member", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        return row != null && !row.isEmpty();
    }

}
