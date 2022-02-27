package com.chaoqer.client.user;

import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.user.UidDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "user-card")
public interface UserCardClient {

    @RpcClientMethod
    RpcResult getUserCardPageResultList(PageDTO pageDTO);

    @RpcClientMethod
    RpcResult getUserCardTotal(UidDTO uidDTO);

    @RpcClientMethod
    RpcResult isUserSendCard(UidDTO uidDTO);

    @RpcClientMethod
    RpcResult postUserCard(UidDTO uidDTO);

    @RpcClientMethod
    RpcResult deleteUserCard(UidDTO uidDTO);

}
