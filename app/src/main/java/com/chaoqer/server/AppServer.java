package com.chaoqer.server;

import com.chaoqer.common.entity.app.FeedbackDTO;
import com.chaoqer.common.entity.app.ReportDTO;
import com.chaoqer.common.entity.app.VersionResult;
import com.chaoqer.common.entity.base.*;
import com.chaoqer.common.entity.base.origin.OriginFeedBackType;
import com.chaoqer.common.entity.base.origin.OriginReportType;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.OriginResult;
import com.chaoqer.repository.AppOTS;
import com.chaoqer.repository.RoomOTS;
import com.chaoqer.repository.UserOTS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RpcServer(value = "app", type = RpcType.SYNC, threadNum = 4)
public class AppServer {

    @Autowired
    private AppOTS appOTS;
    @Autowired
    private RoomOTS roomOTS;
    @Autowired
    private UserOTS userOTS;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getLatestVersion(AuthedDTO authedDTO) {
        ClientInfo clientInfo = authedDTO.getClientInfo();
        ClientType clientType = ClientType.getClientType(clientInfo.getClientType());
        if (clientType == ClientType.IOS || clientType == ClientType.ANDROID) {
            // 所有比较新版本
            List<VersionResult> versionResultList = appOTS.getLatestVersionList(clientInfo.getClientType(), clientInfo.getVersionCode());
            if (!versionResultList.isEmpty()) {
                VersionResult latestVersionResult = versionResultList.get(0);
                if (latestVersionResult.getForcedUpgrade() == 0 && (versionResultList.size() >= 5 || versionResultList.stream().anyMatch(versionResult -> versionResult.getForcedUpgrade() > 0))) {
                    latestVersionResult.setForcedUpgrade(1);
                }
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getAppLatestVersionKey(clientInfo.getClientType(), clientInfo.getVersionCode()), latestVersionResult, 1, TimeUnit.DAYS);
                if (latestVersionResult.getVersionCode() == clientInfo.getVersionCode()) {
                    return ServerResult.build(OperateStatus.FAILURE).message("当前已经是最新版本");
                }
                return ServerResult.buildSuccessResult(latestVersionResult);
            }
        }
        return ServerResult.build(OperateStatus.FAILURE).message("当前已经是最新版本");
    }

    @RpcServerMethod
    public ServerResult postFeedback(@Validated FeedbackDTO feedbackDTO) {
        List<String> feedbackImageList = feedbackDTO.getFeedbackImageList();
        if (feedbackImageList != null && feedbackImageList.size() > 3) {
            return ServerResult.buildFailureMessage("至多添加三张图片");
        }
        FeedbackType feedbackType = FeedbackType.getFeedbackType(feedbackDTO.getFeedbackType());
        OriginFeedBackType originFeedBackType = OriginFeedBackType.getOriginFeedBackType(feedbackDTO.getOriginFeedbackType());
        String uid = feedbackDTO.getAuthedUid();
        String originId = feedbackDTO.getOriginId();
        OriginResult originResult;
        switch (originFeedBackType) {
            case APP:
                appOTS.asyncSaveFeedback(uid, originFeedBackType, null, feedbackDTO.getClientInfo(), feedbackType, feedbackDTO.getFeedbackContent(), feedbackImageList);
                break;
            case ROOM:
                originResult = roomOTS.getOriginResultByRoomId(originId);
                if (originResult != null) {
                    appOTS.asyncSaveFeedback(uid, originFeedBackType, originResult, feedbackDTO.getClientInfo(), feedbackType, feedbackDTO.getFeedbackContent(), feedbackImageList);
                }
                break;
            default:
                break;
        }
        return ServerResult.build(OperateStatus.SUCCESS).message("反馈成功");
    }

    @RpcServerMethod
    public ServerResult postReport(@Validated ReportDTO reportDTO) {
        ReportReasonType reportReasonType = ReportReasonType.getReportReasonType(reportDTO.getReportReasonType());
        OriginReportType originReportType = OriginReportType.getOriginReportType(reportDTO.getOriginReportType());
        String uid = reportDTO.getAuthedUid();
        String originId = reportDTO.getOriginId();
        OriginResult originResult;
        switch (originReportType) {
            case USER:
                originResult = userOTS.getOriginResultByUid(originId);
                if (originResult != null) {
                    appOTS.asyncSaveReport(uid, originReportType, originResult, reportReasonType, reportDTO.getReason());
                }
                break;
            case ROOM:
                originResult = roomOTS.getOriginResultByRoomId(originId);
                if (originResult != null) {
                    appOTS.asyncSaveReport(uid, originReportType, originResult, reportReasonType, reportDTO.getReason());
                }
                break;
            default:
                break;
        }
        return ServerResult.build(OperateStatus.SUCCESS).message("投诉成功");
    }

}
