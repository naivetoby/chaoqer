package com.chaoqer.common.entity.push;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PushMessageDTO {

    // 用户ID
    @NotBlank(message = "uid不能为空")
    private String uid;

    // 来源Uid
    private String originUid;

    // 来源类型
    @NotNull(message = "originPushType类型不能为空")
    @Range(min = 0, max = 2, message = "originPushType类型不存在")
    private Integer originPushType;

    // 是否持久化消息(0: 不需要 / 1: 需要)
    @NotNull(message = "store类型不能为空")
    @Range(min = 0, max = 1, message = "store类型不存在")
    private Integer store = 0;

}
