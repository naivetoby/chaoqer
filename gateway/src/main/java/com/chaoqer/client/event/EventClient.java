package com.chaoqer.client.event;

import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.event.EventDTO;
import com.chaoqer.common.entity.event.EventIdDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "event")
public interface EventClient {

    @RpcClientMethod
    RpcResult postEvent(EventDTO eventDTO);

    @RpcClientMethod
    RpcResult putEvent(EventDTO eventDTO);

    @RpcClientMethod
    RpcResult deleteEvent(EventIdDTO eventIdDTO);

    @RpcClientMethod
    RpcResult getEvent(EventIdDTO eventIdDTO);

    @RpcClientMethod
    RpcResult getEventPageResultList(PageDTO pageDTO);

    @RpcClientMethod
    RpcResult postEventNotify(EventIdDTO eventIdDTO);

    @RpcClientMethod
    RpcResult isEventNotify(EventIdDTO eventIdDTO);

    @RpcClientMethod
    RpcResult startEvent(EventIdDTO eventIdDTO);

}
