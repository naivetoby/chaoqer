package com.chaoqer.client;

import com.chaoqer.common.entity.club.ClubIdDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "club")
public interface ClubClient {

    @RpcClientMethod
    RpcResult getClub(ClubIdDTO clubIdDTO);

}
