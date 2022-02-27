package com.chaoqer.repository;

import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.DeleteStatus;
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
import java.util.concurrent.TimeUnit;

@Component
public class UserCardOTS {

    private final static Logger logger = LoggerFactory.getLogger(UserCardOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public JSONObject getUserCardPageResultList(String uid, Page page) {
        String lastId = page.getLastId();
        int count = page.getCount();
        List<UserProfilePageResult> userProfilePageResultList = new ArrayList<>(count);
        int offset = 1;
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        if (StringUtils.isBlank(lastId)) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString(lastId));
        }
        primaryKeyBuilder.addPrimaryKeyColumn("origin_uid", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("origin_uid", PrimaryKeyValue.INF_MAX);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("create_time");
        Pair<List<Row>, PrimaryKey> data;
        while (userProfilePageResultList.size() < count && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "user_card_index", columns, Direction.BACKWARD, startKey, endKey, offset, count);
            for (Row row : data.getFirst()) {
                UserProfilePageResult userProfilePageResult = new UserProfilePageResult();
                userProfilePageResult.setUid(row.getPrimaryKey().getPrimaryKeyColumn("origin_uid").getValue().asString());
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

    public long getUserCardTotal(String uid) {
        String userCardTotalKey = RedisKeyGenerator.getUserCardTotalKey(uid);
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("user_card_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("origin_uid", PrimaryKeyValue.INF_MIN);
        PrimaryKey startKey = primaryKeyBuilder.build();
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(startKey);
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("deleted", PrimaryKeyValue.fromLong(DeleteStatus.NORMAL.getStatus()));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        primaryKeyBuilder.addPrimaryKeyColumn("origin_uid", PrimaryKeyValue.INF_MAX);
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
        RedisUtil.delObject(redisTemplate, userCardTotalKey);
        RedisUtil.increment(redisTemplate, userCardTotalKey, total);
        RedisUtil.setExpireTime(redisTemplate, userCardTotalKey, 3, TimeUnit.DAYS);
        return total;
    }

    public int getUserSendCardDeleted(String originUid, String uid) {
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
            return (int) row.getLatestColumn("deleted").getValue().asLong();
        }
        return -1;
    }

    public boolean isUserSendCard(String originUid, String uid) {
        return getUserSendCardDeleted(originUid, uid) != -1;
    }

    public boolean saveUserCard(String originUid, String uid, DeleteStatus deleteStatus) {
        String userCardTotalKey = RedisKeyGenerator.getUserCardTotalKey(uid);
        long now = System.currentTimeMillis();
        try {
            if (deleteStatus == DeleteStatus.NORMAL && getUserSendCardDeleted(originUid, uid) == DeleteStatus.NORMAL.getStatus()) {
                return true;
            }
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            primaryKeyBuilder.addPrimaryKeyColumn("origin_uid", PrimaryKeyValue.fromString(originUid));
            // 存 user_card 表
            RowPutChange userCardRowPutChange = new RowPutChange("user_card", primaryKeyBuilder.build());
            // 插入值
            userCardRowPutChange.addColumn("deleted", ColumnValue.fromLong(deleteStatus.getStatus()));
            userCardRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            userCardRowPutChange.addColumn("sequence_id", ColumnValue.fromString(now + ":" + originUid));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(userCardRowPutChange));
            // 存缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getUserSendCardKey(originUid, uid), deleteStatus == DeleteStatus.NORMAL ? 1 : 0);
            // 删除缓存
            RedisUtil.delObject(redisTemplate, userCardTotalKey);
            return true;
        } catch (Exception e) {
            logger.error("saveUserBlock Error: " + e.getMessage(), e);
        }
        return false;
    }

}
