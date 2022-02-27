package com.chaoqer.server;

import com.alibaba.fastjson.JSON;
import com.chaoqer.common.entity.base.AuthedDTO;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.push.UserMessageIdDTO;
import com.chaoqer.common.entity.push.UserMessageResult;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.UserMessageOTS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.concurrent.TimeUnit;

@RpcServer(value = "user-message", type = RpcType.SYNC, threadNum = 4)
public class UserMessageServer {

    @Autowired
    private UserMessageOTS userMessageOTS;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getUserMessagePageResultList(PageDTO pageDTO) {
        Page page = pageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        String userMessageUnReadTotalKey = RedisKeyGenerator.getUserMessageUnReadTotalKey(pageDTO.getAuthedUid());
        RedisUtil.delObject(redisTemplate, userMessageUnReadTotalKey);
        RedisUtil.increment(redisTemplate, userMessageUnReadTotalKey, 0);
        return ServerResult.buildSuccessResult(userMessageOTS.getUserMessagePageResultList(pageDTO.getAuthedUid(), page));
    }

    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getUserMessage(@Validated UserMessageIdDTO userMessageIdDTO) {
        String uid = userMessageIdDTO.getAuthedUid();
        long messageId = userMessageIdDTO.getMessageId();
        UserMessageResult userMessageResult = userMessageOTS.getUserMessageResult(uid, messageId);
        if (userMessageResult != null) {
            // 自动已读
            userMessageOTS.asyncUpdateUserMessageRead(uid, messageId);
            // 缓存1周
            UserMessageResult tempUserMessageResult = JSON.parseObject(JSON.toJSONString(userMessageResult), UserMessageResult.class);
            tempUserMessageResult.setUnread(0);
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getUserMessageKey(messageId), tempUserMessageResult, 7, TimeUnit.DAYS);
            return ServerResult.buildSuccessResult(userMessageResult);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.USER_MESSAGE_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_MESSAGE_NOT_EXIST.getCode());
    }

    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getUserMessageUnReadTotal(AuthedDTO authedDTO) {
        return ServerResult.buildSuccessResult(userMessageOTS.getUserMessageUnReadTotal(authedDTO.getAuthedUid()));
    }

    @RpcServerMethod
    public ServerResult deleteUserMessage(@Validated UserMessageIdDTO userMessageIdDTO) {
        String uid = userMessageIdDTO.getAuthedUid();
        long messageId = userMessageIdDTO.getMessageId();
        if (RedisUtil.isKeyExist(redisTemplate, RedisKeyGenerator.getUserMessageKey(messageId)) || userMessageOTS.getUserMessageResult(uid, messageId) != null) {
            userMessageOTS.asyncUpdateUserMessageDeleted(uid, messageId);
            return ServerResult.build(OperateStatus.SUCCESS).message("删除成功");
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.USER_MESSAGE_NOT_EXIST.getMessage()).errorCode(OperateErrorCode.USER_MESSAGE_NOT_EXIST.getCode());
    }

}
