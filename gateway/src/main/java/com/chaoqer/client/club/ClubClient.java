package com.chaoqer.client.club;

import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.club.ClubIdDTO;
import com.chaoqer.common.entity.club.ClubIdPageDTO;
import com.chaoqer.common.entity.club.PostClubDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "club")
public interface ClubClient {

    @RpcClientMethod
    RpcResult postClub(PostClubDTO postClubDTO);

    @RpcClientMethod
    RpcResult getClub(ClubIdDTO clubIdDTO);

    @RpcClientMethod
    RpcResult getClubMemberJoined(ClubIdDTO clubIdDTO);

    @RpcClientMethod
    RpcResult getClubMemberPageResultList(ClubIdPageDTO clubIdPageDTO);

    @RpcClientMethod
    RpcResult getClubMemberTotal(ClubIdDTO clubIdDTO);

    @RpcClientMethod
    RpcResult getClubPageResultList(PageDTO pageDTO);

    @RpcClientMethod
    RpcResult joinClub(ClubIdDTO clubIdDTO);

    @RpcClientMethod
    RpcResult leaveClub(ClubIdDTO clubIdDTO);

}
