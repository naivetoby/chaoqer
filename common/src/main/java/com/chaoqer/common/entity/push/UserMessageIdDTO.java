package com.chaoqer.common.entity.push;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserMessageIdDTO extends AuthedDTO {

    // 消息ID
    @NotNull(message = "messageId不能为空")
    private Long messageId;

}
