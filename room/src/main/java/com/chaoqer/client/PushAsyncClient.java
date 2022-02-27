package com.chaoqer.client;

import com.chaoqer.common.entity.push.AlertMessageDTO;
import com.chaoqer.common.entity.push.AttachMessageDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcType;

@RpcClient(value = "push", type = RpcType.ASYNC)
public interface PushAsyncClient {

    @RpcClientMethod
    void pushAlertMessage(AlertMessageDTO alertMessageDTO);

    @RpcClientMethod
    void pushAttachMessage(AttachMessageDTO attachMessageDTO);

}
