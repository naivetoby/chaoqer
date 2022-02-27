package com.chaoqer.server;

import com.chaoqer.common.entity.base.AccountStatus;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.entity.user.group.InitProfileGroup;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.UserProfileOTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

@RpcServer(value = "user-profile", type = RpcType.SYNC, threadNum = 4)
public class UserProfileServer {

    private final static Logger logger = LoggerFactory.getLogger(UserProfileServer.class);

    @Autowired
    private UserProfileOTS userProfileOTS;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 提交用户资料(初始化)
    @RpcServerMethod
    public ServerResult postProfile(@Validated({InitProfileGroup.class}) UserProfileDTO userProfileDTO) {
        String uid = userProfileDTO.getAuthedUid();
        String accountStatusKey = RedisKeyGenerator.getAccountStatusKey(uid);
        if (RedisUtil.isKeyExist(redisTemplate, accountStatusKey) && AccountStatus.getAccountStatus(Integer.parseInt(RedisUtil.getString(redisTemplate, accountStatusKey))) == AccountStatus.NORMAL) {
            // 已初始化
            return ServerResult.buildSuccessResult(userProfileOTS.getProfile(userProfileDTO.getAuthedUid()));
        }
        if (userProfileOTS.initProfile(userProfileDTO)) {
            // 清除缓存
            RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(uid));
        }
        return ServerResult.buildSuccessResult(userProfileOTS.getProfile(userProfileDTO.getAuthedUid()));
    }

    // 修改用户资料
    @RpcServerMethod
    public ServerResult putProfile(@Validated UserProfileDTO userProfileDTO) {
        UserProfileResult userProfileResult = userProfileOTS.getProfile(userProfileDTO.getAuthedUid());
        if (userProfileResult != null) {
            if (userProfileOTS.putProfile(userProfileResult, userProfileDTO)) {
                // 清除缓存
                RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getUserProfileKey(userProfileDTO.getAuthedUid()));
                return ServerResult.buildSuccessResult(userProfileOTS.getProfile(userProfileDTO.getAuthedUid()));
            }
            return ServerResult.build(OperateStatus.FAILURE);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
    }

    // 获取用户资料
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getProfile(UidDTO uidDTO) {
        UserProfileResult userProfileResult = userProfileOTS.getProfile(uidDTO.getUid());
        if (userProfileResult != null) {
            return ServerResult.buildSuccessResult(userProfileResult);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
    }

}
