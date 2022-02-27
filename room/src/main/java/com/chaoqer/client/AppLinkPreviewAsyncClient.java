package com.chaoqer.client;

import com.chaoqer.common.entity.app.LinkDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcType;

@RpcClient(value = "app-link-preview", type = RpcType.ASYNC)
public interface AppLinkPreviewAsyncClient {

    @RpcClientMethod
    void postLink(LinkDTO linkDTO);

}
