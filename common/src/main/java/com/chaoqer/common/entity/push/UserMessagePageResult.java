package com.chaoqer.common.entity.push;

import com.chaoqer.common.entity.base.PageResult;
import lombok.Data;

@Data
public class UserMessagePageResult extends PageResult {

    // 消息ID
    private long messageId;
    // UserMessage
    private UserMessageResult userMessageResult;

}
