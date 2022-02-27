package com.chaoqer.client.push;

import com.chaoqer.common.entity.push.AlertMessageDTO;
import com.chaoqer.common.entity.push.AttachMessageDTO;
import com.chaoqer.common.entity.push.JPushIdDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcType;

@RpcClient(value = "push", type = RpcType.ASYNC)
public interface PushAsyncClient {

    @RpcClientMethod
    void updateDevice(JPushIdDTO jPushIdDTO);

    @RpcClientMethod
    void pushAlertMessage(AlertMessageDTO alertMessageDTO);

    @RpcClientMethod
    void pushAttachMessage(AttachMessageDTO attachMessageDTO);

}
