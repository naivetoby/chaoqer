package com.chaoqer.common.entity.push;

import com.alibaba.fastjson.JSONObject;
import com.chaoqer.common.entity.event.EventResult;
import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.entity.user.UserProfileResult;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@Data
public class AlertMessageDTO extends PushMessageDTO {

    // 是否通知(0: 不需要 / 1: 需要)
    @NotNull(message = "store类型不能为空")
    @Range(min = 0, max = 1, message = "store类型不存在")
    private Integer alert = 1;

    // 标题
    @NotNull(message = "标题不能为空")
    private String title;

    // 内容
    @NotNull(message = "内容不能为空")
    private String content;

    // 跳转地址
    @NotNull(message = "地址不能为空")
    private String url;

    public JSONObject getMessageBody() {
        JSONObject messageBody = new JSONObject();
        messageBody.put("title", title);
        messageBody.put("content", content);
        messageBody.put("url", url);
        return messageBody;
    }

    /**
     * 基本的互动通知
     *
     * @param originUid
     * @param toUid
     * @param title
     * @param content
     * @param url
     * @return
     */
    public static AlertMessageDTO buildBasicInteractiveAlertMessage(String originUid, String toUid, String title, String content, String url, int alert) {
        AlertMessageDTO alertMessageDTO = new AlertMessageDTO();
        alertMessageDTO.setUid(toUid);
        alertMessageDTO.setOriginUid(originUid);
        alertMessageDTO.setOriginPushType(OriginPushType.INTERACTIVE.getType());
        alertMessageDTO.setStore(1);
        alertMessageDTO.setTitle(title);
        alertMessageDTO.setContent(content);
        alertMessageDTO.setUrl(url);
        alertMessageDTO.setAlert(alert);
        return alertMessageDTO;
    }

    /**
     * 基本的系统通知
     *
     * @param toUid
     * @param title
     * @param content
     * @param url
     * @param alert
     * @return
     */
    public static AlertMessageDTO buildBasicSystemAlertMessage(String toUid, String title, String content, String url, int alert) {
        AlertMessageDTO alertMessageDTO = new AlertMessageDTO();
        alertMessageDTO.setUid(toUid);
        alertMessageDTO.setOriginPushType(OriginPushType.SYSTEM.getType());
        alertMessageDTO.setStore(1);
        alertMessageDTO.setTitle(title);
        alertMessageDTO.setContent(content);
        alertMessageDTO.setUrl(url);
        alertMessageDTO.setAlert(alert);
        return alertMessageDTO;
    }

    /**
     * 活动邀请
     */
    public static AlertMessageDTO buildRoomInviteMessage(UserProfileResult originUser, String toUid, RoomResult roomResult) {
        String roomName = roomResult.getName();
        if (StringUtils.isBlank(roomName)) {
            roomName = originUser.getNickname().concat("创建的活动");
        }
        return buildBasicInteractiveAlertMessage(originUser.getUid(), toUid, originUser.getNickname().concat("邀请你加入活动"), roomName, "chaoqer://room/".concat(roomResult.getRoomId()), 1);
    }

    /**
     * 发送名片
     */
    public static AlertMessageDTO buildSendCardMessage(UserProfileResult originUser, String toUid) {
        return buildBasicInteractiveAlertMessage(originUser.getUid(), toUid, originUser.getNickname(), "给你发送了一张名片", "chaoqer://profile/".concat(originUser.getUid()), 1);
    }

    /**
     * 日程邀请
     */
    public static AlertMessageDTO buildEventInviteMessage(UserProfileResult originUser, String toUid, EventResult eventResult) {
        return buildBasicInteractiveAlertMessage(originUser.getUid(), toUid, originUser.getNickname().concat("已添加你为活动嘉宾"), eventResult.getName(), "chaoqer://event/".concat(eventResult.getEventId()), 1);
    }

    /**
     * 日程活动提醒
     */
    public static AlertMessageDTO buildEventNotifyMessage(String toUid, EventResult eventResult) {
        return buildBasicSystemAlertMessage(toUid, "你预约的活动已经开始了", eventResult.getName(), "chaoqer://room/".concat(eventResult.getRoomId()), 1);
    }

}
