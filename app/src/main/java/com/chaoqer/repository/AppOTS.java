package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.app.VersionResult;
import com.chaoqer.common.entity.base.ClientInfo;
import com.chaoqer.common.entity.base.FeedbackType;
import com.chaoqer.common.entity.base.ReportReasonType;
import com.chaoqer.common.entity.base.origin.OriginFeedBackType;
import com.chaoqer.common.entity.base.origin.OriginReportType;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.common.util.TablestoreUtil;
import com.chaoqer.entity.OriginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AppOTS {

    private final static Logger logger = LoggerFactory.getLogger(AppOTS.class);

    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public VersionResult getVersion(String versionId) {
        VersionResult versionResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getAppVersionKey(versionId), VersionResult.class);
        if (versionResult != null) {
            return versionResult;
        }
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("version_id", PrimaryKeyValue.fromString(versionId));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("app_version", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        criteria.addColumnsToGet("client_type");
        criteria.addColumnsToGet("version_code");
        criteria.addColumnsToGet("version_name");
        criteria.addColumnsToGet("version_desc");
        criteria.addColumnsToGet("forced_upgrade");
        criteria.addColumnsToGet("download_url");
        criteria.addColumnsToGet("create_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            versionResult = new VersionResult();
            versionResult.setVersionId(row.getPrimaryKey().getPrimaryKeyColumn("version_id").getValue().asString());
            versionResult.setClientType((int) row.getLatestColumn("client_type").getValue().asLong());
            versionResult.setVersionCode((int) row.getLatestColumn("version_code").getValue().asLong());
            versionResult.setVersionName(row.getLatestColumn("version_name").getValue().asString());
            versionResult.setVersionDesc(row.getLatestColumn("version_desc").getValue().asString());
            versionResult.setForcedUpgrade((int) row.getLatestColumn("forced_upgrade").getValue().asLong());
            versionResult.setDownloadUrl(row.getLatestColumn("download_url").getValue().asString());
            versionResult.setCreateTime(row.getLatestColumn("create_time").getValue().asLong());
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAppVersionKey(versionId), versionResult);
            return versionResult;
        }
        return null;
    }

    public List<VersionResult> getLatestVersionList(int clientType, int currentVersionCode) {
        List<VersionResult> versionResultList = new ArrayList<>(10);
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("client_type", PrimaryKeyValue.fromLong(clientType));
        primaryKeyBuilder.addPrimaryKeyColumn("version_code", PrimaryKeyValue.INF_MAX);
        primaryKeyBuilder.addPrimaryKeyColumn("version_id", PrimaryKeyValue.INF_MAX);
        PrimaryKey startKey = primaryKeyBuilder.build();
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("client_type", PrimaryKeyValue.fromLong(clientType));
        primaryKeyBuilder.addPrimaryKeyColumn("version_code", PrimaryKeyValue.fromLong(currentVersionCode));
        primaryKeyBuilder.addPrimaryKeyColumn("version_id", PrimaryKeyValue.INF_MIN);
        PrimaryKey endKey = primaryKeyBuilder.build();
        // 查询
        Pair<List<Row>, PrimaryKey> data;
        while (versionResultList.size() < 10 && startKey != null) {
            data = TablestoreUtil.readByPage(dataSyncClient, "app_version_index", null, Direction.BACKWARD, startKey, endKey, 0, 10);
            for (Row row : data.getFirst()) {
                VersionResult versionResult = getVersion(row.getPrimaryKey().getPrimaryKeyColumn("version_id").getValue().asString());
                if (versionResult != null) {
                    versionResultList.add(versionResult);
                }
            }
            startKey = data.getSecond();
        }
        return versionResultList;
    }

    public void asyncSaveFeedback(String uid, OriginFeedBackType originFeedBackType, OriginResult originResult, ClientInfo clientInfo, FeedbackType feedbackType, String feedbackContent, List<String> feedbackImageList) {
        // 当前时间
        long now = System.currentTimeMillis();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("origin_feedback_type", PrimaryKeyValue.fromLong(originFeedBackType.getType()));
        primaryKeyBuilder.addPrimaryKeyColumn("origin_id", PrimaryKeyValue.fromString(originResult == null ? "-1" : originResult.getOriginId()));
        primaryKeyBuilder.addPrimaryKeyColumn("create_time", PrimaryKeyValue.fromLong(now));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 存 app_feedback 表
        RowPutChange appFeedbackRowPutChange = new RowPutChange("app_feedback", primaryKey);
        // 插入值
        if (originResult != null) {
            appFeedbackRowPutChange.addColumn("origin_uid", ColumnValue.fromString(originResult.getUid()));
            appFeedbackRowPutChange.addColumn("origin_content", ColumnValue.fromString(originResult.getContent()));
            appFeedbackRowPutChange.addColumn("origin_content_create_time", ColumnValue.fromLong(originResult.getCreateTime()));
        }
        appFeedbackRowPutChange.addColumn("client_type", ColumnValue.fromLong(clientInfo.getClientType()));
        appFeedbackRowPutChange.addColumn("version_name", ColumnValue.fromString(clientInfo.getVersionName()));
        appFeedbackRowPutChange.addColumn("version_code", ColumnValue.fromLong(clientInfo.getVersionCode()));
        appFeedbackRowPutChange.addColumn("feedback_type", ColumnValue.fromLong(feedbackType.getType()));
        appFeedbackRowPutChange.addColumn("feedback_content", ColumnValue.fromString(feedbackContent));
        if (feedbackImageList != null && feedbackImageList.size() > 0) {
            appFeedbackRowPutChange.addColumn("feedback_image_list", ColumnValue.fromString(String.join(",", feedbackImageList)));
        }
        appFeedbackRowPutChange.addColumn("status", ColumnValue.fromLong(0));
        // 异步更新
        dataAsyncClient.putRow(new PutRowRequest(appFeedbackRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("asyncSaveFeedback Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                if (!ex.getMessage().equals("Condition check failed.")) {
                    logger.error("asyncSaveFeedback Error! {}", ex.getMessage(), ex);
                }
            }
        });
    }

    public void asyncSaveReport(String uid, OriginReportType originReportType, OriginResult originResult, ReportReasonType reportReasonType, String reason) {
        // 当前时间
        long now = System.currentTimeMillis();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        primaryKeyBuilder.addPrimaryKeyColumn("origin_report_type", PrimaryKeyValue.fromLong(originReportType.getType()));
        primaryKeyBuilder.addPrimaryKeyColumn("origin_id", PrimaryKeyValue.fromString(originResult.getOriginId()));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 存 app_report 表
        RowPutChange appReportRowPutChange = new RowPutChange("app_report", primaryKey);
        // 插入值
        appReportRowPutChange.addColumn("origin_uid", ColumnValue.fromString(originResult.getUid()));
        appReportRowPutChange.addColumn("origin_content", ColumnValue.fromString(originResult.getContent()));
        appReportRowPutChange.addColumn("origin_content_create_time", ColumnValue.fromLong(originResult.getCreateTime()));
        appReportRowPutChange.addColumn("report_reason_type", ColumnValue.fromLong(reportReasonType.getType()));
        appReportRowPutChange.addColumn("reason", ColumnValue.fromString(reportReasonType == ReportReasonType.OTHER ? reason : reportReasonType.getReason()));
        appReportRowPutChange.addColumn("status", ColumnValue.fromLong(0));
        appReportRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
        // 设置条件更新，行条件检查为期望原行不存在
        appReportRowPutChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_NOT_EXIST));
        // 异步更新
        dataAsyncClient.putRow(new PutRowRequest(appReportRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("asyncSaveReport Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                if (!ex.getMessage().equals("Condition check failed.")) {
                    logger.error("asyncSaveReport Error! {}", ex.getMessage(), ex);
                }
            }
        });
    }

}
