package com.chaoqer.server;

import com.chaoqer.client.PushAsyncClient;
import com.chaoqer.common.entity.base.DeleteStatus;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.push.AlertMessageDTO;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.entity.user.UserProfileResult;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.UserBlockOTS;
import com.chaoqer.repository.UserCardOTS;
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

@RpcServer(value = "user-card", type = RpcType.SYNC, threadNum = 4)
public class UserCardServer {

    private final static Logger logger = LoggerFactory.getLogger(UserCardServer.class);

    @Autowired
    private UserCardOTS userCardOTS;
    @Autowired
    private UserBlockOTS userBlockOTS;
    @Autowired
    private UserProfileOTS userProfileOTS;
    @Autowired
    private PushAsyncClient pushAsyncClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 获取名片列表
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getUserCardPageResultList(PageDTO pageDTO) {
        Page page = pageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        return ServerResult.buildSuccessResult(userCardOTS.getUserCardPageResultList(pageDTO.getAuthedUid(), page));
    }

    // 获取名片总数
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getUserCardTotal(@Validated UidDTO uidDTO) {
        String uid = uidDTO.getUid();
        UserProfileResult userProfileResult = userProfileOTS.getProfile(uid);
        if (userProfileResult == null) {
            return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
        }
        return ServerResult.buildSuccessResult(userCardOTS.getUserCardTotal(uid));
    }

    // 是否已经发送过名片
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult isUserSendCard(@Validated UidDTO uidDTO) {
        String originUid = uidDTO.getAuthedUid();
        String uid = uidDTO.getUid();
        int sendCard = 0;
        if (!originUid.equals(uid)) {
            sendCard = userCardOTS.isUserSendCard(originUid, uid) ? 1 : 0;
        }
        RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getUserSendCardKey(originUid, uid), sendCard);
        return ServerResult.buildSuccessResult(sendCard);
    }

    // 发送名片
    @RpcServerMethod
    public ServerResult postUserCard(@Validated UidDTO uidDTO) {
        String originUid = uidDTO.getAuthedUid();
        String uid = uidDTO.getUid();
        if (originUid.equals(uid)) {
            return ServerResult.buildFailureMessage("无法发送名片给自己");
        }
        UserProfileResult userProfileResult = userProfileOTS.getProfile(uid);
        if (userProfileResult == null) {
            return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
        }
        DeleteStatus deleteStatus = DeleteStatus.NORMAL;
        // 被对方拉黑了
        if (userBlockOTS.isUserBlock(uid, originUid)) {
            deleteStatus = DeleteStatus.DELETED_BY_USER;
        }
        boolean pushFlag = (userCardOTS.getUserSendCardDeleted(originUid, uid) != 0);
        if (userCardOTS.saveUserCard(originUid, uid, deleteStatus)) {
            if (pushFlag) {
                pushAsyncClient.pushAlertMessage(AlertMessageDTO.buildSendCardMessage(userProfileOTS.getProfile(originUid), uid));
            }
            return ServerResult.build(OperateStatus.SUCCESS).message("你给了对方一张自己的名片");
        }
        return ServerResult.buildFailureMessage("发送失败");
    }

    // 删除名片
    @RpcServerMethod
    public ServerResult deleteUserCard(@Validated UidDTO uidDTO) {
        String uid = uidDTO.getAuthedUid();
        String originUid = uidDTO.getUid();
        UserProfileResult userProfileResult = userProfileOTS.getProfile(originUid);
        if (userProfileResult == null) {
            return ServerResult.buildFailureMessage(OperateErrorCode.USER_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_NOT_EXIST.getCode());
        }
        if (userCardOTS.getUserSendCardDeleted(originUid, uid) == 1) {
            if (userCardOTS.saveUserCard(originUid, uid, DeleteStatus.DELETED_BY_USER)) {
                return ServerResult.build(OperateStatus.SUCCESS).message("删除成功");
            }
        }
        return ServerResult.build(OperateStatus.SUCCESS).message("删除成功");
    }

}
