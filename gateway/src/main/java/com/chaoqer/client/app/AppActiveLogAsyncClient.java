package com.chaoqer.client.app;

import com.chaoqer.common.entity.app.AppUserActiveLogDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcType;

@RpcClient(value = "app-active-log", type = RpcType.ASYNC)
public interface AppActiveLogAsyncClient {

    @RpcClientMethod
    void createAppUserActiveLog(AppUserActiveLogDTO appUserActiveLogDTO);

}