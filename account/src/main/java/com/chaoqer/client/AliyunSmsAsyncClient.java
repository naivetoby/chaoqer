package com.chaoqer.client;

import com.chaoqer.common.entity.aliyun.AccessSmsDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcType;

@RpcClient(value = "aliyun-sms", type = RpcType.ASYNC)
public interface AliyunSmsAsyncClient {

    @RpcClientMethod
    void createAccessSms(AccessSmsDTO accessSmsDTO);

}
