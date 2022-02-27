package com.chaoqer.client;

import com.chaoqer.common.entity.room.RoomIdDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "room")
public interface RoomClient {

    @RpcClientMethod
    RpcResult closeRoom(RoomIdDTO roomIdDTO);

}
