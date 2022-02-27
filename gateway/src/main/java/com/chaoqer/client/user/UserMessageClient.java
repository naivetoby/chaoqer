package com.chaoqer.client.user;

import com.chaoqer.common.entity.base.AuthedDTO;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.push.UserMessageIdDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "user-message")
public interface UserMessageClient {

    @RpcClientMethod
    RpcResult getUserMessagePageResultList(PageDTO pageDTO);

    @RpcClientMethod
    RpcResult getUserMessage(UserMessageIdDTO userMessageIdDTO);

    @RpcClientMethod
    RpcResult getUserMessageUnReadTotal(AuthedDTO authedDTO);

}
