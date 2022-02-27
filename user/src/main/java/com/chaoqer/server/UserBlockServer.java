package com.chaoqer.server;

import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.UserBlockOTS;
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

@RpcServer(value = "user-block", type = RpcType.SYNC, threadNum = 4)
public class UserBlockServer {

    private final static Logger logger = LoggerFactory.getLogger(UserBlockServer.class);

    @Autowired
    private UserBlockOTS userBlockOTS;
    @Autowired
    private UserProfileOTS userProfileOTS;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 获取拉黑列表
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getUserBlockPageResultList(PageDTO pageDTO) {
        Page page = pageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        return ServerResult.buildSuccessResult(userBlockOTS.getUserBlockPageResultList(pageDTO.getAuthedUid(), page));
    }

    // 是否已经拉黑
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult isUserBlock(@Validated UidDTO uidDTO) {
        String uid = uidDTO.getAuthedUid();
        String blockUid = uidDTO.getUid();
        int blocked = 0;
        if (!blockUid.equals(uid)) {
            blocked = userBlockOTS.isUserBlock(uid, blockUid) ? 1 : 0;
        }
        RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getUserBlockKey(uid, blockUid), blocked);
        return ServerResult.buildSuccessResult(blocked);
    }

    // 拉黑用户
    @RpcServerMethod
    public ServerResult postUserBlock(@Validated UidDTO uidDTO) {
        String uid = uidDTO.getAuthedUid();
        String blockUid = uidDTO.getUid();
        if (blockUid.equals(uid)) {
            return ServerResult.buildFailureMessage("无法屏蔽自己");
        }
        UserProfileResult userProfileResult = userProfileOTS.getProfile(blockUid);
        if (userProfileResult == null) {
            return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
        }
        if (userBlockOTS.isUserBlock(uid, blockUid)) {
            return ServerResult.build(OperateStatus.SUCCESS).message("屏蔽成功");
        }
        if (userBlockOTS.saveUserBlock(uid, blockUid)) {
            return ServerResult.build(OperateStatus.SUCCESS).message("屏蔽成功");
        }
        return ServerResult.buildFailureMessage("屏蔽失败");
    }

    // 移除拉黑用户
    @RpcServerMethod
    public ServerResult deleteUserBlock(@Validated UidDTO uidDTO) {
        String uid = uidDTO.getAuthedUid();
        String blockUid = uidDTO.getUid();
        UserProfileResult userProfileResult = userProfileOTS.getProfile(blockUid);
        if (userProfileResult == null) {
            return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
        }
        if (!userBlockOTS.isUserBlock(uid, blockUid)) {
            return ServerResult.build(OperateStatus.SUCCESS).message("解除屏蔽成功");
        }
        if (userBlockOTS.deleteUserBlock(uid, blockUid)) {
            return ServerResult.build(OperateStatus.SUCCESS).message("解除屏蔽成功");
        }
        return ServerResult.buildFailureMessage("解除屏蔽失败");
    }

}
