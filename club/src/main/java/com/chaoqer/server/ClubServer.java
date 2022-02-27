package com.chaoqer.server;

import com.chaoqer.common.entity.base.OperateErrorCode;
import com.chaoqer.common.entity.base.Page;
import com.chaoqer.common.entity.base.PageDTO;
import com.chaoqer.common.entity.club.ClubIdDTO;
import com.chaoqer.common.entity.club.ClubIdPageDTO;
import com.chaoqer.common.entity.club.ClubResult;
import com.chaoqer.common.entity.club.PostClubDTO;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.repository.ClubOTS;
import com.chaoqer.repository.RoomOTS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.concurrent.TimeUnit;

@RpcServer(value = "club", threadNum = 4, type = RpcType.SYNC)
public class ClubServer {

    @Autowired
    private ClubOTS clubOTS;
    @Autowired
    private RoomOTS roomOTS;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 创建圈子
    @RpcServerMethod
    public ServerResult postClub(@Validated PostClubDTO postClubDTO) {
        String uid = postClubDTO.getAuthedUid();
        String clubId = DigestUtil.getUUID();
        String name = escapesJavaScript(postClubDTO.getName()).replace("\r\n", " ").replace("\n", " ");
        if (clubOTS.saveClub(clubId, uid, name, postClubDTO.getCover())) {
            // 自动加入圈子
            if (clubOTS.saveClubMember(clubId, uid, 1)) {
                return ServerResult.buildSuccessResult(clubOTS.getClub(clubId));
            }
        }
        return ServerResult.build(OperateStatus.FAILURE).message("创建圈子失败");
    }

    // 获取圈子
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getClub(@Validated ClubIdDTO clubIdDTO) {
        String clubId = clubIdDTO.getClubId();
        ClubResult clubResult = clubOTS.getClub(clubId);
        if (clubResult != null) {
            // 默认3天缓存
            RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getClubKey(clubId), clubResult, 30, TimeUnit.DAYS);
            return ServerResult.buildSuccessResult(clubResult);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode());
    }

    // 获取是否是圈子成员
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getClubMemberJoined(@Validated ClubIdDTO clubIdDTO) {
        String clubId = clubIdDTO.getClubId();
        ClubResult clubResult = clubOTS.getClub(clubId);
        if (clubResult != null) {
            return ServerResult.buildSuccessResult(clubOTS.isClubMember(clubId, clubIdDTO.getAuthedUid()) ? 1 : 0);
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode());
    }

    // 获取圈子成员
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getClubMemberPageResultList(ClubIdPageDTO clubIdPageDTO) {
        Page page = clubIdPageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        String clubId = clubIdPageDTO.getClubId();
        // 圈子是否存在
        ClubResult clubResult = clubOTS.getClub(clubId);
        if (clubResult != null) {
            return ServerResult.buildSuccessResult(clubOTS.getClubMemberPageResultList(clubId, page));
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode());
    }

    // 获取圈子成员总数
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getClubMemberTotal(@Validated ClubIdDTO clubIdDTO) {
        return ServerResult.buildSuccessResult(clubOTS.getClubMemberTotal(clubIdDTO.getClubId()));
    }

    // 获取圈子列表
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getClubPageResultList(PageDTO pageDTO) {
        Page page = pageDTO.getPage();
        if (page == null) {
            return ServerResult.buildFailureMessage("page不能为空");
        }
        return ServerResult.buildSuccessResult(clubOTS.getClubPageResultList(pageDTO.getAuthedUid(), page));
    }

    // 加入圈子
    @RpcServerMethod
    public ServerResult joinClub(@Validated ClubIdDTO clubIdDTO) {
        String clubId = clubIdDTO.getClubId();
        String uid = clubIdDTO.getAuthedUid();
        // 判断活动是否存在
        String clubKey = RedisKeyGenerator.getClubKey(clubId);
        if (RedisUtil.isKeyExist(redisTemplate, clubKey) || clubOTS.getClub(clubId) != null) {
            if (clubOTS.isClubMember(clubId, uid)) {
                return ServerResult.buildSuccessResult(clubOTS.getClub(clubId));
            }
            if (clubOTS.saveClubMember(clubId, uid, 0)) {
                // 扩散圈子所有活动
                roomOTS.getOpenedClubRoomIdList(clubId).forEach(roomId -> {
                    RoomResult roomResult = roomOTS.getRoom(roomId);
                    if (roomResult != null) {
                        roomOTS.asyncSaveUserRoom(roomId, uid, roomResult.getCreateTime());
                    }
                });
                // 成员数加1
                RedisUtil.increment(redisTemplate, RedisKeyGenerator.getClubMemberTotalKey(clubId));
                return ServerResult.buildSuccessResult(clubOTS.getClub(clubId));
            }
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode());
    }

    // 退出圈子
    @RpcServerMethod
    public ServerResult leaveClub(@Validated ClubIdDTO clubIdDTO) {
        String clubId = clubIdDTO.getClubId();
        String uid = clubIdDTO.getAuthedUid();
        // 必须在圈子里面
        if (clubOTS.isClubMember(clubId, uid)) {
            ClubResult clubResult = clubOTS.getClub(clubId);
            if (clubResult != null) {
                if (clubResult.getUid().equals(uid)) {
                    // 删除圈子成员, 删除圈子
                    if (clubOTS.getClubMemberTotal(clubId) == 1 && clubOTS.deleteClubMember(clubId, uid) && clubOTS.deleteClub(clubId)) {
                        // 删除圈子所有活动
                        roomOTS.getAllClubRoomIdList(clubId).forEach(roomId -> roomOTS.asyncDeleteUserRoom(roomId, uid));
                        // 删除缓存
                        RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getClubKey(clubId));
                        RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getClubMemberTotalKey(clubId));
                        return ServerResult.build(OperateStatus.SUCCESS);
                    }
                    return ServerResult.buildFailureMessage("创建人暂时无法退出圈子");
                }
                if (clubOTS.deleteClubMember(clubId, uid)) {
                    // 删除圈子所有活动
                    roomOTS.getAllClubRoomIdList(clubId).forEach(roomId -> roomOTS.asyncDeleteUserRoom(roomId, uid));
                    // 成员数减1
                    RedisUtil.decrement(redisTemplate, RedisKeyGenerator.getClubMemberTotalKey(clubId));
                    return ServerResult.build(OperateStatus.SUCCESS);
                }
                return ServerResult.build(OperateStatus.FAILURE);
            }
        }
        return ServerResult.buildFailureMessage(OperateErrorCode.CLUB_NOT_FOUND.getMessage()).errorCode(OperateErrorCode.CLUB_NOT_FOUND.getCode());
    }

    private String escapesJavaScript(String content) {
        content = content.replace("\u0008", " ");
        content = content.replace("\u0009", " ");
        content = content.replace("\u000B", " ");
        content = content.replace("\u000C", " ");
        content = content.replace("\u00A0", " ");
        content = content.replace("\u2028", "\n");
        content = content.replace("\u2029", "\n");
        content = content.replace("\uFEFF", " ");
        return content;
    }

}
