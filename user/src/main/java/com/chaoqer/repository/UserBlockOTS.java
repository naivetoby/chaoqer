package com.chaoqer.repository;

import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.user.UserProfilePageResult;
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

@Component
public class UserBlockOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserBlockOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public JSONObject getUserBlockPageResultList(String uid, Page page) {
        String lastId = page.getLastId();
        int count = page.getCount();
        List<UserProfilePageResult> userProfilePageResultList = new ArrayList<>(count);
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
        primaryKeyBuilder.addPrimaryKeyColumn("block_uid", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("block_uid", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("create_time");
        Pair<List<Row>, PrimaryKey> data;
        while (userProfilePageResultList.size() < count && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "user_block_index", columns, Direction.BACKWARD, startKey, endKey, offset, count);
            for (Row row : data.getFirst()) {
                UserProfilePageResult userProfilePageResult = new UserProfilePageResult();
                userProfilePageResult.setUid(row.getPrimaryKey().getPrimaryKeyColumn("block_uid").getValue().asString());
                userProfilePageResult.setSequenceId(row.getPrimaryKey().getPrimaryKeyColumn("sequence_id").getValue().asString());
                userProfilePageResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                userProfilePageResultList.add(userProfilePageResult);
            }
            startKey = data.getSecond();
        }
        JSONObject result = new JSONObject();
        result.put("page", page);
        result.put("list", userProfilePageResultList);
        return result;
    }

    public boolean isUserBlock(String uid, String blockUid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("block_uid", PrimaryKeyValue.fromString(blockUid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("user_block", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        return row != null && !row.isEmpty();
    }

    public boolean saveUserBlock(String uid, String blockUid) {
        long now = System.currentTimeMillis();
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            primaryKeyBuilder.addPrimaryKeyColumn("block_uid", PrimaryKeyValue.fromString(blockUid));
            // 存 user_block 表
            RowPutChange userBlockRowPutChange = new RowPutChange("user_block", primaryKeyBuilder.build());
            // 插入值
            userBlockRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            userBlockRowPutChange.addColumn("sequence_id", ColumnValue.fromString(now + ":" + blockUid));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(userBlockRowPutChange));
            // 存缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getUserBlockKey(uid, blockUid), 1);
            return true;
        } catch (Exception e) {
            logger.error("saveUserBlock Error: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean deleteUserBlock(String uid, String blockUid) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            primaryKeyBuilder.addPrimaryKeyColumn("block_uid", PrimaryKeyValue.fromString(blockUid));
            // 删 user_block 表
            dataSyncClient.deleteRow(new DeleteRowRequest(new RowDeleteChange("user_block", primaryKeyBuilder.build())));
            // 存缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getUserBlockKey(uid, blockUid), 0);
            return true;
        } catch (Exception e) {
            logger.error("deleteUserBlock Error: " + e.getMessage(), e);
        }
        return false;
    }

}
