package com.chaoqer.server;

import com.alibaba.fastjson.JSONObject;
import com.chaoqer.common.entity.base.AuthedDTO;
import com.chaoqer.common.entity.push.PushConfigDTO;
import com.chaoqer.common.entity.push.PushConfigType;
import com.chaoqer.repository.UserPushConfigOTS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.Arrays;

@RpcServer(value = "user-push-config", type = RpcType.SYNC, threadNum = 4)
public class UserPushConfigServer {

    @Autowired
    private UserPushConfigOTS userPushConfigOTS;

    @RpcServerMethod
    public ServerResult putPushConfig(@Validated PushConfigDTO pushConfigDTO) {
        String uid = pushConfigDTO.getAuthedUid();
        PushConfigType pushConfigType = PushConfigType.getPushConfigType(pushConfigDTO.getPushConfigType());
        if (pushConfigType == null) {
            return ServerResult.buildFailureMessage("pushConfigType类型不存在");
        }
        userPushConfigOTS.savePushConfig(uid, pushConfigType.getType(), pushConfigDTO.getEnable());
        return ServerResult.build(OperateStatus.SUCCESS);
    }

    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getPushConfig(AuthedDTO authedDTO) {
        String uid = authedDTO.getAuthedUid();
        // 初始化设置
        Arrays.stream(PushConfigType.values()).filter(pushConfigType -> userPushConfigOTS.getPushConfigEnable(uid, pushConfigType.getType()) == null).forEach(pushConfigType -> userPushConfigOTS.savePushConfig(uid, pushConfigType.getType(), pushConfigType.getDefaultValue()));
        // 获取设置
        JSONObject result = new JSONObject();
        Arrays.stream(PushConfigType.values()).forEach(pushConfigType -> result.put(pushConfigType.getName(), userPushConfigOTS.getPushConfigEnable(uid, pushConfigType.getType())));
        return ServerResult.buildSuccessResult(result);
    }

}
