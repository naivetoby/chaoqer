package com.chaoqer.repository;

import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.club.ClubPageResult;
import com.chaoqer.common.entity.club.ClubResult;
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
public class ClubOTS {

    private final static Logger logger = LoggerFactory.getLogger(ClubOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean saveClub(String clubId, String uid, String name, String cover) {
        // 当前时间
        long now = System.currentTimeMillis();
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 存 comment_meta 表
            RowPutChange roomRowPutChange = new RowPutChange("club_meta", primaryKey);
            // 插入值
            roomRowPutChange.addColumn("uid", ColumnValue.fromString(uid));
            roomRowPutChange.addColumn("name", ColumnValue.fromString(name));
            roomRowPutChange.addColumn("cover", ColumnValue.fromString(cover));
            roomRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(roomRowPutChange));
            return true;
        } catch (Exception e) {
            logger.error("saveClub Error: " + e.getMessage(), e);
        }
        return false;
    }

    public ClubResult getClub(String clubId) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("club_meta", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        criteria.addColumnsToGet("uid");
        criteria.addColumnsToGet("name");
        criteria.addColumnsToGet("cover");
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            ClubResult clubResult = new ClubResult();
            clubResult.setClubId(clubId);
            clubResult.setUid(row.getLatestColumn("uid").getValue().asString());
            clubResult.setName(row.getLatestColumn("name").getValue().asString());
            clubResult.setCover(row.getLatestColumn("cover").getValue().asString());
            clubResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
            clubResult.setMemberTotal(getClubMemberTotal(clubId));
            return clubResult;
        }
        return null;
    }

    public boolean deleteClub(String clubId) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 删除 club_member 表
            RowDeleteChange clubMemberRowDeleteChange = new RowDeleteChange("club_meta", primaryKey);
            // 写入数据表
            dataSyncClient.deleteRow(new DeleteRowRequest(clubMemberRowDeleteChange));
            return true;
        } catch (Exception e) {
            logger.error("deleteClub Error: " + e.getMessage(), e);
        }
        return false;
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

    public JSONObject getClubMemberPageResultList(String clubId, Page page) {
        int offset = 1;
        List<UserProfilePageResult> userProfilePageResultList = new ArrayList<>(page.getCount());
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        if (StringUtils.isBlank(page.getLastId())) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString(page.getLastId()));
        }
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("create_time");
        Pair<List<Row>, PrimaryKey> data;
        while (userProfilePageResultList.size() < page.getCount() && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "club_member_club_id_index", columns, Direction.BACKWARD, startKey, endKey, offset, page.getCount());
            for (Row row : data.getFirst()) {
                UserProfilePageResult userProfilePageResult = new UserProfilePageResult();
                userProfilePageResult.setUid(row.getPrimaryKey().getPrimaryKeyColumn("uid").getValue().asString());
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

    public long getClubMemberTotal(String clubId) {
        String clubMemberTotalKey = RedisKeyGenerator.getClubMemberTotalKey(clubId);
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("club_member_club_id_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MIN);
        PrimaryKey startKey = primaryKeyBuilder.build();
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(startKey);
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MAX);
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
        RedisUtil.delObject(redisTemplate, clubMemberTotalKey);
        RedisUtil.increment(redisTemplate, clubMemberTotalKey, total);
        RedisUtil.setExpireTime(redisTemplate, clubMemberTotalKey, 1, TimeUnit.DAYS);
        return total;
    }

    public JSONObject getClubPageResultList(String uid, Page page) {
        int offset = 1;
        List<ClubPageResult> clubPageResultList = new ArrayList<>(page.getCount());
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        if (StringUtils.isBlank(page.getLastId())) {
            offset = 0;
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MAX);
        } else {
            primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.fromString(page.getLastId()));
        }
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("sequence_id", PrimaryKeyValue.INF_MIN);
        primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        List<String> columns = new ArrayList<>();
        columns.add("create_time");
        Pair<List<Row>, PrimaryKey> data;
        while (clubPageResultList.size() < page.getCount() && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "club_member_uid_index", columns, Direction.BACKWARD, startKey, endKey, offset, page.getCount());
            for (Row row : data.getFirst()) {
                ClubPageResult clubPageResult = new ClubPageResult();
                clubPageResult.setClubId(row.getPrimaryKey().getPrimaryKeyColumn("club_id").getValue().asString());
                clubPageResult.setSequenceId(row.getPrimaryKey().getPrimaryKeyColumn("sequence_id").getValue().asString());
                clubPageResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
                clubPageResultList.add(clubPageResult);
            }
            startKey = data.getSecond();
        }
        JSONObject result = new JSONObject();
        result.put("page", page);
        result.put("list", clubPageResultList);
        return result;
    }

    public boolean saveClubMember(String clubId, String uid, int admin) {
        // 当前时间
        long now = System.currentTimeMillis();
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 存 club_member 表
            RowPutChange clubMemberRowPutChange = new RowPutChange("club_member", primaryKey);
            // 插入值
            clubMemberRowPutChange.addColumn("admin", ColumnValue.fromLong(admin));
            clubMemberRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            clubMemberRowPutChange.addColumn("sequence_id", ColumnValue.fromString(now + ":" + clubId));
            // 写入数据表
            dataSyncClient.putRow(new PutRowRequest(clubMemberRowPutChange));
            return true;
        } catch (Exception e) {
            logger.error("saveClubMember Error: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean deleteClubMember(String clubId, String uid) {
        try {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("club_id", PrimaryKeyValue.fromString(clubId));
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 删除 club_member 表
            RowDeleteChange clubMemberRowDeleteChange = new RowDeleteChange("club_member", primaryKey);
            // 写入数据表
            dataSyncClient.deleteRow(new DeleteRowRequest(clubMemberRowDeleteChange));
            return true;
        } catch (Exception e) {
            logger.error("deleteClubMember Error: " + e.getMessage(), e);
        }
        return false;
    }

}
