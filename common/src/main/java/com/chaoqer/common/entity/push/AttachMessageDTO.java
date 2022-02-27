package com.chaoqer.common.entity.push;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@Data
public class AttachMessageDTO extends PushMessageDTO {

    // 透传类型
    @NotNull(message = "attachType类型不能为空")
    @Range(min = 1, max = 2, message = "attachType类型不存在")
    private Integer attachType;

    // 透传内容(可选)
    private JSONObject attachBody;

    public JSONObject getMessageBody() {
        JSONObject messageBody = new JSONObject();
        messageBody.put("attachType", attachType);
        messageBody.put("attachBody", attachBody == null ? new JSONObject() : attachBody);
        return messageBody;
    }

    /**
     * 发送对话框
     */
    public static AttachMessageDTO buildDialogMessage(String toUid, String title, String content, String cancel, String confirm, String action) {
        AttachMessageDTO attachMessageDTO = new AttachMessageDTO();
        attachMessageDTO.setUid(toUid);
        attachMessageDTO.setOriginPushType(OriginPushType.SYSTEM.getType());
        attachMessageDTO.setAttachType(AttachType.DIALOG.getType());
        JSONObject attachBody = new JSONObject();
        attachBody.put("title", title);
        attachBody.put("content", content);
        attachBody.put("cancel", cancel);
        attachBody.put("confirm", confirm);
        attachBody.put("url", action);
        attachMessageDTO.setAttachBody(attachBody);
        return attachMessageDTO;
    }

    /**
     * 发送退出
     */
    public static AttachMessageDTO buildLogoutMessage(String toUid) {
        AttachMessageDTO attachMessageDTO = new AttachMessageDTO();
        attachMessageDTO.setUid(toUid);
        attachMessageDTO.setOriginPushType(OriginPushType.SYSTEM.getType());
        attachMessageDTO.setAttachType(AttachType.LOGOUT.getType());
        return attachMessageDTO;
    }

    /**
     * 发送新提醒
     */
    public static AttachMessageDTO buildNewNotificationMessage(String toUid) {
        AttachMessageDTO attachMessageDTO = new AttachMessageDTO();
        attachMessageDTO.setUid(toUid);
        attachMessageDTO.setOriginPushType(OriginPushType.SYSTEM.getType());
        attachMessageDTO.setAttachType(AttachType.NEW_NOTIFICATION.getType());
        return attachMessageDTO;
    }

}
