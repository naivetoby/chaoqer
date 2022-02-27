package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.aliyun.AccessSmsDTO;
import com.chaoqer.common.util.DigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountOTS {

    private final static Logger logger = LoggerFactory.getLogger(AccountOTS.class);

    @Autowired
    private AsyncClient dataAsyncClient;

    // 存 user_sms_daily 表
    public void saveUserSmsDaily(AccessSmsDTO accessSmsDTO) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("country_code_mobile_md5", PrimaryKeyValue.fromString(DigestUtil.md5(accessSmsDTO.getCountryCode().concat(":").concat(accessSmsDTO.getMobile()))));
        primaryKeyBuilder.addPrimaryKeyColumn("create_time", PrimaryKeyValue.fromLong(System.currentTimeMillis()));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        RowPutChange activeLogRowPutChange = new RowPutChange("user_sms_daily", primaryKey);
        // 写入数据到表格存储
        dataAsyncClient.putRow(new PutRowRequest(activeLogRowPutChange), new TableStoreCallback<PutRowRequest, PutRowResponse>() {
            @Override
            public void onCompleted(PutRowRequest req, PutRowResponse res) {
                logger.debug("saveUserSmsDaily Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(PutRowRequest req, Exception ex) {
                logger.error("saveUserSmsDaily Error! {}", ex.getMessage(), ex);
            }
        });
    }

}
