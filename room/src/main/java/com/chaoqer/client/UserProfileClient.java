package com.chaoqer.client;

import com.chaoqer.common.entity.base.AuthedDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "user-profile")
public interface UserProfileClient {

    @RpcClientMethod
    RpcResult getProfile(AuthedDTO authedDTO);

}
