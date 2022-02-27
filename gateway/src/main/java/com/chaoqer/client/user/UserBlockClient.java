package com.chaoqer.client.user;

import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.user.UidDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "user-block")
public interface UserBlockClient {

    @RpcClientMethod
    RpcResult getUserBlockPageResultList(PageDTO pageDTO);

    @RpcClientMethod
    RpcResult isUserBlock(UidDTO uidDTO);

    @RpcClientMethod
    RpcResult postUserBlock(UidDTO uidDTO);

    @RpcClientMethod
    RpcResult deleteUserBlock(UidDTO uidDTO);

}
