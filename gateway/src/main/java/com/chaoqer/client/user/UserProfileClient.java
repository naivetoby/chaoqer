package com.chaoqer.client.user;

import com.chaoqer.common.entity.base.AuthedDTO;
import com.chaoqer.common.entity.user.UserProfileDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "user-profile")
public interface UserProfileClient {

    @RpcClientMethod
    RpcResult postProfile(UserProfileDTO userProfileDTO);

    @RpcClientMethod
    RpcResult putProfile(UserProfileDTO userProfileDTO);

    @RpcClientMethod
    RpcResult getProfile(AuthedDTO authedDTO);

}
