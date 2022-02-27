package com.chaoqer.server;

import com.chaoqer.client.AliyunSmsAsyncClient;
import com.chaoqer.client.PushAsyncClient;
import com.chaoqer.common.entity.account.AccountAuthDTO;
import com.chaoqer.common.entity.account.CaptchaDTO;
import com.chaoqer.common.entity.account.CaptchaLoginDTO;
import com.chaoqer.common.entity.aliyun.AccessSmsDTO;
import com.chaoqer.common.entity.base.*;
import com.chaoqer.common.entity.base.group.RpcGroup;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RandomUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.Account;
import com.chaoqer.entity.AccountAuth;
import com.chaoqer.entity.AccountAuthResult;
import com.chaoqer.entity.AccountProperties;
import com.chaoqer.repository.AccountOTS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.Calendar;

@RpcServer(value = "account", type = RpcType.SYNC, threadNum = 4)
public class AccountServer {

    private final static Logger logger = LoggerFactory.getLogger(AccountServer.class);

    @Value("${account.auth.secret-key}")
    private String secretKey;
    // 验证码的失效时间
    @Value("${account.sms.captcha-timeout}")
    private int captchaTimeout;
    // 验证码重发间隔
    @Value("${account.sms.captcha-duration}")
    private int captchaDuration;
    // 每小时短信次数限制
    @Value("${account.sms.hour-total}")
    private int hourTotal;
    // 每天短信次数限制
    @Value("${account.sms.day-total}")
    private int dayTotal;
    // 密码的盐Salt的长度
    @Value("${account.auth.salt-size}")
    private int saltSize;
    // 密码的hash次数，故意增加登录校验的时间
    @Value("${account.auth.hash-iterations}")
    private int hashIterations;

    @Autowired
    private AccountOTS accountOTS;
    @Autowired
    private PushAsyncClient pushAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AccountProperties accountProperties;
    @Autowired
    private AliyunSmsAsyncClient aliyunSmsAsyncClient;

    // 账号验证
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult accountAuth(@Validated({RpcGroup.class}) AccountAuthDTO accountAuthDTO) {
        if (accountOTS.accountAuth(accountAuthDTO)) {
            return ServerResult.build(OperateStatus.SUCCESS);
        }
        return ServerResult.build(OperateStatus.FAILURE);
    }

    // 获取账号密钥
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getAccountSecret(AuthedDTO authedDTO) {
        String secret = accountOTS.getAccountSecret(authedDTO);
        if (StringUtils.isNotBlank(secret)) {
            return ServerResult.buildSuccessResult(secret);
        }
        return ServerResult.build(OperateStatus.FAILURE);
    }

    // 获取短信验证码
    @RpcServerMethod
    public ServerResult getCaptcha(@Validated CaptchaDTO captchaDTO) {
        String countryCode = captchaDTO.getCountryCode();
        String mobile = captchaDTO.getMobile();
        // 运营账号
        if (countryCode.equals("86") && accountProperties.getWhiteMobileList().contains(mobile)) {
            return ServerResult.build(OperateStatus.SUCCESS).message("请输入验证码8888直接登录");
        }
        // 判断是否已经发送
        String accountCaptchaSendKey = RedisKeyGenerator.getAccountCaptchaSendKey(countryCode, mobile);
        if (RedisUtil.isKeyExist(redisTemplate, accountCaptchaSendKey)) {
            logger.warn("country: " + countryCode + ", mobile: " + mobile + ", 验证码发送太频繁，请在" + captchaDuration + "秒后重试");
            return ServerResult.buildFailureMessage("验证码发送太频繁，请在" + captchaDuration + "秒后重试");
        }
        // 验证码间隔
        RedisUtil.setObject(redisTemplate, accountCaptchaSendKey, countryCode.concat(mobile), captchaDuration);
        // TODO 运营免验证登录
        // TODO 需要风险控制
        ClientInfo clientInfo = captchaDTO.getClientInfo();
        // 短信频率限制
        Calendar startTime = Calendar.getInstance();
        startTime.add(Calendar.HOUR_OF_DAY, -1);
        int total = accountOTS.countUserSmsDailyAfter(countryCode, mobile, startTime.getTimeInMillis());
        if (total > hourTotal - 1) {
            logger.warn("country: " + countryCode + ", mobile: " + mobile + ", 验证码发送太频繁，1小时内最多发送" + hourTotal + "条短信");
            return ServerResult.buildFailureMessage("验证码发送太频繁，每小时最多发送" + hourTotal + "条短信");
        }
        startTime.add(Calendar.HOUR_OF_DAY, -23);
        total = accountOTS.countUserSmsDailyAfter(countryCode, mobile, startTime.getTimeInMillis());
        if (total > dayTotal - 1) {
            logger.warn("country: " + countryCode + ", mobile: " + mobile + ", 验证码发送太频繁，24小时内最多发送" + dayTotal + "条短信");
            return ServerResult.buildFailureMessage("验证码发送太频繁，每天最多发送" + dayTotal + "条短信");
        }
        // 生成短信验证码
        String captcha = RandomUtil.getRandNumber(4);
        // 此处添加将验证码发往用户手机
        AccessSmsDTO accessSmsDTO = new AccessSmsDTO();
        accessSmsDTO.setCountryCode(countryCode);
        accessSmsDTO.setMobile(mobile);
        accessSmsDTO.setCaptcha(captcha);
        aliyunSmsAsyncClient.createAccessSms(captchaDTO.buildDTO(accessSmsDTO, true));
        // 缓存验证码
        RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountCaptchaKey(countryCode, mobile), captcha, captchaTimeout);
        // 重置验证次数
        RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAccountCaptchaVerifyCountKey(countryCode, mobile), 0, captchaTimeout);
        // 返回状态
        return ServerResult.build(OperateStatus.SUCCESS).message("验证码已发送，请注意查收");
    }

    // 短信验证码登录
    @RpcServerMethod
    public ServerResult captchaLogin(@Validated CaptchaLoginDTO captchaLoginDTO) {
        ClientInfo clientInfo = captchaLoginDTO.getClientInfo();
        ClientType clientType = ClientType.getClientType(clientInfo.getClientType());
        String countryCode = captchaLoginDTO.getCountryCode();
        String mobile = captchaLoginDTO.getMobile();
        String captcha = captchaLoginDTO.getCaptcha();
        // 校验短信验证码
        if (!verifyCaptcha(countryCode, mobile, captcha)) {
            if (RedisUtil.isKeyExist(redisTemplate, RedisKeyGenerator.getAccountCaptchaKey(countryCode, mobile))) {
                return ServerResult.buildFailureMessage("验证码不正确");
            }
            return ServerResult.buildFailureMessage("验证码已过期，请重新获取验证码");
        }
        long now = System.currentTimeMillis();
        // 登录过去时间默认30天
        int expireTimeDuration = 30 * 24 * 60 * 60;
        AccountAuth accountAuth = new AccountAuth();
        accountAuth.setAccessToken(DigestUtil.getRandString(32));
        accountAuth.setAccessSecret(DigestUtil.getRandString(32));
        accountAuth.setNow(now);
        accountAuth.setExpireTimeDuration(expireTimeDuration);
        // 校验账号状态
        Account account = accountOTS.getAccount(countryCode, mobile);
        // 账号不存在
        if (account == null) {
            account = new Account();
            account.setUid(DigestUtil.getUUID());
            // 第一次注册
            if (accountOTS.saveAccount(countryCode, mobile, account, accountAuth)) {
                AccountAuthResult accountAuthResult = getAccountAuthResult(account.getUid(), accountAuth);
                logger.debug("Register Success, accountAuthResult: " + accountAuthResult);
                return ServerResult.buildSuccessResult(accountAuthResult).message("注册成功");
            }
            logger.warn("Register Failure, captchaLoginDTO: " + captchaLoginDTO);
            return ServerResult.build(OperateStatus.FAILURE);
        }
        AccountStatus accountStatus = AccountStatus.getAccountStatus(account.getStatus());
        // 账号正常
        if (accountStatus == AccountStatus.DEFAULT || accountStatus == AccountStatus.NORMAL) {
            // 更新密钥
            accountOTS.updateAccountAuth(account, accountAuth);
            AccountAuthResult accountAuthResult = getAccountAuthResult(account.getUid(), accountAuth);
            logger.debug("SmsLogin Success, accountAuthResult: " + accountAuthResult);
            //  如果不是登录正常的推送设备, 解绑推送
            if (clientType != ClientType.IOS && clientType != ClientType.ANDROID) {
                UidDTO uidDTO = new UidDTO();
                uidDTO.setUid(account.getUid());
                pushAsyncClient.deleteDevice(uidDTO);
            }
            return ServerResult.buildSuccessResult(accountAuthResult).message("登录成功");
        }
        // 账号被注销 FIXME 不可能出现，因为手机号被处理了
        // 账号被冻结
        return ServerResult.buildFailureMessage(OperateErrorCode.ACCOUNT_LOCKED.getMessage()).errorCode(OperateErrorCode.ACCOUNT_LOCKED.getCode());
    }

    // 登出
    @RpcServerMethod
    public ServerResult logout(AuthedDTO authedDTO) {
        String uid = authedDTO.getAuthedUid();
        // 删除缓存
        RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getAccountTokenKey(uid));
        RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getAccountSecretKey(uid));
        // 解绑推送
        UidDTO uidDTO = new UidDTO();
        uidDTO.setUid(authedDTO.getAuthedUid());
        pushAsyncClient.deleteDevice(uidDTO);
        // 修改数据库
        accountOTS.asyncUpdateAccountAuthExpireTime(authedDTO.getAuthedUid());
        return ServerResult.build(OperateStatus.SUCCESS).message("退出成功");
    }

    // 校验验证码
    private boolean verifyCaptcha(String countryCode, String mobile, String captcha) {
        String captchaKey = RedisKeyGenerator.getAccountCaptchaKey(countryCode, mobile);
        String captchaVerifyCountKey = RedisKeyGenerator.getAccountCaptchaVerifyCountKey(countryCode, mobile);
        if (captcha.equals(RedisUtil.getString(redisTemplate, captchaKey)) || (countryCode.equals("86") && accountProperties.getWhiteMobileList().contains(mobile) && captcha.equals("8888"))) {
            // 验证成功，删除key
            RedisUtil.delObject(redisTemplate, captchaKey);
            RedisUtil.delObject(redisTemplate, captchaVerifyCountKey);
            return true;
        }
        String count = RedisUtil.getString(redisTemplate, captchaVerifyCountKey);
        if (StringUtils.isNotBlank(count)) {
            int countInt = Integer.parseInt(count);
            if (countInt >= 4) {
                // 失败五次，删除key
                RedisUtil.delObject(redisTemplate, captchaKey);
                RedisUtil.delObject(redisTemplate, captchaVerifyCountKey);
            } else {
                RedisUtil.setObject(redisTemplate, captchaVerifyCountKey, countInt + 1);
            }
        }
        return false;
    }

    // 提取认证返回结果
    private AccountAuthResult getAccountAuthResult(String uid, AccountAuth accountAuth) {
        AccountAuthResult accountAuthResult = new AccountAuthResult();
        accountAuthResult.setUid(uid);
        accountAuthResult.setAccessToken(accountAuth.getAccessToken());
        accountAuthResult.setAccessSecret(accountAuth.getAccessSecret());
        accountAuthResult.setExpireTime(accountAuth.getNow() + accountAuth.getExpireTimeDuration() * 1000L);
        return accountAuthResult;
    }

}
