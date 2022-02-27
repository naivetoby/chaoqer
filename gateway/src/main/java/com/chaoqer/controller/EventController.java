package com.chaoqer.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.club.ClubClient;
import com.chaoqer.client.event.EventClient;
import com.chaoqer.client.room.RoomClient;
import com.chaoqer.client.user.UserProfileClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.event.EventDTO;
import com.chaoqer.common.entity.event.EventIdDTO;
import com.chaoqer.common.entity.event.EventPageResult;
import com.chaoqer.common.entity.event.EventResult;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.common.util.ResponseUtil;
import com.chaoqer.util.FixedUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcResult;
import vip.toby.rpc.entity.ServerResult;
import vip.toby.rpc.entity.ServerStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "event", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class EventController {

    @Autowired
    private RoomClient roomClient;
    @Autowired
    private ClubClient clubClient;
    @Autowired
    private EventClient eventClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserProfileClient userProfileClient;

    /**
     * 创建日程
     */
    @RequestMapping(method = RequestMethod.POST)
    public String postEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody EventDTO eventDTO,
            @RequestAttribute Authed authed
    ) {
        RpcResult rpcResult = eventClient.postEvent(authed.buildDTO(eventDTO));
        if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), EventResult.class))).toString();
        }
        return ResponseUtil.createRpcResult(request, response, rpcResult);
    }

    /**
     * 编辑日程
     */
    @RequestMapping(method = RequestMethod.PUT, path = "{eventId:[a-f0-9]{32}}")
    public String putEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody EventDTO eventDTO,
            @PathVariable String eventId,
            @RequestAttribute Authed authed
    ) {
        eventDTO.setEventId(eventId);
        RpcResult rpcResult = eventClient.putEvent(authed.buildDTO(eventDTO));
        if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), EventResult.class))).toString();
        }
        return ResponseUtil.createRpcResult(request, response, rpcResult);
    }

    /**
     * 删除日程
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "{eventId:[a-f0-9]{32}}")
    public String deleteEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            EventIdDTO eventIdDTO,
            @PathVariable String eventId,
            @RequestAttribute Authed authed
    ) {
        eventIdDTO.setEventId(eventId);
        return ResponseUtil.createRpcResult(request, response, eventClient.deleteEvent(authed.buildDTO(eventIdDTO)));
    }

    /**
     * 获取日程
     */
    @RequestMapping(method = RequestMethod.GET, path = "{eventId:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE)
    public String getEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            EventIdDTO eventIdDTO,
            @PathVariable String eventId,
            @RequestAttribute Authed authed
    ) {
        EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventId), EventResult.class);
        if (eventResult != null) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, eventResult)).toString();
        }
        eventIdDTO.setEventId(eventId);
        RpcResult eventRpcResult = eventClient.getEvent(authed.buildDTO(eventIdDTO));
        // 错误直接跳过
        if (eventRpcResult.getServerStatus() == ServerStatus.SUCCESS && eventRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) eventRpcResult.getServerResult().getResult(), EventResult.class))).toString();
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.EVENT_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.EVENT_NOT_FOUND.getCode()).toString();
    }

    /**
     * 分页获取日程列表
     */
    @RequestMapping(method = RequestMethod.GET, consumes = MediaType.ALL_VALUE)
    public String getEventPageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            PageDTO pageDTO,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        pageDTO.setPage(page);
        RpcResult rpcResult = eventClient.getEventPageResultList(authed.buildDTO(pageDTO));
        if (rpcResult.getServerStatus() != ServerStatus.SUCCESS || rpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
            return ResponseUtil.createRpcResult(request, response, rpcResult);
        }
        JSONObject result = (JSONObject) rpcResult.getServerResult().getResult();
        // 缓存
        List<EventPageResult> eventPageResultList = new ArrayList<>();
        result.getJSONArray("list").forEach(obj -> {
            EventPageResult eventPageResult = JSON.toJavaObject((JSON) obj, EventPageResult.class);
            eventPageResultList.add(eventPageResult);
            EventResult eventResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getEventKey(eventPageResult.getEventId()), EventResult.class);
            if (eventResult != null) {
                eventPageResult.setEventResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, eventResult));
            } else {
                EventIdDTO eventIdDTO = new EventIdDTO();
                eventIdDTO.setEventId(eventPageResult.getEventId());
                RpcResult eventRpcResult = eventClient.getEvent(authed.buildDTO(eventIdDTO));
                // 错误直接跳过
                if (eventRpcResult.getServerStatus() == ServerStatus.SUCCESS && eventRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    eventPageResult.setEventResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) eventRpcResult.getServerResult().getResult(), EventResult.class)));
                }
            }
        });
        result.put("list", eventPageResultList);
        return ServerResult.buildSuccessResult(result).toString();
    }

    /**
     * 预约日程
     */
    @RequestMapping(method = RequestMethod.POST, path = "{eventId:[a-f0-9]{32}}/notify")
    public String postEventNotify(
            HttpServletRequest request,
            HttpServletResponse response,
            EventIdDTO eventIdDTO,
            @PathVariable String eventId,
            @RequestAttribute Authed authed
    ) {
        eventIdDTO.setEventId(eventId);
        return ResponseUtil.createRpcResult(request, response, eventClient.postEventNotify(authed.buildDTO(eventIdDTO)));
    }

    /**
     * 开始活动
     */
    @RequestMapping(method = RequestMethod.POST, path = "{eventId:[a-f0-9]{32}}/start")
    public String startEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            EventIdDTO eventIdDTO,
            @PathVariable String eventId,
            @RequestAttribute Authed authed
    ) {
        eventIdDTO.setEventId(eventId);
        RpcResult rpcResult = eventClient.startEvent(authed.buildDTO(eventIdDTO));
        if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedEventResult(eventClient, clubClient, roomClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), EventResult.class))).toString();
        }
        return ResponseUtil.createRpcResult(request, response, rpcResult);
    }

}
