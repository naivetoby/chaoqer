package com.chaoqer.repository;

import com.alibaba.fastjson.JSON;
import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.app.AppUserActiveLogDTO;
import com.chaoqer.common.entity.base.ClientInfo;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LogOTS {

    private final static Logger logger = LoggerFactory.getLogger(LogOTS.class);

    @Autowired
    private AsyncClient logAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void createAppUserActiveLog(AppUserActiveLogDTO appUserActiveLogDTO) {
        // 存 app_user_client_meta 表
        String accountClientMetaMd5Key = RedisKeyGenerator.getAccountClientMetaMd5Key(appUserActiveLogDTO.getClientMetaId());
        if (!RedisUtil.isKeyExist(redisTemplate, accountClientMetaMd5Key)) {
            // 构造主键
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("client_meta_id", PrimaryKeyValue.fromString(appUserActiveLogDTO.getClientMetaId()));
            primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(appUserActiveLogDTO.getAuthedUid()));
            primaryKeyBuilder.addPrimaryKeyColumn("version_id", PrimaryKeyValue.fromString(appUserActiveLogDTO.getVersionId()));
            PrimaryKey primaryKey = primaryKeyBuilder.build();
            // 设备信息
            ClientInfo clientInfo = appUserActiveLogDTO.getClientInfo();
            // 设置数据表名称
            RowPutChange clientMetaRowPutChange = new RowPutChange("app_user_client_meta", primaryKey);
            // 设置条件更新，行条件检查为期望原行不存在
            clientMetaRowPutChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_NOT_EXIST));
            // 加入属性列
            clientMetaRowPutChange.addColumn(new Column("client_type", ColumnValue.fromLong(clientInfo.getClientType())));
            clientMetaRowPutChange.addColumn(new Column("version_name", ColumnValue.fromString(clientInfo.getVersionName())));
            clientMetaRowPutChange.addColumn(new Column("version_code", ColumnValue.fromLong(clientInfo.getVersionCode())));
            clientMetaRowPutChange.addColumn(new Column("create_time", ColumnValue.fromLong(System.currentTimeMillis())));
            // 写入数据到表格存储
            logAsyncClient.putRow(new PutRowRequest(clientMetaRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
                @Override
                public void onCompleted(PutRowRequest req, PutRowResponse res) {
                    // 存一天的缓存
                    RedisUtil.setObject(redisTemplate, accountClientMetaMd5Key, true, 1, TimeUnit.DAYS);
                    logger.debug("createAppUserActiveLog clientMetaRowPutChange Success! requestId: {}", res.getRequestId());
                }

                @Override
                public void onFailed(PutRowRequest req, Exception ex) {
                    if (!ex.getMessage().equals("Condition check failed.")) {
                        logger.error("createAppUserActiveLog clientMetaRowPutChange Error! {}", ex.getMessage(), ex);
                    }
                }
            });
        }
        // 存 app_user_active_log 表
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("client_meta_id", PrimaryKeyValue.fromString(appUserActiveLogDTO.getClientMetaId()));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(appUserActiveLogDTO.getAuthedUid()));
        primaryKeyBuilder.addPrimaryKeyColumn("create_time", PrimaryKeyValue.fromLong(System.currentTimeMillis()));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        RowPutChange activeLogRowPutChange = new RowPutChange("app_user_active_log", primaryKey);
        // 加入属性列
        activeLogRowPutChange.addColumn(new Column("api_path", ColumnValue.fromString(appUserActiveLogDTO.getApiPath())));
        activeLogRowPutChange.addColumn(new Column("parameter_map", ColumnValue.fromString(JSON.toJSONString(appUserActiveLogDTO.getParameterMap()))));
        activeLogRowPutChange.addColumn(new Column("ip", ColumnValue.fromString(appUserActiveLogDTO.getIp())));
        // 写入数据到表格存储
        logAsyncClient.putRow(new PutRowRequest(activeLogRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("createAppUserActiveLog activeLogRowPutChange Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                logger.error("createAppUserActiveLog activeLogRowPutChange Error! {}", ex.getMessage(), ex);
            }
        });
    }

}
