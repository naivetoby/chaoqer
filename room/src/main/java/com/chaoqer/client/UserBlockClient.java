package com.chaoqer.client;

import com.chaoqer.common.entity.user.UidDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "user-block")
public interface UserBlockClient {

    @RpcClientMethod
    RpcResult isUserBlock(UidDTO uidDTO);

}
