package com.chaoqer.client.room;

import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.room.PostRoomDTO;
import com.chaoqer.common.entity.room.RoomIdDTO;
import com.chaoqer.common.entity.room.RoomInviteDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "room")
public interface RoomClient {

    @RpcClientMethod
    RpcResult postRoom(PostRoomDTO postRoomDTO);

    @RpcClientMethod
    RpcResult getRoom(RoomIdDTO roomIdDTO);

    @RpcClientMethod
    RpcResult getRoomAgoraToken(RoomIdDTO roomIdDTO);

    @RpcClientMethod
    RpcResult getRoomPageResultList(PageDTO pageDTO);

    @RpcClientMethod
    RpcResult closeRoom(RoomIdDTO roomIdDTO);

    @RpcClientMethod
    RpcResult roomInvite(RoomInviteDTO roomInviteDTO);

    @RpcClientMethod
    RpcResult buildRoomCache(RoomIdDTO roomIdDTO);

}
