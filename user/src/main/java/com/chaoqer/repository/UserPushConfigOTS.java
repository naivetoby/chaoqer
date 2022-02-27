package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserPushConfigOTS {

    @Autowired
    private SyncClient dataSyncClient;

    public Integer getPushConfigEnable(String uid, int pushConfigType) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("push_config_type", PrimaryKeyValue.fromLong(pushConfigType));
        PrimaryKey pushConfigPrimaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("user_push_config", pushConfigPrimaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        // 设置读取某些列
        criteria.addColumnsToGet("enable");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            return (int) row.getLatestColumn("enable").getValue().asLong();
        }
        return null;
    }

    public void savePushConfig(String uid, int pushConfigType, int enable) {
        // 当前时间
        long now = System.currentTimeMillis();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("push_config_type", PrimaryKeyValue.fromLong(pushConfigType));
        PrimaryKey pushConfigPrimaryKey = primaryKeyBuilder.build();
        try {
            // 更新 user_push_config 表
            RowUpdateChange pushConfigRowUpdateChange = new RowUpdateChange("user_push_config", pushConfigPrimaryKey);
            pushConfigRowUpdateChange.put("enable", ColumnValue.fromLong(enable));
            pushConfigRowUpdateChange.put("update_time", ColumnValue.fromLong(now));
            // 设置条件更新，行条件检查为期望原行存在
            pushConfigRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            dataSyncClient.updateRow(new UpdateRowRequest(pushConfigRowUpdateChange));
        } catch (Exception e) {
            if (e.getMessage().equals("Condition check failed.")) {
                // 不存在数据，需要插入
                RowPutChange pushConfigRowPutChange = new RowPutChange("user_push_config", pushConfigPrimaryKey);
                pushConfigRowPutChange.addColumn("uid", ColumnValue.fromString(uid));
                pushConfigRowPutChange.addColumn("push_config_type", ColumnValue.fromLong(pushConfigType));
                pushConfigRowPutChange.addColumn("enable", ColumnValue.fromLong(enable));
                pushConfigRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
                pushConfigRowPutChange.addColumn("update_time", ColumnValue.fromLong(now));
                dataSyncClient.putRow(new PutRowRequest(pushConfigRowPutChange));
            }
        }
    }

}
