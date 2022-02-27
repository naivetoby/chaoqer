package com.chaoqer.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.club.ClubClient;
import com.chaoqer.client.room.RoomClient;
import com.chaoqer.client.user.UserProfileClient;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.room.*;
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
@RequestMapping(value = "room", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class RoomController {

    @Autowired
    private RoomClient roomClient;
    @Autowired
    private ClubClient clubClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserProfileClient userProfileClient;

    /**
     * 创建活动
     */
    @RequestMapping(method = RequestMethod.POST)
    public String postRoom(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody PostRoomDTO postRoomDTO,
            @RequestAttribute Authed authed
    ) {
        RpcResult rpcResult = roomClient.postRoom(authed.buildDTO(postRoomDTO));
        if (rpcResult.getServerStatus() == ServerStatus.SUCCESS && rpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
            return ServerResult.buildSuccessResult(FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) rpcResult.getServerResult().getResult(), RoomResult.class))).toString();
        }
        return ResponseUtil.createRpcResult(request, response, rpcResult);
    }

    /**
     * 分页获取活动列表
     */
    @RequestMapping(method = RequestMethod.GET, consumes = MediaType.ALL_VALUE)
    public String getRoomPageResultList(
            HttpServletRequest request,
            HttpServletResponse response,
            PageDTO pageDTO,
            @Validated Page page,
            @RequestAttribute Authed authed
    ) {
        pageDTO.setPage(page);
        RpcResult rpcResult = roomClient.getRoomPageResultList(authed.buildDTO(pageDTO));
        if (rpcResult.getServerStatus() != ServerStatus.SUCCESS || rpcResult.getServerResult().getOperateStatus() != OperateStatus.SUCCESS) {
            return ResponseUtil.createRpcResult(request, response, rpcResult);
        }
        JSONObject result = (JSONObject) rpcResult.getServerResult().getResult();
        // 缓存
        List<RoomPageResult> roomPageResultList = new ArrayList<>();
        result.getJSONArray("list").forEach(obj -> {
            RoomPageResult roomPageResult = JSON.toJavaObject((JSON) obj, RoomPageResult.class);
            roomPageResultList.add(roomPageResult);
            RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomPageResult.getRoomId()), RoomResult.class);
            if (roomResult != null) {
                roomPageResult.setRoomResult(FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, roomResult));
            } else {
                RoomIdDTO roomIdDTO = new RoomIdDTO();
                roomIdDTO.setRoomId(roomPageResult.getRoomId());
                RpcResult roomRpcResult = roomClient.getRoom(authed.buildDTO(roomIdDTO));
                // 错误直接跳过
                if (roomRpcResult.getServerStatus() == ServerStatus.SUCCESS && roomRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                    roomPageResult.setRoomResult(FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) roomRpcResult.getServerResult().getResult(), RoomResult.class)));
                }
            }
        });
        result.put("list", roomPageResultList);
        return ServerResult.buildSuccessResult(result).toString();
    }

    /**
     * 获取活动
     */
    @RequestMapping(method = RequestMethod.GET, path = "{roomId:[a-f0-9]{32}}", consumes = MediaType.ALL_VALUE)
    public String getRoom(
            @PathVariable String roomId,
            @RequestAttribute Authed authed
    ) {
        RoomResult roomResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getRoomKey(roomId), RoomResult.class);
        if (roomResult != null) {
            roomResult = FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, roomResult);
        }
        if (roomResult == null) {
            RoomIdDTO roomIdDTO = new RoomIdDTO();
            roomIdDTO.setRoomId(roomId);
            RpcResult roomRpcResult = roomClient.getRoom(authed.buildDTO(roomIdDTO));
            // 错误直接跳过
            if (roomRpcResult.getServerStatus() == ServerStatus.SUCCESS && roomRpcResult.getServerResult().getOperateStatus() == OperateStatus.SUCCESS) {
                roomResult = FixedUtil.fixedRoomResult(clubClient, userProfileClient, redisTemplate, authed, JSONObject.toJavaObject((JSON) roomRpcResult.getServerResult().getResult(), RoomResult.class));
            }
        }
        if (roomResult != null) {
            return ServerResult.buildSuccessResult(roomResult).toString();
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.ROOM_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.ROOM_NOT_FOUND.getCode()).toString();
    }

    /**
     * 创建活动缓存
     */
    @RequestMapping(method = RequestMethod.GET, path = "{roomId:[a-f0-9]{32}}/cache", consumes = MediaType.ALL_VALUE)
    public String buildRoomCache(
            HttpServletRequest request,
            HttpServletResponse response,
            RoomIdDTO roomIdDTO,
            @PathVariable String roomId,
            @RequestAttribute Authed authed
    ) {
        roomIdDTO.setRoomId(roomId);
        return ResponseUtil.createRpcResult(request, response, roomClient.buildRoomCache(authed.buildDTO(roomIdDTO)));
    }

    /**
     * 获取活动声网Token
     */
    @RequestMapping(method = RequestMethod.GET, path = "{roomId:[a-f0-9]{32}}/token", consumes = MediaType.ALL_VALUE)
    public String getRoomAgoraToken(
            HttpServletRequest request,
            HttpServletResponse response,
            RoomIdDTO roomIdDTO,
            @PathVariable String roomId,
            @RequestAttribute Authed authed
    ) {
        roomIdDTO.setRoomId(roomId);
        return ResponseUtil.createRpcResult(request, response, roomClient.getRoomAgoraToken(authed.buildDTO(roomIdDTO)));
    }

    /**
     * 关闭活动
     */
    @RequestMapping(method = RequestMethod.POST, path = "{roomId:[a-f0-9]{32}}/close")
    public String closeRoom(
            HttpServletRequest request,
            HttpServletResponse response,
            RoomIdDTO roomIdDTO,
            @PathVariable String roomId,
            @RequestAttribute Authed authed
    ) {
        roomIdDTO.setRoomId(roomId);
        return ResponseUtil.createRpcResult(request, response, roomClient.closeRoom(authed.buildDTO(roomIdDTO)));
    }

    /**
     * 邀请用户加入活动
     */
    @RequestMapping(method = RequestMethod.POST, path = "{roomId:[a-f0-9]{32}}/invite/{uid:[a-f0-9]{32}}")
    public String roomInvite(
            HttpServletRequest request,
            HttpServletResponse response,
            RoomInviteDTO roomInviteDTO,
            @PathVariable String roomId,
            @PathVariable String uid,
            @RequestAttribute Authed authed
    ) {
        roomInviteDTO.setRoomId(roomId);
        roomInviteDTO.setUid(uid);
        return ResponseUtil.createRpcResult(request, response, roomClient.roomInvite(authed.buildDTO(roomInviteDTO)));
    }

}
