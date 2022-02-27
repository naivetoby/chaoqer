package com.chaoqer.common.entity.push;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class UserMessageResult {

    // 用户ID
    private String uid;
    // 消息ID(自增长)
    private long messageId;
    // 消息来源Uid
    private String originUid;
    // 消息来源类型
    private int originPushType;
    // 消息类型
    private int messageType;
    // 消息内容
    private JSONObject messageBody;
    // 是否已读
    private int unread;
    // 创建时间
    private long createTime;

}
