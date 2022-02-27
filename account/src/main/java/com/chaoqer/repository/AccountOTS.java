package com.chaoqer.repository;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.TableStoreCallback;
import com.alicloud.openservices.tablestore.model.*;
import com.chaoqer.common.entity.account.AccountAuthDTO;
import com.chaoqer.common.entity.base.AccountStatus;
import com.chaoqer.common.entity.base.AuthedDTO;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.Account;
import com.chaoqer.entity.AccountAuth;
import com.chaoqer.entity.AccountAuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountOTS {

    private final static Logger logger = LoggerFactory.getLogger(AccountOTS.class);

    @Autowired
    private Environment env;
    @Autowired
    private SyncClient dataSyncClient;
    @Autowired
    private AsyncClient dataAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean accountAuth(AccountAuthDTO accountAuthDTO) {
        AccountStatus accountStatus = AccountStatus.getAccountStatus(getAccountStatus(accountAuthDTO.getUid()));
        if (accountStatus == AccountStatus.DEFAULT || accountStatus == AccountStatus.NORMAL) {
            AccountAuthResult accountAuthResult = getAccountAuthResult(accountAuthDTO.getUid());
            if (accountAuthResult != null && accountAuthResult.getExpireTime() > System.currentTimeMillis()) {
                int expireTimeDuration = 30 * 24 * 60 * 60;
                // 权限缓存(默认有效期一个月)
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountTokenKey(accountAuthDTO.getUid()), accountAuthResult.getAccessToken(), expireTimeDuration);
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountSecretKey(accountAuthDTO.getUid()), accountAuthResult.getAccessSecret(), expireTimeDuration);
                return true;
            }
        }
        return false;
    }

    public String getAccountSecret(AuthedDTO authedDTO) {
        AccountStatus accountStatus = AccountStatus.getAccountStatus(getAccountStatus(authedDTO.getAuthedUid()));
        if (accountStatus == AccountStatus.DEFAULT || accountStatus == AccountStatus.NORMAL) {
            AccountAuthResult accountAuthResult = getAccountAuthResult(authedDTO.getAuthedUid());
            if (accountAuthResult != null && accountAuthResult.getExpireTime() > System.currentTimeMillis()) {
                int expireTimeDuration = 30 * 24 * 60 * 60;
                // 权限缓存(默认有效期一个月)
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountTokenKey(authedDTO.getAuthedUid()), accountAuthResult.getAccessToken(), expireTimeDuration);
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountSecretKey(authedDTO.getAuthedUid()), accountAuthResult.getAccessSecret(), expireTimeDuration);
                return accountAuthResult.getAccessSecret();
            }
        }
        return "";
    }

    public AccountAuthResult getAccountAuthResult(String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("account_auth", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        // 设置读取某些列
        criteria.addColumnsToGet("access_token");
        criteria.addColumnsToGet("access_secret");
        criteria.addColumnsToGet("expire_time");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            AccountAuthResult accountAuthResult = new AccountAuthResult();
            accountAuthResult.setUid(uid);
            accountAuthResult.setAccessToken(row.getLatestColumn("access_token").getValue().asString());
            accountAuthResult.setAccessSecret(row.getLatestColumn("access_secret").getValue().asString());
            accountAuthResult.setExpireTime(row.getLatestColumn("expire_time").getValue().asLong());
            return accountAuthResult;
        }
        return null;
    }

    public Integer getAccountStatus(String uid) {
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 设置数据表名称
        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria("account", primaryKey);
        // 设置读取最新版本
        criteria.setMaxVersions(1);
        // 设置读取某些列
        criteria.addColumnsToGet("status");
        Row row = dataSyncClient.getRow(new GetRowRequest(criteria)).getRow();
        if (row != null && !row.isEmpty()) {
            int accountStatus = (int) row.getLatestColumn("status").getValue().asLong();
            // 缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountStatusKey(uid), accountStatus);
            return accountStatus;
        }
        return null;
    }

    public int countUserSmsDailyAfter(String countryCode, String mobile, long startTime) {
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("user_sms_daily");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("country_code_mobile_md5", PrimaryKeyValue.fromString(DigestUtil.md5(countryCode.concat(":").concat(mobile))));
        primaryKeyBuilder.addPrimaryKeyColumn("create_time", PrimaryKeyValue.fromLong(startTime));
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(primaryKeyBuilder.build());
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("country_code_mobile_md5", PrimaryKeyValue.fromString(DigestUtil.md5(countryCode.concat(":").concat(mobile))));
        primaryKeyBuilder.addPrimaryKeyColumn("create_time", PrimaryKeyValue.INF_MAX);
        rangeRowQueryCriteria.setExclusiveEndPrimaryKey(primaryKeyBuilder.build());
        // 设置读取最新版本
        rangeRowQueryCriteria.setMaxVersions(1);
        int total = 0;
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
        return total;
    }

    public Account getAccount(String countryCode, String mobile) {
        // 设置数据表名称
        RangeRowQueryCriteria rangeRowQueryCriteria = new RangeRowQueryCriteria("account_country_code_mobile_index");
        // 构造起始主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("country_code_mobile_md5", PrimaryKeyValue.fromString(DigestUtil.md5(countryCode.concat(":").concat(mobile))));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MIN);
        rangeRowQueryCriteria.setInclusiveStartPrimaryKey(primaryKeyBuilder.build());
        // 构造结束主键
        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("country_code_mobile_md5", PrimaryKeyValue.fromString(DigestUtil.md5(countryCode.concat(":").concat(mobile))));
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.INF_MAX);
        rangeRowQueryCriteria.setExclusiveEndPrimaryKey(primaryKeyBuilder.build());
        // 设置读取最新版本
        rangeRowQueryCriteria.setMaxVersions(1);
        // 设置读取某些列
        rangeRowQueryCriteria.addColumnsToGet("status");
        rangeRowQueryCriteria.addColumnsToGet("password");
        rangeRowQueryCriteria.addColumnsToGet("salt");
        // 查询
        List<Row> rows = dataSyncClient.getRange(new GetRangeRequest(rangeRowQueryCriteria)).getRows();
        if (rows != null && !rows.isEmpty()) {
            Row row = rows.get(0);
            Account account = new Account();
            account.setUid(row.getPrimaryKey().getPrimaryKeyColumn("uid").getValue().asString());
            account.setStatus((int) row.getLatestColumn("status").getValue().asLong());
            if (row.contains("password")) {
                account.setPassword(row.getLatestColumn("password").getValue().asString());
            }
            if (row.contains("salt")) {
                account.setSalt(row.getLatestColumn("salt").getValue().asString());
            }
            return account;
        }
        return null;
    }

    public boolean saveAccount(String countryCode, String mobile, Account account, AccountAuth accountAuth) {
        // 当前时间
        long now = accountAuth.getNow();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(account.getUid()));
        PrimaryKey primaryKey = primaryKeyBuilder.build();
        // 局部事务
        String accountTransactionID = "";
        String userProfileTransactionID = "";
        if (CommonUtil.isProdEnvironment(env)) {
            accountTransactionID = dataSyncClient.startLocalTransaction(new StartLocalTransactionRequest("account", primaryKey)).getTransactionID();
            userProfileTransactionID = dataSyncClient.startLocalTransaction(new StartLocalTransactionRequest("user_profile", primaryKey)).getTransactionID();
        }
        try {
            // 存 account 表
            RowPutChange accountRowPutChange = new RowPutChange("account", primaryKey);
            // 插入值
            accountRowPutChange.addColumn("country_code_mobile_md5", ColumnValue.fromString(DigestUtil.md5(countryCode.concat(":").concat(mobile))));
            accountRowPutChange.addColumn("country_code", ColumnValue.fromString(countryCode));
            accountRowPutChange.addColumn("mobile", ColumnValue.fromString(mobile));
            accountRowPutChange.addColumn("username", ColumnValue.fromString(account.getUid()));
            accountRowPutChange.addColumn("status", ColumnValue.fromLong(AccountStatus.DEFAULT.getStatus()));
            accountRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
            accountRowPutChange.addColumn("update_time", ColumnValue.fromLong(now));
            accountRowPutChange.addColumn("active_time", ColumnValue.fromLong(now));
            PutRowRequest accountPutRowRequest = new PutRowRequest(accountRowPutChange);
            if (CommonUtil.isProdEnvironment(env)) {
                accountPutRowRequest.setTransactionId(accountTransactionID);
            }
            dataSyncClient.putRow(accountPutRowRequest);
            // 存 user_profile 表
            RowPutChange useProfilePutChange = new RowPutChange("user_profile", primaryKey);
            useProfilePutChange.addColumn("create_time", ColumnValue.fromLong(now));
            useProfilePutChange.addColumn("update_time", ColumnValue.fromLong(now));
            PutRowRequest useProfilePutRowRequest = new PutRowRequest(useProfilePutChange);
            if (CommonUtil.isProdEnvironment(env)) {
                useProfilePutRowRequest.setTransactionId(userProfileTransactionID);
            }
            dataSyncClient.putRow(useProfilePutRowRequest);
            // 提交事务
            if (CommonUtil.isProdEnvironment(env)) {
                dataSyncClient.commitTransaction(new CommitTransactionRequest(accountTransactionID));
                dataSyncClient.commitTransaction(new CommitTransactionRequest(userProfileTransactionID));
            }
            // 用户默认状态
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountStatusKey(account.getUid()), AccountStatus.DEFAULT.getStatus());
            // 权限缓存(默认有效期一个月)
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountTokenKey(account.getUid()), accountAuth.getAccessToken(), accountAuth.getExpireTimeDuration());
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountSecretKey(account.getUid()), accountAuth.getAccessSecret(), accountAuth.getExpireTimeDuration());
            // 更新 account_auth 表
            updateAccountAuth(account, accountAuth);
            return true;
        } catch (Exception e) {
            if (CommonUtil.isProdEnvironment(env)) {
                dataSyncClient.abortTransaction(new AbortTransactionRequest(accountTransactionID));
                dataSyncClient.abortTransaction(new AbortTransactionRequest(userProfileTransactionID));
            }
            logger.error("saveAccount Error: " + e.getMessage(), e);
        }
        return false;
    }

    public void updateAccountAuth(Account account, AccountAuth accountAuth) {
        // 当前时间
        long now = accountAuth.getNow();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(account.getUid()));
        PrimaryKey accountAuthPrimaryKey = primaryKeyBuilder.build();
        try {
            // 更新 account_auth 表
            RowUpdateChange accountAuthRowUpdateChange = new RowUpdateChange("account_auth", accountAuthPrimaryKey);
            accountAuthRowUpdateChange.put("access_token", ColumnValue.fromString(accountAuth.getAccessToken()));
            accountAuthRowUpdateChange.put("access_secret", ColumnValue.fromString(accountAuth.getAccessSecret()));
            accountAuthRowUpdateChange.put("expire_time", ColumnValue.fromLong(now + accountAuth.getExpireTimeDuration() * 1000L));
            accountAuthRowUpdateChange.put("login_time", ColumnValue.fromLong(now));
            accountAuthRowUpdateChange.put("update_time", ColumnValue.fromLong(now));
            // 设置条件更新，行条件检查为期望原行存在
            accountAuthRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
            dataSyncClient.updateRow(new UpdateRowRequest(accountAuthRowUpdateChange));
        } catch (Exception e) {
            if (e.getMessage().equals("Condition check failed.")) {
                // 不存在数据，需要插入
                RowPutChange accountAuthRowPutChange = new RowPutChange("account_auth", accountAuthPrimaryKey);
                accountAuthRowPutChange.addColumn("access_token", ColumnValue.fromString(accountAuth.getAccessToken()));
                accountAuthRowPutChange.addColumn("access_secret", ColumnValue.fromString(accountAuth.getAccessSecret()));
                accountAuthRowPutChange.addColumn("expire_time", ColumnValue.fromLong(now + accountAuth.getExpireTimeDuration() * 1000L));
                accountAuthRowPutChange.addColumn("login_time", ColumnValue.fromLong(now));
                accountAuthRowPutChange.addColumn("create_time", ColumnValue.fromLong(now));
                accountAuthRowPutChange.addColumn("update_time", ColumnValue.fromLong(now));
                // 设置条件更新，行条件检查为期望原行不存在
//                accountAuthRowPutChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_NOT_EXIST));
                dataSyncClient.putRow(new PutRowRequest(accountAuthRowPutChange));
            }
        }
        // 权限缓存(默认有效期一个月)
        RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountTokenKey(account.getUid()), accountAuth.getAccessToken(), accountAuth.getExpireTimeDuration());
        RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountSecretKey(account.getUid()), accountAuth.getAccessSecret(), accountAuth.getExpireTimeDuration());
    }

    public void asyncUpdateAccountAuthExpireTime(String uid) {
        // 当前时间
        long now = System.currentTimeMillis();
        // 构造主键
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn("uid", PrimaryKeyValue.fromString(uid));
        PrimaryKey accountAuthPrimaryKey = primaryKeyBuilder.build();
        // 更新 account_auth 表
        RowUpdateChange accountAuthRowUpdateChange = new RowUpdateChange("account_auth", accountAuthPrimaryKey);
        accountAuthRowUpdateChange.put("expire_time", ColumnValue.fromLong(now - 1000L));
        accountAuthRowUpdateChange.put("update_time", ColumnValue.fromLong(now));
        // 设置条件更新，行条件检查为期望原行存在
        accountAuthRowUpdateChange.setCondition(new Condition(RowExistenceExpectation.EXPECT_EXIST));
        // 异步更新
        dataAsyncClient.updateRow(new UpdateRowRequest(accountAuthRowUpdateChange), new TableStoreCallback<UpdateRowRequest, UpdateRowResponse>() {
            @Override
            public void onCompleted(UpdateRowRequest req, UpdateRowResponse res) {
                logger.debug("asyncUpdateAccountAuthExpireTime Success! requestId: {}", res.getRequestId());
            }

            @Override
            public void onFailed(UpdateRowRequest req, Exception ex) {
                logger.error("asyncUpdateAccountAuthExpireTime Error! {}", ex.getMessage(), ex);
            }
        });
    }

}
