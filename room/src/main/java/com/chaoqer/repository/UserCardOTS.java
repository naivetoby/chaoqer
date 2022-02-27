package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserCardOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserCardOTS.class);

    @Autowired
    private SyncClient dataSyncClient;

    public boolean isUserSendCard(String originUid, String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("origin_uid", PrimaryKeyValue.fromString(originUid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("user_card", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            return row.getLatestColumn("deleted").getValue().asLong() == 0;
        }
        return false;
    }

}
